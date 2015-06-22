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

package org.apache.ambari.server;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import com.google.inject.util.Modules;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.easymock.Capture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

public class StateRecoveryManagerTest {

  private Injector injector;
  private HostVersionDAO hostVersionDAOMock;
  private ClusterVersionDAO clusterVersionDAOMock;

  @Before
  public void setup() throws Exception {
    // Create instances of mocks
    clusterVersionDAOMock = createNiceMock(ClusterVersionDAO.class);
    hostVersionDAOMock = createNiceMock(HostVersionDAO.class);
    // Initialize injector
    InMemoryDefaultTestModule module = new InMemoryDefaultTestModule();
    injector = Guice.createInjector(Modules.override(module).with(new MockModule()));
    injector.getInstance(GuiceJpaInitializer.class);
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testCheckHostAndClusterVersions() throws Exception {
    StateRecoveryManager stateRecoveryManager = injector.getInstance(StateRecoveryManager.class);

    // Adding all possible host version states

    final Capture<RepositoryVersionState> installFailedHostVersionCapture = new Capture<RepositoryVersionState>();
    final Capture<RepositoryVersionState> installingHostVersionCapture = new Capture<RepositoryVersionState>();
    final Capture<RepositoryVersionState> installedHostVersionCapture = new Capture<RepositoryVersionState>();
    final Capture<RepositoryVersionState> outOfSyncHostVersionCapture = new Capture<RepositoryVersionState>();
    final Capture<RepositoryVersionState> upgradeFailedHostVersionCapture = new Capture<RepositoryVersionState>();
    final Capture<RepositoryVersionState> upgradingHostVersionCapture = new Capture<RepositoryVersionState>();
    final Capture<RepositoryVersionState> upgradedHostVersionCapture = new Capture<RepositoryVersionState>();
    final Capture<RepositoryVersionState> currentHostVersionCapture = new Capture<RepositoryVersionState>();

    expect(hostVersionDAOMock.findAll()).andReturn(new ArrayList<HostVersionEntity>() {{
      add(getHostVersionMock("install_failed_version", RepositoryVersionState.INSTALL_FAILED, installFailedHostVersionCapture));
      add(getHostVersionMock("installing_version", RepositoryVersionState.INSTALLING, installingHostVersionCapture));
      add(getHostVersionMock("installed_version", RepositoryVersionState.INSTALLED, installedHostVersionCapture));
      add(getHostVersionMock("out_of_sync_version", RepositoryVersionState.OUT_OF_SYNC, outOfSyncHostVersionCapture));
      add(getHostVersionMock("upgrade_failed_version", RepositoryVersionState.UPGRADE_FAILED, upgradeFailedHostVersionCapture));
      add(getHostVersionMock("upgrading_version", RepositoryVersionState.UPGRADING, upgradingHostVersionCapture));
      add(getHostVersionMock("upgraded_version", RepositoryVersionState.UPGRADED, upgradedHostVersionCapture));
      add(getHostVersionMock("current_version", RepositoryVersionState.CURRENT, currentHostVersionCapture));
    }});

    // Adding all possible cluster version states

    final Capture<RepositoryVersionState> installFailedClusterVersionCapture = new Capture<RepositoryVersionState>();
    final Capture<RepositoryVersionState> installingClusterVersionCapture = new Capture<RepositoryVersionState>();
    final Capture<RepositoryVersionState> installedClusterVersionCapture = new Capture<RepositoryVersionState>();
    final Capture<RepositoryVersionState> outOfSyncClusterVersionCapture = new Capture<RepositoryVersionState>();
    final Capture<RepositoryVersionState> upgradeFailedClusterVersionCapture = new Capture<RepositoryVersionState>();
    final Capture<RepositoryVersionState> upgradingClusterVersionCapture = new Capture<RepositoryVersionState>();
    final Capture<RepositoryVersionState> upgradedClusterVersionCapture = new Capture<RepositoryVersionState>();
    final Capture<RepositoryVersionState> currentClusterVersionCapture = new Capture<RepositoryVersionState>();

    expect(clusterVersionDAOMock.findAll()).andReturn(new ArrayList<ClusterVersionEntity>() {{
      add(getClusterVersionMock("install_failed_version", RepositoryVersionState.INSTALL_FAILED, installFailedClusterVersionCapture));
      add(getClusterVersionMock("installing_version", RepositoryVersionState.INSTALLING, installingClusterVersionCapture));
      add(getClusterVersionMock("installed_version", RepositoryVersionState.INSTALLED, installedClusterVersionCapture));
      add(getClusterVersionMock("out_of_sync_version", RepositoryVersionState.OUT_OF_SYNC, outOfSyncClusterVersionCapture));
      add(getClusterVersionMock("upgrade_failed_version", RepositoryVersionState.UPGRADE_FAILED, upgradeFailedClusterVersionCapture));
      add(getClusterVersionMock("upgrading_version", RepositoryVersionState.UPGRADING, upgradingClusterVersionCapture));
      add(getClusterVersionMock("upgraded_version", RepositoryVersionState.UPGRADED, upgradedClusterVersionCapture));
      add(getClusterVersionMock("current_version", RepositoryVersionState.CURRENT, currentClusterVersionCapture));
    }});

    replay(hostVersionDAOMock, clusterVersionDAOMock);

    stateRecoveryManager.checkHostAndClusterVersions();

    // Checking that only invalid host version states have been changed
    assertFalse(installFailedHostVersionCapture.hasCaptured());
    assertEquals(installingHostVersionCapture.getValue(), RepositoryVersionState.INSTALL_FAILED);
    assertFalse(installedHostVersionCapture.hasCaptured());
    assertFalse(outOfSyncHostVersionCapture.hasCaptured());
    assertFalse(upgradeFailedHostVersionCapture.hasCaptured());
    assertFalse(upgradingHostVersionCapture.hasCaptured());
    assertFalse(upgradedHostVersionCapture.hasCaptured());
    assertFalse(currentHostVersionCapture.hasCaptured());

    // Checking that only invalid cluster version states have been changed
    assertFalse(installFailedClusterVersionCapture.hasCaptured());
    assertEquals(installingClusterVersionCapture.getValue(), RepositoryVersionState.INSTALL_FAILED);
    assertFalse(installedClusterVersionCapture.hasCaptured());
    assertFalse(outOfSyncClusterVersionCapture.hasCaptured());
    assertFalse(upgradeFailedClusterVersionCapture.hasCaptured());
    assertFalse(upgradingClusterVersionCapture.hasCaptured());
    assertFalse(upgradedClusterVersionCapture.hasCaptured());
    assertFalse(currentClusterVersionCapture.hasCaptured());
  }


  private HostVersionEntity getHostVersionMock(String name, RepositoryVersionState state,
                                               Capture<RepositoryVersionState> newStateCaptor) {
    HostVersionEntity hvMock = createNiceMock(HostVersionEntity.class);
    expect(hvMock.getState()).andReturn(state);

    hvMock.setState(capture(newStateCaptor));
    expectLastCall();

    RepositoryVersionEntity rvMock = createNiceMock(RepositoryVersionEntity.class);
    expect(rvMock.getDisplayName()).andReturn(name);

    expect(hvMock.getRepositoryVersion()).andReturn(rvMock);
    expect(hvMock.getHostName()).andReturn("somehost");

    replay(hvMock, rvMock);

    return hvMock;
  }


  private ClusterVersionEntity getClusterVersionMock(String name, RepositoryVersionState state,
                                               Capture<RepositoryVersionState> newStateCaptor) {
    ClusterVersionEntity cvMock = createNiceMock(ClusterVersionEntity.class);
    expect(cvMock.getState()).andReturn(state);

    cvMock.setState(capture(newStateCaptor));
    expectLastCall();

    RepositoryVersionEntity rvMock = createNiceMock(RepositoryVersionEntity.class);
    expect(rvMock.getDisplayName()).andReturn(name);

    expect(cvMock.getRepositoryVersion()).andReturn(rvMock);

    ClusterEntity ceMock = createNiceMock(ClusterEntity.class);
    expect(ceMock.getClusterName()).andReturn("somecluster");

    expect(cvMock.getClusterEntity()).andReturn(ceMock);

    replay(cvMock, rvMock, ceMock);

    return cvMock;
  }

  public class MockModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(HostVersionDAO.class).toInstance(hostVersionDAOMock);
      bind(ClusterVersionDAO.class).toInstance(clusterVersionDAOMock);
    }
  }

}