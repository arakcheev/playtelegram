package bot.api.mvc

import com.typesafe.scalalogging.LazyLogging
import reflect.runtime.universe._

import scala.util.{Failure, Success, Try}

//abstract button to sent to user in message text
trait AbstractCommandButton[T]{
  def apply(value: T): String
  def unapply(arg: String): Option[T]
}

//Simple command
abstract class SimpleCommand(name: String) extends AbstractCommandButton[Unit]{
  override def apply(value: Unit): String = name
  override def unapply(arg: String) = if( arg equals name) Some(()) else None
  def apply(): String = apply(())
}

abstract class ExtractPatternCommand[T: TypeTag] extends AbstractCommandButton[T] with LazyLogging{

  def prefix: String
  def extractor: String

  private val fullPattern = s"""$prefix$extractor"""
  protected val regex = fullPattern.r.unanchored

  protected def toT(v: String): T = {
    if(typeOf[T] <:< typeOf[String]) v.asInstanceOf[T]
    else if(typeOf[T] =:= typeOf[Double]) v.toDouble.asInstanceOf[T]
    else if(typeOf[T] =:= typeOf[Long]) v.toLong.asInstanceOf[T]
    else if(typeOf[T] =:= typeOf[Int]) v.toInt.asInstanceOf[T]
    else if(typeOf[T] =:= typeOf[Byte]) v.toByte.asInstanceOf[T]
    else throw new RuntimeException(s"Invalid type class $v")
  }

  override def apply(value: T) = s"""$prefix$value"""

  override def unapply(arg: String): Option[T] = {
    Try{
      val regex(a) = arg
      toT(a)
    } match {
      case Success(v) ⇒
        logger.trace(s"Extract [$v] from string [$arg] by pattern [$fullPattern]")
        Some(v)
      case Failure(_) ⇒
        logger.trace(s"Cant parse [$arg] for pattern [$fullPattern]")
        None
    }
  }
}

abstract class ExtractIntCommand
(   val prefix: String,
    val extractor: String = "(\\d+)"
) extends ExtractPatternCommand[Int]

abstract class ExtractLongCommand
(   val prefix: String,
    val extractor: String = "(\\d+)"
) extends ExtractPatternCommand[Long]

abstract class ExtractStringCommand
(   val prefix: String,
    val extractor: String = "(\\w+)"
) extends ExtractPatternCommand[String]
