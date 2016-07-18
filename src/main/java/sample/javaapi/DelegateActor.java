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

package sample.javaapi;

import akka.actor.UntypedActor;
import com.github.levkhomich.akka.tracing.TracingExtension;
import com.github.levkhomich.akka.tracing.TracingExtensionImpl;

import java.util.Random;

public class DelegateActor extends UntypedActor {

    TracingExtensionImpl trace = (TracingExtensionImpl) TracingExtension.apply(context().system());
    Random rng = new Random();

    public void onReceive(Object message) throws Exception {
        if (message instanceof InternalRequest) {
            InternalRequest msg = (InternalRequest) message;
            System.out.print("DelegateActor received " + msg);

            // notify tracing extension about external request to be sampled and traced, name service processing request
            trace.sample(msg, this.getClass().getSimpleName(), false);
            // another computation (sometimes leading to timeout)
            Thread.sleep(rng.nextInt(200));
            sender().tell(new InternalResponse(200, "Hello, " + msg.getPayload()), self());
            // fire server send event
            trace.finish(msg);
        } else {
            unhandled(message);
        }
    }
}
