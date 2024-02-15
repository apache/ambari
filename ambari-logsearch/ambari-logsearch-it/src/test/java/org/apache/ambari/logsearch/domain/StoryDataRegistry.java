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
package org.apache.ambari.logsearch.domain;

import org.apache.ambari.logsearch.config.json.model.inputconfig.impl.InputAdapter;
import org.apache.solr.client.solrj.SolrClient;
import org.jbehave.web.selenium.WebDriverProvider;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class StoryDataRegistry {
  public static final StoryDataRegistry INSTANCE = new StoryDataRegistry();

  public static final String CLUSTER = "cl1";
  public static final String LOGSEARCH_GLOBAL_CONFIG = "[\n" +
          "    {\n" +
          "      \"add_fields\": {\n" +
          "        \"cluster\": \""+ CLUSTER +"\"\n" +
          "      },\n" +
          "      \"source\": \"file\",\n" +
          "      \"tail\": \"true\",\n" +
          "      \"gen_event_md5\": \"true\"\n" +
          "    }\n" +
          "]";


  private SolrClient solrClient;
  private boolean logsearchContainerStarted = false;
  private String dockerHost;
  private String ambariFolder;
  private String shellScriptLocation;
  private String shellScriptFolder;
  private final int solrPort = 8886;
  private final int logsearchPort = 61888;
  private final int zookeeperPort = 9983;
  private final String serviceLogsCollection = "hadoop_logs";
  private final String auditLogsCollection = "audit_logs";
  private WebDriverProvider webDriverProvider;

  private StoryDataRegistry() {
    JsonParser jsonParser = new JsonParser();
    JsonElement globalConfigJsonElement = jsonParser.parse(LOGSEARCH_GLOBAL_CONFIG);
    InputAdapter.setGlobalConfigs(globalConfigJsonElement.getAsJsonArray());
  }

  public String getDockerHost() {
    return dockerHost;
  }

  public void setDockerHost(String dockerHost) {
    this.dockerHost = dockerHost;
  }

  public int getSolrPort() {
    return solrPort;
  }

  public int getLogsearchPort() {
    return logsearchPort;
  }

  public int getZookeeperPort() {
    return zookeeperPort;
  }

  public String getServiceLogsCollection() {
    return serviceLogsCollection;
  }

  public String getAuditLogsCollection() {
    return auditLogsCollection;
  }

  public SolrClient getSolrClient() {
    return solrClient;
  }

  public void setSolrClient(SolrClient solrClient) {
    this.solrClient = solrClient;
  }

  public String getAmbariFolder() {
    return ambariFolder;
  }

  public void setAmbariFolder(String ambariFolder) {
    this.ambariFolder = ambariFolder;
  }

  public String getShellScriptLocation() {
    return shellScriptLocation;
  }

  public void setShellScriptLocation(String shellScriptLocation) {
    this.shellScriptLocation = shellScriptLocation;
  }

  public boolean isLogsearchContainerStarted() {
    return logsearchContainerStarted;
  }

  public void setLogsearchContainerStarted(boolean logsearchContainerStarted) {
    this.logsearchContainerStarted = logsearchContainerStarted;
  }

  public WebDriverProvider getWebDriverProvider() {
    return webDriverProvider;
  }

  public void setWebDriverProvider(WebDriverProvider webDriverProvider) {
    this.webDriverProvider = webDriverProvider;
  }

  public String getShellScriptFolder() {
    return shellScriptFolder;
  }

  public void setShellScriptFolder(String shellScriptFolder) {
    this.shellScriptFolder = shellScriptFolder;
  }

  public WebClient logsearchClient() {
    return new WebClient(dockerHost, logsearchPort);
  }
}
