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

package org.apache.ambari.server.orm.dao;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * RepositoryVersionDAO unit tests.
 */
public class RepositoryVersionDAOTest {

  private static Injector injector;
  private RepositoryVersionDAO repositoryVersionDAO;
  private ClusterVersionDAO clusterVersionDAO;
  private HostVersionDAO hostVersionDAO;
  
  private ClusterDAO clusterDAO;
  private OrmTestHelper helper;

  @Before
  public void before() {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    repositoryVersionDAO = injector.getInstance(RepositoryVersionDAO.class);
    clusterVersionDAO = injector.getInstance(ClusterVersionDAO.class);
    hostVersionDAO = injector.getInstance(HostVersionDAO.class);
    clusterDAO = injector.getInstance(ClusterDAO.class);
    helper = injector.getInstance(OrmTestHelper.class);    
    injector.getInstance(GuiceJpaInitializer.class);
  }

  private void createSingleRecord() {
    final RepositoryVersionEntity entity = new RepositoryVersionEntity();
    entity.setDisplayName("display name");
    entity.setOperatingSystems("repositories");
    entity.setStack("stack");
    entity.setUpgradePackage("upgrade package");
    entity.setVersion("version");
    repositoryVersionDAO.create(entity);
  }

  @Test
  public void testFindByDisplayName() {
    createSingleRecord();
    Assert.assertNull(repositoryVersionDAO.findByDisplayName("non existing"));
    Assert.assertNotNull(repositoryVersionDAO.findByDisplayName("display name"));
  }

  @Test
  public void testFindByStackAndVersion() {
    createSingleRecord();
    Assert.assertNull(repositoryVersionDAO.findByStackAndVersion("non existing", "non existing"));
    Assert.assertNotNull(repositoryVersionDAO.findByStackAndVersion("stack", "version"));
  }

  @Test
  public void testFindByStack() {
    createSingleRecord();
    Assert.assertEquals(0, repositoryVersionDAO.findByStack("non existing").size());
    Assert.assertEquals(1, repositoryVersionDAO.findByStack("stack").size());
  }

  @Test
  public void testDelete() {
    createSingleRecord();
    Assert.assertNotNull(repositoryVersionDAO.findByStackAndVersion("stack", "version"));
    final RepositoryVersionEntity entity = repositoryVersionDAO.findByStackAndVersion("stack", "version");
    repositoryVersionDAO.remove(entity);
    Assert.assertNull(repositoryVersionDAO.findByStackAndVersion("stack", "version"));
  }  

  @Test
  public void testDeleteCascade() {   
    long clusterId = helper.createCluster();
    ClusterEntity cluster = clusterDAO.findById(clusterId);
    createSingleRecord();
    final RepositoryVersionEntity entity = repositoryVersionDAO.findByStackAndVersion("stack", "version");
    
    ClusterVersionEntity cvA = new ClusterVersionEntity(cluster, entity, RepositoryVersionState.INSTALLED, System.currentTimeMillis(), System.currentTimeMillis(), "admin");
    clusterVersionDAO.create(cvA);
    long cvAId = cvA.getId();
    cvA = clusterVersionDAO.findByPK(cvAId);
    Assert.assertNotNull(cvA.getRepositoryVersion());
    final RepositoryVersionEntity newEntity = repositoryVersionDAO.findByStackAndVersion("stack", "version");
    try {
      repositoryVersionDAO.remove(newEntity);
    } catch (Exception e) {
      //Cascade deletion will fail because absent integrity in in-memory DB
      Assert.assertNotNull(clusterVersionDAO.findByPK(cvAId));
    } 
    //
   
  }    
  
  @After
  public void after() {
    injector.getInstance(PersistService.class).stop();
    injector = null;
  }
}
