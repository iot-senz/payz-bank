package utils

import protocols.{Matm, Senz, Trans}

object TransUtils {
  def getTrans(senz: Senz): Trans = {
    val fromAcc = senz.sender
    val toAcc = senz.attributes.getOrElse("acc", "")
    val amount = senz.attributes.getOrElse("amnt", "").toInt
    val timestamp = senz.attributes.getOrElse("time", "")

    // unique trans id
    val tId = s"$fromAcc$toAcc$timestamp"

    // generate two random no's
    // this requires when handing trans with MATM protocol
    val fKey = randomKey(5)
    val tKey = randomKey(5)

    Trans(tId, fromAcc, toAcc, timestamp, amount, fKey, tKey, "INIT")
  }

  def getMatm(senz: Senz): Matm = {
    val tId = senz.attributes.getOrElse("tid", "")
    val key = senz.attributes.getOrElse("key", "")
    val user = senz.sender

    Matm(tId, key, user)
  }

  private def randomKey(length: Int) = {
    scala.util.Random.alphanumeric.take(length).mkString
  }

}

