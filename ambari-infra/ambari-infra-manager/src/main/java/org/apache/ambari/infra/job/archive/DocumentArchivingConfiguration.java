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

import static org.apache.ambari.infra.job.JobsPropertyMap.PARAMETERS_CONTEXT_KEY;
import static org.apache.ambari.infra.job.archive.SolrQueryBuilder.computeEnd;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.File;

import javax.inject.Inject;

import org.apache.ambari.infra.conf.InfraManagerDataConfig;
import org.apache.ambari.infra.job.AbstractJobsConfiguration;
import org.apache.ambari.infra.job.JobContextRepository;
import org.apache.ambari.infra.job.JobScheduler;
import org.apache.ambari.infra.job.ObjectSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentArchivingConfiguration extends AbstractJobsConfiguration<ArchivingProperties, ArchivingProperties> {
  private static final Logger logger = LogManager.getLogger(DocumentArchivingConfiguration.class);
  private static final DocumentWiper NOT_DELETE = (firstDocument, lastDocument) -> { };

  private final StepBuilderFactory steps;
  private final Step exportStep;

  @Inject
  public DocumentArchivingConfiguration(
          DocumentArchivingPropertyMap jobsPropertyMap,
          JobScheduler scheduler,
          StepBuilderFactory steps,
          JobBuilderFactory jobs,
          @Qualifier("exportStep") Step exportStep,
          JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor) {
    super(jobsPropertyMap.getSolrDataArchiving(), scheduler, jobs, jobRegistryBeanPostProcessor);
    this.exportStep = exportStep;
    this.steps = steps;
  }

  @Override
  protected Job buildJob(JobBuilder jobBuilder) {
    return jobBuilder.start(exportStep).build();
  }

  @Bean
  @JobScope
  public Step exportStep(DocumentExporter documentExporter) {
    return steps.get("export")
            .tasklet(documentExporter)
            .build();
  }

  @Bean
  @StepScope
  public DocumentExporter documentExporter(DocumentItemReader documentItemReader,
                                           @Value("#{stepExecution.jobExecution.jobId}") String jobId,
                                           @Value("#{stepExecution.jobExecution.executionContext.get('" + PARAMETERS_CONTEXT_KEY + "')}") ArchivingProperties parameters,
                                           InfraManagerDataConfig infraManagerDataConfig,
                                           @Value("#{jobParameters[end]}") String intervalEnd,
                                           DocumentWiper documentWiper,
                                           JobContextRepository jobContextRepository) {

    File baseDir = new File(infraManagerDataConfig.getDataFolder(), "exporting");
    CompositeFileAction fileAction = new CompositeFileAction(new BZip2Compressor());
    switch (parameters.getDestination()) {
      case HDFS:
        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
        fileAction.add(new HdfsUploader(conf,
                parameters.hdfsProperties().orElseThrow(() -> new IllegalStateException("HDFS properties are not provided!"))));
        break;
      case LOCAL:
        baseDir = new File(parameters.getLocalDestinationDirectory());
        break;
    }

    FileNameSuffixFormatter fileNameSuffixFormatter = FileNameSuffixFormatter.from(parameters);
    LocalItemWriterListener itemWriterListener = new LocalItemWriterListener(fileAction, documentWiper);
    File destinationDirectory = new File(
            baseDir,
            String.format("%s_%s_%s",
                    parameters.getSolr().getCollection(),
                    jobId,
                    isBlank(intervalEnd) ? "" : fileNameSuffixFormatter.format(intervalEnd)));
    logger.info("Destination directory path={}", destinationDirectory);
    if (!destinationDirectory.exists()) {
      if (!destinationDirectory.mkdirs()) {
        logger.warn("Unable to create directory {}", destinationDirectory);
      }
    }

    return new DocumentExporter(
            documentItemReader,
            firstDocument -> new LocalDocumentItemWriter(
                    outFile(parameters.getSolr().getCollection(), destinationDirectory, fileNameSuffixFormatter.format(firstDocument)), itemWriterListener),
            parameters.getWriteBlockSize(), jobContextRepository);
  }

  @Bean
  @StepScope
  public DocumentWiper documentWiper(@Value("#{stepExecution.jobExecution.executionContext.get('" + PARAMETERS_CONTEXT_KEY + "')}") ArchivingProperties parameters,
                                     SolrDAO solrDAO) {
    if (isBlank(parameters.getSolr().getDeleteQueryText()))
      return NOT_DELETE;
    return solrDAO;
  }

  @Bean
  @StepScope
  public SolrDAO solrDAO(@Value("#{stepExecution.jobExecution.executionContext.get('" + PARAMETERS_CONTEXT_KEY + "')}") ArchivingProperties parameters) {
    return new SolrDAO(parameters.getSolr());
  }

  private File outFile(String collection, File directoryPath, String suffix) {
    File file = new File(directoryPath, String.format("%s_-_%s.json", collection, suffix));
    logger.info("Exporting to temp file {}", file.getAbsolutePath());
    return file;
  }

  @Bean
  @StepScope
  public DocumentItemReader reader(ObjectSource<Document> documentSource,
                                   @Value("#{stepExecution.jobExecution.executionContext.get('" + PARAMETERS_CONTEXT_KEY + "')}") ArchivingProperties properties) {
    return new DocumentItemReader(documentSource, properties.getReadBlockSize());
  }

  @Bean
  @StepScope
  public ObjectSource<Document> documentSource(@Value("#{stepExecution.jobExecution.executionContext.get('" + PARAMETERS_CONTEXT_KEY + "')}") ArchivingProperties parameters,
                                               SolrDAO solrDAO) {

    return new SolrDocumentSource(solrDAO, parameters.getStart(), computeEnd(parameters.getEnd(), parameters.getTtl()));
  }
}
