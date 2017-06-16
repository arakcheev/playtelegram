package bot.ws

import java.io.IOException
import java.net.URL

import com.google.inject.{ImplementedBy, Inject}
import com.typesafe.scalalogging.LazyLogging
import net.ruippeixotog.scalascraper.browser.JsoupBrowser.JsoupDocument
import org.jsoup.{Connection, Jsoup}
import org.jsoup.helper.HttpConnection
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.collection.JavaConverters._

case object TimeOutException extends RuntimeException("timeout")

/**
  * This class represents request. Request consists of url, method, request headers and request cookies.
  */
final case class WSRequest(
    url                   : URL,
    method                : String,
    cookies               : Map[String, String],
    headers               : Map[String, String]) {

  def getUserAgent: String = headers.getOrElse("User-Agent", WSRequest.DEFAULT_USER_AGENT)
}

object WSRequest {
  val DEFAULT_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 Safari/537.36"
  val defaultHeaders = Map(
                            "Accept" -> "text/html, application/xhtml+xml, application/xml",
                            "Accept-Charset" -> "utf-8",
                            "Accept-Encoding" -> "gzip, deflate",
                            "Accept-Language" -> "ru-RU,ru;en-US",
                            "Cache-Control" -> "no-cache",
                            "Pragma" -> "no-cache",
                            "User-Agent" -> DEFAULT_USER_AGENT
                          )

  def apply(url: URL): WSRequest = {
    WSRequest(url, "GET", cookies = Map.empty, headers = defaultHeaders)
  }

  def apply(url: String): WSRequest = apply(new URL(url))
}

/**
  * Response class that consists of url, that was fetch, returned body, headers and cookies.
  */
final case class WSResponse(
    url                      : URL,
    body                     : Array[Byte],
    charset                  : Option[String],
    contentType              : String,
    status                   : Int,
    cookies                  : Map[String, String],
    headers                  : Map[String, String]) extends LazyLogging{

  def bodyAsDocument: JsoupDocument = {
    val string = bodyAsString
    JsoupDocument(Jsoup.parse(string))
  }

  def bodyAsString: String = {
    new String(body, charset.getOrElse("utf-8"))
  }

  def json: JsValue = Json.parse(body)
}

/**
  * Interface, that represents HTTP(S) request execution.
  */
@ImplementedBy(classOf[JsoupWSApi])
trait WSApi {
  val MAX_RETRY = 10


  /**
    * Execute request at specific execution context.
    *
    * @param request          request to execute
    * @param executionContext execution context, where request should be executed
    */
  def execute(request: WSRequest)(implicit executionContext: ExecutionContext): Future[WSResponse]

  def retryingExecute(request: WSRequest)(implicit executionContext: ExecutionContext): Future[WSResponse] = {

    def loop(attempt: Int): Future[WSResponse] = {
      if (attempt >= MAX_RETRY) Future.failed(new RuntimeException(s"Cant fetch url $request. Max attempts reached."))
      else {
        val futureResponse: Future[WSResponse] = execute(request)

        futureResponse recoverWith{
          case _: IOException ⇒ loop(attempt + 1)
        }
      }
    }

    loop(0)

  }
}

/**
  * Request execution implementation, based on JSOUP html parser library
  */
class JsoupWSApi @Inject() extends WSApi with LazyLogging {


  /**
    * Execute request at specific execution context.
    *
    * Default timeout for request is 3 seconds.
    *
    * @param request          request to execute
    * @param executionContext execution context, where request should be executed
    */
  override def execute(request: WSRequest)(implicit executionContext: ExecutionContext): Future[WSResponse] = {
    Future {
      val start = System.currentTimeMillis()
      val connection: Connection = HttpConnection.connect(request.url)
      connection.method(Connection.Method.valueOf(request.method.toUpperCase))

      //Ignore HTTP errors due to we need to parse error body otherwise
      connection.ignoreHttpErrors(true)
      //Accepted any content type
      connection.ignoreContentType(true)

      connection.validateTLSCertificates(false)
      connection.cookies(request.cookies.asJava)
      connection.followRedirects(true)
      connection.timeout(60000)

      request.headers.foreach {
        case (name, value) => connection.header(name, value)
      }

      var httpResponse: Connection.Response = null

      try {
        httpResponse = connection.execute()

        WSResponse(
                  request.url,
                  httpResponse.bodyAsBytes(),
                  Option(httpResponse.charset()),
                  httpResponse.contentType(),
                  httpResponse.statusCode(),
                  httpResponse.cookies().asScala.toMap,
                  httpResponse.headers().asScala.toMap)
      } catch {
        case NonFatal(e) =>
          logger.warn(s"Error execute request, cause ${e.getMessage}")
          throw e
      } finally {
        val stop = System.currentTimeMillis()
        logger.trace(s"Request to url [${request.url}] was executed by ${stop - start} millis.")
      }
    }
  }
}
