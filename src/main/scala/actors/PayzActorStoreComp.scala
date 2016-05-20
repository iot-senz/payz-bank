package actors

import akka.actor.ActorRef

/**
 * Created by eranga on 5/20/16.
 */
trait PayzActorStoreComp extends ActorStoreComp {

  val actorStore = new PayzActorStore

  object PayzActorStore {
    val ActorRefs = scala.collection.mutable.Map[String, ActorRef]()
  }

  class PayzActorStore extends ActorStore {

    import PayzActorStore._

    override def getActor(id: String): Option[ActorRef] = {
      ActorRefs.get(id)
    }

    override def addActor(id: String, actorRef: ActorRef) = {
      ActorRefs.put(id, actorRef)
    }

    override def removeActor(id: String) {
      ActorRefs.remove(id)
    }
  }

}
