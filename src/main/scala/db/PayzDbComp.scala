package db

import protocols.{Acc, Trans}


/**
 * Created by eranga on 2/2/16.
 */
trait PayzDbComp {

  val payzDb: PayzDb

  trait PayzDb {
    def createAcc(acc: Acc)

    def getAcc(name: String): Option[Acc]

    def createTrans(trans: Trans)

    def updateTransStatus(trans: Trans)

    def getTrans(tId: String): Option[Trans]

    def transferMoney(trans: Trans)
  }

}
