package actors

import actors.SenzSender.SenzMsg
import akka.actor.{Actor, Props}
import config.Configuration
import db.PayzDbComp
import org.slf4j.LoggerFactory
import protocols.Trans

import scala.concurrent.duration._

case class InitTrans(trans: Trans)

case class TransTimeout()

trait TransHandlerComp {

  this: PayzDbComp =>

  object TransHandler {
    def props(trans: Trans): Props = Props(new TransHandler(trans))
  }

  class TransHandler(trans: Trans) extends Actor with Configuration {

    import context._

    def logger = LoggerFactory.getLogger(this.getClass)

    // we need senz sender to send reply back
    val senzSender = context.actorSelection("/user/SenzSender")

    // send message to self in order to init trans
    val transCancellable = system.scheduler.scheduleOnce(0 seconds, self, InitTrans(trans))

    // handle timeout in 5 seconds
    //val timeoutCancellable = system.scheduler.scheduleOnce(5 seconds, self, TransTimeout)

    override def preStart() = {
      logger.info("[_________START ACTOR__________] " + context.self.path)
    }

    override def receive: Receive = {
      case InitTrans(trans) =>
        logger.info("InitTrans: [" + trans.from_acc + "] [" + trans.to_acc + "] [" + trans.amount + "]")

        // TODO handle according to MATM protocol

        // create trans in db
        transDb.createTrans(trans)

        // transfer money is error prone
        try {
          transDb.transferMoney(trans)
          sendResponse("PUTDONE")
        } catch {
          case ex: Exception =>
            logger.error("Fail to money transfer " + ex)
            sendResponse("PUTFAIL")
        }
      case TransTimeout =>
        // timeout
        logger.error("TransTimeout")
    }

    def sendResponse(status: String) = {
      // send status back
      val senz = s"DATA #msg $status @${trans.from_acc} ^payzbank"
      senzSender ! SenzMsg(senz)
    }
  }

}
