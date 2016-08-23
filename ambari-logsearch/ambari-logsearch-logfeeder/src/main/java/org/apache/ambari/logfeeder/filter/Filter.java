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

import org.apache.ambari.logfeeder.common.ConfigBlock;
import org.apache.ambari.logfeeder.common.LogfeederException;
import org.apache.ambari.logfeeder.input.Input;
import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.ambari.logfeeder.mapper.Mapper;
import org.apache.ambari.logfeeder.metrics.MetricCount;
import org.apache.ambari.logfeeder.output.OutputMgr;
import org.apache.ambari.logfeeder.util.AliasUtil;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.ambari.logfeeder.util.AliasUtil.ALIAS_PARAM;
import org.apache.ambari.logfeeder.util.AliasUtil.ALIAS_TYPE;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

public abstract class Filter extends ConfigBlock {
  private static final Logger logger = Logger.getLogger(Filter.class);

  protected Input input;
  private Filter nextFilter = null;
  private OutputMgr outputMgr;

  private Map<String, List<Mapper>> postFieldValueMappers = new HashMap<String, List<Mapper>>();

  @Override
  public void init() throws Exception {
    super.init();

    initializePostMapValues();
    if (nextFilter != null) {
      nextFilter.init();
    }
  }

  @SuppressWarnings("unchecked")
  private void initializePostMapValues() {
    Map<String, Object> postMapValues = (Map<String, Object>) getConfigValue("post_map_values");
    if (postMapValues == null) {
      return;
    }
    for (String fieldName : postMapValues.keySet()) {
      List<Map<String, Object>> mapList = null;
      Object values = postMapValues.get(fieldName);
      if (values instanceof List<?>) {
        mapList = (List<Map<String, Object>>) values;
      } else {
        mapList = new ArrayList<Map<String, Object>>();
        mapList.add((Map<String, Object>) values);
      }
      for (Map<String, Object> mapObject : mapList) {
        for (String mapClassCode : mapObject.keySet()) {
          Mapper mapper = getMapper(mapClassCode);
          if (mapper == null) {
            break;
          }
          if (mapper.init(getInput().getShortDescription(),
            fieldName, mapClassCode,
            mapObject.get(mapClassCode))) {
            List<Mapper> fieldMapList = postFieldValueMappers
              .get(fieldName);
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

  private Mapper getMapper(String mapClassCode) {
    String classFullName = AliasUtil.getInstance().readAlias(mapClassCode, ALIAS_TYPE.MAPPER, ALIAS_PARAM.KLASS);
    if (classFullName != null && !classFullName.isEmpty()) {
      Mapper mapper = (Mapper) LogFeederUtil.getClassInstance(classFullName, ALIAS_TYPE.MAPPER);
      return mapper;
    }
    return null;
  }

  public void setOutputMgr(OutputMgr outputMgr) {
    this.outputMgr = outputMgr;
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
  public void apply(String inputStr, InputMarker inputMarker) throws LogfeederException  {
    // TODO: There is no transformation for string types.
    if (nextFilter != null) {
      nextFilter.apply(inputStr, inputMarker);
    } else {
      outputMgr.write(inputStr, inputMarker);
    }
  }

  public void apply(Map<String, Object> jsonObj, InputMarker inputMarker) throws LogfeederException {
    if (postFieldValueMappers.size() > 0) {
      for (String fieldName : postFieldValueMappers.keySet()) {
        Object value = jsonObj.get(fieldName);
        if (value != null) {
          for (Mapper mapper : postFieldValueMappers.get(fieldName)) {
            value = mapper.apply(jsonObj, value);
          }
        }
      }
    }
    if (nextFilter != null) {
      nextFilter.apply(jsonObj, inputMarker);
    } else {
      outputMgr.write(jsonObj, inputMarker);
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
  public boolean isFieldConditionMatch(String fieldName, String stringValue) {
    if (!super.isFieldConditionMatch(fieldName, stringValue)) {
      if (input != null) {
        return input.isFieldConditionMatch(fieldName, stringValue);
      } else {
        return false;
      }
    }
    return true;
  }

  @Override
  public String getShortDescription() {
    return null;
  }

  @Override
  public boolean logConfgs(Priority level) {
    if (!super.logConfgs(level)) {
      return false;
    }
    logger.log(level, "input=" + input.getShortDescription());
    return true;
  }

  @Override
  public void addMetricsContainers(List<MetricCount> metricsList) {
    super.addMetricsContainers(metricsList);
    if (nextFilter != null) {
      nextFilter.addMetricsContainers(metricsList);
    }
  }

}
