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

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.ClusterStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.entities.ClusterStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.HostComponentStateEntityPK;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

public class ClustersTest {

  private Clusters clusters;
  private Injector injector;
  @Inject
  private AmbariMetaInfo metaInfo;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    injector.injectMembers(this);
    metaInfo.init();
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }
  
  private void setOsFamily(Host host, String osFamily, String osVersion) {
	    Map<String, String> hostAttributes = new HashMap<String, String>();
	    hostAttributes.put("os_family", osFamily);
	    hostAttributes.put("os_release_version", osVersion);
	    
	    host.setHostAttributes(hostAttributes);
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
  public void testAddAndGetCluster() throws AmbariException {

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

    Map<String, Cluster> verifyClusters = clusters.getClusters();
    Assert.assertTrue(verifyClusters.containsKey(c1));
    Assert.assertTrue(verifyClusters.containsKey(c2));
    Assert.assertNotNull(verifyClusters.get(c1));
    Assert.assertNotNull(verifyClusters.get(c2));

    Cluster c = clusters.getCluster(c1);
    c.setClusterName("foobar");
    long cId = c.getClusterId();

    Cluster changed = clusters.getCluster("foobar");
    Assert.assertNotNull(changed);
    Assert.assertEquals(cId, changed.getClusterId());

    Assert.assertEquals("foobar",
        clusters.getClusterById(cId).getClusterName());

  }


  @Test
  public void testAddAndGetHost() throws AmbariException {
    String h1 = "h1";
    String h2 = "h2";
    String h3 = "h3";

    clusters.addHost(h1);

    try {
      clusters.addHost(h1);
      fail("Expected exception on duplicate host entry");
    } catch (Exception e) {
      // Expected
    }

    clusters.addHost(h2);
    clusters.addHost(h3);

    List<Host> hosts = clusters.getHosts();
    Assert.assertEquals(3, hosts.size());

    Assert.assertNotNull(clusters.getHost(h1));
    Assert.assertNotNull(clusters.getHost(h2));
    Assert.assertNotNull(clusters.getHost(h3));

    Host h = clusters.getHost(h2);
    Assert.assertNotNull(h);

    try {
      clusters.getHost("foo");
      fail("Expected error for unknown host");
    } catch (HostNotFoundException e) {
      // Expected
    }

  }

  @Test
  public void testClusterHostMapping() throws AmbariException {
    String c1 = "c1";
    String c2 = "c2";
    String h1 = "h1";
    String h2 = "h2";
    String h3 = "h3";

    try {
      clusters.mapHostToCluster(h1, c1);
      fail("Expected exception for invalid cluster/host");
    } catch (Exception e) {
      // Expected
    }

    clusters.addCluster(c1);
    clusters.addCluster(c2);
    clusters.getCluster(c1).setDesiredStackVersion(new StackId("HDP-0.1"));
    clusters.getCluster(c2).setDesiredStackVersion(new StackId("HDP-0.1"));
    Assert.assertNotNull(clusters.getCluster(c1));
    Assert.assertNotNull(clusters.getCluster(c2));
    try {
      clusters.mapHostToCluster(h1, c1);
      fail("Expected exception for invalid host");
    } catch (Exception e) {
      // Expected
    }

    clusters.addHost(h1);
    clusters.addHost(h2);
    clusters.addHost(h3);
    Assert.assertNotNull(clusters.getHost(h1));
    setOsFamily(clusters.getHost(h1), "redhat", "6.4");
    setOsFamily(clusters.getHost(h2), "redhat", "5.9");
    setOsFamily(clusters.getHost(h3), "redhat", "6.4");
    clusters.getHost(h1).persist();
    clusters.getHost(h2).persist();
    clusters.getHost(h3).persist();

    Set<Cluster> c = clusters.getClustersForHost(h3);
    Assert.assertEquals(0, c.size());

    clusters.mapHostToCluster(h1, c1);
    clusters.mapHostToCluster(h2, c1);
    
    try {
      clusters.mapHostToCluster(h1, c1);
      fail("Expected exception for duplicate");
    } catch (DuplicateResourceException e) {
      // expected
    }
    
    /* make sure 2 host mapping to same cluster are the same cluster objects */
    
    Cluster c3 = (Cluster) clusters.getClustersForHost(h1).toArray()[0];
    Cluster c4 = (Cluster) clusters.getClustersForHost(h2).toArray()[0];
    
    Assert.assertEquals(c3, c4);
    Set<String> hostnames = new HashSet<String>();
    hostnames.add(h1);
    hostnames.add(h2);

    clusters.mapHostsToCluster(hostnames, c2);

    c = clusters.getClustersForHost(h1);
    Assert.assertEquals(2, c.size());
    
    c = clusters.getClustersForHost(h2);
    Assert.assertEquals(2, c.size());


    // TODO write test for getHostsForCluster
    Map<String, Host> hostsForC1 = clusters.getHostsForCluster(c1);
    Assert.assertEquals(2, hostsForC1.size());
    Assert.assertTrue(hostsForC1.containsKey(h1));
    Assert.assertTrue(hostsForC1.containsKey(h2));
    Assert.assertNotNull(hostsForC1.get(h1));
    Assert.assertNotNull(hostsForC1.get(h2));
  }

  @Test
  public void testDebugDump() throws AmbariException {
    String c1 = "c1";
    String c2 = "c2";
    String h1 = "h1";
    String h2 = "h2";
    String h3 = "h3";
    clusters.addCluster(c1);
    clusters.addCluster(c2);
    clusters.getCluster(c1).setDesiredStackVersion(new StackId("HDP-0.1"));
    clusters.getCluster(c2).setDesiredStackVersion(new StackId("HDP-0.1"));
    clusters.addHost(h1);
    clusters.addHost(h2);
    clusters.addHost(h3);
    setOsFamily(clusters.getHost(h1), "redhat", "6.4");
    setOsFamily(clusters.getHost(h2), "redhat", "5.9");
    setOsFamily(clusters.getHost(h3), "redhat", "6.4");
    clusters.getHost(h1).persist();
    clusters.getHost(h2).persist();
    clusters.getHost(h3).persist();
    clusters.mapHostToCluster(h1, c1);
    clusters.mapHostToCluster(h2, c1);

    StringBuilder sb = new StringBuilder();
    clusters.debugDump(sb);
    // TODO verify dump output?
  }

  @Test
  public void testDeleteCluster() throws Exception {
    String c1 = "c1";
    final String h1 = "h1";
    final String h2 = "h2";

    clusters.addCluster(c1);

    Cluster cluster = clusters.getCluster(c1);
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));
    cluster.setCurrentStackVersion(new StackId("HDP-0.1"));

    final Config config1 = injector.getInstance(ConfigFactory.class).createNew(cluster, "t1",
        new HashMap<String, String>() {{
          put("prop1", "val1");
        }}, new HashMap<String, Map<String,String>>());
    config1.setTag("1");
    config1.persist();
    
    Config config2 = injector.getInstance(ConfigFactory.class).createNew(cluster, "t1",
        new HashMap<String, String>() {{
          put("prop2", "val2");
        }}, new HashMap<String, Map<String,String>>());
    config2.setTag("2");
    config2.persist();
    
    // cluster desired config
    cluster.addDesiredConfig("_test", Collections.singleton(config1));

    clusters.addHost(h1);
    clusters.addHost(h2);

    Host host1 = clusters.getHost(h1);
    Host host2 = clusters.getHost(h2);
    setOsFamily(clusters.getHost(h1), "centos", "5.9");
    setOsFamily(clusters.getHost(h2), "centos", "5.9");
    host1.persist();
    host2.persist();

    clusters.mapHostsToCluster(new HashSet<String>() {
      {
        addAll(Arrays.asList(h1, h2));
      }
    }, c1);

    // host config override
    host1.addDesiredConfig(cluster.getClusterId(), true, "_test", config2);
    host1.persist();

    Service hdfs = cluster.addService("HDFS");
    hdfs.persist();
    
    Assert.assertNotNull(injector.getInstance(ClusterServiceDAO.class).findByClusterAndServiceNames(c1, "HDFS"));

    ServiceComponent nameNode = hdfs.addServiceComponent("NAMENODE");
    nameNode.persist();
    ServiceComponent dataNode = hdfs.addServiceComponent("DATANODE");
    dataNode.persist();
    
    ServiceComponent serviceCheckNode = hdfs.addServiceComponent("HDFS_CLIENT");
    serviceCheckNode.persist();

    ServiceComponentHost nameNodeHost = nameNode.addServiceComponentHost(h1);
    nameNodeHost.persist();

    ServiceComponentHost dataNodeHost = dataNode.addServiceComponentHost(h2);
    dataNodeHost.persist();
    
    ServiceComponentHost serviceCheckNodeHost = serviceCheckNode.addServiceComponentHost(h2);
    serviceCheckNodeHost.persist();
    serviceCheckNodeHost.setState(State.UNKNOWN);

    HostComponentStateEntityPK hkspk = new HostComponentStateEntityPK();
    HostComponentDesiredStateEntityPK hkdspk = new HostComponentDesiredStateEntityPK();

    hkspk.setClusterId(nameNodeHost.getClusterId());
    hkspk.setHostName(nameNodeHost.getHostName());
    hkspk.setServiceName(nameNodeHost.getServiceName());
    hkspk.setComponentName(nameNodeHost.getServiceComponentName());

    hkdspk.setClusterId(nameNodeHost.getClusterId());
    hkdspk.setHostName(nameNodeHost.getHostName());
    hkdspk.setServiceName(nameNodeHost.getServiceName());
    hkdspk.setComponentName(nameNodeHost.getServiceComponentName());

    Assert.assertNotNull(injector.getInstance(HostComponentStateDAO.class).findByPK(hkspk));
    Assert.assertNotNull(injector.getInstance(HostComponentDesiredStateDAO.class).findByPK(hkdspk));
    Assert.assertEquals(2, injector.getProvider(EntityManager.class).get().createQuery("SELECT config FROM ClusterConfigEntity config").getResultList().size());
    Assert.assertEquals(1, injector.getProvider(EntityManager.class).get().createQuery("SELECT state FROM ClusterStateEntity state").getResultList().size());
    Assert.assertEquals(1, injector.getProvider(EntityManager.class).get().createQuery("SELECT config FROM ClusterConfigMappingEntity config").getResultList().size());
    
    clusters.deleteCluster(c1);

    Assert.assertEquals(2, injector.getInstance(HostDAO.class).findAll().size());
    Assert.assertNull(injector.getInstance(HostComponentStateDAO.class).findByPK(hkspk));
    Assert.assertNull(injector.getInstance(HostComponentDesiredStateDAO.class).findByPK(hkdspk));
    Assert.assertEquals(0, injector.getProvider(EntityManager.class).get().createQuery("SELECT config FROM ClusterConfigEntity config").getResultList().size());
    Assert.assertEquals(0, injector.getProvider(EntityManager.class).get().createQuery("SELECT state FROM ClusterStateEntity state").getResultList().size());
    Assert.assertEquals(0, injector.getProvider(EntityManager.class).get().createQuery("SELECT config FROM ClusterConfigMappingEntity config").getResultList().size());
    
  }
  @Test
  public void testSetCurrentStackVersion() throws AmbariException {

    String c1 = "foo3";

    try
    {
      clusters.setCurrentStackVersion("", null);
      fail("Exception should be thrown on invalid set");
    }
      catch (AmbariException e) {
      // Expected
    }

    try
    {
      clusters.setCurrentStackVersion(c1, null);
      fail("Exception should be thrown on invalid set");
    }
    catch (AmbariException e) {
      // Expected
    }

    StackId stackId = new StackId("HDP-0.1");

    try
    {
      clusters.setCurrentStackVersion(c1, stackId);
      fail("Exception should be thrown on invalid set");
    }
    catch (AmbariException e) {
      // Expected
      Assert.assertTrue(e.getMessage().contains("Cluster not found"));
    }

    clusters.addCluster(c1);
    clusters.setCurrentStackVersion(c1, stackId);

    Assert.assertNotNull(clusters.getCluster(c1));
    ClusterStateEntity entity = injector.getInstance(ClusterStateDAO.class).findByPK(clusters.getCluster(c1).getClusterId());
    Assert.assertNotNull(entity);
    Assert.assertTrue(entity.getCurrentStackVersion().contains(stackId.getStackName()) &&
        entity.getCurrentStackVersion().contains(stackId.getStackVersion()));
    Assert.assertTrue(clusters.getCluster(c1).getCurrentStackVersion().getStackName().equals(stackId.getStackName()));
    Assert.assertTrue(
        clusters.getCluster(c1).getCurrentStackVersion().getStackVersion().equals(stackId.getStackVersion()));
  }
}
