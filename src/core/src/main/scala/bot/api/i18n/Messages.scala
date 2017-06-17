package bot.api.i18n

import java.io.InputStream
import java.text.MessageFormat
import java.util.{Locale, ResourceBundle}

import com.typesafe.scalalogging.LazyLogging

import scala.util.{Failure, Success, Try}

/** Messages externalization
  *
  * == Overview ==
  * You would use it like so:
  *
  * {{{
  * Localized(user) { implicit lang =>
  *   val error = Messages("error")
  * }
  * }}}
  *
  * Messages are stored in `messages_XXX.txt` files in UTF-8 encoding in resources.
  * The lookup will fallback to default file `messages.txt` if the string is not found in
  * the language-specific file.
  *
  * Messages are formatted with `java.text.MessageFormat`.
  */
trait Messages extends LazyLogging{
  private [i18n] val FileName = "messages"
  private [i18n] val FileExt = "txt"

//  /** get the message w/o formatting */
//  def raw(msg: String)(implicit lang: Lang): String = {
//    val bundle = ResourceBundle.getBundle(FileName, lang.locale, UTF8BundleControl)
//    bundle.getString(msg)
//  }

  def apply(msg: String, args: Any*)(implicit lang: Lang): String = {
    Try{
      val bundle = ResourceBundle.getBundle(FileName, lang.locale, UTF8BundleControl)
      val str = bundle.getString(msg)
      new MessageFormat(str, lang.locale).format(args.map(_.asInstanceOf[java.lang.Object]).toArray)
    } match {
      case Success(v) ⇒ v
      case Failure(ex) ⇒
        logger.error(s"Error apply message for key $msg", ex)
        throw ex
    }
  }
}

object Messages extends Messages

object I18N extends Messages

// @see https://gist.github.com/alaz/1388917
// @see http://stackoverflow.com/questions/4659929/how-to-use-utf-8-in-resource-properties-with-resourcebundle
private[i18n] object UTF8BundleControl extends ResourceBundle.Control with LazyLogging{
  val Format = "properties.utf8"

  override def getFormats(baseName: String): java.util.List[String] = {
    import collection.JavaConverters._
    Seq(Format).asJava
  }

  override def getFallbackLocale(baseName: String, locale: Locale) =
    if (locale == Locale.getDefault) null
    else Locale.getDefault

  override def newBundle(baseName: String, locale: Locale, fmt: String, loader: ClassLoader, reload: Boolean): ResourceBundle = {
    import java.io.InputStreamReader
    import java.util.PropertyResourceBundle

    // The below is an approximate copy of the default Java implementation
    //todo: ay be its not best realisation for this method. Need to check nulls of locale and language
    def resourceName = {
      locale match {
        case null ⇒ baseName
        case l ⇒
          l.getLanguage match {
            case null ⇒ baseName
            case "" ⇒ baseName
            case lang ⇒ baseName + "." + lang
          }
      }
    }

    def stream: Option[InputStream] =
      if (reload) {
        for {url <- Option(loader getResource resourceName)
             connection <- Option(url.openConnection)}
          yield {
            connection.setUseCaches(false)
            connection.getInputStream
          }
      } else
        Option(loader getResourceAsStream resourceName)

    (for {format <- Option(fmt) if format == Format
          is <- stream}
      yield new PropertyResourceBundle(new InputStreamReader(is, "UTF-8"))).orNull
  }
}