/**
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology;

import junit.framework.Assert;
import org.easymock.EasyMockRule;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

/**
 * Unit test for the ConfigureClusterTask class.
 * As business methods of this class don't return values, the assertions are made by verifying method calls on mocks.
 * Thus having strict mocks is essential!
 */
public class ConfigureClusterTaskTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigureClusterTaskTest.class);

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.STRICT)
  private ClusterConfigurationRequest clusterConfigurationRequest;

  @Mock(type = MockType.STRICT)
  private ClusterTopology clusterTopology;

  private TopologyManager.ConfigureClusterTask testSubject;


  @Before
  public void before() {
    reset(clusterConfigurationRequest, clusterTopology);
    testSubject = new TopologyManager.ConfigureClusterTask(clusterTopology, clusterConfigurationRequest);

  }

  @Test
  public void testShouldConfigureClusterTaskLogicBeExecutedWhenRequiredHostgroupsAreResolved() throws
      Exception {
    // GIVEN
    // is it OK to handle the non existence of hostgroups as a success?!
    expect(clusterConfigurationRequest.getRequiredHostGroups()).andReturn(Collections.EMPTY_LIST);
    expect(clusterTopology.getHostGroupInfo()).andReturn(Collections.EMPTY_MAP);

    // this is only called if the "prerequisites" are satisfied
    clusterConfigurationRequest.process();

    replay(clusterConfigurationRequest, clusterTopology);

    // WHEN
    Boolean result = testSubject.call();

    // THEN
    verify();
    Assert.assertTrue(result);
  }

  @Test
  public void testsShouldConfigureClusterTaskExecuteWhenCalledFromAsyncCallableService() throws Exception {
    // GIVEN
    // is it OK to handle the non existence of hostgroups as a success?!
    expect(clusterConfigurationRequest.getRequiredHostGroups()).andReturn(Collections.EMPTY_LIST);
    expect(clusterTopology.getHostGroupInfo()).andReturn(Collections.EMPTY_MAP);

    // this is only called if the "prerequisites" are satisfied
    clusterConfigurationRequest.process();

    replay(clusterConfigurationRequest, clusterTopology);

    AsyncCallableService<Boolean> asyncService = new AsyncCallableService<>(testSubject, 5000, 500, Executors
        .newScheduledThreadPool(3));

    // WHEN
    asyncService.call();
    // THEN


  }

  private Collection<String> mockRequiredHostGroups() {
    return Arrays.asList("test-hostgroup-1");
  }

  private Map<String, HostGroupInfo> mockHostGroupInfo() {
    Map<String, HostGroupInfo> hostGroupInfoMap = new HashMap<>();
    HostGroupInfo hostGroupInfo = new HostGroupInfo("test-hostgroup-1");
    hostGroupInfo.addHost("test-host-1");
    hostGroupInfo.setRequestedCount(2);

    hostGroupInfoMap.put("test-hostgroup-1", hostGroupInfo);
    return hostGroupInfoMap;
  }


}