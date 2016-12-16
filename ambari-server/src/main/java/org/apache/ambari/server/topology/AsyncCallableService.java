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

package org.apache.ambari.server.topology;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Callable service implementation for executing tasks asynchronously.
 * The service repeatedly tries to execute the provided task till it successfully completes, or the provided timeout
 * interval is exceeded.
 *
 * @param <T> the type returned by the task to be executed
 */
public class AsyncCallableService<T> implements Callable<T> {

  private static final Logger LOG = LoggerFactory.getLogger(AsyncCallableService.class);

  // task execution is done on a separate thread provided by this executor
  private final ScheduledExecutorService executorService;

  // the task to be executed
  private final Callable<T> task;

  // the total time the allowed for the task to be executed (retries will be happen within this timeframe in
  // milliseconds)
  private final long timeout;

  // the delay between two consecutive execution trials in milliseconds
  private final long delay;

  private T serviceResult;

  private final Set<Exception> errors = new HashSet<>();

  public AsyncCallableService(Callable<T> task, long timeout, long delay,
                              ScheduledExecutorService executorService) {
    this.task = task;
    this.executorService = executorService;
    this.timeout = timeout;
    this.delay = delay;
  }

  @Override
  public T call() {

    long startTimeInMillis = Calendar.getInstance().getTimeInMillis();
    LOG.info("Task execution started at: {}", startTimeInMillis);

    // task execution started on a new thread
    Future<T> future = executorService.submit(task);

    while (!taskCompleted(future)) {
      if (!timeoutExceeded(startTimeInMillis)) {
        LOG.debug("Retrying task execution in [ {} ] milliseconds.", delay);
        future = executorService.schedule(task, delay, TimeUnit.MILLISECONDS);
      } else {
        LOG.debug("Timout exceeded, cancelling task ... ");
        // making sure the task gets cancelled!
        if (!future.isDone()) {
          boolean cancelled = future.cancel(true);
          LOG.debug("Task cancelled: {}", cancelled);
        } else {
          LOG.debug("Task already done.");
        }
        LOG.info("Timeout exceeded, task execution won't be retried!");
        // exit the "retry" loop!
        break;
      }
    }

    LOG.info("Exiting Async task execution with the result: [ {} ]", serviceResult);
    return serviceResult;
  }

  private boolean taskCompleted(Future<T> future) {
    boolean completed = false;
    try {
      LOG.debug("Retrieving task execution result ...");
      // should receive task execution result within the configured timeout interval
      // exceptions thrown from the task are propagated here
      T taskResult = future.get(timeout, TimeUnit.MILLISECONDS);

      // task failures are expected to be reportesd as exceptions
      LOG.debug("Task successfully executed: {}", taskResult);
      setServiceResult(taskResult);
      errors.clear();
      completed = true;
    } catch (Exception e) {
      // Future.isDone always true here!
      LOG.info("Exception during task execution: ", e);
      errors.add(e);
    }
    return completed;
  }

  private boolean timeoutExceeded(long startTimeInMillis) {
    return timeout < Calendar.getInstance().getTimeInMillis() - startTimeInMillis;
  }

  private void setServiceResult(T serviceResult) {
    this.serviceResult = serviceResult;
  }

  public Set<Exception> getErrors() {
    return errors;
  }

}
