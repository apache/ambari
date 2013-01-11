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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.StackId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
    clusters.getHost(h1).setOsType("redhat6");
    clusters.getHost(h2).setOsType("centos5");
    clusters.getHost(h3).setOsType("centos6");
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
    clusters.getHost(h1).setOsType("redhat6");
    clusters.getHost(h2).setOsType("centos5");
    clusters.getHost(h3).setOsType("centos6");
    clusters.getHost(h1).persist();
    clusters.getHost(h2).persist();
    clusters.getHost(h3).persist();
    clusters.mapHostToCluster(h1, c1);
    clusters.mapHostToCluster(h2, c1);

    StringBuilder sb = new StringBuilder();
    clusters.debugDump(sb);
    // TODO verify dump output?
  }

}
