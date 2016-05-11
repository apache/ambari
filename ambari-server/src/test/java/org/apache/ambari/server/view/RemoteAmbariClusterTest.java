/**
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

package org.apache.ambari.server.view;

import org.apache.ambari.server.orm.entities.RemoteAmbariClusterEntity;
import org.apache.ambari.view.AmbariStreamProvider;
import org.easymock.IAnswer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

public class RemoteAmbariClusterTest {

  @Rule
  public ExpectedException thrown= ExpectedException.none();


  @Test
  public void testGetConfigurationValue() throws Exception {

    AmbariStreamProvider clusterStreamProvider = createNiceMock(AmbariStreamProvider.class);

    final String desiredConfigsString = "{\"Clusters\": {\"desired_configs\": {\"test-site\": {\"tag\": \"TAG\"}}}}";
    final String configurationString = "{\"items\": [{\"properties\": {\"test.property.name\": \"test property value\"}}]}";
    final int[] desiredConfigPolls = {0};
    final int[] testConfigPolls = {0};

    expect(clusterStreamProvider.readFrom(eq(  "?fields=services/ServiceInfo,hosts,Clusters"),
      eq("GET"), (String) isNull(), (Map<String, String>) anyObject())).andAnswer(new IAnswer<InputStream>() {
      @Override
      public InputStream answer() throws Throwable {
        desiredConfigPolls[0] += 1;
        return new ByteArrayInputStream(desiredConfigsString.getBytes());
      }
    }).anyTimes();

    expect(clusterStreamProvider.readFrom(eq( "/configurations?(type=test-site&tag=TAG)"),
      eq("GET"), (String)isNull(), (Map<String, String>) anyObject())).andAnswer(new IAnswer<InputStream>() {
      @Override
      public InputStream answer() throws Throwable {
        testConfigPolls[0] += 1;
        return new ByteArrayInputStream(configurationString.getBytes());
      }
    }).anyTimes();

    RemoteAmbariClusterEntity entity = createNiceMock(RemoteAmbariClusterEntity.class);

    replay(clusterStreamProvider,entity);

    RemoteAmbariCluster cluster = new RemoteAmbariCluster("Test", clusterStreamProvider);

    String value = cluster.getConfigurationValue("test-site", "test.property.name");
    assertEquals(value, "test property value");
    assertEquals(desiredConfigPolls[0], 1);
    assertEquals(testConfigPolls[0], 1);

    value = cluster.getConfigurationValue("test-site", "test.property.name");
    assertEquals(value, "test property value");
    assertEquals(desiredConfigPolls[0], 1);  // cache hit
    assertEquals(testConfigPolls[0], 1);
  }
}