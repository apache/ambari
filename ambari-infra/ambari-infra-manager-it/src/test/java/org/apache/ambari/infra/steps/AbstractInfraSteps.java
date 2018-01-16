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
package org.apache.ambari.infra.steps;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import org.apache.ambari.infra.InfraClient;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.jbehave.core.annotations.AfterStories;
import org.jbehave.core.annotations.BeforeStories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.function.BooleanSupplier;

import static java.lang.System.currentTimeMillis;

public abstract class AbstractInfraSteps {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractInfraSteps.class);

  private static final int SOLR_PORT = 8983;
  private static final int INFRA_MANAGER_PORT = 61890;
  private static final int FAKE_S3_PORT = 4569;
  private static final String AUDIT_LOGS_COLLECTION = "audit_logs";
  protected static final String S3_BUCKET_NAME = "testbucket";
  private String ambariFolder;
  private String shellScriptLocation;
  private String dockerHost;
  private SolrClient solrClient;
  private AmazonS3Client s3client;
  private int documentId = 0;

  public InfraClient getInfraClient() {
    return new InfraClient(String.format("http://%s:%d/api/v1/jobs", dockerHost, INFRA_MANAGER_PORT));
  }

  public SolrClient getSolrClient() {
    return solrClient;
  }

  public AmazonS3Client getS3client() {
    return s3client;
  }

  @BeforeStories
  public void initDockerContainer() throws Exception {
    LOG.info("Create new docker container for testing Ambari Infra Manager ...");
    URL location = AbstractInfraSteps.class.getProtectionDomain().getCodeSource().getLocation();
    ambariFolder = new File(location.toURI()).getParentFile().getParentFile().getParentFile().getParent();
    shellScriptLocation = ambariFolder + "/ambari-infra/ambari-infra-manager/docker/infra-manager-docker-compose.sh";

    runCommand(new String[]{shellScriptLocation, "start"});

    dockerHost = System.getProperty("docker.host") != null ? System.getProperty("docker.host") : "localhost";

    waitUntilSolrIsUp();

    solrClient = new LBHttpSolrClient.Builder().withBaseSolrUrls(String.format("http://%s:%d/solr/%s_shard1_replica1",
            dockerHost,
            SOLR_PORT,
            AUDIT_LOGS_COLLECTION)).build();

    LOG.info("Creating collection");
    runCommand(new String[]{"docker", "exec", "docker_solr_1", "solr", "create_collection", "-c", AUDIT_LOGS_COLLECTION, "-d", "configsets/"+ AUDIT_LOGS_COLLECTION +"/conf", "-n", AUDIT_LOGS_COLLECTION + "_conf"});

    LOG.info("Initializing s3 client");
    s3client = new AmazonS3Client(new BasicAWSCredentials("remote-identity", "remote-credential"));
    s3client.setEndpoint(String.format("http://%s:%d", dockerHost, FAKE_S3_PORT));
    s3client.createBucket(S3_BUCKET_NAME);

    checkInfraManagerReachable();
  }

  protected void runCommand(String[] command) {
    try {
      LOG.info("Exec command: {}", StringUtils.join(command, " "));
      Process process = Runtime.getRuntime().exec(command);
      String stdout = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
      LOG.info("Exec command result {}", stdout);
    } catch (Exception e) {
      throw new RuntimeException("Error during execute shell command: ", e);
    }
  }

  private void waitUntilSolrIsUp() throws Exception {
    try(CloseableHttpClient httpClient = HttpClientBuilder.create().setRetryHandler(new DefaultHttpRequestRetryHandler(0, false)).build()) {
      doWithin(60, "Start Solr", () -> pingSolr(httpClient));
    }
  }

  protected void doWithin(int sec, String actionName, BooleanSupplier predicate) {
    doWithin(sec, actionName, () -> {
      if (!predicate.getAsBoolean())
        throw new RuntimeException("Predicate was false!");
    });
  }

  protected void doWithin(int sec, String actionName, Runnable runnable) {
    long start = currentTimeMillis();
    Exception exception;
    while (true) {
      try {
        runnable.run();
        return;
      }
      catch (Exception e) {
        exception = e;
      }

      if (currentTimeMillis() - start > sec * 1000) {
        throw new AssertionError(String.format("Unable to perform action '%s' within %d seconds", actionName, sec), exception);
      }
      else {
        LOG.info("Performing action '{}' failed. retrying...", actionName);
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    }
  }

  private boolean pingSolr(CloseableHttpClient httpClient) {
    try (CloseableHttpResponse response = httpClient.execute(new HttpGet(String.format("http://%s:%d/solr/admin/collections?action=LIST", dockerHost, SOLR_PORT)))) {
      return response.getStatusLine().getStatusCode() == 200;
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void checkInfraManagerReachable() throws Exception {
    try (InfraClient httpClient = getInfraClient()) {
      doWithin(30, "Start Ambari Infra Manager", httpClient::getJobs);
      LOG.info("Ambari Infra Manager is up and running");
    }
  }

  protected void addDocument(OffsetDateTime logtime) throws SolrServerException, IOException {
    SolrInputDocument solrInputDocument = new SolrInputDocument();
    solrInputDocument.addField("logType", "HDFSAudit");
    solrInputDocument.addField("cluster", "cl1");
    solrInputDocument.addField("event_count", 1);
    solrInputDocument.addField("repo", "hdfs");
    solrInputDocument.addField("reqUser", "ambari-qa");
    solrInputDocument.addField("type", "hdfs_audit");
    solrInputDocument.addField("seq_num", 9);
    solrInputDocument.addField("result", 1);
    solrInputDocument.addField("path", "/root/test-logs/hdfs-audit/hdfs-audit.log");
    solrInputDocument.addField("ugi", "ambari-qa (auth:SIMPLE)");
    solrInputDocument.addField("host", "logfeeder.apache.org");
    solrInputDocument.addField("action", "getfileinfo");
    solrInputDocument.addField("log_message", "allowed=true\tugi=ambari-qa (auth:SIMPLE)\tip=/192.168.64.102\tcmd=getfileinfo\tsrc=/ats/active\tdst=null\tperm=null\tproto=rpc\tcallerContext=HIVE_QUERY_ID:ambari-qa_20160317200111_223b3079-4a2d-431c-920f-6ba37ed63e9f");
    solrInputDocument.addField("logger_name", "FSNamesystem.audit");
    solrInputDocument.addField("id", Integer.toString(documentId++));
    solrInputDocument.addField("authType", "SIMPLE");
    solrInputDocument.addField("logfile_line_number", 1);
    solrInputDocument.addField("cliIP", "/192.168.64.102");
    solrInputDocument.addField("level", "INFO");
    solrInputDocument.addField("resource", "/ats/active");
    solrInputDocument.addField("ip", "172.18.0.2");
    solrInputDocument.addField("evtTime", "2017-12-08T10:23:16.452Z");
    solrInputDocument.addField("req_caller_id", "HIVE_QUERY_ID:ambari-qa_20160317200111_223b3079-4a2d-431c-920f-6ba37ed63e9f");
    solrInputDocument.addField("repoType", 1);
    solrInputDocument.addField("enforcer", "hadoop-acl");
    solrInputDocument.addField("cliType", "rpc");
    solrInputDocument.addField("message_md5", "-6778765776916226588");
    solrInputDocument.addField("event_md5", "5627261521757462732");
    solrInputDocument.addField("logtime", new Date(logtime.toInstant().toEpochMilli()));
    solrInputDocument.addField("_ttl_", "+7DAYS");
    solrInputDocument.addField("_expire_at_", "2017-12-15T10:23:19.106Z");
    solrClient.add(solrInputDocument);
  }

  @AfterStories
  public void shutdownContainers() throws Exception {
    Thread.sleep(2000); // sync with s3 server
    ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(S3_BUCKET_NAME);
    ObjectListing objectListing = getS3client().listObjects(listObjectsRequest);
    LOG.info("Found {} files on s3.", objectListing.getObjectSummaries().size());
    objectListing.getObjectSummaries().forEach(s3ObjectSummary ->  LOG.info("Found file in s3 with key {}", s3ObjectSummary.getKey()));

    LOG.info("shutdown containers");
    runCommand(new String[]{shellScriptLocation, "stop"});
  }
}
