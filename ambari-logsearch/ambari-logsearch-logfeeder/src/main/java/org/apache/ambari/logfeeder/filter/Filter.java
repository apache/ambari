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

package org.apache.ambari.logfeeder.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.logfeeder.common.ConfigItem;
import org.apache.ambari.logfeeder.common.LogFeederException;
import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.input.Input;
import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.ambari.logfeeder.mapper.Mapper;
import org.apache.ambari.logfeeder.metrics.MetricData;
import org.apache.ambari.logfeeder.output.OutputManager;
import org.apache.ambari.logfeeder.util.AliasUtil;
import org.apache.ambari.logfeeder.util.AliasUtil.AliasType;
import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.MapFieldDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.PostMapValues;
import org.apache.commons.lang.BooleanUtils;
import org.apache.log4j.Priority;

public abstract class Filter extends ConfigItem {
  protected FilterDescriptor filterDescriptor;
  protected Input input;
  private Filter nextFilter = null;
  private OutputManager outputManager;

  private Map<String, List<Mapper>> postFieldValueMappers = new HashMap<String, List<Mapper>>();

  public void loadConfig(FilterDescriptor filterDescriptor) {
    this.filterDescriptor = filterDescriptor;
  }

  public FilterDescriptor getFilterDescriptor() {
    return filterDescriptor;
  }

  @Override
  public void init(LogFeederProps logFeederProps) throws Exception {
    super.init(logFeederProps);

    initializePostMapValues();
    if (nextFilter != null) {
      nextFilter.init(logFeederProps);
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
          Mapper mapper = (Mapper) AliasUtil.getClassInstance(mapClassCode, AliasType.MAPPER);
          if (mapper == null) {
            LOG.warn("Unknown mapper type: " + mapClassCode);
            continue;
          }
          if (mapper.init(getInput().getShortDescription(), fieldName, mapClassCode, mapFieldDescriptor)) {
            List<Mapper> fieldMapList = postFieldValueMappers.get(fieldName);
            if (fieldMapList == null) {
              fieldMapList = new ArrayList<Mapper>();
              postFieldValueMappers.put(fieldName, fieldMapList);
            }
            fieldMapList.add(mapper);
          }
        }
      }
    }
  }

  public void setOutputManager(OutputManager outputManager) {
    this.outputManager = outputManager;
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

  /**
   * Deriving classes should implement this at the minimum
   */
  public void apply(String inputStr, InputMarker inputMarker) throws LogFeederException  {
    // TODO: There is no transformation for string types.
    if (nextFilter != null) {
      nextFilter.apply(inputStr, inputMarker);
    } else {
      outputManager.write(inputStr, inputMarker);
    }
  }

  public void apply(Map<String, Object> jsonObj, InputMarker inputMarker) throws LogFeederException {
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

  public void close() {
    if (nextFilter != null) {
      nextFilter.close();
    }
  }

  public void flush() {

  }

  @Override
  public void logStat() {
    super.logStat();
    if (nextFilter != null) {
      nextFilter.logStat();
    }
  }

  @Override
  public boolean isEnabled() {
    return BooleanUtils.isNotFalse(filterDescriptor.isEnabled());
  }

  @Override
  public String getShortDescription() {
    return null;
  }

  @Override
  public boolean logConfigs(Priority level) {
    if (!super.logConfigs(level)) {
      return false;
    }
    LOG.log(level, "input=" + input.getShortDescription());
    return true;
  }

  @Override
  public void addMetricsContainers(List<MetricData> metricsList) {
    super.addMetricsContainers(metricsList);
    if (nextFilter != null) {
      nextFilter.addMetricsContainers(metricsList);
    }
  }
}
