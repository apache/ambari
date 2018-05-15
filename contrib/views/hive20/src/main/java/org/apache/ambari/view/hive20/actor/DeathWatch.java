/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive20.actor;

import akka.actor.ActorRef;
import akka.actor.Terminated;
import org.apache.ambari.view.hive20.actor.message.HiveMessage;
import org.apache.ambari.view.hive20.actor.message.RegisterActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class DeathWatch extends HiveActor {

    private final static Logger LOG =
            LoggerFactory.getLogger(DeathWatch.class);

    @Override
    public void handleMessage(HiveMessage hiveMessage) {
        Object message = hiveMessage.getMessage();
        if(message instanceof RegisterActor){
            RegisterActor registerActor = (RegisterActor) message;
            ActorRef actorRef = registerActor.getActorRef();
            this.getContext().watch(actorRef);
            LOG.info("Registered new actor "+ actorRef);
            LOG.info("Registration for {} at {}", actorRef,new Date());
        }else if(message instanceof Terminated){
            Terminated terminated = (Terminated) message;
            ActorRef actor = terminated.actor();
            LOG.info("Received terminate for actor {} with message : {}", actor, terminated);
            LOG.info("Termination for {} at {}", actor,new Date());

        }else{
            LOG.info("received unknown message : {}", hiveMessage);
        }

    }
}
