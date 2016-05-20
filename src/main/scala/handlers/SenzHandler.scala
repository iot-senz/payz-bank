package handlers

import actors.RegHandler.{RegDone, RegFail, Registered}
import actors._
import akka.actor.ActorContext
import db.{CassandraPayzDbComp, PayzCassandraCluster, PayzDbComp}
import org.slf4j.LoggerFactory
import protocols.{Senz, SenzType, SignatureVerificationFail}
import utils.TransUtils

class SenzHandler {
  this: PayzDbComp with PayzActorStoreComp =>

  def logger = LoggerFactory.getLogger(this.getClass)

  object Handler {

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
      //    1. first Trans PUT
      //    2. second Matm PUT
      if (senz.attributes.contains("acc") && senz.attributes.contains("amnt")) {
        // first Trans PUT
        // create Trans form senz
        val trans = TransUtils.getTrans(senz)

        actorStore.getActor(trans.tId) match {
          case Some(actorRef) =>
            // have actor to handle the trans with this id
            actorRef ! trans
          case _ =>
            // no matching actor to handle the trans, so create actor
            val transHandlerComp = new TransHandlerComp with CassandraPayzDbComp with PayzCassandraCluster with PayzActorStoreComp
            val actorRef = context.actorOf(transHandlerComp.TransHandler.props(trans))

            // store actor in map
            actorStore.addActor(trans.tId, actorRef)

            // send trans to created actor
            actorRef ! trans
        }
      } else if (senz.attributes.contains("key") && senz.attributes.contains("tid")) {
        // second Matm PUT
        // create Matm from senz
        val matm = TransUtils.getMatm(senz)

        // send Matm to actor
        val actorRef = actorStore.getActor(matm.tId)
        actorRef.get ! matm
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
