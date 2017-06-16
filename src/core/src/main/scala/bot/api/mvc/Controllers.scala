package bot.api.mvc

import info.mukel.telegrambot4s.methods.ParseMode.ParseMode
import info.mukel.telegrambot4s.methods.{ApiRequest, EditMessageReplyMarkup, EditMessageText, ParseMode, SendMessage}
import info.mukel.telegrambot4s.models.{InlineKeyboardMarkup, Message, ReplyMarkup}
import play.twirl.api.Html

import scala.concurrent.Future
import scala.language.implicitConversions

trait Controllers {

  implicit def s_to_s(r: ApiRequest[_]): Seq[ApiRequest[_]] = Seq(r)

  def noopM: Future[SeqResult] = Future.successful(noop)
  def noop: SeqResult = Seq.empty

  //Generate text message from template
  def Text(
      c: Html)
    (implicit request: Request): Result =
    Text(c.body, Some(ParseMode.HTML))

  def Text(
      c: Html, markup: ReplyMarkup)
    (implicit request: Request): Result =
    Text(c.body, Some(ParseMode.HTML), replyMarkup = Some(markup))

  def Text(
      c: Html,
      markup: ReplyMarkup,
      disableWebPagePreview: Boolean)
    (implicit request: Request): Result =
    Text(c.body, Some(ParseMode.HTML), replyMarkup = Some(markup), disableWebPagePreview = Some(disableWebPagePreview))

  def Text(text            : String,
      parseMode            : Option[ParseMode] = None,
      disableWebPagePreview: Option[Boolean] = None,
      disableNotification  : Option[Boolean] = None,
      replyToMessageId     : Option[Long] = None,
      replyMarkup          : Option[ReplyMarkup] = None)(implicit request: Request): Result = {

    val chatId = request.chatId.getOrElse(throw new RuntimeException("Missing chat id in request"))

    SendMessage(Left(chatId), text, parseMode, disableWebPagePreview, disableNotification, replyToMessageId, replyMarkup)
  }

  /**
    * Create text message based on AbstractTextMessage class.
    * This is the prioritize method to send text messages.
    * @param m abstract text message
    */
//  def Text(m: AbstractTextMessage): Result = m.build()

  def EditMarkup(markup: InlineKeyboardMarkup)(implicit message: Message): Result = {
    val chatId = message.chat.id
    val messageId = message.messageId
    EditMessageReplyMarkup(Some(Left(chatId)), Some(messageId), replyMarkup = Some(markup))
  }

  def EditMessage(text: String,
      markup: Option[ReplyMarkup] = None,
      disableWebPagePreview : Option[Boolean] = None)(implicit request: Request): Result = {
    val chatId = request.chatId.getOrElse(throw new RuntimeException("Missing chat id in request"))
    val messageId = request.messageId.getOrElse(throw new RuntimeException("Missing message id in request"))
    EditMessageText(
                     Some(Left(chatId)),
                     Some(messageId),
                     text = text,
                     parseMode = Some(ParseMode.HTML),
                     replyMarkup = markup
                   )
  }

  def EditMessage(html: Html,
      markup: ReplyMarkup)
    (implicit request: Request): Result = {

    EditMessage(html.body, Some(markup))
  }

  def EditMessage(html: Html,
      markup: ReplyMarkup,
      disableWebPagePreview : Boolean)
    (implicit request: Request): Result = {

    EditMessage(html.body, Some(markup), Some(disableWebPagePreview))
  }
}