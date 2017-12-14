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
package org.apache.ambari.server.checks;

import static org.mockito.Mockito.mock;

import java.util.Set;

import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Sets;
import com.google.inject.Provider;

/**
 * Tests {@link RequiredServicesInRepositoryCheck}.
 */
@RunWith(MockitoJUnitRunner.class)
public class RequiredServicesInRepositoryCheckTest {

  private static final String CLUSTER_NAME = "c1";

  @Mock
  private VersionDefinitionXml m_vdfXml;

  @Mock
  private RepositoryVersionEntity m_repositoryVersion;

  private RequiredServicesInRepositoryCheck m_requiredServicesCheck;

  /**
   * Used to return the missing dependencies for the test.
   */
  private Set<String> m_missingDependencies = Sets.newTreeSet();

  @Before
  public void setUp() throws Exception {
    final Clusters clusters = mock(Clusters.class);
    m_requiredServicesCheck = new RequiredServicesInRepositoryCheck();
    m_requiredServicesCheck.clustersProvider = new Provider<Clusters>() {
      @Override
      public Clusters get() {
        return clusters;
      }
    };

    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(clusters.getCluster(CLUSTER_NAME)).thenReturn(cluster);

    Mockito.when(m_repositoryVersion.getRepositoryXml()).thenReturn(m_vdfXml);
    Mockito.when(m_vdfXml.getMissingDependencies(Mockito.eq(cluster))).thenReturn(m_missingDependencies);
  }

  /**
   * Tests that a no missing services results in a passed test.
   *
   * @throws Exception
   */
  @Test
  public void testNoMissingServices() throws Exception {
    PrereqCheckRequest request = new PrereqCheckRequest(CLUSTER_NAME);
    request.setTargetRepositoryVersion(m_repositoryVersion);

    PrerequisiteCheck check = new PrerequisiteCheck(null, CLUSTER_NAME);
    m_requiredServicesCheck.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());
  }

  /**
   * Tests that a missing required service causes the test to fail.
   *
   * @throws Exception
   */
  @Test
  public void testMissingRequiredService() throws Exception {
    PrereqCheckRequest request = new PrereqCheckRequest(CLUSTER_NAME);
    request.setTargetRepositoryVersion(m_repositoryVersion);

    m_missingDependencies.add("BAR");

    PrerequisiteCheck check = new PrerequisiteCheck(null, CLUSTER_NAME);
    m_requiredServicesCheck.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
  }
}