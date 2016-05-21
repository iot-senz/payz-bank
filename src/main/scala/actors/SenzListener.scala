package actors

import java.net.{DatagramPacket, DatagramSocket}

import actors.RegHandler.{RegDone, RegFail, Registered}
import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, OneForOneStrategy, Props}
import db.{CassandraPayzDbComp, PayzCassandraCluster}
import org.slf4j.LoggerFactory
import protocols.{Senz, SenzType, SignatureVerificationFail}
import utils.{SenzParser, TransUtils}

case class InitListener()

trait SenzListenerComp {

  this: ActorStoreComp =>

  object SenzListener {

    def props(socket: DatagramSocket): Props = Props(new SenzListener(socket))

  }

  class SenzListener(socket: DatagramSocket) extends Actor {

    def logger = LoggerFactory.getLogger(this.getClass)

    override def preStart() = {
      logger.info("[_________START ACTOR__________] " + context.self.path)
    }

    override def supervisorStrategy = OneForOneStrategy() {
      case e: NullPointerException =>
        logger.error("Null pointer exception caught " + e)
        Restart
      case e: Exception =>
        logger.error("Exception caught, [STOP] " + e)
        Stop
    }

    override def receive: Receive = {
      case InitListener =>
        logger.debug("InitListener")

        val buf = new Array[Byte](1024)

        // TODO remove while loop and handle with Akka UDP
        // listen for udp socket in order to receive messages
        while (true) {
          // receiving packet
          val senzIn = new DatagramPacket(buf, buf.length)
          socket.receive(senzIn)
          val msg = new String(senzIn.getData, 0, senzIn.getLength)

          logger.debug("Senz received: " + msg)

          // handle received senz
          // parse senz first
          val senz = SenzParser.getSenz(msg)
          handleSenz(senz)
        }
    }

    def handleSenz(senz: Senz) = {
      senz match {
        case Senz(SenzType.GET, sender, receiver, attr, signature) =>
          logger.debug(s"GET senz: @$sender ^$receiver")
        case Senz(SenzType.SHARE, sender, receiver, attr, signature) =>
          logger.debug(s"SHARE senz: @$sender ^$receiver")
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

    def handlePut(senz: Senz) = {
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

    def handleData(senz: Senz) = {
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
