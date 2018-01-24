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

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import org.apache.ambari.logsearch.common.LogType;
import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.common.StatusMessage;
import org.apache.ambari.logsearch.common.LabelFallbackHandler;
import org.apache.ambari.logsearch.conf.UIMappingConfig;
import org.apache.ambari.logsearch.dao.AuditSolrDao;
import org.apache.ambari.logsearch.dao.SolrSchemaFieldDao;
import org.apache.ambari.logsearch.model.metadata.AuditFieldMetadataResponse;
import org.apache.ambari.logsearch.model.metadata.FieldMetadata;
import org.apache.ambari.logsearch.model.request.impl.AuditBarGraphRequest;
import org.apache.ambari.logsearch.model.request.impl.AuditComponentRequest;
import org.apache.ambari.logsearch.model.request.impl.AuditLogRequest;
import org.apache.ambari.logsearch.model.request.impl.AuditServiceLoadRequest;
import org.apache.ambari.logsearch.model.request.impl.TopFieldAuditLogRequest;
import org.apache.ambari.logsearch.model.request.impl.UserExportRequest;
import org.apache.ambari.logsearch.model.response.AuditLogData;
import org.apache.ambari.logsearch.model.response.AuditLogResponse;
import org.apache.ambari.logsearch.model.response.BarGraphDataListResponse;
import org.apache.ambari.logsearch.model.response.LogData;
import org.apache.ambari.logsearch.solr.ResponseDataGenerator;
import org.apache.ambari.logsearch.solr.SolrConstants;
import org.apache.ambari.logsearch.solr.model.SolrAuditLogData;
import org.apache.ambari.logsearch.solr.model.SolrComponentTypeLogData;
import org.apache.ambari.logsearch.util.DownloadUtil;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.apache.ambari.logsearch.util.SolrUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.ambari.logsearch.common.VResponse;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.solr.core.query.SimpleFacetQuery;
import org.springframework.data.solr.core.query.SimpleQuery;

import static org.apache.ambari.logsearch.solr.SolrConstants.AuditLogConstants.AUDIT_COMPONENT;
import static org.apache.ambari.logsearch.solr.SolrConstants.CommonLogConstants.CLUSTER;

@Named
public class AuditLogsManager extends ManagerBase<AuditLogData, AuditLogResponse> {
  private static final Logger logger = Logger.getLogger(AuditLogsManager.class);

  private static final String AUDIT_LOG_TEMPLATE = "audit_log_txt.ftl";

  @Inject
  private AuditSolrDao auditSolrDao;
  @Inject
  private ResponseDataGenerator responseDataGenerator;
  @Inject
  private ConversionService conversionService;
  @Inject
  private Configuration freemarkerConfiguration;
  @Inject
  private SolrSchemaFieldDao solrSchemaFieldDao;
  @Inject
  private UIMappingConfig uiMappingConfig;
  @Inject
  private LabelFallbackHandler labelFallbackHandler;

  public AuditLogResponse getLogs(AuditLogRequest request) {
    String event = "/audit/logs";
    SimpleQuery solrQuery = conversionService.convert(request, SimpleQuery.class);
    if (request.isLastPage()) {
      return getLastPage(auditSolrDao, solrQuery, event);
    } else {
      AuditLogResponse response = getLogAsPaginationProvided(solrQuery, auditSolrDao, event);
      if (response.getTotalCount() > 0 && CollectionUtils.isEmpty(response.getLogList())) {
        request.setLastPage(true);
        solrQuery = conversionService.convert(request, SimpleQuery.class);
        AuditLogResponse lastResponse = getLastPage(auditSolrDao, solrQuery, event);
        if (lastResponse != null){
          response = lastResponse;
        }
      }
      return response;
    }
  }

  private List<LogData> getComponents(AuditComponentRequest request) {
    SimpleFacetQuery facetQuery = conversionService.convert(request, SimpleFacetQuery.class);
    List<LogData> docList = new ArrayList<>();
    QueryResponse queryResponse = auditSolrDao.process(facetQuery);
    List<Count> componentsCount = responseDataGenerator.generateCount(queryResponse);

    for (Count component : componentsCount) {
      SolrComponentTypeLogData logData = new SolrComponentTypeLogData();
      logData.setType(component.getName());
      docList.add(logData);
    }
    return docList;
  }

  public Map<String, String> getAuditComponents(String clusters) {
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("*:*");
    solrQuery.setRows(0);
    SolrUtil.setFacetField(solrQuery, AUDIT_COMPONENT);
    QueryResponse queryResponse = auditSolrDao.process(solrQuery);
    return responseDataGenerator.generateComponentMetadata(queryResponse, AUDIT_COMPONENT,
      uiMappingConfig.getAuditComponentLabels());
  }

  public BarGraphDataListResponse getAuditBarGraphData(AuditBarGraphRequest request) {
    SolrQuery solrQuery = conversionService.convert(request, SolrQuery.class);
    QueryResponse response = auditSolrDao.process(solrQuery);
    return responseDataGenerator.generateBarGraphDataResponseWithRanges(response, SolrConstants.AuditLogConstants.AUDIT_COMPONENT, true);
  }

  public BarGraphDataListResponse topResources(TopFieldAuditLogRequest request) {
    SimpleFacetQuery facetQuery = conversionService.convert(request, SimpleFacetQuery.class);
    QueryResponse queryResponse = auditSolrDao.process(facetQuery);
    return responseDataGenerator.generateSecondLevelBarGraphDataResponse(queryResponse, 0);
  }

  public AuditFieldMetadataResponse getAuditLogSchemaMetadata() {
    Map<String, List<FieldMetadata>> overrides = new HashMap<>();
    List<FieldMetadata> defaults = new ArrayList<>();

    Map<String, String> schemaFieldsMap = solrSchemaFieldDao.getSchemaFieldNameMap(LogType.AUDIT);

    Map<String, Map<String, String>> fieldLabelMap = uiMappingConfig.getMergedAuditFieldLabelMap();
    Map<String, List<String>> fieldVisibleeMap = uiMappingConfig.getMergedAuditFieldVisibleMap();
    Map<String, List<String>> fieldExcludeMap = uiMappingConfig.getMergedAuditFieldExcludeMap();
    Map<String, List<String>> fieldFilterableExcludeMap = uiMappingConfig.getMergedAuditFieldFilterableExcludesMap();

    Map<String, String> commonFieldLabels = uiMappingConfig.getAuditFieldCommonLabels();
    List<String> commonFieldVisibleList = uiMappingConfig.getAuditFieldCommonVisibleList();
    List<String> commonFieldExcludeList = uiMappingConfig.getAuditFieldCommonExcludeList();
    List<String> commonFieldFilterableExcludeList = uiMappingConfig.getAuditFieldCommonExcludeList();

    Map<String, String> componentLabels = uiMappingConfig.getAuditComponentLabels();

    for (Map.Entry<String, String> component : componentLabels.entrySet()) {
      String componentName = component.getKey();
      List<FieldMetadata> auditComponentFieldMetadataList = new ArrayList<>();
      for (Map.Entry<String, String> fieldEntry : schemaFieldsMap.entrySet()) {
        String field = fieldEntry.getKey();
        if (!fieldExcludeMap.containsKey(field) && !commonFieldExcludeList.contains(field)) {
          String fieldLabel = fieldLabelMap.get(componentName) != null ? fieldLabelMap.get(componentName).get(field): null;
          String fallbackedFieldLabel = labelFallbackHandler.fallbackIfRequired(field, fieldLabel,
            true, true, true,
            uiMappingConfig.getAuditFieldFallbackPrefixes());

          Boolean excludeFromFilter = fieldFilterableExcludeMap.get(componentName) != null && fieldFilterableExcludeMap.get(componentName).contains(field);
          Boolean visible = fieldVisibleeMap.get(componentName) != null && fieldVisibleeMap.get(componentName).contains(field);
          auditComponentFieldMetadataList.add(new FieldMetadata(field, fallbackedFieldLabel, !excludeFromFilter, visible));
        }
        overrides.put(componentName, auditComponentFieldMetadataList);
      }
    }

    for (Map.Entry<String, String> fieldEntry : schemaFieldsMap.entrySet()) {
      String field = fieldEntry.getKey();
      if (!commonFieldExcludeList.contains(field)) {
        String fieldLabel = commonFieldLabels.get(field);
        Boolean visible = commonFieldVisibleList.contains(field);
        Boolean excludeFromFilter = commonFieldFilterableExcludeList.contains(field);
        String fallbackedFieldLabel = labelFallbackHandler.fallbackIfRequired(field, fieldLabel,
          true, true, true,
          uiMappingConfig.getAuditFieldFallbackPrefixes());
        defaults.add(new FieldMetadata(field, fallbackedFieldLabel, !excludeFromFilter, visible));
      }
    }
    return new AuditFieldMetadataResponse(defaults, overrides);
  }

  public BarGraphDataListResponse getServiceLoad(AuditServiceLoadRequest request) {
    SimpleFacetQuery facetQuery = conversionService.convert(request, SimpleFacetQuery.class);
    QueryResponse response = auditSolrDao.process(facetQuery);
    return responseDataGenerator.generateBarGraphFromFieldFacet(response, AUDIT_COMPONENT);
  }

  public Response export(UserExportRequest request) throws TemplateException {
    String startTime = request.getFrom();
    String endTime = request.getTo();
    SimpleFacetQuery facetQuery = conversionService.convert(request, SimpleFacetQuery.class);

    startTime = startTime == null ? "" : startTime;
    endTime = endTime == null ? "" : "_" + endTime;

    String dataFormat = request.getFormat();

    FileOutputStream fis = null;
    try {
      QueryResponse queryResponse = auditSolrDao.process(facetQuery);
      if (queryResponse == null) {
        VResponse response = new VResponse();
        response.setMsgDesc("Query was not able to execute " + facetQuery);
        throw RESTErrorUtil.createRESTException(response);
      }
      BarGraphDataListResponse vBarUserDataList = responseDataGenerator.generateSecondLevelBarGraphDataResponse(queryResponse, 0);
      BarGraphDataListResponse vBarResourceDataList = responseDataGenerator.generateSecondLevelBarGraphDataResponse(queryResponse, 1);
      String data = "";
      if ("text".equals(dataFormat)) {
        StringWriter stringWriter = new StringWriter();
        Template template = freemarkerConfiguration.getTemplate(AUDIT_LOG_TEMPLATE);
        Map<String, Object> models = new HashMap<>();
        DownloadUtil.fillUserResourcesModel(models, vBarUserDataList, vBarResourceDataList);
        template.process(models, stringWriter);
        data = stringWriter.toString();

      } else {
        data = "{" + convertObjToString(vBarUserDataList) + "," + convertObjToString(vBarResourceDataList) + "}";
        dataFormat = "json";
      }
      String fileName = "Users_Resource" + startTime + endTime + ".";
      File file = File.createTempFile(fileName, dataFormat);
      fis = new FileOutputStream(file);
      fis.write(data.getBytes());
      return Response
        .ok(file, MediaType.APPLICATION_OCTET_STREAM)
        .header("Content-Disposition", "attachment;filename=" + fileName + dataFormat)
        .build();

    } catch (IOException e) {
      logger.error("Error during download file (audit log) " + e);
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
        }
      }
    }
  }

  @Override
  protected List<AuditLogData> convertToSolrBeans(QueryResponse response) {
    List<SolrAuditLogData> solrAuditLogData = response.getBeans(SolrAuditLogData.class);
    List<AuditLogData> auditLogData = new ArrayList<>();
    auditLogData.addAll( solrAuditLogData );
    return auditLogData;
  }

  @Override
  protected AuditLogResponse createLogSearchResponse() {
    return new AuditLogResponse();
  }

  public StatusMessage deleteLogs(AuditLogRequest request) {
    SimpleQuery solrQuery = conversionService.convert(request, SimpleQuery.class);
    UpdateResponse updateResponse = auditSolrDao.deleteByQuery(solrQuery, "/audit/logs");
    return new StatusMessage(updateResponse.getStatus());
  }

  public List<String> getClusters() {
    return getClusters(auditSolrDao, CLUSTER, "/audit/logs/clusters");
  }
}
