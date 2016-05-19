package handlers

import actors.RegHandler.{RegDone, RegFail, Registered}
import actors._
import akka.actor.{ActorRef, ActorContext}
import db.{CassandraPayzDbComp, PayzDbComp, PayzCassandraCluster}
import org.slf4j.LoggerFactory
import protocols.{Senz, SenzType, SignatureVerificationFail}
import utils.TransUtils

class SenzHandler {
  this: PayzDbComp =>

  def logger = LoggerFactory.getLogger(this.getClass)

  object Handler {

    val actorRefs = scala.collection.mutable.Map[String, ActorRef]()

    def handle(senz: Senz)(implicit context: ActorContext) = {
      senz match {
        case Senz(SenzType.GET, sender, receiver, attr, signature) =>
          logger.debug(s"GET senz: @$sender ^$receiver")

          val senz = Senz(SenzType.GET, sender, receiver, attr, signature)
          handleGet(senz)
        case Senz(SenzType.SHARE, sender, receiver, attr, signature) =>
          logger.debug(s"SHARE senz: @$sender ^$receiver")

          val senz = Senz(SenzType.SHARE, sender, receiver, attr, signature)
          handlerShare(senz)
        case Senz(SenzType.PUT, sender, receiver, attr, signature) =>
          logger.debug(s"PUT senz: @$sender ^$receiver")

          val senz = Senz(SenzType.PUT, sender, receiver, attr, signature)
          handlePut(senz)
        case Senz(SenzType.DATA, sender, receiver, attr, signature) =>
          logger.debug(s"DATA senz: @$sender ^$receiver")

          val senz = Senz(SenzType.DATA, sender, receiver, attr, signature)
          handleData(senz)
        case Senz(SenzType.PING, _, _, _, _) =>
          logger.debug(s"PING senz")
      }
    }

    def handleGet(senz: Senz)(implicit context: ActorContext) = {
      // save in database

      // send trans request to epic
    }

    def handlerShare(senz: Senz)(implicit context: ActorContext) = {
      // nothing to do with share
    }

    def handlePut(senz: Senz)(implicit context: ActorContext) = {
      // match for attr to check weather PUT is for
      //    1. first trans PUT
      //    2. second random key PUT
      if (senz.attributes.contains("acc") && senz.attributes.contains("amnt")) {
        // first trans PUT
        // create trans form senz
        val trans = TransUtils.getTrans(senz)

        // check trans exists
        transDb.getTrans(trans.tId) match {
          case Some(existingTrans) =>
            // already existing trans
            logger.debug("Trans exists, no need to recreate: " + "[" + existingTrans.fromAcc + ", " + existingTrans.toAcc + ", " + existingTrans.amount + "]")
          case None =>
            // new trans, so create and process it
            logger.debug("New Trans, process it: " + "[" + trans.fromAcc + ", " + trans.toAcc + ", " + trans.amount + "]")

            // transaction request via trans actor
            val transHandlerComp = new TransHandlerComp with CassandraPayzDbComp with PayzCassandraCluster
            val actorRef = context.actorOf(transHandlerComp.TransHandler.props(trans))

            // store actor in map
            actorRefs(trans.tId) = actorRef
        }
      } else if (senz.attributes.contains("key") && senz.attributes.contains("tid")) {
        // second random key PUT
        // create Matm from senz
        val matm = TransUtils.getMatm(senz)

        // send Matm to actor
        val actorRef = actorRefs(matm.tId)
        actorRef ! matm
      }

    }

    def handleData(senz: Senz)(implicit context: ActorContext) = {
      val regActor = context.actorSelection("/user/SenzSender/RegHandler")
      val agentRegActor = context.actorSelection("/user/SenzReader/*")

      senz.attributes.get("msg") match {
        case Some("ShareDone") =>
          agentRegActor ! ShareDone
        case Some("ShareFail") =>
          agentRegActor ! ShareFail
        case Some("REGISTRATION_DONE") =>
          regActor ! RegDone
        case Some("REGISTRATION_FAIL") =>
          regActor ! RegFail
        case Some("ALREADY_REGISTERED") =>
          regActor ! Registered
        case Some("SignatureVerificationFailed") =>
          context.actorSelection("/user/Senz*") ! SignatureVerificationFail
        case other =>
          logger.error("UNSUPPORTED DATA message " + other)
      }
    }
  }

}
