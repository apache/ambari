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

package org.apache.ambari.server.orm.dao;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.entities.*;
import org.apache.ambari.server.state.ClusterVersionState;
import org.apache.ambari.server.state.UpgradeState;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * {@link org.apache.ambari.server.orm.dao.HostVersionDAO} unit tests.
 */
public class HostVersionDAOTest {

  private static Injector injector;
  private ResourceTypeDAO resourceTypeDAO;
  private ClusterDAO clusterDAO;
  private ClusterVersionDAO clusterVersionDAO;
  private HostDAO hostDAO;
  private HostVersionDAO hostVersionDAO;

  @Before
  public void before() {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    resourceTypeDAO = injector.getInstance(ResourceTypeDAO.class);
    clusterDAO = injector.getInstance(ClusterDAO.class);
    clusterVersionDAO = injector.getInstance(ClusterVersionDAO.class);
    hostDAO = injector.getInstance(HostDAO.class);
    hostVersionDAO = injector.getInstance(HostVersionDAO.class);

    createDefaultData();
  }

  /**
   * Helper function to bootstrap some basic data about clusters, cluster version, host, and host versions.
   */
  private void createDefaultData() {
    // Create the cluster
    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE);
    resourceTypeEntity.setName(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE_NAME);
    resourceTypeEntity = resourceTypeDAO.merge(resourceTypeEntity);

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterName("test_cluster1");
    clusterEntity.setClusterInfo("test_cluster_info1");
    clusterEntity.setResource(resourceEntity);
    clusterDAO.create(clusterEntity);

    // Create the Cluster Version and link it to the cluster
    ClusterVersionEntity clusterVersionEntity = new ClusterVersionEntity(clusterEntity, "HDP", "2.2.0.0-995", ClusterVersionState.CURRENT, System.currentTimeMillis(), System.currentTimeMillis(), "admin");
    List<ClusterVersionEntity> clusterVersionEntities = new ArrayList<ClusterVersionEntity>();
    clusterVersionEntities.add(clusterVersionEntity);
    clusterEntity.setClusterVersionEntities(clusterVersionEntities);

    clusterVersionDAO.create(clusterVersionEntity);
    clusterDAO.merge(clusterEntity);

    // Create the hosts
    HostEntity host1 = new HostEntity();
    HostEntity host2 = new HostEntity();
    HostEntity host3 = new HostEntity();

    host1.setHostName("test_host1");
    host2.setHostName("test_host2");
    host3.setHostName("test_host3");
    host1.setIpv4("192.168.0.1");
    host2.setIpv4("192.168.0.2");
    host3.setIpv4("192.168.0.3");

    List<HostEntity> hostEntities = new ArrayList<HostEntity>();
    hostEntities.add(host1);
    hostEntities.add(host2);
    hostEntities.add(host3);

    clusterEntity.setHostEntities(hostEntities);
    // Both sides of relation should be set when modifying in runtime
    host1.setClusterEntities(Arrays.asList(clusterEntity));
    host2.setClusterEntities(Arrays.asList(clusterEntity));
    host2.setClusterEntities(Arrays.asList(clusterEntity));

    hostDAO.create(host1);
    hostDAO.create(host2);
    hostDAO.create(host3);
    clusterDAO.merge(clusterEntity);

    // Create the Host Versions
    HostVersionEntity hostVersionEntity1 = new HostVersionEntity(host1.getHostName(), clusterVersionEntity.getStack(), clusterVersionEntity.getVersion(), UpgradeState.NONE);
    HostVersionEntity hostVersionEntity2 = new HostVersionEntity(host2.getHostName(), clusterVersionEntity.getStack(), clusterVersionEntity.getVersion(), UpgradeState.NONE);
    HostVersionEntity hostVersionEntity3 = new HostVersionEntity(host3.getHostName(), clusterVersionEntity.getStack(), clusterVersionEntity.getVersion(), UpgradeState.NONE);
    hostVersionEntity1.setHostEntity(host1);
    hostVersionEntity2.setHostEntity(host2);
    hostVersionEntity3.setHostEntity(host3);

    hostVersionDAO.create(hostVersionEntity1);
    hostVersionDAO.create(hostVersionEntity2);
    hostVersionDAO.create(hostVersionEntity3);
  }

  /**
   * Helper function to bootstrap additional data on top of the default data.
   */
  private void addMoreVersions() {
    ClusterEntity clusterEntity = clusterDAO.findByName("test_cluster1");

    // Create another Cluster Version and mark the old one as INSTALLED
    if (clusterEntity.getClusterVersionEntities() != null && clusterEntity.getClusterVersionEntities().size() > 0) {
      ClusterVersionEntity installedClusterVersion = clusterVersionDAO.findByClusterAndStateCurrent(clusterEntity.getClusterName());
      installedClusterVersion.setState(ClusterVersionState.INSTALLED);
      clusterVersionDAO.merge(installedClusterVersion);
    } else {
      Assert.fail("Cluster is expected to have at least one cluster version");
    }

    ClusterVersionEntity newClusterVersionEntity = new ClusterVersionEntity(clusterEntity, "HDP", "2.2.0.1-996", ClusterVersionState.CURRENT, System.currentTimeMillis(), System.currentTimeMillis(), "admin");
    clusterEntity.addClusterVersionEntity(newClusterVersionEntity);
    clusterVersionDAO.create(newClusterVersionEntity);

    HostEntity[] hostEntities = clusterEntity.getHostEntities().toArray(new HostEntity[clusterEntity.getHostEntities().size()]);
    // Must sort by host name in ascending order to ensure that state is accurately set later on.
    Arrays.sort(hostEntities);

    // For each of the hosts, add a host version
    for (HostEntity host : hostEntities) {
      HostVersionEntity hostVersionEntity = new HostVersionEntity(host.getHostName(), "HDP", "2.2.0.1-996", UpgradeState.NONE);
      hostVersionEntity.setHostEntity(host);
      hostVersionDAO.create(hostVersionEntity);
    }

    // For each of the hosts, add one more host version
    for (int i = 0; i < hostEntities.length; i++) {
      UpgradeState desiredState = UpgradeState.NONE;
      if (i % 3 == 0) {
        desiredState = UpgradeState.PENDING;
      }
      if (i % 3 == 1) {
        desiredState = UpgradeState.IN_PROGRESS;
      }
      if (i % 3 == 2) {
        desiredState = UpgradeState.FAILED;
      }

      HostVersionEntity hostVersionEntity = new HostVersionEntity(hostEntities[i].getHostName(), "HDP", "2.2.1.0-500", desiredState);
      hostVersionEntity.setHostEntity(hostEntities[i]);
      hostVersionDAO.create(hostVersionEntity);
    }
  }

  /**
   * Test the {@link HostVersionDAO#findAll()} method.
   */
  @Test
  public void testFindAll() {
    Assert.assertEquals(3, hostVersionDAO.findAll().size());
  }

  /**
   * Test the {@link HostVersionDAO#findByClusterStackAndVersion(String, String, String)} method.
   */
  @Test
  public void testFindByClusterStackAndVersion() {
    Assert.assertEquals(3, hostVersionDAO.findByClusterStackAndVersion("test_cluster1", "HDP", "2.2.0.0-995").size());
    Assert.assertEquals(3, hostVersionDAO.findAll().size());

    addMoreVersions();

    Assert.assertEquals(3, hostVersionDAO.findByClusterStackAndVersion("test_cluster1", "HDP", "2.2.0.1-996").size());
    Assert.assertEquals(3, hostVersionDAO.findByClusterStackAndVersion("test_cluster1", "HDP", "2.2.1.0-500").size());
    Assert.assertEquals(9, hostVersionDAO.findAll().size());
  }

  /**
   * Test the {@link HostVersionDAO#findByClusterAndHost(String, String)} method.
   */
  @Test
  public void testFindByClusterAndHost() {
    Assert.assertEquals(1, hostVersionDAO.findByClusterAndHost("test_cluster1", "test_host1").size());
    Assert.assertEquals(1, hostVersionDAO.findByClusterAndHost("test_cluster1", "test_host2").size());
    Assert.assertEquals(1, hostVersionDAO.findByClusterAndHost("test_cluster1", "test_host3").size());

    addMoreVersions();

    Assert.assertEquals(3, hostVersionDAO.findByClusterAndHost("test_cluster1", "test_host1").size());
    Assert.assertEquals(3, hostVersionDAO.findByClusterAndHost("test_cluster1", "test_host2").size());
    Assert.assertEquals(3, hostVersionDAO.findByClusterAndHost("test_cluster1", "test_host3").size());
  }

  /**
   * Test the {@link HostVersionDAO#findByClusterHostAndState(String, String, org.apache.ambari.server.state.UpgradeState)} method.
   */
  @Test
  public void testFindByClusterHostAndState() {
    Assert.assertEquals(1, hostVersionDAO.findByClusterHostAndState("test_cluster1", "test_host1", UpgradeState.NONE).size());
    Assert.assertEquals(0, hostVersionDAO.findByClusterHostAndState("test_cluster1", "test_host1", UpgradeState.PENDING).size());
    Assert.assertEquals(0, hostVersionDAO.findByClusterHostAndState("test_cluster1", "test_host2", UpgradeState.IN_PROGRESS).size());
    Assert.assertEquals(0, hostVersionDAO.findByClusterHostAndState("test_cluster1", "test_host3", UpgradeState.FAILED).size());

    addMoreVersions();

    Assert.assertEquals(2, hostVersionDAO.findByClusterHostAndState("test_cluster1", "test_host1", UpgradeState.NONE).size());
    Assert.assertEquals(2, hostVersionDAO.findByClusterHostAndState("test_cluster1", "test_host2", UpgradeState.NONE).size());
    Assert.assertEquals(2, hostVersionDAO.findByClusterHostAndState("test_cluster1", "test_host3", UpgradeState.NONE).size());

    Assert.assertEquals(1, hostVersionDAO.findByClusterHostAndState("test_cluster1", "test_host1", UpgradeState.PENDING).size());
    Assert.assertEquals(1, hostVersionDAO.findByClusterHostAndState("test_cluster1", "test_host2", UpgradeState.IN_PROGRESS).size());
    Assert.assertEquals(1, hostVersionDAO.findByClusterHostAndState("test_cluster1", "test_host3", UpgradeState.FAILED).size());
  }

  /**
   * Test the {@link HostVersionDAO#findByClusterStackVersionAndHost(String, String, String, String)} method.
   */
  @Test
  public void testFindByClusterStackVersionAndHost() {
    HostVersionEntity hostVersionEntity1 = new HostVersionEntity("test_host1", "HDP", "2.2.0.0-995", UpgradeState.NONE);
    hostVersionEntity1.setId(1L);
    HostVersionEntity hostVersionEntity2 = new HostVersionEntity("test_host2", "HDP", "2.2.0.0-995", UpgradeState.NONE);
    hostVersionEntity2.setId(2L);
    HostVersionEntity hostVersionEntity3 = new HostVersionEntity("test_host3", "HDP", "2.2.0.0-995", UpgradeState.NONE);
    hostVersionEntity3.setId(3L);

    Assert.assertEquals(hostVersionEntity1, hostVersionDAO.findByClusterStackVersionAndHost("test_cluster1", "HDP", "2.2.0.0-995", "test_host1"));
    Assert.assertEquals(hostVersionEntity2, hostVersionDAO.findByClusterStackVersionAndHost("test_cluster1", "HDP", "2.2.0.0-995", "test_host2"));
    Assert.assertEquals(hostVersionEntity3, hostVersionDAO.findByClusterStackVersionAndHost("test_cluster1", "HDP", "2.2.0.0-995", "test_host3"));

    // Test non-existent objects
    Assert.assertEquals(null, hostVersionDAO.findByClusterStackVersionAndHost("non_existent_cluster", "HDP", "2.2.0.0-995", "test_host3"));
    Assert.assertEquals(null, hostVersionDAO.findByClusterStackVersionAndHost("test_cluster1", "non_existent_stack", "2.2.0.0-995", "test_host3"));
    Assert.assertEquals(null, hostVersionDAO.findByClusterStackVersionAndHost("test_cluster1", "HDP", "non_existent_version", "test_host3"));
    Assert.assertEquals(null, hostVersionDAO.findByClusterStackVersionAndHost("test_cluster1", "HDP", "non_existent_version", "non_existent_host"));

    addMoreVersions();

    // Expected
    HostVersionEntity hostVersionEntity1LastExpected = new HostVersionEntity("test_host1", "HDP", "2.2.1.0-500", UpgradeState.PENDING);
    HostVersionEntity hostVersionEntity2LastExpected = new HostVersionEntity("test_host2", "HDP", "2.2.1.0-500", UpgradeState.IN_PROGRESS);
    HostVersionEntity hostVersionEntity3LastExpected = new HostVersionEntity("test_host3", "HDP", "2.2.1.0-500", UpgradeState.FAILED);

    // Actual
    HostVersionEntity hostVersionEntity1LastActual = hostVersionDAO.findByClusterStackVersionAndHost("test_cluster1", "HDP", "2.2.1.0-500", "test_host1");
    HostVersionEntity hostVersionEntity2LastActual = hostVersionDAO.findByClusterStackVersionAndHost("test_cluster1", "HDP", "2.2.1.0-500", "test_host2");
    HostVersionEntity hostVersionEntity3LastActual = hostVersionDAO.findByClusterStackVersionAndHost("test_cluster1", "HDP", "2.2.1.0-500", "test_host3");

    // Trying to Mock the actual objects to override the getId() method will not work because the class that mockito creates
    // is still a Mockito wrapper. Instead, take advantage of an overloaded constructor that ignores the Id.
    Assert.assertEquals(hostVersionEntity1LastExpected, new HostVersionEntity(hostVersionEntity1LastActual));
    Assert.assertEquals(hostVersionEntity2LastExpected, new HostVersionEntity(hostVersionEntity2LastActual));
    Assert.assertEquals(hostVersionEntity3LastExpected, new HostVersionEntity(hostVersionEntity3LastActual));
  }

  @After
  public void after() {
    injector.getInstance(PersistService.class).stop();
    injector = null;
  }
}