package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.sink;

import java.util.Collection;

import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public interface ExternalMetricsSink {
  /**
   * How many seconds to wait on sink before dropping metrics.
   * Note: Care should be taken that this timeout does not bottleneck the
   * sink thread.
   */
  int getSinkTimeOutSeconds();

  /**
   * How frequently to flush data to external system.
   * Default would be between 60 - 120 seconds, coherent with default sink
   * interval of AMS.
   */
  int getFlushSeconds();

  /**
   * Raw data stream to process / store on external system.
   * The data will be held in an in-memory cache and flushed at flush seconds
   * or when the cache size limit is exceeded we will flush the cache and
   * drop data if write fails.
   *
   * @param metrics {@link Collection<TimelineMetrics>}
   */
  void sinkMetricData(Collection<TimelineMetrics> metrics);
}
