package bot.api

import info.mukel.telegrambot4s.methods.ApiRequest

package object mvc {
  type Result = ApiRequest[_]
  type SeqResult = Seq[ApiRequest[_]]
}
