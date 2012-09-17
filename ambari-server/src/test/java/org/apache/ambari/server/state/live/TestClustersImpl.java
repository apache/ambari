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

package org.apache.ambari.server.state.live;

import static org.junit.Assert.fail;
import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestClustersImpl {

  private Clusters clusters;

  @Before
  public void setup() {
    clusters = new ClustersImpl();
  }

  @After
  public void teardown() {
    clusters = null;
  }

  @Test
  public void testGetInvalidCluster() throws AmbariException {
    try {
      clusters.getCluster("foo");
      fail("Exception should be thrown on invalid get");
    }
    catch (ClusterNotFoundException e) {
      // Expected
    }

  }

  @Test
  public void testAddCluster() throws AmbariException {

    String c1 = "foo";
    String c2 = "foo";
    clusters.addCluster(c1);

    try {
      clusters.addCluster(c1);
      fail("Exception should be thrown on invalid add");
    }
    catch (AmbariException e) {
      // Expected
    }

    try {
      clusters.addCluster(c2);
      fail("Exception should be thrown on invalid add");
    }
    catch (AmbariException e) {
      // Expected
    }

    c2 = "foo2";
    clusters.addCluster(c2);

    Assert.assertNotNull(clusters.getCluster(c1));
    Assert.assertNotNull(clusters.getCluster(c2));

    Assert.assertEquals(c1, clusters.getCluster(c1).getClusterName());
    Assert.assertEquals(c2, clusters.getCluster(c2).getClusterName());

  }
}
