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
import java.lang.Throwable;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationFetchException extends WebApplicationException {

  public ConfigurationFetchException(String message, AmbariApiException ex) {
    super(toEntity(message, ex));
  }

  private static Response toEntity(String message, AmbariApiException ex) {
    Map<String, Object> json = new HashMap<>();
    int status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    json.put("message", String.join(". ", message, ex.getMessage()) );
    json.put("status", status);
    json.put("trace", ex.getCause());
    return Response.status(status).entity(json).type(MediaType.APPLICATION_JSON).build();
  }

}
