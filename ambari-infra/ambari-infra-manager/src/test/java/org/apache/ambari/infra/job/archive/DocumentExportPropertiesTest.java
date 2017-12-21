package org.apache.ambari.infra.job.archive;

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
public class DocumentExportPropertiesTest {
  @Test
  public void testDeepCopy() throws Exception {
    DocumentExportProperties documentExportProperties = new DocumentExportProperties();
    documentExportProperties.setDestinationDirectoryPath("/tmp");
    documentExportProperties.setFileNameSuffixColumn(".json");
    documentExportProperties.setReadBlockSize(10);
    documentExportProperties.setWriteBlockSize(20);
    documentExportProperties.setZooKeeperConnectionString("localhost:2181");
    SolrQueryProperties query = new SolrQueryProperties();
    query.setFilterQueryText("id:1167");
    query.setQueryText("name:'Joe'");
    query.setCollection("Users");
    query.setSortColumn(new String[] {"name"});
    documentExportProperties.setQuery(query);

    DocumentExportProperties parsed = documentExportProperties.deepCopy();

    assertThat(parsed.getDestinationDirectoryPath(), is(documentExportProperties.getDestinationDirectoryPath()));
    assertThat(parsed.getFileNameSuffixColumn(), is(documentExportProperties.getFileNameSuffixColumn()));
    assertThat(parsed.getReadBlockSize(), is(documentExportProperties.getReadBlockSize()));
    assertThat(parsed.getWriteBlockSize(), is(documentExportProperties.getWriteBlockSize()));
    assertThat(parsed.getZooKeeperConnectionString(), is(documentExportProperties.getZooKeeperConnectionString()));
    assertThat(parsed.getQuery().getQueryText(), is(query.getQueryText()));
    assertThat(parsed.getQuery().getFilterQueryText(), is(query.getFilterQueryText()));
    assertThat(parsed.getQuery().getCollection(), is(query.getCollection()));
    assertThat(parsed.getQuery().getSortColumn(), is(query.getSortColumn()));
  }
}