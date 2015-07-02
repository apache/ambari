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

package org.apache.ambari.view.hive.client;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HiveCall <T> {
  private final static Logger LOG =
      LoggerFactory.getLogger(HiveCall.class);

  protected final Connection conn;

  public HiveCall(Connection connection) {
    this.conn = connection;
  }

  public abstract T body() throws HiveClientException;

  public T call() throws HiveClientException {
    T result = null;
    boolean needRetry = false;
    int attempts = 0;
    do {
      if (needRetry) {
        needRetry = false;
        attempts += 1;
        try {
          conn.closeConnection();
        } catch (Exception e) {
          LOG.error("Connection closed with error", e);
        }
      }

      if (conn.getClient() == null) {
        // previous attempt closed the connection, but new was failed to be established.
        // on new call trying to open the connection again.
        conn.openConnection();
      }

      try {

        synchronized (conn) {
          result = body();
        }

      } catch (HiveClientException ex) {
        Throwable root = ExceptionUtils.getRootCause(ex);
        if (attempts < 2 && root instanceof TTransportException) {
          needRetry = true;
          LOG.error("Retry call because of Transport Exception: " + root.toString());
          continue;
        }
        throw ex;
      }
    } while (needRetry);
    return result;
  }

}
