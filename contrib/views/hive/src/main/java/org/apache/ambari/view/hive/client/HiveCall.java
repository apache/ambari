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
import org.apache.hive.service.cli.thrift.TSessionHandle;
import org.apache.hive.service.cli.thrift.TStatus;
import org.apache.hive.service.cli.thrift.TStatusCode;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class HiveCall <T> {
  private final static Logger LOG =
      LoggerFactory.getLogger(HiveCall.class);

  protected final Connection conn;
  protected final TSessionHandle sessionHandle;

  public HiveCall(Connection connection) {
    this(connection,null);
  }

  public HiveCall(Connection connection, TSessionHandle sessionHandle) {
    this.conn = connection;
    this.sessionHandle = sessionHandle;
  }

  public abstract T body() throws HiveClientException;

  public boolean validateSession(T t) throws HiveClientException {
    //invalidate a session
    try {
      Method m = t.getClass().getMethod("getStatus");
      if (m != null) {
        TStatus status = (TStatus) m.invoke(t);
        if (status.getStatusCode().equals(TStatusCode.ERROR_STATUS) &&
          status.getErrorMessage().startsWith("Invalid SessionHandle: SessionHandle")) {
          try {
            conn.invalidateSessionBySessionHandle(sessionHandle);
          } catch (HiveClientException e) {
            LOG.error(e.getMessage(),e);
          }
          throw new HiveClientException("Please Retry." + status.getErrorMessage(), null);
          //return false;
        }
      }
    } catch (NoSuchMethodException e) {

    } catch (InvocationTargetException e) {

    } catch (IllegalAccessException e) {

    }
    return true;
  }

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
          if(sessionHandle !=null) {
            this.validateSession(result);
          }
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
