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

package org.apache.ambari.server.orm.dao;

import java.sql.SQLException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.entities.MpackEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.StackId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * CrudDAO unit tests. Uses hosts table for in-memory DB tests.
 */
public class CrudDAOTest {

  private static Injector injector;
  private CrudDAO<MpackEntity, Long> mpackDAO;
  private int uniqueCounter = 0;
  private static final long FIRST_ID = 1L;

  private static final StackId HDP_206 = new StackId("HDP", "2.0.6");

  private StackDAO stackDAO;

  @Before
  public void before() {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    H2DatabaseCleaner.resetSequences(injector);
    injector.getInstance(GuiceJpaInitializer.class);

    stackDAO = injector.getInstance(StackDAO.class);
    mpackDAO = injector.getInstance(MpackDAO.class);

    // required to populate stacks into the database
    injector.getInstance(AmbariMetaInfo.class);
  }

  private void createSingleRecord() {
    StackEntity stackEntity = stackDAO.find(HDP_206.getStackName(),
        HDP_206.getStackVersion());

    Assert.assertNotNull(stackEntity);

    final MpackEntity entity = new MpackEntity();
    entity.setId(FIRST_ID + uniqueCounter);
    entity.setMpackName("FOO");
    entity.setMpackVersion("1.0.0.0-b1");
    entity.setMpackUri("http://ambari.apache.org");
    mpackDAO.create(entity);
    uniqueCounter++;
  }

  @Test
  public void testFindByPK() {
    Assert.assertNull(mpackDAO.findByPK(FIRST_ID));
    createSingleRecord();
    Assert.assertNotNull(mpackDAO.findByPK(FIRST_ID));
  }

  @Test
  public void testFindAll() {
    Assert.assertEquals(0, mpackDAO.findAll().size());
    createSingleRecord();
    createSingleRecord();
    Assert.assertEquals(2, mpackDAO.findAll().size());
    mpackDAO.remove(mpackDAO.findByPK(FIRST_ID));
    Assert.assertEquals(1, mpackDAO.findAll().size());
  }

  @Test
  public void testCreate() {
    createSingleRecord();
    Assert.assertTrue(mpackDAO.findAll().size() == 1);
    createSingleRecord();
    Assert.assertTrue(mpackDAO.findAll().size() == 2);
  }

  @Test
  public void testMerge() {
    createSingleRecord();
    MpackEntity entity = mpackDAO.findByPK(FIRST_ID);
    entity.setMpackName("BAR");
    mpackDAO.merge(entity);
    entity = mpackDAO.findByPK(FIRST_ID);
    Assert.assertEquals("BAR", entity.getMpackName());
  }

  @Test
  public void testRemove() {
    createSingleRecord();
    createSingleRecord();
    Assert.assertEquals(2, mpackDAO.findAll().size());
    mpackDAO.remove(mpackDAO.findByPK(FIRST_ID));
    Assert.assertEquals(1, mpackDAO.findAll().size());
    Assert.assertNull(mpackDAO.findByPK(1L));
  }

  @After
  public void after() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }
}
