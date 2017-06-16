import Dependencies._
import BuildSettings._

scalaVersion := "2.12.2"

lazy val BotTelegramApi = Project("Bot-Telegram-Api", file("src/telegram-api"))
  .settings(botCommonSettings :_*)
  .settings(
             libraryDependencies ++= botTelegramApiDeps
           )

lazy val BotCore =
  Project("Bot-Core", file("src/core"))
  .settings(botCommonSettings :_*)
  .settings(
            libraryDependencies ++= botCoreDeps
           )
  .dependsOn(BotTelegramApi)

lazy val BotStarter =
  Project("Bot-Starter", file("src/starter"))
    .settings(botCommonSettings :_*)
    .dependsOn(BotCore)

//lazy val BotApi =
//  Project("Bot-Api", file("src/api"))
//    .settings(botCommonSettings :_*)
//    .dependsOn(BotTelegramApi)

lazy val PlayTelegram = Project("Play-Telegram", file("."))
  .settings(
             publishLocal := {},
             publish := {},
             publishArtifact := false
           )
  .aggregate(BotCore, BotStarter, BotTelegramApi)
        