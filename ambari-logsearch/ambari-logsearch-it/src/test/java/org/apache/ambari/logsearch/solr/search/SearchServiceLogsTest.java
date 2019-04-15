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
package org.apache.ambari.logsearch.solr.search;

import static org.apache.ambari.logsearch.solr.Solr.SOLR_DATETIME_FORMATTER;
import static org.apache.ambari.logsearch.solr.SolrDocumentMatcher.solrDoc;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.logsearch.converter.BaseServiceLogRequestQueryConverter;
import org.apache.ambari.logsearch.converter.ServiceLogLevelDateRangeRequestQueryConverter;
import org.apache.ambari.logsearch.model.request.impl.BaseServiceLogRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceGraphRequest;
import org.apache.ambari.logsearch.model.request.impl.query.BaseServiceLogQueryRequest;
import org.apache.ambari.logsearch.model.request.impl.query.ServiceGraphQueryRequest;
import org.apache.ambari.logsearch.solr.Solr;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.PivotField;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.data.solr.core.DefaultQueryParser;
import org.springframework.data.solr.core.query.SimpleQuery;

public class SearchServiceLogsTest {
  private static Solr solr;

  private final BaseServiceLogRequestQueryConverter requestQueryConverter = new BaseServiceLogRequestQueryConverter();

  @BeforeClass
  public static void setUp() throws Exception {
    solr = Solr.core("hadoop_logs");
  }

  @AfterClass
  public static void stopSolr() throws Exception {
    solr.close();
  }

  @After
  public void tearDown() throws Exception {
    solr.clear();
  }

  @Test
  public void testIncludeLogMessageFilter() throws Exception {
    solr.addDoc("0", "Unhandled exception");
    solr.addDoc("1", "exception occurred");
    solr.addDoc("2", "exception");
    solr.addDoc("3", "Unhandled error");
    solr.addDoc("4", "Error occurred");

    BaseServiceLogRequest request = new BaseServiceLogQueryRequest();
    request.setIncludeQuery("[{\"log_message\":\"exception\"}]");
    ArrayList<SolrDocument> found = executeQuery(request);

    assertThat(found, hasSize(3));
    assertThat(found, hasItem(solrDoc("0")));
    assertThat(found, hasItem(solrDoc("1")));
    assertThat(found, hasItem(solrDoc("2")));
  }

  @Test
  public void testIncludeBlankLogMessageFilter() throws Exception {
    solr.addDoc("0", "");
    solr.addDoc("1", "exception occurred");

    BaseServiceLogRequest request = new BaseServiceLogQueryRequest();
    request.setIncludeQuery("[{\"log_message\":\"\\\"\\\"\"}]");
    ArrayList<SolrDocument> found = executeQuery(request);

    assertThat(found, hasSize(0));
  }

  @Test
  public void testIncludeQuotedSpaceLogMessageFilter() throws Exception {
    solr.addDoc("0", " ");
    solr.addDoc("1", "exception occurred");

    BaseServiceLogRequest request = new BaseServiceLogQueryRequest();
    request.setIncludeQuery("[{\"log_message\":\"\\\" \\\"\"}]");
    ArrayList<SolrDocument> found = executeQuery(request);

    assertThat(found, hasSize(0));
  }

  @Test
  public void testIncludeSpaceLogMessageFilter() throws Exception {
    solr.addDoc("0", " ");
    solr.addDoc("1", "exception occurred");

    BaseServiceLogRequest request = new BaseServiceLogQueryRequest();
    request.setIncludeQuery("[{\"log_message\":\" \"}]");
    ArrayList<SolrDocument> found = executeQuery(request);

    assertThat(found, hasSize(2));
  }

  private SolrDocumentList executeQuery(BaseServiceLogRequest request) throws IOException, SolrServerException {
    SimpleQuery simpleQuery = requestQueryConverter.convert(request);
    assert simpleQuery != null;
    return solr.executeQuery(new DefaultQueryParser().doConstructSolrQuery(simpleQuery));
  }


  @Test
  public void testIncludeMultipleLogMessageFilterContainsWildcard() throws Exception {
    solr.addDoc("0", "Unhandled exception");
    solr.addDoc("1", "exception occurred");
    solr.addDoc("2", "exception");
    solr.addDoc("3", "Unhandled error");
    solr.addDoc("4", "Error occurred");

    BaseServiceLogRequest request = new BaseServiceLogQueryRequest();
    request.setIncludeQuery("[{\"log_message\":\"exceptio*\"},{\"log_message\":\"unha*\"}]");
    SolrDocumentList found = executeQuery(request);

    assertThat(found, hasSize(1));
    assertThat(found, hasItem(solrDoc("0")));
  }

  @Test
  public void testIncludeMultipleTerms() throws Exception {
    solr.addDoc("0", "At line 112 an unhandled exception occurred when");
    solr.addDoc("1", "exception occurred");
    solr.addDoc("2", "exception");
    solr.addDoc("3", "Unhandled error");
    solr.addDoc("4", "Error occurred");

    BaseServiceLogRequest request = new BaseServiceLogQueryRequest();
    request.setIncludeQuery("[{\"log_message\":\"\\\"Unhandled exception\\\"\"}]");
    SolrDocumentList found = executeQuery(request);

    assertThat(found, hasSize(1));
    assertThat(found, hasItem(solrDoc("0")));
  }

  @Test
  public void testIncludeMultipleTermsDelimitedByDash() throws Exception {
    solr.addDoc("0", "Unhandled exception");
    solr.addDoc("1", "Unhandled+exception");
    solr.addDoc("2", "At line 112 an unhandled-exception occurred when");
    solr.addDoc("3", "exception occurred");
    solr.addDoc("4", "exception");
    solr.addDoc("5", "Unhandled error");
    solr.addDoc("6", "Error occurred");

    BaseServiceLogRequest request = new BaseServiceLogQueryRequest();
    request.setIncludeQuery("[{\"log_message\":\"\\\"Unhandled-exception\\\"\"}]");
    SolrDocumentList found = executeQuery(request);

    assertThat(found, hasSize(3));
    assertThat(found, hasItem(solrDoc("0")));
    assertThat(found, hasItem(solrDoc("1")));
    assertThat(found, hasItem(solrDoc("2")));
  }

  @Test
  public void testIncludeMultipleTermsDelimitedByPlusSign() throws Exception {
    solr.addDoc("0", "Unhandled exception");
    solr.addDoc("1", "Unhandled+exception");
    solr.addDoc("2", "Unhandled-exception");
    solr.addDoc("3", "exception occurred");
    solr.addDoc("4", "exception");
    solr.addDoc("5", "Unhandled error");
    solr.addDoc("6", "Error occurred");

    BaseServiceLogRequest request = new BaseServiceLogQueryRequest();
    request.setIncludeQuery("[{\"log_message\":\"\\\"Unhandled+exception\\\"\"}]");
    SolrDocumentList found = executeQuery(request);

    assertThat(found, hasSize(3));
    assertThat(found, hasItem(solrDoc("0")));
    assertThat(found, hasItem(solrDoc("1")));
    assertThat(found, hasItem(solrDoc("2")));
  }

  @Test
  public void testIncludeALongMessage() throws Exception {
    solr.addDoc("6", "[   ] org.apache.solr.cloud.autoscaling.OverseerTriggerThread (OverseerTriggerThread.java:400) - Error in trigger 'index_size_trigger' configuration, trigger config ignored: {\\r\\n  \\\"aboveBytes\\\":53687091200,\\r\\n  \\\"aboveOp\\\":\\\"SPLITSHARD\\\",\\r\\n  \\\"event\\\":\\\"indexSize\\\",\\r\\n  \\\"waitFor\\\":1,\\r\\n  \\\"actions\\\":[\\r\\n    {\\r\\n      \\\"name\\\":\\\"compute_plan\\\",\\r\\n      \\\"class\\\":\\\"solr.ComputePlanAction\\\"},\\r\\n    {\\r\\n      \\\"name\\\":\\\"execute_plan\\\",\\r\\n      \\\"class\\\":\\\"solr.ExecutePlanAction\\\"}],\\r\\n  \\\"enabled\\\":true}\\r\\norg.apache.solr.cloud.autoscaling.TriggerValidationException: null\\r\\n\\tat org.apache.solr.cloud.autoscaling.TriggerBase.configure(TriggerBase.java:118) ~[solr-core-7.5.0.jar:7.5.0 b5bf70b7e32d7ddd9742cc821d471c5fabd4e3df - jimczi - 2018-09-18 13:07:55]\\r\\n\\tat org.apache.solr.cloud.autoscaling.IndexSizeTrigger.configure(IndexSizeTrigger.java:87) ~[solr-core-7.5.0.jar:7.5.0 b5bf70b7e32d7ddd9742cc821d471c5fabd4e3df - jimczi - 2018-09-18 13:07:55]\\r\\n\\tat org.apache.solr.cloud.autoscaling.AutoScaling$TriggerFactoryImpl.create(AutoScaling.java:189) ~[solr-core-7.5.0.jar:7.5.0 b5bf70b7e32d7ddd9742cc821d471c5fabd4e3df - jimczi - 2018-09-18 13:07:55]\\r\\n\\tat org.apache.solr.cloud.autoscaling.OverseerTriggerThread.loadTriggers(OverseerTriggerThread.java:398) [solr-core-7.5.0.jar:7.5.0 b5bf70b7e32d7ddd9742cc821d471c5fabd4e3df - jimczi - 2018-09-18 13:07:55]\\r\\n\\tat org.apache.solr.cloud.autoscaling.OverseerTriggerThread.refreshAutoScalingConf(OverseerTriggerThread.java:335) [solr-core-7.5.0.jar:7.5.0 b5bf70b7e32d7ddd9742cc821d471c5fabd4e3df - jimczi - 2018-09-18 13:07:55]\\r\\n\\tat org.apache.solr.cloud.autoscaling.OverseerTriggerThread.run(OverseerTriggerThread.java:161) [solr-core-7.5.0.jar:7.5.0 b5bf70b7e32d7ddd9742cc821d471c5fabd4e3df - jimczi - 2018-09-18 13:07:55]\\r\\n\\tat java.lang.Thread.run(Thread.java:745) [?:1.8.0_112]");
    solr.addDoc("7", "[   ] org.apache.solr.cloud.autoscaling.OverseerTriggerThread (OverseerTriggerThread.java:422) - Something else");
    solr.addDoc("8", "[   ] org.apache.solr.cloud.OverseerTriggerThread (OverseerTriggerThread.java:400) - Different package");

    BaseServiceLogRequest request = new BaseServiceLogQueryRequest();
    request.setIncludeQuery("[{\"log_message\":\"\\\"org.apache.solr.cloud.autoscaling.OverseerTriggerThread (OverseerTriggerThread.java:400)\\\"\"}]");
    SolrDocumentList found = executeQuery(request);

    assertThat(found, hasSize(1));
    assertThat(found, hasItem(solrDoc("6")));
  }

  @Test
  public void testIncludeMultipleWhitespaceDelimitedWords() throws Exception {
    solr.addDoc("0", "At line 112 an unhandled exception occurred when");
    solr.addDoc("1", "At line 112 an unhandled  exception occurred when");
    solr.addDoc("2", "At line 112 an unhandled   \texception occurred when");
    solr.addDoc("3", "At line 112 an exception occurred when");

    BaseServiceLogRequest request = new BaseServiceLogQueryRequest();
    request.setIncludeQuery("[{\"log_message\":\"\\\"Unhandled  exception\\\"\"}]");
    SolrDocumentList found = executeQuery(request);

    assertThat(found, hasSize(3));
    assertThat(found, hasItem(solrDoc("0")));
    assertThat(found, hasItem(solrDoc("1")));
    assertThat(found, hasItem(solrDoc("2")));
  }

  @Test
  public void testIncludeEMailAddress() throws Exception {
    solr.addDoc("0", "Email address: john@hortonworks.com");
    solr.addDoc("1", "Another document doe@hortonworks.com");
    solr.addDoc("2", "Another document without email address");
    solr.addDoc("3", "Just a name and a domain name: john hortonworks.com");
    solr.addDoc("4", "Hi name is John Domain name: hortonworks.com");
    BaseServiceLogRequest request = new BaseServiceLogQueryRequest();
    request.setIncludeQuery("[{\"log_message\":\"\\\"john@hortonworks.com\\\"\"}]");
    SolrDocumentList found = executeQuery(request);

    assertThat(found, hasSize(2));
    assertThat(found, hasItem(solrDoc("0")));
    assertThat(found, hasItem(solrDoc("3")));
  }


  private final ServiceLogLevelDateRangeRequestQueryConverter dateRangeRequestQueryConverter = new ServiceLogLevelDateRangeRequestQueryConverter();

  private ServiceGraphRequest serviceGraphRequest(String includeQuery) {
    ServiceGraphRequest request = new ServiceGraphQueryRequest();
    request.setFrom(SOLR_DATETIME_FORMATTER.format(OffsetDateTime.now(ZoneId.of("UTC")).minusDays(1)));
    request.setTo(SOLR_DATETIME_FORMATTER.format(OffsetDateTime.now(ZoneId.of("UTC")).plusDays(1)));
    request.setIncludeQuery(includeQuery);
    return request;
  }

  private NamedList<List<PivotField>> executeQuery(ServiceGraphRequest request) throws IOException, SolrServerException {
    return solr.executeFacetQuery(dateRangeRequestQueryConverter.convert(request));
  }

  @Test
  public void testDateRangeRequestIncludeLogMessageFilter() throws Exception {
    solr.addDoc("0", "Unhandled exception");
    solr.addDoc("1", "exception occurred");
    solr.addDoc("2", "exception");
    solr.addDoc("3", "Unhandled error");
    solr.addDoc("4", "Error occurred");

    ServiceGraphRequest request = serviceGraphRequest("[{\"log_message\":\"exception\"}]");
    NamedList<List<PivotField>> found = executeQuery(request);

    assertThat(found.size(), is(1));
    assertThat(found.get("level").get(0).getCount(), is(3));
  }

  @Test
  public void testDateRangeRequestIncludeMultipleLogMessageFilterContainsWildcard() throws Exception {
    solr.addDoc("0", "Unhandled exception");
    solr.addDoc("1", "exception occurred");
    solr.addDoc("2", "exception");
    solr.addDoc("3", "Unhandled error");
    solr.addDoc("4", "Error occurred");

    ServiceGraphRequest request = serviceGraphRequest("[{\"log_message\":\"exceptio*\"},{\"log_message\":\"unha*\"}]");
    NamedList<List<PivotField>> found = executeQuery(request);

    assertThat(found.size(), is(1));
    assertThat(found.get("level").get(0).getCount(), is(1));
  }

  @Test
  public void testDateRangeRequestIncludeMultipleTerms() throws Exception {
    solr.addDoc("0", "At line 112 an unhandled exception occurred when");
    solr.addDoc("1", "exception occurred");
    solr.addDoc("2", "exception");
    solr.addDoc("3", "Unhandled error");
    solr.addDoc("4", "Error occurred");

    ServiceGraphRequest request = serviceGraphRequest("[{\"log_message\":\"\\\"Unhandled exception\\\"\"}]");
    NamedList<List<PivotField>> found = executeQuery(request);

    assertThat(found.size(), is(1));
    assertThat(found.get("level").get(0).getCount(), is(1));
  }

  @Test
  public void testDateRangeRequestIncludeMultipleTermsDelimitedByDash() throws Exception {
    solr.addDoc("0", "Unhandled exception");
    solr.addDoc("1", "At line 112 an unhandled-exception occurred when");
    solr.addDoc("2", "exception occurred");
    solr.addDoc("3", "exception");
    solr.addDoc("4", "Unhandled error");
    solr.addDoc("5", "Error occurred");

    ServiceGraphRequest request = serviceGraphRequest("[{\"log_message\":\"\\\"Unhandled-exception\\\"\"}]");
    NamedList<List<PivotField>> found = executeQuery(request);

    assertThat(found.size(), is(1));
    assertThat(found.get("level").get(0).getCount(), is(2));
  }

  @Test
  public void testDateRangeRequestIncludeMultipleTermsDelimitedByPlusSign() throws Exception {
    solr.addDoc("0", "Unhandled exception");
    solr.addDoc("1", "Unhandled+exception");
    solr.addDoc("2", "Unhandled-exception");
    solr.addDoc("3", "exception occurred");
    solr.addDoc("4", "exception");
    solr.addDoc("5", "Unhandled error");
    solr.addDoc("6", "Error occurred");

    ServiceGraphRequest request = serviceGraphRequest("[{\"log_message\":\"\\\"Unhandled+exception\\\"\"}]");
    NamedList<List<PivotField>> found = executeQuery(request);

    assertThat(found.size(), is(1));
    assertThat(found.get("level").get(0).getCount(), is(3));
  }

  @Test
  public void testDateRangeRequestIncludeALongMessage() throws Exception {
    solr.addDoc("6", "[   ] org.apache.solr.cloud.autoscaling.OverseerTriggerThread (OverseerTriggerThread.java:400) - Error in trigger 'index_size_trigger' configuration, trigger config ignored: {\\r\\n  \\\"aboveBytes\\\":53687091200,\\r\\n  \\\"aboveOp\\\":\\\"SPLITSHARD\\\",\\r\\n  \\\"event\\\":\\\"indexSize\\\",\\r\\n  \\\"waitFor\\\":1,\\r\\n  \\\"actions\\\":[\\r\\n    {\\r\\n      \\\"name\\\":\\\"compute_plan\\\",\\r\\n      \\\"class\\\":\\\"solr.ComputePlanAction\\\"},\\r\\n    {\\r\\n      \\\"name\\\":\\\"execute_plan\\\",\\r\\n      \\\"class\\\":\\\"solr.ExecutePlanAction\\\"}],\\r\\n  \\\"enabled\\\":true}\\r\\norg.apache.solr.cloud.autoscaling.TriggerValidationException: null\\r\\n\\tat org.apache.solr.cloud.autoscaling.TriggerBase.configure(TriggerBase.java:118) ~[solr-core-7.5.0.jar:7.5.0 b5bf70b7e32d7ddd9742cc821d471c5fabd4e3df - jimczi - 2018-09-18 13:07:55]\\r\\n\\tat org.apache.solr.cloud.autoscaling.IndexSizeTrigger.configure(IndexSizeTrigger.java:87) ~[solr-core-7.5.0.jar:7.5.0 b5bf70b7e32d7ddd9742cc821d471c5fabd4e3df - jimczi - 2018-09-18 13:07:55]\\r\\n\\tat org.apache.solr.cloud.autoscaling.AutoScaling$TriggerFactoryImpl.create(AutoScaling.java:189) ~[solr-core-7.5.0.jar:7.5.0 b5bf70b7e32d7ddd9742cc821d471c5fabd4e3df - jimczi - 2018-09-18 13:07:55]\\r\\n\\tat org.apache.solr.cloud.autoscaling.OverseerTriggerThread.loadTriggers(OverseerTriggerThread.java:398) [solr-core-7.5.0.jar:7.5.0 b5bf70b7e32d7ddd9742cc821d471c5fabd4e3df - jimczi - 2018-09-18 13:07:55]\\r\\n\\tat org.apache.solr.cloud.autoscaling.OverseerTriggerThread.refreshAutoScalingConf(OverseerTriggerThread.java:335) [solr-core-7.5.0.jar:7.5.0 b5bf70b7e32d7ddd9742cc821d471c5fabd4e3df - jimczi - 2018-09-18 13:07:55]\\r\\n\\tat org.apache.solr.cloud.autoscaling.OverseerTriggerThread.run(OverseerTriggerThread.java:161) [solr-core-7.5.0.jar:7.5.0 b5bf70b7e32d7ddd9742cc821d471c5fabd4e3df - jimczi - 2018-09-18 13:07:55]\\r\\n\\tat java.lang.Thread.run(Thread.java:745) [?:1.8.0_112]");
    solr.addDoc("7", "[   ] org.apache.solr.cloud.autoscaling.OverseerTriggerThread (OverseerTriggerThread.java:422) - Something else");
    solr.addDoc("8", "[   ] org.apache.solr.cloud.OverseerTriggerThread (OverseerTriggerThread.java:400) - Different package");

    ServiceGraphRequest request = serviceGraphRequest("[{\"log_message\":\"\\\"org.apache.solr.cloud.autoscaling.OverseerTriggerThread (OverseerTriggerThread.java:400)\\\"\"}]");
    NamedList<List<PivotField>> found = executeQuery(request);

    assertThat(found.size(), is(1));
    assertThat(found.get("level").get(0).getCount(), is(1));
  }

  @Test
  public void testDateRangeRequestIncludeMultipleWhitespaceDelimitedWords() throws Exception {
    solr.addDoc("0", "At line 112 an unhandled exception occurred when");
    solr.addDoc("1", "At line 112 an unhandled  exception occurred when");
    solr.addDoc("2", "At line 112 an unhandled   \texception occurred when");
    solr.addDoc("3", "At line 112 an exception occurred when");

    ServiceGraphRequest request = serviceGraphRequest("[{\"log_message\":\"\\\"Unhandled  exception\\\"\"}]");
    NamedList<List<PivotField>> found = executeQuery(request);

    assertThat(found.size(), is(1));
    assertThat(found.get("level").get(0).getCount(), is(3));
  }

  @Test
  public void testSearchTermEndsWithDot() throws Exception {
    solr.addDoc("0", "Caught exception checkIn.");
    solr.addDoc("1", "Caught exception checkIn");
    solr.addDoc("2", "Caught exception other");

    BaseServiceLogRequest request = new BaseServiceLogQueryRequest();
    request.setIncludeQuery("[{\"log_message\":\"\\\"checkIn\\\"\"}]");
    SolrDocumentList found = executeQuery(request);

    assertThat(found, hasSize(2));
    assertThat(found, hasItem(solrDoc("0")));
    assertThat(found, hasItem(solrDoc("1")));
  }

  @Test
  public void testSearchPhraseContainsStar() throws Exception {
    solr.addDoc("0", "Caught exception- checkIn");

    BaseServiceLogRequest request = new BaseServiceLogQueryRequest();
    request.setIncludeQuery("[{\"log_message\":\"\\\"Caught exception*\\\"\"}]");
    SolrDocumentList found = executeQuery(request);

    assertThat(found, hasSize(1));
    assertThat(found, hasItem(solrDoc("0")));
  }
}
