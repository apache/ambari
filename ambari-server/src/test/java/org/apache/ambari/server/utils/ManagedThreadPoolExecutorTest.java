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
package org.apache.ambari.server.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import junit.framework.Assert;

public class ManagedThreadPoolExecutorTest {

  @Test
  public void testGetHostAndPortFromProperty() {

    ManagedThreadPoolExecutor  topologyTaskExecutor = new ManagedThreadPoolExecutor(1,
            1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());
    Future<Boolean> feature = topologyTaskExecutor.submit(new Callable<Boolean>() {
      @Override
      public Boolean call() {
        return Boolean.TRUE;
      }
    });

    Assert.assertTrue(!topologyTaskExecutor.isRunning());
    topologyTaskExecutor.start();
    Assert.assertTrue(topologyTaskExecutor.isRunning());
    topologyTaskExecutor.stop();
    Assert.assertTrue(!topologyTaskExecutor.isRunning());

  }
}
