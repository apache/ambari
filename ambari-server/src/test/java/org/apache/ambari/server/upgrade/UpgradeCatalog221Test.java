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

package org.apache.ambari.server.upgrade;


import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.persist.PersistService;
import junit.framework.Assert;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.lang.reflect.Method;

import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

public class UpgradeCatalog221Test {
  private Injector injector;
  private Provider<EntityManager> entityManagerProvider = createStrictMock(Provider.class);
  private EntityManager entityManager = createNiceMock(EntityManager.class);
  private UpgradeCatalogHelper upgradeCatalogHelper;
  private StackEntity desiredStackEntity;



  @Before
  public void init() {
    reset(entityManagerProvider);
    expect(entityManagerProvider.get()).andReturn(entityManager).anyTimes();
    replay(entityManagerProvider);
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    upgradeCatalogHelper = injector.getInstance(UpgradeCatalogHelper.class);
    // inject AmbariMetaInfo to ensure that stacks get populated in the DB
    injector.getInstance(AmbariMetaInfo.class);
    // load the stack entity
    StackDAO stackDAO = injector.getInstance(StackDAO.class);
    desiredStackEntity = stackDAO.find("HDP", "2.2.0");
  }

  @After
  public void tearDown() {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    Method addNewConfigurationsFromXml = AbstractUpgradeCatalog.class.getDeclaredMethod("addNewConfigurationsFromXml");
    Method updateAlerts = UpgradeCatalog221.class.getDeclaredMethod("updateAlerts");



    UpgradeCatalog221 upgradeCatalog221 = createMockBuilder(UpgradeCatalog221.class)
            .addMockedMethod(addNewConfigurationsFromXml)
            .addMockedMethod(updateAlerts)
            .createMock();

    upgradeCatalog221.addNewConfigurationsFromXml();
    expectLastCall().once();
    upgradeCatalog221.updateAlerts();
    expectLastCall().once();


    replay(upgradeCatalog221);

    upgradeCatalog221.executeDMLUpdates();

    verify(upgradeCatalog221);
  }

  @Test
  public void test_AddCheckCommandTimeoutParam_ParamsNotAvailable() {

    UpgradeCatalog221 upgradeCatalog221 = new UpgradeCatalog221(injector);
    String inputSource = "{ \"path\" : \"test_path\", \"type\" : \"SCRIPT\"}";
    String expectedSource = "{\"path\":\"test_path\",\"type\":\"SCRIPT\",\"parameters\":[{\"name\":\"check.command.timeout\",\"display_name\":\"Check command timeout\",\"value\":60.0,\"type\":\"NUMERIC\",\"description\":\"The maximum time before check command will be killed by timeout\",\"units\":\"seconds\"}]}";

    String result = upgradeCatalog221.addCheckCommandTimeoutParam(inputSource);
    Assert.assertEquals(result, expectedSource);
  }

  @Test
  public void test_AddCheckCommandTimeoutParam_ParamsAvailable() {

    UpgradeCatalog221 upgradeCatalog221 = new UpgradeCatalog221(injector);
    String inputSource = "{\"path\":\"test_path\",\"type\":\"SCRIPT\",\"parameters\":[{\"name\":\"test\",\"display_name\":\"Test\",\"value\":10.0,\"type\":\"test\",\"description\":\"test\",\"units\":\"test\"}]}";
    String expectedSource = "{\"path\":\"test_path\",\"type\":\"SCRIPT\",\"parameters\":[{\"name\":\"test\",\"display_name\":\"Test\",\"value\":10.0,\"type\":\"test\",\"description\":\"test\",\"units\":\"test\"},{\"name\":\"check.command.timeout\",\"display_name\":\"Check command timeout\",\"value\":60.0,\"type\":\"NUMERIC\",\"description\":\"The maximum time before check command will be killed by timeout\",\"units\":\"seconds\"}]}";

    String result = upgradeCatalog221.addCheckCommandTimeoutParam(inputSource);
    Assert.assertEquals(result, expectedSource);
  }

  @Test
  public void test_AddCheckCommandTimeoutParam_NeededParamAlreadyAdded() {

    UpgradeCatalog221 upgradeCatalog221 = new UpgradeCatalog221(injector);
    String inputSource = "{\"path\":\"test_path\",\"type\":\"SCRIPT\",\"parameters\":[{\"display_name\":\"Test\",\"value\":10.0,\"type\":\"test\",\"description\":\"test\",\"units\":\"test\"},{\"name\":\"check.command.timeout\",\"display_name\":\"Check command timeout\",\"value\":60.0,\"type\":\"NUMERIC\",\"description\":\"The maximum time before check command will be killed by timeout\",\"units\":\"seconds\"}]}";
    String expectedSource = "{\"path\":\"test_path\",\"type\":\"SCRIPT\",\"parameters\":[{\"display_name\":\"Test\",\"value\":10.0,\"type\":\"test\",\"description\":\"test\",\"units\":\"test\"},{\"name\":\"check.command.timeout\",\"display_name\":\"Check command timeout\",\"value\":60.0,\"type\":\"NUMERIC\",\"description\":\"The maximum time before check command will be killed by timeout\",\"units\":\"seconds\"}]}";

    String result = upgradeCatalog221.addCheckCommandTimeoutParam(inputSource);
    Assert.assertEquals(result, expectedSource);
  }

}
