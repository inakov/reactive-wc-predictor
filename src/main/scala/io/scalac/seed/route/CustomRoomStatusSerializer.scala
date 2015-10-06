package io.scalac.seed.route

import io.scalac.seed.domain.RoomStatus
import org.joda.time.DateTime
import org.json4s.CustomSerializer
import org.json4s.JsonAST.{JInt, JString}

/**
 * Created by inakov on 15-10-6.
 */

object CustomSerializers {
  val all = List(CustomRoomStatusSerializer, CustomDateTimeSerializer)
}

case object CustomRoomStatusSerializer extends CustomSerializer[RoomStatus](format =>
  ({
    case JString(status) if RoomStatus(status).isDefined => RoomStatus(status).get
  },
    {
      case roomStatus: RoomStatus => JString(roomStatus.toString)
    }))

case object CustomDateTimeSerializer extends CustomSerializer[DateTime](format =>
  ({
    case JInt(timestamp) => new DateTime(timestamp)
  },
    {
      case date: DateTime => JInt(date.getMillis)
    }))
