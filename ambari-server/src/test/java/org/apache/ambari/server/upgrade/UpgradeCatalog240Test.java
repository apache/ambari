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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

public class UpgradeCatalog240Test {
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
    Method updateAlerts = UpgradeCatalog240.class.getDeclaredMethod("updateAlerts");



    UpgradeCatalog240 upgradeCatalog240 = createMockBuilder(UpgradeCatalog240.class)
            .addMockedMethod(addNewConfigurationsFromXml)
            .addMockedMethod(updateAlerts)
            .createMock();

    upgradeCatalog240.addNewConfigurationsFromXml();
    expectLastCall().once();
    upgradeCatalog240.updateAlerts();
    expectLastCall().once();


    replay(upgradeCatalog240);

    upgradeCatalog240.executeDMLUpdates();

    verify(upgradeCatalog240);
  }

  @Test
  public void test_addParam_ParamsNotAvailable() {

    UpgradeCatalog240 upgradeCatalog240 = new UpgradeCatalog240(injector);
    String inputSource = "{ \"path\" : \"test_path\", \"type\" : \"SCRIPT\"}";
    List<String> params = Arrays.asList("connection.timeout", "checkpoint.time.warning.threshold", "checkpoint.time.critical.threshold");
    String expectedSource = "{\"path\":\"test_path\",\"type\":\"SCRIPT\",\"parameters\":[{\"name\":\"connection.timeout\",\"display_name\":\"Connection Timeout\",\"value\":5.0,\"type\":\"NUMERIC\",\"description\":\"The maximum time before this alert is considered to be CRITICAL\",\"units\":\"seconds\",\"threshold\":\"CRITICAL\"},{\"name\":\"checkpoint.time.warning.threshold\",\"display_name\":\"Checkpoint Warning\",\"value\":2.0,\"type\":\"PERCENT\",\"description\":\"The percentage of the last checkpoint time greater than the interval in order to trigger a warning alert.\",\"units\":\"%\",\"threshold\":\"WARNING\"},{\"name\":\"checkpoint.time.critical.threshold\",\"display_name\":\"Checkpoint Critical\",\"value\":2.0,\"type\":\"PERCENT\",\"description\":\"The percentage of the last checkpoint time greater than the interval in order to trigger a critical alert.\",\"units\":\"%\",\"threshold\":\"CRITICAL\"}]}";

    String result = upgradeCatalog240.addParam(inputSource, params);
    Assert.assertEquals(result, expectedSource);
  }

  @Test
  public void test_addParam_ParamsAvailableWithOneOFNeededItem() {

    UpgradeCatalog240 upgradeCatalog240 = new UpgradeCatalog240(injector);
    String inputSource = "{\"path\":\"test_path\",\"type\":\"SCRIPT\",\"parameters\":[{\"name\":\"connection.timeout\",\"display_name\":\"Connection Timeout\",\"value\":5.0,\"type\":\"NUMERIC\",\"description\":\"The maximum time before this alert is considered to be CRITICAL\",\"units\":\"seconds\",\"threshold\":\"CRITICAL\"}]}";
    List<String> params = new ArrayList<String>(Arrays.asList("connection.timeout", "checkpoint.time.warning.threshold", "checkpoint.time.critical.threshold"));
    String expectedSource = "{\"path\":\"test_path\",\"type\":\"SCRIPT\",\"parameters\":[{\"name\":\"connection.timeout\",\"display_name\":\"Connection Timeout\",\"value\":5.0,\"type\":\"NUMERIC\",\"description\":\"The maximum time before this alert is considered to be CRITICAL\",\"units\":\"seconds\",\"threshold\":\"CRITICAL\"},{\"name\":\"checkpoint.time.warning.threshold\",\"display_name\":\"Checkpoint Warning\",\"value\":2.0,\"type\":\"PERCENT\",\"description\":\"The percentage of the last checkpoint time greater than the interval in order to trigger a warning alert.\",\"units\":\"%\",\"threshold\":\"WARNING\"},{\"name\":\"checkpoint.time.critical.threshold\",\"display_name\":\"Checkpoint Critical\",\"value\":2.0,\"type\":\"PERCENT\",\"description\":\"The percentage of the last checkpoint time greater than the interval in order to trigger a critical alert.\",\"units\":\"%\",\"threshold\":\"CRITICAL\"}]}";

    String result = upgradeCatalog240.addParam(inputSource, params);
    Assert.assertEquals(result, expectedSource);
  }

}
