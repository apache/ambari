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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;

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
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import com.google.inject.util.Modules;

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

    final Capture<RepositoryVersionState> installFailedHostVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> installingHostVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> installedHostVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> outOfSyncHostVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> upgradeFailedHostVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> upgradingHostVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> upgradedHostVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> currentHostVersionCapture = EasyMock.newCapture();

    expect(hostVersionDAOMock.findAll()).andReturn(new ArrayList<HostVersionEntity>() {{
      add(getHostVersionMock("install_failed_version", RepositoryVersionState.INSTALL_FAILED, installFailedHostVersionCapture));
      add(getHostVersionMock("installing_version", RepositoryVersionState.INSTALLING, installingHostVersionCapture));
      add(getHostVersionMock("installed_version", RepositoryVersionState.INSTALLED, installedHostVersionCapture));
      add(getHostVersionMock("out_of_sync_version", RepositoryVersionState.OUT_OF_SYNC, outOfSyncHostVersionCapture));
      add(getHostVersionMock("current_version", RepositoryVersionState.CURRENT, currentHostVersionCapture));
    }});

    // Adding all possible cluster version states

    final Capture<RepositoryVersionState> installFailedClusterVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> installingClusterVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> installedClusterVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> outOfSyncClusterVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> upgradeFailedClusterVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> upgradingClusterVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> upgradedClusterVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> currentClusterVersionCapture = EasyMock.newCapture();

    expect(clusterVersionDAOMock.findAll()).andReturn(new ArrayList<ClusterVersionEntity>() {{
      add(getClusterVersionMock("install_failed_version", RepositoryVersionState.INSTALL_FAILED, installFailedClusterVersionCapture));
      add(getClusterVersionMock("installing_version", RepositoryVersionState.INSTALLING, installingClusterVersionCapture));
      add(getClusterVersionMock("installed_version", RepositoryVersionState.INSTALLED, installedClusterVersionCapture));
      add(getClusterVersionMock("out_of_sync_version", RepositoryVersionState.OUT_OF_SYNC, outOfSyncClusterVersionCapture));
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
