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

package org.apache.ambari.server.events.listeners.upgrade;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.StackId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

public class HostVersionOutOfSyncListenerTest {
  private static final Logger LOG = LoggerFactory.getLogger(HostVersionOutOfSyncListenerTest.class);
  private final String stackId = "HDP-0.1";

  private Clusters clusters;
  private Cluster c1;
  private Injector injector;
  private OrmTestHelper helper;
  private HostVersionDAO hostVersionDAO;
  private AmbariMetaInfo metaInfo;
  private ServiceComponentHostFactory serviceComponentHostFactory;


  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    metaInfo = injector.getInstance(AmbariMetaInfo.class);
    helper = injector.getInstance(OrmTestHelper.class);
    hostVersionDAO = injector.getInstance(HostVersionDAO.class);
    serviceComponentHostFactory = injector.getInstance(ServiceComponentHostFactory.class);
    clusters.addCluster("c1");
    c1 = clusters.getCluster("c1");
    addHost("h1");

    StackId stackId = new StackId(this.stackId);
    c1.setDesiredStackVersion(stackId);
    helper.getOrCreateRepositoryVersion(stackId.getStackName(), stackId.getStackVersion());
    c1.createClusterVersion(stackId.getStackName(), stackId.getStackVersion(), "admin", RepositoryVersionState.UPGRADING);
    c1.transitionClusterVersion(stackId.getStackName(), stackId.getStackVersion(), RepositoryVersionState.CURRENT);
    clusters.mapHostToCluster("h1", "c1");
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }

  /**
   * When a service is added to a cluster, all non-CURRENT host versions on
   * all affected hosts (where host new components are installed)
   * should transition to OUT_OF_SYNC state
   */
  @Test
  public void testOnServiceEvent() throws AmbariException {
    // Configuring 3-node cluster with 2 repo versions
    String CURRENT_VERSION = "1.0-2086";
    String INSTALLED_VERSION = "1.0-1000";

    Host h1 = clusters.getHost("h1");
    h1.setState(HostState.HEALTHY);

    addHost("h2");
    clusters.mapHostToCluster("h2", "c1");
    addHost("h3");
    clusters.mapHostToCluster("h3", "c1");


    StackId stackId = new StackId(this.stackId);
    RepositoryVersionEntity repositoryVersionEntity = helper.getOrCreateRepositoryVersion(stackId.getStackId(),
            INSTALLED_VERSION);
    c1.createClusterVersion(stackId.getStackId(), INSTALLED_VERSION, "admin", RepositoryVersionState.INSTALLING);
    c1.setCurrentStackVersion(stackId);
    c1.recalculateAllClusterVersionStates();
    checkStackVersionState(stackId.getStackId(), INSTALLED_VERSION, RepositoryVersionState.INSTALLING);

    checkStackVersionState(stackId.getStackId(), CURRENT_VERSION, RepositoryVersionState.CURRENT);

    HostVersionEntity hv1 = helper.createHostVersion("h1", repositoryVersionEntity, RepositoryVersionState.INSTALLED);
    c1.recalculateAllClusterVersionStates();
    checkStackVersionState(stackId.getStackId(), INSTALLED_VERSION, RepositoryVersionState.INSTALLED);
    checkStackVersionState(stackId.getStackId(), CURRENT_VERSION, RepositoryVersionState.CURRENT);

    // Add new host and verify that it has all host versions present
    List<HostVersionEntity> h2Versions = hostVersionDAO.findAll();

    // Check before adding service
    for (HostVersionEntity hostVersionEntity : h2Versions) {
      if (hostVersionEntity.getRepositoryVersion().toString().equals(INSTALLED_VERSION)) {
        assertEquals(hostVersionEntity.getState(), RepositoryVersionState.INSTALLED);
      }
    }

    //Add HDFS service
    List<String> hostList = new ArrayList<String>();
    hostList.add("h1");
    hostList.add("h2");
    hostList.add("h3");
    Map<String, List<Integer>> hdfsTopology = new HashMap<String, List<Integer>>();
    hdfsTopology.put("NAMENODE", Collections.singletonList(0));
    hdfsTopology.put("SECONDARY_NAMENODE", Collections.singletonList(1));
    List<Integer> datanodeHosts = Arrays.asList(0, 1);
    hdfsTopology.put("DATANODE", new ArrayList<Integer>(datanodeHosts));
    addService(c1, hostList, hdfsTopology, "HDFS");

    // Check result
    Set<String> changedHosts = new HashSet<String>();
    changedHosts.add("h1");
    changedHosts.add("h2");

    checkStackVersionState(stackId.getStackId(), INSTALLED_VERSION, RepositoryVersionState.INSTALLED);
    for (HostVersionEntity hostVersionEntity : h2Versions) {
      if (hostVersionEntity.getRepositoryVersion().toString().equals(INSTALLED_VERSION)) {
        if (changedHosts.contains(hostVersionEntity.getHostName())) {
          assertEquals(hostVersionEntity.getState(), RepositoryVersionState.OUT_OF_SYNC);
        } else {
          assertEquals(hostVersionEntity.getState(), RepositoryVersionState.INSTALLED);
        }
      }
    }
  }


  /**
   * When a host is added to a cluster that has non-CURRENT cluster_version records,
   * then Ambari needs to insert host_verion record for each one of
   * those stack versions with a state of OUT_OF_SYNC
   */
  @Test
  public void testOnHostEvent() throws AmbariException {
    // Configuring single-node cluster with 2 repo versions
    Host h1 = clusters.getHost("h1");
    h1.setState(HostState.HEALTHY);

    StackId stackId = new StackId(this.stackId);
    RepositoryVersionEntity repositoryVersionEntity = helper.getOrCreateRepositoryVersion(stackId.getStackId(),
            "1.0-1000");
    c1.createClusterVersion(stackId.getStackId(), "1.0-1000", "admin", RepositoryVersionState.INSTALLING);
    c1.setCurrentStackVersion(stackId);
    c1.recalculateAllClusterVersionStates();
    checkStackVersionState(stackId.getStackId(), "1.0-1000", RepositoryVersionState.INSTALLING);
    checkStackVersionState(stackId.getStackId(), "1.0-2086", RepositoryVersionState.CURRENT);

    HostVersionEntity hv1 = helper.createHostVersion("h1", repositoryVersionEntity, RepositoryVersionState.INSTALLED);
    c1.recalculateAllClusterVersionStates();
    checkStackVersionState(stackId.getStackId(), "1.0-1000", RepositoryVersionState.INSTALLED);
    checkStackVersionState(stackId.getStackId(), "1.0-2086", RepositoryVersionState.CURRENT);

    // Add new host and verify that it has all host versions present
    addHost("h2");
    clusters.mapHostToCluster("h2", "c1");

    List<HostVersionEntity> h2Versions = hostVersionDAO.findByHost("h2");

    for (HostVersionEntity hostVersionEntity : h2Versions) {
      if (hostVersionEntity.getRepositoryVersion().toString().equals("1.0-2086")) {
        assertEquals(hostVersionEntity.getState(), RepositoryVersionState.CURRENT);
      } else {
        assertEquals(hostVersionEntity.getState(), RepositoryVersionState.OUT_OF_SYNC);
      }
    }
  }

  private void addHost(String hostname) throws AmbariException{
    clusters.addHost(hostname);

    Host host1 = clusters.getHost(hostname);
    host1.setIPv4("ipv4");
    host1.setIPv6("ipv6");

    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "5.9");
    host1.setHostAttributes(hostAttributes);

    host1.persist();
  }

  private void addService(Cluster cl, List<String> hostList,
                                Map<String, List<Integer>> topology, String serviceName
                          ) throws AmbariException {
    cl.setDesiredStackVersion(new StackId(stackId));
    cl.addService(serviceName);

    for (Map.Entry<String, List<Integer>> component : topology.entrySet()) {

      String componentName = component.getKey();
      cl.getService(serviceName).addServiceComponent(componentName);

      for (Integer hostIndex : component.getValue()) {
        cl.getService(serviceName)
                .getServiceComponent(componentName)
                .addServiceComponentHost(
                        serviceComponentHostFactory.createNew(cl.getService(serviceName)
                                .getServiceComponent(componentName), hostList.get(hostIndex)));
      }
    }
  }

  private void checkStackVersionState(String stack, String version, RepositoryVersionState state) {
    Collection<ClusterVersionEntity> allClusterVersions = c1.getAllClusterVersions();
    for (ClusterVersionEntity entity : allClusterVersions) {
      if (entity.getRepositoryVersion().getStack().equals(stack)
              && entity.getRepositoryVersion().getVersion().equals(version)) {
        assertEquals(state, entity.getState());
      }
    }
  }
}
