package bot.api.i18n

import java.util.Locale

import scala.annotation.implicitNotFound

@implicitNotFound("Cant find implicit Lang in scope.")
case class Lang(locale: Locale) {
  def language = locale.getLanguage
  def country = locale.getCountry
}

object Lang {
  val Default = Lang(Locale.getDefault)

  def apply(language: String): Lang = Lang(new Locale(language))

  def apply(maybeLang: Option[String], default: Lang = Default): Lang = maybeLang.map(apply) getOrElse default
}