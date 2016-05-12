package actors

import java.net.{DatagramPacket, DatagramSocket}

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, OneForOneStrategy, Props}
import db.{CassandraPayzDbComp, PayzCassandraCluster}
import handlers.SenzHandler
import org.slf4j.LoggerFactory
import utils.SenzParser

object SenzListener {

  case class InitListener()

  def props(socket: DatagramSocket): Props = Props(new SenzListener(socket))

}

class SenzListener(socket: DatagramSocket) extends Actor {

  import SenzListener._

  val senzHandler = new SenzHandler with CassandraPayzDbComp with PayzCassandraCluster

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
        senzHandler.Handler.handle(senz)
      }
  }
}
