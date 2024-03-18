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

import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.SimpleFacetQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;

import javax.inject.Named;

@Named
public class StringFieldFacetQueryConverter extends AbstractConverterAware<String, SimpleFacetQuery> {

  @Override
  public SimpleFacetQuery convert(String fieldName) {
    Criteria criteria = new SimpleStringCriteria("*:*");
    SimpleFacetQuery facetQuery = new SimpleFacetQuery();
    facetQuery.addCriteria(criteria);
    facetQuery.setRows(0);
    FacetOptions facetOptions = new FacetOptions();
    facetOptions.setFacetMinCount(1);
    facetOptions.addFacetOnField(fieldName);
    facetOptions.setFacetLimit(-1);
    facetQuery.setFacetOptions(facetOptions);
    return facetQuery;
  }
}
