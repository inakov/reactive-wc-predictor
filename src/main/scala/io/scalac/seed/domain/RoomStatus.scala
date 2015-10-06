package io.scalac.seed.domain

/**
 * Created by inakov on 15-10-6.
 */

sealed trait RoomStatus{
  def defaultExpiration: Option[Long]
  def fallbackStatus: Option[RoomStatus]
}
object RoomStatus{
  import scala.concurrent.duration._

  case object USABLE extends RoomStatus {
    override def defaultExpiration: Option[Long] = None

    override def fallbackStatus: Option[RoomStatus] = None
  }

  case object DANGER extends RoomStatus {
    override def defaultExpiration: Option[Long] = Option((10 minutes).toMillis)

    override def fallbackStatus: Option[RoomStatus] = Option(BAD)
  }

  case object BAD extends RoomStatus {
    override def defaultExpiration: Option[Long] = Option((5 minutes).toMillis)

    override def fallbackStatus: Option[RoomStatus] = Option(USABLE)
  }

  case object OCCUPIED extends RoomStatus {
    override def defaultExpiration: Option[Long] = Option((1 minutes).toMillis)

    override def fallbackStatus: Option[RoomStatus] = Option(USABLE)
  }

  val values: Set[RoomStatus] = Set(USABLE, OCCUPIED, BAD, DANGER)

  def apply(name: String): Option[RoomStatus] = values.find(_.toString == name)
}

