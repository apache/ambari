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
import org.apache.ambari.view.hive20.actor.HiveActor;
import org.apache.ambari.view.hive20.actor.ResultSetIterator;
import org.apache.ambari.view.hive20.actor.message.HiveMessage;
import org.apache.ambari.view.hive20.actor.message.ResetCursor;
import org.apache.ambari.view.hive20.actor.message.job.Next;
import org.apache.hive.jdbc.HiveQueryResultSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import static org.easymock.EasyMock.*;


public class ResultSetIteratorTest {

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
  public void testGetNext() throws Exception {
    ResultSet resultSet = createNiceMock(HiveQueryResultSet.class);
    ResultSetMetaData resultSetMetaData = createNiceMock(ResultSetMetaData.class);

    ActorRef parent = actorSystem.actorOf(
            Props.create(TestParent.class));
    ActorRef rsi = actorSystem.actorOf(
            Props.create(ResultSetIterator.class, parent, resultSet));
    expect(resultSet.getMetaData()).andReturn(resultSetMetaData);
    expect(resultSetMetaData.getColumnCount()).andReturn(1);
    expect(resultSetMetaData.getColumnName(1)).andReturn("test");
    expect(resultSetMetaData.getColumnTypeName(1)).andReturn("string");
    replay(resultSet, resultSetMetaData);
    rsi.tell(new Next(), parent);
    Thread.sleep(2000);
    verify(resultSet, resultSetMetaData);

  }

  @Test
  public void testResetCursor() throws Exception {
    ResultSet resultSet = createNiceMock(HiveQueryResultSet.class);

    ActorRef parent = actorSystem.actorOf(
            Props.create(TestParent.class));
    ActorRef rsi = actorSystem.actorOf(
            Props.create(ResultSetIterator.class, parent, resultSet));
    resultSet.beforeFirst();
    replay(resultSet);
    rsi.tell(new ResetCursor(), parent);
    Thread.sleep(2000);
    verify(resultSet);

  }


  private static class TestParent extends HiveActor {

    @Override
    public void handleMessage(HiveMessage hiveMessage) {

    }
  }


}