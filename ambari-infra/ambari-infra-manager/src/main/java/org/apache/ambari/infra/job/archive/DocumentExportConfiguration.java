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
package org.apache.ambari.infra.job.archive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.apache.ambari.infra.job.archive.SolrDocumentSource.SOLR_DATETIME_FORMATTER;
import static org.apache.commons.lang.StringUtils.isBlank;

@Configuration
public class DocumentExportConfiguration {
  private static final Logger LOG = LoggerFactory.getLogger(DocumentExportConfiguration.class);
  private static final DateTimeFormatter FILENAME_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH_mm_ss.SSSX");

  @Inject
  private DocumentExportProperties properties;

  @Inject
  private StepBuilderFactory steps;

  @Inject
  private JobBuilderFactory jobs;



  @Bean
  public Job logExportJob(@Qualifier("exportStep") Step logExportStep) {
    return jobs.get("solr_data_export").listener(new DocumentExportJobListener()).start(logExportStep).build();
  }

  @Bean
  @JobScope
  public Step exportStep(DocumentExporter documentExporter) {
    return steps.get("export")
            .tasklet(documentExporter)
            .listener(new DocumentExportStepListener(properties))
            .build();
  }

  @Bean
  @StepScope
  public DocumentExporter getDocumentExporter(DocumentItemReader documentItemReader,
                                              @Value("#{stepExecution.jobExecution.id}") String jobId) {
    File path = Paths.get(
            properties.getDestinationDirectoryPath(),
            String.format("%s_%s", properties.getQuery().getCollection(), jobId)).toFile(); // TODO: add end date
    LOG.info("Destination directory path={}", path);
    if (!path.exists()) {
      if (!path.mkdirs()) {
        LOG.warn("Unable to create directory {}", path);
      }
    }

    CompositeFileAction fileAction = new CompositeFileAction(new TarGzCompressor());

    return new DocumentExporter(
            documentItemReader,
            firstDocument -> new LocalDocumentItemWriter(
                    new File(path, String.format("%s_-_%s.json",
                            properties.getQuery().getCollection(),
                            firstDocument.get(properties.getFileNameSuffixColumn()))),
                    fileAction),
            properties.getWriteBlockSize());
  }

  @Bean
  @StepScope
  public DocumentItemReader reader(DocumentSource documentSource) {
    return new DocumentItemReader(documentSource, properties.getReadBlockSize());
  }

  @Bean
  @StepScope
  public DocumentSource logSource(@Value("#{jobParameters[endDate]}") String endDateText) {
    OffsetDateTime endDate = OffsetDateTime.now(ZoneOffset.UTC);
    if (!isBlank(endDateText))
      endDate = OffsetDateTime.parse(endDateText);

    return new SolrDocumentSource(
            properties.getZooKeeperSocket(),
            properties.getQuery(),
            SOLR_DATETIME_FORMATTER.format(endDate));
  }
}
