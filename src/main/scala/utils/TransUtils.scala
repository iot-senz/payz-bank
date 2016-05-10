package utils

import protocols.{Senz, Trans}

object TransUtils {
  def getTrans(senz: Senz): Trans = {
    val from_account = senz.sender
    val to_account = senz.attributes.getOrElse("acc", "")
    val amnt = senz.attributes.getOrElse("amnt", "").toInt
    val timestamp = senz.attributes.getOrElse("time", "")

    Trans(from_account, to_account, amnt, timestamp, "PENDING")
  }

}

