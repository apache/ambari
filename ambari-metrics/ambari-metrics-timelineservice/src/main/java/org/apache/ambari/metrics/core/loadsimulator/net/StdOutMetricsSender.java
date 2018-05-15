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
package org.apache.ambari.metrics.core.loadsimulator.net;

import java.io.PrintStream;

/**
 * StdOutMetricsSender dumps metrics to defined PrintStream out. It is useful for testing.
 */
public class StdOutMetricsSender implements MetricsSender {
  public final PrintStream out;
  private String metricsHostName;

  /**
   * Creates new StdOutMetricsSender with specified hostname (only used in messages) and sends output to System.out
   *
   * @param metricsHostName a name used in printed messages
   */
  public StdOutMetricsSender(String metricsHostName) {
    this(metricsHostName, System.out);
  }

  /**
   * Creates new StdOutMetricsSender with specified hostname (only used in messages) and PrintStream which is used as
   * an output.
   *
   * @param metricsHostName a name used in printed messages
   * @param out             PrintStream that the Sender will write to, can be System.out
   */
  public StdOutMetricsSender(String metricsHostName, PrintStream out) {
    this.metricsHostName = metricsHostName;
    this.out = out;
  }

  @Override
  public String pushMetrics(String payload) {
    out.println("Sending to " + metricsHostName + ": " + payload);

    return "OK";
  }
}
