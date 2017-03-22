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

import org.apache.ambari.logsearch.model.request.impl.HostLogFilesRequest;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.solr.core.DefaultQueryParser;

import static org.junit.Assert.assertEquals;

public class HostLogFilesRequestQueryConverterTest extends AbstractRequestConverterTest {

  private HostLogFilesRequestQueryConverter underTest;

  @Before
  public void setUp() {
    underTest = new HostLogFilesRequestQueryConverter();
  }

  @Test
  public void testConvertHostNameOnly() {
    // GIVEN
    HostLogFilesRequest request = new HostLogFilesRequest();
    request.setHostName("hostName");
    // WHEN
    SolrQuery query = new DefaultQueryParser().doConstructSolrQuery(underTest.convert(request));
    // THEN
    assertEquals("?q=host%3A%28hostName%29&rows=0&facet=true&facet.mincount=1&facet.limit=-1&facet.pivot=type%2Cpath",
      query.toQueryString());
  }

  @Test
  public void testConvertHostNameAndComponentName() {
    // GIVEN
    HostLogFilesRequest request = new HostLogFilesRequest();
    request.setHostName("hostName");
    request.setComponentName("componentName");
    // WHEN
    SolrQuery query = new DefaultQueryParser().doConstructSolrQuery(underTest.convert(request));
    // THEN
    assertEquals("?q=host%3A%28hostName%29+AND+type%3A%28componentName%29&rows=0&facet=true&facet.mincount=1&facet.limit=-1" +
        "&facet.pivot=type%2Cpath", query.toQueryString());
  }
}
