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

import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.ambari.infra.job.SolrDAOBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;

public class SolrDAO extends SolrDAOBase implements DocumentWiper {
  private static final Logger logger = LogManager.getLogger(SolrDAO.class);

  private final SolrProperties queryProperties;

  public SolrDAO(SolrProperties queryProperties) {
    super(queryProperties.getZooKeeperConnectionString(), queryProperties.getCollection());
    this.queryProperties = queryProperties;
  }

  @Override
  public void delete(Document firstDocument, Document lastDocument) {
    delete(new SolrParametrizedString(queryProperties.getDeleteQueryText())
            .set("start", firstDocument.getFieldMap())
            .set("end", lastDocument.getFieldMap()).toString());
  }

  public SolrDocumentIterator query(String start, String end, Document subIntervalFrom, int rows) {
    SolrQuery query = queryProperties.toQueryBuilder()
            .setInterval(start, end)
            .setDocument(subIntervalFrom)
            .build();
    query.setRows(rows);

    logger.info("Executing solr query {}", query.toLocalParamsString());

    try {
      CloudSolrClient client = createClient();
      QueryResponse response = client.query(query);
      return new SolrDocumentIterator(response, client);
    } catch (SolrServerException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
