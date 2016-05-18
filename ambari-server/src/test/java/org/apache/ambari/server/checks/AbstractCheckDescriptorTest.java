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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceImpl;
import org.apache.ambari.server.state.stack.PrereqCheckType;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.upgrade.RepositoryVersionHelper;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.collections.map.HashedMap;
import org.easymock.EasyMock;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;

/**
 * Unit tests for AbstractCheckDescriptor
 */
public class AbstractCheckDescriptorTest {
  final private Clusters clusters = EasyMock.createNiceMock(Clusters.class);

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

  @Test
  public void testFormatEntityList() {
    AbstractCheckDescriptor check = new TestCheckImpl(PrereqCheckType.HOST);

    Assert.assertEquals("", check.formatEntityList(null));

    final LinkedHashSet<String> failedOn = new LinkedHashSet<String>();
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

    List<String> oneServiceList = new ArrayList<String>() {{
      add("SERVICE1");
    }};
    List<String> atLeastOneServiceList = new ArrayList<String>() {{
      add("SERVICE1");
      add("NON_EXISTED_SERVICE");
    }};
    List<String> allServicesList = new ArrayList<String>(){{
      add("SERVICE1");
      add("SERVICE2");
    }};
    List<String> nonExistedList = new ArrayList<String>(){{
      add("NON_EXISTED_SERVICE");
    }};

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

}
