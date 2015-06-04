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
package org.apache.ambari.server.state.alerts;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.apache.ambari.server.events.AggregateAlertRecalculateEvent;
import org.apache.ambari.server.events.AlertEvent;
import org.apache.ambari.server.events.AlertStateChangeEvent;
import org.apache.ambari.server.events.MockEventListener;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.persist.PersistService;
import com.google.inject.util.Modules;

/**
 * Tests that {@link AlertStateChangeEvent} instances cause
 * {@link AlertNoticeEntity} instances to be created.
 */
public class AlertStateChangedEventTest {

  private AlertEventPublisher eventPublisher;
  private AlertDispatchDAO dispatchDao;
  private Injector injector;
  private MockEventListener m_listener;

  /**
   *
   */
  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    injector.getInstance(GuiceJpaInitializer.class);
    m_listener = injector.getInstance(MockEventListener.class);

    dispatchDao = injector.getInstance(AlertDispatchDAO.class);

    // !!! need a synchronous op for testing
    EventBusSynchronizer.synchronizeAlertEventPublisher(injector).register(m_listener);
    EventBusSynchronizer.synchronizeAmbariEventPublisher(injector).register(m_listener);

    eventPublisher = injector.getInstance(AlertEventPublisher.class);
  }

  /**
   * @throws Exception
   */
  @After
  public void teardown() throws Exception {
    injector.getInstance(PersistService.class).stop();
    injector = null;
  }

  /**
   * Tests that an {@link AlertStateChangeEvent} causes
   * {@link AlertNoticeEntity} instances to be written.
   *
   * @throws Exception
   */
  @Test
  public void testAlertNoticeCreationFromEvent() throws Exception {
    AlertTargetEntity alertTarget = EasyMock.createNiceMock(AlertTargetEntity.class);
    AlertGroupEntity alertGroup = EasyMock.createMock(AlertGroupEntity.class);
    List<AlertGroupEntity> groups = new ArrayList<AlertGroupEntity>();
    Set<AlertTargetEntity> targets = new HashSet<AlertTargetEntity>();

    targets.add(alertTarget);
    groups.add(alertGroup);

    EasyMock.expect(alertGroup.getAlertTargets()).andReturn(targets).once();
    EasyMock.expect(alertTarget.getAlertStates()).andReturn(
        EnumSet.of(AlertState.OK, AlertState.CRITICAL)).atLeastOnce();

    EasyMock.expect(
        dispatchDao.findGroupsByDefinition(EasyMock.anyObject(AlertDefinitionEntity.class))).andReturn(
        groups).once();

    dispatchDao.createNotices(EasyMock.<List<AlertNoticeEntity>>anyObject());
    EasyMock.expectLastCall().once();

    EasyMock.replay(alertTarget, alertGroup, dispatchDao);

    AlertDefinitionEntity definition = EasyMock.createNiceMock(AlertDefinitionEntity.class);
    EasyMock.expect(definition.getDefinitionId()).andReturn(1L);
    EasyMock.expect(definition.getServiceName()).andReturn("HDFS");
    EasyMock.expect(definition.getLabel()).andReturn("hdfs-foo-alert");
    EasyMock.expect(definition.getDescription()).andReturn("HDFS Foo Alert");

    AlertHistoryEntity history = EasyMock.createNiceMock(AlertHistoryEntity.class);
    AlertStateChangeEvent event = EasyMock.createNiceMock(AlertStateChangeEvent.class);
    Alert alert = EasyMock.createNiceMock(Alert.class);

    EasyMock.expect(history.getAlertState()).andReturn(AlertState.CRITICAL).atLeastOnce();
    EasyMock.expect(history.getAlertDefinition()).andReturn(definition).atLeastOnce();
    EasyMock.expect(alert.getText()).andReturn("The HDFS Foo Alert Is Not Good").atLeastOnce();
    EasyMock.expect(alert.getState()).andReturn(AlertState.CRITICAL).atLeastOnce();
    EasyMock.expect(event.getNewHistoricalEntry()).andReturn(history).atLeastOnce();
    EasyMock.expect(event.getAlert()).andReturn(alert).atLeastOnce();

    EasyMock.replay(definition, history, event, alert);

    // async publishing
    eventPublisher.publish(event);
    EasyMock.verify(dispatchDao, history, event);
  }

  /**
   * Tests that an {@link AlertNoticeEntity} is not created for a target that
   * does not match the {@link AlertState} of the alert.
   *
   * @throws Exception
   */
  @Test
  public void testAlertNoticeSkippedForTarget() throws Exception {
    AlertTargetEntity alertTarget = EasyMock.createMock(AlertTargetEntity.class);
    AlertGroupEntity alertGroup = EasyMock.createMock(AlertGroupEntity.class);
    List<AlertGroupEntity> groups = new ArrayList<AlertGroupEntity>();
    Set<AlertTargetEntity> targets = new HashSet<AlertTargetEntity>();

    targets.add(alertTarget);
    groups.add(alertGroup);

    EasyMock.expect(alertGroup.getAlertTargets()).andReturn(targets).once();
    EasyMock.expect(alertTarget.getAlertStates()).andReturn(
        EnumSet.of(AlertState.OK, AlertState.CRITICAL)).atLeastOnce();

    EasyMock.expect(
        dispatchDao.findGroupsByDefinition(EasyMock.anyObject(AlertDefinitionEntity.class))).andReturn(
        groups).once();

    dispatchDao.createNotices(EasyMock.<List<AlertNoticeEntity>>anyObject());

    // dispatchDao should be strict enough to throw an exception on verify
    // that the create alert notice method was not called
    EasyMock.replay(alertTarget, alertGroup, dispatchDao);

    AlertDefinitionEntity definition = EasyMock.createNiceMock(AlertDefinitionEntity.class);
    EasyMock.expect(definition.getDefinitionId()).andReturn(1L);
    EasyMock.expect(definition.getServiceName()).andReturn("HDFS");
    EasyMock.expect(definition.getLabel()).andReturn("hdfs-foo-alert");
    EasyMock.expect(definition.getDescription()).andReturn("HDFS Foo Alert");

    AlertHistoryEntity history = EasyMock.createNiceMock(AlertHistoryEntity.class);
    AlertStateChangeEvent event = EasyMock.createNiceMock(AlertStateChangeEvent.class);
    Alert alert = EasyMock.createNiceMock(Alert.class);

    // use WARNING to ensure that the target (which only cares about OK/CRIT)
    // does not receive the alert notice
    EasyMock.expect(history.getAlertState()).andReturn(AlertState.WARNING).atLeastOnce();

    EasyMock.expect(history.getAlertDefinition()).andReturn(definition).atLeastOnce();
    EasyMock.expect(alert.getText()).andReturn("The HDFS Foo Alert Is Not Good").atLeastOnce();
    EasyMock.expect(alert.getState()).andReturn(AlertState.WARNING).atLeastOnce();
    EasyMock.expect(event.getNewHistoricalEntry()).andReturn(history).atLeastOnce();
    EasyMock.expect(event.getAlert()).andReturn(alert).atLeastOnce();

    EasyMock.replay(definition, history, event, alert);

    // async publishing
    eventPublisher.publish(event);
    EasyMock.verify(dispatchDao, history, event);
  }

  /**
   * Tests that {@link AggregateAlertRecalculateEvent}s are fired correctly.
   *
   * @throws Exception
   */
  @Test
  public void testAggregateAlertRecalculateEvent() throws Exception {
    Class<? extends AlertEvent> eventClass = AggregateAlertRecalculateEvent.class;

    Assert.assertFalse(m_listener.isAlertEventReceived(eventClass));
    AlertsDAO dao = injector.getInstance(AlertsDAO.class);
    dao.removeCurrentByServiceComponentHost(1, "HDFS", "DATANODE", "c6401");
    Assert.assertTrue(m_listener.isAlertEventReceived(eventClass));
    Assert.assertEquals(1, m_listener.getAlertEventReceivedCount(eventClass));
  }

  /**
   *
   */
  private class MockModule implements Module {
    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Binder binder) {
      AlertDispatchDAO dispatchDao = EasyMock.createMock(AlertDispatchDAO.class);
      binder.bind(AlertDispatchDAO.class).toInstance(dispatchDao);
    }
  }
}
