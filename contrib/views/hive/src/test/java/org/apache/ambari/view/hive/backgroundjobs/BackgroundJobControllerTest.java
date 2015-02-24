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

package org.apache.ambari.view.hive.backgroundjobs;

import org.apache.ambari.view.hive.BaseHiveTest;
import org.junit.Assert;
import org.junit.Test;

public class BackgroundJobControllerTest extends BaseHiveTest {

  private static final long MAX_WAIT_TIME = 2000;

  @Test
  public void testStartJob() throws Exception {
    BackgroundJobController backgroundJobController = new BackgroundJobController(context);

    HangingRunnable runnable = new HangingRunnable();
    backgroundJobController.startJob("key", runnable);

    assertStateIs(backgroundJobController, "key", Thread.State.RUNNABLE);

    runnable.goOn();
    assertStateIs(backgroundJobController, "key", Thread.State.TERMINATED);
  }

  @Test
  public void testInterrupt() throws Exception {
    BackgroundJobController backgroundJobController = new BackgroundJobController(context);

    HangingRunnable runnable = new HangingRunnable();
    backgroundJobController.startJob("key", runnable);

    assertStateIs(backgroundJobController, "key", Thread.State.RUNNABLE);

    backgroundJobController.interrupt("key");
    assertStateIs(backgroundJobController, "key", Thread.State.TERMINATED);
  }

  private void assertStateIs(BackgroundJobController backgroundJobController, String key, Thread.State state) throws InterruptedException {
    long start = System.currentTimeMillis();
    while (backgroundJobController.state(key) != state) {
      Thread.sleep(100);
      if (System.currentTimeMillis() - start > MAX_WAIT_TIME)
        break;
    }
    Assert.assertEquals(state, backgroundJobController.state(key));
  }

  private static class HangingRunnable implements Runnable {
    private boolean waitMe = true;

    @Override
    public void run() {
      while(waitMe && !Thread.interrupted());
    }

    public void goOn() {
      this.waitMe = false;
    }
  }
}