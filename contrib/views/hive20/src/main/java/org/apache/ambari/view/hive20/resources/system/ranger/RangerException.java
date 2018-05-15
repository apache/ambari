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

package org.apache.ambari.view.hive20.resources.system.ranger;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.simple.JSONObject;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Exceptions send by the Authorization API
 */
public class RangerException extends WebApplicationException {

  public RangerException(String message, String errorCode, int status, Exception ex) {
    super(errorEntity(message, errorCode, status, ex));
  }

  public RangerException(String message, String errorCode, int status) {
    this(message, errorCode, status, null);
  }

  protected static Response errorEntity(String message, String errorCode, int status, Exception ex) {
    Map<String, Object> response = new HashMap<String, Object>();
    response.put("message", message);
    response.put("errorCode", errorCode);
    if (ex != null) {
      response.put("trace", ExceptionUtils.getStackTrace(ex));
    }

    JSONObject finalResponse = new JSONObject();
    finalResponse.put("errors", response);
    return Response.status(status).entity(new JSONObject(finalResponse)).type(MediaType.APPLICATION_JSON).build();
  }

}
