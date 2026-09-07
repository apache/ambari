/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.agent.stomp;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Collections;

import org.apache.ambari.server.events.TelemetryUpdateEvent;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class PrometheusTargetDiscoveryTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  public void testDiscoveryUsesCachedAssignmentsAndStableEtag() throws Exception {
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    Host host = createMock(Host.class);
    TelemetryHolder telemetryHolder = createMock(TelemetryHolder.class);
    TelemetryUpdateEvent event = assignment();
    event.setHash("host-revision-1");

    expect(clusters.getCluster("cluster-one")).andReturn(cluster).times(2);
    expect(cluster.getClusterId()).andStubReturn(11L);
    expect(cluster.getClusterName()).andStubReturn("cluster-one");
    expect(cluster.getHosts()).andReturn(Collections.singletonList(host)).times(2);
    expect(host.getHostId()).andStubReturn(7L);
    expect(host.getHostName()).andStubReturn("worker1.example.com");
    expect(telemetryHolder.initializeDataIfNeeded(7L, true)).andReturn(event).times(2);
    replay(clusters, cluster, host, telemetryHolder);

    PrometheusTargetDiscovery discovery =
        new PrometheusTargetDiscovery(clusters, telemetryHolder, 9101);
    PrometheusTargetDiscovery.Result first = discovery.discover("cluster-one");
    PrometheusTargetDiscovery.Result second = discovery.discover("cluster-one");
    JsonNode groups = MAPPER.readTree(first.getBody());

    assertSame(first, second);
    assertEquals(first.getEtag(), second.getEtag());
    assertEquals(2, groups.size());
    assertEquals("worker1.example.com:9101", groups.get(0).path("targets").get(0).asText());
    assertEquals("/metrics", groups.get(0).path("labels").path("__metrics_path__").asText());
    assertEquals("/metrics/components/c11-hdfs-datanode",
        groups.get(1).path("labels").path("__metrics_path__").asText());
    assertEquals("DATANODE", groups.get(1).path("labels").path("component").asText());
    verify(clusters, cluster, host, telemetryHolder);
  }

  private TelemetryUpdateEvent assignment() {
    ObjectNode assignment = JsonNodeFactory.instance.objectNode();
    assignment.put("schemaVersion", 1);
    ObjectNode target = assignment.putArray("targets").addObject();
    target.put("id", "c11-hdfs-datanode");
    target.put("clusterId", 11);
    target.put("service", "HDFS");
    target.put("component", "DATANODE");

    ObjectNode otherCluster = assignment.withArray("targets").addObject();
    otherCluster.put("id", "c12-yarn-nodemanager");
    otherCluster.put("clusterId", 12);
    otherCluster.put("service", "YARN");
    otherCluster.put("component", "NODEMANAGER");
    return new TelemetryUpdateEvent(7L, assignment, Collections.emptyMap());
  }
}
