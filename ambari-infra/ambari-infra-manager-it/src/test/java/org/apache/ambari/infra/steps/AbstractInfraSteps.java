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

import static org.apache.ambari.infra.Solr.AUDIT_LOGS_COLLECTION;
import static org.apache.ambari.infra.Solr.HADOOP_LOGS_COLLECTION;
import static org.apache.ambari.infra.TestUtil.doWithin;
import static org.apache.ambari.infra.TestUtil.getDockerHost;
import static org.apache.ambari.infra.TestUtil.runCommand;

import java.io.File;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

import org.apache.ambari.infra.InfraClient;
import org.apache.ambari.infra.S3Client;
import org.apache.ambari.infra.Solr;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.jbehave.core.annotations.AfterStories;
import org.jbehave.core.annotations.BeforeStories;

import spark.resource.ClassPathResource;

public abstract class AbstractInfraSteps {
  private static final Logger logger = LogManager.getLogger(AbstractInfraSteps.class);

  private static final int INFRA_MANAGER_PORT = 61890;
  private static final int FAKE_S3_PORT = 4569;
  protected static final String S3_BUCKET_NAME = "testbucket";
  private String ambariFolder;
  private String shellScriptLocation;
  private S3Client s3client;
  private int documentId = 0;
  private Solr solr;
  private InfraClient infraClient;

  public InfraClient getInfraClient() {
    return infraClient;
  }

  public Solr getSolr() {
    return solr;
  }

  public S3Client getS3client() {
    return s3client;
  }

  public String getLocalDataFolder() {
    return ambariFolder + "/ambari-infra/ambari-infra-manager/docker/test-out";
  }

  public String getInfraManagerConfDir() {
    return ambariFolder + "/ambari-infra/ambari-infra-manager/target/package/conf";
  }

  @BeforeStories
  public void initDockerContainer() throws Exception {
    System.setProperty("HADOOP_USER_NAME", "root");

    URL location = AbstractInfraSteps.class.getProtectionDomain().getCodeSource().getLocation();
    ambariFolder = new File(location.toURI()).getParentFile().getParentFile().getParentFile().getParent();

    String localDataFolder = getLocalDataFolder();
    if (new File(localDataFolder).exists()) {
      logger.info("Clean local data folder {}", localDataFolder);
      FileUtils.cleanDirectory(new File(localDataFolder));
    }

    logger.info("Copy resources");
    FileUtils.copyDirectory(new ClassPathResource("conf").getFile(), new File(getInfraManagerConfDir()));

    shellScriptLocation = ambariFolder + "/ambari-infra/ambari-infra-manager/docker/infra-manager-docker-compose.sh";
    logger.info("Create new docker container for testing Ambari Infra Manager ...");
    runCommand(new String[]{shellScriptLocation, "start"});

    String dockerHost = getDockerHost();

    this.infraClient = new InfraClient(String.format("http://%s:%d/api/v1", dockerHost, INFRA_MANAGER_PORT));

    solr = new Solr();
    solr.waitUntilSolrIsUp();

    solr.createSolrCollection(AUDIT_LOGS_COLLECTION);
    solr.createSolrCollection(HADOOP_LOGS_COLLECTION);

    logger.info("Initializing s3 client");
    s3client = new S3Client(dockerHost, FAKE_S3_PORT, S3_BUCKET_NAME);
    s3client.createBucket();

    checkInfraManagerReachable();
  }

  private void checkInfraManagerReachable() {
    InfraClient infraClient = getInfraClient();
    doWithin(30, "Start Ambari Infra Manager", infraClient::getJobs);
    logger.info("Ambari Infra Manager is up and running");
  }

  protected SolrInputDocument addDocument(OffsetDateTime logtime) {
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
    solrInputDocument.addField("req_caller_id", "HIVE_QUERY_ID:ambari-qa_20160317200111_223b3079-4a2d-431c-920f-6ba37ed63e9f");
    solrInputDocument.addField("repoType", 1);
    solrInputDocument.addField("enforcer", "hadoop-acl");
    solrInputDocument.addField("cliType", "rpc");
    solrInputDocument.addField("message_md5", "-6778765776916226588");
    solrInputDocument.addField("event_md5", "5627261521757462732");
    solrInputDocument.addField("logtime", new Date(logtime.toInstant().toEpochMilli()));
    solrInputDocument.addField("evtTime", new Date(logtime.toInstant().toEpochMilli()));
    solrInputDocument.addField("_ttl_", "+7DAYS");
    solrInputDocument.addField("_expire_at_", new Date(logtime.plusDays(7).toInstant().toEpochMilli()));
    solr.add(solrInputDocument);
    return solrInputDocument;
  }

  @AfterStories
  public void shutdownContainers() throws Exception {
    Thread.sleep(2000); // sync with s3 server
    List<String> objectKeys = getS3client().listObjectKeys();
    logger.info("Found {} files on s3.", objectKeys.size());
    objectKeys.forEach(objectKey ->  logger.info("Found file on s3 with key {}", objectKey));

    logger.info("shutdown containers");
    runCommand(new String[]{shellScriptLocation, "stop"});
  }
}
