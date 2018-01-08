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

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.apache.ambari.infra.InfraClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.jbehave.core.annotations.Alias;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.OffsetDateTime;

import static org.apache.ambari.infra.OffsetDateTimeConverter.SOLR_DATETIME_FORMATTER;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;

public class ExportJobsSteps extends AbstractInfraSteps {
  private static final Logger LOG = LoggerFactory.getLogger(ExportJobsSteps.class);

  @Given("$count documents in solr")
  public void addDocuments(int count) throws Exception {
    OffsetDateTime intervalEnd = OffsetDateTime.now();
    for (int i = 0; i < count; ++i) {
      addDocument(intervalEnd.minusMinutes(i % (count / 10)));
    }
    getSolrClient().commit();
  }

  @Given("$count documents in solr with logtime from $startLogtime to $endLogtime")
  public void addDocuments(long count, OffsetDateTime startLogtime, OffsetDateTime endLogtime) throws Exception {
    Duration duration = Duration.between(startLogtime, endLogtime);
    long increment = duration.toNanos() / count;
    for (int i = 0; i < count; ++i)
      addDocument(startLogtime.plusNanos(increment * i));
    getSolrClient().commit();
  }

  @Given("a file on s3 with key $key")
  public void addFileToS3(String key) throws Exception {
    try (ByteArrayInputStream inputStream = new ByteArrayInputStream("anything".getBytes())) {
      getS3client().putObject(S3_BUCKET_NAME, key, inputStream, new ObjectMetadata());
    }
  }

  @When("start $jobName job")
  public void startJob(String jobName) throws Exception {
    startJob(jobName, null);
  }

  @When("start $jobName job with parameters $parameters")
  @Alias("restart $jobName job with parameters $parameters")
  public void startJob(String jobName, String parameters) throws Exception {
    try (InfraClient httpClient = getInfraClient()) {
      httpClient.startJob(jobName, parameters);
    }
  }

  @When("delete file with key $key from s3")
  public void deleteFileFromS3(String key) {
    getS3client().deleteObject(S3_BUCKET_NAME, key);
  }

  @Then("Check filenames contains the text $text on s3 server after $waitSec seconds")
  public void checkS3After(String text, int waitSec) {
    AmazonS3Client s3Client = getS3client();
    ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(S3_BUCKET_NAME);
    doWithin(waitSec, "check uploaded files to s3", () -> s3Client.doesBucketExist(S3_BUCKET_NAME)
            && !s3Client.listObjects(listObjectsRequest).getObjectSummaries().isEmpty());

    ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);
    assertThat(objectListing.getObjectSummaries(), hasItem(hasProperty("key", containsString(text))));
  }

  @Then("Check $count files exists on s3 server with filenames containing the text $text after $waitSec seconds")
  public void checkNumberOfFilesOnS3(int count, String text, int waitSec) {
    AmazonS3Client s3Client = getS3client();
    ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(S3_BUCKET_NAME);
    doWithin(waitSec, "check uploaded files to s3", () -> s3Client.doesBucketExist(S3_BUCKET_NAME)
            && s3Client.listObjects(listObjectsRequest).getObjectSummaries().stream()
            .filter(s3ObjectSummary -> s3ObjectSummary.getKey().contains(text))
            .count() == count);
  }

  @Then("No file exists on s3 server with filenames containing the text $text")
  public void fileNotExistOnS3(String text) {
    AmazonS3Client s3Client = getS3client();
    ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(S3_BUCKET_NAME);
    assertThat(s3Client.listObjects(listObjectsRequest).getObjectSummaries().stream()
            .anyMatch(s3ObjectSummary -> s3ObjectSummary.getKey().contains(text)), is(false));
  }

  @Then("solr contains $count documents between $startLogtime and $endLogtime")
  public void documentCount(int count, OffsetDateTime startLogTime, OffsetDateTime endLogTime) throws Exception {
    SolrQuery query = new SolrQuery();
    query.setRows(count * 2);
    query.setQuery(String.format("logtime:[\"%s\" TO \"%s\"]", SOLR_DATETIME_FORMATTER.format(startLogTime), SOLR_DATETIME_FORMATTER.format(endLogTime)));
    assertThat(getSolrClient().query(query).getResults().size(), is(count));
  }

  @Then("solr does not contain documents between $startLogtime and $endLogtime after $waitSec seconds")
  public void isSolrEmpty(OffsetDateTime startLogTime, OffsetDateTime endLogTime, int waitSec) {
    SolrQuery query = new SolrQuery();
    query.setRows(1);
    query.setQuery(String.format("logtime:[\"%s\" TO \"%s\"]", SOLR_DATETIME_FORMATTER.format(startLogTime), SOLR_DATETIME_FORMATTER.format(endLogTime)));
    doWithin(waitSec, "check solr is empty", () -> isSolrEmpty(query));
  }

  private boolean isSolrEmpty(SolrQuery query) {
    try {
      return getSolrClient().query(query).getResults().isEmpty();
    } catch (SolrServerException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
