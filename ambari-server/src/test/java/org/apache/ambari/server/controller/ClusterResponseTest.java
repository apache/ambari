package org.apache.ambari.server.controller;

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

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class ClusterResponseTest {

  @Test
  public void testBasicGetAndSet() {
    Long clusterId = new Long(10);
    String clusterName = "foo";
    Set<String> hostNames = new HashSet<String>();
    hostNames.add("h1");

    ClusterResponse r1 =
        new ClusterResponse(clusterId, clusterName, hostNames, "bar");
    
    Assert.assertEquals(clusterId, r1.getClusterId());
    Assert.assertEquals(clusterName, r1.getClusterName());
    Assert.assertArrayEquals(hostNames.toArray(), r1.getHostNames().toArray());
    Assert.assertEquals("bar", r1.getDesiredStackVersion());
  }

  @Test
  public void testToString() {
    ClusterResponse r = new ClusterResponse(null, null, null, null);
    r.toString();
  }
}
