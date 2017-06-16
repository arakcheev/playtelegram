import sbt.Keys._
import sbt._

object BuildSettings{
  def botCommonSettings: Seq[Setting[_]] =
    Seq(
         organization := "com.playtelegram",
         scalaVersion := "2.12.2",
         scalacOptions ++= Seq(
                                "-deprecation",
                                "-encoding", "UTF-8",
                                "-feature",
                                "-unchecked",
                                //"-Xfatal-warnings",
                                "-Yno-adapted-args",
                                "-Ywarn-dead-code"
                              ),
         resolvers ++= Seq(
                            Resolver.sonatypeRepo("releases"),
                            Resolver.typesafeRepo("releases"),
                            Resolver.typesafeIvyRepo("releases")
                          )
       )
}