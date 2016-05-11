package db

import protocols.{Acc, Trans}


/**
 * Created by eranga on 2/2/16.
 */
trait TransDbComp {

  val transDb: TransDb

  trait TransDb {
    def createAcc(acc: Acc)

    def getAcc(name: String): Option[Acc]

    def createTrans(trans: Trans)

    def updateTrans(trans: Trans)

    def getTrans(from_acc: String, timestamp: String): Option[Trans]

    def transferMoney(trans: Trans)
  }

}
