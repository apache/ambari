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
import java.util.Arrays;

import javax.ws.rs.WebApplicationException;

import org.apache.ambari.logsearch.manager.MgrBase.LogType;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.easymock.EasyMock;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import junit.framework.Assert;

public class SolrDaoBaseTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  
  // ----------------------------------------------------------- connectToSolr -----------------------------------------------------------
  
  @Test
  public void testConnectToSolrWithConnectString() throws Exception {
    SolrDaoBase dao = new SolrDaoBase(null) {};
    SolrClient solrClient = dao.connectToSolr(null, "zk_connect_string", "collection");
    
    Assert.assertEquals(solrClient.getClass(), CloudSolrClient.class);
  }
  
  @Test
  public void testConnectToSolrWithUrl() throws Exception {
    SolrDaoBase dao = new SolrDaoBase(null) {};
    SolrClient solrClient = dao.connectToSolr("url", null, "collection");
    
    Assert.assertEquals(solrClient.getClass(), HttpSolrClient.class);
  }
  
  @Test
  public void testConnectToSolrWithBoth() throws Exception {
    SolrDaoBase dao = new SolrDaoBase(null) {};
    SolrClient solrClient = dao.connectToSolr("url", "zk_connect_string", "collection");
    
    Assert.assertEquals(solrClient.getClass(), CloudSolrClient.class);
  }
  
  @Test
  public void testConnectToSolrWithNeither() throws Exception {
    expectedException.expect(Exception.class);
    expectedException.expectMessage("Both zkConnectString and URL are empty. zkConnectString=null, collection=collection, url=null");

    SolrDaoBase dao = new SolrDaoBase(null) {};
    dao.connectToSolr(null, null, "collection");
  }
  
  @Test
  public void testConnectToSolrWithoutCollection() throws Exception {
    expectedException.expect(Exception.class);
    expectedException.expectMessage("For solr, collection name is mandatory. zkConnectString=zk_connect_string, collection=null, url=url");

    SolrDaoBase dao = new SolrDaoBase(null) {};
    dao.connectToSolr("url", "zk_connect_string", null);
  }
  
  // ---------------------------------------------------------- checkSolrStatus ----------------------------------------------------------
  
  @Test
  public void testCheckSolrStatus() throws Exception {
    SolrClient mockSolrClient = EasyMock.strictMock(SolrClient.class);
    
    NamedList<Object> response = new NamedList<Object>();
    NamedList<Object> header = new NamedList<Object>();
    header.add("status", 0);
    response.add("responseHeader", header);
    response.add("collections", new ArrayList<String>());
    
    EasyMock.expect(mockSolrClient.request(EasyMock.anyObject(CollectionAdminRequest.List.class), EasyMock.anyString())).andReturn(response);
    EasyMock.replay(mockSolrClient);
    
    SolrDaoBase dao = new SolrDaoBase(null) {};
    dao.solrClient = mockSolrClient;
    
    boolean status = dao.checkSolrStatus(10000);
    Assert.assertTrue(status);
    
    EasyMock.verify(mockSolrClient);
  }
  
  @Test
  public void testCheckSolrStatusNotSuccessful() throws Exception {
    SolrClient mockSolrClient = EasyMock.strictMock(SolrClient.class);
    EasyMock.replay(mockSolrClient);
    
    SolrDaoBase dao = new SolrDaoBase(null) {};
    dao.solrClient = mockSolrClient;
    
    boolean status = dao.checkSolrStatus(10000);
    Assert.assertFalse(status);
    
    EasyMock.verify(mockSolrClient);
  }
  
  // ------------------------------------------------------------- setupAlias ------------------------------------------------------------
  
  @Test
  public void testSetupAlias() throws Exception {
    SolrClient mockSolrClient = EasyMock.strictMock(SolrClient.class);
    CloudSolrClient mockSolrClouldClient = EasyMock.strictMock(CloudSolrClient.class);
    
    NamedList<Object> response = new NamedList<Object>();
    NamedList<Object> header = new NamedList<Object>();
    header.add("status", 0);
    response.add("responseHeader", header);
    response.add("collections", Arrays.asList("collection1", "collection2"));
    
    EasyMock.expect(mockSolrClient.request(EasyMock.anyObject(CollectionAdminRequest.List.class), EasyMock.anyString())).andReturn(response);
    EasyMock.expect(mockSolrClouldClient.request(EasyMock.anyObject(CollectionAdminRequest.CreateAlias.class), EasyMock.anyString())).andReturn(response);
    mockSolrClouldClient.setDefaultCollection("alias_name"); EasyMock.expectLastCall();
    
    EasyMock.replay(mockSolrClient, mockSolrClouldClient);
    
    SolrDaoBase dao = new SolrDaoBase(null) {};
    dao.isZkConnectString = true;
    dao.solrClient = mockSolrClient;
    dao.solrClouldClient = mockSolrClouldClient;
    dao.collectionName = "test_collection";
    
    dao.setupAlias("alias_name", Arrays.asList("collection1", "collection2"));
    
    Thread.sleep(1000);
    
    EasyMock.verify(mockSolrClient, mockSolrClouldClient);
  }
  
  // ---------------------------------------------------------- setupCollections ---------------------------------------------------------
  
  @Test
  public void testCreateCollectionsDontSplitPopulate() throws Exception {
    SolrClient mockSolrClient = EasyMock.strictMock(SolrClient.class);
    
    NamedList<Object> response = new NamedList<Object>();
    NamedList<Object> header = new NamedList<Object>();
    header.add("status", 0);
    response.add("responseHeader", header);
    response.add("collections", new ArrayList<String>());
    
    EasyMock.expect(mockSolrClient.request(EasyMock.anyObject(CollectionAdminRequest.List.class), EasyMock.anyString())).andReturn(response);
    EasyMock.expect(mockSolrClient.request(EasyMock.anyObject(CollectionAdminRequest.Create.class), EasyMock.anyString())).andReturn(response);
    EasyMock.replay(mockSolrClient);
    
    SolrDaoBase dao = new SolrDaoBase(null) {};
    dao.isZkConnectString = true;
    dao.solrClient = mockSolrClient;
    dao.collectionName = "test_collection";
    
    dao.setupCollections("none", "configName", 1, 1, true);
    
    EasyMock.verify(mockSolrClient);
  }
  
  @Test
  public void testCreateCollectionsSplitDontPopulate() throws Exception {
    SolrClient mockSolrClient = EasyMock.strictMock(SolrClient.class);
    
    NamedList<Object> response = new NamedList<Object>();
    NamedList<Object> header = new NamedList<Object>();
    header.add("status", 0);
    response.add("responseHeader", header);
    response.add("collections", new ArrayList<String>());
    
    EasyMock.expect(mockSolrClient.request(EasyMock.anyObject(CollectionAdminRequest.List.class), EasyMock.anyString())).andReturn(response);
    EasyMock.expect(mockSolrClient.request(EasyMock.anyObject(CollectionAdminRequest.Create.class), EasyMock.anyString())).andReturn(response);
    EasyMock.replay(mockSolrClient);
    
    SolrDaoBase dao = new SolrDaoBase(null) {};
    dao.isZkConnectString = true;
    dao.solrClient = mockSolrClient;
    
    dao.setupCollections("1", "configName", 3, 1, false);
    
    EasyMock.verify(mockSolrClient);
  }
  
  // -------------------------------------------------------------- process --------------------------------------------------------------
  
  @Test
  public void testProcess() throws Exception {
    SolrClient mockSolrClient = EasyMock.strictMock(SolrClient.class);
    EasyMock.expect(mockSolrClient.query(EasyMock.anyObject(SolrQuery.class), EasyMock.eq(METHOD.POST))).andReturn(new QueryResponse());
    EasyMock.replay(mockSolrClient);
    
    SolrDaoBase dao = new SolrDaoBase(null) {};
    dao.solrClient = mockSolrClient;
    
    dao.process(new SolrQuery());
    
    EasyMock.verify(mockSolrClient);
  }
  
  @Test
  public void testProcessNoConnection() throws Exception {
    expectedException.expect(WebApplicationException.class);
    
    SolrDaoBase dao = new SolrDaoBase(LogType.SERVICE) {};
    dao.restErrorUtil = new RESTErrorUtil();
    dao.process(new SolrQuery());
  }
  
  // ----------------------------------------------------------- add/removeDoc -----------------------------------------------------------
  
  @Test
  public void testAddDoc() throws Exception {
    SolrClient mockSolrClient = EasyMock.strictMock(SolrClient.class);
    
    UpdateResponse updateResponse = new UpdateResponse();
    NamedList<Object> response = new NamedList<Object>();
    NamedList<Object> header = new NamedList<Object>();
    header.add("QTime", 1);
    response.add("responseHeader", header);
    updateResponse.setResponse(response);
    
    EasyMock.expect(mockSolrClient.add(EasyMock.anyObject(SolrInputDocument.class))).andReturn(updateResponse);
    EasyMock.expect(mockSolrClient.commit()).andReturn(updateResponse);
    EasyMock.replay(mockSolrClient);
    
    SolrDaoBase dao = new SolrDaoBase(null) {};
    dao.solrClient = mockSolrClient;
    
    dao.addDocs(new SolrInputDocument());
    
    EasyMock.verify(mockSolrClient);
  }
  
  @Test
  public void testRemoveDoc() throws Exception {
    SolrClient mockSolrClient = EasyMock.strictMock(SolrClient.class);
    
    UpdateResponse updateResponse = new UpdateResponse();
    NamedList<Object> response = new NamedList<Object>();
    NamedList<Object> header = new NamedList<Object>();
    header.add("QTime", 1);
    response.add("responseHeader", header);
    updateResponse.setResponse(response);
    
    EasyMock.expect(mockSolrClient.deleteByQuery(EasyMock.anyString())).andReturn(updateResponse);
    EasyMock.expect(mockSolrClient.commit()).andReturn(updateResponse);
    EasyMock.replay(mockSolrClient);
    
    SolrDaoBase dao = new SolrDaoBase(null) {};
    dao.solrClient = mockSolrClient;
    
    dao.removeDoc("query");
    
    EasyMock.verify(mockSolrClient);
  }
}
