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

package org.apache.ambari.metrics.core.timeline.aggregators;

import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.TOPN_DOWNSAMPLER_CLUSTER_METRIC_SELECT_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.TOPN_DOWNSAMPLER_HOST_METRIC_SELECT_SQL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.ambari.metrics.core.timeline.query.TopNCondition;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.TopNConfig;

public class TopNDownSampler implements CustomDownSampler {

  private TopNConfig topNConfig;
  private static final Log LOG = LogFactory.getLog(TopNDownSampler.class);
  protected String metricPatterns;

  public static TopNDownSampler fromConfig(Map<String, String> conf) {
    String metricPatterns = conf.get(DownSamplerUtils.downSamplerConfigPrefix + DownSamplerUtils.topNDownSamplerKey + "." +
      DownSamplerUtils.downSamplerMetricPatternsConfig);

    String topNString = conf.get(DownSamplerUtils.downSamplerConfigPrefix + "topn.value");
    Integer topNValue = topNString != null ? Integer.valueOf(topNString) : 10;
    String topNFunction = conf.get(DownSamplerUtils.downSamplerConfigPrefix + "topn.function");

    return new TopNDownSampler(new TopNConfig(topNValue, topNFunction, false), metricPatterns);
  }

  public TopNDownSampler(TopNConfig topNConfig, String metricPatterns) {
    this.topNConfig = topNConfig;
    this.metricPatterns = metricPatterns;
  }

  @Override
  public boolean validateConfigs() {
    if (topNConfig == null) {
      return false;
    }

    if (topNConfig.getTopN() <= 0) {
      return false;
    }

    if (StringUtils.isEmpty(metricPatterns)) {
      return false;
    }

    return true;
  }

  /**
   * Prepare downsampling SELECT statement(s) used to determine the data to be written into the Aggregate table.
   * @param startTime
   * @param endTime
   * @param tableName
   * @return
   */
  @Override
  public List<String> prepareDownSamplingStatement(Long startTime, Long endTime, String tableName) {
    List<String> stmts = new ArrayList<>();

    Function.ReadFunction readFunction = Function.ReadFunction.getFunction(topNConfig.getTopNFunction());
    Function function = new Function(readFunction, null);
    String columnSelect = TopNCondition.getColumnSelect(function);

    List<String> metricPatternList = Arrays.asList(metricPatterns.split(","));

    for (String metricPattern : metricPatternList) {
      String metricPatternClause = "'" + metricPattern + "'";
      //TODO : Need a better way to find out what kind of aggregation the current one is.
      if (tableName.contains("RECORD")) {
        stmts.add(String.format(TOPN_DOWNSAMPLER_HOST_METRIC_SELECT_SQL,
          endTime, columnSelect, columnSelect, columnSelect, tableName, metricPatternClause,
          startTime, endTime, columnSelect, topNConfig.getTopN()));
      } else {
        stmts.add(String.format(TOPN_DOWNSAMPLER_CLUSTER_METRIC_SELECT_SQL,
          endTime, columnSelect, columnSelect, columnSelect, tableName, metricPatternClause,
          startTime, endTime, columnSelect, topNConfig.getTopN()));
      }
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("DownSampling Stmt: " + stmts.toString());
    }

    return stmts;
  }

}
