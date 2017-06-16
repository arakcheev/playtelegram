package bot.server

import javax.inject.Singleton

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import com.google.inject.{ImplementedBy, Inject}
import com.typesafe.scalalogging.LazyLogging
import info.mukel.telegrambot4s.api.TelegramApiException
import info.mukel.telegrambot4s.marshalling.HttpMarshalling
import info.mukel.telegrambot4s.methods.{ApiRequest, ApiResponse}
import bot.api.Configuration

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AkkaRequestExecutor])
trait RequestExecutor {

  /** Spawns a type-safe request.
    *
    * @param request to send to telegram
    * @tparam R Request's expected result type
    * @return The request result wrapped in a Future (async)
    */
  def apply[R: Manifest](request: ApiRequest[R]): Future[R]
}

@Singleton
class AkkaRequestExecutor @Inject()(configuration: Configuration)
                                   (implicit system: ActorSystem, materializer: Materializer, ec: ExecutionContext) extends RequestExecutor with LazyLogging {

  private val token = configuration.getString("bot.token").getOrElse(sys.error("Missing token in config"))

  import HttpMarshalling._

//  private val apiBaseUrl = s"https://149.154.167.197/bot$token/"
  private val apiBaseUrl = s"https://api.telegram.org/bot$token/"

  private val http: HttpExt = Http()

  private def toHttpRequest[R: Manifest](r: ApiRequest[R]): Future[HttpRequest] = {
    Marshal(r).to[RequestEntity]
      .map {
        re =>
          HttpRequest(HttpMethods.POST, Uri(apiBaseUrl + r.methodName), entity = re)
      }
  }

  private def toApiResponse[R: Manifest](httpResponse: HttpResponse): Future[ApiResponse[R]] = {
    Unmarshal(httpResponse.entity).to[ApiResponse[R]]
  }

  /** Spawns a type-safe request.
    *
    * @param request request to execute
    * @tparam R Request's expected result type
    * @return The request result wrapped in a Future (async)
    */
  override def apply[R: Manifest](request: ApiRequest[R]): Future[R] = {
    toHttpRequest(request)
      .flatMap(http.singleRequest(_))
      .flatMap(toApiResponse[R])
      .flatMap {
        case ApiResponse(true, Some(result), _, _, _) =>
          Future.successful(result)

        case ApiResponse(false, _, description, Some(errorCode), parameters) =>
          val e = TelegramApiException(description.getOrElse("Unexpected/invalid/empty response"), errorCode, None, parameters)
          logger.error("Telegram API exception", e)
          Future.failed(e)

        case _ =>
          val msg = "Error on request response"
          logger.error(msg)
          Future.failed(new Exception(msg))
      }
  }
}

