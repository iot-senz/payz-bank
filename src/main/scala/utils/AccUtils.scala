package utils

import protocols.{AccType, Acc, Senz}

/**
 * Created by eranga on 5/14/16.
 */
object AccUtils {

  def getAcc(senz: Senz): Acc = {
    val name = senz.sender
    val balance = 0
    val accType = AccType.withName(senz.attributes.getOrElse("type", "user"))

    Acc(name, balance, accType)
  }
}
