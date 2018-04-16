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
package org.apache.ambari.logsearch.steps;

import org.apache.ambari.logsearch.domain.StoryDataRegistry;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrDocumentList;
import org.jbehave.core.annotations.AfterStories;
import org.jbehave.core.annotations.BeforeStories;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

public class LogSearchDockerSteps extends AbstractLogSearchSteps {

  private static final Logger LOG = LoggerFactory.getLogger(LogSearchDockerSteps.class);

  @Given("logsearch docker container")
  public void setupLogSearchContainer() throws Exception {
    initDockerContainer();
  }

  @When("logfeeder started (parse logs & send data to solr)")
  public void logfeederStarted() throws Exception {
    // TODO: run ps aux to check LogFeeder process with docker exec
  }

  @BeforeStories
  public void initDocker() throws Exception {
    // TODO: check docker is up
  }

  @AfterStories
  public void removeLogSearchContainer() {
    runCommand(StoryDataRegistry.INSTANCE.getShellScriptFolder(), new String[]{StoryDataRegistry.INSTANCE.getShellScriptLocation(), "stop"});
  }
}
