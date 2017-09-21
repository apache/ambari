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
package org.apache.ambari.metrics.alertservice.prototype.methods.ema;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ambari.metrics.alertservice.prototype.methods.MetricAnomaly;
import org.apache.ambari.metrics.alertservice.prototype.methods.AnomalyDetectionTechnique;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.spark.SparkContext;
import org.apache.spark.mllib.util.Saveable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlRootElement
public class EmaTechnique extends AnomalyDetectionTechnique implements Serializable, Saveable {

  @XmlElement(name = "trackedEmas")
  private Map<String, EmaModel> trackedEmas;
  private static final Log LOG = LogFactory.getLog(EmaTechnique.class);

  private double startingWeight = 0.5;
  private double startTimesSdev = 3.0;
  private String methodType = "ema";

  public EmaTechnique(double startingWeight, double startTimesSdev) {
    trackedEmas = new HashMap<>();
    this.startingWeight = startingWeight;
    this.startTimesSdev = startTimesSdev;
    LOG.info("New EmaTechnique......");
  }

  public List<MetricAnomaly> test(TimelineMetric metric) {
    String metricName = metric.getMetricName();
    String appId = metric.getAppId();
    String hostname = metric.getHostName();
    String key = metricName + "_" + appId + "_" + hostname;

    EmaModel emaModel = trackedEmas.get(key);
    if (emaModel == null) {
      LOG.info("EmaModel not present for " + key);
      LOG.info("Number of tracked Emas : " + trackedEmas.size());
      emaModel  = new EmaModel(metricName, hostname, appId, startingWeight, startTimesSdev);
      trackedEmas.put(key, emaModel);
    } else {
      LOG.info("EmaModel already present for " + key);
    }

    List<MetricAnomaly> anomalies = new ArrayList<>();

    for (Long timestamp : metric.getMetricValues().keySet()) {
      double metricValue = metric.getMetricValues().get(timestamp);
      double anomalyScore = emaModel.testAndUpdate(metricValue);
      if (anomalyScore > 0.0) {
        LOG.info("Found anomaly for : " + key);
        MetricAnomaly metricAnomaly = new MetricAnomaly(key, timestamp, metricValue, methodType, anomalyScore);
        anomalies.add(metricAnomaly);
      } else {
        LOG.info("Discarding non-anomaly for : " + key);
      }
    }
    return anomalies;
  }

  public boolean updateModel(TimelineMetric timelineMetric, boolean increaseSensitivity, double percent) {
    String metricName = timelineMetric.getMetricName();
    String appId = timelineMetric.getAppId();
    String hostname = timelineMetric.getHostName();
    String key = metricName + "_" + appId + "_" + hostname;


    EmaModel emaModel = trackedEmas.get(key);

    if (emaModel == null) {
      LOG.warn("EMA Model for " + key + " not found");
      return false;
    }
    emaModel.updateModel(increaseSensitivity, percent);

    return true;
  }

  @Override
  public void save(SparkContext sc, String path) {
    Gson gson = new Gson();
    try {
      String json = gson.toJson(this);
      try (Writer writer = new BufferedWriter(new OutputStreamWriter(
        new FileOutputStream(path), "utf-8"))) {
        writer.write(json);
      }
    } catch (IOException e) {
      LOG.error(e);
    }
  }

  @Override
  public String formatVersion() {
    return "1.0";
  }

  public Map<String, EmaModel> getTrackedEmas() {
    return trackedEmas;
  }

  public double getStartingWeight() {
    return startingWeight;
  }

  public double getStartTimesSdev() {
    return startTimesSdev;
  }

}

