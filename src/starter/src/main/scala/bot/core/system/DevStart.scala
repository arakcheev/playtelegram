package bot.core.system

import java.io.File

import bot.core.Environment.Mode
import bot.core.{Application, Environment}
import bot.api.Configuration

/**
  * This is development application main class.
  */
object DevStart {
  def main(args: Array[String]): Unit = {

    try {
      val classLoader = Thread.currentThread.getContextClassLoader
      val environment = Environment(new File("."), classLoader, Mode.Dev)
      val configuration = Configuration.load(classLoader)

      new Starter(environment, configuration).injector.instanceOf[Application].run()

//      val routeFile = new File("/Users/artemarakcheev/WorkSpace/merchantbot/src/main/resources/routes")
//
//      import merchantbot.api.routes._
//
//      val file =
//        """
//          |GET next-:id   controllers.App.index(id: Int)
//          |GET next/:id2    controllers.App.index2(id2: Int)
//          |GET next:id3    controllers.App.index3(id3: Int)
//        """.stripMargin
//
//      val res = RouteFileParser.parseContent(file, new File("."))
//
//      res match {
//        case Right(rules) ⇒
//
//          val routes = rules.collect{
//            case r: Route ⇒ r
//          }
//
//          routes.map(_.path).foreach{pathPattern ⇒
//            val matcher = PathMatcher(pathPattern.parts)
//
//            println(matcher.apply("next/12") )
//            println(matcher.apply("next-12") )
//            println(matcher.apply("next12") )
//          }
//
//      }
    } catch {
      case e: Throwable ⇒
        System.err.println("Error start application")
        e.printStackTrace()
        System.exit(1)
    }
  }
}
