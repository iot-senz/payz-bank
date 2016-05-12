package actors

import java.net.{DatagramPacket, DatagramSocket, InetAddress}

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, OneForOneStrategy, Props}
import config.Configuration
import crypto.RSAUtils
import org.slf4j.LoggerFactory
import protocols.Senz
import utils.{SenzParser, SenzUtils}

object SenzSender {

  case class InitSender()

  case class SenzMsg(msg: String)

  def props(socket: DatagramSocket): Props = Props(new SenzSender(socket))

}

class SenzSender(socket: DatagramSocket) extends Actor with Configuration {

  import SenzSender._

  def logger = LoggerFactory.getLogger(this.getClass)

  override def preStart() = {
    logger.info("[_________START ACTOR__________] " + context.self.path)
  }

  override def supervisorStrategy = OneForOneStrategy() {
    case e: NullPointerException =>
      logger.error("Null pointer exception caught " + e)
      Restart
    case e: Exception =>
      logger.error("Exception caught " + e)
      Stop
  }

  override def receive: Receive = {
    case InitSender =>
      logger.info("InitSender")

      // start RegHandler in here
      val regSenzMsg = SenzUtils.getRegistrationSenzMsg
      context.actorOf(RegHandler.props(regSenzMsg), "RegHandler")
    case SenzMsg(msg) =>
      // sign senz
      val senzSignature = RSAUtils.signSenz(msg.trim.replaceAll(" ", ""))
      val signedSenz = s"$msg $senzSignature"

      logger.info("Senz: " + msg)
      logger.info("Signed senz: " + signedSenz)

      send(signedSenz)
    case senz: Senz =>

      // sign senz
      val msg = SenzParser.getSenzMsg(senz)
      val signature = RSAUtils.signSenz(msg.trim.replaceAll(" ", ""))
      val signedSenz = s"$msg $signature"

      logger.info(s"Senz: ${senz.senzType} ^${senz.sender} @${senz.receiver} ${senz.attributes} ")
      logger.info("Signed senz: " + signedSenz)

      send(signedSenz)
  }

  def send(msg: String) = {
    logger.info("Sending SenzMsg: " + msg)

    val senzOut = new DatagramPacket(msg.getBytes, msg.getBytes.length, InetAddress.getByName(switchHost), switchPort)
    socket.send(senzOut)
  }
}
