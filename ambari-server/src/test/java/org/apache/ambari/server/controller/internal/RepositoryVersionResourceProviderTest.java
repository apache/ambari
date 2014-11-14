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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
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
import org.apache.ambari.server.state.OperatingSystemInfo;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * RepositoryVersionResourceProvider tests.
 */
public class RepositoryVersionResourceProviderTest {

  private static Injector injector;

  @Before
  public void before() throws Exception {
    final Set<String> validVersions = Sets.newHashSet("1.1.1.1", "1.1.343432.2", "1.1.343432.2-234234324");
    final AmbariMetaInfo ambariMetaInfo = Mockito.mock(AmbariMetaInfo.class);
    final AbstractModule injectorModule = new InMemoryDefaultTestModule() {
      protected void configure() {
        super.configure();
        bind(AmbariMetaInfo.class).toInstance(ambariMetaInfo);
      };
    };
    injector = Guice.createInjector(injectorModule);

    final StackInfo stackInfo = new StackInfo() {
      @Override
      public Map<String, UpgradePack> getUpgradePacks() {
        final Map<String, UpgradePack> map = new HashMap<String, UpgradePack>();
        final UpgradePack pack1 = new UpgradePack() {
          @Override
          public String getTarget() {
            return "1.1.*.*";
          }
        };
        final UpgradePack pack2 = new UpgradePack() {
          @Override
          public String getTarget() {
            return "1.1.*.*";
          }
        };
        map.put("pack1", pack1);
        map.put("pack2", pack2);
        return map;
      }
    };
    Mockito.when(ambariMetaInfo.getStack(Mockito.anyString(), Mockito.anyString())).thenAnswer(new Answer<StackInfo>() {

      @Override
      public StackInfo answer(InvocationOnMock invocation) throws Throwable {
        final String stack = invocation.getArguments()[0].toString();
        final String version = invocation.getArguments()[1].toString();
        if (stack.equals("HDP") && validVersions.contains(version)) {
          return stackInfo;
        } else {
          throw new Exception("error");
        }
      }

    });

    final HashSet<OperatingSystemInfo> osInfos = new HashSet<OperatingSystemInfo>();
    osInfos.add(new OperatingSystemInfo("redhat6"));
    osInfos.add(new OperatingSystemInfo("redhat7"));
    Mockito.when(ambariMetaInfo.getOperatingSystems(Mockito.anyString(), Mockito.anyString())).thenAnswer(new Answer<Set<OperatingSystemInfo>>() {

      @Override
      public Set<OperatingSystemInfo> answer(InvocationOnMock invocation)
          throws Throwable {
        final String stack = invocation.getArguments()[0].toString();
        final String version = invocation.getArguments()[1].toString();
        if (stack.equals("HDP") && validVersions.contains(version)) {
          return osInfos;
        } else {
          return new HashSet<OperatingSystemInfo>();
        }
      }
    });

    injector.getInstance(GuiceJpaInitializer.class);
  }

  @Test
  public void testCreateResources() throws Exception {
    final ResourceProvider provider = injector.getInstance(ResourceProviderFactory.class).getRepositoryVersionProvider();

    final Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();
    final Map<String, Object> properties = new LinkedHashMap<String, Object>();
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID, "name");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_REPOSITORIES_PROPERTY_ID, "[{\"os\":\"redhat6\"}]");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_PROPERTY_ID, "HDP");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID, "pack1");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_VERSION_PROPERTY_ID, "1.1.1.1");
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
    entity.setRepositories("[{\"os\":\"redhat6\"}]");
    entity.setStack("HDP");
    entity.setUpgradePackage("pack1");
    entity.setVersion("1.1.1.1");

    final Request getRequest = PropertyHelper.getReadRequest(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID);
    Assert.assertEquals(0, provider.getResources(getRequest, null).size());

    repositoryVersionDAO.create(entity);

    Assert.assertEquals(1, provider.getResources(getRequest, null).size());
  }

  @Test
  public void testValidateRepositoryVersion() throws Exception {
    final RepositoryVersionResourceProvider provider = (RepositoryVersionResourceProvider)
        injector.getInstance(ResourceProviderFactory.class).getRepositoryVersionProvider();

    final RepositoryVersionEntity entity = new RepositoryVersionEntity();
    entity.setDisplayName("name");
    entity.setStack("HDP");
    entity.setUpgradePackage("pack1");
    entity.setVersion("1.1.1.1");
    entity.setRepositories(new Gson().toJson("[{\"os\":\"redhat6\"}]"));

    // test valid usecases
    provider.validateRepositoryVersion(entity);
    entity.setVersion("1.1.343432.2");
    provider.validateRepositoryVersion(entity);
    entity.setVersion("1.1.343432.2-234234324");
    provider.validateRepositoryVersion(entity);

    // test invalid usecases
    entity.setRepositories("[{\"os\":\"redhat8\"}]");
    try {
      provider.validateRepositoryVersion(entity);
      Assert.fail("Should throw exception");
    } catch (Exception ex) {
    }

    entity.setRepositories("");
    try {
      provider.validateRepositoryVersion(entity);
      Assert.fail("Should throw exception");
    } catch (Exception ex) {
    }

    entity.setUpgradePackage("pack2");
    try {
      provider.validateRepositoryVersion(entity);
      Assert.fail("Should throw exception");
    } catch (Exception ex) {
    }

    entity.setStack("BIGTOP");
    try {
      provider.validateRepositoryVersion(entity);
      Assert.fail("Should throw exception");
    } catch (Exception ex) {
    }
  }

  @Test
  public void testDeleteResources() throws Exception {
    final ResourceProvider provider = injector.getInstance(ResourceProviderFactory.class).getRepositoryVersionProvider();

    final Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();
    final Map<String, Object> properties = new LinkedHashMap<String, Object>();
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID, "name");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_REPOSITORIES_PROPERTY_ID, "[{\"os\":\"redhat6\"}]");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_PROPERTY_ID, "HDP");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID, "pack1");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_VERSION_PROPERTY_ID, "1.1.1.1");
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
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_REPOSITORIES_PROPERTY_ID, "[{\"os\":\"redhat6\"}]");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_PROPERTY_ID, "HDP");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID, "pack1");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_VERSION_PROPERTY_ID, "1.1.1.1");
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
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_REPOSITORIES_PROPERTY_ID, "[{\"os\":\"redhat7\"}]");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID, "pack2");
    final Request updateRequest = PropertyHelper.getUpdateRequest(properties, null);
    provider.updateResources(updateRequest, null);

    Assert.assertEquals("name2", provider.getResources(getRequest, null).iterator().next().getPropertyValue(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID));
    Assert.assertEquals("[{\"os\":\"redhat7\"}]", new Gson().toJson(provider.getResources(getRequest, null).iterator().next().getPropertyValue(RepositoryVersionResourceProvider.REPOSITORY_VERSION_REPOSITORIES_PROPERTY_ID)));
    Assert.assertEquals("pack2", provider.getResources(getRequest, null).iterator().next().getPropertyValue(RepositoryVersionResourceProvider.REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID));
  }

  @After
  public void after() {
    injector.getInstance(PersistService.class).stop();
    injector = null;
  }
}
