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

package org.apache.ambari.server.orm.entities;

import com.google.gson.Gson;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * BlueprintEntity unit tests
 */
public class BlueprintEntityTest {
  @Test
  public void testSetGetBlueprintName() {
    BlueprintEntity entity = new BlueprintEntity();
    entity.setBlueprintName("foo");
    assertEquals("foo", entity.getBlueprintName());
  }

  @Test
  public void testSetGetStackName() {
    BlueprintEntity entity = new BlueprintEntity();
    entity.setStackName("foo");
    assertEquals("foo", entity.getStackName());
  }

  @Test
  public void testSetGetStackVersion() {
    BlueprintEntity entity = new BlueprintEntity();
    entity.setStackVersion("1");
    assertEquals("1", entity.getStackVersion());
  }

  @Test
  public void testSetGetHostGroups() {
    BlueprintEntity entity = new BlueprintEntity();
    Collection<HostGroupEntity> hostGroups = Collections.emptyList();
    entity.setHostGroups(hostGroups);
    assertSame(hostGroups, entity.getHostGroups());
  }

  @Test
  public void testSetGetConfigurations() {
    BlueprintEntity entity = new BlueprintEntity();
    Collection<BlueprintConfigEntity> configurations = Collections.emptyList();
    entity.setConfigurations(configurations);
    assertSame(configurations, entity.getConfigurations());
  }

  @Test
  public void testValidateConfigurations_clusterConfig() throws Exception {
    AmbariMetaInfo metaInfo = createMock(AmbariMetaInfo.class);

    Map<String, PropertyInfo> requiredProps = new HashMap<String, PropertyInfo>();
    PropertyInfo prop = new PropertyInfo();
    prop.setFilename("core-site.xml");
    prop.setName("super.secret.password");
    prop.setRequireInput(true);
    prop.setType(PropertyInfo.PropertyType.PASSWORD);
    prop.setValue(null);
    requiredProps.put("service1", prop);

    BlueprintEntity entity = new BlueprintEntity();
    entity.setStackName("stackName");
    entity.setStackVersion("version");

    Collection<BlueprintConfigEntity> configurations = new HashSet<BlueprintConfigEntity>();
    BlueprintConfigEntity configEntity = new BlueprintConfigEntity();
    configEntity.setBlueprintEntity(entity);
    configEntity.setBlueprintName("blueprint");
    configEntity.setType("core-site");

    Map<String, String> configData = new HashMap<String, String>();
    configData.put("foo", "val1");
    configData.put("bar", "val2");
    configData.put("super.secret.password", "password");
    configEntity.setConfigData(new Gson().toJson(configData));

    configurations.add(configEntity);
    entity.setConfigurations(configurations);

    Collection<HostGroupEntity> hostGroupEntities = new HashSet<HostGroupEntity>();
    HostGroupEntity hostGroupEntity = new HostGroupEntity();
    Collection<HostGroupComponentEntity> hostGroupComponents = new HashSet<HostGroupComponentEntity>();
    HostGroupComponentEntity componentEntity = new HostGroupComponentEntity();
    componentEntity.setName("component1");
    componentEntity.setBlueprintName("blueprint");
    componentEntity.setHostGroupEntity(hostGroupEntity);
    componentEntity.setHostGroupName("group1");
    hostGroupComponents.add(componentEntity);
    hostGroupEntity.setComponents(hostGroupComponents);
    hostGroupEntity.setConfigurations(Collections.<HostGroupConfigEntity>emptyList());
    hostGroupEntities.add(hostGroupEntity);
    entity.setHostGroups(hostGroupEntities);

    expect(metaInfo.getComponentToService("stackName", "version", "component1")).andReturn("service1");
    expect(metaInfo.getRequiredProperties("stackName", "version", "service1")).andReturn(requiredProps);

    replay(metaInfo);

    Map<String, Map<String, Collection<String>>> missingProps = entity.validateConfigurations(
        metaInfo, PropertyInfo.PropertyType.PASSWORD);

    assertTrue(missingProps.isEmpty());

    verify(metaInfo);
  }

  @Test
  public void testValidateConfigurations_hostGroupConfig() throws Exception {
    AmbariMetaInfo metaInfo = createMock(AmbariMetaInfo.class);

    Map<String, PropertyInfo> requiredProps = new HashMap<String, PropertyInfo>();
    PropertyInfo prop = new PropertyInfo();
    prop.setFilename("core-site.xml");
    prop.setName("super.secret.password");
    prop.setRequireInput(true);
    prop.setType(PropertyInfo.PropertyType.PASSWORD);
    prop.setValue(null);
    requiredProps.put("service1", prop);

    BlueprintEntity entity = new BlueprintEntity();
    entity.setStackName("stackName");
    entity.setStackVersion("version");

    entity.setConfigurations(Collections.<BlueprintConfigEntity>emptyList());

    Collection<HostGroupEntity> hostGroupEntities = new HashSet<HostGroupEntity>();
    HostGroupEntity hostGroupEntity = new HostGroupEntity();
    Collection<HostGroupComponentEntity> hostGroupComponents = new HashSet<HostGroupComponentEntity>();
    HostGroupComponentEntity componentEntity = new HostGroupComponentEntity();
    componentEntity.setName("component1");
    componentEntity.setBlueprintName("blueprint");
    componentEntity.setHostGroupEntity(hostGroupEntity);
    componentEntity.setHostGroupName("group1");
    hostGroupComponents.add(componentEntity);
    hostGroupEntity.setComponents(hostGroupComponents);

    Collection<HostGroupConfigEntity> configurations = new HashSet<HostGroupConfigEntity>();
    HostGroupConfigEntity configEntity = new HostGroupConfigEntity();
    configEntity.setHostGroupEntity(hostGroupEntity);
    configEntity.setBlueprintName("blueprint");
    configEntity.setType("core-site");

    Map<String, String> configData = new HashMap<String, String>();
    configData.put("foo", "val1");
    configData.put("bar", "val2");
    configData.put("super.secret.password", "password");
    configEntity.setConfigData(new Gson().toJson(configData));
    configurations.add(configEntity);

    hostGroupEntity.setConfigurations(configurations);
    hostGroupEntities.add(hostGroupEntity);
    entity.setHostGroups(hostGroupEntities);

    expect(metaInfo.getComponentToService("stackName", "version", "component1")).andReturn("service1");
    expect(metaInfo.getRequiredProperties("stackName", "version", "service1")).andReturn(requiredProps);

    replay(metaInfo);

    Map<String, Map<String, Collection<String>>> missingProps = entity.validateConfigurations(
        metaInfo, PropertyInfo.PropertyType.PASSWORD);

    assertTrue(missingProps.isEmpty());

    verify(metaInfo);
  }

  @Test
  public void testValidateConfigurations_negative() throws Exception {
    AmbariMetaInfo metaInfo = createMock(AmbariMetaInfo.class);

    Map<String, PropertyInfo> requiredProps = new HashMap<String, PropertyInfo>();
    PropertyInfo prop = new PropertyInfo();
    prop.setFilename("core-site.xml");
    prop.setName("super.secret.password");
    prop.setRequireInput(true);
    prop.setType(PropertyInfo.PropertyType.PASSWORD);
    prop.setValue(null);
    requiredProps.put("service1", prop);

    BlueprintEntity entity = new BlueprintEntity();
    entity.setStackName("stackName");
    entity.setStackVersion("version");

    Collection<BlueprintConfigEntity> configurations = new HashSet<BlueprintConfigEntity>();
    BlueprintConfigEntity configEntity = new BlueprintConfigEntity();
    configEntity.setBlueprintEntity(entity);
    configEntity.setBlueprintName("blueprint");
    configEntity.setType("core-site");

    Map<String, String> configData = new HashMap<String, String>();
    configData.put("foo", "val1");
    configData.put("bar", "val2");
    configData.put("some.other.secret.password", "password");
    configEntity.setConfigData(new Gson().toJson(configData));

    configurations.add(configEntity);
    entity.setConfigurations(configurations);

    Collection<HostGroupEntity> hostGroupEntities = new HashSet<HostGroupEntity>();
    HostGroupEntity hostGroupEntity = new HostGroupEntity();
    Collection<HostGroupComponentEntity> hostGroupComponents = new HashSet<HostGroupComponentEntity>();
    HostGroupComponentEntity componentEntity = new HostGroupComponentEntity();
    componentEntity.setName("component1");
    componentEntity.setBlueprintName("blueprint");
    componentEntity.setHostGroupEntity(hostGroupEntity);
    componentEntity.setHostGroupName("group1");
    hostGroupComponents.add(componentEntity);
    hostGroupEntity.setComponents(hostGroupComponents);
    hostGroupEntity.setConfigurations(Collections.<HostGroupConfigEntity>emptyList());
    hostGroupEntities.add(hostGroupEntity);
    entity.setHostGroups(hostGroupEntities);

    expect(metaInfo.getComponentToService("stackName", "version", "component1")).andReturn("service1");
    expect(metaInfo.getRequiredProperties("stackName", "version", "service1")).andReturn(requiredProps);

    replay(metaInfo);

    Map<String, Map<String, Collection<String>>> missingProps = entity.validateConfigurations(
        metaInfo, PropertyInfo.PropertyType.PASSWORD);

    assertEquals(1, missingProps.size());

    verify(metaInfo);
  }
}
