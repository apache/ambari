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

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;

public abstract class SolrDAOBase {
  private static final Logger LOG = LoggerFactory.getLogger(SolrDAOBase.class);

  private final String zooKeeperConnectionString;
  private final String defaultCollection;

  protected SolrDAOBase(String zooKeeperConnectionString, String defaultCollection) {
    this.zooKeeperConnectionString = zooKeeperConnectionString;
    this.defaultCollection = defaultCollection;
  }

  protected void delete(String deleteQueryText) {
    try (CloudSolrClient client = createClient()) {
      try {
        LOG.info("Executing solr delete by query {}", deleteQueryText);
        client.deleteByQuery(deleteQueryText);
        client.commit();
      } catch (Exception e) {
        try {
          client.rollback();
        } catch (SolrServerException e1) {
          LOG.warn("Unable to rollback after solr delete operation failure.", e1);
        }
        throw new RuntimeException(e);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected CloudSolrClient createClient() {
    CloudSolrClient client = new CloudSolrClient.Builder().withZkHost(zooKeeperConnectionString).build();
    client.setDefaultCollection(defaultCollection);
    return client;
  }
}

