package bot.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.server.Directives.{as, complete, entity, pathEndOrSingleSlash, pathPrefix}
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import bot.api.Configuration
import bot.core.RunStop
import com.typesafe.scalalogging.LazyLogging
import info.mukel.telegrambot4s.marshalling.HttpMarshalling
import info.mukel.telegrambot4s.methods.SetWebhook
import info.mukel.telegrambot4s.models.Update

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Random, Success}
import scala.util.control.NonFatal


private[bot] class WebHook(configuration: Configuration, router: Router, requestExecutor: RequestExecutor)(implicit val materializer: Materializer, system: ActorSystem) extends RunStop with LazyLogging {

  /**
    * Path prefix to webhook. For example, https://example.com/path_prefix
    */
  private val prefix: String = Random.alphanumeric.take(20).mkString

  /** URL for the webhook.
    * 'webhookUrl' must be consistent with 'webhookRoute' (by default '/').
    */
  private val webhookUrl = configuration.getString("bot.webhookUrl").getOrElse(sys.error("Missing bot.webhookUrl in configuration.")) + "/" + prefix

  val port: Int = configuration.getInt("bot.port").getOrElse(9000)
  val interfaceIp: String = configuration.getString("bot.interfaceIp").getOrElse("::0")

  private var bindingFuture: Future[Http.ServerBinding] = _

  import HttpMarshalling._

  /**
    * Webhook handler.
    *
    * @return Route handler to process updates.
    */
  private def webhookRoute = {
    pathPrefix(prefix){
      pathEndOrSingleSlash {
        entity(as[Update]) {
          update =>
            try {
              router.handleUpdate(update).map { requests ⇒
                Source.fromIterator(() ⇒ requests.iterator)
                  .runFoldAsync(0) { case (acc, req) ⇒
                    requestExecutor(req).map { response ⇒
                      acc + 1
                    }
                  }
              } recover {
                case NonFatal(e) ⇒
                  logger.error(s"Error execute request.", e)
              }
            } catch {
              case NonFatal(e) =>
                logger.error("Caught exception in update handler", e)
            }
            complete(StatusCodes.OK)
        }
      }
    }
  }

  private def attach(): Unit = {
    import Directives._

    val routes: Route = webhookRoute ~ reject

    bindingFuture = Http().bindAndHandle(routes, interfaceIp, port)

    bindingFuture.foreach { _ =>
      logger.info(Console.GREEN + s"Starting webhook at url: $webhookUrl on port $interfaceIp:$port" + Console.RESET)
    }

  }

  /**
    * Set webhook and attach routes
    */
  override def run() = {
    requestExecutor(SetWebhook(webhookUrl))
      .onComplete {
        case Success(true)  => attach() // Spawn WebRoute
        case Success(false) => logger.error("Failed to set webhook.")
        case Failure(e)     => logger.error("Failed to set webhook", e)
      }
  }

  override def stop() = {
    logger.info(Console.GREEN + "Shutting down webhook." + Console.RESET)
    for {
      b <- bindingFuture
      _ <- b.unbind()
      _ <- system.terminate()
    } yield ()
  }
}
