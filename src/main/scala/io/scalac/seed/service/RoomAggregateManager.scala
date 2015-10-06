package io.scalac.seed.service

import java.util.UUID

import akka.actor.Props
import io.scalac.seed.domain.AggregateRoot.{Remove, GetState}
import io.scalac.seed.domain.RoomStatus.USABLE
import io.scalac.seed.domain.{RoomStatus, RoomAggregate}
import io.scalac.seed.domain.RoomAggregate.{ChangeRoomStatus, Initialize}
import io.scalac.seed.service.RoomAggregateManager._
import org.joda.time._

/**
 * Created by inakov on 15-10-5.
 */
object RoomAggregateManager {

  import AggregateManager._

  case class RegisterRoom(floorId: String, name: String) extends Command
  case class DeleteRoom(id: String) extends Command
  case class GetRoom(id: String) extends Command
  case class UpdateRoomStatus(roomId: String, status: RoomStatus) extends Command
  case class ExpireRoomStatus(roomId: String, currentStatus: RoomStatus) extends Command

  def props = Props(new RoomAggregateManager)
}

class RoomAggregateManager extends AggregateManager{
  /**
   * Processes command.
   * In most cases it should transform message to appropriate aggregate command (and apply some additional logic if
   * needed) and call [[AggregateManager.processAggregateCommand]]
   *
   */
  override def processCommand: Receive = {
    case RegisterRoom(floorId, name) =>
      val id = UUID.randomUUID().toString()
      processAggregateCommand(id, Initialize(floorId, name))
    case UpdateRoomStatus(id, status) =>
      processAggregateCommand(id, ChangeRoomStatus(status))
    case GetRoom(id) =>
      processAggregateCommand(id, GetState)
    case DeleteRoom(id) =>
      processAggregateCommand(id, Remove)
    case ExpireRoomStatus(id, currentStatus) =>
      val nextStatus = currentStatus.fallbackStatus.getOrElse(USABLE)
      processAggregateCommand(id, ChangeRoomStatus(nextStatus))
  }

  /**
   * Returns Props used to create an aggregate with specified id
   *
   * @param id Aggregate id
   * @return Props to create aggregate
   */
  override def aggregateProps(id: String): Props = RoomAggregate.props(id)
}
