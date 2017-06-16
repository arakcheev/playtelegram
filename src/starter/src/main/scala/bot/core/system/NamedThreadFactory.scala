package bot.core.system

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory}


case class NamedThreadFactory(name: String) extends ThreadFactory {
  val threadNo = new AtomicInteger()
  val backingThreadFactory = Executors.defaultThreadFactory()

  def newThread(r: Runnable): Thread = {
    val thread = backingThreadFactory.newThread(r)
    thread.setName(name + "-thread-" + threadNo.incrementAndGet())
    thread
  }
}