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

package org.apache.ambari.logsearch.dao;

import java.util.ArrayList;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import junit.framework.Assert;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/applicationContext.xml" })
public class UserConfigSolrDaoTest {
  
  @Autowired
  private UserConfigSolrDao dao;
  
  @Test
  public void testUserConfigDaoPostConstructor() throws Exception {
    SolrClient mockSolrClient = EasyMock.strictMock(SolrClient.class);
    
    NamedList<Object> requestResponse = new NamedList<Object>();
    NamedList<Object> requestResponseHeader = new NamedList<Object>();
    requestResponseHeader.add("status", 0);
    requestResponse.add("responseHeader", requestResponseHeader);
    requestResponse.add("collections", new ArrayList<String>());
    
    QueryResponse queryResponse = new QueryResponse();
    
    UpdateResponse updateResponse = new UpdateResponse();
    NamedList<Object> updateResponseContent = new NamedList<Object>();
    NamedList<Object> updateResponseHeader = new NamedList<Object>();
    updateResponseHeader.add("QTime", 1);
    updateResponseContent.add("responseHeader", updateResponseHeader);
    updateResponse.setResponse(updateResponseContent);
    
    Capture<CollectionAdminRequest.Create> captureCreateRequest = EasyMock.newCapture(CaptureType.LAST);
    Capture<SolrParams> captureSolrParams = EasyMock.newCapture(CaptureType.LAST);
    Capture<METHOD> captureMethod = EasyMock.newCapture(CaptureType.LAST);
    Capture<SolrInputDocument> captureSolrInputDocument = EasyMock.newCapture(CaptureType.LAST);
    
    EasyMock.expect(mockSolrClient.request(EasyMock.anyObject(CollectionAdminRequest.List.class), EasyMock.anyString())).andReturn(requestResponse);
    mockSolrClient.request(EasyMock.capture(captureCreateRequest), EasyMock.anyString()); EasyMock.expectLastCall().andReturn(requestResponse);
    mockSolrClient.query(EasyMock.capture(captureSolrParams), EasyMock.capture(captureMethod)); EasyMock.expectLastCall().andReturn(queryResponse);
    mockSolrClient.add(EasyMock.capture(captureSolrInputDocument)); EasyMock.expectLastCall().andReturn(updateResponse);
    EasyMock.expect(mockSolrClient.commit()).andReturn(updateResponse);
    EasyMock.replay(mockSolrClient);
    
    dao.solrClient = mockSolrClient;
    dao.isZkConnectString = true;
    
    dao.postConstructor();
    EasyMock.verify(mockSolrClient);
    
    CollectionAdminRequest.Create createRequest = captureCreateRequest.getValue();
    Assert.assertEquals(createRequest.getConfigName(), "test_history_logs_config_name");
    Assert.assertEquals(createRequest.getReplicationFactor().intValue(), 234);
    Assert.assertEquals(createRequest.getCollectionName(), "test_history_logs_collection");
    
    SolrParams solrParams = captureSolrParams.getValue();
    Assert.assertEquals(solrParams.get("q"), "*:*");
    Assert.assertEquals(solrParams.get("fq"), "rowtype:log_feeder_config");
    
    METHOD method = captureMethod.getValue();
    Assert.assertEquals(method, METHOD.POST);
    
    SolrInputDocument solrInputDocument = captureSolrInputDocument.getValue();
    Assert.assertNotNull(solrInputDocument.getFieldValue("id"));
    Assert.assertEquals(solrInputDocument.getFieldValue("rowtype"), "log_feeder_config");
    Assert.assertEquals(solrInputDocument.getFieldValue("jsons"), "{\"filter\":{\"test_component2\":{\"label\":\"test_component2\",\"hosts\":[],\"defaultLevels\":[\"FATAL\",\"ERROR\",\"WARN\",\"INFO\",\"DEBUG\",\"TRACE\"],\"overrideLevels\":[]},\"test_component1\":{\"label\":\"test_component1\",\"hosts\":[],\"defaultLevels\":[\"FATAL\",\"ERROR\",\"WARN\",\"INFO\",\"DEBUG\",\"TRACE\"],\"overrideLevels\":[]}},\"id\":\"" + solrInputDocument.getFieldValue("id") + "\"}");
    Assert.assertEquals(solrInputDocument.getFieldValue("username"), "log_feeder_config");
    Assert.assertEquals(solrInputDocument.getFieldValue("filtername"), "log_feeder_config");
  }
  
  @Test
  public void testDeleteUserConfig() throws Exception {
    SolrClient mockSolrClient = EasyMock.strictMock(SolrClient.class);
    
    UpdateResponse updateResponse = new UpdateResponse();
    NamedList<Object> response = new NamedList<Object>();
    NamedList<Object> header = new NamedList<Object>();
    header.add("QTime", 1);
    response.add("responseHeader", header);
    updateResponse.setResponse(response);
    
    EasyMock.expect(mockSolrClient.deleteByQuery("id:test_id")).andReturn(updateResponse);
    EasyMock.expect(mockSolrClient.commit()).andReturn(updateResponse);
    EasyMock.replay(mockSolrClient);
    
    dao.solrClient = mockSolrClient;
    dao.isZkConnectString = true;
    
    dao.deleteUserConfig("test_id");
    
    EasyMock.verify(mockSolrClient);
  }
}
