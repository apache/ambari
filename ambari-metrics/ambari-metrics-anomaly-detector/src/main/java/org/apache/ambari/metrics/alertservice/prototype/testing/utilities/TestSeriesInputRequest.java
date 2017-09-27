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
package org.apache.ambari.metrics.alertservice.prototype.testing.utilities;

import org.apache.htrace.fasterxml.jackson.core.JsonProcessingException;
import org.apache.htrace.fasterxml.jackson.databind.ObjectMapper;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.Map;

@XmlRootElement
public class TestSeriesInputRequest {

  private String seriesName;
  private String seriesType;
  private Map<String, String> configs;

  public TestSeriesInputRequest() {
  }

  public TestSeriesInputRequest(String seriesName, String seriesType, Map<String, String> configs) {
    this.seriesName = seriesName;
    this.seriesType = seriesType;
    this.configs = configs;
  }

  public String getSeriesName() {
    return seriesName;
  }

  public void setSeriesName(String seriesName) {
    this.seriesName = seriesName;
  }

  public String getSeriesType() {
    return seriesType;
  }

  public void setSeriesType(String seriesType) {
    this.seriesType = seriesType;
  }

  public Map<String, String> getConfigs() {
    return configs;
  }

  public void setConfigs(Map<String, String> configs) {
    this.configs = configs;
  }

  @Override
  public boolean equals(Object o) {
    TestSeriesInputRequest anotherInput = (TestSeriesInputRequest)o;
    return anotherInput.getSeriesName().equals(this.getSeriesName());
  }

  @Override
  public int hashCode() {
    return seriesName.hashCode();
  }

  public static void main(String[] args) {

    ObjectMapper objectMapper = new ObjectMapper();
    TestSeriesInputRequest testSeriesInputRequest = new TestSeriesInputRequest("test", "ema", Collections.singletonMap("key","value"));
    try {
      System.out.print(objectMapper.writeValueAsString(testSeriesInputRequest));
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }
}
