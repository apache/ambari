package org.apache.ambari.infra.job.archive;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

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
public class  SolrPropertiesTest {
  @Test
  public void testMergeSortColumns() {
    JobParameters jobParameters = new JobParametersBuilder()
            .addString("sortColumn[0]", "logtime")
            .addString("sortColumn[1]", "id")
            .toJobParameters();

    SolrProperties solrProperties = new SolrProperties();
    solrProperties.setSortColumn(new String[] {"testColumn"});
    SolrProperties solrParameters = solrProperties.merge(jobParameters);
    assertThat(solrParameters.getSortColumn().length, is(2));
    assertThat(solrParameters.getSortColumn()[0], is("logtime"));
    assertThat(solrParameters.getSortColumn()[1], is("id"));
  }

  @Test
  public void testMergeWhenNoSortIsDefined() {
    JobParameters jobParameters = new JobParametersBuilder()
            .toJobParameters();

    SolrProperties solrProperties = new SolrProperties();
    SolrProperties solrParameters = solrProperties.merge(jobParameters);
    assertThat(solrParameters.getSortColumn(), is(nullValue()));
  }

  @Test
  public void testMergeWhenPropertiesAreDefinedButJobParamsAreNot() {
    JobParameters jobParameters = new JobParametersBuilder()
            .toJobParameters();

    SolrProperties solrProperties = new SolrProperties();
    solrProperties.setSortColumn(new String[] {"testColumn"});
    SolrProperties solrParameters = solrProperties.merge(jobParameters);
    assertThat(solrParameters.getSortColumn().length, is(1));
    assertThat(solrParameters.getSortColumn()[0], is("testColumn"));
  }
}