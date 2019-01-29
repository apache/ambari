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
package org.apache.ambari.infra.job;

import java.time.OffsetDateTime;
import java.util.Date;

import javax.inject.Inject;

import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class InfraJobExecutionDao extends AbstractJdbcBatchMetadataDao {

  private final TransactionTemplate transactionTemplate;

  @Inject
  public InfraJobExecutionDao(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
    setJdbcTemplate(jdbcTemplate);
    this.transactionTemplate = transactionTemplate;
  }

  public void deleteJobExecutions(OffsetDateTime olderThan) {
    transactionTemplate.execute(transactionStatus -> {
      Date olderThanDate = Date.from(olderThan.toInstant());
      deleteStepExecutionContexts(olderThanDate);
      deleteStepExecutions(olderThanDate);
      deleteJobExecutionParams(olderThanDate);
      deleteJobExecutionContexts(olderThanDate);
      getJdbcTemplate().update(getQuery("DELETE FROM %PREFIX%JOB_EXECUTION WHERE CREATE_TIME < ?"), olderThanDate);
      getJdbcTemplate().update(getQuery("DELETE FROM %PREFIX%JOB_INSTANCE WHERE JOB_INSTANCE_ID NOT IN (SELECT JOB_INSTANCE_ID FROM %PREFIX%JOB_EXECUTION)"));
      return null;
    });
  }

  private void deleteStepExecutionContexts(Date olderThan) {
    getJdbcTemplate().update(getQuery("DELETE FROM %PREFIX%STEP_EXECUTION_CONTEXT WHERE STEP_EXECUTION_ID IN (SELECT STEP_EXECUTION_ID FROM %PREFIX%STEP_EXECUTION WHERE JOB_EXECUTION_ID IN (SELECT JOB_EXECUTION_ID FROM %PREFIX%JOB_EXECUTION WHERE CREATE_TIME < ?))"),
            olderThan);
  }

  private void deleteStepExecutions(Date olderThan) {
    getJdbcTemplate().update(getQuery("DELETE FROM %PREFIX%STEP_EXECUTION WHERE JOB_EXECUTION_ID IN (SELECT JOB_EXECUTION_ID FROM %PREFIX%JOB_EXECUTION WHERE CREATE_TIME < ?)"),
            olderThan);
  }

  private void deleteJobExecutionParams(Date olderThan) {
    getJdbcTemplate().update(getQuery("DELETE FROM %PREFIX%JOB_EXECUTION_PARAMS WHERE JOB_EXECUTION_ID IN (SELECT JOB_EXECUTION_ID FROM %PREFIX%JOB_EXECUTION WHERE CREATE_TIME < ?)"),
            olderThan);
  }

  private void deleteJobExecutionContexts(Date olderThan) {
    getJdbcTemplate().update(getQuery("DELETE FROM %PREFIX%JOB_EXECUTION_CONTEXT WHERE JOB_EXECUTION_ID IN (SELECT JOB_EXECUTION_ID FROM  %PREFIX%JOB_EXECUTION WHERE CREATE_TIME < ?)"),
            olderThan);
  }

}
