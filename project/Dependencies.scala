import sbt._

object Dependencies {
  // major.minor are in sync with the elasticsearch releases
  def elasticDeps(version: String = "5.4.2"): Seq[ModuleID] = {
    val main =
      Seq("elastic4s-core", "elastic4s-tcp", "elastic4s-http")
        .map(d ⇒ "com.sksamuel.elastic4s" %% d % version)

    val test = Seq("elastic4s-testkit", "elastic4s-embedded")
      .map(d ⇒ "com.sksamuel.elastic4s" %% d % version % "test")

    main ++ test
  }

  def akkaDeps(v: String = "2.4.18"): Seq[ModuleID] = {
    Seq(
         "com.typesafe.akka" %% "akka-actor"      % v,
         "com.typesafe.akka" %% "akka-stream"     % v
       )
  }

  def akkaHttp(v: String = "10.0.5"): Seq[ModuleID] = {
    Seq("com.typesafe.akka" %% "akka-http"       % v)
  }

  def mongoDeps(v: String = "0.12.1"): Seq[ModuleID] = {
    Seq("org.reactivemongo" %% "reactivemongo" % v)
  }

  def jacksonDeps(v: String = "3.5.0"): Seq[ModuleID] = {
    Seq(
         "org.json4s"                 %% "json4s-jackson"  % v,
         "org.json4s"                 %% "json4s-ext"      % v
       )
  }

  def loggerDeps(v: String = "3.5.0"): Seq[ModuleID] = {
    Seq("com.typesafe.scala-logging" %% "scala-logging"   % v)
  }

  def configDeps(v: String = "1.3.1"): Seq[ModuleID] = {
    Seq("com.typesafe" % "config" % v)
  }

  def injectDeps(v: String = "4.1.0"): Seq[ModuleID] = {
    Seq("com.google.inject" % "guice" % v)
  }

  def logbakcDeps(v: String = "1.1.7"): Seq[ModuleID] = {
    Seq("ch.qos.logback" % "logback-classic" % v)
  }

  def apacheCommonsDeps(v: String = "3.5"): Seq[ModuleID] = {
    Seq("org.apache.commons" % "commons-lang3" % v)
  }

  def commonsIo(v: String = "2.5"): Seq[ModuleID] = {
    Seq("commons-io" % "commons-io" % v)
  }

  def scalaParserCombinatorsDeps(scalaVersion: String): Seq[ModuleID] = CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, major)) if major >= 11 => Seq("org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6")
    case _ => Nil
  }

  def asyncDeps(v: String = "0.9.6"): Seq[ModuleID] ={
    Seq("org.scala-lang.modules" %% "scala-async" % v)
  }

  def playJson(v: String = "2.6.0-RC1"): Seq[ModuleID] = {
    Seq("com.typesafe.play" %% "play-json" % v)
  }


  def botCoreDeps: Seq[ModuleID] = {
    akkaDeps() ++ loggerDeps() ++ logbakcDeps() ++ configDeps() ++ injectDeps()
  }

  def botTelegramApiDeps: Seq[ModuleID] = {
    akkaDeps() ++ akkaHttp() ++ jacksonDeps() ++ loggerDeps() ++ logbakcDeps() ++ apacheCommonsDeps()
  }
}