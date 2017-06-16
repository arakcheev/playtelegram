package bot.core.system

import bot.api.Configuration
import bot.core.Environment.Mode
import bot.core.{AkkaModule, Application, DefaultApplication, Environment, GuiceInjector, ModuleLoader}
import bot.server.HandlerModule
import com.google.inject.{AbstractModule, Guice, Stage}
import com.typesafe.scalalogging.Logger
import com.google.inject.util.Modules

/**
  * Application starter.
  */
private[system] class Starter(
    environment: Environment,
    configuration: Configuration
) {

  val logger = Logger(getClass)

  /**
    * Load modules from configuration.
    */
  def loadModulesFromConfiguration(): Seq[AbstractModule] = {
    val loader = new ModuleLoader(configuration, environment)
    loader.load()
  }

  /**
    * Load build in modules.
    */
  def loadBuildInModules(): Seq[AbstractModule] = {

    class CommonModule extends AbstractModule {
      override def configure(): Unit = {
        bind(classOf[Configuration]).toInstance(configuration)
        bind(classOf[Environment]).toInstance(environment)
        bind(classOf[Application]).to(classOf[DefaultApplication])
      }
    }

    Seq(
      new AkkaModule(configuration),
      new HandlerModule,
      new CommonModule
       )
  }

  /**
    * Create main injector. This method will initialize all application.
    */
  def createInjector: GuiceInjector = {
    val stage = environment.mode match {
      case Mode.Prod ⇒ Stage.PRODUCTION
      case _ ⇒ Stage.DEVELOPMENT
    }
    try {
      import scala.collection.JavaConverters._
      //override default modules, like MessageActions to modules, that
      //determine in configuration file
      val modules = Modules.`override`(loadBuildInModules().asJava).`with`(loadModulesFromConfiguration().asJava)
      Guice.createInjector(stage, modules).getInstance(classOf[GuiceInjector])
    } catch {
      case e: Throwable ⇒
        logger.error("Error initialize application", e)
        System.exit(1)
        throw e
    }
  }

  lazy val injector: GuiceInjector = createInjector
}
