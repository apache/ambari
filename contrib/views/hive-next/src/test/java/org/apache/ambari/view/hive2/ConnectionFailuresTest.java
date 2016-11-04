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

package org.apache.ambari.view.hive2;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import com.google.common.base.Optional;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive2.actor.DeathWatch;
import org.apache.ambari.view.hive2.actor.OperationController;
import org.apache.ambari.view.hive2.actor.message.Connect;
import org.apache.ambari.view.hive2.actor.message.ExecuteJob;
import org.apache.ambari.view.hive2.actor.message.HiveJob;
import org.apache.ambari.view.hive2.actor.message.SQLStatementJob;
import org.apache.ambari.view.hive2.internal.ConnectionSupplier;
import org.apache.ambari.view.hive2.internal.DataStorageSupplier;
import org.apache.ambari.view.hive2.internal.HdfsApiSupplier;
import org.apache.ambari.view.hive2.internal.HiveConnectionWrapper;
import org.apache.ambari.view.hive2.persistence.Storage;
import org.apache.ambari.view.hive2.resources.jobs.viewJobs.Job;
import org.apache.ambari.view.hive2.resources.jobs.viewJobs.JobImpl;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.hive.jdbc.HiveConnection;
import org.apache.hive.jdbc.HiveQueryResultSet;
import org.apache.hive.jdbc.HiveStatement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import static org.easymock.EasyMock.*;

public class ConnectionFailuresTest {

  private ActorSystem actorSystem;

  @Before
  public void setUp() throws Exception {
      actorSystem = ActorSystem.create("TestingActorSystem");
  }

  @After
  public void tearDown() throws Exception {
    JavaTestKit.shutdownActorSystem(actorSystem);
  }

  @Test
  public void testConnectionFailure() throws Exception {
    ViewContext viewContext = createNiceMock(ViewContext.class);
    ConnectionSupplier connectionSupplier = createNiceMock(ConnectionSupplier.class);
    DataStorageSupplier dataStorageSupplier = createNiceMock(DataStorageSupplier.class);
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);
    HdfsApiSupplier hdfsApiSupplier = createNiceMock(HdfsApiSupplier.class);
    Connect connect = createNiceMock(Connect.class);
    Storage storage = createNiceMock(Storage.class);
    JobImpl jobImpl = createNiceMock(JobImpl.class);
    ResultSet resultSet = createNiceMock(HiveQueryResultSet.class);
    HiveStatement statement = createNiceMock(HiveStatement.class);
    ConnectionDelegate delegate = createNiceMock(ConnectionDelegate.class);
    HiveConnectionWrapper connectionWrapper = createNiceMock(HiveConnectionWrapper.class);
    HiveConnection hiveConnection = createNiceMock(HiveConnection.class);
    HiveJob test = new SQLStatementJob(HiveJob.Type.ASYNC, new String[]{"select * from test"}, "test", "1", "test.log");
    ExecuteJob executeJob = new ExecuteJob(connect, test);
    ActorRef deathwatch = actorSystem.actorOf(Props.create(DeathWatch.class));
    ActorRef operationControl = actorSystem.actorOf(
            Props.create(OperationController.class, actorSystem, deathwatch, viewContext, connectionSupplier, dataStorageSupplier, hdfsApiSupplier), "operationController-test");
    expect(hdfsApiSupplier.get(viewContext)).andReturn(Optional.of(hdfsApi));
    expect(viewContext.getInstanceName()).andReturn("test").anyTimes();
    expect(viewContext.getProperties()).andReturn(new HashMap<String, String>()).anyTimes();
    expect(connect.getConnectable(anyObject(AuthParams.class))).andReturn(connectionWrapper);
    expect(connectionWrapper.isOpen()).andReturn(false).anyTimes();
    expect(connectionWrapper.getConnection()).andReturn(Optional.<HiveConnection>absent()).anyTimes();
    expect(dataStorageSupplier.get(viewContext)).andReturn(storage);
    expect(connectionSupplier.get(viewContext)).andReturn(delegate);
    expect(storage.load(JobImpl.class, "1")).andReturn(jobImpl).anyTimes();
    expect(jobImpl.getDateSubmitted()).andReturn(0L).times(1);
    connectionWrapper.connect();
    jobImpl.setStatus(Job.JOB_STATE_ERROR);
    storage.store(JobImpl.class, jobImpl);
    replay(viewContext, connect, hdfsApiSupplier, dataStorageSupplier, connectionWrapper,
            storage, jobImpl, connectionSupplier, delegate, statement, resultSet);

    operationControl.tell(executeJob, ActorRef.noSender());
    Thread.sleep(5000);
    verify(connect, hdfsApiSupplier, dataStorageSupplier, connectionWrapper,
            storage, jobImpl, connectionSupplier, delegate, statement, resultSet);

  }

  @Test
  public void testExecutionFailure() throws Exception {
    ViewContext viewContext = createNiceMock(ViewContext.class);
    ConnectionSupplier connectionSupplier = createNiceMock(ConnectionSupplier.class);
    DataStorageSupplier dataStorageSupplier = createNiceMock(DataStorageSupplier.class);
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);
    HdfsApiSupplier hdfsApiSupplier = createNiceMock(HdfsApiSupplier.class);
    Connect connect = createNiceMock(Connect.class);
    Storage storage = createNiceMock(Storage.class);
    JobImpl jobImpl = createNiceMock(JobImpl.class);
    ResultSet resultSet = createNiceMock(HiveQueryResultSet.class);
    HiveStatement statement = createNiceMock(HiveStatement.class);
    ConnectionDelegate delegate = createNiceMock(ConnectionDelegate.class);
    HiveConnectionWrapper connectionWrapper = createNiceMock(HiveConnectionWrapper.class);
    HiveConnection hiveConnection = createNiceMock(HiveConnection.class);
    HiveJob test = new SQLStatementJob(HiveJob.Type.ASYNC, new String[]{"select * from test"}, "test", "1", "test.log");
    ExecuteJob executeJob = new ExecuteJob(connect, test);
    ActorRef deathwatch = actorSystem.actorOf(Props.create(DeathWatch.class));
    ActorRef operationControl = actorSystem.actorOf(
            Props.create(OperationController.class, actorSystem, deathwatch, viewContext, connectionSupplier, dataStorageSupplier, hdfsApiSupplier), "operationController-test");
    expect(hdfsApiSupplier.get(viewContext)).andReturn(Optional.of(hdfsApi));
    expect(viewContext.getProperties()).andReturn(new HashMap<String, String>()).anyTimes();
    expect(connect.getConnectable(anyObject(AuthParams.class))).andReturn(connectionWrapper);
    expect(connectionWrapper.isOpen()).andReturn(false);
    expect(connectionWrapper.getConnection()).andReturn(Optional.of(hiveConnection)).anyTimes();
    expect(dataStorageSupplier.get(viewContext)).andReturn(storage);
    expect(connectionSupplier.get(viewContext)).andReturn(delegate);
    expect(storage.load(JobImpl.class, "1")).andReturn(jobImpl).anyTimes();
    expect(delegate.createStatement(hiveConnection)).andReturn(statement);
    expect(delegate.execute("select * from test")).andThrow(new SQLException("Syntax error"));
    expect(jobImpl.getDateSubmitted()).andReturn(0L).times(2);
    jobImpl.setStatus(Job.JOB_STATE_RUNNING);
    storage.store(JobImpl.class, jobImpl);
    connectionWrapper.connect();
    jobImpl.setStatus(Job.JOB_STATE_ERROR);
    storage.store(JobImpl.class, jobImpl);
    replay(viewContext, connect, hdfsApiSupplier, dataStorageSupplier, connectionWrapper,
            storage, jobImpl, connectionSupplier, delegate, statement, resultSet);

    operationControl.tell(executeJob, ActorRef.noSender());
    Thread.sleep(5000);
    verify(connect, hdfsApiSupplier, dataStorageSupplier, connectionWrapper,
            storage, jobImpl, connectionSupplier, delegate, statement, resultSet);
  }


}