package db

import protocols.{Account, Trans}


/**
 * Created by eranga on 2/2/16.
 */
trait TransDbComp {

  val transDb: TransDb

  trait TransDb {
    def createAccount(account: Account)

    def getAccount(name: String): Option[Account]

    def transferMoney(trans: Trans)

    def createTrans(trans: Trans)

    def updateTrans(trans: Trans)

    def getTrans(agent: String, timestamp: String): Option[Trans]
  }

}
