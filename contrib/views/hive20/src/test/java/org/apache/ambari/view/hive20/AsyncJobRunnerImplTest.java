/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.view.hive20;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import com.google.common.base.Optional;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive20.actor.HiveActor;
import org.apache.ambari.view.hive20.actor.ResultSetIterator;
import org.apache.ambari.view.hive20.actor.message.Connect;
import org.apache.ambari.view.hive20.actor.message.ExecuteJob;
import org.apache.ambari.view.hive20.actor.message.FetchError;
import org.apache.ambari.view.hive20.actor.message.FetchResult;
import org.apache.ambari.view.hive20.actor.message.HiveMessage;
import org.apache.ambari.view.hive20.actor.message.SQLStatementJob;
import org.apache.ambari.view.hive20.actor.message.job.CancelJob;
import org.apache.ambari.view.hive20.actor.message.job.Failure;
import org.apache.ambari.view.hive20.client.AsyncJobRunnerImpl;
import org.apache.ambari.view.hive20.client.ConnectionConfig;
import org.apache.ambari.view.hive20.client.NonPersistentCursor;
import org.apache.ambari.view.hive20.resources.jobs.viewJobs.Job;
import org.apache.hive.jdbc.HiveQueryResultSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.ResultSet;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class AsyncJobRunnerImplTest {

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
  public void testSubmitJob() throws Exception {
    ConnectionConfig connectionConfig = createNiceMock(ConnectionConfig.class);
    SQLStatementJob sqlStatementJob = createNiceMock(SQLStatementJob.class);
    Job job = createNiceMock(Job.class);
    Connect connect = createNiceMock(Connect.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);
    ActorRef controller = actorSystem.actorOf(
            Props.create(TestParent.class));
    AsyncJobRunnerImpl runner = new AsyncJobRunnerImpl(viewContext, controller, actorSystem);
    expect(job.getId()).andReturn("1");
    expect(connect.getJdbcUrl()).andReturn("testjdbc");
    expect(connectionConfig.createConnectMessage("1")).andReturn(connect);
    replay(job, connectionConfig);
    runner.submitJob(connectionConfig, sqlStatementJob, job);
    verify(job, connectionConfig);
  }

  @Test
  public void testCancelJob() throws Exception {
    ViewContext viewContext = createNiceMock(ViewContext.class);
    ActorRef controller = actorSystem.actorOf(
            Props.create(TestParent.class));
    AsyncJobRunnerImpl runner = new AsyncJobRunnerImpl(viewContext, controller, actorSystem);
    runner.cancelJob("1", "test");
  }

  @Test
  public void testGetCursor() throws Exception {
    ViewContext viewContext = createNiceMock(ViewContext.class);
    ActorRef controller = actorSystem.actorOf(
            Props.create(TestParent.class));
    AsyncJobRunnerImpl runner = new AsyncJobRunnerImpl(viewContext, controller, actorSystem);
    Optional<NonPersistentCursor> cursor = runner.getCursor("1", "test");
    assertTrue(cursor.isPresent());
  }


  @Test
  public void testGetError() throws Exception {
    ViewContext viewContext = createNiceMock(ViewContext.class);
    ActorRef controller = actorSystem.actorOf(
            Props.create(TestParent.class));
    AsyncJobRunnerImpl runner = new AsyncJobRunnerImpl(viewContext, controller, actorSystem);
    Optional<Failure> failure = runner.getError("1", "test");
    assertTrue(failure.isPresent());
    assertEquals("failure", failure.get().getMessage());
  }

  private static class TestParent extends HiveActor {

    @Override
    public void handleMessage(HiveMessage hiveMessage) {
      if (hiveMessage.getMessage() instanceof ExecuteJob) {
        ExecuteJob executeJob = (ExecuteJob) hiveMessage.getMessage();
        assertEquals(executeJob.getConnect().getJdbcUrl(), "testjdbc");
      }
      if (hiveMessage.getMessage() instanceof CancelJob) {
        CancelJob cancelJob = (CancelJob) hiveMessage.getMessage();
        assertEquals("1", cancelJob.getJobId());
        assertEquals("test", cancelJob.getUsername());
      }
      if (hiveMessage.getMessage() instanceof FetchError) {
        sender().tell(Optional.of(new Failure("failure", new NullPointerException())), self());
      }
      if (hiveMessage.getMessage() instanceof FetchResult) {
        ResultSet resultSet = createNiceMock(HiveQueryResultSet.class);
        ActorRef rsi = context().actorOf(
                Props.create(ResultSetIterator.class, self(), resultSet));
        sender().tell(Optional.of(rsi), self());
      }
    }
  }
}