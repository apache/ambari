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

import org.apache.ambari.logsearch.model.request.impl.ServiceLogRequest;
import org.apache.ambari.logsearch.util.SolrUtil;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.solr.core.DefaultQueryParser;
import org.springframework.data.solr.core.query.SimpleQuery;

import static org.junit.Assert.assertEquals;

public class BaseServiceLogRequestQueryConverterTest extends AbstractRequestConverterTest {

  private BaseServiceLogRequestQueryConverter underTest;

  @Before
  public void setUp() {
    underTest = new BaseServiceLogRequestQueryConverter();
  }

  @Test
  public void testConvertRequest() {
    // GIVEN
    ServiceLogRequest logRequest = new ServiceLogRequest();
    fillBaseLogRequestWithTestData(logRequest);
    logRequest.setLevel("FATAL,ERROR,WARN,UNKNOWN");
    logRequest.setFileName("/var/log/myfile-*-hdfs.log");
    logRequest.setComponentName("component");
    logRequest.setHostName("logsearch.com");
    // WHEN
    SimpleQuery query = underTest.convert(logRequest);
    DefaultQueryParser defaultQueryParser = new DefaultQueryParser();
    SolrQuery solrQuery = defaultQueryParser.doConstructSolrQuery(query);
    SolrUtil.removeDoubleOrTripleEscapeFromFilters(solrQuery);
    // THEN
    assertEquals("?q=*%3A*&start=0&rows=25&fq=type%3A%28logsearch_app+secure_log%29&fq=-type%3A%28hst_agent+system_message%29" +
      "&fq=log_message%3Amyincludemessage&fq=-log_message%3Amyexcludemessage&fq=cluster%3Acl1" +
      "&fq=host%3Alogsearch.com&fq=path%3A%5C%2Fvar%5C%2Flog%5C%2Fmyfile%5C-%5C*%5C-hdfs.log&fq=type%3Acomponent" +
      "&fq=level%3A%28FATAL+ERROR+WARN+UNKNOWN%29&fq=logtime%3A%5B2016-09-13T22%3A00%3A01.000Z+TO+2016-09-14T22%3A00%3A01.000Z%5D&sort=logtime+desc%2Cseq_num+desc",
      solrQuery.toQueryString());
  }

  @Test
  public void testConvertRequestWithoutData() {
    // GIVEN
    ServiceLogRequest logRequest = new ServiceLogRequest();
    // WHEN
    SimpleQuery query = underTest.convert(logRequest);
    // THEN
    assertEquals(Integer.valueOf(99999), query.getRows());
  }

}
