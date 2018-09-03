package org.apache.ambari.logsearch.web.filters;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.apache.ambari.logsearch.common.StatusMessage;
import org.apache.ambari.logsearch.conf.SolrPropsConfig;
import org.apache.ambari.logsearch.conf.SolrServiceLogPropsConfig;
import org.apache.ambari.logsearch.conf.global.SolrCollectionState;
import org.apache.ambari.logsearch.conf.global.SolrServiceLogsState;
import org.junit.Before;
import org.junit.Test;

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
public class GlobalStateTest {


  private SolrCollectionState solrCollectionState;
  private SolrPropsConfig solrPropsConfig;

  @Before
  public void setUp() {
    solrCollectionState = new SolrServiceLogsState();
    solrPropsConfig = new SolrServiceLogPropsConfig();
    solrPropsConfig.setCollection("test_collection");
    solrPropsConfig.setConfigName("test_config");
  }

  @Test
  public void testGetStatusMessageReturnsNullIfZnodeAndSolrCollectionIsReady() {
    solrCollectionState.setZnodeReady(true);
    solrCollectionState.setSolrCollectionReady(true);
    solrCollectionState.setConfigurationUploaded(true);

    GlobalStateProvider globalState = new GlobalStateProvider(solrCollectionState, solrPropsConfig);
    assertThat(globalState.getStatusMessage("/api/v1/test"), is(nullValue()));
  }

  @Test
  public void testGetStatusMessageReturnsZnodeIsNotReady() {
    solrCollectionState.setZnodeReady(false);
    solrCollectionState.setConfigurationUploaded(false);
    solrCollectionState.setSolrCollectionReady(false);

    GlobalStateProvider globalState = new GlobalStateProvider(solrCollectionState, solrPropsConfig);
    StatusMessage statusMessage = globalState.getStatusMessage("/api/v1/test");
    assertThat(statusMessage.getMessage().contains("ZNode is not available"), is(true));
  }

  @Test
  public void testGetStatusMessageReturnsZkConfingNotReady() {
    solrCollectionState.setZnodeReady(true);
    solrCollectionState.setConfigurationUploaded(false);
    solrCollectionState.setSolrCollectionReady(false);

    GlobalStateProvider globalState = new GlobalStateProvider(solrCollectionState, solrPropsConfig);
    StatusMessage statusMessage = globalState.getStatusMessage("/api/v1/test");
    assertThat(statusMessage.getMessage().contains("Collection configuration has not uploaded yet"), is(true));
  }

  @Test
  public void testGetStatusMessageReturnsSolrCollectionNotReady() {
    solrCollectionState.setZnodeReady(true);
    solrCollectionState.setConfigurationUploaded(true);
    solrCollectionState.setSolrCollectionReady(false);

    GlobalStateProvider globalState = new GlobalStateProvider(solrCollectionState, solrPropsConfig);
    StatusMessage statusMessage = globalState.getStatusMessage("/api/v1/test");
    assertThat(statusMessage.getMessage().contains("Solr has not accessible yet"), is(true));
  }
}