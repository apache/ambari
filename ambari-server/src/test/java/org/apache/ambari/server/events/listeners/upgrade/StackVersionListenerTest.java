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

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.lang.reflect.Field;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.events.HostComponentVersionAdvertisedEvent;
import org.apache.ambari.server.events.publishers.VersionEventPublisher;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.UpgradeState;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Provider;

import junit.framework.Assert;

/**
 * StackVersionListener tests.
 */
@RunWith(EasyMockRunner.class)
public class StackVersionListenerTest extends EasyMockSupport {

  private static final String INVALID_NEW_VERSION = "1.2.3.4-5678";
  private static final String VALID_NEW_VERSION = "2.4.0.0-1000";
  private static final String SERVICE_COMPONENT_NAME = "Some component name";
  private static final String SERVICE_NAME = "Service name";
  private static final Long CLUSTER_ID = 1L;
  private static final String UNKNOWN_VERSION = "UNKNOWN";
  private static final String VALID_PREVIOUS_VERSION = "2.2.0.0";
  private static final RepositoryVersionEntity DUMMY_REPOSITORY_VERSION_ENTITY = new RepositoryVersionEntity();
  private static final UpgradeEntity DUMMY_UPGRADE_ENTITY = new UpgradeEntity();
  public static final String STACK_NAME = "HDP-2.4.0.0";
  public static final String STACK_VERSION = "2.4.0.0";

  private Cluster cluster;
  private ServiceComponentHost sch;
  private Service service;
  private ServiceComponent serviceComponent;
  private VersionEventPublisher publisher = new VersionEventPublisher();
  private AmbariMetaInfo ambariMetaInfo;
  private ComponentInfo componentInfo;
  private StackId stackId;

  @TestSubject
  private StackVersionListener listener = new StackVersionListener(publisher);

  @Mock
  private Provider<AmbariMetaInfo> ambariMetaInfoProvider;

  @Before
  public void setup() throws Exception {
    cluster = createNiceMock(Cluster.class);
    sch = createNiceMock(ServiceComponentHost.class);
    service = createNiceMock(Service.class);
    serviceComponent = createNiceMock(ServiceComponent.class);
    componentInfo = createNiceMock(ComponentInfo.class);
    stackId = createNiceMock(StackId.class);

    ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);

    expect(ambariMetaInfoProvider.get()).andReturn(ambariMetaInfo);
    expect(ambariMetaInfo.getComponent(anyString(),anyString(),anyString(),anyString())).andReturn(componentInfo);

    expect(cluster.getDesiredStackVersion()).andReturn(stackId).atLeastOnce();
    expect(stackId.getStackName()).andReturn(STACK_NAME);
    expect(stackId.getStackVersion()).andReturn(STACK_VERSION);
    expect(cluster.getClusterId()).andReturn(CLUSTER_ID);

    expect(cluster.getService(SERVICE_NAME)).andReturn(service).atLeastOnce();
    expect(service.getServiceComponent(SERVICE_COMPONENT_NAME)).andReturn(serviceComponent).atLeastOnce();
    expect(sch.getServiceName()).andReturn(SERVICE_NAME).atLeastOnce();
    expect(sch.getServiceComponentName()).andReturn(SERVICE_COMPONENT_NAME).atLeastOnce();
  }

  @Test
  public void testRecalculateHostVersionStateWhenVersionIsNullAndNewVersionIsNotBlank() throws AmbariException {
    expect(sch.getVersion()).andReturn(null);
    expect(serviceComponent.isVersionAdvertised()).andReturn(Boolean.TRUE);
    sch.setVersion(INVALID_NEW_VERSION);
    expectLastCall().once();
    expect(sch.recalculateHostVersionState()).andReturn(null).once();

    replayAll();

    sendEventAndVerify(INVALID_NEW_VERSION);
  }

  @Test
  public void testRecalculateHostVersionStateWhenVersionIsUnknownAndNewVersionIsNotBlank() throws AmbariException {
    expect(sch.getVersion()).andReturn(UNKNOWN_VERSION);
    expect(serviceComponent.isVersionAdvertised()).andReturn(Boolean.TRUE);
    sch.setVersion(INVALID_NEW_VERSION);
    expectLastCall().once();
    expect(sch.recalculateHostVersionState()).andReturn(null).once();

    replayAll();

    sendEventAndVerify(INVALID_NEW_VERSION);
  }

  @Test
  public void testRecalculateClusterVersionStateWhenVersionIsNullAndNewVersionIsValid() throws AmbariException {
    expect(sch.getVersion()).andReturn(null);
    expect(serviceComponent.isVersionAdvertised()).andReturn(Boolean.TRUE);
    sch.setVersion(VALID_NEW_VERSION);
    expectLastCall().once();
    expect(sch.recalculateHostVersionState()).andReturn(DUMMY_REPOSITORY_VERSION_ENTITY).once();
    cluster.recalculateClusterVersionState(DUMMY_REPOSITORY_VERSION_ENTITY);
    expectLastCall().once();

    replayAll();

    sendEventAndVerify(VALID_NEW_VERSION);
  }

  @Test
  public void testRecalculateClusterVersionStateWhenVersionIsUnknownAndNewVersionIsValid() throws AmbariException {
    expect(sch.getVersion()).andReturn(UNKNOWN_VERSION);
    expect(serviceComponent.isVersionAdvertised()).andReturn(Boolean.TRUE);
    sch.setVersion(VALID_NEW_VERSION);
    expectLastCall().once();
    expect(sch.recalculateHostVersionState()).andReturn(DUMMY_REPOSITORY_VERSION_ENTITY).once();
    cluster.recalculateClusterVersionState(DUMMY_REPOSITORY_VERSION_ENTITY);
    expectLastCall().once();

    replayAll();

    sendEventAndVerify(VALID_NEW_VERSION);
  }

  @Test
  public void testRecalculateHostVersionStateWhenComponentDesiredVersionIsUnknownAndNewVersionIsNotValid() throws AmbariException {
    expect(serviceComponent.getDesiredVersion()).andReturn(UNKNOWN_VERSION);
    expect(serviceComponent.isVersionAdvertised()).andReturn(Boolean.TRUE);
    serviceComponent.setDesiredVersion(INVALID_NEW_VERSION);
    expectLastCall().once();
    sch.setUpgradeState(UpgradeState.NONE);
    expectLastCall().once();
    sch.setVersion(INVALID_NEW_VERSION);
    expectLastCall().once();
    expect(sch.recalculateHostVersionState()).andReturn(null).once();

    replayAll();

    sendEventAndVerify(INVALID_NEW_VERSION);
  }

  @Test
  public void testRecalculateClusterVersionStateWhenComponentDesiredVersionIsUnknownAndNewVersionIsValid() throws AmbariException {
    expect(serviceComponent.getDesiredVersion()).andReturn(UNKNOWN_VERSION);
    expect(serviceComponent.isVersionAdvertised()).andReturn(Boolean.TRUE);
    serviceComponent.setDesiredVersion(VALID_NEW_VERSION);
    expectLastCall().once();
    sch.setUpgradeState(UpgradeState.NONE);
    expectLastCall().once();
    sch.setVersion(VALID_NEW_VERSION);
    expectLastCall().once();
    expect(sch.recalculateHostVersionState()).andReturn(DUMMY_REPOSITORY_VERSION_ENTITY).once();
    cluster.recalculateClusterVersionState(DUMMY_REPOSITORY_VERSION_ENTITY);
    expectLastCall().once();

    replayAll();

    sendEventAndVerify(VALID_NEW_VERSION);
  }

  @Test
  public void testRecalculateClusterVersionStateWhenVersionNotAdvertised() throws AmbariException {
    expect(serviceComponent.isVersionAdvertised()).andReturn(Boolean.FALSE);
    replayAll();
    sendEventAndVerify(VALID_NEW_VERSION);
  }


  @Test
  public void testNoActionTakenOnNullVersion() {
    expect(serviceComponent.isVersionAdvertised()).andReturn(true);
    resetAll();
    replayAll();

    sendEventAndVerify(null);
  }

  @Test
  public void testSetUpgradeStateToCompleteWhenUpgradeIsInProgressAndNewVersionIsEqualToComponentDesiredVersion() {
    expect(sch.getVersion()).andReturn(VALID_PREVIOUS_VERSION);
    expect(sch.getUpgradeState()).andReturn(UpgradeState.IN_PROGRESS);
    expect(serviceComponent.isVersionAdvertised()).andReturn(Boolean.TRUE);
    sch.setUpgradeState(UpgradeState.COMPLETE);
    expectLastCall().once();

    expect(serviceComponent.getDesiredVersion()).andStubReturn(VALID_NEW_VERSION);
    replayAll();

    sendEventAndVerify(VALID_NEW_VERSION);
  }

  @Test
  public void testSetUpgradeStateToVersionMismatchWhenUpgradeIsInProgressAndNewVersionIsNotEqualToComponentDesiredVersion() {
    expect(sch.getVersion()).andReturn(VALID_PREVIOUS_VERSION);
    expect(sch.getUpgradeState()).andReturn(UpgradeState.IN_PROGRESS);
    expect(serviceComponent.isVersionAdvertised()).andReturn(Boolean.TRUE);
    sch.setUpgradeState(UpgradeState.VERSION_MISMATCH);
    expectLastCall().once();

    expect(serviceComponent.getDesiredVersion()).andStubReturn(VALID_PREVIOUS_VERSION);
    replayAll();

    sendEventAndVerify(VALID_NEW_VERSION);
  }

  @Test
  public void testSetUpgradeStateToCompleteWhenHostHasVersionMismatchAndNewVersionIsEqualToComponentDesiredVersionAndClusterUpgradeIsInProgress() {
    expect(sch.getVersion()).andReturn(VALID_PREVIOUS_VERSION);
    expect(sch.getUpgradeState()).andReturn(UpgradeState.VERSION_MISMATCH);
    expect(cluster.getUpgradeEntity()).andReturn(DUMMY_UPGRADE_ENTITY);
    expect(serviceComponent.getDesiredVersion()).andStubReturn(VALID_NEW_VERSION);
    expect(serviceComponent.isVersionAdvertised()).andReturn(Boolean.TRUE);
    sch.setUpgradeState(UpgradeState.COMPLETE);
    expectLastCall().once();

    replayAll();

    sendEventAndVerify(VALID_NEW_VERSION);
  }

  @Test
  public void testSetUpgradeStateToNoneWhenHostHasVersionMismatchAndNewVersionIsEqualToComponentDesiredVersion() {
    expect(sch.getVersion()).andReturn(VALID_PREVIOUS_VERSION);
    expect(sch.getUpgradeState()).andReturn(UpgradeState.VERSION_MISMATCH);
    expect(serviceComponent.getDesiredVersion()).andStubReturn(VALID_NEW_VERSION);
    expect(serviceComponent.isVersionAdvertised()).andReturn(Boolean.TRUE);
    sch.setUpgradeState(UpgradeState.NONE);
    expectLastCall().once();

    replayAll();

    sendEventAndVerify(VALID_NEW_VERSION);
  }

  @Test
  public void testSetUpgradeStateToVersionMismatchByDefaultWhenHostAndNewVersionsAreValid() {
    expect(sch.getVersion()).andReturn(VALID_PREVIOUS_VERSION);
    expect(serviceComponent.isVersionAdvertised()).andReturn(Boolean.TRUE);
    sch.setUpgradeState(UpgradeState.VERSION_MISMATCH);
    expectLastCall().once();

    replayAll();

    sendEventAndVerify(VALID_NEW_VERSION);
  }

  @Test
  public void testSetRepositoryVersion() throws Exception {
    expect(sch.getVersion()).andReturn(UNKNOWN_VERSION);
    expect(serviceComponent.isVersionAdvertised()).andReturn(Boolean.TRUE);

    RepositoryVersionDAO dao = createNiceMock(RepositoryVersionDAO.class);
    RepositoryVersionEntity entity = createNiceMock(RepositoryVersionEntity.class);
    expect(entity.getVersion()).andReturn("2.4.0.0").once();
    expect(dao.findByPK(1L)).andReturn(entity).once();
    expect(dao.merge(entity)).andReturn(entity).once();

    replayAll();

    String newVersion = VALID_NEW_VERSION;

    HostComponentVersionAdvertisedEvent event = new HostComponentVersionAdvertisedEvent(cluster, sch, newVersion, 1L);
    // !!! avoid injector for test class
    Field field = StackVersionListener.class.getDeclaredField("repositoryVersionDAO");
    field.setAccessible(true);
    field.set(listener, dao);

    listener.onAmbariEvent(event);

    verifyAll();
  }

  /**
   * Tests that the {@link RepositoryVersionEntity} is not updated if there is
   * an upgrade, even if the repo ID is passed back and the versions don't
   * match.
   *
   * @throws Exception
   */
  @Test
  public void testRepositoryVersionNotSetDuringUpgrade() throws Exception {
    // this call will make it seem like there is an upgrade in progress
    expect(cluster.getUpgradeInProgress()).andReturn(createNiceMock(UpgradeEntity.class));

    // create the DAO - nothing will be called on it, so make it strict
    RepositoryVersionDAO dao = createStrictMock(RepositoryVersionDAO.class);

    replayAll();

    // !!! avoid injector for test class

    Field field = StackVersionListener.class.getDeclaredField("repositoryVersionDAO");
    field.setAccessible(true);
    field.set(listener, dao);

    HostComponentVersionAdvertisedEvent event = new HostComponentVersionAdvertisedEvent(cluster,
        sch, VALID_NEW_VERSION, 1L);

    // make sure that a repo ID will come back
    Assert.assertNotNull(event.getRepositoryVersionId());

    listener.onAmbariEvent(event);

    verifyAll();
  }

  private void sendEventAndVerify(String newVersion) {
    HostComponentVersionAdvertisedEvent event = new HostComponentVersionAdvertisedEvent(cluster, sch, newVersion);
    listener.onAmbariEvent(event);

    verifyAll();
  }
}