package bot.api.mvc

import com.typesafe.scalalogging.LazyLogging
import info.mukel.telegrambot4s.methods.ApiRequest
import info.mukel.telegrambot4s.models.{CallbackQuery, Message, PreCheckoutQuery, ShippingQuery}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Head trait for incoming request from telegram
  */
sealed trait Request{

  /**
    * Optional chat of of this request
    * @return chat id for request if exists
    */
  def chatId: Option[Long]

  /**
    * Optional message id of this request.
    * @return message id for request if exists.
    */
  def messageId: Option[Long]
}

object Request{

  /**
    * Create request based on message.
    * @param message message to create request.
    * @return new Request
    */
  def apply(message: Message): Request = {
    new Request {
      override def chatId = Some(message.chat.id)
      override def messageId = Some(message.messageId)
    }
  }
}

//main request to be handle (user type message)
class MessageRequest(val message: Message) extends Request{
  override def chatId = Some(message.chat.id)
  override def messageId = Some(message.messageId)
}
//User inline keyboards callback queries
class CallbackRequest(val query: CallbackQuery) extends Request{
  override def chatId = query.message.map(_.chat.id)
  override def messageId = query.message.map(_.messageId)
}
//User shipping query
class ShippingRequest(val query: ShippingQuery) extends Request{
  override def chatId = None
  override def messageId = None
}

class PreCheckoutRequest(val query: PreCheckoutQuery) extends Request{
  override def chatId = None
  override def messageId = None
}

//main message action. This action will handle request from telegram.
trait EssentialAction extends (Request ⇒ Future[SeqResult]) with LazyLogging{
  def executionContext: ExecutionContext
}

trait ActionFunction[-R, +A]{ self ⇒
  protected def executionContext: ExecutionContext
  //this is the main method to implement
  def invokeBlock(req: R, block: A ⇒ Future[SeqResult]): Future[SeqResult]

  //compose other action with this one applied first
  def andThen[B](other : ActionFunction[A, B]): ActionFunction[R, B] =
    new ActionFunction[R, B] {
      override protected def executionContext = self.executionContext
      override def invokeBlock(req: R, block: (B) ⇒ Future[SeqResult]) =
        self.invokeBlock(req, other.invokeBlock(_, block))
    }

  //compose other action with this one applied last
  def compose[B](other: ActionFunction[B, R]): ActionFunction[B, A] =
    other.andThen(this)
}

trait ActionBuilder[R] extends ActionFunction[Request, R]{ self ⇒
  // Construct action and that returned message action
  final def async(block: R ⇒ Future[SeqResult]): EssentialAction = {
    new EssentialAction {
      override def executionContext = self.executionContext
      override def apply(req: Request) = try{
        invokeBlock(req, block)
      } catch {
        // NotImplementedError is not caught by NonFatal, wrap it
        case e: NotImplementedError => throw new RuntimeException(e)
      }
    }
  }

  final def async(block: ⇒ Future[SeqResult]): EssentialAction = async(_ ⇒ block)
  //construct action with request
  final def apply(block: R ⇒ SeqResult): EssentialAction = async(block andThen Future.successful)
  //construct action without request
  final def apply(block: ⇒ SeqResult): EssentialAction = apply(_ ⇒ block)

  override def andThen[B](other: ActionFunction[R, B]): ActionBuilder[B] =
    new ActionBuilder[B] {
      override protected def executionContext = self.executionContext
      override def invokeBlock(req: Request, block: (B) ⇒ Future[SeqResult]) = self.invokeBlock(req, other.invokeBlock(_, block))
    }
}

trait ActionRefine[From, To] extends ActionFunction[From, To]{
  protected def refine(req: From): Future[ApiRequest[_] Either To]
  override def invokeBlock(req: From, block: (To) ⇒ Future[SeqResult]) = {
    refine(req).flatMap {
      case Left(res) ⇒ Future.successful(Seq(res))
      case Right(to) ⇒ block(to)
    }(executionContext)
  }
}

trait ActionTransformer[From, To] extends ActionRefine[From, To]{
  protected def transform(req: From): Future[To]
  override protected def refine(req: From) = transform(req).map(Right(_))(executionContext)
}

//injected base default action
object DefaultAction extends ActionBuilder[MessageRequest]{
  override protected def executionContext = ExecutionContext.global
  override def invokeBlock(req: Request, block: (MessageRequest) ⇒ Future[SeqResult]) = block(req.asInstanceOf[MessageRequest])
}