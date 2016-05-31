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

package org.apache.ambari.view.hive2.utils;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.AccessControlException;
import java.util.HashMap;

public class ServiceFormattedException extends WebApplicationException {
  private final static Logger LOG =
      LoggerFactory.getLogger(ServiceFormattedException.class);

  public ServiceFormattedException(String message) {
    super(errorEntity(message, null, suggestStatus(null), null));
  }

  public ServiceFormattedException(Throwable exception) {
    super(errorEntity(null, exception, suggestStatus(exception), null));
  }

  public ServiceFormattedException(String message, Throwable exception) {
    super(errorEntity(message, exception, suggestStatus(exception), null));
  }

  public ServiceFormattedException(String message, Throwable exception, int status) {
    super(errorEntity(message, exception, status, null));
  }

  public ServiceFormattedException(String message, Exception ex, String curl) {
    super(errorEntity(message, ex, suggestStatus(ex), curl));
  }

  private static int suggestStatus(Throwable exception) {
    int status = 500;
    if (exception == null) {
      return status;
    }
    if (exception instanceof AccessControlException) {
      status = 403;
    }
    /*if (exception instanceof HiveInvalidQueryException) {
      status = 400;
    }*/
    return status;
  }

  protected static Response errorEntity(String message, Throwable e, int status, String header) {
    HashMap<String, Object> response = new HashMap<String, Object>();

    String trace = null;

    response.put("message", message);
    if (e != null) {
      trace = e.toString() + "\n\n";
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      trace += sw.toString();

      if (message == null) {
        String innerMessage = e.getMessage();
        String autoMessage;

        if (innerMessage != null)
          autoMessage = String.format("E090 %s [%s]", innerMessage, e.getClass().getSimpleName());
        else
          autoMessage = "E090 " + e.getClass().getSimpleName();
        response.put("message", autoMessage);
      }
    }
    response.put("trace", trace);
    response.put("status", status);

    if(message != null && status != 400) LOG.error(message);
    if(trace != null && status != 400) LOG.error(trace);

    Response.ResponseBuilder responseBuilder = Response.status(status).entity(new JSONObject(response)).type(MediaType.APPLICATION_JSON);
    if (header != null)
      responseBuilder.header("X-INFO", header);
    return responseBuilder.build();
  }
}
