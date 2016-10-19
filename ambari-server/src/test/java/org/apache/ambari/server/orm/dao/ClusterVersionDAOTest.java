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

import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.StackId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * ClusterVersionDAO unit tests.
 */
public class ClusterVersionDAOTest {

  private static Injector injector;
  private ClusterVersionDAO clusterVersionDAO;
  private ClusterDAO clusterDAO;
  private OrmTestHelper helper;

  private long clusterId;
  ClusterEntity cluster;
  private int lastStep = -1;

  ClusterVersionEntity cvA;
  long cvAId = 0L;

  ClusterVersionEntity cvB;
  long cvBId = 0L;

  ClusterVersionEntity cvC;
  long cvCId = 0L;

  private final static StackId HDP_22_STACK = new StackId("HDP", "2.2.0");
  private final static StackId BAD_STACK = new StackId("BADSTACK", "1.0");

  @Before
  public void before() {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    clusterVersionDAO = injector.getInstance(ClusterVersionDAO.class);
    clusterDAO = injector.getInstance(ClusterDAO.class);
    helper = injector.getInstance(OrmTestHelper.class);
    injector.getInstance(GuiceJpaInitializer.class);
  }

  /**
   * Helper function to transition the cluster through several cluster versions.
   * @param currStep Step to go to is a value from 1 - 7, inclusive.
   */
  private void createRecordsUntilStep(int currStep) throws Exception {
    // Fresh install on A
    if (currStep >= 1 && lastStep <= 0) {
      clusterId = helper.createCluster();
      cluster = clusterDAO.findById(clusterId);

      cvA = new ClusterVersionEntity(cluster, helper.getOrCreateRepositoryVersion(HDP_22_STACK, "2.2.0.0-995"), RepositoryVersionState.CURRENT, System.currentTimeMillis(), System.currentTimeMillis(), "admin");
      clusterVersionDAO.create(cvA);
      cvAId = cvA.getId();
    } else {
      cluster = clusterDAO.findById(clusterId);
      cvA = clusterVersionDAO.findByPK(cvAId);
    }

    // Install B
    if (currStep >= 2) {
      if (lastStep <= 1) {
        cvB = new ClusterVersionEntity(cluster, helper.getOrCreateRepositoryVersion(HDP_22_STACK, "2.2.0.1-998"), RepositoryVersionState.INSTALLED, System.currentTimeMillis(), System.currentTimeMillis(), "admin");
        clusterVersionDAO.create(cvB);
        cvBId = cvB.getId();
      } else {
        cvB = clusterVersionDAO.findByPK(cvBId);
      }
    }

    // Switch from A to B
    if (currStep >= 3 && lastStep <= 2) {
      cvA.setState(RepositoryVersionState.INSTALLED);
      cvB.setState(RepositoryVersionState.CURRENT);
      clusterVersionDAO.merge(cvA);
      clusterVersionDAO.merge(cvB);
    }

    // Start upgrading C
    if (currStep >= 4) {
      if (lastStep <= 3) {
        cvC = new ClusterVersionEntity(cluster, helper.getOrCreateRepositoryVersion(HDP_22_STACK, "2.2.0.0-100"), RepositoryVersionState.INSTALLING, System.currentTimeMillis(), "admin");
        clusterVersionDAO.create(cvC);
        cvCId = cvC.getId();
      } else {
        cvC = clusterVersionDAO.findByPK(cvCId);
      }
    }

    // Fail upgrade for C
    if (currStep >= 5 && lastStep <= 4) {
        cvC.setState(RepositoryVersionState.INSTALL_FAILED);
        cvC.setEndTime(System.currentTimeMillis());
        clusterVersionDAO.merge(cvC);
    }

    // Retry upgrade on C
    if (currStep >= 6 && lastStep <= 5) {
        cvC.setState(RepositoryVersionState.INSTALLING);
        cvC.setEndTime(0L);
        clusterVersionDAO.merge(cvC);
    }

    // Finalize upgrade on C to make it the current cluster version
    if (currStep >= 7 && lastStep <= 6) {
        cvC.setState(RepositoryVersionState.CURRENT);
        cvC.setEndTime(System.currentTimeMillis());
        clusterVersionDAO.merge(cvC);

        cvA.setState(RepositoryVersionState.INSTALLED);
        cvB.setState(RepositoryVersionState.INSTALLED);
        clusterVersionDAO.merge(cvA);
        clusterVersionDAO.merge(cvB);
    }

    lastStep = currStep;
  }

  @Test
  public void testFindByStackAndVersion() throws Exception {
    createRecordsUntilStep(1);
    Assert.assertEquals(
        0,
        clusterVersionDAO.findByStackAndVersion("non existing", "non existing",
            "non existing").size());

    Assert.assertEquals(
        1,
        clusterVersionDAO.findByStackAndVersion(HDP_22_STACK.getStackName(),
            HDP_22_STACK.getStackVersion(), "2.2.0.0-995").size());
  }

  @Test
  public void testFindByCluster() throws Exception {
    createRecordsUntilStep(1);
    Assert.assertEquals(0, clusterVersionDAO.findByCluster("non existing").size());
    Assert.assertEquals(1, clusterVersionDAO.findByCluster(cluster.getClusterName()).size());
  }

  @Test
  public void testFindByClusterAndStackAndVersion() throws Exception {
    createRecordsUntilStep(1);
    Assert.assertNull(clusterVersionDAO.findByClusterAndStackAndVersion(
        cluster.getClusterName(), BAD_STACK, "non existing"));

    Assert.assertNotNull(clusterVersionDAO.findByClusterAndStackAndVersion(
        cluster.getClusterName(), HDP_22_STACK, "2.2.0.0-995"));
  }

  /**
   * At all times the cluster should have a cluster version whose state is {@link org.apache.ambari.server.state.RepositoryVersionState#CURRENT}
   */
  @Test
  public void testFindByClusterAndStateCurrent() throws Exception {
    createRecordsUntilStep(1);
    Assert.assertNotNull(clusterVersionDAO.findByClusterAndStateCurrent(cluster.getClusterName()));

    createRecordsUntilStep(2);
    Assert.assertNotNull(clusterVersionDAO.findByClusterAndStateCurrent(cluster.getClusterName()));

    createRecordsUntilStep(3);
    Assert.assertNotNull(clusterVersionDAO.findByClusterAndStateCurrent(cluster.getClusterName()));

    createRecordsUntilStep(4);
    Assert.assertNotNull(clusterVersionDAO.findByClusterAndStateCurrent(cluster.getClusterName()));

    createRecordsUntilStep(5);
    Assert.assertNotNull(clusterVersionDAO.findByClusterAndStateCurrent(cluster.getClusterName()));

    createRecordsUntilStep(6);
    Assert.assertNotNull(clusterVersionDAO.findByClusterAndStateCurrent(cluster.getClusterName()));

    createRecordsUntilStep(7);
    Assert.assertNotNull(clusterVersionDAO.findByClusterAndStateCurrent(cluster.getClusterName()));
  }

  /**
   * Test the state of certain cluster versions.
   */
  @Test
  public void testFindByClusterAndState() throws Exception {
    createRecordsUntilStep(1);
    Assert.assertEquals(1, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.CURRENT).size());
    Assert.assertEquals(0, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.INSTALLED).size());
    Assert.assertEquals(0, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.INSTALLING).size());
    Assert.assertEquals(0, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.INSTALL_FAILED).size());

    createRecordsUntilStep(2);
    Assert.assertEquals(1, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.CURRENT).size());
    Assert.assertEquals(1, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.INSTALLED).size());
    Assert.assertEquals(0, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.INSTALLING).size());
    Assert.assertEquals(0, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.INSTALL_FAILED).size());

    createRecordsUntilStep(3);
    Assert.assertEquals(1, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.CURRENT).size());
    Assert.assertEquals(1, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.INSTALLED).size());
    Assert.assertEquals(0, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.INSTALLING).size());
    Assert.assertEquals(0, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.INSTALL_FAILED).size());

    createRecordsUntilStep(4);
    Assert.assertEquals(1, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.CURRENT).size());
    Assert.assertEquals(1, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.INSTALLED).size());
    Assert.assertEquals(1, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.INSTALLING).size());
    Assert.assertEquals(0, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.INSTALL_FAILED).size());

    createRecordsUntilStep(5);
    Assert.assertEquals(1, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.CURRENT).size());
    Assert.assertEquals(1, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.INSTALLED).size());
    Assert.assertEquals(0, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.INSTALLING).size());
    Assert.assertEquals(1, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.INSTALL_FAILED).size());

    createRecordsUntilStep(6);
    Assert.assertEquals(1, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.CURRENT).size());
    Assert.assertEquals(1, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.INSTALLED).size());
    Assert.assertEquals(1, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.INSTALLING).size());
    Assert.assertEquals(0, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.INSTALL_FAILED).size());

    createRecordsUntilStep(7);
    Assert.assertEquals(1, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.CURRENT).size());
    Assert.assertEquals(2, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.INSTALLED).size());
    Assert.assertEquals(0, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.INSTALLING).size());
    Assert.assertEquals(0, clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.INSTALL_FAILED).size());
  }

  @After
  public void after() {
    injector.getInstance(PersistService.class).stop();
    injector = null;
  }
}
