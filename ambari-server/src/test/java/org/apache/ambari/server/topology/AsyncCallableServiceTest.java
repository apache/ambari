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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.verify;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncCallableServiceTest extends EasyMockSupport {
  public static final Logger LOGGER = LoggerFactory.getLogger(AsyncCallableService.class);

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.STRICT)
  private Callable<Boolean> taskMock;

  @Mock
  private ScheduledExecutorService executorServiceMock;

  @Mock
  private ScheduledFuture<Boolean> futureMock;

  private long timeout;

  private long delay;

  private AsyncCallableService<Boolean> asyncCallableService;

  @Before
  public void setup() {
    // default timeout, overwrite it if necessary
    timeout = 1000;

    // default delay between tries
    delay = 500;
  }


  @Test
  public void testCallableServiceShouldCancelTaskWhenTimeoutExceeded() throws Exception {
    // GIVEN

    //the timeout period should be small!!!
    timeout = 1l;

    // the task to be executed never completes successfully
    expect(futureMock.get(timeout, TimeUnit.MILLISECONDS)).andThrow(new TimeoutException("Testing the timeout exceeded case"));
    expect(futureMock.isDone()).andReturn(Boolean.FALSE);

    // this is only called when a timeout occurs
    expect(futureMock.cancel(true)).andReturn(Boolean.TRUE);

    expect(executorServiceMock.submit(taskMock)).andReturn(futureMock);

    replayAll();

    asyncCallableService = new AsyncCallableService<>(taskMock, timeout, delay, executorServiceMock);

    // WHEN
    Boolean serviceResult = asyncCallableService.call();

    // THEN
    verify();
    Assert.assertNull("Service result must be null", serviceResult);
    Assert.assertFalse("The service should have errors!", asyncCallableService.getErrors().isEmpty());
  }

  @Test
  public void testCallableServiceShouldCancelTaskWhenTaskHangsAndTimeoutExceeded() throws Exception {
    // GIVEN
    //the task call hangs, it doesn't return within a reasonable period of time
    Callable<Boolean> hangingTask = new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        Thread.sleep(10000000);
        return false;
      }
    };

    asyncCallableService = new AsyncCallableService<>(hangingTask, timeout, delay, Executors.newScheduledThreadPool(2));

    // WHEN
    Boolean serviceResult = asyncCallableService.call();

    // THEN
    Assert.assertNull("Service result must be null", serviceResult);
    Assert.assertFalse("The service should have errors!", asyncCallableService.getErrors().isEmpty());
  }

  @Test
  public void testCallableServiceShouldExitWhenTaskCompleted() throws Exception {
    // GIVEN
    // the task to be executed never completes successfully
    expect(taskMock.call()).andReturn(Boolean.TRUE).times(1);

    replayAll();
    asyncCallableService = new AsyncCallableService<>(taskMock, timeout, delay, Executors.newScheduledThreadPool(2));

    // WHEN
    Boolean serviceResult = asyncCallableService.call();

    // THEN
    verify();
    Assert.assertNotNull("Service result must not be null", serviceResult);
    Assert.assertTrue(serviceResult);
  }

  @Test
  public void testCallableServiceShouldRetryTaskExecutionTillTimeoutExceededWhenTaskThrowsException() throws Exception {
    // GIVEN

    // the task to be throws exception
    expect(taskMock.call()).andThrow(new IllegalStateException("****************** TESTING ****************")).times(2, 3);
    replayAll();
    asyncCallableService = new AsyncCallableService<>(taskMock, timeout, delay, Executors.newScheduledThreadPool(2));

    // WHEN
    Boolean serviceResult = asyncCallableService.call();

    // THEN
    verify();
    // THEN
    Assert.assertNull("Service result must be null", serviceResult);

  }


  @Test
  public void testShouldAsyncCallableServiceRetryExecutionWhenTaskThrowsException() throws Exception {
    // GIVEN
    //the task call hangs, it doesn't return within a reasonable period of time
    Callable<Boolean> hangingTask = new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        throw new IllegalStateException("****************** TESTING ****************");
      }
    };

    asyncCallableService = new AsyncCallableService<>(hangingTask, timeout, delay, Executors.newScheduledThreadPool(2));

    // WHEN
    Boolean serviceResult = asyncCallableService.call();

    // THEN
    verify();
    Assert.assertNull("Service result must be null", serviceResult);
  }
}