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

import akka.actor.{ActorSystem, Props, Actor}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Random

import com.github.levkhomich.akka.tracing.{TracingActorLogging, ActorTracing, TracingSupport}

final case class InternalMessage(str: String, int: Integer) extends TracingSupport

class BasicHandler extends Actor
    // mix to add tracing support
    with ActorTracing
    // mix to add trace logging support (and add trace logger to typesafe config)
    with TracingActorLogging {

  override def receive: Receive = {
    case msg @ InternalMessage(str, int) =>
      // sample message
      trace.sample(msg, "basics")

      // annotate trace using different methods
      trace.record(msg, "Start processing")
      trace.recordKeyValue(msg, "longAnswer", Random.nextLong())

      // log to trace
      log.debug("request: " + msg)

      try {
        // do something heavy
        Thread.sleep(Random.nextInt(200))
        val result = InternalMessage(str.reverse, -int)

        // mark response
        sender ! result.asResponseTo(msg)
      } catch {
        case e: Exception =>
          // record stack trace
          trace.record(msg, e)
      }
  }
}

// create actor system and send messages every second
object TracingBasics extends App {
  implicit val askTimeout: Timeout = 500.milliseconds
  val system = ActorSystem.create("TracingBasics", ConfigFactory.load("application"))
  val handler = system.actorOf(Props[BasicHandler])
  system.scheduler.schedule(3.seconds, 1.second) {
    val msg = InternalMessage(UUID.randomUUID.toString, Random.nextInt())
    println("Received " + msg)
    handler ? msg
  }
  system.awaitTermination()
}