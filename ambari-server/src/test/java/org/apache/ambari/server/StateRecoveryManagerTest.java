/*
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
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.sql.SQLException;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.MpackHostStateDAO;
import org.apache.ambari.server.orm.entities.MpackHostStateEntity;
import org.apache.ambari.server.state.Mpack;
import org.apache.ambari.server.state.MpackInstallState;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

public class StateRecoveryManagerTest extends EasyMockSupport {

  private Injector injector;
  private MpackHostStateDAO mpackHostStateDAO;
  private AmbariMetaInfo ambariMetaInfo;

  @Before
  public void setup() throws Exception {
    // Create instances of mocks
    mpackHostStateDAO = createNiceMock(MpackHostStateDAO.class);
    ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);

    expect(ambariMetaInfo.getMpack(EasyMock.anyLong())).andReturn(
        EasyMock.createNiceMock(Mpack.class)).atLeastOnce();

    // Initialize injector
    InMemoryDefaultTestModule module = new InMemoryDefaultTestModule();
    injector = Guice.createInjector(Modules.override(module).with(new MockModule()));
    injector.getInstance(GuiceJpaInitializer.class);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  /**
   * Tests that the installation states are correctly reset on system startup.
   *
   * @throws Exception
   */
  @Test
  public void testMpackInstallState() throws Exception {
    StateRecoveryManager stateRecoveryManager = injector.getInstance(StateRecoveryManager.class);

    final Capture<MpackInstallState> installFailedCapture = EasyMock.newCapture();
    final Capture<MpackInstallState> installingCapture = EasyMock.newCapture();
    final Capture<MpackInstallState> installedCapture = EasyMock.newCapture();
    final Capture<MpackInstallState> notInstalledCapture = EasyMock.newCapture();

    expect(mpackHostStateDAO.findAll()).andReturn(Lists.newArrayList(
        getMpackInstallStateMock("install_failed", MpackInstallState.INSTALL_FAILED, installFailedCapture),
        getMpackInstallStateMock("installing", MpackInstallState.INSTALLING, installingCapture),
        getMpackInstallStateMock("installed", MpackInstallState.INSTALLED, installedCapture),
        getMpackInstallStateMock("not_installed", MpackInstallState.NOT_INSTALLED, notInstalledCapture)));


    replayAll();

    stateRecoveryManager.doWork();

    // Checking that only invalid host version states have been changed
    assertFalse(installFailedCapture.hasCaptured());
    assertEquals(installingCapture.getValue(), MpackInstallState.INSTALL_FAILED);
    assertFalse(installedCapture.hasCaptured());
    assertFalse(notInstalledCapture.hasCaptured());
  }


  /**
   * Creates a mock {@link MpackHostStateEntity} which can capture the
   * {@link MpackHostStateEntity#setState(MpackInstallState)} method.
   *
   * @param name
   * @param state
   * @param newStateCaptor
   * @return
   */
  private MpackHostStateEntity getMpackInstallStateMock(String name, MpackInstallState state,
      Capture<MpackInstallState> newStateCaptor) {
    MpackHostStateEntity mock = createNiceMock(MpackHostStateEntity.class);
    expect(mock.getState()).andReturn(state);

    mock.setState(capture(newStateCaptor));
    expectLastCall();

    expect(mock.getHostName()).andReturn("somehost");
    return mock;
  }

  public class MockModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(MpackHostStateDAO.class).toInstance(mpackHostStateDAO);
      bind(AmbariMetaInfo.class).toInstance(ambariMetaInfo);
    }
  }

}
