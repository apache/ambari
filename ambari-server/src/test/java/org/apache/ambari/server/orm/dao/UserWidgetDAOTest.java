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
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.UserWidgetEntity;
import org.apache.ambari.server.orm.entities.WidgetLayoutEntity;
import org.apache.ambari.server.orm.entities.WidgetLayoutUserWidgetEntity;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

import java.util.LinkedList;
import java.util.List;

/**
 * UserWidgetDAO unit tests.
 */
public class UserWidgetDAOTest {

  private static Injector injector;
  private UserWidgetDAO userWidgetDAO;
  OrmTestHelper helper;
  Long clusterId;


  @Before
  public void before() {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    userWidgetDAO = injector.getInstance(UserWidgetDAO.class);
    injector.getInstance(GuiceJpaInitializer.class);
    helper = injector.getInstance(OrmTestHelper.class);
    clusterId = helper.createCluster();
  }

  private void createRecords() {
    for (int i=0; i<3; i++) {
      UserWidgetEntity userWidgetEntity = new UserWidgetEntity();
      userWidgetEntity.setDisplayName("display name" + i);
      userWidgetEntity.setAuthor("author");
      userWidgetEntity.setClusterId(clusterId);
      userWidgetEntity.setMetrics("metrics");
      userWidgetEntity.setDescription("description");
      userWidgetEntity.setProperties("{\"warning_threshold\": 0.5,\"error_threshold\": 0.7 }");
      userWidgetEntity.setScope("CLUSTER");
      userWidgetEntity.setUserWidgetName("widget" + i);
      userWidgetEntity.setUserWidgetType("GAUGE");
      userWidgetEntity.setWidgetValues("${`jvmMemoryHeapUsed + jvmMemoryHeapMax`}");
      final WidgetLayoutEntity widgetLayoutEntity = new WidgetLayoutEntity();
      widgetLayoutEntity.setClusterId(clusterId);
      widgetLayoutEntity.setLayoutName("layout name" + i);
      widgetLayoutEntity.setSectionName("section" + i%2);
      final WidgetLayoutUserWidgetEntity widgetLayoutUserWidget = new WidgetLayoutUserWidgetEntity();
      widgetLayoutUserWidget.setUserWidget(userWidgetEntity);
      widgetLayoutUserWidget.setWidgetLayout(widgetLayoutEntity);
      widgetLayoutUserWidget.setWidgetOrder(0);

      List<WidgetLayoutUserWidgetEntity> widgetLayoutUserWidgetEntityList = new LinkedList<WidgetLayoutUserWidgetEntity>();
      widgetLayoutUserWidgetEntityList.add(widgetLayoutUserWidget);

      userWidgetEntity.setListWidgetLayoutUserWidgetEntity(widgetLayoutUserWidgetEntityList);
      userWidgetDAO.create(userWidgetEntity);
    }
  }

  @Test
  public void testFindByCluster() {
    createRecords();
    Assert.assertEquals(0, userWidgetDAO.findByCluster(99999).size());
    Assert.assertEquals(3, userWidgetDAO.findByCluster(clusterId).size());
  }

  @Test
  public void testFindBySectionName() {
    createRecords();
    Assert.assertEquals(0, userWidgetDAO.findBySectionName("non existing").size());
    Assert.assertEquals(2, userWidgetDAO.findBySectionName("section0").size());
    Assert.assertEquals(1, userWidgetDAO.findBySectionName("section1").size());
  }

  @Test
  public void testFindAll() {
    createRecords();
    Assert.assertEquals(3, userWidgetDAO.findAll().size());
  }

  @After
  public void after() {
    injector.getInstance(PersistService.class).stop();
    injector = null;
  }
}
