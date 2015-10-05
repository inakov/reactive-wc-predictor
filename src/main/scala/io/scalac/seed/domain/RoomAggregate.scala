package io.scalac.seed.domain

import akka.actor.{Cancellable, Props}
import akka.persistence.SnapshotMetadata
import io.scalac.seed.domain.AggregateRoot._
import io.scalac.seed.domain.RoomAggregate.RoomStatusType.RoomStatusType
import org.joda.time.{Period, DateTime}

/**
 * Created by inakov on 15-10-5.
 */
object RoomAggregate {

  import AggregateRoot._

  object RoomStatusType extends Enumeration {
    type RoomStatusType = Value

    val  USABLE = Value(1, "USABLE")
    val DANGER = Value(2, "DANGER")
    val BAD = Value(3, "BAD")
    val OCCUPIED = Value(4, "OCCUPIED")
  }

  case class Room(id: String,
                  floorId: String,
                  name: String,
                  statusType: RoomStatusType,
                  lastStatusUpdate: Option[DateTime],
                  statusExpiration: Option[DateTime]) extends State

  case class Initialize(floorId: String, name: String) extends Command
  case class ChangeRoomStatus(statusType: RoomStatusType, statusExpiration: Option[DateTime]) extends Command

  case class RoomInitialized(floorId: String, name: String, statusType: RoomStatusType) extends Event
  case class RoomStatusChanged(statusType: RoomStatusType,
                               lastStatusUpdate: Option[DateTime],
                               statusExpiration: Option[DateTime]) extends Event
  case class RoomStatusExpired(roomId: String) extends Event
  case object RoomRemoved extends Event

  def props(id: String): Props = Props(new RoomAggregate(id))
}

class RoomAggregate(id: String) extends AggregateRoot {

  import RoomAggregate._
  import scala.concurrent.duration._

  override def persistenceId = id

  var statusExpirationSchedule: Option[Cancellable] = None

  /**
   * Updates internal processor state according to event that is to be applied.
   *
   * @param evt Event to apply
   */
  override def updateState(evt: Event): Unit = evt match {
    case RoomInitialized(floorId, name, statusType) =>
      context.become(created)
      state = Room(id, floorId, name, statusType, None, None)
    case RoomStatusChanged(status, lastUpdate, expiration) => state match {
      case s: Room =>
        statusExpirationSchedule = scheduleStatusExpiration(expiration, status)
        state = s.copy(statusType = status, lastStatusUpdate = lastUpdate, statusExpiration = expiration)
      case _ => //nothing
    }
    case RoomRemoved =>
      context.become(removed)
      state = Removed
  }

  def scheduleStatusExpiration(statusExpiration: Option[DateTime],
                               currentStatus: RoomStatusType): Option[Cancellable] = statusExpiration match {
    case Some(expirationTime) if expirationTime.isAfterNow =>
      val delay: Long = expirationTime.getMillis - DateTime.now.getMillis
      Some(context.system.scheduler.scheduleOnce(delay millis, context.parent, "test"))
    case _ => None
  }

  override def receiveCommand: Receive = initial

  val initial: Receive = {
    case Initialize(floorId, name) =>
      persist(RoomInitialized(floorId, name, RoomStatusType.USABLE))(afterEventPersisted)
    case GetState =>
      respond()
    case KillAggregate =>
      context.stop(self)
  }

  val created: Receive = {
    case ChangeRoomStatus(status, expiration) =>
      persist(RoomStatusChanged(status, Some(DateTime.now), expiration))(afterEventPersisted)
    case GetState =>
      respond()
    case KillAggregate =>
      context.stop(self)
  }

  val removed: Receive = {
    case GetState =>
      respond()
    case KillAggregate =>
      context.stop(self)
  }

  override protected def restoreFromSnapshot(metadata: SnapshotMetadata, state: State): Unit = ???
}
