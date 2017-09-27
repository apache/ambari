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
package org.apache.ambari.metrics.adservice.prototype.methods.ema;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

import static org.apache.ambari.metrics.adservice.prototype.methods.ema.EmaTechnique.suppressAnomaliesTheshold;

@XmlRootElement
public class EmaModel implements Serializable {

  private String metricName;
  private String hostname;
  private String appId;
  private double ema;
  private double ems;
  private double weight;
  private double timessdev;

  private int ctr = 0;

  private static final Log LOG = LogFactory.getLog(EmaModel.class);

  public EmaModel(String name, String hostname, String appId, double weight, double timessdev) {
    this.metricName = name;
    this.hostname = hostname;
    this.appId = appId;
    this.weight = weight;
    this.timessdev = timessdev;
    this.ema = 0.0;
    this.ems = 0.0;
  }

  public String getMetricName() {
    return metricName;
  }

  public String getHostname() {
    return hostname;
  }

  public String getAppId() {
    return appId;
  }

  public double testAndUpdate(double metricValue) {

    double anomalyScore = 0.0;
    LOG.info("Before Update ->" + metricName + ":" + appId + ":" + hostname + " - " + "ema = " + ema + ", ems = " + ems + ", timessdev = " + timessdev);
    update(metricValue);
    if (ctr > suppressAnomaliesTheshold) {
      anomalyScore = test(metricValue);
      if (anomalyScore > 0.0) {
        LOG.info("Anomaly ->" + metricName + ":" + appId + ":" + hostname + " - " + "ema = " + ema + ", ems = " + ems +
          ", timessdev = " + timessdev + ", metricValue = " + metricValue);
      } else {
        LOG.info("Not an Anomaly ->" + metricName + ":" + appId + ":" + hostname + " - " + "ema = " + ema + ", ems = " + ems +
          ", timessdev = " + timessdev + ", metricValue = " + metricValue);
      }
    } else {
      ctr++;
      if (ctr > suppressAnomaliesTheshold) {
        LOG.info("Ema Model for " + metricName + ":" + appId + ":" + hostname + " is ready for testing data.");
      }
    }
    return anomalyScore;
  }

  public void update(double metricValue) {
    ema = weight * ema + (1 - weight) * metricValue;
    ems = Math.sqrt(weight * Math.pow(ems, 2.0) + (1 - weight) * Math.pow(metricValue - ema, 2.0));
    LOG.debug("In update : ema = " + ema + ", ems = " + ems);
  }

  public double test(double metricValue) {
    LOG.debug("In test : ema = " + ema + ", ems = " + ems);
    double diff = Math.abs(ema - metricValue) - (timessdev * ems);
    LOG.debug("diff = " + diff);
    if (diff > 0) {
      return Math.abs((metricValue - ema) / ems); //Z score
    } else {
      return 0.0;
    }
  }

  public void updateModel(boolean increaseSensitivity, double percent) {
    LOG.info("Updating model for " + metricName + " with increaseSensitivity = " + increaseSensitivity + ", percent = " + percent);
    double delta = percent / 100;
    if (increaseSensitivity) {
      delta = delta * -1;
    }
    this.timessdev = timessdev + delta * timessdev;
    //this.weight = Math.min(1.0, weight + delta * weight);
    LOG.info("New model parameters " + metricName + " : timessdev = " + timessdev + ", weight = " + weight);
  }

  public double getWeight() {
    return weight;
  }

  public void setWeight(double weight) {
    this.weight = weight;
  }

  public double getTimessdev() {
    return timessdev;
  }

  public void setTimessdev(double timessdev) {
    this.timessdev = timessdev;
  }
}
