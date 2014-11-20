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
package org.apache.ambari.server.state.services;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.notifications.DispatchFactory;
import org.apache.ambari.server.notifications.Notification;
import org.apache.ambari.server.notifications.NotificationDispatcher;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.NotificationState;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.state.alert.SourceType;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

/**
 * Tests the {@link AlertNoticeDispatchService}.
 */
public class AlertNoticeDispatchServiceTest extends AlertNoticeDispatchService {

  final static String ALERT_NOTICE_UUID = UUID.randomUUID().toString();
  final static String ALERT_UNIQUE_TEXT = "0eeda438-2b13-4869-a416-137e35ff76e9";
  final static String HOSTNAME = "c6401.ambari.apache.org";
  final static Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

  private AmbariMetaInfo m_metaInfo = null;
  private DispatchFactory m_dispatchFactory = null;
  private AlertDispatchDAO m_dao = null;
  private Injector m_injector;

  List<AlertDefinitionEntity> m_definitions = new ArrayList<AlertDefinitionEntity>();
  List<AlertHistoryEntity> m_histories = new ArrayList<AlertHistoryEntity>();

  @Before
  public void before() {
    m_dao = createStrictMock(AlertDispatchDAO.class);
    m_dispatchFactory = createStrictMock(DispatchFactory.class);
    m_metaInfo = createNiceMock(AmbariMetaInfo.class);

    // create an injector which will inject the mocks
    m_injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    Assert.assertNotNull(m_injector);

    // create 5 definitions
    for (int i = 0; i < 5; i++) {
      AlertDefinitionEntity definition = new AlertDefinitionEntity();
      definition.setDefinitionName("Alert Definition " + i);
      definition.setServiceName("Service " + i);
      definition.setComponentName(null);
      definition.setClusterId(1L);
      definition.setHash(UUID.randomUUID().toString());
      definition.setScheduleInterval(Integer.valueOf(60));
      definition.setScope(Scope.SERVICE);
      definition.setSource("{\"type\" : \"SCRIPT\"}");
      definition.setSourceType(SourceType.SCRIPT);

      m_definitions.add(definition);
    }


    // create 10 historical alerts for each definition, 8 OK and 2 CRIT
    calendar.clear();
    calendar.set(2014, Calendar.JANUARY, 1);

    for (AlertDefinitionEntity definition : m_definitions) {
      for (int i = 0; i < 10; i++) {
        AlertHistoryEntity history = new AlertHistoryEntity();
        history.setServiceName(definition.getServiceName());
        history.setClusterId(1L);
        history.setAlertDefinition(definition);
        history.setAlertLabel(definition.getDefinitionName() + " " + i);
        history.setAlertText(definition.getDefinitionName() + " " + i);
        history.setAlertTimestamp(calendar.getTimeInMillis());
        history.setHostName(HOSTNAME);

        history.setAlertState(AlertState.OK);
        if (i == 0 || i == 5) {
          history.setAlertState(AlertState.CRITICAL);
        }

        // increase the days for each
        calendar.add(Calendar.DATE, 1);
        m_histories.add(history);
      }
    }
  }

  /**
   * Tests the parsing of the {@link AlertHistoryEntity} list into
   * {@link AlertInfo}.
   *
   * @throws Exception
   */
  @Test
  public void testAlertInfo() throws Exception {
    AlertInfo alertInfo = new AlertInfo(m_histories);
    assertEquals(50, alertInfo.getAlerts().size());
    assertEquals(10, alertInfo.getAlerts("Service 1").size());
    assertEquals(10, alertInfo.getAlerts("Service 2").size());

    assertEquals(8, alertInfo.getAlerts("Service 1", "OK").size());
    assertEquals(2, alertInfo.getAlerts("Service 1", "CRITICAL").size());
    assertNull(alertInfo.getAlerts("Service 1", "WARNING"));
    assertNull(alertInfo.getAlerts("Service 1", "UNKNOWN"));

    assertEquals(5, alertInfo.getServices().size());
  }

  /**
   * Tests that the dispatcher is not called when there are no notices.
   *
   * @throws Exception
   */
  @Test
  public void testNoDispatch() throws Exception {
    EasyMock.expect(m_dao.findPendingNotices()).andReturn(
        new ArrayList<AlertNoticeEntity>()).once();

    // m_dispatchFactory should not be called at all
    EasyMock.replay(m_dao, m_dispatchFactory);

    // "startup" the service so that its initialization is done
    AlertNoticeDispatchService service = m_injector.getInstance(AlertNoticeDispatchService.class);
    service.startUp();

    // service trigger
    service.runOneIteration();

    EasyMock.verify(m_dao, m_dispatchFactory);
  }

  /**
   * Tests that the dispatcher is not called when there are no notices.
   *
   * @throws Exception
   */
  @Test
  public void testDispatch() throws Exception {
    MockDispatcher dispatcher = new MockDispatcher();

    EasyMock.expect(m_dao.findPendingNotices()).andReturn(getMockNotices()).once();

    EasyMock.expect(m_dispatchFactory.getDispatcher("EMAIL")).andReturn(
        dispatcher).once();

    EasyMock.replay(m_dao, m_dispatchFactory);

    // "startup" the service so that its initialization is done
    AlertNoticeDispatchService service = m_injector.getInstance(AlertNoticeDispatchService.class);
    service.startUp();

    // service trigger with mock executor that blocks
    service.setExecutor(new MockExecutor());
    service.runOneIteration();

    EasyMock.verify(m_dao, m_dispatchFactory);

    Notification notification = dispatcher.getNotification();
    assertNotNull(notification);

    assertTrue(notification.Subject.contains("OK[1]"));
    assertTrue(notification.Subject.contains("Critical[0]"));
    assertTrue(notification.Body.contains(ALERT_UNIQUE_TEXT));
  }

  /**
   * Tests that a failed dispatch invokes the callback to mark the UUIDs of the
   * notices as FAILED.
   *
   * @throws Exception
   */
  @Test
  public void testFailedDispatch() throws Exception {
    MockDispatcher dispatcher = new MockDispatcher();
    List<AlertNoticeEntity> notices = getMockNotices();
    AlertNoticeEntity notice = notices.get(0);

    // these expectations happen b/c we need to mark the notice as FAILED
    EasyMock.expect(m_dao.findPendingNotices()).andReturn(notices).once();
    EasyMock.expect(m_dao.findNoticeByUuid(ALERT_NOTICE_UUID)).andReturn(notice).once();
    EasyMock.expect(m_dao.merge(getMockNotices().get(0))).andReturn(notice).once();

    EasyMock.replay(m_dao, m_dispatchFactory);

    // do NOT startup the service which will force a template NPE
    AlertNoticeDispatchService service = m_injector.getInstance(AlertNoticeDispatchService.class);

    // service trigger with mock executor that blocks
    service.setExecutor(new MockExecutor());
    service.runOneIteration();

    EasyMock.verify(m_dao, m_dispatchFactory);

    Notification notification = dispatcher.getNotification();
    assertNull(notification);
  }

  /**
   * Gets PENDING notices.
   *
   * @return
   */
  private List<AlertNoticeEntity> getMockNotices(){
    AlertHistoryEntity history = new AlertHistoryEntity();
    history.setServiceName("HDFS");
    history.setClusterId(1L);
    history.setAlertDefinition(null);
    history.setAlertLabel("Label");
    history.setAlertState(AlertState.OK);
    history.setAlertText(ALERT_UNIQUE_TEXT);
    history.setAlertTimestamp(System.currentTimeMillis());

    AlertTargetEntity target = new AlertTargetEntity();
    target.setAlertStates(EnumSet.allOf(AlertState.class));
    target.setTargetName("Alert Target");
    target.setDescription("Mock Target");
    target.setNotificationType("EMAIL");

    String properties = "{ \"foo\" : \"bar\" }";
    target.setProperties(properties);

    AlertNoticeEntity notice = new AlertNoticeEntity();
    notice.setUuid(ALERT_NOTICE_UUID);
    notice.setAlertTarget(target);
    notice.setAlertHistory(history);
    notice.setNotifyState(NotificationState.PENDING);

    ArrayList<AlertNoticeEntity> notices = new ArrayList<AlertNoticeEntity>();
    notices.add(notice);

    return notices;
  }

  /**
   * A mock dispatcher that captures the {@link Notification}.
   */
  private static final class MockDispatcher implements NotificationDispatcher {

    private Notification m_notificaiton;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
      return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispatch(Notification notification) {
      m_notificaiton = notification;
    }

    public Notification getNotification() {
      return m_notificaiton;
    }
  }

  /**
   * An {@link Executor} that calls {@link Runnable#run()} directly in the
   * current thread.
   */
  private static final class MockExecutor implements Executor {

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Runnable runnable) {
      runnable.run();
    }
  }

  /**
   *
   */
  private class MockModule implements Module {
    /**
     *
     */
    @Override
    public void configure(Binder binder) {
      binder.bind(AlertDispatchDAO.class).toInstance(m_dao);
      binder.bind(DispatchFactory.class).toInstance(m_dispatchFactory);
      binder.bind(AmbariMetaInfo.class).toInstance(m_metaInfo);

      EasyMock.expect(m_metaInfo.getServerVersion()).andReturn("2.0.0").anyTimes();
    }
  }
}
