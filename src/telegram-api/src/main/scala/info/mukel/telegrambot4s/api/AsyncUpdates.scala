package info.mukel.telegrambot4s.api

import info.mukel.telegrambot4s.models.Update

import scala.concurrent.Future

/**
  * Asynchronify update handlers.
  * It affects all handlers since all of them are called by onUpdate directly.
  * Mix it last.
  */
trait AsyncUpdates extends BotBase with AkkaImplicits {
  abstract override def onUpdate(u: Update): Unit = Future(super.onUpdate(u))
}
