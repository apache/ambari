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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.base.Function;

public class AsyncCallableServiceTest extends EasyMockSupport {

  private static final long TIMEOUT = 1000; // default timeout
  private static final long RETRY_DELAY = 50; // default delay between tries

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.STRICT)
  private Callable<Boolean> taskMock;

  @Mock
  private ScheduledExecutorService executorServiceMock;

  @Mock
  private ScheduledFuture<Boolean> futureMock;

  @Mock
  private Function<Throwable, ?> onErrorMock;

  private AsyncCallableService<Boolean> asyncCallableService;

  @Test
  public void testCallableServiceShouldCancelTaskWhenTimeoutExceeded() throws Exception {
    // GIVEN
    long timeout = -1; // guaranteed timeout
    TimeoutException timeoutException = new TimeoutException("Testing the timeout exceeded case");
    expect(futureMock.get(timeout, TimeUnit.MILLISECONDS)).andThrow(timeoutException);
    expect(futureMock.isDone()).andReturn(Boolean.FALSE);
    expect(futureMock.cancel(true)).andReturn(Boolean.TRUE);
    expect(executorServiceMock.submit(taskMock)).andReturn(futureMock);
    expect(onErrorMock.apply(timeoutException)).andReturn(null);
    replayAll();

    asyncCallableService = new AsyncCallableService<>(taskMock, timeout, RETRY_DELAY, "test", executorServiceMock, onErrorMock);

    // WHEN
    Boolean serviceResult = asyncCallableService.call();

    // THEN
    verifyAll();
    Assert.assertNull("No result expected in case of timeout", serviceResult);
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
    expect(onErrorMock.apply(anyObject(TimeoutException.class))).andReturn(null);
    replayAll();

    asyncCallableService = new AsyncCallableService<>(hangingTask, TIMEOUT, RETRY_DELAY,  "test", onErrorMock);

    // WHEN
    Boolean serviceResult = asyncCallableService.call();

    // THEN
    verifyAll();
    Assert.assertNull("No result expected from hanging task", serviceResult);
  }

  @Test
  public void testCallableServiceShouldExitWhenTaskCompleted() throws Exception {
    // GIVEN
    expect(taskMock.call()).andReturn(Boolean.TRUE);
    expect(onErrorMock.apply(anyObject(TimeoutException.class))).andThrow(new AssertionError("No error expected")).anyTimes();
    replayAll();
    asyncCallableService = new AsyncCallableService<>(taskMock, TIMEOUT, RETRY_DELAY,  "test", onErrorMock);

    // WHEN
    Boolean serviceResult = asyncCallableService.call();

    // THEN
    verifyAll();
    Assert.assertEquals(Boolean.TRUE, serviceResult);
  }

  @Test
  public void testCallableServiceShouldRetryTaskExecutionTillTimeoutExceededWhenTaskThrowsException() throws Exception {
    // GIVEN
    expect(taskMock.call()).andThrow(new IllegalStateException("****************** TESTING ****************")).times(2, 3);
    expect(onErrorMock.apply(anyObject(IllegalStateException.class))).andReturn(null).once();
    replayAll();
    asyncCallableService = new AsyncCallableService<>(taskMock, TIMEOUT, RETRY_DELAY,  "test", onErrorMock);

    // WHEN
    Boolean serviceResult = asyncCallableService.call();

    // THEN
    verifyAll();
    Assert.assertNull("No result expected from throwing task", serviceResult);
  }


  @Test
  public void testShouldAsyncCallableServiceRetryExecutionWhenTaskThrowsException() throws Exception {
    // GIVEN
    // the task throws exception
    Callable<Boolean> throwingTask = new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        throw new IllegalStateException("****************** TESTING ****************");
      }
    };
    expect(onErrorMock.apply(anyObject(IllegalStateException.class))).andReturn(null).once();
    replayAll();

    asyncCallableService = new AsyncCallableService<>(throwingTask, TIMEOUT, RETRY_DELAY,  "test", onErrorMock);

    // WHEN
    Boolean serviceResult = asyncCallableService.call();

    // THEN
    verifyAll();
    Assert.assertNull("No result expected from throwing task", serviceResult);
  }
}
