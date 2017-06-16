package bot.server

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.google.inject.{Inject, Singleton}
import com.typesafe.scalalogging.LazyLogging
import info.mukel.telegrambot4s.methods.{DeleteWebhook, GetUpdates}
import info.mukel.telegrambot4s.models.Update
import bot.core.RunStop

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.util.control.NonFatal

@Singleton
class Pooling @Inject()(router: Router, requestExecutor: RequestExecutor)
(implicit val materializer: Materializer, system: ActorSystem) extends RunStop with LazyLogging {

  implicit val ex: ExecutionContext = system.dispatcher

  val pollingInterval: Int = 30

  private val updates: Source[Update, NotUsed] = {
    type Offset = Long
    type Updates = Seq[Update]
    type OffsetUpdates = Future[(Offset, Updates)]

    val seed: OffsetUpdates = Future.successful((0L, Seq.empty[Update]))

    val iterator = Iterator.iterate(seed) {
      _ flatMap {
        case (offset, updts) =>
          val maxOffset = updts.map(_.updateId).fold(offset)(_ max _)
          requestExecutor(GetUpdates(Some(maxOffset + 1), timeout = Some(pollingInterval)))
            .recover {
              case NonFatal(e) =>
                logger.error("GetUpdates failed", e)
                Seq.empty[Update]
            }
            .map {
              (maxOffset, _)
            }
      }
    }

    val parallelism = Runtime.getRuntime.availableProcessors()

    val updateGroups =
      Source.fromIterator(() => iterator)
        .mapAsync(parallelism)(
          _ map {
            case (_, getUpdates) => getUpdates
          })

    updateGroups.mapConcat(_.to) // unravel groups
  }

  def run(): Unit = {
    requestExecutor(DeleteWebhook).onComplete {
      case Success(true) =>
        logger.info(Console.GREEN + s"Starting polling: interval = $pollingInterval" + Console.RESET)

        // Updates are executed synchronously by default to preserve order.
        // To make the it async, just wrap the update handler in a Future
        // or mix AsyncUpdates.
        updates
          .runForeach {
            update =>
              try {
                router.handleUpdate(update).map{requests ⇒
                  Source.fromIterator(() ⇒ requests.iterator).runFoldAsync(0){case (acc, req) ⇒
                    requestExecutor(req).map{response ⇒
//                      response
                      acc + 1
                    }
                  }
//                  requests.foreach{req ⇒ requestExecutor(req).map{res ⇒
//                    //TODO: handle request results
////                    logger.trace(s"Got response [${res}] from request [${req}]")
//                  }}
                } recover {
                  case NonFatal(e) ⇒
                    logger.error(s"Error execute request ", e)
                }
              } catch {
                case NonFatal(e) =>
                  logger.error("Caught exception in update handler", e)
              }
          }

      case Success(false) =>
        logger.error("Failed to clear webhook")

      case Failure(e) =>
        logger.error("Failed to clear webhook", e)
    }
  }

  def stop(): Future[Unit] = {
    logger.info(Console.GREEN + "Shutting down polling" + Console.RESET)
    system.terminate()
    Future.successful(())
  }
}
