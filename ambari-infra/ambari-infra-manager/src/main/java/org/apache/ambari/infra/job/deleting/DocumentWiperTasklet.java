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
package org.apache.ambari.infra.job.deleting;

import org.apache.ambari.infra.job.SolrDAOBase;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class DocumentWiperTasklet extends SolrDAOBase implements Tasklet {
  private final String filterField;
  private final String start;
  private final String end;

  public DocumentWiperTasklet(DocumentDeletingProperties properties, String start, String end) {
    super(properties.getZooKeeperConnectionString(), properties.getCollection());
    this.filterField = properties.getFilterField();
    this.start = start;
    this.end = end;
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    delete(String.format("%s:[%s TO %s]", filterField, getValue(start), getValue(end)));
    return RepeatStatus.FINISHED;
  }

  private String getValue(String value) {
    return "*".equals(value) ? value : ClientUtils.escapeQueryChars(value);
  }
}
