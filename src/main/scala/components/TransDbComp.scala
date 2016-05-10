package components

import protocols.{Trans, Account}


/**
 * Created by eranga on 2/2/16.
 */
trait TransDbComp {

  val transDb: TransDb

  trait TransDb {
    def createAgent(agent: Account)

    def getAgent(name: String): Option[Account]

    def createTrans(trans: Trans)

    def updateTrans(trans: Trans)

    def getTrans(agent: String, timestamp: String): Option[Trans]
  }

}
