/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.resources;

import java.io.IOException;
import java.nio.file.Files;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.ambari.server.mpack.MpackException;

/** Serve only indexed immutable mpack archives through the existing resource boundary. */
public class MpackResourceServlet extends HttpServlet {
  private final ResourceManager resources;

  public MpackResourceServlet(ResourceManager resources) {
    this.resources = resources;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String path = request.getPathInfo();
    if (path == null || path.equals("/")) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    try {
      java.nio.file.Path file = resources.getResource("mpacks" + path).toPath();
      response.setContentType("application/octet-stream");
      response.setContentLengthLong(Files.size(file));
      Files.copy(file, response.getOutputStream());
    } catch (MpackException exception) {
      int status = switch (exception.getCode()) {
        case NOT_FOUND -> HttpServletResponse.SC_NOT_FOUND;
        case STORAGE_FAILURE -> HttpServletResponse.SC_SERVICE_UNAVAILABLE;
        default -> HttpServletResponse.SC_BAD_REQUEST;
      };
      response.sendError(status);
    }
  }
}
