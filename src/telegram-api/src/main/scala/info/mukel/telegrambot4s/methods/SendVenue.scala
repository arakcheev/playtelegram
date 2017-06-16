package info.mukel.telegrambot4s.methods

import info.mukel.telegrambot4s.models.{Message, ReplyMarkup}

/** Use this method to send information about a venue. On success, the sent Message is returned.
  *
  * @param chatId               Integer or String Unique identifier for the target chat or username of the target channel (in the format @channelusername)
  * @param latitude             Float number Latitude of the venue
  * @param longitude            Float number Longitude of the venue
  * @param title                String Name of the venue
  * @param address              String Address of the venue
  * @param foursquareId         String Optional Foursquare identifier of the venue
  * @param disableNotification  Boolean Optional Sends the message silently. iOS users will not receive a notification, Android users will receive a notification with no sound.
  * @param replyToMessageId     Integer Optional If the message is a reply, ID of the original message
  * @param replyMarkup          InlineKeyboardMarkup or ReplyKeyboardMarkup or ReplyKeyboardHide or ForceReply Optional Additional interface options. A JSON-serialized object for an inline keyboard, custom reply keyboard, instructions to hide reply keyboard or to force a reply from the user.
  */
case class SendVenue(
                    chatId              : Long Either String,
                    latitude            : Double,
                    longitude           : Double,
                    title               : String,
                    address             : String,
                    foursquareId        : Option[String] = None,
                    duration            : Option[String] = None,
                    disableNotification : Option[Boolean] = None,
                    replyToMessageId    : Option[Long] = None,
                    replyMarkup         : Option[ReplyMarkup] = None
                  ) extends ApiRequestJson[Message]
