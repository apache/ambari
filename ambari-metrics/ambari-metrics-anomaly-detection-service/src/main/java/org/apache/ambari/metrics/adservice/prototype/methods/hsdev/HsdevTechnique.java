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
package org.apache.ambari.metrics.adservice.prototype.methods.hsdev;

import org.apache.ambari.metrics.adservice.prototype.common.DataSeries;
import org.apache.ambari.metrics.adservice.prototype.methods.MetricAnomaly;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import static org.apache.ambari.metrics.adservice.prototype.common.StatisticUtils.median;
import static org.apache.ambari.metrics.adservice.prototype.common.StatisticUtils.sdev;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class HsdevTechnique implements Serializable {

  private Map<String, Double> hsdevMap;
  private String methodType = "hsdev";
  private static final Log LOG = LogFactory.getLog(HsdevTechnique.class);

  public HsdevTechnique() {
    hsdevMap = new HashMap<>();
  }

  public MetricAnomaly runHsdevTest(String key, DataSeries trainData, DataSeries testData) {
    int testLength = testData.values.length;
    int trainLength = trainData.values.length;

    if (trainLength < testLength) {
      LOG.info("Not enough train data.");
      return null;
    }

    if (!hsdevMap.containsKey(key)) {
      hsdevMap.put(key, 3.0);
    }

    double n = hsdevMap.get(key);

    double historicSd = sdev(trainData.values, false);
    double historicMedian = median(trainData.values);
    double currentMedian = median(testData.values);


    if (historicSd > 0) {
      double diff = Math.abs(currentMedian - historicMedian);
      LOG.info("Found anomaly for metric : " + key + " in the period ending " + new Date((long)testData.ts[testLength - 1]));
      LOG.info("Current median = " + currentMedian + ", Historic Median = " + historicMedian + ", HistoricSd = " + historicSd);

      if (diff > n * historicSd) {
        double zScore = diff / historicSd;
        LOG.info("Z Score of current series : " + zScore);
        return new MetricAnomaly(key,
          (long) testData.ts[testLength - 1],
          testData.values[testLength - 1],
          methodType,
          zScore);
      }
    }

    return null;
  }

}
