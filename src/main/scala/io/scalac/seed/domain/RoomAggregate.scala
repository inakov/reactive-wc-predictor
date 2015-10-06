package io.scalac.seed.domain

import akka.actor.{Cancellable, Props}
import akka.persistence.SnapshotMetadata
import io.scalac.seed.domain.AggregateRoot._
import io.scalac.seed.domain.RoomStatus.USABLE
import io.scalac.seed.service.RoomAggregateManager.ExpireRoomStatus
import org.joda.time.DateTime

/**
 * Created by inakov on 15-10-5.
 */
object RoomAggregate {

  import AggregateRoot._

  case class Room(id: String,
                  floorId: String,
                  name: String,
                  status: RoomStatus,
                  lastStatusUpdate: Option[DateTime],
                  statusExpiration: Option[DateTime]) extends State

  case class Initialize(floorId: String, name: String) extends Command
  case class ChangeRoomStatus(status: RoomStatus) extends Command

  case class RoomInitialized(floorId: String, name: String, statusType: RoomStatus) extends Event
  case class RoomStatusChanged(status: RoomStatus,
                               lastStatusUpdate: Option[DateTime],
                               statusExpiration: Option[DateTime]) extends Event
  case object RoomRemoved extends Event

  def props(id: String): Props = Props(new RoomAggregate(id))
}

class RoomAggregate(id: String) extends AggregateRoot {

  import RoomAggregate._

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
    case RoomStatusChanged(statusType, lastUpdate, expiration) => state match {
      case s: Room =>
        cancelStatusExpiration
        statusExpirationSchedule = scheduleStatusExpiration(expiration, statusType)
        state = s.copy(status = statusType, lastStatusUpdate = lastUpdate, statusExpiration = expiration)
      case _ => //nothing
    }
    case RoomRemoved =>
      context.become(removed)
      cancelStatusExpiration
      state = Removed
  }

  def cancelStatusExpiration: Unit = statusExpirationSchedule match {
    case Some(schedule) if !schedule.isCancelled =>
      schedule.cancel
      statusExpirationSchedule = None
    case _ =>
      statusExpirationSchedule = None
  }

  def scheduleStatusExpiration(statusExpiration: Option[DateTime],
                               currentStatus: RoomStatus): Option[Cancellable] = statusExpiration match {
    case Some(expirationTime) if expirationTime.isAfterNow =>
      import scala.concurrent.duration._
      import context.dispatcher
      val delay = (expirationTime.getMillis - DateTime.now.getMillis).millis
      Some(context.system.scheduler.scheduleOnce(delay, context.parent, ExpireRoomStatus(id, currentStatus)))
    case _ => None
  }

  override def receiveCommand: Receive = initial

  val initial: Receive = {
    case Initialize(floorId, name) =>
      persist(RoomInitialized(floorId, name, USABLE))(afterEventPersisted)
    case GetState =>
      respond()
    case KillAggregate =>
      context.stop(self)
  }

  val created: Receive = {
    case ChangeRoomStatus(status) =>
      val lastStatusUpdate = Some(DateTime.now)
      val statusExpiration = calculateStatusExpiration(status, lastStatusUpdate)
      persist(RoomStatusChanged(status, lastStatusUpdate, statusExpiration))(afterEventPersisted)
    case GetState =>
      respond()
    case Remove =>
      persist(RoomRemoved)(afterEventPersisted)
    case KillAggregate =>
      context.stop(self)
  }

  private def calculateStatusExpiration(status: RoomStatus, lastStatusUpdate: Option[DateTime]) =
    status.defaultExpiration match {
      case Some(delay) => Some(lastStatusUpdate.getOrElse(DateTime.now).plus(delay))
      case _=> None
    }

  val removed: Receive = {
    case GetState =>
      respond()
    case KillAggregate =>
      context.stop(self)
  }

  override protected def restoreFromSnapshot(metadata: SnapshotMetadata, state: State): Unit = {
    this.state = state
    state match {
      case Uninitialized => context become initial
      case Removed => context become removed
      case _: Room => context become created
    }
  }
}
