package org.apache.ambari.infra.job;

import org.apache.ambari.infra.job.archive.DocumentArchivingProperties;
import org.apache.ambari.infra.job.archive.SolrProperties;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
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
public class JobPropertiesTest {
  @Test
  public void testDeepCopy() throws Exception {
    DocumentArchivingProperties documentArchivingProperties = new DocumentArchivingProperties();
    documentArchivingProperties.setLocalDestinationDirectory("/tmp");
    documentArchivingProperties.setFileNameSuffixColumn(".json");
    documentArchivingProperties.setReadBlockSize(10);
    documentArchivingProperties.setWriteBlockSize(20);
    SolrProperties solr = new SolrProperties();
    solr.setZooKeeperConnectionString("localhost:2181");
    solr.setFilterQueryText("id:1167");
    solr.setQueryText("name:'Joe'");
    solr.setCollection("Users");
    solr.setSortColumn(new String[] {"name"});
    documentArchivingProperties.setSolr(solr);

    DocumentArchivingProperties parsed = documentArchivingProperties.deepCopy();

    assertThat(parsed.getLocalDestinationDirectory(), is(documentArchivingProperties.getLocalDestinationDirectory()));
    assertThat(parsed.getFileNameSuffixColumn(), is(documentArchivingProperties.getFileNameSuffixColumn()));
    assertThat(parsed.getReadBlockSize(), is(documentArchivingProperties.getReadBlockSize()));
    assertThat(parsed.getWriteBlockSize(), is(documentArchivingProperties.getWriteBlockSize()));
    assertThat(parsed.getSolr().getZooKeeperConnectionString(), is(documentArchivingProperties.getSolr().getZooKeeperConnectionString()));
    assertThat(parsed.getSolr().getQueryText(), is(solr.getQueryText()));
    assertThat(parsed.getSolr().getFilterQueryText(), is(solr.getFilterQueryText()));
    assertThat(parsed.getSolr().getCollection(), is(solr.getCollection()));
    assertThat(parsed.getSolr().getSortColumn(), is(solr.getSortColumn()));
  }
}