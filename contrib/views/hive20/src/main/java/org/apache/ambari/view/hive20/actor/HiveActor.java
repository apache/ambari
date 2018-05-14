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

import akka.actor.UntypedActor;
import org.apache.ambari.view.hive20.actor.message.HiveMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HiveActor extends UntypedActor {

  private static final Logger LOG = LoggerFactory.getLogger(HiveActor.class);

  @Override
  final public void onReceive(Object message) throws Exception {
    HiveMessage hiveMessage = new HiveMessage(message);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Received message: " + message.getClass().getName() + ", generated id: " + hiveMessage.getId() +
          " sent by: " + sender() + ", recieved by" + self());
    }

    handleMessage(hiveMessage);

    if (LOG.isDebugEnabled()) {
      LOG.debug("Message submitted: " + hiveMessage.getId());
    }
  }

  public abstract void handleMessage(HiveMessage hiveMessage);
}
