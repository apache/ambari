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
package org.apache.ambari.infra.env;

import javax.sql.DataSource;

import org.springframework.batch.admin.service.JdbcSearchableJobExecutionDao;
import org.springframework.batch.admin.service.JdbcSearchableJobInstanceDao;
import org.springframework.batch.admin.service.SearchableJobExecutionDao;
import org.springframework.batch.admin.service.SearchableJobInstanceDao;
import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.Jackson2ExecutionContextStringSerializer;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.sqlite.SQLiteConfig;

@Configuration
@ComponentScan(basePackages = {"org.apache.ambari.infra.env"})
public class TestAppConfig {

  @Value("classpath:org/springframework/batch/core/schema-drop-sqlite.sql")
  private Resource dropRepositoryTables;

  @Value("classpath:org/springframework/batch/core/schema-sqlite.sql")
  private Resource dataRepositorySchema;

  @Bean
  public DataSource dataSource() {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName("org.sqlite.JDBC");
    dataSource.setUrl("jdbc:sqlite:test.db");
    dataSource.setUsername("test");
    dataSource.setPassword("test");
    SQLiteConfig config = new SQLiteConfig();
    config.enforceForeignKeys(true);
    dataSource.setConnectionProperties(config.toProperties());
    return dataSource;
  }

  @Bean
  public DataSourceInitializer dataSourceInitializer() {
    ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
    databasePopulator.addScript(dropRepositoryTables);
    databasePopulator.setIgnoreFailedDrops(true);
    databasePopulator.addScript(dataRepositorySchema);
    databasePopulator.setContinueOnError(true);

    DataSourceInitializer initializer = new DataSourceInitializer();
    initializer.setDataSource(dataSource());
    initializer.setDatabasePopulator(databasePopulator);

    return initializer;
  }

  @Bean
  public JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  @Bean
  public SearchableJobInstanceDao searchableJobInstanceDao(JdbcTemplate jdbcTemplate) {
    JdbcSearchableJobInstanceDao dao = new JdbcSearchableJobInstanceDao();
    dao.setJdbcTemplate(jdbcTemplate);
    return dao;
  }

  @Bean
  public SearchableJobExecutionDao searchableJobExecutionDao(JdbcTemplate jdbcTemplate, DataSource dataSource) {
    JdbcSearchableJobExecutionDao dao = new JdbcSearchableJobExecutionDao();
    dao.setJdbcTemplate(jdbcTemplate);
    dao.setDataSource(dataSource);
    return dao;
  }

  @Bean
  public ExecutionContextSerializer executionContextSerializer() {
    return new Jackson2ExecutionContextStringSerializer();
  }

  @Bean
  public PlatformTransactionManager transactionManager(DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
  }

  @Bean
  public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
    return new TransactionTemplate(transactionManager);
  }

  @Bean
  public JobRepository jobRepository(ExecutionContextSerializer executionContextSerializer, PlatformTransactionManager transactionManager) throws Exception {
    JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
    factory.setDataSource(dataSource());
    factory.setTransactionManager(transactionManager);
    factory.setSerializer(executionContextSerializer);
    factory.afterPropertiesSet();
    return factory.getObject();
  }

}
