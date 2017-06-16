import sbt._

object Generators{
  // Generates a scala file that contains the playtelegram version for use at runtime.
  def BotVersion(version: String, scalaVersion: String, sbtVersion: String, dir: File): Seq[File] = {
    val file = dir / "BotVersion.scala"
    val scalaSource =
      """|package bot.core
         |
         |object BotVersion {
         |  val current = "%s"
         |  val scalaVersion = "%s"
         |  val sbtVersion = "%s"
         |}
         |""".stripMargin.format(version, scalaVersion, sbtVersion)

    if (!file.exists() || IO.read(file) != scalaSource) {
      IO.write(file, scalaSource)
    }

    Seq(file)
  }
}