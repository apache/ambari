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

public class LogSearchDockerSteps {

  private static final Logger LOG = LoggerFactory.getLogger(LogSearchDockerSteps.class);

  @Given("logsearch docker container")
  public void setupLogSearchContainer() throws Exception {
    boolean logsearchStarted = StoryDataRegistry.INSTANCE.isLogsearchContainerStarted();
    if (!logsearchStarted) {
      LOG.info("Create new docker container for Log Search ..");
      URL location = LogSearchDockerSteps.class.getProtectionDomain().getCodeSource().getLocation();
      String ambariFolder = new File(location.toURI()).getParentFile().getParentFile().getParentFile().getParent();
      StoryDataRegistry.INSTANCE.setAmbariFolder(ambariFolder);
      String shellScriptLocation = ambariFolder + "/ambari-logsearch/docker/logsearch-docker.sh";
      StoryDataRegistry.INSTANCE.setShellScriptLocation(shellScriptLocation);
      String output = runCommand(new String[]{StoryDataRegistry.INSTANCE.getShellScriptLocation(), "start"});
      LOG.info("Command output: {}", output);
      StoryDataRegistry.INSTANCE.setLogsearchContainerStarted(true);

      // TODO: create a script which returns the proper host for docker, use: runCommand or an env variable
      String dockerHostFromUri = "localhost";

      StoryDataRegistry.INSTANCE.setDockerHost(dockerHostFromUri);
      checkHostAndPortReachable(dockerHostFromUri, StoryDataRegistry.INSTANCE.getLogsearchPort(), "LogSearch");
      waitUntilSolrIsUp();
      waitUntilSolrHasAnyData();

      LOG.info("Waiting for logfeeder to finish the test log parsings... (10 sec)");
      Thread.sleep(10000);
    }
  }

  @When("logfeeder started (parse logs & send data to solr)")
  public void logfeederStarted() throws Exception {
    // TODO: run ps aux to check LogFeeder process with docker exec
  }

  @BeforeStories
  public void checkDockerApi() {
    // TODO: check docker is up
  }

  @AfterStories
  public void removeLogSearchContainer() {
    runCommand(new String[]{StoryDataRegistry.INSTANCE.getShellScriptLocation(), "stop"});
  }

  private void waitUntilSolrIsUp() throws Exception {
    int maxTries = 30;
    boolean solrIsUp = false;
    for (int tries = 1; tries < maxTries; tries++) {
      try {
        SolrClient solrClient = new LBHttpSolrClient(String.format("http://%s:%d/solr/%s_shard0_replica1",
          StoryDataRegistry.INSTANCE.getDockerHost(),
          StoryDataRegistry.INSTANCE.getSolrPort(),
          StoryDataRegistry.INSTANCE.getServiceLogsCollection()));
        StoryDataRegistry.INSTANCE.setSolrClient(solrClient);
        SolrPingResponse pingResponse = solrClient.ping();
        if (pingResponse.getStatus() != 0) {
          LOG.info("Solr is not up yet, retrying... ({})", tries);
          Thread.sleep(2000);
        } else {
          solrIsUp = true;
          LOG.info("Solr is up and running");
          break;
        }
      } catch (Exception e) {
        LOG.error("Error occurred during pinging solr ({}). retrying {} times", e.getMessage(), tries);
        Thread.sleep(2000);
      }
    }

    if (!solrIsUp) {
      throw new IllegalStateException(String.format("Solr is not up after %d tries", maxTries));
    }
  }

  private void waitUntilSolrHasAnyData() throws IOException, SolrServerException, InterruptedException {
    boolean solrHasData = false;

    int maxTries = 60;
    for (int tries = 1; tries < maxTries; tries++) {
      try {
        SolrClient solrClient = StoryDataRegistry.INSTANCE.getSolrClient();
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("*:*");
        QueryResponse queryResponse = solrClient.query(solrQuery);
        SolrDocumentList list = queryResponse.getResults();
        if (list.size() > 0) {
          solrHasData = true;
          break;
        } else {
          Thread.sleep(2000);
          LOG.info("Solr has no data yet, retrying... ({} tries)", tries);
        }
      } catch (Exception e) {
        LOG.error("Error occurred during checking solr ({}). retrying {} times", e.getMessage(), tries);
        Thread.sleep(2000);
      }
    }
    if (!solrHasData) {
      throw new IllegalStateException(String.format("Solr has no data after %d tries", maxTries));
    }
  }


  private void checkHostAndPortReachable(String host, int port, String serviceName) throws InterruptedException {
    boolean reachable = false;
    int maxTries = 60;
    for (int tries = 1; tries < maxTries; tries++ ) {
      try (Socket socket = new Socket()) {
        socket.connect(new InetSocketAddress(host, port), 1000);
        reachable = true;
        break;
      } catch (IOException e) {
        Thread.sleep(2000);
        LOG.info("{} is not reachable yet, retrying..", serviceName);
      }
    }
    if (!reachable) {
      throw new IllegalStateException(String.format("%s is not reachable after %s tries", serviceName, maxTries));
    }
  }


  private String runCommand(String[] command) {
    try {
      Process process = Runtime.getRuntime().exec(command);
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      return reader.readLine();
    } catch (Exception e) {
      throw new RuntimeException("Error during execute shell command: ", e);
    }
  }
}
