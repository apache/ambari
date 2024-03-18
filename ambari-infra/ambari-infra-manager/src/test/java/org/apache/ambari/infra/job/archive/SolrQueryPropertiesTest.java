package org.apache.ambari.infra.job.archive;

import org.junit.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
public class SolrQueryPropertiesTest {
  @Test
  public void testApplySortColumns() throws Exception {
    JobParameters jobParameters = new JobParametersBuilder()
            .addString("sortColumn[0]", "logtime")
            .addString("sortColumn[1]", "id")
            .toJobParameters();

    SolrQueryProperties solrQueryProperties = new SolrQueryProperties();
    solrQueryProperties.setSortColumn(new String[] {"testColumn"});
    solrQueryProperties.apply(jobParameters);
    assertThat(solrQueryProperties.getSortColumn().length, is(2));
    assertThat(solrQueryProperties.getSortColumn()[0], is("logtime"));
    assertThat(solrQueryProperties.getSortColumn()[1], is("id"));
  }

  @Test
  public void testApplyWhenNoSortIsDefined() throws Exception {
    JobParameters jobParameters = new JobParametersBuilder()
            .toJobParameters();

    SolrQueryProperties solrQueryProperties = new SolrQueryProperties();
    solrQueryProperties.setSortColumn(new String[] {"testColumn"});
    solrQueryProperties.apply(jobParameters);
    assertThat(solrQueryProperties.getSortColumn().length, is(1));
  }
}