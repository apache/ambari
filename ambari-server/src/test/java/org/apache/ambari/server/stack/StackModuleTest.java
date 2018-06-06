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

package org.apache.ambari.server.stack;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.ambari.server.state.stack.ServiceMetainfoXml;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;


/**
 * Tests for StackModule
 */
public class StackModuleTest {

  @Test
  public void removedServicesInitialValue () throws Exception {
    StackModule sm = createStackModule("FooBar",
        "2.4",
        Optional.absent(),
        Lists.newArrayList(repoInfo("bar", "2.0.1", "http://bar.org", "centos6")),
        Lists.newArrayList(repoInfo("bar", "2.0.1", "http://bar.org", "centos7")));
    List<String> removedServices = sm.getModuleInfo().getRemovedServices();
    assertEquals(removedServices.size(), 0);
  }

  @Test
  public void servicesWithNoConfigsInitialValue() throws Exception {
    StackModule sm = createStackModule("FooBar",
        "2.4",
        Optional.absent(),
        Lists.newArrayList(repoInfo("bar", "2.0.1", "http://bar.org", "centos6")),
        Lists.newArrayList(repoInfo("bar", "2.0.1", "http://bar.org", "centos7")));
    List<String> servicesWithNoConfigs = sm.getModuleInfo().getServicesWithNoConfigs();
    assertEquals(servicesWithNoConfigs.size(), 0);
  }

  @SafeVarargs
  private static StackModule createStackModule(String stackName, String stackVersion, Optional<? extends List<RepositoryInfo>> stackRepos,
                                        List<RepositoryInfo>... serviceRepoLists) throws AmbariException {
    StackDirectory sd = mock(StackDirectory.class);
    List<ServiceDirectory> serviceDirectories = Lists.newArrayList();
    for (List<RepositoryInfo> serviceRepoList: serviceRepoLists) {
      StackServiceDirectory svd = mock(StackServiceDirectory.class);
      RepositoryXml serviceRepoXml = mock(RepositoryXml.class);
      when(svd.getRepoFile()).thenReturn(serviceRepoXml);
      when(serviceRepoXml.getRepositories()).thenReturn(serviceRepoList);
      ServiceMetainfoXml serviceMetainfoXml = mock(ServiceMetainfoXml.class);
      when(serviceMetainfoXml.isValid()).thenReturn(true);
      ServiceInfo serviceInfo = mock(ServiceInfo.class);
      when(serviceInfo.isValid()).thenReturn(true);
      when(serviceInfo.getName()).thenReturn(UUID.randomUUID().toString()); // unique service names
      when(serviceMetainfoXml.getServices()).thenReturn(Lists.newArrayList(serviceInfo));
      when(svd.getMetaInfoFile()).thenReturn(serviceMetainfoXml);
      serviceDirectories.add(svd);
    }
    if (stackRepos.isPresent()) {
      RepositoryXml stackRepoXml = mock(RepositoryXml.class);
      when(sd.getRepoFile()).thenReturn(stackRepoXml);
      when(stackRepoXml.getRepositories()).thenReturn(stackRepos.get());
    }
    when(sd.getServiceDirectories()).thenReturn(serviceDirectories);
    when(sd.getStackDirName()).thenReturn(stackName);
    when(sd.getDirectory()).thenReturn(new File(stackVersion));
    StackContext ctx = mock(StackContext.class);
    StackModule sm = new StackModule(sd, ctx);
    sm.resolve(null,
        ImmutableMap.of(String.format("%s:%s", stackName, stackVersion), sm),
        ImmutableMap.of(), ImmutableMap.of());
    return sm;
  }

  private RepositoryInfo repoInfo(String repoName, String repoVersion, String url) {
    return repoInfo(repoName, repoVersion, url, "centos6");
  }

  private List<RepositoryInfo> repoInfosForAllOs(String repoName, String repoVersion, String url) {
    List<RepositoryInfo> repos = new ArrayList<>(3);
    for (String os: new String[]{ "centos5", "centos6", "centos7"}) {
      repos.add(repoInfo(repoName, repoVersion, url, os));
    }
    return repos;
  }


  private RepositoryInfo repoInfo(String repoName, String repoVersion, String url, String osType) {
    RepositoryInfo info = new RepositoryInfo();
    info.setRepoId(String.format("%s:%s", repoName, repoVersion));
    info.setRepoName(repoName);
    info.setBaseUrl(url);
    info.setOsType(osType);
    return info;
  }

  private Set<String> getIds(List<RepositoryInfo> repoInfos) {
    return ImmutableSet.copyOf(Lists.transform(repoInfos, RepositoryInfo.GET_REPO_ID_FUNCTION));
  }

  private Multiset<String> getIdsMultiple(List<RepositoryInfo> repoInfos) {
    return ImmutableMultiset.copyOf(Lists.transform(repoInfos, RepositoryInfo.GET_REPO_ID_FUNCTION));
  }


}
