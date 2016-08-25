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
package org.apache.ambari.logsearch.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.view.VBarDataList;
import org.apache.ambari.logsearch.view.VBarGraphData;
import org.apache.ambari.logsearch.view.VHost;
import org.apache.ambari.logsearch.view.VNameValue;
import org.apache.ambari.logsearch.view.VSummary;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.SimpleOrderedMap;

public class BizUtil {
  private static final Logger logger = Logger.getLogger(BizUtil.class);

  private BizUtil() {
    throw new UnsupportedOperationException();
  }
  
  public static String convertObjectToNormalText(SolrDocumentList docList) {
    String textToSave = "";
    HashMap<String, String> blankFieldsMap = new HashMap<String, String>();
    if (docList == null){
      return "no data";
    }
    if (docList.isEmpty()) {
      return "no data";
    }
    SolrDocument docForBlankCaculation = docList.get(0);
    if(docForBlankCaculation == null){
      return "no data";
    }
    Collection<String> fieldsForBlankCaculation = docForBlankCaculation.getFieldNames();

    int maxLengthOfField = 0;
    if (fieldsForBlankCaculation == null) {
      return "no data";
    }
    for (String field : fieldsForBlankCaculation) {
      if (!StringUtils.isBlank(field) && field.length() > maxLengthOfField) {
        maxLengthOfField = field.length();
      }
    }

    for (String field : fieldsForBlankCaculation) {
      if (!StringUtils.isBlank(field)) {
        blankFieldsMap.put(field, StringUtils.rightPad(field, maxLengthOfField - field.length()));
      }
    }

    for (SolrDocument doc : docList) {
      if (doc != null) {
        StringBuffer textTowrite = new StringBuffer();

        if (doc.getFieldValue(LogSearchConstants.LOGTIME) != null) {
          textTowrite.append(doc.getFieldValue(LogSearchConstants.LOGTIME).toString() + " ");
        }
        if (doc.getFieldValue(LogSearchConstants.SOLR_LEVEL) != null) {
          textTowrite.append(doc.getFieldValue(LogSearchConstants.SOLR_LEVEL).toString()).append(" ");
        }
        if (doc.getFieldValue(LogSearchConstants.SOLR_THREAD_NAME) != null) {
          textTowrite.append(doc.getFieldValue(LogSearchConstants.SOLR_THREAD_NAME).toString().trim()).append(" ");
        }
        if (doc.getFieldValue(LogSearchConstants.SOLR_LOGGER_NAME) != null) {
          textTowrite.append(doc.getFieldValue(LogSearchConstants.SOLR_LOGGER_NAME).toString().trim()).append(" ");
        }
        if (doc.getFieldValue(LogSearchConstants.SOLR_FILE) != null && doc.getFieldValue(LogSearchConstants.SOLR_LINE_NUMBER) != null) {
          textTowrite
              .append(doc.getFieldValue(LogSearchConstants.SOLR_FILE).toString())
              .append(":")
              .append(doc.getFieldValue(LogSearchConstants.SOLR_LINE_NUMBER).toString())
              .append(" ");
        }
        if (doc.getFieldValue(LogSearchConstants.SOLR_LOG_MESSAGE) != null) {
          textTowrite.append("- ")
              .append(doc.getFieldValue(LogSearchConstants.SOLR_LOG_MESSAGE).toString());
        }
        textTowrite.append("\n");
        textToSave += textTowrite.toString();
      }
    }
    return textToSave;
  }

  public static VSummary buildSummaryForLogFile(SolrDocumentList docList) {
    VSummary vsummary = new VSummary();
    if (CollectionUtils.isEmpty(docList)) {
      return vsummary;
    }
    int numLogs = 0;
    List<VHost> vHosts = new ArrayList<VHost>();
    vsummary.setHosts(vHosts);
    String levels = "";
    for (SolrDocument doc : docList) {
      if (doc != null) {
        // adding Host and Component appropriately
        String hostname = (String) doc.getFieldValue("host");
        String comp = (String) doc.getFieldValue("type");
        String level = (String) doc.getFieldValue("level");
        if (StringUtils.isBlank(level)) {
          level = "";
        }
        boolean newHost = true;
        for (VHost host : vHosts) {
          if (host != null && host.getName().equals(hostname)) {
            newHost = false;
            if (StringUtils.isBlank(comp)) {
              Set<String> compList = host.getComponents();
              if (compList != null) {
                compList.add(comp);
              }
            }
            break;
          }
        }
        if (newHost) {
          VHost vHost = new VHost();
          if (!StringUtils.isBlank(hostname)) {
            vHost.setName(hostname);
          }
          Set<String> component = new LinkedHashSet<String>();
          if (StringUtils.isBlank(comp)) {
            component.add(comp);
          }
          vHost.setComponents(component);
          vHosts.add(vHost);
        }
        // getting levels
        if (!levels.contains(level)) {
          levels = levels + ", " + level;
        }
        numLogs += 1;
      }
  }
    levels = levels.replaceFirst(", ", "");
    vsummary.setLevels(levels);
    vsummary.setNumberLogs("" + numLogs);
    return vsummary;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static VBarDataList buildSummaryForTopCounts(SimpleOrderedMap<Object> jsonFacetResponse,String innerJsonKey,String outerJsonKey) {

    VBarDataList vBarDataList = new VBarDataList();

    Collection<VBarGraphData> dataList = new ArrayList<VBarGraphData>();
    if (jsonFacetResponse == null) {
      logger.info("Solr document list in null");
      return vBarDataList;
    }
    List<Object> userList = jsonFacetResponse.getAll(outerJsonKey);
    if (userList.isEmpty()) {
      return vBarDataList;
    }
    SimpleOrderedMap<Map<String, Object>> userMap = (SimpleOrderedMap<Map<String, Object>>) userList.get(0);
    if (userMap == null) {
      logger.info("No top user details found");
      return vBarDataList;
    }
    List<SimpleOrderedMap> userUsageList = (List<SimpleOrderedMap>) userMap.get("buckets");
    if(userUsageList == null){
      return vBarDataList;
    }
    for (SimpleOrderedMap usageMap : userUsageList) {
      if (usageMap != null) {
        VBarGraphData vBarGraphData = new VBarGraphData();
        String userName = (String) usageMap.get("val");
        if (!StringUtils.isBlank(userName)) {
          vBarGraphData.setName(userName);
        }
        SimpleOrderedMap repoMap = (SimpleOrderedMap) usageMap.get(innerJsonKey);
        List<VNameValue> componetCountList = new ArrayList<VNameValue>();
        List<SimpleOrderedMap> repoUsageList = (List<SimpleOrderedMap>) repoMap.get("buckets");
        if (repoMap != null) {
          for (SimpleOrderedMap repoUsageMap : repoUsageList) {
            VNameValue componetCount = new VNameValue();
            if (repoUsageMap.get("val") != null) {
              componetCount.setName(repoUsageMap.get("val").toString());
            }
            String eventCount = "";
            if (repoUsageMap.get("eventCount") != null) {
              eventCount = repoUsageMap.get("eventCount").toString();
            }
            eventCount = eventCount.replace(".0", "");
            eventCount = eventCount.replace(".00", "");

            componetCount.setValue(eventCount);
            componetCountList.add(componetCount);
          }
          vBarGraphData.setDataCounts(componetCountList);
          dataList.add(vBarGraphData);
        }
      }}
    vBarDataList.setGraphData(dataList);
    logger.info("getting graph data");

    return vBarDataList;
  }
  
  public static HashMap<String, String> sortHashMapByValues(HashMap<String, String> passedMap) {
    if (passedMap == null ) {
      return passedMap;
    }
    HashMap<String, String> sortedMap = new LinkedHashMap<String, String>();
    List<String> mapValues = new ArrayList<String>(passedMap.values());
    HashMap<String, String> invertedKeyValue = new HashMap<String, String>();
    Collections.sort(mapValues, new Comparator<String>() {
      @Override
      public int compare(String s1, String s2) {
        return s1.compareToIgnoreCase(s2);
      }
    });
    Iterator<Entry<String, String>> it = passedMap.entrySet().iterator();
    while (it.hasNext()) {
      @SuppressWarnings("rawtypes")
      Map.Entry pair = (Map.Entry) it.next();
      invertedKeyValue.put("" + pair.getValue(), "" + pair.getKey());
    }

    for (String valueOfKey : mapValues) {
      sortedMap.put(invertedKeyValue.get(valueOfKey), valueOfKey);
    }

    return sortedMap;
  }

}
