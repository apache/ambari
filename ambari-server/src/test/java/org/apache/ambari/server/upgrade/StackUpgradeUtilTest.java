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
package org.apache.ambari.server.upgrade;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.entities.MetainfoEntity;
import org.apache.ambari.server.state.RepositoryInfo;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Tests the StackUpgradeHelper
 */
public class StackUpgradeUtilTest {

  private Injector injector;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
  }

  @After
  public void teardown() throws Exception {
    injector.getInstance(PersistService.class).stop();
  }

  private void reset(String stackName, String stackVersion) throws Exception {
    AmbariMetaInfo ami = injector.getInstance(AmbariMetaInfo.class);

    for (Entry<String, List<RepositoryInfo>> entry : ami.getRepository(stackName, stackVersion).entrySet()) {
      for (RepositoryInfo ri : entry.getValue()) {
        if (-1 == ri.getRepoId().indexOf("epel")) {
          ami.updateRepo(stackName, stackVersion,
              ri.getOsType(), ri.getRepoId(), "", null);
        }
      }
    }

  }

  @Test
  public void testUpgradeStack() throws Exception {
    StackUpgradeUtil stackUpgradeUtil = injector.getInstance(StackUpgradeUtil.class);

    String stackName = "HDP";
    String stackVersion = "1.3.0";
    String localRepoUrl = "http://foo.bar";

    // check updating all
    stackUpgradeUtil.updateLocalRepo(stackName, stackVersion, localRepoUrl, null, null);

    MetainfoDAO dao = injector.getInstance(MetainfoDAO.class);

    Collection<MetainfoEntity> entities = dao.findAll();
    Assert.assertTrue(entities.size() > 0);

    for (MetainfoEntity entity : entities) {
      Assert.assertTrue(entity.getMetainfoName().startsWith("repo:/HDP/1.3.0/"));
      Assert.assertEquals(localRepoUrl, entity.getMetainfoValue());
    }

    reset (stackName, stackVersion);
    entities = dao.findAll();
    Assert.assertEquals(0, entities.size());

    // check updating only centos6
    stackUpgradeUtil.updateLocalRepo(stackName, stackVersion, localRepoUrl, "centos6", null);

    entities = dao.findAll();
    for (MetainfoEntity entity : entities) {
      Assert.assertTrue(entity.getMetainfoName().startsWith("repo:/HDP/1.3.0/centos6") ||
          entity.getMetainfoName().startsWith("repo:/HDP/1.3.0/redhat6"));
      Assert.assertEquals(localRepoUrl, entity.getMetainfoValue());
    }

    reset (stackName, stackVersion);
    entities = dao.findAll();
    Assert.assertTrue(0 == entities.size());

    // check updating only centos6 and centos5
    stackUpgradeUtil.updateLocalRepo(stackName, stackVersion, localRepoUrl, "centos6,centos5", null);

    entities = dao.findAll();
    for (MetainfoEntity entity : entities) {
      Assert.assertTrue(entity.getMetainfoName().startsWith("repo:/HDP/1.3.0/centos6") ||
          entity.getMetainfoName().startsWith("repo:/HDP/1.3.0/redhat6") ||
          entity.getMetainfoName().startsWith("repo:/HDP/1.3.0/redhat5") ||
          entity.getMetainfoName().startsWith("repo:/HDP/1.3.0/centos5"));
      Assert.assertEquals(localRepoUrl, entity.getMetainfoValue());
    }

    // verify that a change to centos6 also changes redhat6
    localRepoUrl = "http://newfoo.bar";
    stackUpgradeUtil.updateLocalRepo(stackName, stackVersion, localRepoUrl, "centos6", null);
    entities = dao.findAll();
    boolean foundCentos6 = false;
    boolean foundRedhat6 = false;
    for (MetainfoEntity entity : entities) {
      if (-1 != entity.getMetainfoName().indexOf("centos6")) {
        foundCentos6 = true;
        Assert.assertEquals(localRepoUrl, entity.getMetainfoValue());
      } else if (-1 != entity.getMetainfoName().indexOf("redhat6")) {
        foundRedhat6 = true;
        Assert.assertEquals(localRepoUrl, entity.getMetainfoValue());
      } else {
        Assert.assertFalse(localRepoUrl.equals(entity.getMetainfoValue()));
      }
    }
    Assert.assertTrue(foundCentos6);
    Assert.assertTrue(foundRedhat6);

  }


}
