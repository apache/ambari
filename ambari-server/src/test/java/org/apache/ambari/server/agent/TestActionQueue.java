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
package org.apache.ambari.server.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.ambari.server.agent.AgentCommand.AgentCommandType;
import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestActionQueue {

  private static final Logger LOG = LoggerFactory.getLogger(TestActionQueue.class);

  private static int threadCount = 100;
  static class ActionQueueOperation implements Runnable {

    enum OpType {
      ENQUEUE,
      DEQUEUE,
      DEQUEUEALL,
      CHECKPENDING,
      UPDATEHOSTLIST
    }

    private volatile boolean shouldRun = true;
    private long [] opCounts;
    private ActionQueue actionQueue;
    private OpType operation;
    private String[] hosts;

    public ActionQueueOperation(ActionQueue aq, String [] hosts, OpType op) {
      actionQueue = aq;
      operation = op;
      this.hosts = hosts;
      opCounts = new long [hosts.length];
      for (int i = 0; i < hosts.length; i++) {
        opCounts[i] = 0;
      }
    }

    public long [] getOpCounts() {
      return opCounts;
    }

    public void stop() {
      shouldRun = false;
    }

    @Override
    public void run() {
      try {
        switch (operation) {
          case ENQUEUE:
            enqueueOp();
            break;
          case DEQUEUE:
            dequeueOp();
            break;
          case DEQUEUEALL:
            dequeueAllOp();
            break;
          case CHECKPENDING:
            checkPending();
            break;
          case UPDATEHOSTLIST:
            updateHostList();
            break;
        }
      } catch (Exception ex) {
        LOG.error("Failure", ex);
        throw new RuntimeException("Failure", ex);
      }
    }

    private void checkPending() throws InterruptedException {
      while (shouldRun) {
        int index = 0;
        for (String host: hosts) {
          actionQueue.hasPendingTask(host);
          opCounts[index]++;
          index++;
        }
        Thread.sleep(1);
      }
    }

    private void updateHostList() throws InterruptedException {
      HashSet<String> hostsWithTasks = new HashSet<>();
      while (shouldRun) {
        for (String host: hosts) {
          hostsWithTasks.add(host);
          if (hostsWithTasks.size() % 2 == 0) {
            actionQueue.updateListOfHostsWithPendingTask(hostsWithTasks);
          } else {
            actionQueue.updateListOfHostsWithPendingTask(null);
          }
          opCounts[0]++;
        }
        Thread.sleep(1);
      }
    }

    private void enqueueOp() throws InterruptedException {
      while (shouldRun) {
        int index = 0;
        for (String host: hosts) {
          actionQueue.enqueue(host, new StatusCommand());
          opCounts[index]++;
          index++;
        }
        Thread.sleep(1);
      }
    }

    private void dequeueOp() throws InterruptedException {
      while (shouldRun) {
        int index = 0;
        for (String host: hosts) {
          AgentCommand cmd = actionQueue.dequeue(host);
          if (cmd != null) {
            opCounts[index]++;
          }
          index++;
        }
        Thread.sleep(1);
      }
    }

    private void dequeueAllOp() throws InterruptedException {
      while (shouldRun) {
        int index = 0;
        for (String host : hosts) {
          List<AgentCommand> cmds = actionQueue.dequeueAll(host);
          if (cmds != null && !cmds.isEmpty()) {
            opCounts[index] += cmds.size();
          }
          index++;
        }
        Thread.sleep(1);
      }
    }
  }

  @Test
  public void testConcurrentOperations() throws InterruptedException {
    ActionQueue aq = new ActionQueue();
    String[] hosts = new String[] { "h0", "h1", "h2", "h3", "h4", "h5", "h6",
        "h7", "h8", "h9" };

    ActionQueueOperation[] enqueOperators = new ActionQueueOperation[threadCount];
    ActionQueueOperation[] dequeOperators = new ActionQueueOperation[threadCount];
    ActionQueueOperation[] dequeAllOperators = new ActionQueueOperation[threadCount];

    List<Thread> producers = new ArrayList<>();
    List<Thread> consumers = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      dequeOperators[i] = new ActionQueueOperation(aq, hosts,
          ActionQueueOperation.OpType.DEQUEUE);
      Thread t = new Thread(dequeOperators[i]);
      consumers.add(t);
      t.start();
    }

    for (int i = 0; i < threadCount; i++) {
      enqueOperators[i] = new ActionQueueOperation(aq, hosts,
          ActionQueueOperation.OpType.ENQUEUE);
      Thread t = new Thread(enqueOperators[i]);
      producers.add(t);
      t.start();
    }

    for (int i = 0; i < threadCount; i++) {
      dequeAllOperators[i] = new ActionQueueOperation(aq, hosts,
          ActionQueueOperation.OpType.DEQUEUEALL);
      Thread t = new Thread(dequeAllOperators[i]);
      consumers.add(t);
      t.start();
    }

    // Run for some time
    Thread.sleep(100);

    // Stop the enqueue
    for (int i = 0; i < threadCount; i++) {
      enqueOperators[i].stop();
    }

    for (Thread producer : producers) {
      producer.join();
    }

    // Give time to get everything dequeued
    boolean allDequeued = false;
    while (!allDequeued) {
      Thread.sleep(10);
      allDequeued = true;
      for (String host: hosts) {
        if (aq.size(host) > 0) {
          allDequeued = false;
          break;
        }
      }
    }

    // Stop all threads
    for (int i = 0; i < threadCount; i++) {
      dequeOperators[i].stop();
      dequeAllOperators[i].stop();
    }

    for (Thread consumer : consumers) {
      consumer.join();
    }

    for (int h = 0; h<hosts.length; h++) {
      long opsEnqueued = 0;
      long opsDequeued = 0;
      for (int i = 0; i < threadCount; i++) {
        opsEnqueued += enqueOperators[i].getOpCounts()[h];
        opsDequeued += dequeOperators[i].getOpCounts()[h];
        opsDequeued += dequeAllOperators[i].getOpCounts()[h];
      }
      assertTrue(opsEnqueued != 0); //Prevent degenerate case of all zeros.
      assertEquals(0, aq.size(hosts[h])); //Everything should be dequeued
      LOG.info("Host: " + hosts[h] + ", opsEnqueued: " + opsEnqueued
          + ", opsDequeued: " + opsDequeued);
      assertEquals(opsDequeued, opsEnqueued);
    }
  }

  @Test
  public void testConcurrentHostCheck() throws InterruptedException {
    ActionQueue aq = new ActionQueue();
    String[] hosts = new String[] { "h0", "h1", "h2", "h3", "h4" };

    ActionQueueOperation[] hostCheckers = new ActionQueueOperation[threadCount];
    ActionQueueOperation[] hostUpdaters = new ActionQueueOperation[threadCount];

    List<Thread> producers = new ArrayList<>();
    List<Thread> consumers = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      hostCheckers[i] = new ActionQueueOperation(aq, hosts,
                                                   ActionQueueOperation.OpType.CHECKPENDING);
      Thread t = new Thread(hostCheckers[i]);
      consumers.add(t);
      t.start();
    }

    for (int i = 0; i < threadCount; i++) {
      hostUpdaters[i] = new ActionQueueOperation(aq, hosts,
                                                   ActionQueueOperation.OpType.UPDATEHOSTLIST);
      Thread t = new Thread(hostUpdaters[i]);
      producers.add(t);
      t.start();
    }

    // Run for some time
    Thread.sleep(100);

    for (int i = 0; i < threadCount; i++) {
      hostUpdaters[i].stop();
    }

    for (Thread producer : producers) {
      producer.join();
    }

    for (int i = 0; i < threadCount; i++) {
      hostCheckers[i].stop();
    }

    for (Thread consumer : consumers) {
      consumer.join();
    }

    int totalChecks = 0;
    int totalUpdates = 0;
    for (int i = 0; i < threadCount; i++) {
      totalChecks += hostUpdaters[i].getOpCounts()[0];
      for (int h = 0; h<hosts.length; h++) {
        totalUpdates += hostCheckers[i].getOpCounts()[h];
      }
    }
    LOG.info("Report: totalChecks: " + totalChecks + ", totalUpdates: " + totalUpdates);

    HashSet<String> hostsWithPendingtasks = new HashSet<>();
    aq.updateListOfHostsWithPendingTask(hostsWithPendingtasks);
    hostsWithPendingtasks.add("h1");
    aq.updateListOfHostsWithPendingTask(hostsWithPendingtasks);
    assertTrue(aq.hasPendingTask("h1"));
    assertFalse(aq.hasPendingTask("h2"));

    hostsWithPendingtasks.add("h1");
    hostsWithPendingtasks.add("h2");
    aq.updateListOfHostsWithPendingTask(hostsWithPendingtasks);
    assertTrue(aq.hasPendingTask("h1"));
    assertTrue(aq.hasPendingTask("h2"));

    hostsWithPendingtasks.clear();
    hostsWithPendingtasks.add("h2");
    aq.updateListOfHostsWithPendingTask(hostsWithPendingtasks);
    assertFalse(aq.hasPendingTask("h1"));
    assertTrue(aq.hasPendingTask("h2"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testDequeueCommandType() throws Exception {
    ActionQueue queue = new ActionQueue();
    String c6401 = "c6401.ambari.apache.org";
    String c6402 = "c6402.ambari.apache.org";

    queue.enqueue(c6401,
        EasyMock.createMockBuilder(ExecutionCommand.class).createNiceMock());

    queue.enqueue(c6401,
        EasyMock.createMockBuilder(StatusCommand.class).createNiceMock());

    queue.enqueue(c6401,
        EasyMock.createMockBuilder(AlertDefinitionCommand.class).createNiceMock());

    queue.enqueue(c6401,
        EasyMock.createMockBuilder(StatusCommand.class).createNiceMock());

    queue.enqueue(c6401,
        EasyMock.createMockBuilder(AlertDefinitionCommand.class).createNiceMock());

    queue.enqueue(c6401,
        EasyMock.createMockBuilder(StatusCommand.class).createNiceMock());

    queue.enqueue(c6401,
        EasyMock.createMockBuilder(AlertDefinitionCommand.class).createNiceMock());

    queue.enqueue(c6402,
        EasyMock.createMockBuilder(ExecutionCommand.class).createNiceMock());

    queue.enqueue(c6402,
        EasyMock.createMockBuilder(StatusCommand.class).createNiceMock());

    queue.enqueue(c6402,
        EasyMock.createMockBuilder(AlertDefinitionCommand.class).createNiceMock());

    assertEquals(7, queue.size(c6401));

    List<AgentCommand> commands = queue.dequeue(c6401,
        AgentCommandType.ALERT_DEFINITION_COMMAND);

    assertNotNull(commands);
    assertEquals(3, commands.size());
    assertEquals(4, queue.size(c6401));
    assertEquals(3, queue.size(c6402));
  }
}
