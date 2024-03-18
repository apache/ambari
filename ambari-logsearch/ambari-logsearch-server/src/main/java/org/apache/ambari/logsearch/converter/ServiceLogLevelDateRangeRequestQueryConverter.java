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
import org.apache.ambari.logsearch.model.request.impl.ServiceGraphRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;

import javax.inject.Named;

import java.util.Arrays;
import java.util.List;

import static org.apache.ambari.logsearch.solr.SolrConstants.CommonLogConstants.CLUSTER;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.COMPONENT;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.HOST;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.LEVEL;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.LOGTIME;

@Named
public class ServiceLogLevelDateRangeRequestQueryConverter extends AbstractDateRangeFacetQueryConverter<ServiceGraphRequest>{

  @Override
  public String getDateFieldName() {
    return LOGTIME;
  }

  @Override
  public String getTypeFieldName() {
    return LEVEL;
  }

  @Override
  public SolrQuery convert(ServiceGraphRequest request) {
    SolrQuery solrQuery = super.convert(request);
    addListFilterToSolrQuery(solrQuery, LEVEL, request.getLevel());
    if (request.getHostList() != null && StringUtils.isEmpty(request.getHostName())) {
      List<String> hosts = request.getHostList().length() == 0 ? Arrays.asList("\\-1") : splitValueAsList(request.getHostList(), ",");
      if (hosts.size() > 1) {
        solrQuery.addFilterQuery(String.format("%s:(%s)", HOST, StringUtils.join(hosts, " OR ")));
      } else {
        solrQuery.addFilterQuery(String.format("%s:%s", HOST, hosts.get(0)));
      }
    }
    addListFilterToSolrQuery(solrQuery, CLUSTER, request.getClusters());
    addListFilterToSolrQuery(solrQuery, COMPONENT, request.getMustBe());
    addIncludeFieldValues(solrQuery, request.getIncludeQuery());
    addExcludeFieldValues(solrQuery, request.getExcludeQuery());
    return solrQuery;
  }

  @Override
  public LogType getLogType() {
    return LogType.SERVICE;
  }


}
