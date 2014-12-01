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
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.service.Service.STATE;
import org.apache.hadoop.util.ExitUtil;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics
  .timeline.DefaultPhoenixDataSource;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics
  .timeline.PhoenixHBaseAccessor;
import org.apache.zookeeper.ClientCnxn;
import org.easymock.EasyMock;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
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
import java.sql.Statement;

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics
  .timeline.TimelineMetricConfiguration.METRICS_SITE_CONFIGURATION_FILE;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.*;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.*;
import static org.powermock.api.easymock.PowerMock.*;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier
  .suppress;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ PhoenixHBaseAccessor.class, UserGroupInformation.class,
  ClientCnxn.class, DefaultPhoenixDataSource.class})
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

    Connection connection = createNiceMock(Connection.class);
    Statement stmt = createNiceMock(Statement.class);
    mockStatic(DriverManager.class);
    expect(DriverManager.getConnection("jdbc:phoenix:localhost:2181:/hbase"))
      .andReturn(connection).anyTimes();
    expect(connection.createStatement()).andReturn(stmt).anyTimes();
    suppress(method(Statement.class, "executeUpdate", String.class));
    connection.close();
    expectLastCall();

    EasyMock.replay(connection, stmt);
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
