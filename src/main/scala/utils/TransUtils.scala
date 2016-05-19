package utils

import protocols.{Matm, Senz, Trans}

import scala.util.Random
import scala.math.log10

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
    val fKey = getKey(4)
    val tKey = getKey(4)

    Trans(tId, fromAcc, toAcc, timestamp, amount, fKey, tKey, "PENDING")
  }

  def getMatm(senz: Senz): Matm = {
    val tId = senz.attributes.getOrElse("tid", "")
    val key = senz.attributes.getOrElse("key", "")
    val user = senz.sender

    Matm(tId, key, user)
  }

  private def getKey(id: Int): String = {
    val size = (log10(id) + 4).toInt
    Random.alphanumeric.take(Random.nextInt(size) + 1).mkString
  }

}

