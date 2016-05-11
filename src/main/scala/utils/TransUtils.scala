package utils

import protocols.{Senz, Trans}

object TransUtils {
  def getTrans(senz: Senz): Trans = {
    val from_acc = senz.sender
    val to_acc = senz.attributes.getOrElse("user", "")
    val amount = senz.attributes.getOrElse("amnt", "").toInt
    val timestamp = senz.attributes.getOrElse("time", "")

    Trans(from_acc, to_acc, amount, timestamp, "PENDING")
  }

}

