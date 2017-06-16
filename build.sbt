import Dependencies._
import BuildSettings._
import Generators.BotVersion

lazy val BotWsApi = Project("bot-ws", file("src/bot-ws"))
  .settings(botCommonSettings :_*)
  .settings(
             libraryDependencies ++= botWsDeps
           )

lazy val BotTelegramApi = Project("Bot-Telegram-Api", file("src/telegram-api"))
  .settings(botCommonSettings :_*)
  .settings(
             libraryDependencies ++= botTelegramApiDeps
           )

lazy val BotCore =
  Project("Bot-Core", file("src/core"))
    .settings(botCommonSettings :_*)
    .enablePlugins(SbtTwirl)
    .settings(
               libraryDependencies ++= botCoreDeps,
               sourceGenerators in Compile += Def.task(BotVersion(version.value, scalaVersion.value, sbtVersion.value, (sourceManaged in Compile).value)).taskValue
           )
    .dependsOn(BotTelegramApi)

lazy val BotStarter =
  Project("Bot-Starter", file("src/starter"))
    .settings(botCommonSettings :_*)
    .dependsOn(BotCore)

lazy val BotSbtPlugin =
  Project("Bot-Sbt-Plugin", file("src/sbt-plugin"))
    .settings(botCommonSettings :_*)
    .settings(
               scalaVersion := "2.10.6",
               libraryDependencies ++= sbtDependencies(sbtVersion.value, scalaVersion.value),
               sbtPlugin := true,
               sourceGenerators in Compile += Def.task(BotVersion(version.value, scalaVersion.value, sbtVersion.value, (sourceManaged in Compile).value)).taskValue
             )

lazy val PlayTelegram = Project("Play-Telegram", file("."))
  .settings(
             publishLocal := {},
             publish := {},
             publishArtifact := false
           )
  .aggregate(BotCore, BotStarter, BotTelegramApi, BotSbtPlugin, BotWsApi)
        