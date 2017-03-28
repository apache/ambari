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

import org.apache.ambari.logsearch.model.request.impl.ServiceLogTruncatedRequest;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.solr.core.DefaultQueryParser;

import static org.junit.Assert.assertEquals;

public class ServiceLogTruncatedRequestQueryConverterTest extends AbstractRequestConverterTest {

  private ServiceLogTruncatedRequestQueryConverter underTest;

  @Before
  public void setUp() {
    underTest = new ServiceLogTruncatedRequestQueryConverter();
  }

  @Test
  public void testConvert() {
    // GIVEN
    ServiceLogTruncatedRequest request = new ServiceLogTruncatedRequest();
    fillBaseLogRequestWithTestData(request);
    request.setScrollType("0");
    request.setNumberRows(10);
    request.setId("id");
    // WHEN
    SolrQuery query = new DefaultQueryParser().doConstructSolrQuery(underTest.convert(request));
    // THEN
    assertEquals("?q=*%3A*&start=0&rows=10&fq=type%3A%28logsearch_app+secure_log%29&fq=-type%3A%28hst_agent+system_message%29" +
      "&fq=log_message%3Amyincludemessage&fq=-log_message%3Amyexcludemessage&fq=cluster%3Acl1&sort=logtime+desc%2Cseq_num+desc",
      query.toQueryString());
  }

  @Test
  public void testConvertWithoutData() {
    // GIVEN
    ServiceLogTruncatedRequest request = new ServiceLogTruncatedRequest();
    // WHEN
    SolrQuery query = new DefaultQueryParser().doConstructSolrQuery(underTest.convert(request));
    // THEN
    assertEquals("?q=*%3A*&start=0&sort=logtime+desc%2Cseq_num+desc",
      query.toQueryString());
  }
}
