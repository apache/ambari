/*
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

package org.apache.ambari.server.api;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.authorization.jwt.JwtAuthenticationProperties;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.AbstractHttpConnection;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Custom error handler for Jetty to return response as JSON instead of stub http page
 */
public class AmbariErrorHandler extends ErrorHandler {
  private final Gson gson;
  private Configuration configuration;

  @Inject
  public AmbariErrorHandler(@Named("prettyGson") Gson prettyGson, Configuration configuration) {
    this.gson = prettyGson;
    this.configuration = configuration;
  }

  @Override
  public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
    AbstractHttpConnection connection = AbstractHttpConnection.getCurrentConnection();
    connection.getRequest().setHandled(true);

    response.setContentType(MimeTypes.TEXT_PLAIN);

    Map<String, Object> errorMap = new LinkedHashMap<>();
    int code = connection.getResponse().getStatus();
    errorMap.put("status", code);
    String message = connection.getResponse().getReason();
    if (message == null) {
      message = HttpStatus.getMessage(code);
    }
    errorMap.put("message", message);

    if (code == HttpServletResponse.SC_FORBIDDEN) {
      //if SSO is configured we should provide info about it in case of access error
      JwtAuthenticationProperties jwtProperties = configuration.getJwtProperties();
      if (jwtProperties != null) {
        errorMap.put("jwtProviderUrl", jwtProperties.getAuthenticationProviderUrl() + "?" +
          jwtProperties.getOriginalUrlQueryParam() + "=");
      }
    }

    gson.toJson(errorMap, response.getWriter());
  }
}
