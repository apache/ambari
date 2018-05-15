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
package org.apache.ambari.infra;

import static org.apache.ambari.infra.TestUtil.doWithin;
import static org.apache.ambari.infra.TestUtil.getDockerHost;
import static org.apache.ambari.infra.TestUtil.runCommand;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Solr {
  private static final Logger LOG = LoggerFactory.getLogger(Solr.class);
  public static final String AUDIT_LOGS_COLLECTION = "audit_logs";
  public static final String HADOOP_LOGS_COLLECTION = "hadoop_logs";
  private static final int SOLR_PORT = 8983;

  private final SolrClient solrClient;
  private final String configSetPath;

  public Solr() {
    this("");
  }

  public Solr(String configSetPath) {
    this.configSetPath = configSetPath;
    this.solrClient = new LBHttpSolrClient.Builder().withBaseSolrUrls(String.format("http://%s:%d/solr/%s_shard1_replica1",
            getDockerHost(),
            SOLR_PORT,
            AUDIT_LOGS_COLLECTION)).build();
  }

  public  void waitUntilSolrIsUp() throws Exception {
    try (CloseableHttpClient httpClient = HttpClientBuilder.create().setRetryHandler(new DefaultHttpRequestRetryHandler(0, false)).build()) {
      doWithin(60, "Check Solr running", () -> pingSolr(httpClient));
    }
  }

  private boolean pingSolr(CloseableHttpClient httpClient) {
    try (CloseableHttpResponse response = httpClient.execute(new HttpGet(String.format("http://%s:%d/solr/admin/collections?action=LIST", getDockerHost(), SOLR_PORT)))) {
      return response.getStatusLine().getStatusCode() == 200;
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void add(SolrInputDocument solrInputDocument) {
    try {
      solrClient.add(solrInputDocument);
    } catch (SolrServerException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void createSolrCollection(String collectionName) {
    LOG.info("Creating collection");
    runCommand(new String[]{"docker", "exec", "docker_solr_1", "solr", "create_collection", "-force", "-c", collectionName, "-d", Paths.get(configSetPath, "configsets", collectionName, "conf").toString(), "-n", collectionName + "_conf"});
  }

  public QueryResponse query(SolrQuery query) {
    try {
      return solrClient.query(query);
    } catch (SolrServerException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void commit() {
    try {
      solrClient.commit();
    } catch (SolrServerException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
