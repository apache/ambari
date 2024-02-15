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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.zookeeper.client.ConnectStringParser;

public abstract class SolrDAOBase {
  private static final Logger logger = LogManager.getLogger(SolrDAOBase.class);

  private final String zooKeeperConnectionString;
  private final String defaultCollection;

  protected SolrDAOBase(String zooKeeperConnectionString, String defaultCollection) {
    this.zooKeeperConnectionString = zooKeeperConnectionString;
    this.defaultCollection = defaultCollection;
  }

  protected void delete(String deleteQueryText) {
    try (CloudSolrClient client = createClient()) {
      try {
        logger.info("Executing solr delete by query {}", deleteQueryText);
        client.deleteByQuery(deleteQueryText);
        client.commit();
      } catch (Exception e) {
        try {
          client.rollback();
        } catch (SolrServerException e1) {
          logger.warn("Unable to rollback after solr delete operation failure.", e1);
        }
        throw new RuntimeException(e);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected CloudSolrClient createClient() {
    ConnectStringParser connectStringParser = new ConnectStringParser(zooKeeperConnectionString);
    List<String> zkHosts = connectStringParser.getServerAddresses().stream()
            .map(InetSocketAddress::toString)
            .collect(Collectors.toList());
    CloudSolrClient client = new CloudSolrClient.Builder(
            zkHosts, Optional.ofNullable(connectStringParser.getChrootPath())).build();
    client.setDefaultCollection(defaultCollection);
    return client;
  }
}

