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

import org.apache.ambari.view.hive.client.Connection;
import org.apache.ambari.view.hive.client.HiveClientException;
import org.apache.ambari.view.hive.utils.HiveClientFormattedException;
import org.apache.ambari.view.hive.utils.ServiceFormattedException;
import org.apache.commons.codec.binary.Hex;
import org.apache.hive.service.cli.thrift.TOperationHandle;
import org.apache.hive.service.cli.thrift.TSessionHandle;


public class ConnectionController {
  private OperationHandleControllerFactory operationHandleControllerFactory;
  private Connection connection;

  public ConnectionController(OperationHandleControllerFactory operationHandleControllerFactory, Connection connection) {
    this.connection = connection;
    this.operationHandleControllerFactory = operationHandleControllerFactory;
  }

  public TSessionHandle getSessionByTag(String tag) throws HiveClientException {
    return connection.getSessionByTag(tag);
  }

  public String openSession() {
    try {
      TSessionHandle sessionHandle = connection.openSession();
      return getTagBySession(sessionHandle);
    } catch (HiveClientException e) {
      throw new HiveClientFormattedException(e);
    }
  }

  public static String getTagBySession(TSessionHandle sessionHandle) {
    return Hex.encodeHexString(sessionHandle.getSessionId().getGuid());
  }

  public void selectDatabase(TSessionHandle session, String database) {
    try {
      connection.executeSync(session, "use " + database + ";");
    } catch (HiveClientException e) {
      throw new HiveClientFormattedException(e);
    }
  }

  public OperationHandleController executeQuery(TSessionHandle session, String cmd) {
    TOperationHandle operationHandle = null;
    try {
      operationHandle = connection.executeAsync(session, cmd);
    } catch (HiveClientException e) {
      throw new HiveClientFormattedException(e);
    }
    StoredOperationHandle storedOperationHandle = StoredOperationHandle.buildFromTOperationHandle(operationHandle);
    return operationHandleControllerFactory.createControllerForHandle(storedOperationHandle);
  }
}
