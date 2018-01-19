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
package org.apache.ambari.infra.job.archive;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

import static org.apache.ambari.infra.job.archive.SolrQueryBuilder.PARAMETER_PATTERN;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class SolrQueryBuilderTest {
  private static final Document DOCUMENT = new Document(new HashMap<String, String>() {{
    put("logtime", "2017-10-02'T'10:00:11.634Z");
    put("id", "1");
  }});

  @Test
  public void testDefaultQuery() throws Exception {
    SolrQuery solrQuery = new SolrQueryBuilder()
            .build();
    assertThat(solrQuery.getQuery(), is("*:*"));
  }

  @Test
  public void testSetQuery() throws Exception {
    SolrQuery solrQuery = new SolrQueryBuilder()
            .setQueryText("logtime:[* TO \"${end}\"]")
            .setEndValue("2017-11-27'T'10:12:11.372Z")
            .build();
    assertThat(solrQuery.getQuery(), is("logtime:[* TO \"2017-11-27'T'10:12:11.372Z\"]"));
  }

  @Test
  public void testSetFilterQuery() throws Exception {
    SolrQuery solrQuery = new SolrQueryBuilder()
            .setFilterQueryText("(logtime:\"${logtime}\" AND id:{\"${id}\" TO *]) OR logtime:{\"${logtime}\" TO \"${end}\"]")
            .setDocument(DOCUMENT)
            .setEndValue("2017-11-27'T'10:12:11.372Z")
            .build();
    assertThat(solrQuery.getFilterQueries()[0], is("(logtime:\"2017-10-02'T'10:00:11.634Z\" AND id:{\"1\" TO *]) OR logtime:{\"2017-10-02'T'10:00:11.634Z\" TO \"2017-11-27'T'10:12:11.372Z\"]"));
  }

  @Test
  public void testSetFilterQueryWhenDocumentIsNull() throws Exception {
    SolrQuery solrQuery = new SolrQueryBuilder()
            .setFilterQueryText("(logtime:\"${logtime}\" AND id:{\"${id}\" TO *]) OR logtime:{\"${logtime}\" TO \"${end}\"]")
            .setEndValue("2017-11-27'T'10:12:11.372Z")
            .build();
    assertThat(solrQuery.getFilterQueries(), is(nullValue()));
  }

  @Test
  public void testSetFilterQueryWhenEndValueIsNull() throws Exception {
    SolrQuery solrQuery = new SolrQueryBuilder()
            .setFilterQueryText("logtime:\"${logtime}\" AND id:{\"${id}\" TO *]")
            .setDocument(DOCUMENT)
            .build();
    assertThat(solrQuery.getFilterQueries()[0], is("logtime:\"2017-10-02'T'10:00:11.634Z\" AND id:{\"1\" TO *]"));
  }

  @Test
  public void testSetFilterQueryWhenQueryFilterIsNullButDocumentIsNot() throws Exception {
    SolrQuery solrQuery = new SolrQueryBuilder()
            .setDocument(DOCUMENT)
            .build();
    assertThat(solrQuery.getFilterQueries(), is(nullValue()));
  }

  @Test
  public void testRegex() throws Exception {
    Matcher matcher = PARAMETER_PATTERN.matcher("(logtime:\"${logtime}\" AND id:{\"${id}\" TO *]) OR logtime:{\"${logtime}\" TO \"${end}\"]");
    List<String> parameters = new ArrayList<>();
    while (matcher.find())
      parameters.add(matcher.group());

    assertThat(parameters, hasSize(4));
    assertThat(parameters.get(0), is("${logtime}"));
    assertThat(parameters.get(1), is("${id}"));
    assertThat(parameters.get(2), is("${logtime}"));
    assertThat(parameters.get(3), is("${end}"));
  }

  @Test
  public void testSort() throws Exception {
    SolrQuery solrQuery = new SolrQueryBuilder().addSort("logtime", "id").build();
    assertThat(solrQuery.getSorts().get(0).getItem(), is("logtime"));
    assertThat(solrQuery.getSorts().get(1).getItem(), is("id"));
  }
}
