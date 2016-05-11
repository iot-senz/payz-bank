package actors

import akka.actor.SupervisorStrategy.{Stop, Restart}
import akka.actor.{OneForOneStrategy, Actor, Props}
import db.{CassandraTransDbComp, SenzCassandraCluster}
import exceptions.EmptySenzException
import org.slf4j.LoggerFactory
import utils.SenzUtils

object SenzReader {

  case class InitReader()

  def props(): Props = Props(new SenzReader())
}

class SenzReader extends Actor {

  import SenzReader._

  def logger = LoggerFactory.getLogger(this.getClass)

  override def preStart() = {
    logger.debug("Start actor: " + context.self.path)
  }

  override def supervisorStrategy = OneForOneStrategy() {
    case e: NullPointerException =>
      logger.error("Null pointer exception caught [RESTART]" + e)
      Restart
    case e: Exception =>
      logger.error("Exception caught, [STOP] " + e)
      Stop
  }

  override def receive: Receive = {
    case InitReader =>
      // listen for user inputs form commandline
      println()
      println()
      println("-----------------------------------------------")
      println("ENTER #SENZ[SHARE #acc #amnt @agent ^payzbank]")
      println("-----------------------------------------------")
      println()

      // read user input from the command line
      val inputSenz = scala.io.StdIn.readLine()

      logger.debug("Input Senz: " + inputSenz)

      // validate senz
      try {
        SenzUtils.isValidSenz(inputSenz)

        // handle share
        val shareHandlerComp = new ShareHandlerComp with CassandraTransDbComp with SenzCassandraCluster
        context.actorOf(shareHandlerComp.ShareHandler.props(inputSenz))
      } catch {
        case e: EmptySenzException =>
          logger.error("Empty senz")
          println("[ERROR: EMPTY SENZ]")
          self ! InitReader
        case e: Exception =>
          logger.error("Invalid senz", e)
          println("[ERROR: INVALID SENZ]")
          self ! InitReader
      }
  }
}