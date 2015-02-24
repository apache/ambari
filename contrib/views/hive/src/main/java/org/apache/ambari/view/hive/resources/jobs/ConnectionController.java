/**
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

package org.apache.ambari.view.hive.resources.jobs;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive.client.Connection;
import org.apache.ambari.view.hive.client.ConnectionPool;
import org.apache.ambari.view.hive.client.HiveClientException;
import org.apache.ambari.view.hive.utils.ServiceFormattedException;
import org.apache.hive.service.cli.thrift.TOperationHandle;

import java.util.HashMap;
import java.util.Map;

public class ConnectionController {
  private ViewContext context;
  private Connection connection;
  private OperationHandleControllerFactory operationHandleControllerFactory;

  private ConnectionController(ViewContext context) {
    this.context = context;
    connection = ConnectionPool.getConnection(context);
    operationHandleControllerFactory = OperationHandleControllerFactory.getInstance(context);
  }

  private static Map<String, ConnectionController> viewSingletonObjects = new HashMap<String, ConnectionController>();
  public static ConnectionController getInstance(ViewContext context) {
    if (!viewSingletonObjects.containsKey(context.getInstanceName()))
      viewSingletonObjects.put(context.getInstanceName(), new ConnectionController(context));
    return viewSingletonObjects.get(context.getInstanceName());
  }

  public void selectDatabase(String database) {
    executeQuery("use " + database + ";");
  }

  public OperationHandleController executeQuery(String cmd) {
    TOperationHandle operationHandle = null;
    try {
      operationHandle = connection.executeAsync(cmd);
    } catch (HiveClientException e) {
      throw new ServiceFormattedException(e.toString(), e);
    }
    return operationHandleControllerFactory.createControllerForHandle(operationHandle);
  }
}
