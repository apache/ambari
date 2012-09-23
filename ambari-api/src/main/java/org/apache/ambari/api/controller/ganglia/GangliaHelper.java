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

package org.apache.ambari.api.controller.ganglia;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 */
public class GangliaHelper {

  public static List<GangliaMetric> getGangliaProperty(String target,
                                                       String gangliaCluster,
                                                       String host,
                                                       String metric) {
    String s = "http://" +
        target +
        "/ganglia/graph.php" +
        "?c=" + gangliaCluster +
        "&h=" + (host == null ? "" : host) +
        "&m=" + metric +
        "&json=1";

//        System.out.println("url=" + s);

    try {
      URLConnection connection = new URL(s).openConnection();

      connection.setDoOutput(true);

      return new ObjectMapper().readValue(connection.getInputStream(),
          new TypeReference<List<GangliaMetric>>() {
          });

    } catch (IOException e) {
      throw new IllegalStateException("Can't get metric " + metric + ".", e);
    }
  }


  public static List<GangliaMetric> getGangliaMetrics(String target,
                                                      String gangliaCluster,
                                                      String host,
                                                      String metric,
                                                      Date startTime,
                                                      Date endTime,
                                                      long step) {
    String s = "http://" +
        target +
        "/ganglia/graph.php" +
        "?c=" + gangliaCluster +
        "&h=" + (host == null ? "" : host) +
        "&m=" + metric +
        "&cs=" + startTime.getTime() / 1000 +
        "&ce=" + endTime.getTime() / 1000 +
        "&step=" + step +
        "&json=1";

    System.out.println("url=" + s);

    try {
      URLConnection connection = new URL(s).openConnection();

      connection.setDoOutput(true);

      return new ObjectMapper().readValue(connection.getInputStream(),
          new TypeReference<List<GangliaMetric>>() {
          });

    } catch (IOException e) {
      throw new IllegalStateException("Can't get metric " + metric + ".", e);
    }
  }
}
