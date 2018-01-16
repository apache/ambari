/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.logfeeder.output;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.conf.LogFeederSecurityConfig;
import org.apache.ambari.logfeeder.input.Input;
import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.ambari.logsearch.config.api.LogSearchConfigLogFeeder;
import org.apache.ambari.logsearch.config.api.model.outputconfig.OutputSolrProperties;
import org.apache.ambari.logsearch.config.zookeeper.model.outputconfig.impl.OutputSolrPropertiesImpl;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OutputSolrTest {
  private static final Logger LOG = Logger.getLogger(OutputSolrTest.class);

  private OutputSolr outputSolr;
  private LogSearchConfigLogFeeder logSearchConfigMock;
  private Map<Integer, SolrInputDocument> receivedDocs = new ConcurrentHashMap<>();
  private LogFeederProps logFeederProps = new LogFeederProps();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void init() throws Exception {
    LogFeederSecurityConfig logFeederSecurityConfig = new LogFeederSecurityConfig();
    logFeederSecurityConfig.setSolrKerberosEnabled(false);
    logFeederProps.setLogFeederSecurityConfig(logFeederSecurityConfig);
    outputSolr = new OutputSolr() {
      @SuppressWarnings("deprecation")
      @Override
      CloudSolrClient getSolrClient(int count) throws Exception, MalformedURLException {
        return new CloudSolrClient(null) {
          private static final long serialVersionUID = 1L;

          @Override
          public UpdateResponse add(Collection<SolrInputDocument> docs) {
            for (SolrInputDocument doc : docs) {
              receivedDocs.put((Integer) doc.getField("id").getValue(), doc);
            }

            UpdateResponse response = new UpdateResponse();
            response.setResponse(new NamedList<Object>());
            return response;
          }
        };
      }
    };
    
    OutputSolrProperties outputSolrProperties = new OutputSolrPropertiesImpl("hadoop_logs", "none");
    logSearchConfigMock = EasyMock.createNiceMock(LogSearchConfigLogFeeder.class);
    EasyMock.expect(logSearchConfigMock.getOutputSolrProperties("service")).andReturn(outputSolrProperties);
    EasyMock.replay(logSearchConfigMock);
    
    outputSolr.setLogSearchConfig(logSearchConfigMock);
  }

  @Test
  public void testOutputToSolr_uploadData() throws Exception {
    LOG.info("testOutputToSolr_uploadData()");

    Map<String, Object> config = new HashMap<String, Object>();
    config.put("zk_connect_string", "some zk_connect_string");
    config.put("workers", "3");
    config.put("type", "service");

    outputSolr.loadConfig(config);
    outputSolr.init(logFeederProps);

    Map<Integer, SolrInputDocument> expectedDocs = new HashMap<>();

    int count = 0;
    for (int i = 0; i < 10; i++) {
      Map<String, Object> jsonObj = new HashMap<>();
      for (int j = 0; j < 3; j++)
        jsonObj.put("name" + ++count, "value" + ++count);
      jsonObj.put("id", ++count);

      InputMarker inputMarker = new InputMarker(EasyMock.mock(Input.class), null, 0);
      outputSolr.write(jsonObj, inputMarker);

      SolrInputDocument doc = new SolrInputDocument();
      for (Map.Entry<String, Object> e : jsonObj.entrySet())
        doc.addField(e.getKey(), e.getValue());

      expectedDocs.put(count, doc);
    }

    Thread.sleep(100);
    while (outputSolr.getPendingCount() > 0)
      Thread.sleep(100);

    int waitToFinish = 0;
    if (receivedDocs.size() < 10 && waitToFinish < 10) {
      Thread.sleep(100);
      waitToFinish++;
    }

    Set<Integer> ids = new HashSet<>();
    ids.addAll(receivedDocs.keySet());
    ids.addAll(expectedDocs.keySet());
    for (Integer id : ids) {
      SolrInputDocument receivedDoc = receivedDocs.get(id);
      SolrInputDocument expectedDoc = expectedDocs.get(id);

      assertNotNull("No document received for id: " + id, receivedDoc);
      assertNotNull("No document expected for id: " + id, expectedDoc);

      Set<String> fieldNames = new HashSet<>();
      fieldNames.addAll(receivedDoc.getFieldNames());
      fieldNames.addAll(expectedDoc.getFieldNames());

      for (String fieldName : fieldNames) {
        Object receivedValue = receivedDoc.getFieldValue(fieldName);
        Object expectedValue = expectedDoc.getFieldValue(fieldName);

        assertNotNull("No received document field found for id: " + id + ", fieldName: " + fieldName, receivedValue);
        assertNotNull("No expected document field found for id: " + id + ", fieldName: " + fieldName, expectedValue);

        assertEquals("Field value not matching for id: " + id + ", fieldName: " + fieldName, receivedValue, expectedValue);
      }
    }
  }

  @Test
  public void testOutputToSolr_noZkConnectString() throws Exception {
    LOG.info("testOutputToSolr_noUrlOrZkConnectString()");

    expectedException.expect(Exception.class);
    expectedException.expectMessage("For solr output the zk_connect_string property need to be set");

    Map<String, Object> config = new HashMap<String, Object>();
    config.put("workers", "3");
    config.put("type", "service");

    outputSolr.loadConfig(config);
    outputSolr.init(logFeederProps);
  }

  @After
  public void cleanUp() {
    receivedDocs.clear();
    EasyMock.verify(logSearchConfigMock);
  }
}
