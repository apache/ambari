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
import akka.actor.Inbox;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import org.apache.ambari.view.hive2.actor.OperationController;
import org.apache.ambari.view.hive2.actor.message.AsyncJob;
import org.apache.ambari.view.hive2.actor.message.ExecuteJob;
import org.apache.ambari.view.hive2.actor.message.FetchResult;
import org.apache.ambari.view.hive2.actor.message.JobSubmitted;
import org.apache.ambari.view.hive2.actor.message.job.AsyncExecutionFailed;
import org.apache.ambari.view.hive2.actor.message.job.Next;
import org.apache.ambari.view.hive2.internal.ConnectionException;
import org.apache.ambari.view.hive2.internal.Either;
import org.apache.ambari.view.hive2.internal.HiveResult;
import org.apache.hive.jdbc.HiveStatement;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import scala.concurrent.duration.Duration;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertTrue;

public class AsyncQueriesTest extends MockSupport {


    private static ActorSystem actorSystem;

    @Before
    public void setup() {
        actorSystem = ActorSystem.create("TestingActorSystem");
        Logger.getRootLogger().setLevel(Level.DEBUG);
    }

    @After
    public void teardown() {
        JavaTestKit.shutdownActorSystem(actorSystem);
    }


    /**
     * Test the actor inactivity timer
     * Send the actor a message and dont care about the result
     *
     * @throws SQLException
     * @throws ConnectionException
     * @throws InterruptedException
     */
    @Test
    @Ignore
    public void testAsyncQuerySubmission() throws SQLException, ConnectionException, InterruptedException {
        mockDependencies();
        setUpDefaultExpectations();
        String[] statements = {"select * from test"};
        AsyncJob job = new AsyncJob("10", "admin", statements, "tst.log", viewContext);
        for (String s : statements) {
            expect(((HiveStatement) statement).executeAsync(s)).andReturn(true);
        }

        ActorRef operationControl = actorSystem.actorOf(
                Props.create(OperationController.class, actorSystem, connectionSupplier, supplier, hdfsSupplier), "operationController-test");

        Inbox inbox = Inbox.create(actorSystem);

        ExecuteJob executeJob = new ExecuteJob(connect, job);
        inbox.send(operationControl, executeJob);

        replay(connection, resultSet, resultSetMetaData, statement, viewContext, connect, connectable, hdfsSupplier, hdfsApi, supplier, connectionSupplier);

        try {

            Object submitted = inbox.receive(Duration.create(1, TimeUnit.MINUTES));

            assertTrue(submitted instanceof JobSubmitted);
            inbox.send(operationControl,new FetchResult("10","admin"));

            Either<ActorRef, AsyncExecutionFailed> receive = (Either<ActorRef, AsyncExecutionFailed>) inbox.receive(Duration.create(1, TimeUnit.MINUTES));

            inbox.send(receive.getLeft(),new Next());

            HiveResult result = (HiveResult)inbox.receive(Duration.create(1, TimeUnit.MINUTES));

            List<HiveResult.Row> rows = result.getRows();
            System.out.println(rows);

            verify(connection, resultSet, resultSetMetaData, statement, viewContext, connect, connectable, hdfsSupplier, hdfsApi, supplier);


        } catch (Throwable e) {
            e.printStackTrace();
        }


    }
}
