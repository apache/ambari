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
package org.apache.ambari.infra.conf.batch;

import org.apache.ambari.infra.job.dummy.DummyItemProcessor;
import org.apache.ambari.infra.job.dummy.DummyItemWriter;
import org.apache.ambari.infra.job.dummy.DummyObject;
import org.springframework.batch.admin.service.JdbcSearchableJobExecutionDao;
import org.springframework.batch.admin.service.JdbcSearchableJobInstanceDao;
import org.springframework.batch.admin.service.JdbcSearchableStepExecutionDao;
import org.springframework.batch.admin.service.JobService;
import org.springframework.batch.admin.service.SearchableJobExecutionDao;
import org.springframework.batch.admin.service.SearchableJobInstanceDao;
import org.springframework.batch.admin.service.SearchableStepExecutionDao;
import org.springframework.batch.admin.service.SimpleJobService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.DefaultExecutionContextSerializer;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JdbcExecutionContextDao;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.net.MalformedURLException;

@Configuration
@EnableBatchProcessing
@EnableScheduling
public class InfraManagerBatchConfig {

  @Value("classpath:org/springframework/batch/core/schema-drop-sqlite.sql")
  private Resource dropRepositoryTables;

  @Value("classpath:org/springframework/batch/core/schema-sqlite.sql")
  private Resource dataRepositorySchema;

  @Value("${infra-manager.batch.db.init:false}")
  private boolean dropDatabaseOnStartup;

  @Value("${infra-manager.batch.db.file:/etc/ambari-inra-manager/conf/repository.db}")
  private String sqliteDbFileLocation;

  @Value("${infra-manager.batch.db.username}")
  private String databaseUsername;

  @Value("${infra-manager.batch.db.password}")
  private String databasePassword;

  @Inject
  private StepBuilderFactory steps;

  @Inject
  private JobBuilderFactory jobs;

  @Inject
  private JobRegistry jobRegistry;

  @Inject
  private JobExplorer jobExplorer;

  @Bean
  public DataSource dataSource() {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName("org.sqlite.JDBC");
    dataSource.setUrl("jdbc:sqlite:" + sqliteDbFileLocation);
    dataSource.setUsername(databaseUsername);
    dataSource.setPassword(databasePassword);
    return dataSource;
  }

  @Bean
  public DataSourceInitializer dataSourceInitializer(DataSource dataSource)
    throws MalformedURLException {
    ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
    if (dropDatabaseOnStartup) {
      databasePopulator.addScript(dropRepositoryTables);
      databasePopulator.setIgnoreFailedDrops(true);
    }
    databasePopulator.addScript(dataRepositorySchema);
    databasePopulator.setContinueOnError(true);

    DataSourceInitializer initializer = new DataSourceInitializer();
    initializer.setDataSource(dataSource);
    initializer.setDatabasePopulator(databasePopulator);

    return initializer;
  }

  @Bean
  public JobRepository jobRepository() throws Exception {
    JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
    factory.setDataSource(dataSource());
    factory.setTransactionManager(getTransactionManager());
    factory.afterPropertiesSet();
    return factory.getObject();
  }

  @Bean
  public PlatformTransactionManager getTransactionManager() {
    return new ResourcelessTransactionManager();
  }

  @Bean(name = "jobLauncher")
  public JobLauncher jobLauncher() throws Exception {
    SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
    jobLauncher.setJobRepository(jobRepository());
    jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
    jobLauncher.afterPropertiesSet();
    return jobLauncher;
  }

  @Bean
  public JobOperator jobOperator() throws Exception {
    SimpleJobOperator jobOperator = new SimpleJobOperator();
    jobOperator.setJobExplorer(jobExplorer);
    jobOperator.setJobLauncher(jobLauncher());
    jobOperator.setJobRegistry(jobRegistry);
    jobOperator.setJobRepository(jobRepository());
    return jobOperator;
  }

  @Bean
  public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor() {
    JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor = new JobRegistryBeanPostProcessor();
    jobRegistryBeanPostProcessor.setJobRegistry(jobRegistry);
    return jobRegistryBeanPostProcessor;
  }

  @Bean
  public JdbcTemplate jdbcTemplate() {
    return new JdbcTemplate(dataSource());
  }

  @Bean
  public SearchableJobInstanceDao searchableJobInstanceDao() {
    JdbcSearchableJobInstanceDao dao = new JdbcSearchableJobInstanceDao();
    dao.setJdbcTemplate(jdbcTemplate());
    return dao;
  }

  @Bean
  public SearchableJobExecutionDao searchableJobExecutionDao() {
    JdbcSearchableJobExecutionDao dao = new JdbcSearchableJobExecutionDao();
    dao.setJdbcTemplate(jdbcTemplate());
    dao.setDataSource(dataSource());
    return dao;
  }

  @Bean
  public SearchableStepExecutionDao searchableStepExecutionDao() {
    JdbcSearchableStepExecutionDao dao = new JdbcSearchableStepExecutionDao();
    dao.setDataSource(dataSource());
    dao.setJdbcTemplate(jdbcTemplate());
    return dao;
  }

  @Bean
  public ExecutionContextDao executionContextDao() {
    JdbcExecutionContextDao dao = new JdbcExecutionContextDao();
    dao.setSerializer(new DefaultExecutionContextSerializer());
    dao.setJdbcTemplate(jdbcTemplate());
    return dao;
  }

  @Bean
  public JobService jobService() throws Exception {
    return new
      SimpleJobService(searchableJobInstanceDao(), searchableJobExecutionDao(), searchableStepExecutionDao(),
      jobRepository(), jobLauncher(), jobRegistry, executionContextDao());
  }

  @Bean(name = "dummyStep")
  protected Step dummyStep(ItemReader<DummyObject> reader,
                       ItemProcessor<DummyObject, String> processor,
                       ItemWriter<String> writer) {
    return steps.get("dummyStep").<DummyObject, String> chunk(2)
      .reader(reader).processor(processor).writer(writer).build();
  }

  @Bean(name = "dummyJob")
  public Job job(@Qualifier("dummyStep") Step dummyStep) {
    return jobs.get("dummyJob").start(dummyStep).build();
  }

  @Bean
  public ItemReader<DummyObject> dummyItemReader() {
    FlatFileItemReader<DummyObject> csvFileReader = new FlatFileItemReader<>();
    csvFileReader.setResource(new ClassPathResource("dummy/dummy.txt"));
    csvFileReader.setLinesToSkip(1);
    LineMapper<DummyObject> lineMapper = dummyLineMapper();
    csvFileReader.setLineMapper(lineMapper);
    return csvFileReader;
  }

  @Bean
  public ItemProcessor<DummyObject, String> dummyItemProcessor() {
    return new DummyItemProcessor();
  }

  @Bean
  public ItemWriter<String> dummyItemWriter() {
    return new DummyItemWriter();
  }

  private LineMapper<DummyObject> dummyLineMapper() {
    DefaultLineMapper<DummyObject> lineMapper = new DefaultLineMapper<>();

    LineTokenizer dummyTokenizer = dummyTokenizer();
    lineMapper.setLineTokenizer(dummyTokenizer);

    FieldSetMapper<DummyObject> dummyFieldSetMapper = dummyFieldSetMapper();
    lineMapper.setFieldSetMapper(dummyFieldSetMapper);

    return lineMapper;
  }

  private FieldSetMapper<DummyObject> dummyFieldSetMapper() {
    BeanWrapperFieldSetMapper<DummyObject> studentInformationMapper = new BeanWrapperFieldSetMapper<>();
    studentInformationMapper.setTargetType(DummyObject.class);
    return studentInformationMapper;
  }

  private LineTokenizer dummyTokenizer() {
    DelimitedLineTokenizer studentLineTokenizer = new DelimitedLineTokenizer();
    studentLineTokenizer.setDelimiter(",");
    studentLineTokenizer.setNames(new String[]{"f1", "f2"});
    return studentLineTokenizer;
  }

}
