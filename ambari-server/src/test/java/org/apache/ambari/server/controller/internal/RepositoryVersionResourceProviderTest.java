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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.apache.ambari.server.controller.ResourceProviderFactory;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * RepositoryVersionResourceProvider tests.
 */
public class RepositoryVersionResourceProviderTest {

  private static Injector injector;

  @Before
  public void before() {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
  }

  @Test
  public void testCreateResources() throws Exception {
    final ResourceProvider provider = injector.getInstance(ResourceProviderFactory.class).getRepositoryVersionProvider();

    final Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();
    final Map<String, Object> properties = new LinkedHashMap<String, Object>();
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID, "name");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_REPOSITORIES_PROPERTY_ID, "repositories");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_PROPERTY_ID, "stack");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID, "upgrade");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_VERSION_PROPERTY_ID, "version");
    propertySet.add(properties);

    final Request getRequest = PropertyHelper.getReadRequest(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID);
    Assert.assertEquals(0, provider.getResources(getRequest, null).size());

    final Request createRequest = PropertyHelper.getCreateRequest(propertySet, null);
    provider.createResources(createRequest);

    Assert.assertEquals(1, provider.getResources(getRequest, null).size());
  }

  @Test
  public void testGetResources() throws Exception {
    final ResourceProvider provider = injector.getInstance(ResourceProviderFactory.class).getRepositoryVersionProvider();
    final RepositoryVersionDAO repositoryVersionDAO = injector.getInstance(RepositoryVersionDAO.class);
    final RepositoryVersionEntity entity = new RepositoryVersionEntity();
    entity.setDisplayName("name");
    entity.setRepositories("repositories");
    entity.setStack("stack");
    entity.setUpgradePackage("upgrade");
    entity.setVersion("version");

    final Request getRequest = PropertyHelper.getReadRequest(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID);
    Assert.assertEquals(0, provider.getResources(getRequest, null).size());

    repositoryVersionDAO.create(entity);

    Assert.assertEquals(1, provider.getResources(getRequest, null).size());
  }

  @Test
  public void testDeleteResources() throws Exception {
    final ResourceProvider provider = injector.getInstance(ResourceProviderFactory.class).getRepositoryVersionProvider();

    final Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();
    final Map<String, Object> properties = new LinkedHashMap<String, Object>();
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID, "name");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_REPOSITORIES_PROPERTY_ID, "repositories");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_PROPERTY_ID, "stack");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID, "upgrade");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_VERSION_PROPERTY_ID, "version");
    propertySet.add(properties);

    final Request getRequest = PropertyHelper.getReadRequest(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID);
    Assert.assertEquals(0, provider.getResources(getRequest, null).size());

    final Request createRequest = PropertyHelper.getCreateRequest(propertySet, null);
    provider.createResources(createRequest);

    Assert.assertEquals(1, provider.getResources(getRequest, null).size());

    final Predicate predicate = new PredicateBuilder().property(RepositoryVersionResourceProvider.REPOSITORY_VERSION_ID_PROPERTY_ID).equals("1").toPredicate();
    provider.deleteResources(predicate);

    Assert.assertEquals(0, provider.getResources(getRequest, null).size());
  }

  @Test
  public void testUpdateResources() throws Exception {
    final ResourceProvider provider = injector.getInstance(ResourceProviderFactory.class).getRepositoryVersionProvider();

    final Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();
    final Map<String, Object> properties = new LinkedHashMap<String, Object>();
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID, "name");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_REPOSITORIES_PROPERTY_ID, "repositories");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_PROPERTY_ID, "stack");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID, "upgrade");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_VERSION_PROPERTY_ID, "version");
    propertySet.add(properties);

    final Request getRequest = PropertyHelper.getReadRequest(
        RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID,
        RepositoryVersionResourceProvider.REPOSITORY_VERSION_REPOSITORIES_PROPERTY_ID,
        RepositoryVersionResourceProvider.REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID);
    Assert.assertEquals(0, provider.getResources(getRequest, null).size());

    final Request createRequest = PropertyHelper.getCreateRequest(propertySet, null);
    provider.createResources(createRequest);

    Assert.assertEquals(1, provider.getResources(getRequest, null).size());
    Assert.assertEquals("name", provider.getResources(getRequest, null).iterator().next().getPropertyValue(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID));

    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_ID_PROPERTY_ID, "1");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID, "name2");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_REPOSITORIES_PROPERTY_ID, "repositories2");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID, "upgrade2");
    final Request updateRequest = PropertyHelper.getUpdateRequest(properties, null);
    provider.updateResources(updateRequest, null);

    Assert.assertEquals("name2", provider.getResources(getRequest, null).iterator().next().getPropertyValue(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID));
    Assert.assertEquals("repositories2", provider.getResources(getRequest, null).iterator().next().getPropertyValue(RepositoryVersionResourceProvider.REPOSITORY_VERSION_REPOSITORIES_PROPERTY_ID));
    Assert.assertEquals("upgrade2", provider.getResources(getRequest, null).iterator().next().getPropertyValue(RepositoryVersionResourceProvider.REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID));
  }

  @After
  public void after() {
    injector.getInstance(PersistService.class).stop();
    injector = null;
  }
}
