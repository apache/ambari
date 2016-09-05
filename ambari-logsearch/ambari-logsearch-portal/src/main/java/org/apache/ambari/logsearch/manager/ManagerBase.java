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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.model.response.LogData;
import org.apache.ambari.logsearch.model.response.LogSearchResponse;
import org.apache.ambari.logsearch.query.model.SearchCriteria;
import org.apache.ambari.logsearch.dao.SolrDaoBase;
import org.apache.ambari.logsearch.query.QueryGeneration;
import org.apache.ambari.logsearch.util.DateUtil;
import org.apache.ambari.logsearch.util.JSONUtil;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.apache.ambari.logsearch.util.SolrUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;

import javax.inject.Inject;

public abstract class ManagerBase<LOG_DATA_TYPE extends LogData, SEARCH_RESPONSE extends LogSearchResponse> extends JsonManagerBase {
  private static final Logger logger = Logger.getLogger(ManagerBase.class);

  @Inject
  protected QueryGeneration queryGenerator;

  public enum LogType {
    SERVICE("Service"),
    AUDIT("Audit");
    
    private String label;
    
    private LogType(String label) {
      this.label = label;
    }
    
    public String getLabel() {
      return label;
    }
  }

  public ManagerBase() {
    super();
  }

  public String getHadoopServiceConfigJSON() {
    StringBuilder result = new StringBuilder("");

    // Get file from resources folder
    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader.getResource("HadoopServiceConfig.json").getFile());

    try (Scanner scanner = new Scanner(file)) {

      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        result.append(line).append("\n");
      }

      scanner.close();

    } catch (IOException e) {
      logger.error("Unable to read HadoopServiceConfig.json", e);
      throw RESTErrorUtil.createRESTException(e.getMessage(), MessageEnums.ERROR_SYSTEM);
    }

    String hadoopServiceConfig = result.toString();
    if (JSONUtil.isJSONValid(hadoopServiceConfig)) {
      return hadoopServiceConfig;
    }
    throw RESTErrorUtil.createRESTException("Improper JSON", MessageEnums.ERROR_SYSTEM);

  }
  
  protected SEARCH_RESPONSE getLastPage(SearchCriteria searchCriteria, String logTimeField, SolrDaoBase solrDoaBase,
                                    SolrQuery lastPageQuery) {
    
    Integer maxRows = searchCriteria.getMaxRows();
    String givenSortType = searchCriteria.getSortType();
    searchCriteria = new SearchCriteria();
    searchCriteria.setSortBy(logTimeField);
    if (givenSortType == null || givenSortType.equals(LogSearchConstants.DESCENDING_ORDER)) {
      lastPageQuery.removeSort(LogSearchConstants.LOGTIME);
      searchCriteria.setSortType(LogSearchConstants.ASCENDING_ORDER);
    } else {
      searchCriteria.setSortType(LogSearchConstants.DESCENDING_ORDER);
    }
    queryGenerator.setSingleSortOrder(lastPageQuery, searchCriteria);


    Long totalLogs = 0l;
    int startIndex = 0;
    int numberOfLogsOnLastPage = 0;
    SEARCH_RESPONSE logResponse = null;
    try {
      SolrUtil.setStart(lastPageQuery, 0);
      SolrUtil.setRowCount(lastPageQuery, maxRows);
      logResponse = getLogAsPaginationProvided(lastPageQuery, solrDoaBase);
      totalLogs = countQuery(lastPageQuery,solrDoaBase);
      startIndex = Integer.parseInt("" + ((totalLogs / maxRows) * maxRows));
      numberOfLogsOnLastPage = Integer.parseInt("" + (totalLogs - startIndex));
      logResponse.setStartIndex(startIndex);
      logResponse.setTotalCount(totalLogs);
      logResponse.setPageSize(maxRows);
      List<LOG_DATA_TYPE> docList = logResponse.getLogList();
      List<LOG_DATA_TYPE> lastPageDocList = new ArrayList<>();
      logResponse.setLogList(lastPageDocList);
      int cnt = 0;
      for(LOG_DATA_TYPE doc:docList){
        if(cnt<numberOfLogsOnLastPage){
          lastPageDocList.add(doc);
        }
        cnt++;
      }
      Collections.reverse(lastPageDocList);

    } catch (SolrException | SolrServerException | IOException | NumberFormatException e) {
      logger.error("Count Query was not executed successfully",e);
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
    return logResponse;
  }

  protected SEARCH_RESPONSE getLogAsPaginationProvided(SolrQuery solrQuery, SolrDaoBase solrDaoBase) {
    try {
      QueryResponse response = solrDaoBase.process(solrQuery);
      SEARCH_RESPONSE logResponse = createLogSearchResponse();
      SolrDocumentList docList = response.getResults();
      List<LOG_DATA_TYPE> serviceLogDataList = convertToSolrBeans(response);
      if (docList != null && !docList.isEmpty()) {
        logResponse.setLogList(serviceLogDataList);
        logResponse.setStartIndex((int) docList.getStart());
        logResponse.setTotalCount(docList.getNumFound());
        Integer rowNumber = solrQuery.getRows();
        if (rowNumber == null) {
          logger.error("No RowNumber was set in solrQuery");
          return createLogSearchResponse();
        }
        logResponse.setPageSize(rowNumber);
      }
      return logResponse;
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }
  
  protected Long countQuery(SolrQuery query,SolrDaoBase solrDaoBase) throws SolrException, SolrServerException, IOException {
    query.setRows(0);
    QueryResponse response = solrDaoBase.process(query);
    if (response == null) {
      return 0l;
    }
    SolrDocumentList docList = response.getResults();
    if (docList == null) {
      return 0l;
    }
    return docList.getNumFound();
  }

  protected String getUnit(String unit) {
    if (StringUtils.isBlank(unit)) {
      unit = "+1HOUR";
    }
    return unit;
  }

  protected String getFrom(String from) {
    if (StringUtils.isBlank(from)) {
      Date date = DateUtil.getTodayFromDate();
      try {
        from = DateUtil.convertGivenDateFormatToSolrDateFormat(date);
      } catch (ParseException e) {
        from = "NOW";
      }
    }
    return from;
  }

  protected String getTo(String to) {
    if (StringUtils.isBlank(to)) {
      to = "NOW";
    }
    return to;
  }

  protected abstract List<LOG_DATA_TYPE> convertToSolrBeans(QueryResponse response);

  protected abstract SEARCH_RESPONSE createLogSearchResponse();
}
