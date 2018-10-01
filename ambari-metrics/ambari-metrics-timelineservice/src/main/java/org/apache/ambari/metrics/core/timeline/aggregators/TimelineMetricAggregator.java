package org.apache.ambari.metrics.core.timeline.aggregators;

import org.apache.ambari.metrics.core.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public interface TimelineMetricAggregator extends Runnable {
  /**
   * Aggregate metric data within the time bounds.
   *
   * @param startTime start time millis
   * @param endTime   end time millis
   * @return success
   */
  boolean doWork(long startTime, long endTime);

  /**
   * Is aggregator is disabled by configuration.
   *
   * @return true/false
   */
  boolean isDisabled();

  /**
   * Return aggregator Interval
   *
   * @return Interval in Millis
   */
  Long getSleepIntervalMillis();

  /**
   * Get aggregator name
   * @return @AGGREGATOR_NAME
   */
  AGGREGATOR_NAME getName();

  /**
   * Known aggregator types
   */
  enum AGGREGATOR_TYPE {
    CLUSTER,
    HOST
  }
}
