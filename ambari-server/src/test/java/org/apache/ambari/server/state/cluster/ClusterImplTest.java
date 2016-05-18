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

package org.apache.ambari.server.state.cluster;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.controller.AmbariSessionManager;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.verify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ClusterImplTest {

  private static Injector injector;
  private static Clusters clusters;

  @BeforeClass
  public static void setUpClass() throws Exception {
    Injector injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
  }
  
  @Test
  public void testAddSessionAttributes() throws Exception {
    Map<String, Object> attributes = new HashMap<String, Object>();
    attributes.put("foo", "bar");

    AmbariSessionManager sessionManager = createMock(AmbariSessionManager.class);

    ClusterImpl cluster =
        createMockBuilder(ClusterImpl.class).
            addMockedMethod("getSessionManager").
            addMockedMethod("getClusterName").
            addMockedMethod("getSessionAttributes").
            createMock();

    expect(cluster.getSessionManager()).andReturn(sessionManager);
    expect(cluster.getClusterName()).andReturn("c1");
    expect(cluster.getSessionAttributes()).andReturn(attributes);
    sessionManager.setAttribute("cluster_session_attributes:c1", attributes);

    replay(sessionManager, cluster);

    cluster.addSessionAttributes(attributes);

    verify(sessionManager, cluster);
  }

  @Test
  public void testSetSessionAttribute() throws Exception {
    Map<String, Object> attributes = new HashMap<String, Object>();
    attributes.put("foo", "bar");
    attributes.put("foo2", "bar2");

    Map<String, Object> updatedAttributes = new HashMap<String, Object>(attributes);
    updatedAttributes.put("foo2", "updated value");

    Map<String, Object> addedAttributes = new HashMap<String, Object>(updatedAttributes);
    updatedAttributes.put("foo3", "added value");

    AmbariSessionManager sessionManager = createMock(AmbariSessionManager.class);

    ClusterImpl cluster =
      createMockBuilder(ClusterImpl.class).
        addMockedMethod("getSessionManager").
        addMockedMethod("getClusterName").
        addMockedMethod("getSessionAttributes").
        createMock();

    expect(cluster.getSessionManager()).andReturn(sessionManager);
    expect(cluster.getClusterName()).andReturn("c1");
    expect(cluster.getSessionAttributes()).andReturn(attributes);

    sessionManager.setAttribute("cluster_session_attributes:c1", updatedAttributes);
    expectLastCall().once();

    expect(cluster.getSessionManager()).andReturn(sessionManager);
    expect(cluster.getClusterName()).andReturn("c1");
    expect(cluster.getSessionAttributes()).andReturn(updatedAttributes);

    sessionManager.setAttribute("cluster_session_attributes:c1", addedAttributes);
    expectLastCall().once();

    replay(sessionManager, cluster);

    cluster.setSessionAttribute("foo2", "updated value");
    cluster.setSessionAttribute("foo3", "added value");

    verify(sessionManager, cluster);
  }

  @Test
  public void testRemoveSessionAttribute() throws Exception {
    Map<String, Object> attributes = new HashMap<String, Object>();
    attributes.put("foo", "bar");
    attributes.put("foo2", "bar2");

    Map<String, Object> trimmedAttributes = new HashMap<String, Object>(attributes);
    trimmedAttributes.remove("foo2");

    AmbariSessionManager sessionManager = createMock(AmbariSessionManager.class);

    ClusterImpl cluster =
      createMockBuilder(ClusterImpl.class).
        addMockedMethod("getSessionManager").
        addMockedMethod("getClusterName").
        addMockedMethod("getSessionAttributes").
        createMock();

    expect(cluster.getSessionManager()).andReturn(sessionManager);
    expect(cluster.getClusterName()).andReturn("c1");
    expect(cluster.getSessionAttributes()).andReturn(attributes);
    sessionManager.setAttribute("cluster_session_attributes:c1", trimmedAttributes);
    expectLastCall().once();

    replay(sessionManager, cluster);

    cluster.removeSessionAttribute("foo2");

    verify(sessionManager, cluster);
  }

  @Test
  public void testGetSessionAttributes() throws Exception {
    Map<String, Object> attributes = new HashMap<String, Object>();
    attributes.put("foo", "bar");

    AmbariSessionManager sessionManager = createMock(AmbariSessionManager.class);

    ClusterImpl cluster =
        createMockBuilder(ClusterImpl.class).
            addMockedMethod("getSessionManager").
            addMockedMethod("getClusterName").
            createMock();

    expect(cluster.getSessionManager()).andReturn(sessionManager).anyTimes();
    expect(cluster.getClusterName()).andReturn("c1").anyTimes();
    expect(sessionManager.getAttribute("cluster_session_attributes:c1")).andReturn(attributes);
    expect(sessionManager.getAttribute("cluster_session_attributes:c1")).andReturn(null);

    replay(sessionManager, cluster);

    assertEquals(attributes, cluster.getSessionAttributes());
    assertEquals(Collections.<String, Object>emptyMap(), cluster.getSessionAttributes());

    verify(sessionManager, cluster);
  }

  @Test
  public void testDeleteService() throws Exception {
    // Given
    String serviceToDelete = "TEZ";

    String clusterName = "TEST_CLUSTER";
    String hostName1 = "HOST1", hostName2 = "HOST2";

    clusters.addCluster(clusterName, new StackId("HDP-2.1.1"));

    Cluster cluster = clusters.getCluster(clusterName);

    clusters.addHost(hostName1);
    clusters.addHost(hostName2);

    Host host1 = clusters.getHost(hostName1);
    host1.setHostAttributes(ImmutableMap.of("os_family", "centos", "os_release_version", "6.0"));
    host1.persist();

    Host host2 = clusters.getHost(hostName2);
    host2.setHostAttributes(ImmutableMap.of("os_family", "centos", "os_release_version", "6.0"));
    host2.persist();

    clusters.mapHostsToCluster(Sets.newHashSet(hostName1, hostName2), clusterName);

    Service hdfs = cluster.addService("HDFS");
    hdfs.persist();

    ServiceComponent nameNode = hdfs.addServiceComponent("NAMENODE");
    nameNode.persist();
    nameNode.addServiceComponentHost(hostName1).persist();

    ServiceComponent dataNode = hdfs.addServiceComponent("DATANODE");
    dataNode.persist();
    dataNode.addServiceComponentHost(hostName1).persist();
    dataNode.addServiceComponentHost(hostName2).persist();

    ServiceComponent hdfsClient = hdfs.addServiceComponent("HDFS_CLIENT");
    hdfsClient.persist();
    hdfsClient.addServiceComponentHost(hostName1).persist();
    hdfsClient.addServiceComponentHost(hostName2).persist();

    Service tez = cluster.addService(serviceToDelete);
    tez.persist();

    ServiceComponent tezClient = tez.addServiceComponent("TEZ_CLIENT");
    tezClient.persist();
    ServiceComponentHost tezClientHost1 =  tezClient.addServiceComponentHost(hostName1);
    tezClientHost1.persist();
    ServiceComponentHost tezClientHost2 = tezClient.addServiceComponentHost(hostName2);
    tezClientHost2.persist();

    // When
    cluster.deleteService(serviceToDelete);

    // Then
    assertFalse("Deleted service should be removed from the service collection !", cluster.getServices().containsKey(serviceToDelete));

    assertEquals("All components of the deleted service should be removed from all hosts", 0, cluster.getServiceComponentHosts(serviceToDelete, null).size());

    boolean checkHost1 = !cluster.getServiceComponentHosts(hostName1).contains(tezClientHost1);
    boolean checkHost2 = !cluster.getServiceComponentHosts(hostName2).contains(tezClientHost2);

    assertTrue("All components of the deleted service should be removed from all hosts", checkHost1 && checkHost2);

  }

  @Test
  public void testDeleteHost() throws Exception {
    // Given


    String clusterName = "TEST_DELETE_HOST";
    String hostName1 = "HOSTNAME1", hostName2 = "HOSTNAME2";
    String hostToDelete = hostName2;

    clusters.addCluster(clusterName, new StackId("HDP-2.1.1"));

    Cluster cluster = clusters.getCluster(clusterName);

    clusters.addHost(hostName1);
    clusters.addHost(hostName2);

    Host host1 = clusters.getHost(hostName1);
    host1.setHostAttributes(ImmutableMap.of("os_family", "centos", "os_release_version", "6.0"));
    host1.persist();

    Host host2 = clusters.getHost(hostName2);
    host2.setHostAttributes(ImmutableMap.of("os_family", "centos", "os_release_version", "6.0"));
    host2.persist();

    clusters.mapHostsToCluster(Sets.newHashSet(hostName1, hostName2), clusterName);

    // When
    clusters.deleteHost(hostToDelete);

    // Then
    assertTrue(clusters.getClustersForHost(hostToDelete).isEmpty());
    assertFalse(clusters.getHostsForCluster(clusterName).containsKey(hostToDelete));

    assertFalse(cluster.getHosts().contains(hostToDelete));

    try {
      clusters.getHost(hostToDelete);
      fail("getHost(hostName) should throw Exception when invoked for deleted host !");
    }
    catch(HostNotFoundException e){

    }


  }
}