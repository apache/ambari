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
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.RepositoryType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.stack.PrereqCheckType;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Provider;

/**
 * Unit tests for AbstractCheckDescriptor
 */
public class AbstractCheckDescriptorTest {
  final private Clusters clusters = EasyMock.createNiceMock(Clusters.class);
  private final RepositoryVersionDAO repositoryVersionDao = EasyMock.createNiceMock(RepositoryVersionDAO.class);

  @UpgradeCheck(
      group = UpgradeCheckGroup.DEFAULT,
      order = 1.0f,
      required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
  private class TestCheckImpl extends AbstractCheckDescriptor {
    private PrereqCheckType m_type;

    TestCheckImpl(PrereqCheckType type) {
      super(null);
      m_type = type;

      clustersProvider = new Provider<Clusters>() {
        @Override
        public Clusters get() {
          return clusters;
        }
      };

      repositoryVersionDaoProvider = new Provider<RepositoryVersionDAO>() {
        @Override
        public RepositoryVersionDAO get() {
          return repositoryVersionDao;
        }
      };
    }

    @Override
    public PrereqCheckType getType() {
      return m_type;
    }

    @Override
    public void perform(PrerequisiteCheck prerequisiteCheck,
        PrereqCheckRequest request) throws AmbariException {
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
    final Cluster cluster = EasyMock.createMock(Cluster.class);


    Map<String, Service> services = new HashMap<String, Service>(){{
      put("SERVICE1", null);
      put("SERVICE2", null);
      put("SERVICE3", null);
    }};

    expect(clusters.getCluster(anyString())).andReturn(cluster).atLeastOnce();
    expect(cluster.getServices()).andReturn(services).atLeastOnce();

    replay(clusters, cluster);

    AbstractCheckDescriptor check = new TestCheckImpl(PrereqCheckType.SERVICE);
    PrereqCheckRequest request = new PrereqCheckRequest(clusterName, UpgradeType.ROLLING);

    List<String> oneServiceList = Arrays.asList("SERVICE1");

    List<String> atLeastOneServiceList = Arrays.asList("SERVICE1", "NON_EXISTED_SERVICE");

    List<String> allServicesList = Arrays.asList("SERVICE1", "SERVICE2");

    List<String> nonExistedList = Arrays.asList("NON_EXISTED_SERVICE");


    // case, where we need at least one service to be present
    Assert.assertEquals(true, check.isApplicable(request, oneServiceList, false));
    Assert.assertEquals(true, check.isApplicable(request, atLeastOneServiceList, false));

    // case, where all services need to be present
    Assert.assertEquals(false, check.isApplicable(request,atLeastOneServiceList, true));
    Assert.assertEquals(true, check.isApplicable(request, allServicesList, true));

    // Case with empty list of the required services
    Assert.assertEquals(false, check.isApplicable(request, new ArrayList<String>(), true));
    Assert.assertEquals(false, check.isApplicable(request, new ArrayList<String>(), false));

    // Case with non existed services
    Assert.assertEquals(false, check.isApplicable(request, nonExistedList, false));
    Assert.assertEquals(false, check.isApplicable(request, nonExistedList, true));
  }

  @Test
  public void testIsApplicableWithVDF() throws Exception{
    final String clusterName = "c1";
    final Cluster cluster = EasyMock.createMock(Cluster.class);

    Map<String, Service> services = new HashMap<String, Service>(){{
      put("SERVICE1", null);
      put("SERVICE2", null);
      put("SERVICE3", null);
    }};

    expect(clusters.getCluster(anyString())).andReturn(cluster).atLeastOnce();
    expect(cluster.getServices()).andReturn(services).atLeastOnce();

    RepositoryVersionEntity repoVersion = EasyMock.createMock(RepositoryVersionEntity.class);
    VersionDefinitionXml repoXml = EasyMock.createMock(VersionDefinitionXml.class);
    expect(repoVersion.getType()).andReturn(RepositoryType.PATCH).atLeastOnce();
    expect(repoVersion.getRepositoryXml()).andReturn(repoXml).atLeastOnce();
    expect(repoXml.getAvailableServiceNames()).andReturn(Collections.singleton("SERVICE2")).atLeastOnce();

    expect(repositoryVersionDao.findByStackNameAndVersion(
        anyString(), anyString())).andReturn(repoVersion).atLeastOnce();

    replay(clusters, cluster, repositoryVersionDao, repoVersion, repoXml);

    AbstractCheckDescriptor check = new TestCheckImpl(PrereqCheckType.SERVICE);
    PrereqCheckRequest request = new PrereqCheckRequest(clusterName, UpgradeType.ROLLING);
    request.setTargetStackId(new StackId("HDP-2.5"));

    List<String> allServicesList = Arrays.asList("SERVICE1", "SERVICE2");

    // SERVICE2 is the only thing in VDF
    Assert.assertEquals(true, check.isApplicable(request, allServicesList, false));

    List<String> oneServicesList = Arrays.asList("SERVICE1");

    // SERVICE2 is the only thing in VDF, a check for only SERVICE1 fail
    Assert.assertEquals(false, check.isApplicable(request, oneServicesList, false));

    // a VDF without available services is technically invalid, so expect any passed services to return false
    reset(repoXml);
    expect(repoXml.getAvailableServiceNames()).andReturn(Collections.<String>emptySet()).atLeastOnce();
    replay(repoXml);
    Assert.assertEquals(false, check.isApplicable(request, allServicesList, false));
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

}
