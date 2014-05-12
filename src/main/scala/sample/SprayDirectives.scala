/**
 * Copyright 2014 the Akka Tracing contributors. See AUTHORS for more details.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample

import akka.actor.{ActorLogging, Actor, Props, ActorSystem}
import akka.io.IO
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random
import spray.can.Http
import spray.client.pipelining._
import spray.http._
import spray.httpx.marshalling.Marshaller
import spray.httpx.unmarshalling._
import spray.routing.HttpService

import com.github.levkhomich.akka.tracing.{ActorTracing, TracingSupport}
import com.github.levkhomich.akka.tracing.http.unmarshalling._
import com.github.levkhomich.akka.tracing.http.TracingDirectives
import java.util.UUID

case class BasicRequest(data: String) extends TracingSupport

object BasicRequest {
  // spray unmarshaller
  implicit val basicRequestUnmarshaller =
    Unmarshaller[BasicRequest](ContentTypeRange.*) {
      case HttpEntity.NonEmpty(_, data) => BasicRequest(data.asString)
      case HttpEntity.Empty => BasicRequest("")
    }
  // spray marshaller
  implicit val basicRequestMarshaller =
    Marshaller.of[BasicRequest](ContentTypes.`text/plain`) { case (BasicRequest(data), _, ctx) =>
      ctx.marshalTo(HttpEntity(data))
    }

  // helper which provides unmarshaller that initializes message by tracing params passed via request headers
  // regular unmarshaller for message should be provided implicitly
  implicit val RootRequestUnmarshallerWithTracingSupport = unmarshallerWithTracingSupport[BasicRequest]
}

class SprayDirectivesServiceActor extends Actor with ActorTracing with HttpService with TracingDirectives with ActorLogging {

  import SprayDirectives._

  implicit def executionContext = actorRefFactory.dispatcher

  def actorRefFactory = context
  def receive = runRoute(route)

  def process(r: BasicRequest): Future[BasicRequest] =
    Future {
      Thread.sleep(Random.nextInt(500) + 500)
      trace.recordKeyValue(r, "data", r.data)
      BasicRequest(r.data.reverse)
    }

  val route = {
    post {
      path(unmarshallingPath) {
        // this case uses spray directives with custom unmarshalling, which allows 
        // to continue traces passed from outsied (from frontend for example)
        entity(as[BasicRequest]) { request =>
          // but you still need to sample
          trace.sample(request, "spray-directives-service")

          val result = process(request)

          // and mark responses
          result.onComplete(response => response.asResponseTo(request))

          complete(result)
        }
      } ~
      path(tracedHandleWithPath) {
        // works like regular handleWith, but samples request and marks response automatically
        tracedHandleWith {
          process
        }
      } ~
      path(tracedCompletePath) {
        entity(as[BasicRequest]) { request =>
          // works like regular complete, but samples request and marks response automatically
          tracedComplete("BasicRequest")(process(request))
        }
      }
    }
  }
}

class ClientActor extends Actor {
  import context.dispatcher
  import SprayDirectives._

  // pass tracing headers
  def pipeline =
    addHeader("X-B3-TraceId", Random.nextLong().toString) ~>
    addHeader("X-B3-SpanId", Random.nextLong().toString) ~>
    sendReceive ~>
    unmarshal[BasicRequest]

  context.system.scheduler.schedule(3.seconds, 1.second) {
    // call random url each second
    self ! s"http://$host:$port/" + Random.shuffle(paths).head
  }

  override def receive: Receive = {
    // forward and log all messages
    case url: String =>
      val msg = BasicRequest(UUID.randomUUID().toString)
      pipeline(Post(url, msg))
      println(s"Sent $msg to $url")
  }
}

object SprayDirectives extends App {
  val host = "localhost"
  val port = 8889
  val unmarshallingPath = "unmarshalling"
  val tracedHandleWithPath = "tracedHandleWith"
  val tracedCompletePath = "tracedCompleteWith"
  val paths = List(unmarshallingPath, tracedHandleWithPath, tracedCompletePath)

  implicit val system = ActorSystem("SprayDirectives")
  val service = system.actorOf(Props[SprayDirectivesServiceActor], "spray-directives-service")
  IO(Http) ! Http.Bind(service, host, port)
//  system.actorOf(Props[ClientActor])
  system.awaitTermination()
}
