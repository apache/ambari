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

import org.apache.ambari.logsearch.model.request.impl.TopFieldAuditLogRequest;
import org.apache.ambari.logsearch.model.request.impl.query.TopFieldAuditLogQueryRequest;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.solr.core.DefaultQueryParser;

public class TopFieldAuditLogRequestQueryConverterTest extends AbstractRequestConverterTest {

  private TopFieldAuditLogRequestQueryConverter underTest;

  @Before
  public void setUp() {
    underTest = new TopFieldAuditLogRequestQueryConverter();
  }

  @Test
  public void testConvert() {
    // GIVEN
    TopFieldAuditLogRequest request = new TopFieldAuditLogQueryRequest();
    fillBaseLogRequestWithTestData(request);
    request.setTop(10);
    request.setField("myfield");
    // WHEN
    SolrQuery query = new DefaultQueryParser().doConstructSolrQuery(underTest.convert(request));
    // THEN
    assertEquals("?q=*%3A*&rows=0&fq=evtTime%3A%5B2016-09-13T22%3A00%3A01.000Z+TO+2016-09-14T22%3A00%3A01.000Z%5D" +
        "&fq=log_message%3Amyincludemessage&fq=-log_message%3Amyexcludemessage&fq=repo%3A%28logsearch_app+%22OR%22+secure_log%29" +
        "&fq=-repo%3A%28hst_agent+%22OR%22+system_message%29&fq=cluster%3Acl1&facet=true&facet.mincount=1&facet.limit=10&facet.pivot=myfield%2Crepo",
      query.toQueryString());
  }

  @Test(expected = IllegalArgumentException.class) // TODO: later use @Valid on the fields to validate object
  public void testConvertWithoutData() {
    // GIVEN
    TopFieldAuditLogRequest request = new TopFieldAuditLogQueryRequest();
    // WHEN
    new DefaultQueryParser().doConstructSolrQuery(underTest.convert(request));
  }
}
