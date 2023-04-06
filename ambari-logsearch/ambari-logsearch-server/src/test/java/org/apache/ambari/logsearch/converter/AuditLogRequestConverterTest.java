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

import static org.junit.Assert.assertEquals;

import org.apache.ambari.logsearch.model.request.impl.AuditLogRequest;
import org.apache.ambari.logsearch.model.request.impl.query.AuditLogQueryRequest;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.solr.core.DefaultQueryParser;
import org.springframework.data.solr.core.query.SimpleQuery;

public class AuditLogRequestConverterTest extends AbstractRequestConverterTest {

  private AuditLogRequestQueryConverter underTest;

  @Before
  public void setUp() {
    underTest = new AuditLogRequestQueryConverter();
  }

  @Test
  public void testConvert() {
    // GIVEN
    AuditLogRequest request = new AuditLogQueryRequest();
    fillBaseLogRequestWithTestData(request);
    request.setUserList("joe,steven");
    // WHEN
    SimpleQuery simpleQuery = underTest.convert(request);
    SolrQuery queryResult = new DefaultQueryParser().doConstructSolrQuery(simpleQuery);
    // THEN
    assertEquals("?q=*%3A*&start=0&rows=25&fq=repo%3A%28logsearch_app+%22OR%22+secure_log%29&fq=-repo%3A%28hst_agent+%22OR%22+system_message%29" +
        "&fq=log_message%3Amyincludemessage&fq=-log_message%3Amyexcludemessage&fq=cluster%3Acl1&fq=reqUser%3A%28joe+%22OR%22+steven%29&sort=evtTime+desc%2Cseq_num+desc",
      queryResult.toQueryString());
  }

  @Test
  public void testConvertWithoutData() {
    // GIVEN
    AuditLogRequest request = new AuditLogQueryRequest();
    // WHEN
    SimpleQuery simpleQuery = underTest.convert(request);
    SolrQuery queryResult = new DefaultQueryParser().doConstructSolrQuery(simpleQuery);
    // THEN
    assertEquals("?q=*%3A*&start=0&rows=99999&sort=evtTime+desc%2Cseq_num+desc", queryResult.toQueryString());
  }

}
