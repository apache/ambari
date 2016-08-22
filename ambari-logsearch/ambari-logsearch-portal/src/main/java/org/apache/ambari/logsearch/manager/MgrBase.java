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
import java.util.Collections;
import java.util.Date;
import java.util.Scanner;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.common.SearchCriteria;
import org.apache.ambari.logsearch.dao.SolrDaoBase;
import org.apache.ambari.logsearch.query.QueryGeneration;
import org.apache.ambari.logsearch.util.DateUtil;
import org.apache.ambari.logsearch.util.JSONUtil;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.apache.ambari.logsearch.util.SolrUtil;
import org.apache.ambari.logsearch.view.VSolrLogList;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class MgrBase {
  private static final Logger logger = Logger.getLogger(MgrBase.class);

  @Autowired
  protected SolrUtil solrUtil;

  @Autowired
  protected JSONUtil jsonUtil;

  @Autowired
  protected QueryGeneration queryGenerator;

  @Autowired
  protected RESTErrorUtil restErrorUtil;

  @Autowired
  protected DateUtil dateUtil;

  private JsonSerializer<Date> jsonDateSerialiazer = null;
  private JsonDeserializer<Date> jsonDateDeserialiazer = null;

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

  public MgrBase() {
    jsonDateSerialiazer = new JsonSerializer<Date>() {

      @Override
      public JsonElement serialize(Date paramT, java.lang.reflect.Type paramType, JsonSerializationContext paramJsonSerializationContext) {
        return paramT == null ? null : new JsonPrimitive(paramT.getTime());
      }
    };

    jsonDateDeserialiazer = new JsonDeserializer<Date>() {

      @Override
      public Date deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context)
          throws JsonParseException {
        return json == null ? null : new Date(json.getAsLong());
      }

    };
  }

  protected String convertObjToString(Object obj) {
    if (obj == null) {
      return "";
    }

    Gson gson = new GsonBuilder()
        .registerTypeAdapter(Date.class, jsonDateSerialiazer)
        .registerTypeAdapter(Date.class, jsonDateDeserialiazer).create();

    return gson.toJson(obj);
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
      throw restErrorUtil.createRESTException(e.getMessage(), MessageEnums.ERROR_SYSTEM);
    }

    String hadoopServiceConfig = result.toString();
    if (jsonUtil.isJSONValid(hadoopServiceConfig)) {
      return hadoopServiceConfig;
    }
    throw restErrorUtil.createRESTException("Improper JSON", MessageEnums.ERROR_SYSTEM);

  }
  
  protected VSolrLogList getLastPage(SearchCriteria searchCriteria, String logTimeField, SolrDaoBase solrDoaBase,
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
    VSolrLogList collection = null;
    try {
      queryGenerator.setStart(lastPageQuery, 0);
      queryGenerator.setRowCount(lastPageQuery, maxRows);
      collection = getLogAsPaginationProvided(lastPageQuery, solrDoaBase);
      totalLogs = countQuery(lastPageQuery,solrDoaBase);
      if(maxRows != null){
        startIndex = Integer.parseInt("" + ((totalLogs/maxRows) * maxRows));
        numberOfLogsOnLastPage = Integer.parseInt("" + (totalLogs-startIndex));
      }
      collection.setStartIndex(startIndex);
      collection.setTotalCount(totalLogs);
      collection.setPageSize(maxRows);
      SolrDocumentList docList = collection.getList();
      SolrDocumentList lastPageDocList = new SolrDocumentList();
      collection.setSolrDocuments(lastPageDocList);
      int cnt = 0;
      for(SolrDocument doc:docList){
        if(cnt<numberOfLogsOnLastPage){
          lastPageDocList.add(doc);
        }
        cnt++;
      }
      Collections.reverse(lastPageDocList);

    } catch (SolrException | SolrServerException | IOException | NumberFormatException e) {
      logger.error("Count Query was not executed successfully",e);
      throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
    return collection;
  }

  protected VSolrLogList getLogAsPaginationProvided(SolrQuery solrQuery, SolrDaoBase solrDaoBase) {
    try {
      QueryResponse response = solrDaoBase.process(solrQuery);
      VSolrLogList collection = new VSolrLogList();
      SolrDocumentList docList = response.getResults();
      if (docList != null && !docList.isEmpty()) {
        collection.setSolrDocuments(docList);
        collection.setStartIndex((int) docList.getStart());
        collection.setTotalCount(docList.getNumFound());
        Integer rowNumber = solrQuery.getRows();
        if (rowNumber == null) {
          logger.error("No RowNumber was set in solrQuery");
          return new VSolrLogList();
        }
        collection.setPageSize(rowNumber);
      }
      return collection;
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
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
      Date date =  dateUtil.getTodayFromDate();
      try {
        from = dateUtil.convertGivenDateFormatToSolrDateFormat(date);
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
}
