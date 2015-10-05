import akka.actor.Actor.Receive
import akka.actor.{ActorLogging, Actor}
import io.scalac.seed.domain.RoomAggregate.{RoomStatusType, Room}
import io.scalac.seed.service.RoomAggregateManager
import io.scalac.seed.service.RoomAggregateManager.RegisterRoom

/**
 * Created by inakov on 15-10-5.
 */
class RoomTestActor extends Actor with ActorLogging{
  val roomAggregateManger = context.actorOf(RoomAggregateManager.props)

  import RoomAggregateManager._

  roomAggregateManger ! RegisterRoom("5", "left")
  roomAggregateManger ! RegisterRoom("5", "right")

  override def receive: Receive = {
    case room: Room if room.statusType == RoomStatusType.USABLE =>
      log.debug("Received: " + room)
      roomAggregateManger ! UpdateRoomStatus(room.id, RoomStatusType.OCCUPIED)
    case room: Room =>
      log.debug("Received: " + room)
  }
}
