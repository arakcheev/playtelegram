package bot.sbt

import sbt._
import sbt.Keys._
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging

object BotSbtSettings {

  lazy val defaultSettings: Seq[Setting[_]] =
    Seq(
         //will need for routes compilation
//         TwirlKeys.constructorAnnotations += "@javax.inject.Inject()",

         javacOptions in(Compile, doc) := List("-encoding", "utf8"),

      //add main dependency to project. This must be the main of all bot.
         libraryDependencies += {
           "com.playtelegram" %% "bot-starter" % bot.core.BotVersion.current
         },

         // Adds app directory's source files to continuous hot reloading
         watchSources ++= {
           ((sourceDirectory in Compile).value ** "*").get
         },

         commands ++= {
           Seq()
         },

         // THE `in Compile` IS IMPORTANT!
         //Keys.run in Compile := PlayRun.playDefaultRunTask.evaluated,
         mainClass in(Compile, Keys.run) := Some("bot.core.system.DevStart"),
         mainClass in Compile := Some("bot.core.system.ProdStart"),

         mappings in Universal ++= {
           val confFiles = (resourceDirectory in Compile).value ** "*"

           confFiles.get.map(file => file -> ("conf/" + file.name))
         }

       )
}
