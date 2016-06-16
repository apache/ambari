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

package org.apache.ambari.view.hive2;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive2.actor.DeathWatch;
import org.apache.ambari.view.hive2.actor.OperationController;
import org.apache.ambari.view.hive2.internal.ConnectionSupplier;
import org.apache.ambari.view.hive2.internal.DataStorageSupplier;
import org.apache.ambari.view.hive2.internal.HdfsApiSupplier;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConnectionSystem {

  private static final String ACTOR_CONF_FILE = "application.conf";
  private static final String ACTOR_SYSTEM_NAME = "HiveViewActorSystem";
  private ActorSystem actorSystem = null;
  private static volatile ConnectionSystem instance = null;
  private static final Object lock = new Object();
  private static Map<String, ActorRef> operationControllerMap = new HashMap<>();

  private ConnectionSystem() {
    this.actorSystem = ActorSystem.create(ACTOR_SYSTEM_NAME);
    ;
  }

  public static ConnectionSystem getInstance() {
    if (instance == null) {
      synchronized (lock) {
        if (instance == null) {
          instance = new ConnectionSystem();
        }
      }
    }
    return instance;
  }

  private ActorRef createOperationController(ViewContext context) {
    ActorRef deathWatch = actorSystem.actorOf(Props.create(DeathWatch.class));
    return actorSystem.actorOf(
      Props.create(OperationController.class, actorSystem, deathWatch, context,
        new ConnectionSupplier(), new DataStorageSupplier(), new HdfsApiSupplier()));
  }

  public ActorSystem getActorSystem() {
    return actorSystem;
  }

  /**
   * Returns one operationController per View Instance
   *
   * @param context
   * @return operationController Instance
   */
  public ActorRef getOperationController(ViewContext context) {
    String instanceName = context.getInstanceName();
    ActorRef ref = operationControllerMap.get(instanceName);
    if (ref == null) {
      synchronized (lock) {
        ref = operationControllerMap.get(instanceName);
        if (ref == null) {
          ref = createOperationController(context);
          operationControllerMap.put(instanceName, ref);
        }
      }
    }
    return ref;
  }

  public void shutdown() {
    if (!actorSystem.isTerminated()) {
      actorSystem.shutdown();
    }
  }
}
