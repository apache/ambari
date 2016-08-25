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
package org.apache.ambari.logsearch.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.ambari.logsearch.manager.MgrBase;
import org.apache.ambari.logsearch.util.DateUtil;
import org.apache.ambari.logsearch.view.VBarGraphData;
import org.apache.ambari.logsearch.view.VNameValue;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;

class GraphDataGeneratorBase extends MgrBase {

  private static final String BUCKETS = "buckets";
  
  private static enum DataType {
    LONG("long"),
    DOUBLE("double"),
    FLOAT("long"),
    INT("long");
    
    private String type;
    
    DataType(String type) {
      this.type = type;
    }
    
    String getType() {
      return type;
    }
  }

  protected static enum GraphType {
    UNKNOWN,
    NORMAL_GRAPH,
    RANGE_NON_STACK_GRAPH,
    NON_RANGE_STACK_GRAPH,
    RANGE_STACK_GRAPH;
  }

  @SuppressWarnings("unchecked")
  protected void extractRangeStackValuesFromBucket(SimpleOrderedMap<Object> jsonFacetResponse, String outerField,
      String innerField, List<VBarGraphData> histogramData) {
    if (jsonFacetResponse != null) {
      NamedList<Object> stack = (NamedList<Object>) jsonFacetResponse.get(outerField);
      if (stack != null) {
        ArrayList<Object> stackBuckets = (ArrayList<Object>) stack.get(BUCKETS);
        if (stackBuckets != null) {
          for (Object stackBucket : stackBuckets) {
            VBarGraphData vBarGraphData = new VBarGraphData();
            SimpleOrderedMap<Object> level = (SimpleOrderedMap<Object>) stackBucket;
            if (level != null) {
              String name = level.getVal(0) != null ? level.getVal(0).toString().toUpperCase() : "";
              vBarGraphData.setName(name);
              Collection<VNameValue> vNameValues = new ArrayList<VNameValue>();
              NamedList<Object> innerFiledValue = (NamedList<Object>) level.get(innerField);
              if (innerFiledValue != null) {
                ArrayList<Object> levelBuckets = (ArrayList<Object>) innerFiledValue.get(BUCKETS);
                if (levelBuckets != null) {
                  for (Object levelBucket : levelBuckets) {
                    SimpleOrderedMap<Object> countValue = (SimpleOrderedMap<Object>) levelBucket;
                    if (countValue != null) {
                      String innerName = DateUtil.convertDateWithMillisecondsToSolrDate((Date) countValue.getVal(0));
                      String innerValue = countValue.getVal(1) != null ? countValue.getVal(1).toString() : "";
                      VNameValue vNameValue = new VNameValue(innerName, innerValue);
                      vNameValues.add(vNameValue);
                    }
                  }
                }
              }
              vBarGraphData.setDataCounts(vNameValues);
            }
            histogramData.add(vBarGraphData);
          }
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  protected boolean extractNonRangeStackValuesFromBucket(SimpleOrderedMap<Object> jsonFacetResponse, String level,
      Collection<VBarGraphData> vGraphDatas, String typeXAxis) {
    boolean zeroFlag = true;
    if (jsonFacetResponse == null || jsonFacetResponse.get(level) == null
        || jsonFacetResponse.get(level).toString().equals("{count=0}")) {
      return false;
    }
    NamedList<Object> levelList = (NamedList<Object>) jsonFacetResponse.get(level);
    if (levelList != null) {
      ArrayList<Object> bucketList = (ArrayList<Object>) levelList.get(BUCKETS);
      if (bucketList != null) {
        for (int index = 0; index < bucketList.size(); index++) {
          SimpleOrderedMap<Object> valueCount = (SimpleOrderedMap<Object>) bucketList.get(index);
          if (valueCount != null && valueCount.size() > 2) {
            VBarGraphData vGraphData = new VBarGraphData();
            Collection<VNameValue> levelCounts = new ArrayList<VNameValue>();
            String name = valueCount.getVal(0) != null ? valueCount.getVal(0).toString().trim() : "";
            if (isTypeNumber(typeXAxis)) {
              VNameValue nameValue = new VNameValue();
              Double sumValue = (Double) valueCount.getVal(2);
              String value = "0";// default is zero
              if (sumValue != null) {
                value = "" + sumValue.longValue();
              }
              nameValue.setName(name);
              nameValue.setValue(value);
              levelCounts.add(nameValue);
            } else {
              SimpleOrderedMap<Object> valueCountMap = (SimpleOrderedMap<Object>) valueCount.getVal(2);
              if (valueCountMap != null) {
                ArrayList<Object> buckets = (ArrayList<Object>) valueCountMap.get(BUCKETS);
                if (buckets != null) {
                  for (int innerIndex = 0; innerIndex < buckets.size(); innerIndex++) {
                    SimpleOrderedMap<Object> innerValueCount = (SimpleOrderedMap<Object>) buckets.get(innerIndex);
                    if (innerValueCount != null) {
                      String innerName = innerValueCount.getVal(0) != null ? innerValueCount.getVal(0).toString().trim() : "";
                      String innerValue = innerValueCount.getVal(1) != null ? innerValueCount.getVal(1).toString().trim() : "";
                      VNameValue nameValue = new VNameValue(innerName, innerValue);
                      levelCounts.add(nameValue);
                    }
                  }
                }
              }
            }
            vGraphData.setName(name);
            vGraphData.setDataCounts(levelCounts);
            vGraphDatas.add(vGraphData);
          }
        }
      }
    }
    return zeroFlag;
  }

  protected boolean isTypeNumber(String typeXAxis) {
    if (StringUtils.isBlank(typeXAxis)) {
      return false;
    } else {
      return typeXAxis.contains(DataType.LONG.getType()) || typeXAxis.contains(DataType.INT.getType())
          || typeXAxis.contains(DataType.FLOAT.getType()) || typeXAxis.contains(DataType.DOUBLE.getType());
    }
  }
}
