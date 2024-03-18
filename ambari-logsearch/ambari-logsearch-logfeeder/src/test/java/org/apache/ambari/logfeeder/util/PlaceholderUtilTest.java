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

package org.apache.ambari.logfeeder.util;

import java.util.HashMap;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PlaceholderUtilTest {
  @Test
  public void testPlaceholderUtil_replaceVariables() {
    String hostName = "host1";
    String ip = "127.0.0.1";
    String clusterName = "test-cluster";
    
    HashMap<String, String> contextParam = new HashMap<String, String>();
    contextParam.put("host", hostName);
    contextParam.put("ip", ip);
    contextParam.put("cluster", clusterName);
    
    String resultStr = PlaceholderUtil.replaceVariables("$CLUSTER/logfeeder/$HOST-$IP/logs", contextParam);
    String expectedStr = clusterName + "/logfeeder/" + hostName + "-" + ip + "/logs";
    
    assertEquals("Result string :" + resultStr + " is not equal to exptected string :" + expectedStr, resultStr, expectedStr);
  }
}
