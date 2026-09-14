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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.zookeeper.Watcher.Event.KeeperState.SyncConnected;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * I can open connections to ZooKeeper
 */
public class ZkConnection {

  interface PendingConnection {
    ZooKeeper client();

    boolean isConnected();

    void close() throws InterruptedException;
  }

  @FunctionalInterface
  interface ConnectionFactory {
    PendingConnection create(String serverAddress, int sessionTimeoutMillis, Watcher watcher)
        throws IOException;
  }

  /**
   * Opens a connection to zookeeper and waits until the connection established
   */
  public static ZooKeeper open(String serverAddress, int sessionTimeoutMillis, int connectionTimeoutMillis)
    throws IOException, InterruptedException, IllegalStateException
  {
    return open(serverAddress, sessionTimeoutMillis, connectionTimeoutMillis,
        (address, timeout, watcher) -> {
          ZooKeeper client = new ZooKeeper(address, timeout, watcher);
          return new PendingConnection() {
            @Override
            public ZooKeeper client() {
              return client;
            }

            @Override
            public boolean isConnected() {
              return client.getState().isConnected();
            }

            @Override
            public void close() throws InterruptedException {
              client.close();
            }
          };
        });
  }

  static ZooKeeper open(String serverAddress, int sessionTimeoutMillis, int connectionTimeoutMillis,
      ConnectionFactory factory) throws IOException, InterruptedException, IllegalStateException {
    final CountDownLatch connSignal = new CountDownLatch(1);
    PendingConnection pending = factory.create(serverAddress, sessionTimeoutMillis, new Watcher() {
      @Override
      public void process(WatchedEvent event) {
        if (event.getState() == SyncConnected) {
          connSignal.countDown();
        }
      }
    });
    boolean connected = false;
    try {
      if (!connSignal.await(connectionTimeoutMillis, MILLISECONDS)
          || !pending.isConnected()) {
        throw new IllegalStateException("ZooKeeper connection timed out before SyncConnected");
      }
      connected = true;
      return pending.client();
    } finally {
      if (!connected) {
        try {
          pending.close();
        } catch (InterruptedException closeInterrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }
}
