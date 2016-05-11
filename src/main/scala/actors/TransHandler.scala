package actors

import actors.SenzSender.SenzMsg
import akka.actor.{Actor, Props}
import config.Configuration
import db.TransDbComp
import org.slf4j.LoggerFactory
import protocols.Trans

import scala.concurrent.duration._

case class TransMsg(msgStream: Array[Byte])

case class TransResp(esh: String, status: String, rst: String)

case class TransTimeout()

trait TransHandlerComp {

  this: TransDbComp =>

  object TransHandler {
    def props(trans: Trans): Props = Props(new TransHandler(trans))
  }

  class TransHandler(trans: Trans) extends Actor with Configuration {

    import context._

    def logger = LoggerFactory.getLogger(this.getClass)

    // we need senz sender to send reply back
    val senzSender = context.actorSelection("/user/SenzSender")

    // handle timeout in 5 seconds
    val timeoutCancellable = system.scheduler.scheduleOnce(5 seconds, self, TransTimeout)

    override def preStart() = {
      logger.debug("Start actor: " + context.self.path)
    }

    override def receive: Receive = {
      case TransTimeout =>
        // timeout
        logger.error("TransTimeout")
        handleResponse("response")
    }

    def handleResponse(response: String) = {
      // update db
      // TODO update according to the status
      transDb.updateTrans(Trans(trans.from_account, trans.to_account, trans.amount, trans.timestamp, "DONE"))

      // send status back
      // TODO status according to the response
      val senz = s"DATA #msg PUTDONE @${trans.from_account} ^payzbank"
      senzSender ! SenzMsg(senz)
    }
  }

}
