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
package org.apache.ambari.tools.zk;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public class ZkConnectionTest {

  @Test
  public void interruptedOpenClosesThePendingClient() throws Exception {
    AtomicBoolean closed = new AtomicBoolean();
    ZkConnection.ConnectionFactory factory = (address, timeout, watcher) ->
        new ZkConnection.PendingConnection() {
          @Override
          public org.apache.zookeeper.ZooKeeper client() {
            return null;
          }

          @Override
          public boolean isConnected() {
            return false;
          }

          @Override
          public void close() {
            closed.set(true);
          }
        };

    Thread.currentThread().interrupt();
    try {
      ZkConnection.open("unreachable.example.test:2181", 5000, 30000, factory);
      fail("expected interrupted wait");
    } catch (InterruptedException expected) {
      // Expected.
    }

    assertTrue("pending ZooKeeper client was not closed", closed.get());
  }

  @Test
  public void timedOutOpenClosesThePendingClient() throws Exception {
    AtomicBoolean closed = new AtomicBoolean();
    ZkConnection.ConnectionFactory factory = (address, timeout, watcher) ->
        new ZkConnection.PendingConnection() {
          @Override
          public org.apache.zookeeper.ZooKeeper client() {
            return null;
          }

          @Override
          public boolean isConnected() {
            return false;
          }

          @Override
          public void close() {
            closed.set(true);
          }
        };

    try {
      ZkConnection.open("unreachable.example.test:2181", 5000, 0, factory);
      fail("expected connection timeout");
    } catch (IllegalStateException expected) {
      // Expected.
    }

    assertTrue("pending ZooKeeper client was not closed", closed.get());
  }
}
