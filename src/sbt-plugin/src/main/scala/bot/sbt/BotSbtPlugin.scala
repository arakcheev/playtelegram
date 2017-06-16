package bot.sbt

import com.typesafe.sbt.packager.archetypes.JavaServerAppPackaging
import play.twirl.sbt.SbtTwirl
import sbt._
import sbt.Keys._

object BotSbtPlugin extends sbt.AutoPlugin{

  override def requires = JavaServerAppPackaging && SbtTwirl

  object autoImport {
    val botWs: ModuleID = "com.playtelegram" %% "bot-ws" % bot.core.BotVersion.current
  }

  override def projectSettings =
    BotSbtSettings.defaultSettings ++
      Seq(
           scalacOptions ++= Seq("-deprecation", "-unchecked", "-encoding", "utf8"),
           javacOptions in Compile ++= Seq("-encoding", "utf8", "-g")
         )
}
