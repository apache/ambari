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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.ambari.logsearch.util.DateUtil;
import org.apache.ambari.logsearch.view.VBarGraphData;
import org.apache.ambari.logsearch.view.VNameValue;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.springframework.beans.factory.annotation.Autowired;

public class GraphDataGeneratorBase {

  @Autowired
  DateUtil dateUtil;

  @SuppressWarnings("unchecked")
  protected void extractRangeStackValuesFromBucket(
    SimpleOrderedMap<Object> jsonFacetResponse, String outerField,
    String innerField, List<VBarGraphData> histogramData) {
    NamedList<Object> stack = (NamedList<Object>) jsonFacetResponse
      .get(outerField);
    ArrayList<Object> stackBuckets = (ArrayList<Object>) stack
      .get("buckets");
    for (Object temp : stackBuckets) {
      VBarGraphData vBarGraphData = new VBarGraphData();

      SimpleOrderedMap<Object> level = (SimpleOrderedMap<Object>) temp;
      String name = ((String) level.getVal(0)).toUpperCase();
      vBarGraphData.setName(name);

      Collection<VNameValue> vNameValues = new ArrayList<VNameValue>();
      vBarGraphData.setDataCounts(vNameValues);
      ArrayList<Object> levelBuckets = (ArrayList<Object>) ((NamedList<Object>) level
        .get(innerField)).get("buckets");
      for (Object temp1 : levelBuckets) {
        SimpleOrderedMap<Object> countValue = (SimpleOrderedMap<Object>) temp1;
        String value = dateUtil
          .convertDateWithMillisecondsToSolrDate((Date) countValue
            .getVal(0));

        String count = "" + countValue.getVal(1);
        VNameValue vNameValue = new VNameValue();
        vNameValue.setName(value);
        vNameValue.setValue(count);
        vNameValues.add(vNameValue);
      }
      histogramData.add(vBarGraphData);
    }
  }

  @SuppressWarnings("unchecked")
  protected boolean extractNonRangeStackValuesFromBucket(
    SimpleOrderedMap<Object> jsonFacetResponse, String level,
    Collection<VBarGraphData> vGraphDatas, String typeXAxis) {

    boolean zeroFlag = true;
    if (jsonFacetResponse.get(level).toString().equals("{count=0}")) {
      return false;
    }

    NamedList<Object> list = (NamedList<Object>) jsonFacetResponse
      .get(level);

    ArrayList<Object> list3 = (ArrayList<Object>) list.get("buckets");
    int i = 0;
    for (i = 0; i < list3.size(); i++) {
      VBarGraphData vGraphData = new VBarGraphData();


      Collection<VNameValue> levelCounts = new ArrayList<VNameValue>();
      vGraphData.setDataCounts(levelCounts);

      SimpleOrderedMap<Object> valueCount = (SimpleOrderedMap<Object>) list3
        .get(i);
      String name = ("" + valueCount.getVal(0)).trim();
      if (isTypeNumber(typeXAxis)) {
        VNameValue nameValue = new VNameValue();
        String value = ("" + valueCount.getVal(2)).trim().substring(0, ("" + valueCount.getVal(2)).indexOf("."));
        nameValue.setName(name);
        nameValue.setValue(value);
        levelCounts.add(nameValue);
      } else {
        SimpleOrderedMap<Object> l1 = (SimpleOrderedMap<Object>) valueCount
          .getVal(2);
        ArrayList<Object> l2 = (ArrayList<Object>) l1.get("buckets");
        for (int j = 0; l2 != null && j < l2.size(); j++) {
          VNameValue nameValue = new VNameValue();
          SimpleOrderedMap<Object> innerValueCount = (SimpleOrderedMap<Object>) l2
            .get(j);
          nameValue.setName(("" + innerValueCount.getVal(0)).trim());
          nameValue.setValue(("" + innerValueCount.getVal(1)).trim());
          levelCounts.add(nameValue);
        }
      }

      vGraphData.setName(name);
      vGraphDatas.add(vGraphData);
    }
    return zeroFlag;
  }

  @SuppressWarnings("unchecked")
  protected boolean extractValuesFromJson(
    SimpleOrderedMap<Object> jsonFacetResponse, String level,
    VBarGraphData histogramData, List<RangeFacet.Count> counts) {
    histogramData.setName(level);
    Collection<VNameValue> levelCounts = new ArrayList<VNameValue>();
    histogramData.setDataCounts(levelCounts);
    boolean zeroFlag = true;
    if (jsonFacetResponse.get(level).toString().equals("{count=0}")) {
      for (RangeFacet.Count date : counts) {
        VNameValue nameValue = new VNameValue();

        nameValue.setName(date.getValue());
        nameValue.setValue("0");

        levelCounts.add(nameValue);
      }
      return false;
    }
    NamedList<Object> list = (NamedList<Object>) jsonFacetResponse
      .get(level);
    NamedList<Object> list2 = (NamedList<Object>) list.getVal(1);
    ArrayList<Object> list3 = (ArrayList<Object>) list2.get("buckets");
    int i = 0;
    for (RangeFacet.Count date : counts) {
      VNameValue nameValue = new VNameValue();
      SimpleOrderedMap<Object> valueCount = (SimpleOrderedMap<Object>) list3
        .get(i);
      String count = ("" + valueCount.getVal(1)).trim();
      if (!"0".equals(count)) {
        zeroFlag = false;
      }
      nameValue.setName(date.getValue());
      nameValue.setValue(count);

      levelCounts.add(nameValue);
      i++;
    }

    return zeroFlag;
  }

  protected boolean isTypeNumber(String typeXAxis) {
    return "long".contains(typeXAxis) || "int".contains(typeXAxis)
      || "float".contains(typeXAxis) || "double".contains(typeXAxis);
  }

  public String convertObjToString(Object obj) throws IOException {
    if (obj == null) {
      return "";
    }
    ObjectMapper mapper = new ObjectMapper();
    ObjectWriter w = mapper.writerWithDefaultPrettyPrinter();
    return w.writeValueAsString(obj);
  }

}
