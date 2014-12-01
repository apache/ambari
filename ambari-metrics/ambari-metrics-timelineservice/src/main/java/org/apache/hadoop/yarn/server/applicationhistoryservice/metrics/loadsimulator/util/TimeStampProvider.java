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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics
  .loadsimulator.util;

/**
 */
public class TimeStampProvider {
  private int timeStep;
  private long currentTime;
  private int sendInterval;

  public TimeStampProvider(long startTime, int timeStep, int sendInterval) {
    this.timeStep = timeStep;
    this.currentTime = startTime - timeStep;
    this.sendInterval = sendInterval;
  }

  public long next() {
    return currentTime += timeStep;
  }

  public long[] timestampsForNextInterval() {
    return timestampsForInterval(sendInterval);
  }

  private long[] timestampsForInterval(int sendInterval) {
    int steps = sendInterval / timeStep;
    long[] timestamps = new long[steps];

    for (int i = 0; i < timestamps.length; i++) {
      timestamps[i] = next();
    }

    return timestamps;
  }
}
