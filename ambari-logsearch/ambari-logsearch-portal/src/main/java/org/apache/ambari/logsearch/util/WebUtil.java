/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ambari.logsearch.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebUtil {
  private static final Logger LOG = LoggerFactory.getLogger(WebUtil.class);

  private static final String WEB_RESOURCE_FOLDER = "webapps/app";

  private WebUtil() {
    throw new UnsupportedOperationException();
  }

  public static URI findWebResourceBase() {
    URL fileCompleteUrl = Thread.currentThread().getContextClassLoader().getResource(WEB_RESOURCE_FOLDER);
    String errorMessage = "Web Resource Folder " + WEB_RESOURCE_FOLDER + " not found in classpath";
    if (fileCompleteUrl != null) {
      try {
        return fileCompleteUrl.toURI().normalize();
      } catch (URISyntaxException e) {
        LOG.error(errorMessage, e);
        System.exit(1);
      }
    } else {
      LOG.error(errorMessage);
      System.exit(1);
    }
    throw new IllegalStateException(errorMessage);
  }

  public static void checkPort(int port) {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
    } catch (IOException ex) {
      LOG.error(ex.getLocalizedMessage() + " PORT :" + port);
      System.exit(1);
    }
  }

}
