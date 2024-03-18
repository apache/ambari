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
package org.apache.ambari.logsearch.converter;

import org.apache.ambari.logsearch.common.LogType;
import org.apache.ambari.logsearch.model.request.impl.HostLogFilesRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.FacetOptions.FacetSort;
import org.springframework.data.solr.core.query.SimpleFacetQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;

import static org.apache.ambari.logsearch.solr.SolrConstants.CommonLogConstants.CLUSTER;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.HOST;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.COMPONENT;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.PATH;

import javax.inject.Named;

@Named
public class HostLogFilesRequestQueryConverter extends AbstractOperationHolderConverter<HostLogFilesRequest, SimpleFacetQuery>{

  @Override
  public SimpleFacetQuery convert(HostLogFilesRequest request) {
    SimpleFacetQuery facetQuery = new SimpleFacetQuery();
    facetQuery.addCriteria(new SimpleStringCriteria(String.format("%s:(%s)", HOST, request.getHostName())));
    if (StringUtils.isNotEmpty(request.getComponentName())) {
      facetQuery.addCriteria(new SimpleStringCriteria(String.format("%s:(%s)", COMPONENT, request.getComponentName())));
    }
    FacetOptions facetOptions = new FacetOptions();
    facetOptions.setFacetMinCount(1);
    facetOptions.setFacetLimit(-1);
    facetOptions.setFacetSort(FacetSort.COUNT);
    facetOptions.addFacetOnPivot(COMPONENT, PATH);
    facetQuery.setFacetOptions(facetOptions);
    addInFilterQuery(facetQuery, CLUSTER, splitValueAsList(request.getClusters(), ","));
    facetQuery.setRows(0);
    return facetQuery;
  }

  @Override
  public LogType getLogType() {
    return LogType.SERVICE;
  }
}
