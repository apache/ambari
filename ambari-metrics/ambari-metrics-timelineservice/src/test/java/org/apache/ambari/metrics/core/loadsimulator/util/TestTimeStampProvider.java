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
package org.apache.ambari.metrics.core.loadsimulator.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class TestTimeStampProvider {

  @Test
  public void testReturnSingle() {
    long startTime = 1411663170112L;
    int timeStep = 5000;
    TimeStampProvider tm = new TimeStampProvider(startTime, timeStep, 0);

    long tStamp = tm.next();

    assertEquals("First generated timestamp should match starttime", startTime, tStamp);
  }

  @Test
  public void testReturnTstampsForSendInterval() throws Exception {
    long startTime = 0;
    int collectInterval = 5;
    int sendInterval = 30;
    TimeStampProvider tsp = new TimeStampProvider(startTime, collectInterval, sendInterval);

    long[] timestamps = tsp.timestampsForNextInterval();

    assertThat(timestamps)
      .hasSize(6)
      .containsOnly(0, 5, 10, 15, 20, 25);
  }
}