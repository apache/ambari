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

package org.apache.ambari.metrics.alertservice.prototype.methods.kstest;

import org.apache.ambari.metrics.alertservice.prototype.RFunctionInvoker;
import org.apache.ambari.metrics.alertservice.prototype.common.DataSeries;
import org.apache.ambari.metrics.alertservice.prototype.common.ResultSet;
import org.apache.ambari.metrics.alertservice.prototype.methods.MetricAnomaly;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class KSTechnique implements Serializable {

  private String methodType = "ks";
  private Map<String, Double> pValueMap;
  private static final Log LOG = LogFactory.getLog(KSTechnique.class);

  public KSTechnique() {
    pValueMap = new HashMap();
  }

  public MetricAnomaly runKsTest(String key, DataSeries trainData, DataSeries testData) {

    int testLength = testData.values.length;
    int trainLength = trainData.values.length;

    if (trainLength < testLength) {
      LOG.info("Not enough train data.");
      return null;
    }

    if (!pValueMap.containsKey(key)) {
      pValueMap.put(key, 0.05);
    }
    double pValue = pValueMap.get(key);

    ResultSet result = RFunctionInvoker.ksTest(trainData, testData, Collections.singletonMap("ks.p_value", String.valueOf(pValue)));
    if (result == null) {
      LOG.error("Resultset is null when invoking KS R function...");
      return null;
    }

    if (result.resultset.size() > 0) {

      LOG.info("Is size 1 ? result size = " + result.resultset.get(0).length);
      LOG.info("p_value = " + result.resultset.get(3)[0]);
      double dValue = result.resultset.get(2)[0];

      return new MetricAnomaly(key,
        (long) testData.ts[testLength - 1],
        testData.values[testLength - 1],
        methodType,
        dValue);
    }

    return null;
  }

  public void updateModel(String metricKey, boolean increaseSensitivity, double percent) {

    LOG.info("Updating KS model for " + metricKey + " with increaseSensitivity = " + increaseSensitivity + ", percent = " + percent);

    if (!pValueMap.containsKey(metricKey)) {
      LOG.error("Unknown metric key : " + metricKey);
      LOG.info("pValueMap :" + pValueMap.toString());
      return;
    }

    double delta = percent / 100;
    if (!increaseSensitivity) {
      delta = delta * -1;
    }

    double pValue = pValueMap.get(metricKey);
    double newPValue = Math.min(1.0, pValue + delta * pValue);
    pValueMap.put(metricKey, newPValue);
    LOG.info("New pValue = " + newPValue);
  }

}
