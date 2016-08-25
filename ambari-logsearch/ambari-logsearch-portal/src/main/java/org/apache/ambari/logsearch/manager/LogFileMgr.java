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
package org.apache.ambari.logsearch.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.common.SearchCriteria;
import org.apache.ambari.logsearch.dao.AuditSolrDao;
import org.apache.ambari.logsearch.dao.ServiceLogsSolrDao;
import org.apache.ambari.logsearch.dao.SolrDaoBase;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.apache.ambari.logsearch.util.SolrUtil;
import org.apache.ambari.logsearch.view.VLogFile;
import org.apache.ambari.logsearch.view.VLogFileList;
import org.apache.ambari.logsearch.view.VSolrLogList;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class LogFileMgr extends MgrBase {

  private static final Logger logger = Logger.getLogger(LogFileMgr.class);

  @Autowired
  private ServiceLogsSolrDao serviceLogsSolrDao;
  @Autowired
  private AuditSolrDao auditSolrDao;

  public String searchLogFiles(SearchCriteria searchCriteria) {
    VLogFileList logFileList = new VLogFileList();
    List<VLogFile> logFiles = new ArrayList<VLogFile>();
    String componentName = (String) searchCriteria.getParamValue("component");
    String host = (String) searchCriteria.getParamValue("host");
    int minCount = 1;// to remove zero count facet
    SolrQuery solrQuery = new SolrQuery();
    SolrUtil.setMainQuery(solrQuery, null);
    SolrUtil.setFacetFieldWithMincount(solrQuery, LogSearchConstants.SOLR_PATH, minCount);
    // adding filter
    queryGenerator.setSingleIncludeFilter(solrQuery, LogSearchConstants.SOLR_COMPONENT, componentName);
    queryGenerator.setSingleIncludeFilter(solrQuery, LogSearchConstants.SOLR_HOST, host);
    try {
      String logType = (String) searchCriteria.getParamValue("logType");
      if (StringUtils.isBlank(logType)) {
        logType = LogType.SERVICE.name();// default is service Log
      }
      SolrDaoBase daoMgr = null;
      if (logType.equalsIgnoreCase(LogType.SERVICE.name())) {
        daoMgr = serviceLogsSolrDao;
      } else if (logType.equalsIgnoreCase(LogType.AUDIT.name())) {
        daoMgr = auditSolrDao;
      } else {
        throw RESTErrorUtil.createRESTException(logType + " is not a valid logType", MessageEnums.INVALID_INPUT_DATA);
      }
      QueryResponse queryResponse = daoMgr.process(solrQuery);
      if (queryResponse.getFacetField(LogSearchConstants.SOLR_PATH) != null) {
        FacetField queryFacetField = queryResponse.getFacetField(LogSearchConstants.SOLR_PATH);
        if (queryFacetField != null) {
          List<Count> countList = queryFacetField.getValues();
          for (Count count : countList) {
            VLogFile vLogFile = new VLogFile();
            String filePath = count.getName();
            String fileName = FilenameUtils.getName(filePath);
            vLogFile.setPath(filePath);
            vLogFile.setName(fileName);
            logFiles.add(vLogFile);
          }
        }
      }
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error in solr query  :" + e.getLocalizedMessage() + "\n Query :" + solrQuery.toQueryString(), e.getCause());
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
    logFileList.setLogFiles(logFiles);
    String jsonStr = "";
    jsonStr = convertObjToString(logFileList);

    return jsonStr;
  }

  public String getLogFileTail(SearchCriteria searchCriteria) {
    String host = (String) searchCriteria.getParamValue("host");
    String logFile = (String) searchCriteria.getParamValue("name");
    String component = (String) searchCriteria.getParamValue("component");
    String tailSize = (String) searchCriteria.getParamValue("tailSize");
    if (StringUtils.isBlank(host)) {
      throw RESTErrorUtil.createRESTException("missing Host Name", MessageEnums.ERROR_SYSTEM);
    }
    tailSize = (StringUtils.isBlank(tailSize)) ? "10" : tailSize;
    SolrQuery logFileTailQuery = new SolrQuery();
    try {
      int tail = Integer.parseInt(tailSize);
      tail = tail > 100 ? 100 : tail;
      SolrUtil.setMainQuery(logFileTailQuery, null);
      queryGenerator.setSingleIncludeFilter(logFileTailQuery, LogSearchConstants.SOLR_HOST, host);
      if (!StringUtils.isBlank(logFile)) {
        queryGenerator.setSingleIncludeFilter(logFileTailQuery, LogSearchConstants.SOLR_PATH, SolrUtil.makeSolrSearchString(logFile));
      } else if (!StringUtils.isBlank(component)) {
        queryGenerator.setSingleIncludeFilter(logFileTailQuery, LogSearchConstants.SOLR_COMPONENT, component);
      } else {
        throw RESTErrorUtil.createRESTException("component or logfile parameter must be present", MessageEnums.ERROR_SYSTEM);
      }

      SolrUtil.setRowCount(logFileTailQuery, tail);
      queryGenerator.setSortOrderDefaultServiceLog(logFileTailQuery, new SearchCriteria());
      VSolrLogList solrLogList = getLogAsPaginationProvided(logFileTailQuery, serviceLogsSolrDao);
      return convertObjToString(solrLogList);

    } catch (NumberFormatException ne) {

      throw RESTErrorUtil.createRESTException(ne.getMessage(),
        MessageEnums.ERROR_SYSTEM);

    }
  }
}
