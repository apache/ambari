/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive20.resources.system;

import akka.actor.ActorRef;
import org.apache.ambari.view.hive20.BaseService;
import org.apache.ambari.view.hive20.ConnectionSystem;
import org.apache.ambari.view.hive20.actor.message.Ping;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * System services which are required for the working of the application
 */
public class SystemService extends BaseService {

  /**
   * Clients should sent pings to the server at regular interval so that the system could keep alive stuffs or do
   * cleanup work when the pings stops
   * @return No content
   */
  @POST
  @Path("ping")
  public Response ping() {
    //TODO: Change this to EventBus implementation
    ActorRef metaDataManager = ConnectionSystem.getInstance().getMetaDataManager(context);
    metaDataManager.tell(new Ping(context.getUsername(), context.getInstanceName()), ActorRef.noSender());
    return Response.ok().status(Response.Status.NO_CONTENT).build();
  }
}
