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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.RollbackException;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import com.google.inject.util.Modules;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.events.HostAddedEvent;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
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

  private Clusters clusters;
  private Cluster c1;
  private Injector injector;
  private OrmTestHelper helper;
  private HostVersionDAO hostVersionDAO;
  private AmbariMetaInfo metaInfo;


  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    metaInfo = injector.getInstance(AmbariMetaInfo.class);
    helper = injector.getInstance(OrmTestHelper.class);
    hostVersionDAO = injector.getInstance(HostVersionDAO.class);
    metaInfo.init();
    clusters.addCluster("c1");
    c1 = clusters.getCluster("c1");
    addHost("h1");

    StackId stackId = new StackId("HDP-0.1");
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
   * When a host is added to a cluster that has non-CURRENT cluster_version records,
   * then Ambari needs to insert host_verion record for each one of
   * those stack versions with a state of OUT_OF_SYNC
   */
  @Test
  public void testOnHostEvent() throws AmbariException {
    // Configuring single-node cluster with 2 repo versions
    Host h1 = clusters.getHost("h1");
    h1.setState(HostState.HEALTHY);

    StackId stackId = new StackId("HDP-0.1");
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
