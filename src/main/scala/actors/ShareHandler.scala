package actors

import actors.SenzSender.SenzMsg
import akka.actor.{Actor, Props}
import org.slf4j.LoggerFactory
import protocols.SignatureVerificationFail

import scala.concurrent.duration._


object ShareHandler {

  case class Share(senzMsg: String)

  case class ShareDone()

  case class ShareFail()

  case class ShareTimeout()

  def props(senzMsg: String): Props = Props(new ShareHandler(senzMsg))

}

class ShareHandler(senzMsg: String) extends Actor {

  import ShareHandler._
  import context._

  def logger = LoggerFactory.getLogger(this.getClass)

  val senzSender = context.actorSelection("/user/SenzSender")

  // send regSenz in every 4 seconds
  val shareCancellable = system.scheduler.schedule(0 milliseconds, 4 seconds, self, Share(senzMsg))

  // send timeout message after 12 seconds
  val timeoutCancellable = system.scheduler.scheduleOnce(10 seconds, self, ShareTimeout)

  override def preStart = {
    logger.debug("Start actor: " + context.self.path)
  }

  override def receive: Receive = {
    case Share(senzMsg) =>
      logger.debug("SHARE received: " + senzMsg)

      context.setReceiveTimeout(30 milliseconds)
      senzSender ! SenzMsg(senzMsg)
    case ShareDone =>
      // success
      logger.debug("ShareDone")

      shareCancellable.cancel()
      timeoutCancellable.cancel()
      context.stop(self)
    case ShareFail =>
      // fail
      logger.error("ShareFail")

      shareCancellable.cancel()
      timeoutCancellable.cancel()
      context.stop(self)
    case SignatureVerificationFail =>
      logger.error("Signature verification fail")

      // cancel scheduler
      shareCancellable.cancel()
      timeoutCancellable.cancel()

      // stop the actor
      context.stop(self)
    case ShareTimeout =>
      logger.error("Timeout")

      // cancel scheduler
      shareCancellable.cancel()
      timeoutCancellable.cancel()

      // stop the actor
      context.stop(self)
  }
}