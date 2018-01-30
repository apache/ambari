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

import org.apache.ambari.logsearch.model.request.impl.AuditBarGraphRequest;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AuditBarGraphRequestQueryConverterTest extends AbstractRequestConverterTest {

  private AuditBarGraphRequestQueryConverter underTest;

  @Before
  public void setUp() {
    underTest = new AuditBarGraphRequestQueryConverter();
  }

  @Test
  public void testConvert() {
    // GIVEN
    AuditBarGraphRequest request = new AuditBarGraphRequest();
    request.setUserList("joe,steven");
    // WHEN
    fillBaseLogRequestWithTestData(request);
    request.setUnit("+1HOUR");
    // THEN
    SolrQuery query = underTest.convert(request);
    assertEquals("?q=*%3A*&facet=true&facet.pivot=%7B%21range%3Dr1%7Drepo&facet.mincount=1&facet.limit=-1&facet.sort=index" +
      "&facet.range=%7B%21tag%3Dr1%7DevtTime&f.evtTime.facet.range.start=2016-09-13T22%3A00%3A01.000Z&f.evtTime.facet.range.end=2016-09-14T22%3A00%3A01.000Z&f.evtTime.facet.range.gap=%2B1HOUR" +
      "&rows=0&start=0&fq=cluster%3Acl1&fq=reqUser%3A%28joe+OR+steven%29",
      query.toQueryString());
  }

  @Test
  public void testConvertWithoutData() {
    // GIVEN
    AuditBarGraphRequest request = new AuditBarGraphRequest();
    // WHEN
    SolrQuery query = underTest.convert(request);
    // THEN
    assertEquals(Integer.valueOf(0), query.getRows());
    assertEquals(-1, query.getFacetLimit());
  }

}
