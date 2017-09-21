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

package org.apache.ambari.metrics.alertservice.prototype;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Map;

@XmlRootElement
public class MetricAnomalyDetectorTestInput {

  public MetricAnomalyDetectorTestInput() {
  }

  //Train data
  private String trainDataName;
  private String trainDataType;
  private Map<String, String> trainDataConfigs;
  private int trainDataSize;

  //Test data
  private String testDataName;
  private String testDataType;
  private Map<String, String> testDataConfigs;
  private int testDataSize;

  //Algorithm data
  private List<String> methods;
  private Map<String, String> methodConfigs;

  public String getTrainDataName() {
    return trainDataName;
  }

  public void setTrainDataName(String trainDataName) {
    this.trainDataName = trainDataName;
  }

  public String getTrainDataType() {
    return trainDataType;
  }

  public void setTrainDataType(String trainDataType) {
    this.trainDataType = trainDataType;
  }

  public Map<String, String> getTrainDataConfigs() {
    return trainDataConfigs;
  }

  public void setTrainDataConfigs(Map<String, String> trainDataConfigs) {
    this.trainDataConfigs = trainDataConfigs;
  }

  public String getTestDataName() {
    return testDataName;
  }

  public void setTestDataName(String testDataName) {
    this.testDataName = testDataName;
  }

  public String getTestDataType() {
    return testDataType;
  }

  public void setTestDataType(String testDataType) {
    this.testDataType = testDataType;
  }

  public Map<String, String> getTestDataConfigs() {
    return testDataConfigs;
  }

  public void setTestDataConfigs(Map<String, String> testDataConfigs) {
    this.testDataConfigs = testDataConfigs;
  }

  public Map<String, String> getMethodConfigs() {
    return methodConfigs;
  }

  public void setMethodConfigs(Map<String, String> methodConfigs) {
    this.methodConfigs = methodConfigs;
  }

  public int getTrainDataSize() {
    return trainDataSize;
  }

  public void setTrainDataSize(int trainDataSize) {
    this.trainDataSize = trainDataSize;
  }

  public int getTestDataSize() {
    return testDataSize;
  }

  public void setTestDataSize(int testDataSize) {
    this.testDataSize = testDataSize;
  }

  public List<String> getMethods() {
    return methods;
  }

  public void setMethods(List<String> methods) {
    this.methods = methods;
  }
}
