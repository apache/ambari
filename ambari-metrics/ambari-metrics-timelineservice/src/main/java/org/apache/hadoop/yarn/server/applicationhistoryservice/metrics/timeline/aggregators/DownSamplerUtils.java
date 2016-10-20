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

package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DownSampler utility class. Responsible for fetching downsampler configs from Metrics config, and determine if
 * any downsamplers are configured.
 */

public class DownSamplerUtils {

  public static final String downSamplerConfigPrefix = "timeline.metrics.downsampler.";
  public static final String downSamplerMetricPatternsConfig = "metric.patterns";
  public static final String topNDownSampler = "topn";
  private static final Log LOG = LogFactory.getLog(DownSamplerUtils.class);



  /**
   * Get the list of metrics that are requested to be downsampled.
   * @param configuration
   * @return List of metric patterns/names that are to be downsampled.
   */
  public static List<String> getDownsampleMetricPatterns(Configuration configuration) {
    Map<String, String> conf = configuration.getValByRegex(downSamplerConfigPrefix + "*");
    List<String> metricPatterns = new ArrayList<>();
    Set<String> keys = conf.keySet();
    for (String key : keys) {
      if (key.endsWith(downSamplerMetricPatternsConfig)) {
        String patternString = conf.get(key);
        String[] patterns = StringUtils.split(patternString, ",");
        for (String pattern : patterns) {
          if (StringUtils.isNotEmpty(pattern)) {
            String trimmedPattern = pattern.trim();
            metricPatterns.add(trimmedPattern);
          }
        }
      }
    }
    return metricPatterns;
  }

  /**
   * Get the list of downsamplers that are configured in ams-site
   * Sample config
   <name>timeline.metrics.downsampler.topn.metric.patterns</name>
   <value>dfs.NNTopUserOpCounts.windowMs=60000.op%,dfs.NNTopUserOpCounts.windowMs=300000.op%</value>

   <name>timeline.metrics.downsampler.topn.value</name>
   <value>10</value>

   <name>timeline.metrics.downsampler.topn.function</name>
   <value>max</value>
   * @param configuration
   * @return
   */
  public static List<CustomDownSampler> getDownSamplers(Configuration configuration) {

    Map<String,String> conf = configuration.getValByRegex(downSamplerConfigPrefix + "*");
    List<CustomDownSampler> downSamplers = new ArrayList<>();
    Set<String> keys = conf.keySet();

    try {
      for (String key : keys) {
        if (key.startsWith(downSamplerConfigPrefix) && key.endsWith(downSamplerMetricPatternsConfig)) {
          String type = key.split("\\.")[3];
          CustomDownSampler downSampler = getDownSamplerByType(type, conf);
          if (downSampler != null) {
            downSamplers.add(downSampler);
          }
        }
      }
    } catch (Exception e) {
      LOG.warn("Exception caught while parsing downsampler configs from ams-site : " + e.getMessage());
    }
    return downSamplers;
  }

  public static CustomDownSampler getDownSamplerByType(String type, Map<String, String> conf) {
    if (type == null) {
      return null;
    }

    if (StringUtils.isNotEmpty(type) && type.equalsIgnoreCase(topNDownSampler)) {
      return TopNDownSampler.fromConfig(conf);
    }

    LOG.warn("Unknown downsampler requested : " + type);
    return null;
  }
}
