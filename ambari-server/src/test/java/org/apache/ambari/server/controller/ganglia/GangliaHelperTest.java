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
package org.apache.ambari.server.controller.ganglia;

import org.junit.Ignore;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 */
public class GangliaHelperTest {


  @Ignore
  @Test
  public void testGetGangliaMetrics() throws Exception {
    //MM/dd/yy HH:mm:ss

    String target = "ec2-107-22-86-120.compute-1.amazonaws.com";
    String cluster = "HDPNameNode";
    String host = "domU-12-31-39-15-25-C7.compute-1.internal";
    Date startTime = new SimpleDateFormat("MM/dd/yy HH:mm:ss").parse("09/12/12 10:00:00");
    Date endTime = new SimpleDateFormat("MM/dd/yy HH:mm:ss").parse("09/12/12 16:15:00");
    long step = 60;
//        String api  = "rpcdetailed.rpcdetailed.sendHeartbeat_num_ops";
    String metric = "cpu_nice";

//    List<GangliaMetric> metrics = GangliaHelper.getGangliaMetrics(target, cluster, host, metric, startTime, endTime, step);

    //TODO : assertions
  }
}
