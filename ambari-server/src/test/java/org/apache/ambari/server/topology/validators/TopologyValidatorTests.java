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
package org.apache.ambari.server.topology.validators;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.Configuration;

/**
 * Helper for TopologyValidator tests.
 */
public class TopologyValidatorTests {

  /**
   * Creates a mock ClusterTopology with the given properties.
   */
  static ClusterTopology topologyWithProperties(Map<String, Map<String, String>> properties) {
    Configuration topologyConfig = new Configuration(properties, new HashMap<>());
    ClusterTopology topology = createNiceMock(ClusterTopology.class);
    expect(topology.getConfiguration()).andReturn(topologyConfig).anyTimes();
    replay(topology);
    return topology;
  }

}
