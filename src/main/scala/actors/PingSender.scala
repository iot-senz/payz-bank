package actors

import actors.SenzSender.SenzMsg
import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{OneForOneStrategy, Actor, Props}
import org.slf4j.LoggerFactory
import utils.SenzUtils

import scala.concurrent.duration._


object PingSender {

  case class InitPing()

  case class Ping()

  def props(): Props = Props(new PingSender())

}

class PingSender extends Actor {

  import PingSender._
  import context._

  val senzSender = context.actorSelection("/user/SenzSender")

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
    case InitPing =>
      logger.debug("InitPing")

      // start scheduler to PING on every 10 seconds
      system.scheduler.schedule(0 milliseconds, 10 minutes, self, Ping)

    case Ping =>
      logger.debug("PING")

      // send ping via sender
      val pingMsg = SenzUtils.getPingSenzMsg
      senzSender ! SenzMsg(pingMsg)
  }
}