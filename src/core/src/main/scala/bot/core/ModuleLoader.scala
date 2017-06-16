package bot.core

import scala.util.{Failure, Success, Try}
import com.google.inject.AbstractModule
import org.apache.commons.lang3.reflect.ConstructorUtils
import bot.api.Configuration

class ModuleLoader(configuration: Configuration, environment: Environment) {


  def load(): Seq[AbstractModule] = {
    val modules = configuration.getStringList("bot.modules")

    val anyModules = modules.map { name ⇒
      initializeModule(name, () ⇒ environment.classLoader.loadClass(name).asInstanceOf[Class[Any]])
    }

    anyModules.map {
      case m: AbstractModule ⇒ m
      case unknown ⇒ throw CoreException(s"Module [$unknown] is not a Guice AbstractModule.")
    }
  }

  private def initializeModule[T](className: String, loadModuleClass: () => Class[T]): T = {
    Try {
      val moduleClass = loadModuleClass()
      ConstructorUtils.getAccessibleConstructor(moduleClass).newInstance()
    } match {
      case Success(t) ⇒ t
      case Failure(e) ⇒
        throw CoreException(s"Module $className cant be initialized.", e)
    }
  }
}
