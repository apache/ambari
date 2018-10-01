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

import org.apache.ambari.logsearch.model.request.impl.AuditComponentRequest;
import org.apache.ambari.logsearch.model.request.impl.query.AuditComponentQueryRequest;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.solr.core.DefaultQueryParser;
import org.springframework.data.solr.core.query.SimpleFacetQuery;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

public class AuditComponentRequestQueryConverterTest extends AbstractRequestConverterTest {

  private AuditComponentsRequestQueryConverter underTest;

  @Before
  public void setUp() {
    underTest = new AuditComponentsRequestQueryConverter();
  }

  @Test
  public void testConvert() {
    // GIVEN
    AuditComponentRequest request = new AuditComponentQueryRequest();
    fillCommonRequestWithTestData(request);
    // WHEN
    SimpleFacetQuery facetQuery = underTest.convert(request);
    SolrQuery query = new DefaultQueryParser().doConstructSolrQuery(facetQuery);
    // THEN
    assertEquals("?q=*%3A*&rows=0&fq=evtTime%3A%5B*+TO+*%5D&fq=cluster%3Acl1&facet=true&facet.mincount=1&facet.limit=-1&facet.sort=index&facet.field=repo",
      query.toQueryString());
  }

  @Test
  public void testConvertWithoutData() {
    // GIVEN
    AuditComponentRequest request = new AuditComponentQueryRequest();
    // WHEN
    SimpleFacetQuery facetQuery = underTest.convert(request);
    SolrQuery query = new DefaultQueryParser().doConstructSolrQuery(facetQuery);
    // THEN
    assertNotNull(facetQuery);
    assertEquals("?q=*%3A*&rows=0&fq=evtTime%3A%5B*+TO+*%5D&facet=true&facet.mincount=1&facet.limit=-1&facet.sort=index&facet.field=repo",
      query.toQueryString());
  }

}
