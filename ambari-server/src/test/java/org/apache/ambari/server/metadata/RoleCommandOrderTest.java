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

package org.apache.ambari.server.metadata;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.metadata.RoleCommandOrder.RoleCommandPair;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

public class RoleCommandOrderTest {

  private Injector injector;

  private final static String TEST_RCO_DATA_FILE = "test_rco_data.json";

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }


  /**
   * Tests building dependencies in HCFS cluster. Uses real dependency mapping
   * file (role_command_order.json)
   */
  @Test
  public void testInitializeAtHCFSCluster() throws AmbariException {

    RoleCommandOrder rco = injector.getInstance(RoleCommandOrder.class);
    ClusterImpl cluster = createMock(ClusterImpl.class);
    Service service = createMock(Service.class);
    expect(cluster.getCurrentStackVersion()).andReturn(new StackId("HDP", "2.0.6"));
    expect(cluster.getService("HCFS")).andReturn(service);
    expect(cluster.getService("HDFS")).andReturn(null);
    replay(cluster);

    Map<RoleCommandPair, Set<RoleCommandPair>> deps = rco.getDependencies();
    assertTrue("Dependencies are empty before initialization", deps.size() == 0);
    rco.initialize(cluster);
    assertTrue("Dependencies are loaded after initialization", deps.size() > 0);
    verify(cluster);
	// Check that HDFS components are not present in dependencies
    // Checking blocked roles
    assertFalse(dependenciesContainBlockedRole(deps, Role.DATANODE));
    assertFalse(dependenciesContainBlockedRole(deps, Role.NAMENODE));
    assertFalse(dependenciesContainBlockedRole(deps, Role.SECONDARY_NAMENODE));
    assertFalse(dependenciesContainBlockedRole(deps, Role.JOURNALNODE));
    assertFalse(dependenciesContainBlockedRole(deps, Role.NAMENODE_SERVICE_CHECK));
    assertFalse(dependenciesContainBlockedRole(deps, Role.HDFS_SERVICE_CHECK));
    assertFalse(dependenciesContainBlockedRole(deps, Role.HDFS_CLIENT));
    // Checking blocker roles
    assertFalse(dependenciesContainBlockerRole(deps, Role.DATANODE));
    assertFalse(dependenciesContainBlockerRole(deps, Role.NAMENODE));
    assertFalse(dependenciesContainBlockerRole(deps, Role.SECONDARY_NAMENODE));
    assertFalse(dependenciesContainBlockerRole(deps, Role.JOURNALNODE));
    assertFalse(dependenciesContainBlockerRole(deps, Role.NAMENODE_SERVICE_CHECK));
    assertFalse(dependenciesContainBlockerRole(deps, Role.HDFS_SERVICE_CHECK));
    assertFalse(dependenciesContainBlockerRole(deps, Role.HDFS_CLIENT));

    // And that some HCFS components are present (section has been loaded)
    assertTrue(dependenciesContainBlockerRole(deps, Role.PEERSTATUS));

  }


  /**
   * Tests building dependencies in not HA-enabled HDFS cluster. Uses real
   * dependency mapping file (role_command_order.json)
   */
  @Test
  public void testInitializeAtHDFSCluster() throws AmbariException {

    RoleCommandOrder rco = injector.getInstance(RoleCommandOrder.class);
    ClusterImpl cluster = createMock(ClusterImpl.class);
    expect(cluster.getService("HCFS")).andReturn(null);

    Service hdfsService = createMock(Service.class);

    expect(cluster.getService("HDFS")).andReturn(hdfsService).atLeastOnce();
    expect(hdfsService.getServiceComponent("JOURNALNODE")).andReturn(null);
    expect(cluster.getCurrentStackVersion()).andReturn(new StackId("HDP", "2.0.6"));

    replay(cluster);
    replay(hdfsService);

    Map<RoleCommandPair, Set<RoleCommandPair>> deps = rco.getDependencies();
    assertTrue("Dependencies are empty before initialization", deps.size() == 0);
    rco.initialize(cluster);
    assertTrue("Dependencies are loaded after initialization", deps.size() > 0);
    verify(cluster);
    verify(hdfsService);
    // Check that HCFS components are not present in dependencies
    // Checking blocked roles
    assertFalse(dependenciesContainBlockedRole(deps, Role.PEERSTATUS));
    assertFalse(dependenciesContainBlockedRole(deps, Role.HCFS_SERVICE_CHECK));
    assertFalse(dependenciesContainBlockedRole(deps, Role.HCFS_CLIENT));
    // Checking blocker roles
    assertFalse(dependenciesContainBlockerRole(deps, Role.PEERSTATUS));
    assertFalse(dependenciesContainBlockerRole(deps, Role.HCFS_SERVICE_CHECK));
    assertFalse(dependenciesContainBlockerRole(deps, Role.HCFS_CLIENT));

    // And that some HDFS components are present (section has been loaded)
    assertTrue(dependenciesContainBlockerRole(deps, Role.DATANODE));
    // Check that there is no HA NN dependencies present
    assertFalse(dependenciesContainBlockerRole(deps, Role.JOURNALNODE));
    assertFalse(dependenciesContainBlockedRole(deps, Role.ZKFC));
  }


  /**
   * Tests building dependencies in HA-enabled HDFS cluster. Uses real
   * dependency mapping file (role_command_order.json)
   */
  @Test
  public void testInitializeAtHaHDFSCluster() throws AmbariException {
    RoleCommandOrder rco = injector.getInstance(RoleCommandOrder.class);
    ClusterImpl cluster = createMock(ClusterImpl.class);
    expect(cluster.getService("HCFS")).andReturn(null);

    Service hdfsService = createMock(Service.class);
    ServiceComponent journalnodeSC = createMock(ServiceComponent.class);

    expect(cluster.getService("HDFS")).andReturn(hdfsService).atLeastOnce();
    expect(hdfsService.getServiceComponent("JOURNALNODE")).andReturn(journalnodeSC);
    expect(cluster.getCurrentStackVersion()).andReturn(new StackId("HDP", "2.0.6"));

    replay(cluster);
    replay(hdfsService);

    Map<RoleCommandPair, Set<RoleCommandPair>> deps = rco.getDependencies();
    assertTrue("Dependencies are empty before initialization", deps.size() == 0);
    rco.initialize(cluster);
    assertTrue("Dependencies are loaded after initialization", deps.size() > 0);
    verify(cluster);
    verify(hdfsService);
    // Check that HCFS components are not present in dependencies
    // Checking blocked roles
    assertFalse(dependenciesContainBlockedRole(deps, Role.PEERSTATUS));
    assertFalse(dependenciesContainBlockedRole(deps, Role.HCFS_SERVICE_CHECK));
    assertFalse(dependenciesContainBlockedRole(deps, Role.HCFS_CLIENT));
    // Checking blocker roles
    assertFalse(dependenciesContainBlockerRole(deps, Role.PEERSTATUS));
    assertFalse(dependenciesContainBlockerRole(deps, Role.HCFS_SERVICE_CHECK));
    assertFalse(dependenciesContainBlockerRole(deps, Role.HCFS_CLIENT));

    // And that some HDFS components are present (section has been loaded)
    assertTrue(dependenciesContainBlockerRole(deps, Role.DATANODE));
    // Check that some HA NN dependencies are present
    assertTrue(dependenciesContainBlockerRole(deps, Role.JOURNALNODE));
    assertTrue(dependenciesContainBlockedRole(deps, Role.ZKFC));
  }


  @Test
  public void testAddDependencies() throws IOException {
    RoleCommandOrder rco = injector.getInstance(RoleCommandOrder.class);

    InputStream testJsonIS = getClass().getClassLoader().
            getResourceAsStream(TEST_RCO_DATA_FILE);
    
    ObjectMapper mapper = new ObjectMapper();
    Map<String,Object> testData = mapper.readValue(testJsonIS,
        new TypeReference<Map<String,Object>>() {});
        
    rco.addDependencies(testData);

    mapper.setVisibility(JsonMethod.ALL, JsonAutoDetect.Visibility.ANY);
    String dump = mapper.writeValueAsString(rco.getDependencies());
    
    String expected = "{\"RoleCommandPair{role=SECONDARY_NAMENODE, " +
        "cmd=UPGRADE}\":[{\"role\":{\"name\":\"NAMENODE\"},\"cmd\":\"UPGRADE\"}]," +
        "\"RoleCommandPair{role=SECONDARY_NAMENODE, cmd=START}\":[{\"role\":{\"name\":\"NAMENODE\"}," +
    		"\"cmd\":\"START\"}],\"RoleCommandPair{role=DATANODE, cmd=STOP}\":[{\"role\":" +
        "{\"name\":\"HBASE_MASTER\"},\"cmd\":\"STOP\"},{\"role\":{\"name\":\"RESOURCEMANAGER\"}," +
    		"\"cmd\":\"STOP\"},{\"role\":{\"name\":\"TASKTRACKER\"},\"cmd\":\"STOP\"}," +
        "{\"role\":{\"name\":\"NODEMANAGER\"},\"cmd\":\"STOP\"},{\"role\":{\"name\":\"HISTORYSERVER\"}," +
    		"\"cmd\":\"STOP\"},{\"role\":{\"name\":\"JOBTRACKER\"},\"cmd\":\"STOP\"}]}";

    assertEquals(expected, dump);
  }
  
  
  @Test
  public void testInitializeDefault() throws IOException {
    RoleCommandOrder rco = injector.getInstance(RoleCommandOrder.class);
    ClusterImpl cluster = createMock(ClusterImpl.class);
    expect(cluster.getService("HCFS")).andReturn(null);

    Service hdfsService = createMock(Service.class);

    expect(cluster.getService("HDFS")).andReturn(hdfsService).atLeastOnce();
    expect(hdfsService.getServiceComponent("JOURNALNODE")).andReturn(null);
    //There is no rco file in this stack, should use default
    expect(cluster.getCurrentStackVersion()).andReturn(new StackId("HDP", "2.0.5"));

    replay(cluster);
    replay(hdfsService);
    
    rco.initialize(cluster);
    
    verify(cluster);
    verify(hdfsService);
  }
  
  private boolean dependenciesContainBlockedRole(Map<RoleCommandPair,
          Set<RoleCommandPair>> deps, Role blocked) {
    for (RoleCommandPair blockedPair : deps.keySet()) {
      if (blockedPair.getRole() == blocked) {
        return true;
      }
    }
    return false;
  }

  private boolean dependenciesContainBlockerRole(Map<RoleCommandPair,
          Set<RoleCommandPair>> deps, Role blocker) {
    for(Set<RoleCommandPair> blockerSet: deps.values()) {
      for (RoleCommandPair roleCommandPair : blockerSet) {
        if (roleCommandPair.getRole() == blocker) {
          return true;
        }
      }
    }
    return false;
  }

}
