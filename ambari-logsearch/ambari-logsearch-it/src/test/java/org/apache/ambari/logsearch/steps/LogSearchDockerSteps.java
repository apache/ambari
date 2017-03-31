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

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.google.common.base.Preconditions;
import org.apache.ambari.logsearch.domain.StoryDataRegistry;
import org.apache.commons.lang.ArrayUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.jbehave.core.annotations.AfterStories;
import org.jbehave.core.annotations.BeforeStories;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.List;

public class LogSearchDockerSteps {

  private static final Logger LOG = LoggerFactory.getLogger(LogSearchDockerSteps.class);

  @Given("logsearch docker container")
  public void setupLogSearchContainer() throws Exception {
    boolean logsearchStarted = StoryDataRegistry.INSTANCE.isLogsearchContainerStarted();
    if (!logsearchStarted) {
      DockerClient dockerClient = StoryDataRegistry.INSTANCE.getDockerClient();
      LOG.info("Create new docker container for Log Search ..");
      URL location = LogSearchDockerSteps.class.getProtectionDomain().getCodeSource().getLocation();
      String ambariFolder = new File(location.toURI()).getParentFile().getParentFile().getParentFile().getParent();
      StoryDataRegistry.INSTANCE.setAmbariFolder(ambariFolder);
      String dockerBaseDirectory = ambariFolder + "/ambari-logsearch/docker";
      String dockerFileLocation = dockerBaseDirectory + "/Dockerfile";

      String imageId = dockerClient.buildImageCmd()
        .withTag("ambari-logsearch:v1.0")
        .withBaseDirectory(new File(dockerBaseDirectory))
        .withDockerfile(new File(dockerFileLocation))
        .exec(new BuildImageResultCallback())
        .awaitImageId();
      LOG.info("Docker image id: {}", imageId);

      removeLogSearchContainerIfExists();

      // volume bindings
      Volume testLogsVolume = new Volume("/root/test-logs");
      Volume testConfigVolume = new Volume("/root/test-config");
      Volume ambariVolume = new Volume("/root/ambari");
      Volume logfeederClassesVolume = new Volume("/root/ambari/ambari-logsearch/ambari-logsearch-logfeeder/target/package/classes");
      Volume logsearchClassesVolume = new Volume("/root/ambari/ambari-logsearch/ambari-logsearch-portal/target/package/classes");
      Volume logsearchWebappVolume = new Volume("/root/ambari/ambari-logsearch/ambari-logsearch-portal/target/package/classes/webapps/app");
      Bind testLogsBind = new Bind(ambariFolder +"/ambari-logsearch/docker/test-logs", testLogsVolume);
      Bind testConfigBind = new Bind(ambariFolder +"/ambari-logsearch/docker/test-config", testConfigVolume);
      Bind ambariRootBind = new Bind(ambariFolder, ambariVolume);
      Bind logfeederClassesBind = new Bind(ambariFolder + "/ambari-logsearch/ambari-logsearch-logfeeder/target/classes", logfeederClassesVolume);
      Bind logsearchClassesBind = new Bind(ambariFolder + "/ambari-logsearch/ambari-logsearch-portal/target/classes", logsearchClassesVolume);
      Bind logsearchWebappBind = new Bind(ambariFolder + "/ambari-logsearch/ambari-logsearch-portal/src/main/webapp", logsearchWebappVolume);

      // port bindings
      Ports ports = new Ports();
      ports.bind(new ExposedPort(5005), new Ports.Binding("0.0.0.0", "5005"));
      ports.bind(new ExposedPort(5006), new Ports.Binding("0.0.0.0", "5006"));
      ports.bind(new ExposedPort(StoryDataRegistry.INSTANCE.getSolrPort()), new Ports.Binding("0.0.0.0", "8886"));
      ports.bind(new ExposedPort(StoryDataRegistry.INSTANCE.getLogsearchPort()), new Ports.Binding("0.0.0.0", "61888"));
      ports.bind(new ExposedPort(StoryDataRegistry.INSTANCE.getZookeeperPort()), new Ports.Binding("0.0.0.0", "9983"));

      LOG.info("Creating docker cointainer...");
      CreateContainerResponse createResponse = dockerClient.createContainerCmd("ambari-logsearch:v1.0")
        .withHostName("logsearch.apache.org")
        .withName("logsearch")
        .withVolumes(testLogsVolume, testConfigVolume, ambariVolume, logfeederClassesVolume, logsearchClassesVolume, logsearchWebappVolume)
        .withBinds(testLogsBind, testConfigBind, ambariRootBind, logfeederClassesBind, logsearchClassesBind, logsearchWebappBind)
        .withExposedPorts(
          new ExposedPort(StoryDataRegistry.INSTANCE.getLogsearchPort()),
          new ExposedPort(5005),
          new ExposedPort(5006),
          new ExposedPort(StoryDataRegistry.INSTANCE.getSolrPort()),
          new ExposedPort(StoryDataRegistry.INSTANCE.getZookeeperPort()))
        .withPortBindings(ports)
        .exec();
      LOG.info("Created docker container id: {}", createResponse.getId());

      dockerClient.startContainerCmd(createResponse.getId()).exec();
      StoryDataRegistry.INSTANCE.setLogsearchContainerStarted(true);
      String dockerHostFromUri = StoryDataRegistry.INSTANCE.getDockerClientConfig().getDockerHost().getHost();
      StoryDataRegistry.INSTANCE.setDockerHost(dockerHostFromUri);
      checkHostAndPortReachable(dockerHostFromUri, StoryDataRegistry.INSTANCE.getLogsearchPort(), "LogSearch");
      waitUntilSolrHasAnyData();

      LOG.info("Waiting for logfeeder to finish the test log parsings... (10 sec)");
      Thread.sleep(10000);
    }
  }

  @When("logfeeder started (parse logs & send data to solr)")
  public void logfeederStarted() throws Exception {
    // TODO: run ps aux to check LogFeeder process with docker exec
    /**
    DockerClient dockerClient = StoryDataRegistry.INSTANCE.getDockerClient();
    ExecCreateCmdResponse execResp = dockerClient
      .execCreateCmd(containerId)
      .withAttachStdout(true)
      .withCmd("ps", "aux").exec();
    execResp.getId();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ExecStartResultCallback res = dockerClient
      .execStartCmd(execResp.getId())
      .withDetach(true)
      .withTty(true)
      .exec(new ExecStartResultCallback(outputStream,  outputStream)).awaitCompletion();
     **/
  }

  @BeforeStories
  public void checkDockerApi() {
    LOG.info("Tries to setup docker client configuration");
    final String dockerHost = System.getenv("DOCKER_HOST");
    final String dockerCertPath = System.getenv("DOCKER_CERT_PATH");
    final String dockerApiVersion = System.getenv("DOCKER_API_VERSION") == null ? "1.20" : System.getenv("DOCKER_API_VERSION");

    Preconditions.checkArgument(dockerHost != null, "Set 'DOCKER_HOST' env variable");
    Preconditions.checkArgument(dockerCertPath != null, "Set 'DOCKER_CERT_PATH' env variable");
    LOG.info("DOCKER_HOST: {}", dockerHost);
    LOG.info("DOCKER_CERT_PATH: {}", dockerCertPath);
    LOG.info("DOCKER_API_VERSION: {}", dockerApiVersion);
    DockerClientConfig dockerClientConfig = DockerClientConfig.createDefaultConfigBuilder()
      .withDockerHost(dockerHost)
      .withDockerCertPath(dockerCertPath)
      .withApiVersion(dockerApiVersion)
      .withDockerTlsVerify(true)
      .build();
    StoryDataRegistry.INSTANCE.setDockerClientConfig(dockerClientConfig);
    DockerClient dockerClient = DockerClientBuilder.getInstance(dockerClientConfig).build();
    StoryDataRegistry.INSTANCE.setDockerClient(dockerClient);
    LOG.info("Docker client setup successfully.");
  }

  @AfterStories
  public void removeLogSearchContainer() {
    removeLogSearchContainerIfExists();
  }

  private void removeLogSearchContainerIfExists() {
    DockerClient dockerClient = StoryDataRegistry.INSTANCE.getDockerClient();
    List<Container> containerList = dockerClient
      .listContainersCmd()
      .withShowAll(true)
      .exec();

    boolean isLogSearchContainerExists = false;
    String containerId = null;
    for (Container container : containerList) {
      isLogSearchContainerExists = ArrayUtils.contains(container.getNames(), "/logsearch");
      if (isLogSearchContainerExists) {
        containerId = container.getId();
        break;
      }
    }

    if (isLogSearchContainerExists) {
      LOG.info("Remove logsearch container: {}", containerId);
      dockerClient.removeContainerCmd(containerId).withForce(true).exec();
    }
  }

  private void waitUntilSolrHasAnyData() throws IOException, SolrServerException, InterruptedException {
    boolean solrHasData = false;
    CloudSolrClient solrClient = new CloudSolrClient(String.format("%s:%d",
      StoryDataRegistry.INSTANCE.getDockerHost(),
      StoryDataRegistry.INSTANCE.getZookeeperPort()));
    StoryDataRegistry.INSTANCE.setCloudSolrClient(solrClient);
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("*:*");

    int maxTries = 60;
    for (int tries = 1; tries < maxTries; tries++) {
      QueryResponse queryResponse = solrClient.query(StoryDataRegistry.INSTANCE.getServiceLogsCollection(), solrQuery);
      SolrDocumentList list = queryResponse.getResults();
      if (list.size() > 0) {
        solrHasData = true;
        break;
      } else {
        Thread.sleep(2000);
        LOG.info("Solr has no data yet, retrying...");
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
}
