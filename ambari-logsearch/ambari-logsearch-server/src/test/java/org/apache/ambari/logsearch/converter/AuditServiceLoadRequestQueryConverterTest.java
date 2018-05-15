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

import org.apache.ambari.logsearch.model.request.impl.AuditServiceLoadRequest;
import org.apache.ambari.logsearch.model.request.impl.query.AuditServiceLoadQueryRequest;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.solr.core.DefaultQueryParser;

import static org.junit.Assert.assertEquals;

public class AuditServiceLoadRequestQueryConverterTest extends AbstractRequestConverterTest {

  private AuditServiceLoadRequestQueryConverter underTest;

  @Before
  public void setUp() {
    underTest = new AuditServiceLoadRequestQueryConverter();
  }

  @Test
  public void testConvert() {
    // GIVEN
    AuditServiceLoadRequest request = new AuditServiceLoadQueryRequest();
    fillBaseLogRequestWithTestData(request);
    // WHEN
    SolrQuery solrQuery = new DefaultQueryParser().doConstructSolrQuery(underTest.convert(request));
    // THEN
    assertEquals("?q=*%3A*&rows=0&fq=evtTime%3A%5B2016-09-13T22%3A00%3A01.000Z+TO+2016-09-14T22%3A00%3A01.000Z%5D" +
      "&fq=log_message%3Amyincludemessage&fq=-log_message%3Amyexcludemessage&fq=repo%3A%28logsearch_app+OR+secure_log%29" +
      "&fq=-repo%3A%28hst_agent+OR+system_message%29&fq=cluster%3Acl1&facet=true&facet.mincount=1&facet.limit=10&facet.field=repo", solrQuery.toQueryString());
  }

  @Test
  public void testConvertWithoutData() {
    // GIVEN
    AuditServiceLoadRequest request = new AuditServiceLoadQueryRequest();
    // WHEN
    SolrQuery solrQuery = new DefaultQueryParser().doConstructSolrQuery(underTest.convert(request));
    // THEN
    assertEquals("?q=*%3A*&rows=0&fq=evtTime%3A%5B*+TO+*%5D&facet=true&facet.mincount=1&facet.limit=10&facet.field=repo",
      solrQuery.toQueryString());
  }
}
