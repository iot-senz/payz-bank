package actors

import actors.SenzSender.SenzMsg
import akka.actor.{Actor, Props}
import config.Configuration
import components.{ActorStoreComp, PayzDbComp}
import org.slf4j.LoggerFactory
import protocols.{Matm, Trans}

import scala.concurrent.duration._

case class TransTimeout()

trait TransHandlerComp {

  this: PayzDbComp with ActorStoreComp =>

  object TransHandler {
    def props(trans: Trans): Props = Props(new TransHandler(trans))
  }

  class TransHandler(trans: Trans) extends Actor with Configuration {

    import context._

    def logger = LoggerFactory.getLogger(this.getClass)

    // we need senz sender to send reply back
    val senzSender = context.actorSelection("/user/SenzSender")

    // send message to self in order to init trans
    val transCancellable = system.scheduler.scheduleOnce(0 seconds, self, trans)

    // handle timeout in 5 seconds
    val timeoutCancellable = system.scheduler.scheduleOnce(40 seconds, self, TransTimeout)

    override def preStart() = {
      logger.info("[_________START ACTOR__________] " + context.self.path)
    }

    override def receive: Receive = {
      case trans: Trans =>
        logger.info("Handle trans: [" + trans.fromAcc + "] [" + trans.toAcc + "] [" + trans.amount + "]")

        // check trans exists
        payzDb.getTrans(trans.tId) match {
          case Some(existingTrans) =>
            // already existing trans
            logger.debug("Trans exists, no need to recreate: " + "[" + existingTrans.fromAcc + ", " + existingTrans.toAcc + ", " + existingTrans.amount + "]")
          case None =>
            // new trans, so create and process it
            logger.debug("New Trans, process it: " + "[" + trans.fromAcc + ", " + trans.toAcc + ", " + trans.amount + "]")

            // create trans in db
            payzDb.createTrans(trans)

            // send trans response
            sendTransResponse(trans)
        }
      case matm: Matm =>
        logger.info(s"Handle matm: [${matm.tId} ][${matm.acc}] [${matm.key}]")

        matm.acc match {
          case trans.fromAcc =>
            logger.info(s"Handle matm sends by USER ${matm.acc}")

            // send by user
            if (matm.key == trans.tKey) {
              // valid key exchange
              val status = processMatm(matm)
              sendMatmResponse(status, trans)
            }
          case trans.toAcc =>
            logger.info(s"Handle matm sends by SHOP ${matm.acc}")

            // send by shop
            if (matm.key == trans.fKey) {
              // valid key exchange
              val status = processMatm(matm)
              sendMatmResponse(status, trans)
            }
        }
      case TransTimeout =>
        // timeout
        logger.error("TransTimeout")

        // TODO may be send status back

        // remove actorRef from map
        actorStore.removeActor(trans.tId)
    }

    def sendTransResponse(trans: Trans) = {
      senzSender ! SenzMsg(s"DATA #tid ${trans.tId} #key ${trans.fKey} @${trans.fromAcc} ^payzbank")
      senzSender ! SenzMsg(s"DATA #tid ${trans.tId} #key ${trans.tKey} @${trans.toAcc} ^payzbank")
    }

    def processMatm(matm: Matm): Option[String] = {
      val trans = payzDb.getTrans(matm.tId)
      trans match {
        case Some(Trans(tId, fromAcc, toAcc, timestamp, amount, fKey, tKey, "INIT")) =>
          // INIT stage
          // update to PENDING
          payzDb.updateTransStatus(Trans(tId, fromAcc, toAcc, timestamp, amount, fKey, tKey, "PENDING"))
          Some("PENDING")
        case Some(Trans(tId, fromAcc, toAcc, timestamp, amount, fKey, tKey, "PENDING")) =>
          // PENDING state
          // update to DONE
          payzDb.updateTransStatus(Trans(tId, fromAcc, toAcc, timestamp, amount, fKey, tKey, "DONE"))
          Some("DONE")
        case _ =>
          None
      }
    }

    def sendMatmResponse(status: Option[String], trans: Trans) = {
      status match {
        case Some("DONE") =>
          // transfer money now
          // transfer money is error prone
          try {
            payzDb.transferMoney(trans)
            senzSender ! SenzMsg(s"DATA #msg DONE @${trans.fromAcc} ^payzbank")
            senzSender ! SenzMsg(s"DATA #msg DONE @${trans.toAcc} ^payzbank")
          } catch {
            case ex: Exception =>
              logger.error("Fail to money transfer " + ex)
              senzSender ! SenzMsg(s"DATA #msg FAIL @${trans.fromAcc} ^payzbank")
              senzSender ! SenzMsg(s"DATA #msg FAIL @${trans.toAcc} ^payzbank")
          }
        case _ =>
        // nothing to do
      }
    }
  }

}
