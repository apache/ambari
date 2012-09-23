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

package org.apache.ambari.api.controller.jmx;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 */
public class JMXHelper {

  public static JMXMetrics getJMXMetrics(String target, String bean) {
    String s = "http://" + target + "/jmx?qry=" + (bean == null ? "Hadoop:*" : bean);
    try {
      URLConnection connection = new URL(s).openConnection();

      connection.setDoOutput(true);

      return new ObjectMapper().readValue(connection.getInputStream(),
          JMXMetrics.class);

    } catch (IOException e) {
      System.out.println("getJMXMetrics : caught " + e);
      throw new IllegalStateException("Can't get metric " + ".", e);
    }
  }
}
