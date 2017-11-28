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
package org.apache.ambari.infra.job.archive;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;

public class SolrDocumentSource implements DocumentSource {
  public static final DateTimeFormatter SOLR_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
  private static final Logger LOG = LoggerFactory.getLogger(SolrDocumentSource.class);

  private final String zkHost;
  private final SolrQueryProperties properties;
  private final String endValue;

  public SolrDocumentSource(String zkHost, SolrQueryProperties properties, String endValue) {
    this.zkHost = zkHost;
    this.properties = properties;
    this.endValue = endValue;
  }

  @Override
  public DocumentIterator open(Document current, int rows) {
    CloudSolrClient client = new CloudSolrClient.Builder().withZkHost(zkHost).build();
    client.setDefaultCollection(properties.getCollection());

    SolrQuery query = properties.toQueryBuilder()
            .setEndValue(endValue)
            .setDocument(current)
            .build();
    query.setRows(rows);

    LOG.info("Executing solr query {}", query.toLocalParamsString());

    try {
      QueryResponse response = client.query(query);
      return new SolrDocumentIterator(response, client);
    } catch (SolrServerException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
