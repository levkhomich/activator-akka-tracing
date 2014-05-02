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

import java.util
import java.util.UUID
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Random

import akka.actor.{ActorSystem, Props, ActorRef, Actor}
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import com.github.levkhomich.akka.tracing.{ActorTracing, TracingSupport}
import com.typesafe.config.ConfigFactory

final case class ExternalRequest(headers: util.Map[String, String], payload: String) extends TracingSupport
final case class ExternalResponse(responseCode: Int, payload: String)
final case class InternalRequest(payload: String) extends TracingSupport
final case class InternalResponse(responseCode: Int, payload: String)

class RequestHandler extends Actor with ActorTracing {
  val child: ActorRef = context.actorOf(Props[DelegateActor])
  implicit val askTimeout: Timeout = 100.milliseconds

  override def receive: Receive = {
    case msg @ ExternalRequest(headers, payload) =>
      println("RequestHandler received " + msg)

      // sample request
      trace.sample(msg, this.getClass.getSimpleName)

      // add info about request headers to trace
      headers.foreach { case (k, v) => trace.recordKeyValue(msg, k, v)}

      // to trace child request correctly, mark it using asChildOf
      child ? InternalRequest(payload).asChildOf(msg) recover {
        case e: Exception =>
          // trace exception
          trace.record(msg, e)
          InternalResponse(500, "")
      } map {
        case InternalResponse(responseCode, resp) =>
          // close trace by marking response
          ExternalResponse(responseCode, resp + '!').asResponseTo(msg)
      } pipeTo sender
  }
}

class DelegateActor extends Actor with ActorTracing {
  override def receive: Receive = {
    case msg @ InternalRequest(payload) =>
      println("DelegateActor received " + msg)
      trace.sample(msg, this.getClass.getSimpleName)
      // another computation (sometimes leading to timeout processed by recover block above)
      Thread.sleep(Random.nextInt(200))
      sender ! InternalResponse(200, s"Hello, $payload").asResponseTo(msg)
  }
}

// create actor system and send messages every second
object TraceHierarchy extends App {
  implicit val askTimeout: Timeout = 500.milliseconds
  def random = UUID.randomUUID().toString
  val system = ActorSystem.create("TraceHierarchy", ConfigFactory.load("application"))
  val handler = system.actorOf(Props[RequestHandler])
  system.scheduler.schedule(3.seconds, 1.second) {
    handler ? ExternalRequest(Map("userAgent" -> random), random)
  }
  system.awaitTermination()
}