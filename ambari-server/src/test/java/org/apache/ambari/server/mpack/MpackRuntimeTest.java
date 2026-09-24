/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.mpack;

import static org.junit.Assert.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.ambari.server.state.StackId;
import org.junit.Test;

public class MpackRuntimeTest {
  @Test
  public void maintenanceIsLimitedToDeclaredConsumersAndConfigurationTypes() {
    MpackRuntime runtime = new MpackRuntime();
    runtime.writeLock().lock();
    try { runtime.reserve("operation", List.of(new MpackScope("BASE", "1.0", "DATABASE", Set.of("database-site")))); }
    finally { runtime.writeLock().unlock(); }
    assertThrows(MpackException.class, () -> runtime.requireServiceReady(new StackId("BASE", "1.0"), "DATABASE"));
    runtime.requireServiceReady(new StackId("BASE", "1.0"), "QUEUE");
    runtime.requireServiceReady(new StackId("BASE", "2.0"), "DATABASE");
    runtime.requireExecutionReady();
    runtime.requireConfigurationReady(new StackId("BASE", "1.0"), "queue-site");
    assertThrows(MpackException.class, () -> runtime.requireConfigurationReady(new StackId("BASE", "1.0"), "database-site"));
  }

  @Test
  public void publicationWaitsForAdmittedMutationThenRestoresScopedReadFailure() throws Exception {
    MpackRuntime runtime = new MpackRuntime();
    CountDownLatch entered = new CountDownLatch(1), published = new CountDownLatch(1);
    runtime.readLock().lock();
    Thread publisher = new Thread(() -> {
      entered.countDown();
      runtime.writeLock().lock();
      try {
        runtime.reserve("operation", List.of(new MpackScope("BASE", "1.0", "DATABASE", Set.of())));
        runtime.markUnreadable();
        published.countDown();
      } finally { runtime.writeLock().unlock(); }
    });
    publisher.start();
    try {
      assertTrue(entered.await(5, TimeUnit.SECONDS));
      assertFalse(published.await(50, TimeUnit.MILLISECONDS));
    } finally { runtime.readLock().unlock(); }
    assertTrue(published.await(5, TimeUnit.SECONDS));
    publisher.join(5000);
    runtime.requireReadable(new StackId("BASE", "1.0"), "QUEUE");
    assertThrows(MpackException.class, () -> runtime.requireReadable(new StackId("BASE", "1.0"), "DATABASE"));
  }
}
