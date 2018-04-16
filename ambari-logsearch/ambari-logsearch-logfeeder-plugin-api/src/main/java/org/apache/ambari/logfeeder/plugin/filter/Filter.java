/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logfeeder.plugin.filter;

import org.apache.ambari.logfeeder.plugin.common.AliasUtil;
import org.apache.ambari.logfeeder.plugin.common.ConfigItem;
import org.apache.ambari.logfeeder.plugin.common.LogFeederProperties;
import org.apache.ambari.logfeeder.plugin.common.MetricData;
import org.apache.ambari.logfeeder.plugin.filter.mapper.Mapper;
import org.apache.ambari.logfeeder.plugin.input.Input;
import org.apache.ambari.logfeeder.plugin.input.InputMarker;
import org.apache.ambari.logfeeder.plugin.manager.OutputManager;
import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.MapFieldDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.PostMapValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Filter<PROP_TYPE extends LogFeederProperties> extends ConfigItem<PROP_TYPE> {

  private static final Logger LOG = LoggerFactory.getLogger(Filter.class);

  private final Map<String, List<Mapper>> postFieldValueMappers = new HashMap<>();
  private FilterDescriptor filterDescriptor;
  private PROP_TYPE logFeederProperties;
  private Filter nextFilter = null;
  private Input input;
  private OutputManager outputManager;

  public void loadConfigs(FilterDescriptor filterDescriptor, PROP_TYPE logFeederProperties, OutputManager outputManager) {
    this.filterDescriptor = filterDescriptor;
    this.logFeederProperties = logFeederProperties;
    this.outputManager = outputManager;
  }

  public FilterDescriptor getFilterDescriptor() {
    return filterDescriptor;
  }

  public PROP_TYPE getLogFeederProperties() {
    return logFeederProperties;
  }

  @Override
  public void init(PROP_TYPE logFeederProperties) throws Exception {
    initializePostMapValues();
    if (nextFilter != null) {
      nextFilter.init(logFeederProperties);
    }
  }

  private void initializePostMapValues() {
    Map<String, ? extends List<? extends PostMapValues>> postMapValues = filterDescriptor.getPostMapValues();
    if (postMapValues == null) {
      return;
    }
    for (String fieldName : postMapValues.keySet()) {
      List<? extends PostMapValues> values = postMapValues.get(fieldName);
      for (PostMapValues pmv : values) {
        for (MapFieldDescriptor mapFieldDescriptor : pmv.getMappers()) {
          String mapClassCode = mapFieldDescriptor.getJsonName();
          Mapper mapper = (Mapper) AliasUtil.getClassInstance(mapClassCode, AliasUtil.AliasType.MAPPER);
          if (mapper == null) {
            LOG.warn("Unknown mapper type: " + mapClassCode);
            continue;
          }
          if (mapper.init(getInput().getShortDescription(), fieldName, mapClassCode, mapFieldDescriptor)) {
            List<Mapper> fieldMapList = postFieldValueMappers.computeIfAbsent(fieldName, k -> new ArrayList<>());
            fieldMapList.add(mapper);
          }
        }
      }
    }
  }

  /**
   * Deriving classes should implement this at the minimum
   */
  public void apply(String inputStr, InputMarker inputMarker) throws Exception {
    // TODO: There is no transformation for string types.
    if (nextFilter != null) {
      nextFilter.apply(inputStr, inputMarker);
    } else {
      outputManager.write(inputStr, inputMarker);
    }
  }

  public void apply(Map<String, Object> jsonObj, InputMarker inputMarker) throws Exception {
    for (String fieldName : postFieldValueMappers.keySet()) {
      Object value = jsonObj.get(fieldName);
      if (value != null) {
        for (Mapper mapper : postFieldValueMappers.get(fieldName)) {
          value = mapper.apply(jsonObj, value);
        }
      }
    }
    if (nextFilter != null) {
      nextFilter.apply(jsonObj, inputMarker);
    } else {
      outputManager.write(jsonObj, inputMarker);
    }
  }

  public void loadConfig(FilterDescriptor filterDescriptor) {
    this.filterDescriptor = filterDescriptor;
  }

  public Filter getNextFilter() {
    return nextFilter;
  }

  public void setNextFilter(Filter nextFilter) {
    this.nextFilter = nextFilter;
  }

  public Input getInput() {
    return input;
  }

  public void setInput(Input input) {
    this.input = input;
  }

  public OutputManager getOutputManager() {
    return outputManager;
  }

  public void setOutputManager(OutputManager outputManager) {
    this.outputManager = outputManager;
  }

  public void flush() {
    // empty
  }

  public void close() {
    if (nextFilter != null) {
      nextFilter.close();
    }
  }

  @Override
  public boolean isEnabled() {
    return filterDescriptor.isEnabled() != null ? filterDescriptor.isEnabled() : true;
  }

  @Override
  public void addMetricsContainers(List<MetricData> metricsList) {
    super.addMetricsContainers(metricsList);
    if (nextFilter != null) {
      nextFilter.addMetricsContainers(metricsList);
    }
  }

  @Override
  public boolean logConfigs() {
    LOG.info("filter=" + getShortDescription());
    return true;
  }

  @Override
  public String getStatMetricName() {
    // no metrics yet
    return null;
  }

  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }
}
