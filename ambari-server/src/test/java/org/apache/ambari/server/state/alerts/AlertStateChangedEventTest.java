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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.events.AlertStateChangeEvent;
import org.apache.ambari.server.events.listeners.alerts.AlertServiceStateListener;
import org.apache.ambari.server.events.listeners.alerts.AlertStateChangedListener;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.state.AlertState;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.eventbus.EventBus;
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

  /**
   *
   */
  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    injector.getInstance(GuiceJpaInitializer.class);

    // force singleton init via Guice so the listener registers with the bus
    injector.getInstance(AlertServiceStateListener.class);
    injector.getInstance(AlertStateChangedListener.class);

    dispatchDao = injector.getInstance(AlertDispatchDAO.class);
    eventPublisher = injector.getInstance(AlertEventPublisher.class);

    EventBus synchronizedBus = new EventBus();
    synchronizedBus.register(injector.getInstance(AlertStateChangedListener.class));

    // !!! need a synchronous op for testing
    Field field = AlertEventPublisher.class.getDeclaredField("m_eventBus");
    field.setAccessible(true);
    field.set(eventPublisher, synchronizedBus);
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

    dispatchDao.create(EasyMock.anyObject(AlertNoticeEntity.class));
    EasyMock.expectLastCall().once();

    EasyMock.replay(alertTarget, alertGroup, dispatchDao);

    AlertHistoryEntity history = EasyMock.createNiceMock(AlertHistoryEntity.class);
    AlertStateChangeEvent event = EasyMock.createNiceMock(AlertStateChangeEvent.class);
    EasyMock.expect(history.getAlertState()).andReturn(AlertState.CRITICAL).atLeastOnce();
    EasyMock.expect(event.getNewHistoricalEntry()).andReturn(history).atLeastOnce();

    EasyMock.replay(history, event);

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

    // dispatchDao should be strict enough to throw an exception on verify
    // that the create alert notice method was not called
    EasyMock.replay(alertTarget, alertGroup, dispatchDao);

    AlertHistoryEntity history = EasyMock.createNiceMock(AlertHistoryEntity.class);
    AlertStateChangeEvent event = EasyMock.createNiceMock(AlertStateChangeEvent.class);

    // use WARNING to ensure that the target (which only cares about OK/CRIT)
    // does not receive the alert notice
    EasyMock.expect(history.getAlertState()).andReturn(AlertState.WARNING).atLeastOnce();
    EasyMock.expect(event.getNewHistoricalEntry()).andReturn(history).atLeastOnce();

    EasyMock.replay(history, event);

    // async publishing
    eventPublisher.publish(event);
    EasyMock.verify(dispatchDao, history, event);
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
