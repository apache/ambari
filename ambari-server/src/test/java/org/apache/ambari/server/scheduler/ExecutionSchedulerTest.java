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
package org.apache.ambari.server.scheduler;

import junit.framework.Assert;
import org.apache.ambari.server.configuration.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import java.util.Properties;
import static org.easymock.EasyMock.expect;
import static org.mockito.Mockito.spy;
import static org.powermock.api.easymock.PowerMock.createNiceMock;
import static org.powermock.api.easymock.PowerMock.expectNew;
import static org.powermock.api.easymock.PowerMock.expectPrivate;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ExecutionSchedulerImpl.class })
@PowerMockIgnore("javax.management.*")
public class ExecutionSchedulerTest {

  private Configuration configuration;

  @Before
  public void setup() throws Exception {
    Properties properties = new Properties();
    properties.setProperty(Configuration.EXECUTION_SCHEDULER_THREADS, "2");
    properties.setProperty(Configuration.EXECUTION_SCHEDULER_CLUSTERED, "false");
    properties.setProperty(Configuration.EXECUTION_SCHEDULER_CONNECTIONS, "2");
    properties.setProperty(Configuration.SERVER_JDBC_DRIVER_KEY, "db.driver");
    properties.setProperty(Configuration.SERVER_JDBC_URL_KEY, "db.url");
    properties.setProperty(Configuration.SERVER_JDBC_USER_NAME_KEY, "user");
    properties.setProperty(Configuration.SERVER_JDBC_USER_PASSWD_KEY,
      "ambari-server/src/test/resources/password.dat");
    properties.setProperty(Configuration.SERVER_DB_NAME_KEY, "derby");

    this.configuration = new Configuration(properties);

  }

  @After
  public void teardown() throws Exception {
  }


  @Test
  public void testSchedulerInitialize() throws Exception {

    ExecutionSchedulerImpl executionScheduler = spy(new ExecutionSchedulerImpl(configuration));

    Properties actualProperties = executionScheduler
      .getQuartzSchedulerProperties();

    Assert.assertEquals("2", actualProperties.getProperty("org.quartz.threadPool.threadCount"));
    Assert.assertEquals("2", actualProperties.getProperty("org.quartz.dataSource.myDS.maxConnections"));
    Assert.assertEquals("false", actualProperties.getProperty("org.quartz.jobStore.isClustered"));
    Assert.assertEquals("org.quartz.impl.jdbcjobstore.StdJDBCDelegate",
      actualProperties.getProperty("org.quartz.jobStore.driverDelegateClass"));
    Assert.assertEquals("select 0",
      actualProperties.getProperty("org.quartz.dataSource.myDS.validationQuery"));
    Assert.assertEquals(ExecutionSchedulerImpl.DEFAULT_SCHEDULER_NAME,
      actualProperties.getProperty("org.quartz.scheduler.instanceName"));
    Assert.assertEquals("org.quartz.simpl.SimpleThreadPool",
      actualProperties.getProperty("org.quartz.threadPool.class"));
  }

  @Test
  public void testSchedulerStartStop() throws Exception {
    StdSchedulerFactory factory = createNiceMock(StdSchedulerFactory.class);
    Scheduler scheduler = createNiceMock(Scheduler.class);

    expect(factory.getScheduler()).andReturn(scheduler);
    expectPrivate(scheduler, "start").once();
    expectNew(StdSchedulerFactory.class).andReturn(factory);
    expectPrivate(scheduler, "shutdown").once();

    PowerMock.replay(factory, StdSchedulerFactory.class, scheduler);

    ExecutionSchedulerImpl executionScheduler = new ExecutionSchedulerImpl(configuration);

    executionScheduler.startScheduler();
    executionScheduler.stopScheduler();

    PowerMock.verify(factory, StdSchedulerFactory.class, scheduler);

    Assert.assertTrue(executionScheduler.isInitialized());
  }
}
