package io.scalac.seed.route

import akka.actor.ActorRef
import io.scalac.seed.domain.{RoomStatus, RoomAggregate}
import io.scalac.seed.route.RoomRoute.{ChangeRoomStatus, AddRoom}
import io.scalac.seed.service.AggregateManager
import io.scalac.seed.service.RoomAggregateManager.{UpdateRoomStatus, DeleteRoom, GetRoom, RegisterRoom}
import spray.httpx.Json4sSupport
import spray.routing._

/**
 * Created by inakov on 15-10-6.
 */

object RoomRoute{
  case class AddRoom(floorId: String, name: String)
  case class ChangeRoomStatus(status: RoomStatus)
}

trait RoomRoute extends HttpService with Json4sSupport with RequestHandlerCreator {

  val roomAggregateManager: ActorRef

  val roomRoute =
    path("room" / Segment){ id =>
      get{
        serveGet(GetRoom(id))
      } ~
      delete{
        serveDelete(DeleteRoom(id))
      } ~
      post{
        entity(as[ChangeRoomStatus]){ cmd =>
          serveUpdate(UpdateRoomStatus(id, cmd.status))
        }
      }
    } ~
    path("room"){
      post{
        entity(as[AddRoom]){ cmd =>
          serveAdd(RegisterRoom(cmd.floorId, cmd.name))
        }
      }
    }


  private def serveUpdate(message : AggregateManager.Command): Route =
    ctx => handleUpdate[RoomAggregate.Room](ctx, roomAggregateManager, message)

  private def serveAdd(message : AggregateManager.Command): Route =
    ctx => handleRegister[RoomAggregate.Room](ctx, roomAggregateManager, message)

  private def serveDelete(message : AggregateManager.Command): Route =
    ctx => handleDelete(ctx, roomAggregateManager, message)

  private def serveGet(message : AggregateManager.Command): Route =
    ctx => handleGet[RoomAggregate.Room](ctx, roomAggregateManager, message)
}
