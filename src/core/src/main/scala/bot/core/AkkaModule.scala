package bot.core

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.google.inject.AbstractModule
import bot.api.Configuration

import scala.concurrent.ExecutionContext

class AkkaModule(configuration: Configuration) extends AbstractModule{

  val system = ActorSystem("bot", configuration.underline)
  val materializer = ActorMaterializer()(system)

  override def configure(): Unit = {
    bind(classOf[ActorSystem]).toInstance(system)
    bind(classOf[ActorMaterializer]).toInstance(materializer)
    bind(classOf[Materializer]).toInstance(materializer)
    bind(classOf[ExecutionContext]).toInstance(system.dispatcher)
  }
}
