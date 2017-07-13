/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.apache.ambari.server.orm.entities.ConfigurationBaseEntity;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AmbariConfigurationResourceProviderTest extends EasyMockSupport {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock
  private Request requestMock;

  @Mock
  private AmbariConfigurationDAO ambariConfigurationDAO;

  private Capture<AmbariConfigurationEntity> ambariConfigurationEntityCapture;

  private Gson gson;

  private static final String DATA_MOCK_STR = "[\n" +
    "      {\n" +
    "        \"authentication.ldap.baseDn\" : \"dc=ambari,dc=apache,dc=org\",\n" +
    "        \"authentication.ldap.primaryUrl\" : \"localhost:33389\",\n" +
    "        \"authentication.ldap.secondaryUrl\" : \"localhost:333\"\n" +
    "      }\n" +
    "    ]";

  private static final Long PK_LONG = Long.valueOf(1);
  private static final String PK_STRING = String.valueOf(1);
  private static final String VERSION_TAG = "test version";
  private static final String VERSION = "1";

  @TestSubject
  private AmbariConfigurationResourceProvider ambariConfigurationResourceProvider = new AmbariConfigurationResourceProvider();

  @Before
  public void setup() {
    ambariConfigurationEntityCapture = Capture.newInstance();
    gson = new GsonBuilder().create();
  }

  @Test
  public void testCreateAmbariConfigurationRequestResultsInTheProperPersistenceCall() throws Exception {

    // GIVEN
    // configuration properties parsed from the request
    Set<Map<String, Object>> resourcePropertiesSet = Sets.newHashSet(
      new PropertiesMapBuilder()
        .withId(PK_LONG)
        .withVersion(VERSION)
        .withVersionTag(VERSION_TAG)
        .withData(DATA_MOCK_STR)
        .build());

    // mock the request to return the properties
    EasyMock.expect(requestMock.getProperties()).andReturn(resourcePropertiesSet);

    // capture the entity the DAO gets called with
    ambariConfigurationDAO.create(EasyMock.capture(ambariConfigurationEntityCapture));
    replayAll();

    // WHEN
    ambariConfigurationResourceProvider.createResourcesAuthorized(requestMock);

    // THEN
    AmbariConfigurationEntity capturedAmbariConfigurationEntity = ambariConfigurationEntityCapture.getValue();
    Assert.assertNotNull(capturedAmbariConfigurationEntity);
    Assert.assertNull("The entity identifier should be null", capturedAmbariConfigurationEntity.getId());
    Assert.assertEquals("The entity version is not the expected", Integer.valueOf(VERSION),
      capturedAmbariConfigurationEntity.getConfigurationBaseEntity().getVersion());
    Assert.assertEquals("The entity version tag is not the expected", VERSION_TAG,
      capturedAmbariConfigurationEntity.getConfigurationBaseEntity().getVersionTag());
    Assert.assertEquals("The entity data is not the expected", DATA_MOCK_STR,
      gson.fromJson(capturedAmbariConfigurationEntity.getConfigurationBaseEntity().getConfigurationData(), String.class));
  }

  @Test
  public void testRemoveAmbariConfigurationRequestResultsInTheProperPersistenceCall() throws Exception {
    // GIVEN
    Predicate predicate = new PredicateBuilder().property(
      AmbariConfigurationResourceProvider.ResourcePropertyId.ID.getPropertyId()).equals("1").toPredicate();

    Capture<Long> pkCapture = Capture.newInstance();
    ambariConfigurationDAO.removeByPK(EasyMock.capture(pkCapture));
    replayAll();

    // WHEN
    ambariConfigurationResourceProvider.deleteResourcesAuthorized(requestMock, predicate);

    // THEN
    Assert.assertEquals("The pk of the entity to be removed doen't match the expected id", Long.valueOf(1), pkCapture.getValue());
  }


  @Test
  public void testRetrieveAmbariConfigurationShouldResultsInTheProperDAOCall() throws Exception {
    // GIVEN
    Predicate predicate = new PredicateBuilder().property(
      AmbariConfigurationResourceProvider.ResourcePropertyId.ID.getPropertyId()).equals("1").toPredicate();

    EasyMock.expect(ambariConfigurationDAO.findAll()).andReturn(Lists.newArrayList(createDummyAmbariConfigurationEntity()));
    replayAll();

    // WHEN
    Set<Resource> resourceSet = ambariConfigurationResourceProvider.getResourcesAuthorized(requestMock, predicate);

    // THEN
    Assert.assertNotNull(resourceSet);
    Assert.assertFalse(resourceSet.isEmpty());
  }

  @Test
  public void testUpdateAmbariConfigurationShouldResultInTheProperDAOCalls() throws Exception {
    // GIVEN

    Predicate predicate = new PredicateBuilder().property(
      AmbariConfigurationResourceProvider.ResourcePropertyId.ID.getPropertyId()).equals("1").toPredicate();

    // properteies in the request, representing the updated configuration
    Set<Map<String, Object>> resourcePropertiesSet = Sets.newHashSet(new PropertiesMapBuilder()
      .withId(PK_LONG)
      .withVersion("2")
      .withVersionTag("version-2")
      .withData(DATA_MOCK_STR).build());

    EasyMock.expect(requestMock.getProperties()).andReturn(resourcePropertiesSet);

    AmbariConfigurationEntity persistedEntity = createDummyAmbariConfigurationEntity();
    EasyMock.expect(ambariConfigurationDAO.findByPK(PK_LONG)).andReturn(persistedEntity);
    ambariConfigurationDAO.create(EasyMock.capture(ambariConfigurationEntityCapture));

    replayAll();

    // WHEN
    ambariConfigurationResourceProvider.updateResourcesAuthorized(requestMock, predicate);

    // the captured entity should be the updated one
    AmbariConfigurationEntity updatedEntity = ambariConfigurationEntityCapture.getValue();

    // THEN
    Assert.assertNotNull(updatedEntity);
    Assert.assertEquals("The updated version is wrong", Integer.valueOf(2), updatedEntity.getConfigurationBaseEntity().getVersion());
  }

  private class PropertiesMapBuilder {

    private Map<String, Object> resourcePropertiesMap = Maps.newHashMap();

    private PropertiesMapBuilder() {
    }

    public PropertiesMapBuilder withId(Long id) {
      resourcePropertiesMap.put(AmbariConfigurationResourceProvider.ResourcePropertyId.ID.getPropertyId(), id);
      return this;
    }

    private PropertiesMapBuilder withVersion(String version) {
      resourcePropertiesMap.put(AmbariConfigurationResourceProvider.ResourcePropertyId.VERSION.getPropertyId(), version);
      return this;
    }

    private PropertiesMapBuilder withVersionTag(String versionTag) {
      resourcePropertiesMap.put(AmbariConfigurationResourceProvider.ResourcePropertyId.VERSION_TAG.getPropertyId(), versionTag);
      return this;
    }

    private PropertiesMapBuilder withData(String dataJson) {
      resourcePropertiesMap.put(AmbariConfigurationResourceProvider.ResourcePropertyId.DATA.getPropertyId(), dataJson);
      return this;
    }

    public Map<String, Object> build() {
      return this.resourcePropertiesMap;
    }

  }

  private AmbariConfigurationEntity createDummyAmbariConfigurationEntity() {
    AmbariConfigurationEntity acEntity = new AmbariConfigurationEntity();
    ConfigurationBaseEntity configurationBaseEntity = new ConfigurationBaseEntity();
    acEntity.setConfigurationBaseEntity(configurationBaseEntity);
    acEntity.setId(PK_LONG);
    acEntity.getConfigurationBaseEntity().setConfigurationData(DATA_MOCK_STR);
    acEntity.getConfigurationBaseEntity().setVersion(Integer.valueOf(VERSION));
    acEntity.getConfigurationBaseEntity().setVersionTag(VERSION_TAG);
    acEntity.getConfigurationBaseEntity().setType("ldap-config");

    return acEntity;
  }


}