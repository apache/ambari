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

package org.apache.hadoop.yarn.server.applicationhistoryservice;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.service.Service.STATE;
import org.apache.hadoop.util.ExitUtil;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.HBaseTimelineMetricStore;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.MetricCollectorHAController;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.DefaultPhoenixDataSource;
import org.apache.zookeeper.ClientCnxn;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.METRICS_SITE_CONFIGURATION_FILE;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.expectNew;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.suppress;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ PhoenixHBaseAccessor.class, HBaseTimelineMetricStore.class, UserGroupInformation.class,
  ClientCnxn.class, DefaultPhoenixDataSource.class, ConnectionFactory.class,
  TimelineMetricConfiguration.class, ApplicationHistoryServer.class })
@PowerMockIgnore( {"javax.management.*"})
public class TestApplicationHistoryServer {

  ApplicationHistoryServer historyServer = null;
  Configuration metricsConf = null;

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Before
  @SuppressWarnings("all")
  public void setup() throws URISyntaxException, IOException {
    folder.create();
    File hbaseSite = folder.newFile("hbase-site.xml");
    File amsSite = folder.newFile("ams-site.xml");

    FileUtils.writeStringToFile(hbaseSite, "<configuration>\n" +
      "  <property>\n" +
      "    <name>hbase.defaults.for.version.skip</name>\n" +
      "    <value>true</value>\n" +
      "  </property>" +
      "  <property> " +
      "    <name>hbase.zookeeper.quorum</name>\n" +
      "    <value>localhost</value>\n" +
      "  </property>" +
      "</configuration>");

    FileUtils.writeStringToFile(amsSite, "<configuration>\n" +
      "  <property>\n" +
      "    <name>test</name>\n" +
      "    <value>testReady</value>\n" +
      "  </property>\n" +
      "  <property>\n" +
      "    <name>timeline.metrics.host.aggregator.hourly.disabled</name>\n" +
      "    <value>true</value>\n" +
      "    <description>\n" +
      "      Disable host based hourly aggregations.\n" +
      "    </description>\n" +
      "  </property>\n" +
      "  <property>\n" +
      "    <name>timeline.metrics.host.aggregator.minute.disabled</name>\n" +
      "    <value>true</value>\n" +
      "    <description>\n" +
      "      Disable host based minute aggregations.\n" +
      "    </description>\n" +
      "  </property>\n" +
      "  <property>\n" +
      "    <name>timeline.metrics.cluster.aggregator.hourly.disabled</name>\n" +
      "    <value>true</value>\n" +
      "    <description>\n" +
      "      Disable cluster based hourly aggregations.\n" +
      "    </description>\n" +
      "  </property>\n" +
      "  <property>\n" +
      "    <name>timeline.metrics.cluster.aggregator.minute.disabled</name>\n" +
      "    <value>true</value>\n" +
      "    <description>\n" +
      "      Disable cluster based minute aggregations.\n" +
      "    </description>\n" +
      "  </property>" +
      "</configuration>");

    ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();

    // Add the conf dir to the classpath
    // Chain the current thread classloader
    URLClassLoader urlClassLoader = null;
    try {
      urlClassLoader = new URLClassLoader(new URL[] {
        folder.getRoot().toURI().toURL() }, currentClassLoader);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }

    Thread.currentThread().setContextClassLoader(urlClassLoader);
    metricsConf = new Configuration(false);
    metricsConf.addResource(Thread.currentThread().getContextClassLoader()
      .getResource(METRICS_SITE_CONFIGURATION_FILE).toURI().toURL());
    assertNotNull(metricsConf.get("test"));
  }

  // simple test init/start/stop ApplicationHistoryServer. Status should change.
  @Test(timeout = 50000)
  public void testStartStopServer() throws Exception {
    Configuration config = new YarnConfiguration();
    UserGroupInformation ugi =
      UserGroupInformation.createUserForTesting("ambari", new String[] {"ambari"});

    mockStatic(UserGroupInformation.class);
    expect(UserGroupInformation.getCurrentUser()).andReturn(ugi).anyTimes();
    expect(UserGroupInformation.isSecurityEnabled()).andReturn(false).anyTimes();
    config.set(YarnConfiguration.APPLICATION_HISTORY_STORE,
      "org.apache.hadoop.yarn.server.applicationhistoryservice.NullApplicationHistoryStore");
    Configuration hbaseConf = new Configuration();
    hbaseConf.set("hbase.zookeeper.quorum", "localhost");

    TimelineMetricConfiguration metricConfiguration = PowerMock.createNiceMock(TimelineMetricConfiguration.class);
    expectNew(TimelineMetricConfiguration.class).andReturn(metricConfiguration);
    expect(metricConfiguration.getHbaseConf()).andReturn(hbaseConf);
    Configuration metricsConf = new Configuration();
    expect(metricConfiguration.getMetricsConf()).andReturn(metricsConf).anyTimes();
    expect(metricConfiguration.isTimelineMetricsServiceWatcherDisabled()).andReturn(true);
    expect(metricConfiguration.getTimelineMetricsServiceHandlerThreadCount()).andReturn(20).anyTimes();
    expect(metricConfiguration.getWebappAddress()).andReturn("localhost:9990").anyTimes();
    expect(metricConfiguration.getTimelineServiceRpcAddress()).andReturn("localhost:10299").anyTimes();
    expect(metricConfiguration.getClusterZKQuorum()).andReturn("localhost").anyTimes();
    expect(metricConfiguration.getClusterZKClientPort()).andReturn("2181").anyTimes();

    Connection connection = createNiceMock(Connection.class);
    Statement stmt = createNiceMock(Statement.class);
    PreparedStatement preparedStatement = createNiceMock(PreparedStatement.class);
    ResultSet rs = createNiceMock(ResultSet.class);
    mockStatic(DriverManager.class);
    expect(DriverManager.getConnection("jdbc:phoenix:localhost:2181:/ams-hbase-unsecure"))
      .andReturn(connection).anyTimes();
    expect(connection.createStatement()).andReturn(stmt).anyTimes();
    expect(connection.prepareStatement(anyString())).andReturn(preparedStatement).anyTimes();
    suppress(method(Statement.class, "executeUpdate", String.class));
    expect(preparedStatement.executeQuery()).andReturn(rs).anyTimes();
    expect(rs.next()).andReturn(false).anyTimes();
    preparedStatement.close();
    expectLastCall().anyTimes();
    connection.close();
    expectLastCall();

    MetricCollectorHAController haControllerMock = PowerMock.createMock(MetricCollectorHAController.class);
    expectNew(MetricCollectorHAController.class, metricConfiguration)
      .andReturn(haControllerMock);

    haControllerMock.initializeHAController();
    expectLastCall().once();
    expect(haControllerMock.isInitialized()).andReturn(false).anyTimes();

    org.apache.hadoop.hbase.client.Connection conn = createNiceMock(org.apache.hadoop.hbase.client.Connection.class);
    mockStatic(ConnectionFactory.class);
    expect(ConnectionFactory.createConnection((Configuration) anyObject())).andReturn(conn);
    expect(conn.getAdmin()).andReturn(null);

    EasyMock.replay(connection, stmt, preparedStatement, rs);
    replayAll();

    historyServer = new ApplicationHistoryServer();
    historyServer.init(config);

    verifyAll();

    assertEquals(STATE.INITED, historyServer.getServiceState());
    assertEquals(4, historyServer.getServices().size());
    ApplicationHistoryClientService historyService =
      historyServer.getClientService();
    assertNotNull(historyServer.getClientService());
    assertEquals(STATE.INITED, historyService.getServiceState());

    historyServer.start();
    assertEquals(STATE.STARTED, historyServer.getServiceState());
    assertEquals(STATE.STARTED, historyService.getServiceState());
    historyServer.stop();
    assertEquals(STATE.STOPPED, historyServer.getServiceState());
  }

  // test launch method
  @Ignore
  @Test(timeout = 60000)
  public void testLaunch() throws Exception {

    UserGroupInformation ugi =
      UserGroupInformation.createUserForTesting("ambari", new String[]{"ambari"});
    mockStatic(UserGroupInformation.class);
    expect(UserGroupInformation.getCurrentUser()).andReturn(ugi).anyTimes();
    expect(UserGroupInformation.isSecurityEnabled()).andReturn(false).anyTimes();

    ExitUtil.disableSystemExit();
    try {
      historyServer = ApplicationHistoryServer.launchAppHistoryServer(new String[0]);
    } catch (ExitUtil.ExitException e) {
      assertEquals(0, e.status);
      ExitUtil.resetFirstExitException();
      fail();
    }
  }

  @After
  public void stop() {
    if (historyServer != null) {
      historyServer.stop();
    }
  }
}
