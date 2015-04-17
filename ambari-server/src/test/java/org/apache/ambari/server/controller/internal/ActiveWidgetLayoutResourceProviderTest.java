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
package org.apache.ambari.server.controller.internal;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.dao.WidgetLayoutDAO;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.orm.entities.WidgetLayoutEntity;
import org.apache.ambari.server.orm.entities.WidgetLayoutUserWidgetEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

/**
 * ActiveWidgetLayout tests
 */
public class ActiveWidgetLayoutResourceProviderTest {

  private WidgetLayoutDAO widgetLayoutDAO = null;
  private UserDAO userDAO = null;
  private Injector m_injector;

  @Before
  public void before() {
    widgetLayoutDAO = createStrictMock(WidgetLayoutDAO.class);
    userDAO = createStrictMock(UserDAO.class);

    m_injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetSingleResource() throws Exception {
    Request request = PropertyHelper.getReadRequest(
        ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_CLUSTER_NAME_PROPERTY_ID,
        ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_ID_PROPERTY_ID,
        ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_DISPLAY_NAME_PROPERTY_ID,
        ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_LAYOUT_NAME_PROPERTY_ID,
        ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_SECTION_NAME_PROPERTY_ID,
        ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_USERNAME_PROPERTY_ID,
        ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_SCOPE_PROPERTY_ID,
        ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_WIDGETS_PROPERTY_ID);

    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    UserEntity userEntity = createMock(UserEntity.class);
    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getClusterById(1L)).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterName()).andReturn("c1").anyTimes();

    Predicate predicate = new PredicateBuilder().property(
            ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_USERNAME_PROPERTY_ID).equals("username").toPredicate();


    expect(userDAO.findUserByName("username")).andReturn(userEntity);
    expect(userEntity.getActiveWidgetLayouts()).andReturn("[{\"id\":\"1\"},{\"id\":\"2\"}]");
    expect(widgetLayoutDAO.findById(1L)).andReturn(getMockEntities().get(0));
    expect(widgetLayoutDAO.findById(2L)).andReturn(getMockEntities().get(1));

    replay(amc, clusters, cluster, widgetLayoutDAO, userEntity, userDAO);

    ActiveWidgetLayoutResourceProvider provider = createProvider(amc);
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(2, results.size());

    Resource r = results.iterator().next();
    Assert.assertEquals("section0", r.getPropertyValue(ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_SECTION_NAME_PROPERTY_ID));
    Assert.assertEquals("CLUSTER", r.getPropertyValue(ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_SCOPE_PROPERTY_ID));
    Assert.assertEquals("username", r.getPropertyValue(ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_USERNAME_PROPERTY_ID));
    Assert.assertEquals("displ_name", r.getPropertyValue(ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_DISPLAY_NAME_PROPERTY_ID));
    Assert.assertEquals("layout name0", r.getPropertyValue(ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_LAYOUT_NAME_PROPERTY_ID));

    Assert.assertEquals("[]", r.getPropertyValue(ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_WIDGETS_PROPERTY_ID).toString());

    verify(amc, clusters, cluster, widgetLayoutDAO, userEntity, userDAO);
  }


  /**
   * @throws Exception
   */
  @Test
  public void testCreateResources() throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);

    replay(amc);

    ActiveWidgetLayoutResourceProvider provider = createProvider(amc);

    Map<String, Object> requestProps = new HashMap<String, Object>();

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    try {
      provider.createResources(request);
    } catch (Exception e) {
      //Expected exception
    }

  }

  /**
   * @throws Exception
   */
  @Test
  public void testUpdateResources() throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);

    expect(widgetLayoutDAO.findById(anyLong())).andReturn(getMockEntities().get(0)).anyTimes();
    UserEntity userEntity = new UserEntity();
    expect(userDAO.findUserByName("username")).andReturn(userEntity);
    expect(userDAO.merge((UserEntity) anyObject())).andReturn(userEntity).anyTimes();

    replay(amc, widgetLayoutDAO, userDAO);

    Predicate predicate = new PredicateBuilder().property(
            ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_USERNAME_PROPERTY_ID).equals("username").toPredicate();
    Set<Map<String, String>> widgetLayouts = new HashSet<Map<String, String>>();
    HashMap<String, String> layout = new HashMap<String, String>();
    layout.put("id","1");
    widgetLayouts.add(layout);
    layout.put("id","2");
    widgetLayouts.add(layout);
    HashMap<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT, widgetLayouts);
    requestProps.put(ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_USERNAME_PROPERTY_ID, "username");

    Request request = PropertyHelper.getUpdateRequest(requestProps, null);

    ActiveWidgetLayoutResourceProvider provider = createProvider(amc);
    provider.updateResources(request, predicate);

    Assert.assertTrue(userEntity.getActiveWidgetLayouts().equals("[{\"id\":\"2\"},{\"id\":\"2\"}]"));
    verify(amc, widgetLayoutDAO, userDAO);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testDeleteResources() throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);

    replay(amc);

    ActiveWidgetLayoutResourceProvider provider = createProvider(amc);

    Map<String, Object> requestProps = new HashMap<String, Object>();
    Predicate predicate = new PredicateBuilder().property(
            WidgetLayoutResourceProvider.WIDGETLAYOUT_USERNAME_PROPERTY_ID).equals("username").toPredicate();
    try {
      provider.deleteResources(predicate);
    } catch (Exception e) {
      //Expected exception
    }
  }

  /**
   * @param amc
   * @return
   */
  private ActiveWidgetLayoutResourceProvider createProvider(AmbariManagementController amc) {
    return new ActiveWidgetLayoutResourceProvider(amc);
  }

  /**
   * @return
   */
  private List<WidgetLayoutEntity> getMockEntities() throws Exception {
    List<WidgetLayoutEntity> widgetLayoutEntities = new ArrayList<WidgetLayoutEntity>();
    for (int i=1; i<3; i++) {
      WidgetLayoutEntity widgetLayoutEntity = new WidgetLayoutEntity();
      widgetLayoutEntity.setId((long) i);
      widgetLayoutEntity.setClusterId(Long.valueOf(1L));
      widgetLayoutEntity.setLayoutName("layout name0");
      widgetLayoutEntity.setSectionName("section0");
      widgetLayoutEntity.setUserName("username");
      widgetLayoutEntity.setScope("CLUSTER");
      widgetLayoutEntity.setDisplayName("displ_name");
      List<WidgetLayoutUserWidgetEntity> layoutUserWidgetEntityList = new LinkedList<WidgetLayoutUserWidgetEntity>();
      widgetLayoutEntity.setListWidgetLayoutUserWidgetEntity(layoutUserWidgetEntityList);

      widgetLayoutEntities.add(widgetLayoutEntity);
    }
    return widgetLayoutEntities;
  }

  /**
  *
  */
  private class MockModule implements Module {
    /**
    *
    */
    @Override
    public void configure(Binder binder) {
      binder.bind(WidgetLayoutDAO.class).toInstance(widgetLayoutDAO);
      binder.bind(UserDAO.class).toInstance(userDAO);
      binder.bind(Clusters.class).toInstance(
          EasyMock.createNiceMock(Clusters.class));
      binder.bind(Cluster.class).toInstance(
          EasyMock.createNiceMock(Cluster.class));
      binder.bind(ActionMetadata.class);
    }
  }
}
