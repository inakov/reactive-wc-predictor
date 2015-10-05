import akka.actor.{Props, ActorSystem}
import io.scalac.seed.service.RoomAggregateManager

/**
 * Created by inakov on 15-10-5.
 */
object Main extends App{
  implicit val system = ActorSystem("main-actor-system")
  implicit val executionContext = system.dispatcher

  system.actorOf(Props(new RoomTestActor()), "room-test")
}
