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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import org.apache.ambari.logsearch.common.LabelFallbackHandler;
import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.LogType;
import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.common.StatusMessage;
import org.apache.ambari.logsearch.conf.UIMappingConfig;
import org.apache.ambari.logsearch.dao.ServiceLogsSolrDao;
import org.apache.ambari.logsearch.dao.SolrSchemaFieldDao;
import org.apache.ambari.logsearch.model.metadata.FieldMetadata;
import org.apache.ambari.logsearch.model.metadata.ServiceComponentMetadataWrapper;
import org.apache.ambari.logsearch.model.request.impl.HostLogFilesRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceAnyGraphRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceGraphRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceLogAggregatedInfoRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceLogComponentHostRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceLogComponentLevelRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceLogExportRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceLogHostComponentRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceLogLevelCountRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceLogRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceLogTruncatedRequest;
import org.apache.ambari.logsearch.model.response.BarGraphDataListResponse;
import org.apache.ambari.logsearch.model.response.CountDataListResponse;
import org.apache.ambari.logsearch.model.response.GraphDataListResponse;
import org.apache.ambari.logsearch.model.response.GroupListResponse;
import org.apache.ambari.logsearch.model.response.HostLogFilesResponse;
import org.apache.ambari.logsearch.model.response.LogData;
import org.apache.ambari.logsearch.model.response.LogListResponse;
import org.apache.ambari.logsearch.model.response.NameValueDataListResponse;
import org.apache.ambari.logsearch.model.response.NodeListResponse;
import org.apache.ambari.logsearch.model.response.ServiceLogData;
import org.apache.ambari.logsearch.model.response.ServiceLogResponse;
import org.apache.ambari.logsearch.converter.BaseServiceLogRequestQueryConverter;
import org.apache.ambari.logsearch.converter.ServiceLogTruncatedRequestQueryConverter;
import org.apache.ambari.logsearch.solr.ResponseDataGenerator;
import org.apache.ambari.logsearch.solr.model.SolrComponentTypeLogData;
import org.apache.ambari.logsearch.solr.model.SolrHostLogData;
import org.apache.ambari.logsearch.solr.model.SolrServiceLogData;
import org.apache.ambari.logsearch.util.DownloadUtil;
import org.apache.ambari.logsearch.util.DateUtil;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.apache.ambari.logsearch.util.SolrUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.solr.core.DefaultQueryParser;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleFacetQuery;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;

import static org.apache.ambari.logsearch.solr.SolrConstants.CommonLogConstants.CLUSTER;
import static org.apache.ambari.logsearch.solr.SolrConstants.CommonLogConstants.ID;
import static org.apache.ambari.logsearch.solr.SolrConstants.CommonLogConstants.SEQUENCE_ID;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.COMPONENT;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.HOST;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.KEY_LOG_MESSAGE;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.LEVEL;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.LOGTIME;

@Named
public class ServiceLogsManager extends ManagerBase<ServiceLogData, ServiceLogResponse> {
  private static final Logger logger = Logger.getLogger(ServiceLogsManager.class);

  private static final String SERVICE_LOG_TEMPLATE = "service_log_txt.ftl";

  @Inject
  private ServiceLogsSolrDao serviceLogsSolrDao;
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

  public ServiceLogResponse searchLogs(ServiceLogRequest request) {
    String event = "/service/logs";
    String keyword = request.getKeyWord();
    Boolean isLastPage = request.isLastPage();
    SimpleQuery solrQuery = conversionService.convert(request, SimpleQuery.class);
    if (StringUtils.isNotBlank(keyword)) {
      try {
        return (ServiceLogResponse) getPageByKeyword(request, event);
      } catch (SolrException | SolrServerException e) {
        logger.error("Error while getting keyword=" + keyword, e);
        throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
      }
    } else if (isLastPage) {
      ServiceLogResponse logResponse = getLastPage(serviceLogsSolrDao, solrQuery, event);
      if (logResponse == null){
        logResponse = new ServiceLogResponse();
      }
      return logResponse;
    } else {
      ServiceLogResponse response = getLogAsPaginationProvided(solrQuery, serviceLogsSolrDao, event);
      if (response.getTotalCount() > 0 && CollectionUtils.isEmpty(response.getLogList())) {
        request.setLastPage(true);
        solrQuery = conversionService.convert(request, SimpleQuery.class);
        ServiceLogResponse lastResponse = getLastPage(serviceLogsSolrDao, solrQuery, event);
        if (lastResponse != null){
          response = lastResponse;
        }
      }
      return response;
    }
  }

  public GroupListResponse getHosts(String clusters) {
    return getFields(HOST, clusters, SolrHostLogData.class);
  }

  public GraphDataListResponse getAggregatedInfo(ServiceLogAggregatedInfoRequest request) {
    SimpleQuery solrDataQuery = new BaseServiceLogRequestQueryConverter().convert(request);
    SolrQuery solrQuery = new DefaultQueryParser().doConstructSolrQuery(solrDataQuery);
    String hierarchy = String.format("%s,%s,%s", HOST, COMPONENT, LEVEL);
    solrQuery.setQuery("*:*");
    SolrUtil.setFacetPivot(solrQuery, 1, hierarchy);
    QueryResponse response = serviceLogsSolrDao.process(solrQuery);
    return responseDataGenerator.generateSimpleGraphResponse(response, hierarchy);
  }

  public CountDataListResponse getFieldCount(String field, String clusters) {
    SimpleFacetQuery facetQuery = conversionService.convert(field, SimpleFacetQuery.class);
    if (StringUtils.isEmpty(clusters)) {
      List<String> clusterFilterList = Splitter.on(",").splitToList(clusters);
      facetQuery.addFilterQuery(new SimpleFilterQuery(new Criteria(CLUSTER).in(clusterFilterList)));
    }
    return responseDataGenerator.generateCountResponseByField(serviceLogsSolrDao.process(facetQuery), field);
  }

  public CountDataListResponse getComponentsCount(String clusters) {
    return getFieldCount(COMPONENT, clusters);
  }

  public CountDataListResponse getHostsCount(String clusters) {
    return getFieldCount(HOST, clusters);
  }

  public NodeListResponse getTreeExtension(ServiceLogHostComponentRequest request) {
    SimpleFacetQuery facetQuery = conversionService.convert(request, SimpleFacetQuery.class);
    SolrQuery solrQuery = new DefaultQueryParser().doConstructSolrQuery(facetQuery);
    String hostName = request.getHostName() == null ? "" : request.getHostName();
    if (StringUtils.isNotBlank(hostName)){
      solrQuery.addFilterQuery(String.format("%s:*%s*", HOST, hostName));
    }
    QueryResponse response = serviceLogsSolrDao.process(solrQuery, "/service/logs/tree");
    String firstHierarchy = String.format("%s,%s,%s", HOST, COMPONENT, LEVEL);
    String secondHierarchy = String.format("%s,%s", HOST, LEVEL);
    return responseDataGenerator.generateServiceNodeTreeFromFacetResponse(response, firstHierarchy, secondHierarchy,
      LogSearchConstants.HOST, LogSearchConstants.COMPONENT);
  }

  public NodeListResponse getHostListByComponent(ServiceLogComponentHostRequest request) {
    SimpleFacetQuery facetQuery = conversionService.convert(request, SimpleFacetQuery.class);
    SolrQuery solrQuery = new DefaultQueryParser().doConstructSolrQuery(facetQuery);
    solrQuery.setFacetSort(request.getSortBy() == null ? HOST: request.getSortBy());

    NodeListResponse list = new NodeListResponse();
    String componentName = request.getComponentName() == null ? "" : request.getComponentName();
    if (StringUtils.isNotBlank(componentName)){
      solrQuery.addFilterQuery(COMPONENT + ":"
        + componentName);
      QueryResponse response = serviceLogsSolrDao.process(solrQuery, "/service/logs/hosts/components");
      String firstHierarchy = String.format("%s,%s,%s", COMPONENT, HOST, LEVEL);
      String secondHierarchy = String.format("%s,%s", COMPONENT, LEVEL);
      return responseDataGenerator.generateServiceNodeTreeFromFacetResponse(response, firstHierarchy, secondHierarchy,
        LogSearchConstants.COMPONENT, LogSearchConstants.HOST);
    } else {
      return list;
    }
  }

  public NameValueDataListResponse getLogsLevelCount(ServiceLogLevelCountRequest request) {
    SimpleFacetQuery facetQuery = conversionService.convert(request, SimpleFacetQuery.class);
    QueryResponse response = serviceLogsSolrDao.process(facetQuery, "/service/logs/levels/counts");
    return responseDataGenerator.getNameValueDataListResponseWithDefaults(response, LogSearchConstants.SUPPORTED_LOG_LEVELS, false);
  }

  public BarGraphDataListResponse getHistogramData(ServiceGraphRequest request) {
    SolrQuery solrQuery = conversionService.convert(request, SolrQuery.class);
    QueryResponse response = serviceLogsSolrDao.process(solrQuery, "/service/logs/histogram");
    return responseDataGenerator.generateBarGraphDataResponseWithRanges(response, LEVEL, true);
  }

  public LogListResponse<ServiceLogData> getPageByKeyword(ServiceLogRequest request, String event)
    throws SolrServerException {
    String defaultChoice = "0";
    String keyword = request.getKeyWord();
    if (StringUtils.isBlank(keyword)) {
      throw RESTErrorUtil.createRESTException("Keyword was not given", MessageEnums.DATA_NOT_FOUND);
    }

    boolean isNext = !defaultChoice.equals(request.getKeywordType()); // 1 is next, 0 is previous
    return getPageForKeywordByType(request, keyword, isNext, event);
  }

  private LogListResponse<ServiceLogData> getPageForKeywordByType(ServiceLogRequest request, String keyword, boolean isNext, String event) {
    String fromDate = request.getFrom(); // store start & end dates
    String toDate = request.getTo();
    boolean timeAscending = LogSearchConstants.ASCENDING_ORDER.equals(request.getSortType());

    int currentPageNumber = Integer.parseInt(request.getPage());
    int maxRows = Integer.parseInt(request.getPageSize());
    Date logDate = getDocDateFromNextOrLastPage(request, keyword, isNext, currentPageNumber, maxRows);
    if (logDate == null) {
      throw RESTErrorUtil.createRESTException("The keyword " + "\"" + keyword + "\"" + " was not found", MessageEnums.ERROR_SYSTEM);
    }

    String nextOrPreviousPageDate = DateUtil.convertDateWithMillisecondsToSolrDate(logDate);
    SolrServiceLogData firstKeywordLog = getNextHitForKeyword(request, keyword, isNext, event, timeAscending, nextOrPreviousPageDate);

    long keywordSeqNum = firstKeywordLog.getSeqNum();
    String keywordLogtime = DateUtil.convertDateWithMillisecondsToSolrDate(firstKeywordLog.getLogTime());

    long numberOfDateDuplicates = countNumberOfDuplicates(request, isNext, keywordSeqNum, keywordLogtime);

    long numberOfLogsUntilFound = getNumberOfLogsUntilFound(request, fromDate, toDate, timeAscending, keywordLogtime, numberOfDateDuplicates);
    int start = (int) ((numberOfLogsUntilFound / maxRows));

    request.setFrom(fromDate);
    request.setTo(toDate);
    request.setPage(String.valueOf(start));
    SolrQuery keywordNextPageQuery = new DefaultQueryParser().doConstructSolrQuery(conversionService.convert(request, SimpleQuery.class));
    return getLogAsPaginationProvided(keywordNextPageQuery, serviceLogsSolrDao, event);
  }

  private Long getNumberOfLogsUntilFound(ServiceLogRequest request, String fromDate, String toDate, boolean timeAscending,
                                         String keywordLogtime, long numberOfDateDuplicates) {
    if (!timeAscending) {
      request.setTo(toDate);
      request.setFrom(keywordLogtime);
    } else {
      request.setTo(keywordLogtime);
      request.setFrom(fromDate);
    }
    SimpleQuery rangeQuery = conversionService.convert(request, SimpleQuery.class);
    return serviceLogsSolrDao.count(rangeQuery) - numberOfDateDuplicates;
  }

  private long countNumberOfDuplicates(ServiceLogRequest request, boolean isNext, long keywordSeqNum, String keywordLogtime) {
    request.setFrom(keywordLogtime);
    request.setTo(keywordLogtime);
    SimpleQuery duplicationsQuery = conversionService.convert(request, SimpleQuery.class);
    if (isNext) {
      duplicationsQuery.addFilterQuery(new SimpleFilterQuery(new SimpleStringCriteria(String.format("%s:[* TO %d]", SEQUENCE_ID, keywordSeqNum - 1))));
    } else {
      duplicationsQuery.addFilterQuery(new SimpleFilterQuery(new SimpleStringCriteria(String.format("%s:[%d TO *]", SEQUENCE_ID, keywordSeqNum + 1))));
    }
    return serviceLogsSolrDao.count(duplicationsQuery);
  }

  private SolrServiceLogData getNextHitForKeyword(ServiceLogRequest request, String keyword, boolean isNext, String event, boolean timeAscending, String nextOrPreviousPageDate) {
    if (hasNextOrAscOrder(isNext, timeAscending)) {
      request.setTo(nextOrPreviousPageDate);
    } else {
      request.setFrom(nextOrPreviousPageDate);
    }
    SimpleQuery keywordNextQuery = conversionService.convert(request, SimpleQuery.class);
    keywordNextQuery.addFilterQuery(new SimpleFilterQuery(new Criteria(KEY_LOG_MESSAGE).contains(keyword)));
    keywordNextQuery.setRows(1);
    SolrQuery kewordNextSolrQuery = new DefaultQueryParser().doConstructSolrQuery(keywordNextQuery);
    kewordNextSolrQuery.setStart(0);
    if (hasNextOrAscOrder(isNext, timeAscending)) {
      kewordNextSolrQuery.setSort(LOGTIME, SolrQuery.ORDER.desc);
    } else {
      kewordNextSolrQuery.setSort(LOGTIME, SolrQuery.ORDER.asc);
    }
    kewordNextSolrQuery.addSort(SEQUENCE_ID, SolrQuery.ORDER.desc);
    QueryResponse  queryResponse = serviceLogsSolrDao.process(kewordNextSolrQuery, event);
    if (queryResponse == null) {
      throw RESTErrorUtil.createRESTException("The keyword " + "\"" + keyword + "\"" + " was not found", MessageEnums.ERROR_SYSTEM);
    }
    List<SolrServiceLogData> solrServiceLogDataList = queryResponse.getBeans(SolrServiceLogData.class);
    if (!CollectionUtils.isNotEmpty(solrServiceLogDataList)) {
      throw RESTErrorUtil.createRESTException("The keyword " + "\"" + keyword + "\"" + " was not found", MessageEnums.ERROR_SYSTEM);
    }
    return solrServiceLogDataList.get(0);
  }

  private Date getDocDateFromNextOrLastPage(ServiceLogRequest request, String keyword, boolean isNext, int currentPageNumber, int maxRows) {
    int lastOrFirstLogIndex;
    if (isNext) {
      lastOrFirstLogIndex = ((currentPageNumber + 1) * maxRows);
    } else {
      if (currentPageNumber == 0) {
        throw RESTErrorUtil.createRESTException("This is the first Page", MessageEnums.DATA_NOT_FOUND);
      }
      lastOrFirstLogIndex = (currentPageNumber * maxRows) - 1;
    }
    SimpleQuery sq = conversionService.convert(request, SimpleQuery.class);
    SolrQuery nextPageLogTimeQuery = new DefaultQueryParser().doConstructSolrQuery(sq);
    nextPageLogTimeQuery.remove("start");
    nextPageLogTimeQuery.remove("rows");
    nextPageLogTimeQuery.setStart(lastOrFirstLogIndex);
    nextPageLogTimeQuery.setRows(1);

    QueryResponse queryResponse = serviceLogsSolrDao.process(nextPageLogTimeQuery);
    if (queryResponse == null) {
      throw RESTErrorUtil.createRESTException(String.format("Cannot process next page query for \"%s\" ", keyword), MessageEnums.ERROR_SYSTEM);
    }
    SolrDocumentList docList = queryResponse.getResults();
    if (docList == null || docList.isEmpty()) {
      throw RESTErrorUtil.createRESTException(String.format("Next page element for \"%s\" is not found", keyword), MessageEnums.ERROR_SYSTEM);
    }

    SolrDocument solrDoc = docList.get(0);
    return (Date) solrDoc.get(LOGTIME);
  }

  private boolean hasNextOrAscOrder(boolean isNext, boolean timeAscending) {
    return isNext && !timeAscending || !isNext && timeAscending;
  }

  public Response export(ServiceLogExportRequest request) {
    String defaultFormat = "text";
    SimpleQuery solrQuery = conversionService.convert(request, SimpleQuery.class);
    String from = request.getFrom();
    String to = request.getTo();
    String utcOffset = StringUtils.isBlank(request.getUtcOffset()) ? "0" : request.getUtcOffset();
    String format = request.getFormat() != null && defaultFormat.equalsIgnoreCase(request.getFormat()) ? ".txt" : ".json";
    String fileName = "Component_Logs_" + DateUtil.getCurrentDateInString();

    if (!DateUtil.isDateValid(from) || !DateUtil.isDateValid(to)) {
      logger.error("Not valid date format. Valid format should be" + LogSearchConstants.SOLR_DATE_FORMAT_PREFIX_Z);
      throw RESTErrorUtil.createRESTException("Not valid date format. Valid format should be"
          + LogSearchConstants.SOLR_DATE_FORMAT_PREFIX_Z, MessageEnums.INVALID_INPUT_DATA);

    } else {
      from = from.replace("T", " ");
      from = from.replace(".", ",");

      to = to.replace("T", " ");
      to = to.replace(".", ",");

      to = DateUtil.addOffsetToDate(to, Long.parseLong(utcOffset), "yyyy-MM-dd HH:mm:ss,SSS");
      from = DateUtil.addOffsetToDate(from, Long.parseLong(utcOffset), "yyyy-MM-dd HH:mm:ss,SSS");
    }

    String textToSave = "";
    try {
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
      if (response == null) {
        throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
      }
      SolrDocumentList docList = response.getResults();
      if (docList == null) {
        throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
      }

      if (format.toLowerCase(Locale.ENGLISH).equals(".txt")) {
        Template template = freemarkerConfiguration.getTemplate(SERVICE_LOG_TEMPLATE);
        Map<String, Object> models = new HashMap<>();
        DownloadUtil.fillModelsForLogFile(docList, models, request, format, from, to);
        StringWriter stringWriter = new StringWriter();
        template.process(models, stringWriter);
        textToSave = stringWriter.toString();
      } else if (format.toLowerCase(Locale.ENGLISH).equals(".json")) {
        textToSave = convertObjToString(docList);
      } else {
        throw RESTErrorUtil.createRESTException(
            "unsoported format either should be json or text",
            MessageEnums.ERROR_SYSTEM);
      }
      File file = File.createTempFile(fileName, format);
      FileOutputStream fis = new FileOutputStream(file);
      fis.write(textToSave.getBytes());
      fis.close();
      return Response
        .ok(file, MediaType.APPLICATION_OCTET_STREAM)
        .header("Content-Disposition", "attachment;filename=" + fileName + format)
        .build();
    } catch (SolrException | TemplateException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  public NodeListResponse getComponentListWithLevelCounts(ServiceLogComponentLevelRequest request) {
    SimpleFacetQuery facetQuery = conversionService.convert(request, SimpleFacetQuery.class);
    SolrQuery solrQuery = new DefaultQueryParser().doConstructSolrQuery(facetQuery);
    solrQuery.setFacetSort(StringUtils.isEmpty(request.getSortBy()) ? COMPONENT: request.getSortBy());
    QueryResponse response = serviceLogsSolrDao.process(facetQuery, "/service/logs/components/levels/counts");
    return responseDataGenerator.generateOneLevelServiceNodeTree(response, String.format("%s,%s", COMPONENT, LEVEL));
  }

  public List<FieldMetadata> getServiceLogsSchemaFieldsName() {
    Map<String, String> schemaFieldsMap = solrSchemaFieldDao.getSchemaFieldNameMap(LogType.SERVICE);
    return schemaFieldsMap
      .entrySet()
      .stream()
      .filter(e -> !uiMappingConfig.getServiceFieldExcludeList().contains(e.getKey()))
      .map(e ->
        new FieldMetadata(
          e.getKey(),
          labelFallbackHandler.fallbackIfRequired(
            e.getKey(), uiMappingConfig.getServiceFieldLabels().get(e.getKey()),
            true, false, true,
            uiMappingConfig.getServiceFieldFallbackPrefixes()),
          !uiMappingConfig.getServiceFieldFilterableExcludesList().contains(e.getKey()),
          uiMappingConfig.getServiceFieldVisibleList().contains(e.getKey())))
      .collect(Collectors.toList());
  }

  public BarGraphDataListResponse getAnyGraphCountData(ServiceAnyGraphRequest request) {
    SimpleFacetQuery solrDataQuery = conversionService.convert(request, SimpleFacetQuery.class);
    QueryResponse queryResponse = serviceLogsSolrDao.process(solrDataQuery);
    return responseDataGenerator.getGraphDataWithDefaults(queryResponse, LEVEL, LogSearchConstants.SUPPORTED_LOG_LEVELS);
  }

  public ServiceLogResponse getAfterBeforeLogs(ServiceLogTruncatedRequest request) {
    ServiceLogResponse logResponse = new ServiceLogResponse();
    List<ServiceLogData> docList = null;
    String scrollType = request.getScrollType() != null ? request.getScrollType() : "";

    String logTime = null;
    String sequenceId = null;
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("*:*");
    solrQuery.setRows(1);
    solrQuery.addFilterQuery(String.format("%s:%s", ID, request.getId()));
    QueryResponse response = serviceLogsSolrDao.process(solrQuery);
    if (response == null) {
      return logResponse;
    }
    docList = convertToSolrBeans(response);
    if (docList != null && !docList.isEmpty()) {
      Date date = docList.get(0).getLogTime();
      logTime = DateUtil.convertDateWithMillisecondsToSolrDate(date);
      sequenceId = docList.get(0).getSeqNum().toString();
    }
    if (StringUtils.isBlank(logTime)) {
      return logResponse;
    }
    if (LogSearchConstants.SCROLL_TYPE_BEFORE.equals(scrollType) || LogSearchConstants.SCROLL_TYPE_AFTER.equals(scrollType)) {
      List<ServiceLogData> solrDocList = new ArrayList<>();
      ServiceLogResponse beforeAfterResponse = whenScroll(request, logTime, sequenceId, scrollType);
      if (beforeAfterResponse.getLogList() == null) {
        return logResponse;
      }
      for (ServiceLogData solrDoc : beforeAfterResponse.getLogList()) {
        solrDocList.add(solrDoc);
      }
      logResponse.setLogList(solrDocList);
      return logResponse;

    } else {
      logResponse = new ServiceLogResponse();
      List<ServiceLogData> initial = new ArrayList<>();
      List<ServiceLogData> before = whenScroll(request, logTime, sequenceId, LogSearchConstants.SCROLL_TYPE_BEFORE).getLogList();
      List<ServiceLogData> after = whenScroll(request, logTime, sequenceId, LogSearchConstants.SCROLL_TYPE_AFTER).getLogList();
      if (before != null && !before.isEmpty()) {
        for (ServiceLogData solrDoc : Lists.reverse(before)) {
          initial.add(solrDoc);
        }
      }
      initial.add(docList.get(0));
      if (after != null && !after.isEmpty()) {
        for (ServiceLogData solrDoc : after) {
          initial.add(solrDoc);
        }
      }
      logResponse.setLogList(initial);
      return logResponse;
    }
  }

  private ServiceLogResponse whenScroll(ServiceLogTruncatedRequest request, String logTime, String sequenceId, String afterOrBefore) {
    request.setScrollType(afterOrBefore);
    ServiceLogTruncatedRequestQueryConverter converter = new ServiceLogTruncatedRequestQueryConverter();
    converter.setLogTime(logTime);
    converter.setSequenceId(sequenceId);
    return getLogAsPaginationProvided(converter.convert(request), serviceLogsSolrDao, "service/logs/truncated");
  }

  @Override
  protected List<ServiceLogData> convertToSolrBeans(QueryResponse response) {
    List<SolrServiceLogData> solrServiceLogData = response.getBeans(SolrServiceLogData.class);
    List<ServiceLogData> serviceLogData = new ArrayList<>();
    serviceLogData.addAll(solrServiceLogData);
    return serviceLogData;
  }

  @Override
  protected ServiceLogResponse createLogSearchResponse() {
    return new ServiceLogResponse();
  }

  private <T extends LogData> List<T> getLogDataListByFieldType(Class<T> clazz, QueryResponse response, List<Count> fieldList) {
    List<T> groupList = getComponentBeans(clazz, response);
    String temp = "";
    for (Count cnt : fieldList) {
      T logData = createNewFieldByType(clazz, cnt, temp);
      groupList.add(logData);
    }
    return groupList;
  }

  private <T extends LogData> List<T> getComponentBeans(Class<T> clazz, QueryResponse response) {
    if (clazz.isAssignableFrom(SolrHostLogData.class) || clazz.isAssignableFrom(SolrComponentTypeLogData.class)) {
      return response.getBeans(clazz);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private <T extends LogData> GroupListResponse getFields(String field, String clusters, Class<T> clazz) {
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("*:*");
    SolrUtil.addListFilterToSolrQuery(solrQuery, CLUSTER, clusters);
    GroupListResponse collection = new GroupListResponse();
    SolrUtil.setFacetField(solrQuery, field);
    SolrUtil.setFacetSort(solrQuery, LogSearchConstants.FACET_INDEX);
    QueryResponse response = serviceLogsSolrDao.process(solrQuery);
    if (response == null) {
      return collection;
    }
    FacetField facetField = response.getFacetField(field);
    if (facetField == null) {
      return collection;
    }
    List<Count> fieldList = facetField.getValues();
    if (fieldList == null) {
      return collection;
    }
    SolrDocumentList docList = response.getResults();
    if (docList == null) {
      return collection;
    }
    List<T> logDataListByFieldType = getLogDataListByFieldType(clazz, response, fieldList);
    List<LogData> groupList = new ArrayList<>();
    groupList.addAll(logDataListByFieldType);

    collection.setGroupList(groupList);
    if (!docList.isEmpty()) {
      collection.setStartIndex((int) docList.getStart());
      collection.setTotalCount(docList.getNumFound());
    }
    return collection;
  }

  @SuppressWarnings("unchecked")
  private <T extends LogData> T createNewFieldByType(Class<T> clazz, Count count, String temp) {
    temp = count.getName();
    LogData result = null;
    if (clazz.equals(SolrHostLogData.class)) {
      result = new SolrHostLogData();
      ((SolrHostLogData)result).setHost(temp);
    } else if (clazz.equals(SolrComponentTypeLogData.class)) {
      result = new SolrComponentTypeLogData();
      ((SolrComponentTypeLogData)result).setType(temp);
    } else {
      throw new UnsupportedOperationException();
    }
    
    return (T)result;
  }

  public HostLogFilesResponse getHostLogFileData(HostLogFilesRequest request) {
    SimpleFacetQuery facetQuery = conversionService.convert(request, SimpleFacetQuery.class);
    QueryResponse queryResponse = serviceLogsSolrDao.process(facetQuery, "/service/logs/files");
    return responseDataGenerator.generateHostLogFilesResponse(queryResponse);
  }

  public StatusMessage deleteLogs(ServiceLogRequest request) {
    SimpleQuery solrQuery = conversionService.convert(request, SimpleQuery.class);
    UpdateResponse updateResponse = serviceLogsSolrDao.deleteByQuery(solrQuery, "/service/logs");
    return new StatusMessage(updateResponse.getStatus());
  }

  public List<String> getClusters() {
    return getClusters(serviceLogsSolrDao, CLUSTER, "/service/logs/clusters");
  }


  public ServiceComponentMetadataWrapper getComponentMetadata(String clusters) {
    String pivotFields = COMPONENT + ",group";
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("*:*");
    solrQuery.setRows(0);
    solrQuery.set("facet", true);
    solrQuery.set("facet.pivot", pivotFields);
    SolrUtil.addListFilterToSolrQuery(solrQuery, CLUSTER, clusters);
    QueryResponse queryResponse = serviceLogsSolrDao.process(solrQuery, "/serivce/logs/components");
    return responseDataGenerator.generateGroupedComponentMetadataResponse(
      queryResponse, pivotFields, uiMappingConfig.getServiceGroupLabels(), uiMappingConfig.getServiceComponentLabels());
  }
}
