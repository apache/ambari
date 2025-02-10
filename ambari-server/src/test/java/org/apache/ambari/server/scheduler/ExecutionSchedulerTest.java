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
package org.apache.ambari.server.scheduler;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.apache.ambari.server.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.Scheduler;

import junit.framework.Assert;


@RunWith(MockitoJUnitRunner.class)
public class ExecutionSchedulerTest {

  private Configuration configuration;

  @Mock
  private Scheduler scheduler;

  @Before
  public void setup() throws Exception {
    Properties properties = new Properties();
    properties.setProperty(Configuration.EXECUTION_SCHEDULER_THREADS.getKey(), "2");
    properties.setProperty(Configuration.EXECUTION_SCHEDULER_CLUSTERED.getKey(), "false");
    properties.setProperty(Configuration.EXECUTION_SCHEDULER_CONNECTIONS.getKey(), "2");
    properties.setProperty(Configuration.SERVER_JDBC_DRIVER.getKey(), "db.driver");
    properties.setProperty(Configuration.SERVER_JDBC_URL.getKey(), "jdbc:postgresql://localhost/");
    properties.setProperty(Configuration.SERVER_JDBC_USER_NAME.getKey(), "user");
    properties.setProperty(Configuration.SERVER_DB_NAME.getKey(), "derby");

    configuration = new Configuration(properties);
  }

  private class TestExecutionScheduler extends ExecutionSchedulerImpl {
    public TestExecutionScheduler(Configuration config) throws Exception {
      super(config);
      this.scheduler = ExecutionSchedulerTest.this.scheduler;
      this.isInitialized = true;
    }

    @Override
    protected synchronized void initializeScheduler() {
      // Do nothing - we've already initialized in constructor
    }
  }

  @Test
  public void testSchedulerInitialize() throws Exception {
    ExecutionSchedulerImpl executionScheduler = new ExecutionSchedulerImpl(configuration);

    Properties actualProperties = executionScheduler.getQuartzSchedulerProperties();

    Assert.assertEquals("2", actualProperties.getProperty("org.quartz.threadPool.threadCount"));
    Assert.assertEquals("2", actualProperties.getProperty("org.quartz.dataSource.myDS.maxConnections"));
    Assert.assertEquals("false", actualProperties.getProperty("org.quartz.jobStore.isClustered"));
    Assert.assertEquals("org.quartz.impl.jdbcjobstore.PostgreSQLDelegate",
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
    ExecutionSchedulerImpl executionScheduler = new TestExecutionScheduler(configuration);

    executionScheduler.startScheduler(180);
    executionScheduler.stopScheduler();

    verify(scheduler).startDelayed(180);
    verify(scheduler).shutdown();

    Assert.assertTrue(executionScheduler.isInitialized());
  }

  @Test
  public void testGetQuartzDbDelegateClassAndValidationQuery() throws Exception {
    Properties testProperties = new Properties();
    testProperties.setProperty(Configuration.SERVER_JDBC_URL.getKey(),
            "jdbc:postgresql://host:port/dbname");
    testProperties.setProperty(Configuration.SERVER_DB_NAME.getKey(), "ambari");
    Configuration configuration1 = new Configuration(testProperties);
    ExecutionSchedulerImpl executionScheduler = new ExecutionSchedulerImpl(configuration1);

    String[] subProps = executionScheduler.getQuartzDbDelegateClassAndValidationQuery();

    Assert.assertEquals("org.quartz.impl.jdbcjobstore.PostgreSQLDelegate", subProps[0]);
    Assert.assertEquals("select 0", subProps[1]);

    testProperties.setProperty(Configuration.SERVER_JDBC_URL.getKey(),
            "jdbc:mysql://host:port/dbname");
    configuration1 = new Configuration(testProperties);
    executionScheduler = new ExecutionSchedulerImpl(configuration1);

    subProps = executionScheduler.getQuartzDbDelegateClassAndValidationQuery();

    Assert.assertEquals("org.quartz.impl.jdbcjobstore.StdJDBCDelegate", subProps[0]);
    Assert.assertEquals("select 0", subProps[1]);

    testProperties.setProperty(Configuration.SERVER_JDBC_URL.getKey(),
            "jdbc:oracle:thin://host:port/dbname");
    configuration1 = new Configuration(testProperties);
    executionScheduler = new ExecutionSchedulerImpl(configuration1);

    subProps = executionScheduler.getQuartzDbDelegateClassAndValidationQuery();

    Assert.assertEquals("org.quartz.impl.jdbcjobstore.oracle.OracleDelegate", subProps[0]);
    Assert.assertEquals("select 0 from dual", subProps[1]);
  }

  @Test
  public void testSchedulerStartDelay() throws Exception {

    // 设置模拟行为
    when(scheduler.isStarted()).thenReturn(false);

    // 创建测试实例
    TestExecutionScheduler executionScheduler = new TestExecutionScheduler(configuration);

    // 调用测试方法
    executionScheduler.startScheduler(180);
    executionScheduler.startScheduler(null);

    // 验证调用
    verify(scheduler).startDelayed(180);
    verify(scheduler).start();
    verify(scheduler, atLeastOnce()).isStarted();

    Assert.assertTrue(executionScheduler.isInitialized());
  }
}
