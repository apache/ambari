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

package org.apache.ambari.view.tez.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown by the proxy resources
 */

public class ProxyException extends WebApplicationException {

  public ProxyException(String message, int status) {
    this(message, status, null);
  }

  public ProxyException(String message, int status, String trace) {
    super(toEntity(message, status, trace));
  }

  private static Response toEntity(String message, int status, String trace) {
    Map<String, Object> json = new HashMap<>();
    json.put("message", message);
    json.put("status", status);
    json.put("trace", trace);
    return Response.status(status).entity(json).type(MediaType.APPLICATION_JSON).build();
  }
}
