package bot.core

import java.io.File
import akka.actor.ActorSystem
import akka.stream.Materializer
import com.google.inject.{Inject, Singleton}
import com.typesafe.scalalogging.Logger
import bot.core.Environment.Mode
import bot.api.Configuration
import bot.server.Router
import bot.server.RequestExecutor
import bot.server.Pooling

import scala.concurrent.Future

case class Environment(rootPath: File, classLoader: ClassLoader, mode: Mode.Mode)

object Environment {
  object Mode extends Enumeration {
    type Mode = Value
    val Dev, Prod, Test = Value
  }
}

trait RunStop{
  def run(): Unit
  def stop(): Future[Unit]
}

sealed abstract class Application {
  val injector: GuiceInjector
  val mode: Mode.Mode
  private[core] def run(): Unit
  private[core] def stop(): Future[Unit]
  def isDev: Boolean = mode == Mode.Dev
  def isTest: Boolean = mode == Mode.Test
  def isProd: Boolean = mode == Mode.Prod
  def configuration: Configuration
  def actorSystem: ActorSystem
}

@Singleton
final class DefaultApplication @Inject()(
    environment: Environment,
    override val configuration: Configuration,
    override val injector: GuiceInjector,
    override val actorSystem: ActorSystem,
    val materializer: Materializer
  ) extends Application {

  private val logger = Logger(getClass)
  override val mode: Mode.Mode = environment.mode

  lazy val runStop: RunStop = {
    //Create route instance dynamically
    //This will allow to inject application into baseness logic.
    val router: Router = injector.instanceOf[Router]
    val requestExecutor: RequestExecutor = injector.instanceOf[RequestExecutor]

    mode match {
      case Mode.Dev ⇒
        new Pooling(router, requestExecutor)(materializer, actorSystem)
      case _ ⇒ throw new NotImplementedError
    }
  }

  private[core] override def run(): Unit = {
    try {
      runStop.run()
      mode match {
        case Mode.Dev ⇒ logger.info(Console.GREEN + "Start dev application." + Console.RESET)
        case Mode.Prod ⇒ logger.info(Console.GREEN + "Start prod application." + Console.RESET)
      }
    } catch {
      case e: Throwable ⇒
        logger.error("Error start application.", e)
    }
  }

  private[core] override def stop(): Future[Unit] = {
    logger.warn(Console.RED + "Stop application." + Console.RESET)
    runStop.stop()
  }
}
