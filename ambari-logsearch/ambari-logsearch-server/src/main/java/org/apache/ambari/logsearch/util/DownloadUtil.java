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
import java.util.List;
import java.util.Map;

import com.google.common.base.Splitter;
import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.model.request.impl.ServiceLogExportRequest;
import org.apache.ambari.logsearch.model.response.BarGraphData;
import org.apache.ambari.logsearch.model.response.BarGraphDataListResponse;
import org.apache.ambari.logsearch.model.response.NameValueData;
import org.apache.ambari.logsearch.model.response.TemplateData;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.LINE_NUMBER;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.LOGTIME;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.LOG_MESSAGE;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.LEVEL;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.HOST;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.COMPONENT;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.LOGGER_NAME;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.THREAD_NAME;
import static org.apache.ambari.logsearch.solr.SolrConstants.CommonLogConstants.FILE;


public class DownloadUtil {

  private DownloadUtil() {
    throw new UnsupportedOperationException();
  }

  public static void fillModelsForLogFile(SolrDocumentList docList, Map<String, Object> models, ServiceLogExportRequest request,
                                          String format, String from, String to) {
    long numLogs = docList.getNumFound();
    List<String> hosts = new ArrayList<>();
    List<String> components = new ArrayList<>();
    List<String> levels = new ArrayList<>();
    List<TemplateData> logData = new ArrayList<>();
    for (SolrDocument doc : docList) {
      if (doc != null) {
        String hostname = (String) doc.getFieldValue(HOST);
        String comp = (String) doc.getFieldValue(COMPONENT);
        String level = (String) doc.getFieldValue(LEVEL);

        if (!hosts.contains(hostname)) {
          hosts.add(hostname);
        }

        if (!components.contains(comp)) {
          components.add(comp);
        }

        if (!levels.contains(level)) {
          levels.add(level);
        }

        StringBuffer textToWrite = new StringBuffer();

        if (doc.getFieldValue(LOGTIME) != null) {
          textToWrite.append(doc.getFieldValue(LOGTIME).toString() + " ");
        }
        if (doc.getFieldValue(LEVEL) != null) {
          textToWrite.append(doc.getFieldValue(LEVEL).toString()).append(" ");
        }
        if (doc.getFieldValue(THREAD_NAME) != null) {
          textToWrite.append(doc.getFieldValue(THREAD_NAME).toString().trim()).append(" ");
        }
        if (doc.getFieldValue(LOGGER_NAME) != null) {
          textToWrite.append(doc.getFieldValue(LOGGER_NAME).toString().trim()).append(" ");
        }
        if (doc.getFieldValue(FILE) != null && doc.getFieldValue(LINE_NUMBER) != null) {
          textToWrite
            .append(doc.getFieldValue(FILE).toString())
            .append(":")
            .append(doc.getFieldValue(LINE_NUMBER).toString())
            .append(" ");
        }
        if (doc.getFieldValue(LOG_MESSAGE) != null) {
          textToWrite.append("- ")
            .append(doc.getFieldValue(LOG_MESSAGE).toString());
        }
        logData.add(new TemplateData((textToWrite.toString())));
      }
    }
    models.put("numberOfLogs", numLogs);
    models.put("logs", logData);
    models.put("hosts", "[ " + StringUtils.join(hosts, " ; ") + " ]");
    models.put("components", "[ " + StringUtils.join(components, " ; ") + " ]");
    models.put("format", format);
    models.put("from", from);
    models.put("levels", StringUtils.join(levels, ", "));
    models.put("to", to);
    String includeString = request.getIncludeMessage();
    if (StringUtils.isBlank(includeString)) {
      includeString = "\"\"";
    } else {
      List<String> include = Splitter.on(request.getIncludeMessage()).splitToList(LogSearchConstants.I_E_SEPRATOR);
      includeString = "\"" + StringUtils.join(include, "\", \"") + "\"";
    }
    models.put("iString", includeString);

    String excludeString = request.getExcludeMessage();
    if (StringUtils.isBlank(excludeString)) {
      excludeString = "\"\"";
    } else {
      List<String> exclude = Splitter.on(request.getExcludeMessage()).splitToList(LogSearchConstants.I_E_SEPRATOR);
      excludeString = "\"" + StringUtils.join(exclude, "\", \"") + "\"";
    }
    models.put("eString", excludeString);
  }

  public static void fillUserResourcesModel(Map<String, Object> models, BarGraphDataListResponse vBarUserDataList, BarGraphDataListResponse vBarResourceDataList) {
    List<TemplateData> usersDataList = new ArrayList<>();
    List<TemplateData> resourceDataList = new ArrayList<>();
    Collection<BarGraphData> tableUserData = vBarUserDataList.getGraphData();
    for (BarGraphData graphData : tableUserData) {
      String userName = graphData.getName().length() > 45 ? graphData.getName().substring(0, 45) : graphData.getName();
      Collection<NameValueData> vnameValueList = graphData.getDataCount();
      usersDataList.add(new TemplateData(appendNameValueData(addBlank(userName), vnameValueList)));
    }
    Collection<BarGraphData> tableResourceData = vBarResourceDataList.getGraphData();
    for (BarGraphData graphData : tableResourceData) {
      String resourceName = graphData.getName().length() > 45 ? graphData.getName().substring(0, 45) : graphData.getName();
      Collection<NameValueData> vnameValueList = graphData.getDataCount();
      resourceDataList.add(new TemplateData(appendNameValueData(addBlank(resourceName), vnameValueList)));
    }
    models.put("users", usersDataList);
    models.put("resources", resourceDataList);
    models.put("usersSummary", vBarUserDataList.getGraphData().size());
    models.put("resourcesSummary", vBarResourceDataList.getGraphData().size());
  }

  private static String appendNameValueData(String data, Collection<NameValueData> vnameValueList) {
    int count = 0;
    String blank = "";
    for (NameValueData vNameValue : vnameValueList) {
      data += blank + vNameValue.getName() + " " + vNameValue.getValue();
      if (count == 0)
        blank = addBlank(blank);
      count++;
    }
    return data;
  }

  private static String addBlank(String field) {
    int blanks = 50;
    int strSize = field.length();
    String fieldWithBlank = field;
    for (int i = 0; i < blanks - strSize; i++) {
      fieldWithBlank += " ";
    }
    return fieldWithBlank;
  }
}
