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
package org.apache.ambari.server.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import junit.framework.Assert;

/**
 * Tests parallel loops
 */
public class TestParallel {

  /**
   * Tests {@link org.apache.ambari.server.utils.Parallel} forLoop base cases.
   * @throws Exception
   */
  @Test
  public void testParallelForLoopBaseCases() throws Exception {

    ParallelLoopResult<Integer> nullLoopResult = Parallel.forLoop(
        null,
        new LoopBody<Integer, Integer>() {
          @Override
          public Integer run(Integer integer) {
            return integer;
          }
        });
    Assert.assertTrue(nullLoopResult.getIsCompleted());
    Assert.assertTrue(nullLoopResult.getResult().isEmpty());

    ParallelLoopResult<Integer> emptyLoopResult = Parallel.forLoop(
      new ArrayList<>(),
        new LoopBody<Integer, Integer>() {
          @Override
          public Integer run(Integer integer) {
            return integer;
          }
        });
    Assert.assertTrue(emptyLoopResult.getIsCompleted());
    Assert.assertTrue(emptyLoopResult.getResult().isEmpty());

    ParallelLoopResult<Integer> singleElementLoopResult = Parallel.forLoop(
        Collections.singletonList(7),
        new LoopBody<Integer, Integer>() {
          @Override
          public Integer run(Integer integer) {
            return integer;
          }
        });
    Assert.assertTrue(singleElementLoopResult.getIsCompleted());
    List<Integer> singleElementList = singleElementLoopResult.getResult();
    Assert.assertTrue(singleElementLoopResult.getIsCompleted());
    Assert.assertFalse(singleElementList.isEmpty());
    Assert.assertEquals(1, singleElementList.size());
    Assert.assertNotNull(singleElementList.get(0));
  }

  /**
   * Tests Parallel.forLoop
   * @throws Exception
   */
  @Test
  public void testParallelForLoop() throws Exception {
    final List<Integer> input = new LinkedList<>();
    for(int i = 0; i < 10; i++) {
      input.add(i);
    }

    ParallelLoopResult<Integer> loopResult = Parallel.forLoop(input, new LoopBody<Integer, Integer>() {
      @Override
      public Integer run(Integer in1) {
        return in1 * in1;
      }
    });
    Assert.assertTrue(loopResult.getIsCompleted());
    Assert.assertNotNull(loopResult.getResult());

    List<Integer> output = loopResult.getResult();
    Assert.assertEquals(input.size(), output.size());
    for(int i = 0; i < input.size(); i++) {
      Assert.assertEquals( i * i,  (int)output.get(i));
    }
  }

  /**
   * Tests nested {@link org.apache.ambari.server.utils.Parallel} forLoop
   * @throws Exception
   */
  @Test
  public void testNestedParallelForLoop() throws Exception {
    final List<Integer> input = new LinkedList<>();
    for(int i = 0; i < 10; i++) {
      input.add(i);
    }
    final ParallelLoopResult<Integer>[] innerLoopResults =  new ParallelLoopResult[input.size()];
    ParallelLoopResult<Integer> loopResult = Parallel.forLoop(input, new LoopBody<Integer, Integer>() {
      @Override
      public Integer run(final Integer in1) {
        int sq = in1 * in1;
        ParallelLoopResult<Integer> innerLoopResult = Parallel.forLoop(input, new LoopBody<Integer, Integer>() {
          @Override
          public Integer run(Integer in2) {
            return in1 * in2;
          }
        });
        innerLoopResults[in1] = innerLoopResult;
        return in1 * in1;
      }
    });
    Assert.assertNotNull(loopResult);
    Assert.assertTrue(loopResult.getIsCompleted());
    List<Integer> output = loopResult.getResult();
    Assert.assertNotNull(output);
    Assert.assertEquals(input.size(), output.size());

    for(int i = 0; i < input.size(); i++) {
      Assert.assertEquals(i * i, (int) output.get(i));
      ParallelLoopResult<Integer> innerLoopResult = innerLoopResults[i];
      Assert.assertNotNull(innerLoopResult);
      Assert.assertTrue(innerLoopResult.getIsCompleted());
      List<Integer> innerOutput = innerLoopResult.getResult();
      Assert.assertNotNull(innerOutput);
      Assert.assertEquals(input.size(), innerOutput.size());

      for(int j = 0; j < input.size(); j++) {
        Assert.assertEquals(i*j, (int) innerOutput.get(j));
      }
    }
  }

  /**
   * Tests {@link org.apache.ambari.server.utils.Parallel} forLoop iteration failures
   * @throws Exception
   */
  @Test
  public void testParallelForLoopIterationFailures() throws Exception {
    final List<Integer> input = new LinkedList<>();
    for(int i = 0; i < 10; i++) {
      input.add(i);
    }
    final List<Integer> failForList = Arrays.asList(new Integer[] { 2, 5, 7});
    ParallelLoopResult<Integer> loopResult = Parallel.forLoop(input, new LoopBody<Integer, Integer>() {
      @Override
      public Integer run(Integer in1) {
        if(failForList.contains(in1)) {
          // Return null
          return null;
        }
        return in1 * in1;
      }
    });
    Assert.assertFalse(loopResult.getIsCompleted());
    Assert.assertNotNull(loopResult.getResult());
    List<Integer> output = loopResult.getResult();
    Assert.assertEquals(input.size(), output.size());

    for(int i = 0; i < input.size(); i++) {
      if(failForList.contains(i)) {
        Assert.assertNull(output.get(i));
        output.set(i, i * i);
      } else {
        Assert.assertEquals(i * i, (int) output.get(i));
      }
    }
  }

  /**
   * Tests {@link org.apache.ambari.server.utils.Parallel} forLoop iteration exceptions
   * @throws Exception
   */
  @Test
  public void testParallelForLoopIterationExceptions() throws Exception {
    final List<Integer> input = new LinkedList<>();
    for(int i = 0; i < 10; i++) {
      input.add(i);
    }
    final List<Integer> failForList = Arrays.asList(new Integer[] { 2, 5, 7});
    ParallelLoopResult<Integer> loopResult = Parallel.forLoop(input, new LoopBody<Integer, Integer>() {
      @Override
      public Integer run(Integer in1) {
        if(failForList.contains(in1)) {
          throw new RuntimeException("Ignore this exception");
        }
        return in1 * in1;
      }
    });
    Assert.assertFalse(loopResult.getIsCompleted());
    Assert.assertNotNull(loopResult.getResult());
    List<Integer> output = loopResult.getResult();
    Assert.assertEquals(input.size(), output.size());

    for(int i = 0; i < input.size(); i++) {
      if(failForList.contains(i)) {
        Assert.assertNull(output.get(i));
        output.set(i, i * i);
      } else {
        Assert.assertEquals(i * i, (int) output.get(i));
      }
    }
  }
}
