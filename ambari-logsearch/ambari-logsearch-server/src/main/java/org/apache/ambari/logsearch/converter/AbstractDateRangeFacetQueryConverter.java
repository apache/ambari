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

import java.util.Locale;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.model.request.DateRangeParamDefinition;
import org.apache.ambari.logsearch.model.request.UnitParamDefinition;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;

public abstract class AbstractDateRangeFacetQueryConverter<SOURCE extends DateRangeParamDefinition & UnitParamDefinition>
  extends AbstractOperationHolderConverter<SOURCE , SolrQuery> {

  @Override
  public SolrQuery convert(SOURCE request) {
    SolrQuery solrQuery = new SolrQuery();
    String unit = StringUtils.defaultIfEmpty(request.getUnit(), "+1HOUR");
    solrQuery.setQuery("*:*");
    solrQuery.setFacet(true);
    solrQuery.addFacetPivotField("{!range=r1}" + getTypeFieldName());
    solrQuery.setFacetMinCount(1);
    solrQuery.setFacetLimit(-1);
    solrQuery.setFacetSort(LogSearchConstants.FACET_INDEX);
    solrQuery.add("facet.range", "{!tag=r1}" + getDateFieldName());
    solrQuery.add(String.format(Locale.ROOT, "f.%s.%s", getDateFieldName(), "facet.range.start"), request.getFrom());
    solrQuery.add(String.format(Locale.ROOT, "f.%s.%s", getDateFieldName(), "facet.range.end"), request.getTo());
    solrQuery.add(String.format(Locale.ROOT, "f.%s.%s", getDateFieldName(), "facet.range.gap"), unit);
    solrQuery.remove("sort");
    solrQuery.setRows(0);
    solrQuery.setStart(0);
    return solrQuery;
  }

  public abstract String getDateFieldName();

  public abstract String getTypeFieldName();
}
