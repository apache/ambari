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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.apache.ambari.infra.OffsetDateTimeConverter.SOLR_DATETIME_FORMATTER;
import static org.apache.ambari.infra.TestUtil.doWithin;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.infra.InfraClient;
import org.apache.ambari.infra.S3Client;
import org.apache.ambari.infra.client.model.JobExecutionInfoResponse;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.jbehave.core.annotations.AfterScenario;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ExportJobsSteps extends AbstractInfraSteps {
  private static final Logger logger = LogManager.getLogger(ExportJobsSteps.class);
  private Set<String> documentIds = new HashSet<>();

  private Map<String, JobExecutionInfoResponse> launchedJobs = new HashMap<>();

  @Given("$count documents in solr")
  public void addDocuments(int count) {
    OffsetDateTime intervalEnd = OffsetDateTime.now();
    documentIds.clear();
    for (int i = 0; i < count; ++i) {
      documentIds.add(addDocument(intervalEnd.minusMinutes(i % (count / 10))).get("id").getValue().toString());
    }
    getSolr().commit();
  }

  @Given("$count documents in solr with logtime from $startLogtime to $endLogtime")
  public void addDocuments(long count, OffsetDateTime startLogtime, OffsetDateTime endLogtime) {
    Duration duration = Duration.between(startLogtime, endLogtime);
    long increment = duration.toNanos() / count;
    documentIds.clear();
    for (int i = 0; i < count; ++i) {
      documentIds.add(addDocument(startLogtime.plusNanos(increment * i)).get("id").getValue().toString());
    }
    getSolr().commit();
  }

  @Given("a file on s3 with key $key")
  public void addFileToS3(String key) {
    getS3client().putObject(key, "anything".getBytes());
  }

  @When("start $jobName job")
  public void startJob(String jobName) throws Exception {
    startJob(jobName, null, 0);
  }

  @When("start $jobName job with parameters $parameters after $waitSec seconds")
  public void startJob(String jobName, String parameters, int waitSec) throws Exception {
    Thread.sleep(waitSec * 1000);
    JobExecutionInfoResponse jobExecutionInfo = getInfraClient().startJob(jobName, parameters);
    logger.info("Job {} started: {}", jobName, jobExecutionInfo);
    launchedJobs.put(jobName, jobExecutionInfo);
  }

  @When("restart $jobName job within $waitSec seconds")
  public void restartJob(String jobName, int waitSec) {
    doWithin(waitSec, "Restarting job " + jobName, () ->
            getInfraClient().restartJob(jobName, launchedJobs.get(jobName).getJobInstanceId()));
  }

  @When("stop job $jobName after at least $count file exists in s3 with filename containing text $text within $waitSec seconds")
  public void stopJob(String jobName, int count, String text, int waitSec) throws Exception {
    S3Client s3Client = getS3client();
    doWithin(waitSec, "check uploaded files to s3", () -> s3Client.listObjectKeys(text).size() > count);
    InfraClient infraClient = getInfraClient();
    infraClient.stopJob(launchedJobs.get(jobName).getJobExecutionId());
    doWithin(waitSec, String.format("Wait for job %s stops", jobName), () -> infraClient.isRunning(jobName));
  }

  @When("delete file with key $key from s3")
  public void deleteFileFromS3(String key) {
    getS3client().deleteObject(key);
  }

  @Then("Check filenames contains the text $text on s3 server after $waitSec seconds")
  public void checkS3After(String text, int waitSec) {
    S3Client s3Client = getS3client();
    doWithin(waitSec, "check uploaded files to s3", () -> !s3Client.listObjectKeys().isEmpty());

    List<String> objectKeys = s3Client.listObjectKeys(text);
    assertThat(objectKeys, hasItem(containsString(text)));
  }

  @Then("Check $count files exists on s3 server with filenames containing the text $text after $waitSec seconds")
  public void checkNumberOfFilesOnS3(long count, String text, int waitSec) {
    S3Client s3Client = getS3client();
    doWithin(waitSec, "check uploaded files to s3", () -> s3Client.listObjectKeys(text).size() == count);
  }

  @Then("Less than $count files exists on s3 server with filenames containing the text $text after $waitSec seconds")
  public void checkLessThanFileExistsOnS3(long count, String text, int waitSec) {
    S3Client s3Client = getS3client();
    doWithin(waitSec, "check uploaded files to s3", () -> between(
            s3Client.listObjectKeys(text).size(), 1L, count - 1L));
  }

  private boolean between(long count, long from, long to) {
    return from <= count && count <= to;
  }

  @Then("No file exists on s3 server with filenames containing the text $text")
  public void fileNotExistOnS3(String text) {
    S3Client s3Client = getS3client();
    assertThat(s3Client.listObjectKeys().stream()
            .anyMatch(objectKey -> objectKey.contains(text)), is(false));
  }

  @Then("solr contains $count documents between $startLogtime and $endLogtime")
  public void documentCount(int count, OffsetDateTime startLogTime, OffsetDateTime endLogTime) {
    SolrQuery query = new SolrQuery();
    query.setRows(count * 2);
    query.setQuery(String.format("logtime:[\"%s\" TO \"%s\"]", SOLR_DATETIME_FORMATTER.format(startLogTime), SOLR_DATETIME_FORMATTER.format(endLogTime)));
    assertThat(getSolr().query(query).getResults().size(), is(count));
  }

  @Then("solr does not contain documents between $startLogtime and $endLogtime after $waitSec seconds")
  public void isSolrEmpty(OffsetDateTime startLogTime, OffsetDateTime endLogTime, int waitSec) {
    SolrQuery query = new SolrQuery();
    query.setRows(1);
    query.setQuery(String.format("logtime:[\"%s\" TO \"%s\"]", SOLR_DATETIME_FORMATTER.format(startLogTime), SOLR_DATETIME_FORMATTER.format(endLogTime)));
    doWithin(waitSec, "check solr is empty", () -> isSolrEmpty(query));
  }

  private boolean isSolrEmpty(SolrQuery query) {
    return getSolr().query(query).getResults().isEmpty();
  }

  @Then("Check $count files exists on local filesystem with filenames containing the text $text in the folder $path for job $jobName")
  public void checkNumberOfFilesOnLocalFilesystem(long count, String text, String path, String jobName) {
    File destinationDirectory = new File(getLocalDataFolder(), path.replace("${jobId}", Long.toString(launchedJobs.get(jobName).getJobInstanceId())));
    logger.info("Destination directory path: {}", destinationDirectory.getAbsolutePath());
    doWithin(5, "Destination directory exists", destinationDirectory::exists);

    File[] files = requireNonNull(destinationDirectory.listFiles(),
            String.format("Path %s is not a directory or an I/O error occurred!", destinationDirectory.getAbsolutePath()));
    assertThat(Arrays.stream(files)
            .filter(file -> file.getName().contains(text))
            .count(), is(count));
  }

  private static final ObjectMapper json = new ObjectMapper();

  @Then("Check the files $fileNamePart contains the archived documents")
  public void checkStoredDocumentIds(String fileNamePart) throws Exception {
    S3Client s3Client = getS3client();
    int size = documentIds.size();
    Set<String> storedDocumentIds = new HashSet<>();
    for (String objectKey : s3Client.listObjectKeys(fileNamePart)) {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(new BZip2CompressorInputStream(s3Client.getObject(objectKey)), UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          Map<String, Object> document = json.readValue(line, new TypeReference<HashMap<String, Object>>() {});
          String id = document.get("id").toString();
          storedDocumentIds.add(id);
          documentIds.remove(id);
        }
      }
    }
    assertThat(documentIds.size(), is(0));
    assertThat(storedDocumentIds.size(), is(size));
  }

  @AfterScenario
  public void waitForJobStops() throws InterruptedException {
    InfraClient infraClient = getInfraClient();
    doWithin(20, "Stop all launched jobs", () -> {
      int runningJobCount = 0;
      for (String jobName : launchedJobs.keySet()) {
        if (launchedJobs.get(jobName) == null)
          continue;
        if (!infraClient.isRunning(jobName)) {
          launchedJobs.put(jobName, null);
        }
        else {
          ++runningJobCount;
        }
      }
      return runningJobCount == 0;
    });
  }
}
