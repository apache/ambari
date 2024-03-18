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
package org.apache.ambari.logsearch.solr;

import static org.apache.ambari.logsearch.solr.SolrConstants.AuditLogConstants.AUDIT_EVTTIME;
import static org.apache.ambari.logsearch.solr.SolrConstants.CommonLogConstants.ID;
import static org.apache.ambari.logsearch.solr.SolrConstants.CommonLogConstants.LOG_MESSAGE;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.LEVEL;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.LOGTIME;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.PivotField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.NodeConfig;
import org.apache.solr.core.SolrResourceLoader;

public class Solr {
  public static final DateTimeFormatter SOLR_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

  public static Solr core(String coreName) throws IOException, SolrServerException {
    assert coreName != null;

    String targetLocation = Solr.class.getProtectionDomain().getCodeSource().getLocation().getFile() + "..";
    String logsearchConfigSetDir = targetLocation + "/../../ambari-logsearch-server/src/main/configsets";
    File targetConfigSetDir = new File(targetLocation + "/configsets");
    if (targetConfigSetDir.exists())
      FileUtils.deleteDirectory(targetConfigSetDir);
    FileUtils.copyDirectory(new File(logsearchConfigSetDir), targetConfigSetDir);
    String solrHome = targetLocation + "/solr";
    File solrHomeDir = new File(solrHome);
    if (solrHomeDir.exists())
      FileUtils.deleteDirectory(solrHomeDir);
    solrHomeDir.mkdirs();

    SolrResourceLoader solrResourceLoader = new SolrResourceLoader(solrHomeDir.toPath());

    NodeConfig config = new NodeConfig.NodeConfigBuilder("embeddedSolrServerNode", solrResourceLoader)
            .setConfigSetBaseDirectory(targetConfigSetDir.getAbsolutePath())
            .build();

    EmbeddedSolrServer embeddedSolrServer = new EmbeddedSolrServer(config, coreName);

    CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
    createRequest.setCoreName(coreName);
    createRequest.setConfigSet(coreName);
    embeddedSolrServer.request(createRequest);

    return new Solr(embeddedSolrServer);
  }

  private final EmbeddedSolrServer server;

  private Solr(EmbeddedSolrServer solrServer) {
    server = solrServer;
  }

  public void addDoc(String id, String logMessage) throws SolrServerException, IOException {
    SolrInputDocument doc = new SolrInputDocument();
    doc.addField(ID, id);
    doc.addField(LOGTIME, new Date(OffsetDateTime.now().toInstant().toEpochMilli()));
    doc.addField(AUDIT_EVTTIME, new Date(OffsetDateTime.now().toInstant().toEpochMilli()));
    doc.addField(LOG_MESSAGE, logMessage);
    doc.addField(LEVEL, "INFO");
    server.add(doc);
    server.commit();
  }

  public SolrDocumentList executeQuery(SolrQuery solrQuery) throws SolrServerException, IOException {
    return server.query(solrQuery).getResults();
  }

  public NamedList<List<PivotField>> executeFacetQuery(SolrQuery solrQuery) throws SolrServerException, IOException {
    QueryResponse qResp = server.query(solrQuery);
    return qResp.getFacetPivot();
  }

  public void clear() throws Exception {
    server.deleteByQuery("*:*");
    server.commit();
  }

  public void close() throws IOException {
    server.close();
  }
}
