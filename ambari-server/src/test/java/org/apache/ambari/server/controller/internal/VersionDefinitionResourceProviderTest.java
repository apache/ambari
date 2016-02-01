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

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.ResourceProviderFactory;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Tests the VersionDefinitionResourceProvider class
 */
public class VersionDefinitionResourceProviderTest {
  private Injector injector;

  @Before
  public void before() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    AmbariMetaInfo ami = injector.getInstance(AmbariMetaInfo.class);
    ami.init();

    StackDAO stackDao = injector.getInstance(StackDAO.class);
    StackEntity stack = stackDao.find("HDP", "2.2.0");

    RepositoryVersionEntity entity = new RepositoryVersionEntity();
    entity.setStack(stack);
    entity.setDisplayName("2.2.0.0");
    entity.setVersion("2.3.4.4-1234");

    RepositoryVersionDAO dao = injector.getInstance(RepositoryVersionDAO.class);
    dao.create(entity);

  }

  @After
  public void after() throws Exception {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testWithParent() throws Exception {
    Authentication authentication = TestAuthenticationFactory.createAdministrator();
    SecurityContextHolder.getContext().setAuthentication(authentication);

    File file = new File("src/test/resources/version_definition_resource_provider.xml");

    final ResourceProvider versionProvider = new VersionDefinitionResourceProvider();
    final ResourceProvider provider = injector.getInstance(ResourceProviderFactory.class)
        .getRepositoryVersionResourceProvider();

    final Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();
    final Map<String, Object> properties = new LinkedHashMap<String, Object>();
    properties.put(VersionDefinitionResourceProvider.VERSION_DEF_DEFINITION_URL,
        file.toURI().toURL().toString());
    propertySet.add(properties);


    final Request createRequest = PropertyHelper.getCreateRequest(propertySet, null);
    RequestStatus status = versionProvider.createResources(createRequest);
    Assert.assertEquals(1, status.getAssociatedResources().size());

    Request getRequest = PropertyHelper.getReadRequest("VersionDefinition");
    Set<Resource> results = versionProvider.getResources(getRequest, null);
    Assert.assertEquals(1, results.size());

    final Predicate predicateStackName = new PredicateBuilder().property(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID).equals("HDP").toPredicate();
    final Predicate predicateStackVersion = new PredicateBuilder().property(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID).equals("2.2.0").toPredicate();

    results = provider.getResources(getRequest,
        new AndPredicate(predicateStackName, predicateStackVersion));
    Assert.assertEquals(1, results.size());

    getRequest = PropertyHelper.getReadRequest(
        RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID,
        RepositoryVersionResourceProvider.REPOSITORY_VERSION_ID_PROPERTY_ID,
        RepositoryVersionResourceProvider.REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID,
        RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID,
        RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID,
        RepositoryVersionResourceProvider.SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID,
        RepositoryVersionResourceProvider.SUBRESOURCE_REPOSITORIES_PROPERTY_ID,
        "RepositoryVersions/release", "RepositoryVersions/services",
        "RepositoryVersions/has_children", "RepositoryVersions/parent_id");

    results = provider.getResources(getRequest,
        new AndPredicate(predicateStackName, predicateStackVersion));
    Assert.assertEquals(2, results.size());

    Resource r = null;
    for (Resource result : results) {
      if (result.getPropertyValue("RepositoryVersions/repository_version").equals("2.2.0.8-5678")) {
        r = result;
        break;
      }
    }

    Assert.assertNotNull(r);
    Map<String, Map<String, Object>> map = r.getPropertiesMap();
    Assert.assertTrue(map.containsKey("RepositoryVersions"));

    Map<String, Object> vals = map.get("RepositoryVersions");

    Assert.assertEquals("2.2.0.8-5678", vals.get("repository_version"));
    Assert.assertNotNull(vals.get("parent_id"));
    Assert.assertEquals(Boolean.FALSE, vals.get("has_children"));


    Assert.assertTrue(map.containsKey("RepositoryVersions/release"));
    vals = map.get("RepositoryVersions/release");
    Assert.assertEquals("5678", vals.get("build"));
    Assert.assertEquals("2.3.4.[1-9]", vals.get("compatible_with"));
    Assert.assertEquals("http://docs.hortonworks.com/HDPDocuments/HDP2/HDP-2.3.4/",
        vals.get("notes"));
  }
}
