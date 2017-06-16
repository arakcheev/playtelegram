package bot.server

import com.google.inject.{AbstractModule, Inject, Singleton}
import com.typesafe.scalalogging.LazyLogging
import info.mukel.telegrambot4s.methods.{AnswerPreCheckoutQuery, AnswerShippingQuery}
import info.mukel.telegrambot4s.models.{CallbackQuery, Message, PreCheckoutQuery, ShippingQuery, SuccessfulPayment, Update}
import bot.api.mvc.{CallbackRequest, EssentialAction, MessageRequest, PreCheckoutRequest, ShippingRequest}
import bot.api.mvc.SeqResult

import scala.concurrent.{ExecutionContext, Future}

//main trait to implement message actions
//injected via configuration
trait MessageActions{
  def actionFor(message: Message): Option[EssentialAction]
}

trait CallbackActions{
  def actionFor(callbackQuery: CallbackQuery): Option[EssentialAction]
}

trait ShippingActions{
  def handle(shippingRequest: ShippingRequest): Future[AnswerShippingQuery]
}

trait CheckoutActions{
  def preCheckoutHandler(preCheckoutRequest: PreCheckoutRequest): Future[AnswerPreCheckoutQuery]
  def success(payment: SuccessfulPayment, message: Message): Future[SeqResult]
}

object HandlerModule{
  class DefaultMessageActions extends MessageActions{
    override def actionFor(message: Message) = None
  }
  class DefaultCallbackActions extends CallbackActions{
    override def actionFor(callbackQuery: CallbackQuery) = None
  }
  class DefaultShippingAction extends ShippingActions{
    override def handle(shippingRequest: ShippingRequest) = Future.failed(???)
  }
  class DefaultPreCheckoutAction extends CheckoutActions{
    override def preCheckoutHandler(preCheckoutRequest: PreCheckoutRequest) = Future.failed(???)
    override def success(payment: SuccessfulPayment, message: Message) = Future.successful(Seq.empty)
  }
}

class HandlerModule extends AbstractModule{
  import HandlerModule._
  override def configure() = {
    bind(classOf[MessageActions]).to(classOf[DefaultMessageActions])
    bind(classOf[CallbackActions]).to(classOf[DefaultCallbackActions])
    bind(classOf[ShippingActions]).to(classOf[DefaultShippingAction])
    bind(classOf[CheckoutActions]).to(classOf[DefaultPreCheckoutAction])
  }
}

//Main router for handle updates
@Singleton
class Router @Inject()(
                       messageActions : MessageActions,
                       callbackActions: CallbackActions,
                       shippingActions: ShippingActions,
                       checkoutActions: CheckoutActions) extends LazyLogging{

  private def emptyResults: Future[SeqResult] = Future.successful(Seq.empty)

  private def onMessage(msg: Message): Future[SeqResult] = {
    logger.trace(s"Got message with text ${msg.text}")
    //check for successfulPayment in message.
    msg.successfulPayment match {
      case Some(payment) ⇒ checkoutActions.success(payment, msg)
      case None ⇒
        val messageRequest = new MessageRequest(msg)
        messageActions.actionFor(msg) match {
          case None ⇒ emptyResults
          case Some(action) ⇒ action(messageRequest)
        }
    }
  }

  private def onCallback(cbq: CallbackQuery): Future[SeqResult] = {
    logger.trace(s"Got callback with data ${cbq.data}")
    callbackActions.actionFor(cbq) match {
      case None ⇒ emptyResults
      case Some(action) ⇒ action(new CallbackRequest(cbq))
    }
  }

  private def onShippingQuery(sq: ShippingQuery): Future[SeqResult] = {
    shippingActions.handle(new ShippingRequest(sq)).map(Seq(_))(ExecutionContext.global)
  }

  private def onPreCheckoutQuery(pcq: PreCheckoutQuery): Future[SeqResult] = {
    checkoutActions.preCheckoutHandler(new PreCheckoutRequest(pcq)).map(Seq(_))(ExecutionContext.global)
  }

  def handleUpdate(update: Update): Future[SeqResult] = {
    update.message
      .map(onMessage)
      .orElse(update.editedMessage.map(onMessage))
      .orElse(update.callbackQuery.map(onCallback))
      .orElse(update.shippingQuery.map(onShippingQuery))
      .orElse(update.preCheckoutQuery.map(onPreCheckoutQuery))
      .getOrElse(throw new RuntimeException(s"Invalid update $update"))
  }
}