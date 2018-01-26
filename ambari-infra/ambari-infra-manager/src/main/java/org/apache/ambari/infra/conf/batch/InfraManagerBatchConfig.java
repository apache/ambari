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

import org.springframework.batch.admin.service.JdbcSearchableJobExecutionDao;
import org.springframework.batch.admin.service.JdbcSearchableJobInstanceDao;
import org.springframework.batch.admin.service.JdbcSearchableStepExecutionDao;
import org.springframework.batch.admin.service.JobService;
import org.springframework.batch.admin.service.SearchableJobExecutionDao;
import org.springframework.batch.admin.service.SearchableJobInstanceDao;
import org.springframework.batch.admin.service.SearchableStepExecutionDao;
import org.springframework.batch.admin.service.SimpleJobService;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.Jackson2ExecutionContextStringSerializer;
import org.springframework.batch.core.repository.dao.JdbcExecutionContextDao;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import javax.inject.Inject;
import javax.sql.DataSource;

@Configuration
@EnableBatchProcessing
@EnableScheduling
@EnableAsync
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
  private JobRegistry jobRegistry;

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
  public DataSourceInitializer dataSourceInitializer() {
    ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
    if (dropDatabaseOnStartup) {
      databasePopulator.addScript(dropRepositoryTables);
      databasePopulator.setIgnoreFailedDrops(true);
    }
    databasePopulator.addScript(dataRepositorySchema);
    databasePopulator.setContinueOnError(true);

    DataSourceInitializer initializer = new DataSourceInitializer();
    initializer.setDataSource(dataSource());
    initializer.setDatabasePopulator(databasePopulator);

    return initializer;
  }

  @Bean
  public ExecutionContextSerializer executionContextSerializer() {
    return new Jackson2ExecutionContextStringSerializer();
  }

  @Bean
  public JobRepository jobRepository() throws Exception {
    JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
    factory.setDataSource(dataSource());
    factory.setTransactionManager(transactionManager());
    factory.setSerializer(executionContextSerializer());
    factory.afterPropertiesSet();
    return factory.getObject();
  }

  @Bean
  public PlatformTransactionManager transactionManager() {
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
    jobOperator.setJobExplorer(jobExplorer());
    jobOperator.setJobLauncher(jobLauncher());
    jobOperator.setJobRegistry(jobRegistry);
    jobOperator.setJobRepository(jobRepository());
    return jobOperator;
  }

  @Bean
  public JobExplorer jobExplorer() throws Exception {
    JobExplorerFactoryBean factoryBean = new JobExplorerFactoryBean();
    factoryBean.setSerializer(executionContextSerializer());
    factoryBean.setDataSource(dataSource());
    factoryBean.afterPropertiesSet();
    return factoryBean.getObject();
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
    dao.setSerializer(executionContextSerializer());
    dao.setJdbcTemplate(jdbcTemplate());
    return dao;
  }

  @Bean
  public JobService jobService() throws Exception {
    return new
      SimpleJobService(searchableJobInstanceDao(), searchableJobExecutionDao(), searchableStepExecutionDao(),
      jobRepository(), jobLauncher(), jobRegistry, executionContextDao());
  }
}
