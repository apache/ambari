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
package org.apache.ambari.server.stack;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.OperatingSystemEntity;
import org.apache.ambari.server.orm.entities.RepositoryEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.upgrade.RepositoryVersionHelper;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;

/**
 * Unit test for {@link UpdateActiveRepoVersionOnStartup}
 */
public class UpdateActiveRepoVersionOnStartupTest {

  private static String CLUSTER_NAME = "c1";
  private static String ADD_ON_REPO_ID = "MSFT_R-8.0";

  private RepositoryVersionDAO repositoryVersionDao;
  private RepositoryVersionEntity repoVersion;
  private UpdateActiveRepoVersionOnStartup activeRepoUpdater;

  @Test
  public void addAServiceRepoToExistingRepoVersion() throws Exception {
    init(true);
    activeRepoUpdater.process();
    verifyRepoIsAdded();
  }

  @Test
  public void missingClusterVersionShouldNotCauseException() throws Exception {
    init(false);
    activeRepoUpdater.process();
  }

  /**
   * Verifies if the add-on service repo is added to the repo version entity, both json and xml representations.
   *
   * @throws Exception
   */
  private void verifyRepoIsAdded() throws Exception {
    verify(repositoryVersionDao, times(1)).merge(repoVersion);

    boolean serviceRepoAddedToJson = false;
    outer:
    for (OperatingSystemEntity os: repoVersion.getOperatingSystems()) if (os.getOsType().equals("redhat6")) {
      for (RepositoryEntity repo: os.getRepositories()) if (repo.getRepositoryId().equals(ADD_ON_REPO_ID)) {
        serviceRepoAddedToJson = true;
        break outer;
      }
    }
    Assert.assertTrue(ADD_ON_REPO_ID + " is add-on repo was not added to JSON representation", serviceRepoAddedToJson);
  }

  public void init(boolean addClusterVersion) throws Exception {
    ClusterDAO clusterDao = mock(ClusterDAO.class);
    ClusterVersionDAO clusterVersionDAO = mock(ClusterVersionDAO.class);
    repositoryVersionDao = mock(RepositoryVersionDAO.class);
    final RepositoryVersionHelper repositoryVersionHelper = new RepositoryVersionHelper();
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);

    StackManager stackManager = mock(StackManager.class);
    when(metaInfo.getStackManager()).thenReturn(stackManager);

    ClusterEntity cluster = new ClusterEntity();
    cluster.setClusterName(CLUSTER_NAME);
    when(clusterDao.findAll()).thenReturn(ImmutableList.of(cluster));

    StackEntity stackEntity = new StackEntity();
    stackEntity.setStackName("HDP");
    stackEntity.setStackVersion("2.3");
    cluster.setDesiredStack(stackEntity);

    StackInfo stackInfo = new StackInfo();
    stackInfo.setName("HDP");
    stackInfo.setVersion("2.3");
    RepositoryInfo repositoryInfo = new RepositoryInfo();
    repositoryInfo.setBaseUrl("http://msft.r");
    repositoryInfo.setRepoId(ADD_ON_REPO_ID);
    repositoryInfo.setRepoName("MSFT_R");
    repositoryInfo.setOsType("redhat6");
    stackInfo.getRepositories().add(repositoryInfo);
    when(stackManager.getStack("HDP", "2.3")).thenReturn(stackInfo);

    Provider<RepositoryVersionHelper> repositoryVersionHelperProvider = mock(Provider.class);
    when(repositoryVersionHelperProvider.get()).thenReturn(repositoryVersionHelper);
    InMemoryDefaultTestModule testModule = new InMemoryDefaultTestModule() {
      @Override
      protected void configure() {
        bind(RepositoryVersionHelper.class).toInstance(repositoryVersionHelper);
        requestStaticInjection(RepositoryVersionEntity.class);
      }
    };
    Injector injector = Guice.createInjector(testModule);
    if (addClusterVersion) {
      repoVersion = new RepositoryVersionEntity();
      repoVersion.setStack(stackEntity);
      repoVersion.setOperatingSystems(resourceAsString("org/apache/ambari/server/stack/UpdateActiveRepoVersionOnStartupTest_initialRepos.json"));
      ClusterVersionEntity clusterVersion = new ClusterVersionEntity();
      clusterVersion.setRepositoryVersion(repoVersion);
      when(clusterVersionDAO.findByClusterAndStateCurrent(CLUSTER_NAME)).thenReturn(clusterVersion);

    }

    activeRepoUpdater = new UpdateActiveRepoVersionOnStartup(clusterDao,
        clusterVersionDAO, repositoryVersionDao, repositoryVersionHelper, metaInfo);
  }



  private static String resourceAsString(String resourceName) throws IOException {
    return Resources.toString(Resources.getResource(resourceName), Charsets.UTF_8);
  }

}
