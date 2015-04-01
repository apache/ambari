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

package org.apache.ambari.server.view;

import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.view.configuration.InstanceConfig;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.*;

/**
 * ClusterImpl tests.
 */
public class ClusterImplTest {

  @Test
  public void testGetName() throws Exception {
    Cluster cluster = createNiceMock(Cluster.class);

    expect(cluster.getClusterName()).andReturn("c1").anyTimes();

    replay(cluster);

    ClusterImpl clusterImpl = new ClusterImpl(cluster);

    Assert.assertEquals("c1", clusterImpl.getName());

    verify(cluster);
  }

  @Test
  public void testGetConfigurationValue() throws Exception {
    Cluster cluster = createNiceMock(Cluster.class);
    Config config = createNiceMock(Config.class);

    Map<String, String> properties = new HashMap<String, String>();

    properties.put("foo", "bar");

    expect(cluster.getDesiredConfigByType("core-site")).andReturn(config).anyTimes();
    expect(config.getProperties()).andReturn(properties).anyTimes();

    replay(cluster, config);

    ClusterImpl clusterImpl = new ClusterImpl(cluster);

    Assert.assertEquals("bar", clusterImpl.getConfigurationValue("core-site", "foo"));

    verify(cluster, config);
  }
}