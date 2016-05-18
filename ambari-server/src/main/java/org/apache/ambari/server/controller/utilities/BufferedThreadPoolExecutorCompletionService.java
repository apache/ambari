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
package org.apache.ambari.server.controller.utilities;

import java.util.Queue;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * An {@link ExecutorCompletionService} which takes a {@link ThreadPoolExecutor}
 * and uses its thread pool to execute tasks - buffering any tasks that
 * overflow. Such buffered tasks are later re-submitted to the executor when
 * finished tasks are polled or taken.
 * 
 * This class overrides the {@link ThreadPoolExecutor}'s
 * {@link RejectedExecutionHandler} to collect overflowing tasks.
 * 
 * The {@link ScalingThreadPoolExecutor} can be used in conjunction with this
 * class to provide an efficient buffered, scaling thread pool implementation.
 * 
 * @param <V>
 */
public class BufferedThreadPoolExecutorCompletionService<V> extends ExecutorCompletionService<V> {

  private ThreadPoolExecutor executor;
  private Queue<Runnable> overflowQueue;

  public BufferedThreadPoolExecutorCompletionService(ThreadPoolExecutor executor) {
    super(executor);
    this.executor = executor;
    this.overflowQueue = new LinkedBlockingQueue<Runnable>();
    this.executor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
      /**
       * Once the ThreadPoolExecutor is at full capacity, it starts to reject
       * submissions which are queued for later submission.
       */
      @Override
      public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        overflowQueue.add(r);
      }
    });
  }

  @Override
  public Future<V> take() throws InterruptedException {
    Future<V> take = super.take();
    if (!executor.isTerminating() && !overflowQueue.isEmpty() && executor.getActiveCount() < executor.getMaximumPoolSize()) {
      Runnable overflow = overflowQueue.poll();
      if (overflow != null) {
        executor.execute(overflow);
      }
    }
    return take;
  }

  @Override
  public Future<V> poll() {
    Future<V> poll = super.poll();
    if (!executor.isTerminating() && !overflowQueue.isEmpty() && executor.getActiveCount() < executor.getMaximumPoolSize()) {
      Runnable overflow = overflowQueue.poll();
      if (overflow != null) {
        executor.execute(overflow);
      }
    }
    return poll;
  }

  @Override
  public Future<V> poll(long timeout, TimeUnit unit) throws InterruptedException {
    Future<V> poll = super.poll(timeout, unit);
    if (!executor.isTerminating() && !overflowQueue.isEmpty() && executor.getActiveCount() < executor.getMaximumPoolSize()) {
      Runnable overflow = overflowQueue.poll();
      if (overflow != null) {
        executor.execute(overflow);
      }
    }
    return poll;
  }
}
