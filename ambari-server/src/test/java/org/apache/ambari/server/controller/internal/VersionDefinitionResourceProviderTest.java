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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.HashMap;
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
import org.apache.ambari.server.stack.StackManager;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.collect.Sets;
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
  public void testWithParentFromBase64() throws Exception {
    Authentication authentication = TestAuthenticationFactory.createAdministrator();
    SecurityContextHolder.getContext().setAuthentication(authentication);

    File file = new File("src/test/resources/version_definition_resource_provider.xml");

    byte[] bytes = IOUtils.toByteArray(new FileInputStream(file));
    String base64Str = Base64.encodeBase64String(bytes);

    final ResourceProvider versionProvider = new VersionDefinitionResourceProvider();
    final ResourceProvider provider = injector.getInstance(ResourceProviderFactory.class)
        .getRepositoryVersionResourceProvider();

    final Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();
    final Map<String, Object> properties = new LinkedHashMap<String, Object>();
    properties.put(VersionDefinitionResourceProvider.VERSION_DEF_DEFINITION_BASE64, base64Str);
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

  @Test
  public void testGetAvailable() throws Exception {
    AmbariMetaInfo ami = injector.getInstance(AmbariMetaInfo.class);
    // ensure that all of the latest repo retrieval tasks have completed
    StackManager sm = ami.getStackManager();
    int maxWait = 15000;
    int waitTime = 0;
    while (waitTime < maxWait && ! sm.haveAllRepoUrlsBeenResolved()) {
      Thread.sleep(5);
      waitTime += 5;
    }

    if (waitTime >= maxWait) {
      fail("Latest Repo tasks did not complete");
    }

    Authentication authentication = TestAuthenticationFactory.createAdministrator();
    SecurityContextHolder.getContext().setAuthentication(authentication);

    final ResourceProvider versionProvider = new VersionDefinitionResourceProvider();

    Request getRequest = PropertyHelper.getReadRequest("VersionDefinition");

    Predicate predicate = new PredicateBuilder().property(
        VersionDefinitionResourceProvider.SHOW_AVAILABLE).equals("true").toPredicate();

    Set<Resource> results = versionProvider.getResources(getRequest, predicate);
    Assert.assertEquals(2, results.size());

    boolean found1 = false;
    boolean found2 = false;
    for (Resource res : results) {
      if ("HDP-2.2.0-2.2.1.0".equals(res.getPropertyValue("VersionDefinition/id"))) {
        Assert.assertEquals(Boolean.FALSE, res.getPropertyValue("VersionDefinition/stack_default"));
        found1 = true;
      } else if ("HDP-2.2.0".equals(res.getPropertyValue("VersionDefinition/id"))) {
        Assert.assertEquals(Boolean.TRUE, res.getPropertyValue("VersionDefinition/stack_default"));

        VersionDefinitionXml vdf = ami.getVersionDefinition("HDP-2.2.0");

        Assert.assertNotNull(vdf);
        Assert.assertEquals(2, vdf.repositoryInfo.getOses().size());

        String family1 = vdf.repositoryInfo.getOses().get(0).getFamily();
        String family2 = vdf.repositoryInfo.getOses().get(1).getFamily();

        Assert.assertFalse(family1.equals(family2));
        Assert.assertTrue(Sets.newHashSet("suse11", "redhat6").contains(family1));
        Assert.assertTrue(Sets.newHashSet("suse11", "redhat6").contains(family2));



        found2 = true;
      }
    }

    Assert.assertTrue(found1);
    Assert.assertTrue(found2);

  }

  @Test
  public void testCreateByAvailable() throws Exception {
    AmbariMetaInfo ami = injector.getInstance(AmbariMetaInfo.class);
    // ensure that all of the latest repo retrieval tasks have completed
    StackManager sm = ami.getStackManager();
    int maxWait = 15000;
    int waitTime = 0;
    while (waitTime < maxWait && ! sm.haveAllRepoUrlsBeenResolved()) {
      Thread.sleep(5);
      waitTime += 5;
    }

    if (waitTime >= maxWait) {
      fail("Latest Repo tasks did not complete");
    }

    Authentication authentication = TestAuthenticationFactory.createAdministrator();
    SecurityContextHolder.getContext().setAuthentication(authentication);

    final ResourceProvider versionProvider = new VersionDefinitionResourceProvider();

    Request getRequest = PropertyHelper.getReadRequest("VersionDefinition");
    Set<Resource> results = versionProvider.getResources(getRequest, null);
    Assert.assertEquals(0, results.size());

    Map<String, Object> createMap = new HashMap<>();
    createMap.put("VersionDefinition/available", "HDP-2.2.0-2.2.1.0");

    Request createRequest = PropertyHelper.getCreateRequest(Collections.singleton(createMap), null);
    versionProvider.createResources(createRequest);

    results = versionProvider.getResources(getRequest, null);
    Assert.assertEquals(1, results.size());
  }

  @Test
  public void testCreateDryRun() throws Exception {
    Authentication authentication = TestAuthenticationFactory.createAdministrator();
    SecurityContextHolder.getContext().setAuthentication(authentication);

    File file = new File("src/test/resources/version_definition_resource_provider.xml");

    final ResourceProvider versionProvider = new VersionDefinitionResourceProvider();

    final Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();
    final Map<String, Object> properties = new LinkedHashMap<String, Object>();
    properties.put(VersionDefinitionResourceProvider.VERSION_DEF_DEFINITION_URL,
        file.toURI().toURL().toString());
    propertySet.add(properties);

    Map<String, String> info = Collections.singletonMap(Request.DIRECTIVE_DRY_RUN, "true");

    final Request createRequest = PropertyHelper.getCreateRequest(propertySet, info);
    RequestStatus status = versionProvider.createResources(createRequest);
    Assert.assertEquals(1, status.getAssociatedResources().size());

    Resource res = status.getAssociatedResources().iterator().next();
    // because we aren't using subresources, but return subresource-like properties, the key is an empty string
    Assert.assertTrue(res.getPropertiesMap().containsKey(""));
    Map<String, Object> resMap = res.getPropertiesMap().get("");
    Assert.assertTrue(resMap.containsKey("operating_systems"));

    Assert.assertTrue(res.getPropertiesMap().containsKey("VersionDefinition"));
    Assert.assertEquals("2.2.0.8-5678", res.getPropertyValue("VersionDefinition/repository_version"));
    Assert.assertNull(res.getPropertyValue("VersionDefinition/show_available"));

    Request getRequest = PropertyHelper.getReadRequest("VersionDefinition");
    Set<Resource> results = versionProvider.getResources(getRequest, null);
    Assert.assertEquals(0, results.size());
  }

  @Test
  public void testCreateDryRunByAvailable() throws Exception {
    AmbariMetaInfo ami = injector.getInstance(AmbariMetaInfo.class);
    // ensure that all of the latest repo retrieval tasks have completed
    StackManager sm = ami.getStackManager();
    int maxWait = 15000;
    int waitTime = 0;
    while (waitTime < maxWait && ! sm.haveAllRepoUrlsBeenResolved()) {
      Thread.sleep(5);
      waitTime += 5;
    }

    if (waitTime >= maxWait) {
      fail("Latest Repo tasks did not complete");
    }

    Authentication authentication = TestAuthenticationFactory.createAdministrator();
    SecurityContextHolder.getContext().setAuthentication(authentication);

    final ResourceProvider versionProvider = new VersionDefinitionResourceProvider();

    Request getRequest = PropertyHelper.getReadRequest("VersionDefinition");
    Set<Resource> results = versionProvider.getResources(getRequest, null);
    Assert.assertEquals(0, results.size());

    Map<String, Object> createMap = new HashMap<>();
    createMap.put("VersionDefinition/available", "HDP-2.2.0-2.2.1.0");

    Map<String, String> infoProps = Collections.singletonMap(Request.DIRECTIVE_DRY_RUN, "true");

    Request createRequest = PropertyHelper.getCreateRequest(Collections.singleton(createMap), infoProps);
    RequestStatus status = versionProvider.createResources(createRequest);

    Assert.assertEquals(1, status.getAssociatedResources().size());

    Resource res = status.getAssociatedResources().iterator().next();
    // because we aren't using subresources, but return subresource-like properties, the key is an empty string
    Assert.assertTrue(res.getPropertiesMap().containsKey(""));
    Map<String, Object> resMap = res.getPropertiesMap().get("");
    Assert.assertTrue(resMap.containsKey("operating_systems"));

    Assert.assertTrue(res.getPropertiesMap().containsKey("VersionDefinition"));
    Assert.assertEquals("2.2.1.0", res.getPropertyValue("VersionDefinition/repository_version"));
    Assert.assertNotNull(res.getPropertyValue("VersionDefinition/show_available"));
    Assert.assertNotNull(res.getPropertyValue("VersionDefinition/validation"));

    Set<String> validation = (Set<String>) res.getPropertyValue("VersionDefinition/validation");
    Assert.assertNotNull(validation);
    Assert.assertEquals(0, validation.size());

    getRequest = PropertyHelper.getReadRequest("VersionDefinition");
    results = versionProvider.getResources(getRequest, null);
    Assert.assertEquals(0, results.size());
  }

  @Test
  public void testCreateDryWithValidation() throws Exception {
    AmbariMetaInfo ami = injector.getInstance(AmbariMetaInfo.class);
    // ensure that all of the latest repo retrieval tasks have completed
    StackManager sm = ami.getStackManager();
    int maxWait = 15000;
    int waitTime = 0;
    while (waitTime < maxWait && ! sm.haveAllRepoUrlsBeenResolved()) {
      Thread.sleep(5);
      waitTime += 5;
    }

    if (waitTime >= maxWait) {
      fail("Latest Repo tasks did not complete");
    }


    // !!! make sure we have none
    Authentication authentication = TestAuthenticationFactory.createAdministrator();
    SecurityContextHolder.getContext().setAuthentication(authentication);

    final ResourceProvider versionProvider = new VersionDefinitionResourceProvider();

    Request getRequest = PropertyHelper.getReadRequest("VersionDefinition");
    Set<Resource> results = versionProvider.getResources(getRequest, null);
    Assert.assertEquals(0, results.size());


    // !!! create one
    Map<String, Object> createMap = new HashMap<>();
    createMap.put("VersionDefinition/available", "HDP-2.2.0-2.2.1.0");

    Request createRequest = PropertyHelper.getCreateRequest(Collections.singleton(createMap), null);
    versionProvider.createResources(createRequest);

    results = versionProvider.getResources(getRequest, null);
    Assert.assertEquals(1, results.size());


    // !!! create one, but a dry run to make sure we get two validation errors
    Map<String, String> infoProps = new HashMap<>();
    infoProps.put(Request.DIRECTIVE_DRY_RUN, "true");

    createRequest = PropertyHelper.getCreateRequest(Collections.singleton(createMap), infoProps);
    RequestStatus status = versionProvider.createResources(createRequest);

    Assert.assertEquals(1, status.getAssociatedResources().size());

    Resource res = status.getAssociatedResources().iterator().next();
    // because we aren't using subresources, but return subresource-like properties, the key is an empty string
    Assert.assertTrue(res.getPropertiesMap().containsKey(""));
    Map<String, Object> resMap = res.getPropertiesMap().get("");
    Assert.assertTrue(resMap.containsKey("operating_systems"));

    Assert.assertTrue(res.getPropertiesMap().containsKey("VersionDefinition"));
    Assert.assertEquals("2.2.1.0", res.getPropertyValue("VersionDefinition/repository_version"));
    Assert.assertNotNull(res.getPropertyValue("VersionDefinition/show_available"));
    Assert.assertEquals("HDP-2.2.0.4", res.getPropertyValue("VersionDefinition/display_name"));
    Assert.assertNotNull(res.getPropertyValue("VersionDefinition/validation"));

    Set<String> validation = (Set<String>) res.getPropertyValue("VersionDefinition/validation");
    Assert.assertEquals(3, validation.size());

    validation = (Set<String>) res.getPropertyValue("VersionDefinition/validation");
    Assert.assertEquals(3, validation.size());

    boolean found = false;
    for (String reason : validation) {
      if (reason.contains("http://baseurl1")) {
        found = true;
      }
    }

    Assert.assertTrue("URL validation should be checked", found);


    // !!! test url validation
    infoProps.put(VersionDefinitionResourceProvider.DIRECTIVE_SKIP_URL_CHECK, "true");
    createRequest = PropertyHelper.getCreateRequest(Collections.singleton(createMap), infoProps);
    status = versionProvider.createResources(createRequest);

    Assert.assertEquals(1, status.getAssociatedResources().size());

    res = status.getAssociatedResources().iterator().next();
    Assert.assertTrue(res.getPropertiesMap().containsKey("VersionDefinition"));
    Assert.assertEquals("2.2.1.0", res.getPropertyValue("VersionDefinition/repository_version"));
    Assert.assertNotNull(res.getPropertyValue("VersionDefinition/show_available"));
    Assert.assertNotNull(res.getPropertyValue("VersionDefinition/validation"));

    validation = (Set<String>) res.getPropertyValue("VersionDefinition/validation");
    Assert.assertEquals(2, validation.size());
    for (String reason : validation) {
      if (reason.contains("http://baseurl1")) {
        Assert.fail("URL validation should be skipped for http://baseurl1");
      }
    }

    // dry-run with a changed name
    infoProps.remove(VersionDefinitionResourceProvider.DIRECTIVE_SKIP_URL_CHECK);
    createMap.put(VersionDefinitionResourceProvider.VERSION_DEF_DISPLAY_NAME, "HDP-2.2.0.4-a");
    createRequest = PropertyHelper.getCreateRequest(Collections.singleton(createMap), infoProps);
    status = versionProvider.createResources(createRequest);

    Assert.assertEquals(1, status.getAssociatedResources().size());

    res = status.getAssociatedResources().iterator().next();
    Assert.assertTrue(res.getPropertiesMap().containsKey("VersionDefinition"));
    Assert.assertEquals("HDP-2.2.0.4-a", res.getPropertyValue("VersionDefinition/repository_version"));
    Assert.assertEquals("HDP-2.2.0.4-a", res.getPropertyValue("VersionDefinition/display_name"));
    Assert.assertNotNull(res.getPropertyValue("VersionDefinition/show_available"));
    Assert.assertNotNull(res.getPropertyValue("VersionDefinition/validation"));

    validation = (Set<String>) res.getPropertyValue("VersionDefinition/validation");
    Assert.assertEquals(1, validation.size());
  }
}
