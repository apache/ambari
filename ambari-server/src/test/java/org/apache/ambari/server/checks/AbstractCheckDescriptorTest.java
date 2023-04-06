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

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.RepositoryType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.repository.ClusterVersionSummary;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.stack.PrereqCheckType;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.google.inject.Provider;

import junit.framework.Assert;

/**
 * Unit tests for AbstractCheckDescriptor
 */
public class AbstractCheckDescriptorTest extends EasyMockSupport {
  @Mock
  private Clusters clusters;

  /**
   * Used to mock out what services will be provided to us by the VDF/cluster.
   */
  @Mock
  private ClusterVersionSummary m_clusterVersionSummary;

  /**
   *
   */
  @Mock
  private VersionDefinitionXml m_vdfXml;

  @Before
  public void setup() throws Exception {
    injectMocks(this);
  }

  @Test
  public void testFormatEntityList() {
    AbstractCheckDescriptor check = new TestCheckImpl(PrereqCheckType.HOST);

    Assert.assertEquals("", check.formatEntityList(null));

    final LinkedHashSet<String> failedOn = new LinkedHashSet<>();
    Assert.assertEquals("", check.formatEntityList(failedOn));

    failedOn.add("host1");
    Assert.assertEquals("host1", check.formatEntityList(failedOn));

    failedOn.add("host2");
    Assert.assertEquals("host1 and host2", check.formatEntityList(failedOn));

    failedOn.add("host3");
    Assert.assertEquals("host1, host2 and host3", check.formatEntityList(failedOn));

    check = new TestCheckImpl(PrereqCheckType.CLUSTER);
    Assert.assertEquals("host1, host2 and host3", check.formatEntityList(failedOn));

    check = new TestCheckImpl(PrereqCheckType.SERVICE);
    Assert.assertEquals("host1, host2 and host3", check.formatEntityList(failedOn));

    check = new TestCheckImpl(null);
    Assert.assertEquals("host1, host2 and host3", check.formatEntityList(failedOn));
  }

  @Test
  public void testIsApplicable() throws Exception{
    final String clusterName = "c1";
    final Cluster cluster = createMock(Cluster.class);


    Map<String, Service> services = new HashMap<String, Service>(){{
      put("SERVICE1", null);
      put("SERVICE2", null);
      put("SERVICE3", null);
    }};

    Set<String> oneServiceList = Sets.newHashSet("SERVICE1");
    Set<String> atLeastOneServiceList = Sets.newHashSet("SERVICE1", "MISSING_SERVICE");
    Set<String> allServicesList = Sets.newHashSet("SERVICE1", "SERVICE2");
    Set<String> missingServiceList = Sets.newHashSet("MISSING_SERVICE");

    expect(clusters.getCluster(anyString())).andReturn(cluster).atLeastOnce();
    expect(cluster.getServices()).andReturn(services).atLeastOnce();

    RepositoryVersionEntity repositoryVersion = createNiceMock(RepositoryVersionEntity.class);
    expect(repositoryVersion.getType()).andReturn(RepositoryType.STANDARD).anyTimes();
    expect(repositoryVersion.getRepositoryXml()).andReturn(m_vdfXml).atLeastOnce();
    expect(m_vdfXml.getClusterSummary(EasyMock.anyObject(Cluster.class))).andReturn(
        m_clusterVersionSummary).atLeastOnce();

    expect(m_clusterVersionSummary.getAvailableServiceNames()).andReturn(
        allServicesList).atLeastOnce();


    replayAll();

    TestCheckImpl check = new TestCheckImpl(PrereqCheckType.SERVICE);
    PrereqCheckRequest request = new PrereqCheckRequest(clusterName, UpgradeType.ROLLING);
    request.setTargetRepositoryVersion(repositoryVersion);

    // case, where we need at least one service to be present
    check.setApplicableServices(oneServiceList);
    Assert.assertTrue(check.isApplicable(request));

    check.setApplicableServices(atLeastOneServiceList);
    Assert.assertTrue(check.isApplicable(request));

    check.setApplicableServices(missingServiceList);
    Assert.assertFalse(check.isApplicable(request));
  }

  /**
   * Tests that even though the services are installed, the check doesn't match
   * since it's for a service not in the PATCH.
   *
   * @throws Exception
   */
  @Test
  public void testIsApplicableForPatch() throws Exception {
    final String clusterName = "c1";
    final Cluster cluster = createMock(Cluster.class);

    Map<String, Service> services = new HashMap<String, Service>() {
      {
        put("SERVICE1", null);
        put("SERVICE2", null);
        put("SERVICE3", null);
      }
    };

    Set<String> oneServiceList = Sets.newHashSet("SERVICE1");

    expect(clusters.getCluster(anyString())).andReturn(cluster).atLeastOnce();
    expect(cluster.getServices()).andReturn(services).atLeastOnce();

    RepositoryVersionEntity repositoryVersion = createNiceMock(RepositoryVersionEntity.class);
    expect(repositoryVersion.getType()).andReturn(RepositoryType.STANDARD).anyTimes();
    expect(repositoryVersion.getRepositoryXml()).andReturn(m_vdfXml).atLeastOnce();
    expect(m_vdfXml.getClusterSummary(EasyMock.anyObject(Cluster.class))).andReturn(
        m_clusterVersionSummary).atLeastOnce();

    // the cluster summary will only return 1 service for the upgrade, even
    // though this cluster has 2 services installed
    expect(m_clusterVersionSummary.getAvailableServiceNames()).andReturn(
        oneServiceList).atLeastOnce();

    replayAll();

    TestCheckImpl check = new TestCheckImpl(PrereqCheckType.SERVICE);
    PrereqCheckRequest request = new PrereqCheckRequest(clusterName, UpgradeType.ROLLING);
    request.setTargetRepositoryVersion(repositoryVersion);

    // since the check is for SERVICE2, it should not match even though its
    // installed since the repository is only for SERVICE1
    check.setApplicableServices(Sets.newHashSet("SERVICE2"));
    Assert.assertFalse(check.isApplicable(request));

    // ok, so now change the check to match against SERVICE1
    check.setApplicableServices(Sets.newHashSet("SERVICE1"));
    Assert.assertTrue(check.isApplicable(request));
  }

  /**
   * Tests {@link UpgradeCheck#required()}.
   *
   * @throws Exception
   */
  @Test
  public void testRequired() throws Exception {
    RollingTestCheckImpl rollingCheck = new RollingTestCheckImpl(PrereqCheckType.SERVICE);
    Assert.assertTrue(rollingCheck.isRequired(UpgradeType.ROLLING));
    Assert.assertFalse(rollingCheck.isRequired(UpgradeType.NON_ROLLING));

    NotRequiredCheckTest notRequiredCheck = new NotRequiredCheckTest(PrereqCheckType.SERVICE);
    Assert.assertFalse(notRequiredCheck.isRequired(UpgradeType.ROLLING));
    Assert.assertFalse(notRequiredCheck.isRequired(UpgradeType.NON_ROLLING));
    Assert.assertFalse(notRequiredCheck.isRequired(UpgradeType.HOST_ORDERED));

    TestCheckImpl requiredCheck = new TestCheckImpl(PrereqCheckType.SERVICE);
    Assert.assertTrue(requiredCheck.isRequired(UpgradeType.ROLLING));
    Assert.assertTrue(requiredCheck.isRequired(UpgradeType.NON_ROLLING));
    Assert.assertTrue(requiredCheck.isRequired(UpgradeType.HOST_ORDERED));
  }

  @UpgradeCheck(
      group = UpgradeCheckGroup.DEFAULT,
      order = 1.0f,
      required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
  private class TestCheckImpl extends AbstractCheckDescriptor {
    private PrereqCheckType m_type;
    private Set<String> m_applicableServices = Sets.newHashSet();

    TestCheckImpl(PrereqCheckType type) {
      super(null);
      m_type = type;

      clustersProvider = new Provider<Clusters>() {
        @Override
        public Clusters get() {
          return clusters;
        }
      };
    }

    @Override
    public PrereqCheckType getType() {
      return m_type;
    }

    @Override
    public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request)
        throws AmbariException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getApplicableServices() {
      return m_applicableServices;
    }

    void setApplicableServices(Set<String> applicableServices) {
      m_applicableServices = applicableServices;
    }
  }

  @UpgradeCheck(group = UpgradeCheckGroup.DEFAULT, order = 1.0f, required = { UpgradeType.ROLLING })
  private class RollingTestCheckImpl extends AbstractCheckDescriptor {
    private PrereqCheckType m_type;

    RollingTestCheckImpl(PrereqCheckType type) {
      super(null);
      m_type = type;

      clustersProvider = new Provider<Clusters>() {
        @Override
        public Clusters get() {
          return clusters;
        }
      };
    }

    @Override
    public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request)
        throws AmbariException {
    }
  }

  @UpgradeCheck(group = UpgradeCheckGroup.DEFAULT, order = 1.0f)
  private class NotRequiredCheckTest extends AbstractCheckDescriptor {
    private PrereqCheckType m_type;

    NotRequiredCheckTest(PrereqCheckType type) {
      super(null);
      m_type = type;

      clustersProvider = new Provider<Clusters>() {
        @Override
        public Clusters get() {
          return clusters;
        }
      };
    }

    @Override
    public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request)
        throws AmbariException {
    }
  }
}
