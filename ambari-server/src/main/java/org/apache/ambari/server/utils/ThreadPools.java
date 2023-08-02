/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.utils;


import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;


import org.apache.ambari.server.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ThreadPools {

  /**
   * Determines the need of waiting for all tasks to be processed:
   * true - next task would be processed
   * false - next task would be cancelled
   * @param <T>
   */
  public interface ThreadPoolFutureResult<T> {
    Boolean waitForNextTask(T taskResult);
  }

  private static final String AGENT_COMMAND_PUBLISHER_POOL_NAME = "agent-command-publisher";
  private static final String DEFAULT_FORK_JOIN_POOL_NAME = "default-fork-join-pool";

  private static final Logger LOG = LoggerFactory.getLogger(ThreadPools.class);

  private final Configuration configuration;

  private ForkJoinPool agentPublisherCommandsPool;
  private ForkJoinPool defaultForkJoinPool;

  @Inject
  public ThreadPools(Configuration configuration) {
    this.configuration = configuration;
  }

  private void logThreadPoolCreation(String name, int size) {
    LOG.info(String.format("Creating '%s' thread pool with configured size %d", name, size));
  }

  private ForkJoinPool.ForkJoinWorkerThreadFactory createNamedFactory(String name) {
    return pool -> {
      ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
      worker.setName(name + "-" + worker.getPoolIndex());
      return worker;
    };
  }

  private Boolean forkJoinPoolShutdown(ForkJoinPool pool, boolean forced) {
    if (pool == null) {
      return true;
    }
    if (forced) {
      pool.shutdownNow();
    } else {
      pool.shutdown();
    }
    return pool.isShutdown();
  }

  public ForkJoinPool getAgentPublisherCommandsPool() {
    if (agentPublisherCommandsPool == null){
      logThreadPoolCreation(AGENT_COMMAND_PUBLISHER_POOL_NAME, configuration.getAgentCommandPublisherThreadPoolSize());
      agentPublisherCommandsPool = new ForkJoinPool(
        configuration.getAgentCommandPublisherThreadPoolSize(),
        createNamedFactory(AGENT_COMMAND_PUBLISHER_POOL_NAME),
        (t, e) -> {
          LOG.error("Unexpected exception in thread: " + t, e);
          throw new RuntimeException(e);
        },
        false
      );
    }
    return agentPublisherCommandsPool;
  }

  public ForkJoinPool getDefaultForkJoinPool() {
    if (defaultForkJoinPool == null){
      logThreadPoolCreation(DEFAULT_FORK_JOIN_POOL_NAME, configuration.getDefaultForkJoinPoolSize());
      defaultForkJoinPool = new ForkJoinPool(
        configuration.getDefaultForkJoinPoolSize(),
        createNamedFactory(DEFAULT_FORK_JOIN_POOL_NAME),
        (t, e) -> {
          LOG.error("Unexpected exception in thread: " + t, e);
          throw new RuntimeException(e);
        },
        false
      );
    }
    return defaultForkJoinPool;
  }

  public void shutdownDefaultForkJoinPool(boolean force){
    if (forkJoinPoolShutdown(defaultForkJoinPool, force)) {
      defaultForkJoinPool = null;
    }
  }

  public void shutdownAgentPublisherCommandsPool(boolean force){
    if (forkJoinPoolShutdown(agentPublisherCommandsPool, force)) {
      agentPublisherCommandsPool = null;
    }
  }

  /**
   * Run {@code task} in parallel
   *
   * @param factoryName name of thread pool
   * @param threadPoolSize maximum amount of threads to use
   * @param operation operation caption
   * @param tasks list of callables to be submitted to thread pool
   * @param taskResultFunc process result of each task with
   * @param <T> return type of task callable
   * @throws Exception
   */
  public <T> void parallelOperation(String factoryName, int threadPoolSize, String operation,
                                    List<Callable<T>> tasks, ThreadPoolFutureResult<T> taskResultFunc)
  throws Exception{

    logThreadPoolCreation(factoryName, threadPoolSize);
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(factoryName).build();
    ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize, threadFactory);
    CompletionService<T> completionService = new ExecutorCompletionService<>(executorService);
    List<Future<T>> futures = tasks.stream().map(completionService::submit).collect(Collectors.toList());

    LOG.info("Processing {} {} concurrently...", futures.size(), operation);
    T t;
    try {
      for( int i = 0; i < futures.size(); i++ ) {
        Future<T> future = completionService.take();
        t = future.get();

        if (!taskResultFunc.waitForNextTask(t)){
          break;
        }
      }
    } finally {
      futures.stream()
        .filter(x -> !x.isCancelled() && !x.isDone())
        .forEach(x -> x.cancel(true));

      executorService.shutdown();
    }
  }

  public static ExecutorService getSingleThreadedExecutor(String threadPoolName) {
    return Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat(threadPoolName + "-%d")
        .build()
    );
  }

  @Override
  protected void finalize() throws Throwable {
    shutdownAgentPublisherCommandsPool(true);
    shutdownDefaultForkJoinPool(true);

    super.finalize();
  }
}
