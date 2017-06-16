enablePlugins(BuildInfoPlugin)

val Versions = new {
  val sbtNativePackager = "1.1.6"
  val sbtHeader = "1.8.0"
  val sbtTwirl: String = sys.props.getOrElse("twirl.version", "1.3.0")
}

buildInfoKeys :=
  Seq[BuildInfoKey](
                     "sbtNativePackagerVersion" -> Versions.sbtNativePackager,
                     "sbtTwirlVersion" -> Versions.sbtTwirl
                   )

buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % "1.3.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.0-M8")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")


logLevel := Level.Warn


