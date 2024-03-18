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
package org.apache.ambari.metrics.core.loadsimulator.data;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.metrics.core.loadsimulator.util.TimeStampProvider;
import org.apache.ambari.metrics.core.loadsimulator.util.RandomMetricsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MetricsGeneratorConfigurer is a factory that reads metrics definition from a file,
 * and returns an Single HostMetricsGenerator. Check createMetricsForHost method
 * for details.
 */
public class MetricsGeneratorConfigurer {

  private final static Logger LOG = LoggerFactory.getLogger
    (MetricsGeneratorConfigurer.class);

  /**
   * Creates HostMetricsGenerator configured with metric names loaded from file.
   *
   * @param id         ApplicationInstance descriptor, will be used to create
   *                   HostMetricsGenerator, cannot be null
   * @param timeStamps configured TimeStampProvider that can provide next
   *                   timestamp, cannot be null
   * @return HostMetricsGenerator with given ApplicationInstance id and configured
   * mapping of
   * metric names to data providers
   */
  public static HostMetricsGenerator createMetricsForHost(
    ApplicationInstance id,
    TimeStampProvider timeStamps) {
    return new HostMetricsGenerator(id, timeStamps, readMetrics(id.getAppId()));
  }

  private static Map<String, RandomMetricsProvider> readMetrics(AppID type) {
    InputStream input = null;
    Map<String, RandomMetricsProvider> metrics =
      new HashMap<String, RandomMetricsProvider>();
    String fileName = "metrics_def/" + type.toString() + ".dat";

    try {
      LOG.info("Loading " + fileName);

      input = MetricsGeneratorConfigurer.class.getClassLoader()
        .getResourceAsStream(fileName);

      BufferedReader reader = new BufferedReader(new InputStreamReader(input));

      String line;
      while ((line = reader.readLine()) != null) {
        metrics.put(line.trim(), new RandomMetricsProvider(100, 200));
      }

    } catch (IOException e) {
      LOG.error("Cannot read file " + fileName + " for appID " + type.toString(), e);
    } finally {
      if (input != null) {
        try {
          input.close();
        } catch (IOException ex) {
          // intentionally left blank, here we cannot do anything
        }
      }
    }

    return metrics;
  }
}
