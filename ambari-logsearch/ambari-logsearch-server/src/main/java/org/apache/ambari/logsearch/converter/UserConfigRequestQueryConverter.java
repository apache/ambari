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

import org.apache.ambari.logsearch.model.request.impl.UserConfigRequest;
import org.apache.ambari.logsearch.util.SolrUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;

import javax.inject.Named;

import java.util.ArrayList;
import java.util.List;

import static org.apache.ambari.logsearch.solr.SolrConstants.CommonLogConstants.CLUSTER;
import static org.apache.ambari.logsearch.solr.SolrConstants.UserConfigConstants.FILTER_NAME;
import static org.apache.ambari.logsearch.solr.SolrConstants.UserConfigConstants.ROW_TYPE;

@Named
public class UserConfigRequestQueryConverter extends AbstractConverterAware<UserConfigRequest, SolrQuery> {

  @Override
  public SolrQuery convert(UserConfigRequest userConfigRequest) {
    SolrQuery userConfigQuery = new SolrQuery();
    userConfigQuery.setQuery("*:*");

    int startIndex = StringUtils.isNotEmpty(userConfigRequest.getStartIndex()) && StringUtils.isNumeric(userConfigRequest.getStartIndex())
      ? Integer.parseInt(userConfigRequest.getStartIndex()) : 0;
    int maxRows = StringUtils.isNotEmpty(userConfigRequest.getPageSize()) && StringUtils.isNumeric(userConfigRequest.getPageSize())
      ? Integer.parseInt(userConfigRequest.getPageSize()) : 10;

    SolrQuery.ORDER order = userConfigRequest.getSortType() != null && SolrQuery.ORDER.desc.equals(SolrQuery.ORDER.valueOf(userConfigRequest.getSortType()))
      ? SolrQuery.ORDER.desc : SolrQuery.ORDER.asc;
    String sortBy = StringUtils.isNotEmpty(userConfigRequest.getSortBy()) ? userConfigRequest.getSortBy() : FILTER_NAME;
    String filterName = StringUtils.isBlank(userConfigRequest.getFilterName()) ? "*" : "*" + userConfigRequest.getFilterName() + "*";

    userConfigQuery.addFilterQuery(String.format("%s:%s", ROW_TYPE, userConfigRequest.getRowType()));
    userConfigQuery.addFilterQuery(String.format("%s:%s", FILTER_NAME, SolrUtil.makeSearcableString(filterName)));
    userConfigQuery.setStart(startIndex);
    userConfigQuery.setRows(maxRows);

    SolrQuery.SortClause sortOrder = SolrQuery.SortClause.create(sortBy, order);
    List<SolrQuery.SortClause> sort = new ArrayList<>();
    sort.add(sortOrder);
    userConfigQuery.setSorts(sort);

    SolrUtil.addListFilterToSolrQuery(userConfigQuery, CLUSTER, userConfigRequest.getClusters());

    return userConfigQuery;
  }
}
