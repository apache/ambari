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
package org.apache.ambari.server.checks;

import java.util.Collections;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.inject.Provider;

/**
 * Unit tests for ServicesOozieDistributedCacheCheck
 *
 */
public class ServicesOozieDistributedCacheCheckTest {
  private final Clusters clusters = Mockito.mock(Clusters.class);

  private final ServicesOozieDistributedCacheCheck servicesOozieDistributedCacheCheck = new ServicesOozieDistributedCacheCheck();

  @Before
  public void setup() {
    servicesOozieDistributedCacheCheck.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return clusters;
      }
    };
  }

  @Test
  public void testIsApplicable() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);

    final Service service = Mockito.mock(Service.class);
    Mockito.when(cluster.getService("OOZIE")).thenReturn(service);
    Assert.assertTrue(servicesOozieDistributedCacheCheck.isApplicable(new PrereqCheckRequest("cluster")));

    Mockito.when(cluster.getService("OOZIE")).thenThrow(new ServiceNotFoundException("no", "service"));
    Assert.assertFalse(servicesOozieDistributedCacheCheck.isApplicable(new PrereqCheckRequest("cluster")));
  }

  @Test
  public void testPerform() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(clusters.getClusters()).thenReturn(Collections.singletonMap("cluster", cluster));

    servicesOozieDistributedCacheCheck.mapReduceCheck = new Provider<ServicesMapReduceDistributedCacheCheck>() {
      @Override
      public ServicesMapReduceDistributedCacheCheck get() {
        final ServicesMapReduceDistributedCacheCheck check = Mockito.mock(ServicesMapReduceDistributedCacheCheck.class);
        return check;
      }
    };

    PrerequisiteCheck check = new PrerequisiteCheck(null, null, null, null);
    servicesOozieDistributedCacheCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    servicesOozieDistributedCacheCheck.mapReduceCheck = new Provider<ServicesMapReduceDistributedCacheCheck>() {
      @Override
      public ServicesMapReduceDistributedCacheCheck get() {
        final ServicesMapReduceDistributedCacheCheck check = Mockito.mock(ServicesMapReduceDistributedCacheCheck.class);
        try {
          Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
              PrerequisiteCheck check = (PrerequisiteCheck) invocation.getArguments()[0];
              check.setStatus(PrereqCheckStatus.FAIL);
              return null;
            }
          }).when(check).perform(Mockito.<PrerequisiteCheck> anyObject(), Mockito.<PrereqCheckRequest> anyObject());
        } catch (AmbariException e) {
        }
        return check;
      }
    };

    check = new PrerequisiteCheck(null, null, null, null);
    servicesOozieDistributedCacheCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
  }
}
