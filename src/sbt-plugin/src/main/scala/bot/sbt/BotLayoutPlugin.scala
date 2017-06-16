package bot.sbt

import sbt._

import play.twirl.sbt.Import.TwirlKeys
import sbt.Keys.{baseDirectory, javaSource, resourceDirectory, scalaSource, sourceDirectories, sourceDirectory, target}
import sbt.{AllRequirements, AutoPlugin, Compile, Test}
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._

object BotLayoutPlugin extends AutoPlugin {

  override def requires = BotSbtPlugin

  override def trigger = AllRequirements

  override def projectSettings =
    Seq(
         target := baseDirectory.value / "target",

         sourceDirectory in Compile := baseDirectory.value / "app",
         sourceDirectory in Test := baseDirectory.value / "test",

         resourceDirectory in Compile := baseDirectory.value / "conf",

         scalaSource in Compile := baseDirectory.value / "app",
         scalaSource in Test := baseDirectory.value / "test",

         javaSource in Compile := baseDirectory.value / "app",
         javaSource in Test := baseDirectory.value / "test",

         sourceDirectories in(Compile, TwirlKeys.compileTemplates) := Seq((sourceDirectory in Compile).value),
         sourceDirectories in(Test, TwirlKeys.compileTemplates) := Seq((sourceDirectory in Test).value),


         // Native packager
         sourceDirectory in Universal := baseDirectory.value / "dist"
       )
}
