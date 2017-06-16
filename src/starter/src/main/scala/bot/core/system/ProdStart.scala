package bot.core.system

import java.io.{File, FileOutputStream}

import bot.core.Environment.Mode
import bot.api.Configuration
import bot.core.{Application, Environment}

import scala.util.control.NonFatal

case class ServerStartException(msg: String) extends RuntimeException(msg)

/**
  * This is production application main class.
  */
object ProdStart{

  val mode = Mode.Prod

  val configPath = "prod.conf"

  def main(args: Array[String]): Unit = {
    start(new SystemProcess(args))
  }

  def start(process: SystemProcess): Unit ={
    try{
      val rootDir = new File(".")
      val pidFile = createPidFile(process, rootDir)

      val environment = Environment(rootDir, process.classLoader, mode)
      val configuration: Configuration = Configuration.load(environment.classLoader)

      val starter = new Starter(environment, configuration)

      val application: Application = starter.injector.instanceOf[Application]

      application.run()

      process.addShutdownHook {
        application.stop()
        pidFile.delete()
        assert(!pidFile.exists(), "PID file should not exist!")
      }
    } catch {
      case NonFatal(e) ⇒
        process.exit("Cannot start server", Some(e))
    }
  }

  def createPidFile(process: SystemProcess, root: File): File = {
    val pidFilePath = s"${root.getAbsolutePath}/RUNNING_PID"
    val pidFile = new File(pidFilePath).getAbsoluteFile

    if (pidFile.exists) {
      throw ServerStartException(s"This application is already running or u should delete ${pidFile.getPath} file.")
    }

    val pid = process.pid getOrElse (throw ServerStartException("Couldn't determine current process's pid"))
    val out = new FileOutputStream(pidFile)
    try out.write(pid.getBytes) finally out.close()
    pidFile
  }

}