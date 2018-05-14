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
import akka.actor.Inbox;
import akka.actor.PoisonPill;
import akka.actor.Props;
import com.google.common.base.Optional;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive2.actor.DeathWatch;
import org.apache.ambari.view.hive2.actor.OperationController;
import org.apache.ambari.view.hive2.internal.ConnectionSupplier;
import org.apache.ambari.view.hive2.internal.DataStorageSupplier;
import org.apache.ambari.view.hive2.internal.HdfsApiSupplier;
import org.apache.ambari.view.hive2.internal.SafeViewContext;
import org.apache.parquet.Strings;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionSystem {

  private static final String ACTOR_SYSTEM_NAME = "HiveViewActorSystem";
  private ActorSystem actorSystem = null;
  private static volatile ConnectionSystem instance = null;
  private static final Object lock = new Object();
  private static Map<String, Map<String, ActorRef>> operationControllerMap = new ConcurrentHashMap<>();

  // credentials map stores usernames and passwords
  private static Map<String, String> credentialsMap = new ConcurrentHashMap<>();

  private ConnectionSystem() {
    ClassLoader classLoader = getClass().getClassLoader();
    Config config = ConfigFactory.load(classLoader);
    this.actorSystem = ActorSystem.create(ACTOR_SYSTEM_NAME, config, classLoader);
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
   * @param viewContext
   * @return operationController Instance
   */
  public synchronized ActorRef getOperationController(ViewContext viewContext) {
    SafeViewContext context = new SafeViewContext(viewContext);
    String instanceName = context.getInstanceName();
    ActorRef ref = null;
    Map<String, ActorRef> stringActorRefMap = operationControllerMap.get(instanceName);
    if (stringActorRefMap != null) {
      ref = stringActorRefMap.get(context.getUsername());
    }
    if (ref == null) {
      ref = createOperationController(context);
      if (stringActorRefMap == null) {
        stringActorRefMap = new HashMap<>();
        stringActorRefMap.put(context.getUsername(), ref);
        operationControllerMap.put(instanceName, stringActorRefMap);
      } else {
        stringActorRefMap.put(context.getUsername(), ref);
      }
    }
    return ref;
  }

  public synchronized void persistCredentials(String user,String password){
    if(!Strings.isNullOrEmpty(password)){
      credentialsMap.put(user,password);
    }
  }


  public synchronized Optional<String> getPassword(ViewContext viewContext){
    String pass = credentialsMap.get(viewContext.getUsername());
    return Optional.fromNullable(pass);
  }

  public void removeOperationControllerFromCache(String viewInstanceName) {
    Map<String, ActorRef> refs = operationControllerMap.remove(viewInstanceName);
    if (refs != null) {
      for (ActorRef ref : refs.values()) {
        Inbox inbox = Inbox.create(getActorSystem());
        inbox.send(ref, PoisonPill.getInstance());
      }
    }
  }

  public void shutdown() {
    if (!actorSystem.isTerminated()) {
      actorSystem.shutdown();
    }
  }
}
