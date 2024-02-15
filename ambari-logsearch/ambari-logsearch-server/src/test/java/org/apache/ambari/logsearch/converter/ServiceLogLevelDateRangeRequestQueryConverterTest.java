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

import org.apache.ambari.logsearch.model.request.impl.ServiceGraphRequest;
import org.apache.ambari.logsearch.model.request.impl.query.ServiceGraphQueryRequest;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ServiceLogLevelDateRangeRequestQueryConverterTest extends AbstractRequestConverterTest {

  private ServiceLogLevelDateRangeRequestQueryConverter underTest;

  @Before
  public void setUp() {
    underTest = new ServiceLogLevelDateRangeRequestQueryConverter();
  }

  @Test
  public void testConvert() {
    // GIVEN
    ServiceGraphRequest request = new ServiceGraphQueryRequest();
    fillBaseLogRequestWithTestData(request);
    request.setUnit("+1HOUR");
    request.setLevel("WARN,ERROR,FATAL");
    // WHEN
    SolrQuery query = underTest.convert(request);
    // THEN
    assertEquals("?q=*%3A*&facet=true&facet.pivot=%7B%21range%3Dr1%7Dlevel&facet.mincount=1&facet.limit=-1" +
      "&facet.sort=index&facet.range=%7B%21tag%3Dr1%7Dlogtime&f.logtime.facet.range.start=2016-09-13T22%3A00%3A01.000Z" +
      "&f.logtime.facet.range.end=2016-09-14T22%3A00%3A01.000Z&f.logtime.facet.range.gap=%2B1HOUR&rows=0&start=0&fq=level%3A%28WARN+OR+ERROR+OR+FATAL%29&fq=cluster%3Acl1&fq=type%3A%28logsearch_app+OR+secure_log%29&fq=log_message%3Amyincludemessage&fq=-log_message%3Amyexcludemessage", query.toQueryString());
  }

  @Test
  public void testConvertWithoutData() {
    // GIVEN
    ServiceGraphRequest request = new ServiceGraphQueryRequest();
    request.setUnit("+1HOUR"); // minimal data for date range gap
    request.setFrom("2016-09-13T22:00:01.000Z");
    request.setTo("2016-09-14T22:00:01.000Z");
    // WHEN
    SolrQuery query = underTest.convert(request);
    // THEN
    assertEquals("?q=*%3A*&facet=true&facet.pivot=%7B%21range%3Dr1%7Dlevel&facet.mincount=1&facet.limit=-1&facet.sort=index" +
      "&facet.range=%7B%21tag%3Dr1%7Dlogtime&f.logtime.facet.range.start=2016-09-13T22%3A00%3A01.000Z" +
      "&f.logtime.facet.range.end=2016-09-14T22%3A00%3A01.000Z&f.logtime.facet.range.gap=%2B1HOUR&rows=0&start=0",
      query.toQueryString());
  }
}
