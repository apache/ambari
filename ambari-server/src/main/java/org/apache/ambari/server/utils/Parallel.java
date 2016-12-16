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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.persistence.internal.helper.ConcurrencyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <b>TEMPORARILY DO NOT USE WITH JPA ENTITIES</b>
 * <p/>
 * Deprecated since the use of this class to access JPA from multiple Ambari
 * threads seems to cause thread liveliness problems in
 * {@link ConcurrencyManager}.
 * <p/>
 * This class provides support for parallel loops. Iterations in the loop run in
 * parallel in parallel loops.
 */
@Deprecated
public class Parallel {

  /**
   * Max pool size
   */
  private static final int MAX_POOL_SIZE = Math.max(8, Runtime.getRuntime().availableProcessors());

  /**
   * Keep alive time (15 min)
   */
  // !!! changed from 1 second because EclipseLink was making threads idle and
  // they kept timing out
  private static final int KEEP_ALIVE_TIME_MINUTES = 15;

  /**
   * Poll duration (10 secs)
   */
  private static final int POLL_DURATION_MILLISECONDS = 10000;

  /**
   * Core pool size
   */
  private static final int CORE_POOL_SIZE = 2;

  /**
   * Logger
   */
  private static final Logger LOG = LoggerFactory.getLogger(Parallel.class);

  /**
   *  Thread pool executor
   */
  private static ExecutorService executor = initExecutor();

  /**
   * Initialize executor
   *
   * @return
   */
  private static ExecutorService initExecutor() {

    BlockingQueue<Runnable> blockingQueue = new SynchronousQueue<Runnable>(); // Using synchronous queue

    // Create thread pool
    ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
        CORE_POOL_SIZE,                           // Core pool size
        MAX_POOL_SIZE,                            // Max pool size
        KEEP_ALIVE_TIME_MINUTES,                  // Keep alive time for idle threads
        TimeUnit.MINUTES,
        blockingQueue,                            // Using synchronous queue
        new ParallelLoopsThreadFactory(),         // Thread pool factory to use
        new ThreadPoolExecutor.CallerRunsPolicy() // Rejected tasks will run on calling thread.
    );
    threadPool.allowCoreThreadTimeOut(true);
    LOG.debug(
        "Parallel library initialized: ThreadCount = {}, CurrentPoolSize = {}, CorePoolSize = {}, MaxPoolSize = {}",
        Thread.activeCount(), threadPool.getPoolSize(), threadPool.getCorePoolSize(), threadPool.getMaximumPoolSize());
    return threadPool;
  }

  /**
   * Executes a "for" parallel loop operation over all items in the data source in which iterations run in parallel.
   *
   * @param source      Data source to iterate over
   * @param loopBody    The loop body that is invoked once per iteration
   * @param <T>         The type of data in the source
   * @param <R>         The type of data to be returned
   * @return            {@link ParallelLoopResult} Parallel loop result
   */
  public static <T, R> ParallelLoopResult<R> forLoop(
      List<T> source,
      final LoopBody<T, R> loopBody) {

    if(source == null || source.isEmpty()) {
      return new ParallelLoopResult<>(true, Collections.<R>emptyList());
    }
    return forLoop(source, 0, source.size(), loopBody);
  }

  /**
   * Executes a "for" parallel loop operation in which iterations run in parallel.
   *
   * @param source      Data source to iterate over
   * @param startIndex  The loop start index, inclusive
   * @param endIndex    The loop end index, exclusive
   * @param loopBody    The loop body that is invoked once per iteration
   * @param <T>         The type of data in the source
   * @param <R>         The type of data to be returned
   * @return            {@link ParallelLoopResult} Parallel loop result
   *
   */
  public static <T, R> ParallelLoopResult<R> forLoop(
      final List<T> source,
      int startIndex,
      int endIndex,
      final LoopBody<T, R> loopBody) {

    if(source == null || source.isEmpty() || startIndex == endIndex) {
      return new ParallelLoopResult<>(true, Collections.<R>emptyList());
    }
    if(startIndex < 0 || startIndex >= source.size()) {
      throw new IndexOutOfBoundsException("startIndex is out of bounds");
    }
    if(endIndex < 0 || endIndex > source.size()) {
      throw new IndexOutOfBoundsException("endIndex is out of bounds");
    }
    if(startIndex > endIndex) {
      throw new IndexOutOfBoundsException("startIndex > endIndex");
    }
    if(source.size() == 1 || (endIndex - startIndex) == 1) {
      // Don't spawn a new thread for a single element list
      List<R> result = Collections.singletonList(loopBody.run(source.get(startIndex)));
      return new ParallelLoopResult<R>(true, result);
    }

    // Create a completion service for each call
    CompletionService<ResultWrapper<R>> completionService = new ExecutorCompletionService<ResultWrapper<R>>(executor);

    List<Future<ResultWrapper<R>>> futures = new LinkedList<Future<ResultWrapper<R>>>();
    for (int i = startIndex; i < endIndex; i++) {
      final Integer k = i;
      Future<ResultWrapper<R>> future = completionService.submit(new Callable<ResultWrapper<R>>() {
        @Override
        public ResultWrapper<R> call() throws Exception {
          ResultWrapper<R> res = new ResultWrapper<R>();
          res.index = k;
          res.result = loopBody.run(source.get(k));
          return res;
        }
      });
      futures.add(future);
    }

    boolean completed = true;
    R[] result = (R[]) new Object[futures.size()];
    for (int i = 0; i < futures.size(); i++) {
      try {
        Future<ResultWrapper<R>> futureResult = null;
        try {
          futureResult = completionService.poll(POLL_DURATION_MILLISECONDS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
          LOG.error("Caught InterruptedException in Parallel.forLoop", e);
        }
        if (futureResult == null) {
          // Timed out! no progress was made during the last poll duration. Abort the threads and cancel the threads.
          LOG.error("Completion service in Parallel.forLoop timed out!");
          completed = false;
          for(int fIndex = 0; fIndex < futures.size(); fIndex++) {
            Future<ResultWrapper<R>> future = futures.get(fIndex);
            if(future.isDone()) {
              LOG.debug("    Task - {} has already completed", fIndex);
            } else if(future.isCancelled()) {
              LOG.debug("    Task - {} has already been cancelled", fIndex);
            } else if(!future.cancel(true)) {
              LOG.debug("    Task - {} could not be cancelled", fIndex);
            } else {
              LOG.debug("    Task - {} successfully cancelled", fIndex);
            }
          }
          // Finished processing all futures
          break;
        } else {
          ResultWrapper<R> res = futureResult.get();
          if(res.result != null) {
            result[res.index] = res.result;
          } else {
            LOG.error("Result is null for {}", res.index);
            completed = false;
          }
        }
      } catch (InterruptedException e) {
        LOG.error("Caught InterruptedException in Parallel.forLoop", e);
        completed = false;
      } catch (ExecutionException e) {
        LOG.error("Caught ExecutionException in Parallel.forLoop", e);
        completed = false;
      } catch (CancellationException e) {
        LOG.error("Caught CancellationException in Parallel.forLoop", e);
        completed = false;
      }
    }
    // Return parallel loop result
    return new ParallelLoopResult<R>(completed, Arrays.asList(result));
  }

  /**
   * A custom {@link ThreadFactory} for the threads that will handle
   * {@link org.apache.ambari.server.utils.Parallel} loop iterations.
   */
  private static final class ParallelLoopsThreadFactory implements ThreadFactory {

    private static final AtomicInteger threadId = new AtomicInteger(1);

    /**
     * {@inheritDoc}
     */
    @Override
    public Thread newThread(Runnable r) {
      Thread thread = Executors.defaultThreadFactory().newThread(r);
      thread.setName("parallel-loop-" + threadId.getAndIncrement());
      return thread;
    }
  }

  /**
   * Result wrapper for Parallel.forLoop used internally
   * @param <R> Type of result to wrap
   */
  private static final class ResultWrapper<R> {
    int index;
    R result;
  }
}
