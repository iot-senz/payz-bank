package actors

import actors.PingSender.InitPing
import actors.SenzReader.InitReader
import actors.SenzSender.SenzMsg
import akka.actor.{Actor, Props}
import config.Configuration
import org.slf4j.LoggerFactory
import protocols.SignatureVerificationFail

import scala.concurrent.duration._

object RegHandler {

  case class Reg(senzMsg: String)

  case class RegDone()

  case class RegFail()

  case class Registered()

  case class RegTimeout()

  def props(senzMsg: String): Props = Props(new RegHandler(senzMsg))

}

class RegHandler(senzMsg: String) extends Actor with Configuration {

  import RegHandler._
  import context._

  val senzSender = context.actorSelection("/user/SenzSender")
  val senzReader = context.actorSelection("/user/SenzReader")
  val pingSender = context.actorSelection("/user/PingSender")

  // scheduler to run on 5 seconds
  val regCancellable = system.scheduler.schedule(0 milliseconds, 4 seconds, self, Reg(senzMsg))

  // send timeout message after 12 seconds
  val timeoutCancellable = system.scheduler.scheduleOnce(10 seconds, self, RegTimeout)

  def logger = LoggerFactory.getLogger(this.getClass)

  override def preStart() = {
    logger.info("[_________START ACTOR__________] " + context.self.path)
  }

  override def receive: Receive = {
    case Reg(senz) =>
      logger.debug("Reg: " + senz)
      senzSender ! SenzMsg(senz)
    case RegDone =>
      logger.debug("RegDone")

      // cancel schedulers
      regCancellable.cancel()
      timeoutCancellable.cancel()

      // postReg
      onPostReg()

      // stop
      context.stop(self)
    case RegFail =>
      logger.error("RegFail")

      // cancel schedulers
      regCancellable.cancel()
      timeoutCancellable.cancel()

      // stop
      context.stop(self)
    case Registered =>
      logger.debug("Registered")

      // cancel scheduler
      regCancellable.cancel()
      timeoutCancellable.cancel()

      // postReg
      onPostReg()

      // stop
      context.stop(self)
    case SignatureVerificationFail =>
      logger.error("SignatureVerificationFail")

      // cancel scheduler
      regCancellable.cancel()
      timeoutCancellable.cancel()

      // stop
      context.stop(self)
    case RegTimeout =>
      logger.error("RegTimeout")

      // cancel scheduler
      regCancellable.cancel()
      timeoutCancellable.cancel()

      // stop
      context.stop(self)
  }

  def onPostReg() = {
    // start SenzReader
    senzReader ! InitReader

    // start PingSender
    pingSender ! InitPing
  }

}