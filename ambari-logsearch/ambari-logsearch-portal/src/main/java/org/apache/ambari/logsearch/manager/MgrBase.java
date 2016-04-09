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
import java.util.Date;
import java.util.Scanner;

import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.dao.SolrDaoBase;
import org.apache.ambari.logsearch.query.QueryGeneration;
import org.apache.ambari.logsearch.util.JSONUtil;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.apache.ambari.logsearch.util.SolrUtil;
import org.apache.ambari.logsearch.util.StringUtil;
import org.apache.ambari.logsearch.view.VSolrLogList;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
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
  static private Logger logger = Logger.getLogger(MgrBase.class);

  @Autowired
  SolrUtil solrUtil;

  @Autowired
  JSONUtil jsonUtil;

  @Autowired
  QueryGeneration queryGenrator;

  @Autowired
  StringUtil stringUtil;

  @Autowired
  RESTErrorUtil restErrorUtil;

  JsonSerializer<Date> jsonDateSerialiazer = null;
  JsonDeserializer<Date> jsonDateDeserialiazer = null;

  public MgrBase() {
    jsonDateSerialiazer = new JsonSerializer<Date>() {

      @Override
      public JsonElement serialize(Date paramT,
                                   java.lang.reflect.Type paramType,
                                   JsonSerializationContext paramJsonSerializationContext) {

        return paramT == null ? null : new JsonPrimitive(paramT.getTime());
      }
    };

    jsonDateDeserialiazer = new JsonDeserializer<Date>() {

      @Override
      public Date deserialize(JsonElement json,
                              java.lang.reflect.Type typeOfT,
                              JsonDeserializationContext context) throws JsonParseException {
        return json == null ? null : new Date(json.getAsLong());
      }

    };
  }

  public String convertObjToString(Object obj) throws IOException {
    if (obj == null) {
      return "";
    }
    /*ObjectMapper mapper = new ObjectMapper();
    ObjectWriter w = mapper.writerWithDefaultPrettyPrinter();
    return mapper.writeValueAsString(obj);*/

    Gson gson = new GsonBuilder()
      .registerTypeAdapter(Date.class, jsonDateSerialiazer)
      .registerTypeAdapter(Date.class, jsonDateDeserialiazer).create();

    return gson.toJson(obj);
  }


  public String getHadoopServiceConfigJSON() {
    StringBuilder result = new StringBuilder("");

    // Get file from resources folder
    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader
      .getResource("HadoopServiceConfig.json").getFile());

    try (Scanner scanner = new Scanner(file)) {

      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        result.append(line).append("\n");
      }

      scanner.close();

    } catch (IOException e) {
      logger.error("Unable to read HadoopServiceConfig.json", e);
      throw restErrorUtil.createRESTException(e.getMessage(),
        MessageEnums.ERROR_SYSTEM);
    }

    String hadoopServiceConfig = result.toString();
    if (jsonUtil.isJSONValid(hadoopServiceConfig))
      return hadoopServiceConfig;
    throw restErrorUtil.createRESTException("Improper JSON",
      MessageEnums.ERROR_SYSTEM);

  }

  public VSolrLogList getLogAsPaginationProvided(SolrQuery solrQuery, SolrDaoBase solrDaoBase) {
    try {
      QueryResponse response = solrDaoBase.process(solrQuery);
      SolrDocumentList docList = response.getResults();
      VSolrLogList collection = new VSolrLogList(docList);
      collection.setStartIndex((int) docList.getStart());
      collection.setTotalCount(docList.getNumFound());
      Integer rowNumber = solrQuery.getRows();
      if (rowNumber == null) {
        logger.error("No RowNumber was set in solrQuery");
        return new VSolrLogList();
      }
      collection.setPageSize(rowNumber);
      return collection;
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw restErrorUtil.createRESTException(e.getMessage(),
        MessageEnums.ERROR_SYSTEM);
    }

  }

}
