package bot.core.system

import java.lang.management.ManagementFactory
import java.util.Properties

class SystemProcess(val args: Seq[String]){
  def classLoader: ClassLoader = Thread.currentThread.getContextClassLoader
  def properties: Properties = System.getProperties
  def pid: Option[String] = {
    ManagementFactory.getRuntimeMXBean.getName.split('@').headOption
  }
  def addShutdownHook(hook: => Unit): Unit = {
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run(): Unit = hook
    })
  }
  def exit(message: String, cause: Option[Throwable] = None, returnCode: Int = -1): Nothing = {
    System.err.println(message)
    cause.foreach(_.printStackTrace())
    System.exit(returnCode)
    // Code never reached, but throw an exception to give a type of Nothing
    throw new Exception("SystemProcess.exit called")
  }
}