package actors

import java.net.{DatagramPacket, DatagramSocket}

import akka.actor.{Actor, Props}
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
    logger.debug("Start actor: " + context.self.path)
  }

  override def receive: Receive = {
    case InitListener =>
      logger.debug("InitListener")

      val buf = new Array[Byte](1024)

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
