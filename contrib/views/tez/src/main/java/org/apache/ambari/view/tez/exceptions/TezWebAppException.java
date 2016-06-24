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

package org.apache.ambari.view.tez.exceptions;

import org.apache.ambari.view.utils.ambari.AmbariApiException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.String;
import java.lang.Throwable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * A wrapper class for all exception in Tez View, with more meaningful error message
 * */
public class TezWebAppException extends WebApplicationException {

  /**
   * Creates an exception from a message with status 500:INTERNAL_SERVER_ERROR
   * @param message String Extra message that wouldbe prefixed to the exception
   ***/
  public TezWebAppException(String message) {
    super(toEntity(message, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));
  }

  /**
   * Creates an exception from an exception with status 500:INTERNAL_SERVER_ERROR
   * @param message String Extra message that wouldbe prefixed to the exception
   * @param ex Exception to be wrapped
   ***/
  public TezWebAppException(String message, Exception ex) {
    super(toEntity(message, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ex));
  }

  /**
   * Creates an exception from an exception with custom status
   * @param message String Extra message that wouldbe prefixed to the exception
   * @param status int Custom status
   * @param ex Exception to be wrapped
   ***/
  public TezWebAppException(String message, int status, Exception ex) {
    super(toEntity(message, status, ex));
  }

  /**
   * Creates an exception from message, status and trace
   * @param message String Extra message that wouldbe prefixed to the exception
   * @param status int Custom status
   * @param trace String
   ***/
  public TezWebAppException(String message, int status, String trace) {
    super(toEntity(message, status, trace));
  }

  /**
   * Creates the response object with the given exception
   *
   * @param message String
   * @param status int
   * @param ex Exception
   *
   * @return Response
   * */
  private static Response toEntity(String message, int status, Exception ex) {
    return toEntity(message + ". " + ex.getMessage(), status, ExceptionUtils.getStackTrace(ex));
  }

  /**
   * Creates the response object
   *
   * @param message String
   * @param status int
   * @param trace String
   *
   * @return Response
   * */
  private static Response toEntity(String message, int status, String trace) {
    Map<String, Object> json = new HashMap<>();
    json.put("message", message);
    json.put("status", status);
    json.put("trace", trace);
    return Response.status(status).entity(json).type(MediaType.APPLICATION_JSON).build();
  }

}
