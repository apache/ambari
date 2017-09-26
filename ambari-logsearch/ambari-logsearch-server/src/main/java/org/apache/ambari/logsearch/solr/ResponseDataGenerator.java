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

package org.apache.ambari.logsearch.solr;

import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.COMPONENT;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.PATH;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.ambari.logsearch.model.response.BarGraphData;
import org.apache.ambari.logsearch.model.response.BarGraphDataListResponse;
import org.apache.ambari.logsearch.model.response.CountData;
import org.apache.ambari.logsearch.model.response.CountDataListResponse;
import org.apache.ambari.logsearch.model.response.GraphData;
import org.apache.ambari.logsearch.model.response.GraphDataListResponse;
import org.apache.ambari.logsearch.model.response.HostLogFilesResponse;
import org.apache.ambari.logsearch.model.response.NameValueData;
import org.apache.ambari.logsearch.model.response.NameValueDataListResponse;
import org.apache.ambari.logsearch.model.response.NodeData;
import org.apache.ambari.logsearch.model.response.NodeListResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.PivotField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.common.util.NamedList;

import javax.inject.Named;

@Named
public class ResponseDataGenerator {

  public BarGraphDataListResponse generateBarGraphDataResponseWithRanges(QueryResponse response, String typeField, boolean typeUppercase) {
    BarGraphDataListResponse dataList = new BarGraphDataListResponse();
    if (response == null) {
      return dataList;
    }
    NamedList<List<PivotField>> facetPivotResponse = response.getFacetPivot();
    if (response.getFacetPivot() == null) {
      return dataList;
    }
    List<PivotField> pivotFields = facetPivotResponse.get(typeField);
    for (int pivotIndex = 0; pivotIndex < pivotFields.size(); pivotIndex++) {
      PivotField pivotField = facetPivotResponse.get(typeField).get(pivotIndex);
      List<NameValueData> nameValues = generateNameValueDataList(pivotField.getFacetRanges());
      BarGraphData barGraphData = new BarGraphData();
      barGraphData.setDataCount(nameValues);
      String typeValue = typeUppercase ? StringUtils.upperCase(pivotField.getValue().toString()) : pivotField.getValue().toString();
      barGraphData.setName(typeValue);
      dataList.getGraphData().add(barGraphData);
    }
    return dataList;
  }

  public BarGraphDataListResponse generateSecondLevelBarGraphDataResponse(QueryResponse response, int val) {
    BarGraphDataListResponse barGraphDataListResponse = new BarGraphDataListResponse();
    NamedList<List<PivotField>> pivotFieldNameList = response.getFacetPivot();
    if (pivotFieldNameList == null) {
      return barGraphDataListResponse;
    }
    List<PivotField> pivotFields = pivotFieldNameList.getVal(val);
    List<BarGraphData> barGraphDataList = new ArrayList<>();
    for (PivotField pivotField : pivotFields) {
      BarGraphData barGraphData = new BarGraphData();
      barGraphData.setName(String.valueOf(pivotField.getValue()));
      List<PivotField> secondLevelPivotFields = pivotField.getPivot();
      List<NameValueData> nameValueDataList = new ArrayList<>();
      for (PivotField sPivotField : secondLevelPivotFields) {
        NameValueData nvD = new NameValueData();
        nvD.setName(String.valueOf(sPivotField.getValue()));
        nvD.setValue(String.valueOf(sPivotField.getCount()));
        nameValueDataList.add(nvD);
      }
      barGraphData.setDataCount(nameValueDataList);
      barGraphDataList.add(barGraphData);
    }
    barGraphDataListResponse.setGraphData(barGraphDataList);
    return barGraphDataListResponse;
  }

  public BarGraphDataListResponse generateBarGraphFromFieldFacet(QueryResponse response, String facetField) {
    BarGraphDataListResponse dataList = new BarGraphDataListResponse();
    Collection<BarGraphData> vaDatas = new ArrayList<>();
    dataList.setGraphData(vaDatas);
    if (response == null) {
      return dataList;
    }
    FacetField facetFieldObj = response.getFacetField(facetField);
    if (facetFieldObj == null) {
      return dataList;
    }

    List<Count> counts = facetFieldObj.getValues();
    if (counts == null) {
      return dataList;
    }
    for (Count cnt : counts) {
      List<NameValueData> valueList = new ArrayList<>();
      BarGraphData vBarGraphData = new BarGraphData();
      vaDatas.add(vBarGraphData);
      NameValueData vNameValue = new NameValueData();
      vNameValue.setName(cnt.getName());
      vBarGraphData.setName(cnt.getName().toUpperCase());
      vNameValue.setValue("" + cnt.getCount());
      valueList.add(vNameValue);
      vBarGraphData.setDataCount(valueList);
    }
    return dataList;
  }

  @SuppressWarnings("rawtypes")
  public List<NameValueData> generateNameValueDataList(List<RangeFacet> rangeFacet) {
    List<NameValueData> nameValues = new ArrayList<>();
    if (rangeFacet == null) {
      return nameValues;
    }
    RangeFacet<?, ?> range = rangeFacet.get(0);

    if (range == null) {
      return nameValues;
    }
    List<RangeFacet.Count> listCount = range.getCounts();
    for (RangeFacet.Count cnt : listCount) {
      NameValueData nameValue = new NameValueData();
      nameValue.setName(String.valueOf(cnt.getValue()));
      nameValue.setValue(String.valueOf(cnt.getCount()));
      nameValues.add(nameValue);
    }
    return nameValues;
  }

  public List<Count> generateCount(QueryResponse response) {
    List<Count> counts = new ArrayList<>();
    List<FacetField> facetFields = null;
    FacetField facetField = null;
    if (response == null) {
      return counts;
    }

    facetFields = response.getFacetFields();
    if (facetFields == null) {
      return counts;
    }
    if (!facetFields.isEmpty()) {
      facetField = facetFields.get(0);
    }
    if (facetField != null) {
      counts = facetField.getValues();
    }
    return counts;
  }

  public BarGraphDataListResponse getGraphDataWithDefaults(QueryResponse queryResponse, String field, String[] defaults) {
    BarGraphDataListResponse response = new BarGraphDataListResponse();
    BarGraphData barGraphData = new BarGraphData();
    List<NameValueData> nameValues = generateLevelCountData(queryResponse, defaults, true);
    barGraphData.setName(field);
    barGraphData.setDataCount(nameValues);
    response.setGraphData(Lists.newArrayList(barGraphData));
    return response;
  }

  public NameValueDataListResponse getNameValueDataListResponseWithDefaults(QueryResponse response, String[] defaults, boolean emptyResponseDisabled) {
    NameValueDataListResponse result = new NameValueDataListResponse();
    result.setvNameValues(generateLevelCountData(response, defaults, emptyResponseDisabled));
    return result;
  }

  public NodeListResponse generateServiceNodeTreeFromFacetResponse(QueryResponse queryResponse,
                                                                   String firstHierarchy, String secondHierarchy,
                                                                   String firstType, String secondType) {
    NodeListResponse response = new NodeListResponse();
    if (queryResponse == null) {
      return response;
    }
    NamedList<List<PivotField>> namedPivotFieldList = queryResponse.getFacetPivot();
    List<PivotField> firstLevelPivots = namedPivotFieldList.get(firstHierarchy);
    List<PivotField> secondLevelPivots = namedPivotFieldList.get(secondHierarchy);
    if (!CollectionUtils.isNotEmpty(firstLevelPivots) || !CollectionUtils.isNotEmpty(secondLevelPivots)) {
      return response;
    }
    List<NodeData> nodeDataList = buidTreeData(firstLevelPivots, secondLevelPivots, firstType, secondType);
    response.setvNodeList(nodeDataList);
    return response;
  }

  public NodeListResponse generateOneLevelServiceNodeTree(QueryResponse queryResponse, String componentLevelHirachy) {
    NodeListResponse response = new NodeListResponse();
    List<NodeData> datatList = new ArrayList<>();
    List<List<PivotField>> listPivotField = new ArrayList<>();
    NamedList<List<PivotField>> namedList = queryResponse.getFacetPivot();
    if (namedList != null) {
      listPivotField = namedList.getAll(componentLevelHirachy);
    }
    List<PivotField> secondHirarchicalPivotFields = null;
    if (listPivotField == null || listPivotField.isEmpty()) {
      return response;
    } else {
      secondHirarchicalPivotFields = listPivotField.get(0);
    }
    for (PivotField singlePivotField : secondHirarchicalPivotFields) {
      if (singlePivotField != null) {
        NodeData comp = new NodeData();
        comp.setName("" + singlePivotField.getValue());
        List<PivotField> levelList = singlePivotField.getPivot();
        List<NameValueData> levelCountList = new ArrayList<>();
        comp.setLogLevelCount(levelCountList);
        if (levelList != null) {
          for (PivotField levelPivot : levelList) {
            NameValueData level = new NameValueData();
            level.setName(("" + levelPivot.getValue()).toUpperCase());
            level.setValue("" + levelPivot.getCount());
            levelCountList.add(level);
          }
        }
        datatList.add(comp);
      }
    }
    response.setvNodeList(datatList);
    return response;
  }

  private List<NodeData> buidTreeData(List<PivotField> firstHirarchicalPivotFields,
                                      List<PivotField> secondHirarchicalPivotFields,
                                      String firstPriority, String secondPriority) {
    List<NodeData> extensionTree = new ArrayList<>();
    if (firstHirarchicalPivotFields != null) {
      for (PivotField pivotHost : firstHirarchicalPivotFields) {
        if (pivotHost != null) {
          NodeData hostNode = new NodeData();
          String name = (pivotHost.getValue() == null ? "" : "" + pivotHost.getValue());
          String value = "" + pivotHost.getCount();
          if (StringUtils.isNotBlank(name)) {
            hostNode.setName(name);
          }
          if (StringUtils.isNotBlank(value)) {
            hostNode.setValue(value);
          }
          if (StringUtils.isNotBlank(firstPriority)) {
            hostNode.setType(firstPriority);
          }

          hostNode.setParent(true);
          hostNode.setRoot(true);
          PivotField hostPivot = null;
          for (PivotField searchHost : secondHirarchicalPivotFields) {
            if (StringUtils.isNotBlank(hostNode.getName())
              && hostNode.getName().equals(searchHost.getValue())) {
              hostPivot = searchHost;
              break;
            }
          }
          List<PivotField> pivotLevelHost = hostPivot == null? null : hostPivot.getPivot();
          if (pivotLevelHost != null) {
            Collection<NameValueData> logLevelCount = new ArrayList<>();
            for (PivotField pivotLevel : pivotLevelHost) {
              if (pivotLevel != null) {
                NameValueData vnameValue = new NameValueData();
                String levelName = (pivotLevel.getValue() == null ? "" : "" + pivotLevel.getValue());
                vnameValue.setName(levelName.toUpperCase());
                vnameValue.setValue("" + pivotLevel.getCount());
                logLevelCount.add(vnameValue);
              }
            }
            hostNode.setLogLevelCount(logLevelCount);
          }
          List<PivotField> pivotComponents = pivotHost.getPivot();
          if (pivotComponents != null) {
            Collection<NodeData> componentNodes = new ArrayList<>();
            for (PivotField pivotComp : pivotComponents) {
              if (pivotComp != null) {
                NodeData compNode = new NodeData();
                String compName = (pivotComp.getValue() == null ? "" : "" + pivotComp.getValue());
                compNode.setName(compName);
                if (StringUtils.isNotBlank(secondPriority)) {
                  compNode.setType(secondPriority);
                }
                compNode.setValue("" + pivotComp.getCount());
                compNode.setParent(false);
                compNode.setRoot(false);
                List<PivotField> pivotLevels = pivotComp.getPivot();
                if (pivotLevels != null) {
                  Collection<NameValueData> logLevelCount = new ArrayList<>();
                  for (PivotField pivotLevel : pivotLevels) {
                    if (pivotLevel != null) {
                      NameValueData vnameValue = new NameValueData();
                      String compLevel = pivotLevel.getValue() == null ? "" : "" + pivotLevel.getValue();
                      vnameValue.setName((compLevel).toUpperCase());

                      vnameValue.setValue("" + pivotLevel.getCount());
                      logLevelCount.add(vnameValue);
                    }
                  }
                  compNode.setLogLevelCount(logLevelCount);
                }
                componentNodes.add(compNode);
              }
            }
            hostNode.setChilds(componentNodes);
          }
          extensionTree.add(hostNode);
        }
      }
    }

    return extensionTree;
  }

  private List<NameValueData> generateLevelCountData(QueryResponse queryResponse, String[] defaults, boolean emptyResponseEnabled) {
    List<NameValueData> nameValues = Lists.newLinkedList();
    Map<String, NameValueData> linkedMap = Maps.newLinkedHashMap();
    List<Count> counts = generateCount(queryResponse);
    if (!CollectionUtils.isNotEmpty(counts) && emptyResponseEnabled) {
      return nameValues;
    }
    for (String defaultValue : defaults) {
      NameValueData nameValue = new NameValueData();
      nameValue.setName(defaultValue);
      nameValue.setValue("0");
      linkedMap.put(defaultValue, nameValue);
    }
    if (CollectionUtils.isNotEmpty(counts)) {
      for (Count count : counts) {
        if (!linkedMap.containsKey(count.getName())) {
          NameValueData nameValue = new NameValueData();
          String name = count.getName().toUpperCase();
          nameValue.setName(name);
          nameValue.setValue(String.valueOf(count.getCount()));
          linkedMap.put(name, nameValue);
        }
      }
    }

    for (Map.Entry<String, NameValueData> nameValueDataEntry : linkedMap.entrySet()) {
      nameValues.add(nameValueDataEntry.getValue());
    }
    return nameValues;
  }

  public CountDataListResponse generateCountResponseByField(QueryResponse response, String field) {
    CountDataListResponse collection = new CountDataListResponse();
    List<CountData> vCounts = new ArrayList<>();
    if (response == null) {
      return collection;
    }
    FacetField facetFields = response.getFacetField(field);
    if (facetFields == null) {
      return collection;
    }
    List<Count> fieldList = facetFields.getValues();

    if (fieldList == null) {
      return collection;
    }

    for (Count cnt : fieldList) {
      if (cnt != null) {
        CountData vCount = new CountData();
        vCount.setName(cnt.getName());
        vCount.setCount(cnt.getCount());
        vCounts.add(vCount);
      }
    }
    collection.setvCounts(vCounts);
    return collection;
  }

  public GraphDataListResponse generateSimpleGraphResponse(QueryResponse response, String hierarchy) {
    GraphDataListResponse graphInfo = new GraphDataListResponse();
    if (response == null) {
      return graphInfo;
    }
    List<List<PivotField>> hirarchicalPivotField = new ArrayList<List<PivotField>>();
    List<GraphData> dataList = new ArrayList<>();
    NamedList<List<PivotField>> namedList = response.getFacetPivot();
    if (namedList != null) {
      hirarchicalPivotField = namedList.getAll(hierarchy);
    }
    if (!hirarchicalPivotField.isEmpty()) {
      dataList = buidGraphData(hirarchicalPivotField.get(0));
    }
    if (!dataList.isEmpty()) {
      graphInfo.setGraphData(dataList);
    }

    return graphInfo;
  }

  private List<GraphData> buidGraphData(List<PivotField> pivotFields) {
    List<GraphData> logList = new ArrayList<>();
    if (pivotFields != null) {
      for (PivotField pivotField : pivotFields) {
        if (pivotField != null) {
          GraphData logLevel = new GraphData();
          logLevel.setName("" + pivotField.getValue());
          logLevel.setCount(Long.valueOf(pivotField.getCount()));
          if (pivotField.getPivot() != null) {
            logLevel.setDataList(buidGraphData(pivotField.getPivot()));
          }
          logList.add(logLevel);
        }
      }
    }
    return logList;
  }
  

  public HostLogFilesResponse generateHostLogFilesResponse(QueryResponse queryResponse) {
    HostLogFilesResponse response = new HostLogFilesResponse();
    Map<String, List<String>> componentLogFiles = response.getHostLogFiles();
    
    NamedList<List<PivotField>> facetPivot = queryResponse.getFacetPivot();
    List<PivotField> componentFields = facetPivot.get(COMPONENT + "," + PATH);
    for (PivotField componentField : componentFields) {
      String component = (String)componentField.getValue();
      LinkedList<String> logFileList = new LinkedList<>();
      componentLogFiles.put(component, logFileList);
      
      for (PivotField logField : componentField.getPivot()) {
        // the log file names are in increasing order of their cardinality, using addFirst reverses the list
        logFileList.addFirst((String)logField.getValue());
      }
    }
    
    return response;
  }
}
