package db

import protocols.{Acc, Trans}


/**
 * Created by eranga on 2/2/16.
 */
trait TransDbComp {

  val transDb: TransDb

  trait TransDb {
    def createAccount(account: Acc)

    def getAccount(name: String): Option[Acc]

    def transferMoney(trans: Trans)

    def createTrans(trans: Trans)

    def updateTrans(trans: Trans)

    def getTrans(agent: String, timestamp: String): Option[Trans]
  }

}
