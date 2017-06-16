package info.mukel.telegrambot4s.api

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{Directives, Route}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait WebRoutes extends BotBase with AkkaImplicits {

  import Directives._

  val port: Int
  val interfaceIp: String = "::0"

  def routes: Route = reject

  private var bindingFuture: Future[Http.ServerBinding] = _

  abstract override def run(): Unit = {
    super.run()
    bindingFuture = Http().bindAndHandle(routes, interfaceIp, port)
    bindingFuture.foreach { _ =>
      logger.info(s"Listening on $interfaceIp:$port")
    }

    sys.addShutdownHook {
      Await.ready(shutdown(), 30.seconds)
    }
  }

  abstract override def shutdown(): Future[Unit] = {
    super.shutdown().transformWith {
      _ =>
        for {
          b <- bindingFuture
          _ <- b.unbind()
          t <- system.terminate()
        } yield ()
    }
  }
}

