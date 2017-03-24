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
package org.apache.ambari.infra.solr;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.util.NamedList;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class AmbariSolrCloudClientTest {

  private AmbariSolrCloudClient underTest;

  private CloudSolrClient mockedSolrClient;

  private SolrZkClient mockedSolrZkClient;

  private CollectionAdminResponse mockedResponse;

  @Before
  public void setUp() {
    AmbariSolrCloudClientBuilder builder = new AmbariSolrCloudClientBuilder();

    mockedSolrClient = createMock(CloudSolrClient.class);
    mockedSolrZkClient = createMock(SolrZkClient.class);
    mockedResponse = createMock(CollectionAdminResponse.class);

    builder.solrCloudClient = mockedSolrClient;
    builder.solrZkClient = mockedSolrZkClient;

    underTest = builder
      .withZkConnectString("localhost1:2181,localhost2:2182")
      .withCollection("collection1")
      .withConfigSet("configSet")
      .withShards(1)
      .withReplication(1)
      .withMaxShardsPerNode(2)
      .withInterval(1)
      .withRetry(2)
      .withRouterName("routerName")
      .withRouterField("routerField")
      .build();
  }

  @Test
  public void testCreateCollectionWhenCollectionDoesNotExist() throws Exception {
    // GIVEN
    NamedList<Object> namedList = new NamedList<>();
    namedList.add("collections", Arrays.asList("collection1", "collection2"));

    expect(mockedSolrClient.request(anyObject(CollectionAdminRequest.class), anyString())).andReturn(namedList).times(1);
    replay(mockedSolrClient);

    // WHEN
    String result = underTest.createCollection();
    // THEN
    assertEquals("collection1", result);
    verify(mockedSolrClient);
  }

  @Test
  public void testCreateCollectionWhenCollectionExists() throws Exception {
    // GIVEN
    NamedList<Object> namedList = new NamedList<>();
    namedList.add("collections", Arrays.asList("collection2", "collection3"));

    expect(mockedSolrClient.request(anyObject(CollectionAdminRequest.class), anyString())).andReturn(namedList).times(2);
    replay(mockedSolrClient);

    // WHEN
    String result = underTest.createCollection();
    // THEN
    assertEquals("collection1", result);
    verify(mockedSolrClient);
  }

  @Test
  public void testListCollections() throws Exception {
    // GIVEN
    NamedList<Object> namedList = new NamedList<>();
    namedList.add("collections", Arrays.asList("collection1", "collection2"));

    expect(mockedSolrClient.request(anyObject(CollectionAdminRequest.class), anyString())).andReturn(namedList);

    replay(mockedSolrClient);
    // WHEN
    List<String> result = underTest.listCollections();

    // THEN
    assertTrue(result.contains("collection1"));
    assertTrue(result.contains("collection2"));
    assertEquals(2, result.size());
  }

  @Test(expected = AmbariSolrCloudClientException.class)
  public void testRetries() throws Exception {
    // GIVEN
    expect(mockedSolrClient.request(anyObject(CollectionAdminRequest.class), anyString())).andThrow(new RuntimeException("ex")).times(2);
    replay(mockedSolrClient);
    // WHEN
    underTest.listCollections();
  }
}
