package bot.api

import java.io.File

import com.typesafe.config.{Config, ConfigFactory, ConfigParseOptions}
import com.typesafe.config.impl.ConfigImpl

import scala.collection.JavaConverters._
import scala.util.Try

object Configuration{

  def load(classLoader: ClassLoader): Configuration = {

    val systemConfig = ConfigImpl.systemPropertiesAsConfig()

    val allowMissingApplicationConf = true

    // Resolve application.conf ourselves because:
    // - we may want to load configuration when application.conf is missing.
    // - We also want to delay binding and resolving reference.conf, which
    //   is usually part of the default application.conf loading behavior.
    // - We want to read config.file and config.resource settings from our
    //   own properties and directConfig rather than system properties.
    val applicationConfig: Config = {
      def setting(key: String): Option[AnyRef] =
        Option(System.getProperties.getProperty(key))

      {
        setting("config.resource").map(resource => ConfigFactory.parseResources(classLoader, resource.toString))
      } orElse {
        setting("config.file").map(fileName => ConfigFactory.parseFileAnySyntax(new File(fileName.toString)))
      } getOrElse {
        val parseOptions = ConfigParseOptions.defaults
          .setClassLoader(classLoader)
          .setAllowMissing(allowMissingApplicationConf)
        ConfigFactory.defaultApplication(parseOptions)
      }
    }

    // Resolve reference.conf ourselves because ConfigFactory.defaultReference resolves
    // values, and we won't have a value for `play.server.dir` until all our config is combined.
    val referenceConfig: Config = ConfigFactory.parseResources(classLoader, "reference.conf")

    val combinedConfig: Config = Seq(
                                      systemConfig,
                                      applicationConfig,
                                      referenceConfig
                                    ).reduceLeft(_ withFallback _)

    val resolvedConfig = combinedConfig.resolve

    Configuration(resolvedConfig)
  }
}

case class Configuration(underline : Config) {

  private def get[A](path: String, value: ⇒ A): Option[A] = {
    Try(if(underline.hasPath(path)) Some(value) else None).toOption.flatten
  }

  def getString(path: String): Option[String] = {
    get(path, underline.getString(path))
  }

  def getInt(path: String): Option[Int] = {
    get(path, underline.getInt(path))
  }

  def getStringList(path: String): List[String] = {
    get(path, underline.getStringList(path).asScala.toList).getOrElse(Nil)
  }
}
