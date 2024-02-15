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

import org.apache.ambari.logsearch.model.request.impl.ServiceLogHostComponentRequest;
import org.apache.ambari.logsearch.model.request.impl.query.ServiceLogHostComponentQueryRequest;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.solr.core.DefaultQueryParser;

public class ServiceLogTreeRequestFacetQueryConverterTest extends AbstractRequestConverterTest {
  private ServiceLogTreeRequestFacetQueryConverter underTest;

  @Before
  public void setUp() {
    underTest = new ServiceLogTreeRequestFacetQueryConverter();
  }

  @Test
  public void testConvert() {
    // GIVEN
    ServiceLogHostComponentRequest request = new ServiceLogHostComponentQueryRequest();
    fillBaseLogRequestWithTestData(request);
    request.setLevel("WARN,ERROR,FATAL");
    // WHEN
    SolrQuery query = new DefaultQueryParser().doConstructSolrQuery(underTest.convert(request));
    // THEN
    assertEquals("?q=*%3A*&rows=0&fq=logtime%3A%5B2016-09-13T22%3A00%3A01.000Z+TO+2016-09-14T22%3A00%3A01.000Z%5D" +
        "&fq=log_message%3Amyincludemessage&fq=-log_message%3Amyexcludemessage&fq=type%3A%28logsearch_app+%22OR%22+secure_log%29" +
        "&fq=-type%3A%28hst_agent+%22OR%22+system_message%29&fq=level%3A%28WARN+%22OR%22+ERROR+%22OR%22+FATAL%29&fq=cluster%3Acl1&facet=true" +
        "&facet.mincount=1&facet.limit=-1&facet.sort=index&facet.pivot=host%2Ctype%2Clevel&facet.pivot=host%2Clevel",
      query.toQueryString());
  }

  @Test
  public void testConvertWithoutData() {
    // GIVEN
    ServiceLogHostComponentRequest request = new ServiceLogHostComponentQueryRequest();
    request.setLevel("WARN,ERROR,FATAL");
    // WHEN
    SolrQuery query = new DefaultQueryParser().doConstructSolrQuery(underTest.convert(request));
    // THEN
    assertEquals("?q=*%3A*&rows=0&fq=logtime%3A%5B*+TO+*%5D&fq=level%3A%28WARN+%22OR%22+ERROR+%22OR%22+FATAL%29" +
        "&facet=true&facet.mincount=1&facet.limit=-1&facet.sort=index&facet.pivot=host%2Ctype%2Clevel&facet.pivot=host%2Clevel",
      query.toQueryString());
  }
}
