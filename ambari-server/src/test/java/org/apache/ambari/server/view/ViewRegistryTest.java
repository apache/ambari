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

package org.apache.ambari.server.view;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.bind.JAXBException;

import org.apache.ambari.server.api.resources.SubResourceDefinition;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.orm.dao.MemberDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.dao.ViewDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewEntityTest;
import org.apache.ambari.server.orm.entities.ViewInstanceDataEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntityTest;
import org.apache.ambari.server.security.SecurityHelper;
import org.apache.ambari.server.security.authorization.AmbariGrantedAuthority;
import org.apache.ambari.server.view.configuration.InstanceConfig;
import org.apache.ambari.server.view.configuration.InstanceConfigTest;
import org.apache.ambari.server.view.configuration.PropertyConfig;
import org.apache.ambari.server.view.configuration.ResourceConfig;
import org.apache.ambari.server.view.configuration.ResourceConfigTest;
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.apache.ambari.server.view.configuration.ViewConfigTest;
import org.apache.ambari.server.view.events.EventImpl;
import org.apache.ambari.server.view.events.EventImplTest;
import org.apache.ambari.view.events.Event;
import org.apache.ambari.view.events.Listener;
import org.easymock.EasyMock;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;

/**
 * ViewRegistry tests.
 */
public class ViewRegistryTest {

  private static String view_xml1 = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "</view>";

  private static String view_xml2 = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>2.0.0</version>\n" +
      "</view>";

  private static String xml_valid_instance = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <parameter>\n" +
      "        <name>p1</name>\n" +
      "        <description>Parameter 1.</description>\n" +
      "        <required>true</required>\n" +
      "    </parameter>\n" +
      "    <parameter>\n" +
      "        <name>p2</name>\n" +
      "        <description>Parameter 2.</description>\n" +
      "        <required>false</required>\n" +
      "    </parameter>\n" +
      "    <instance>\n" +
      "        <name>INSTANCE1</name>\n" +
      "        <label>My Instance 1!</label>\n" +
      "        <property>\n" +
      "            <key>p1</key>\n" +
      "            <value>v1-1</value>\n" +
      "        </property>\n" +
      "        <property>\n" +
      "            <key>p2</key>\n" +
      "            <value>v2-1</value>\n" +
      "        </property>\n" +
      "    </instance>\n" +
      "</view>";

  private static String xml_invalid_instance = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <parameter>\n" +
      "        <name>p1</name>\n" +
      "        <description>Parameter 1.</description>\n" +
      "        <required>true</required>\n" +
      "    </parameter>\n" +
      "    <parameter>\n" +
      "        <name>p2</name>\n" +
      "        <description>Parameter 2.</description>\n" +
      "        <required>false</required>\n" +
      "    </parameter>\n" +
      "    <instance>\n" +
      "        <name>INSTANCE1</name>\n" +
      "        <label>My Instance 1!</label>\n" +
      "    </instance>\n" +
      "</view>";

  @Test
  public void testReadViewArchives() throws Exception {
    Configuration configuration = createNiceMock(Configuration.class);
    File viewDir = createNiceMock(File.class);
    File extractedArchiveDir = createNiceMock(File.class);
    File viewArchive = createNiceMock(File.class);
    File archiveDir = createNiceMock(File.class);
    File entryFile  = createNiceMock(File.class);
    File classesDir = createNiceMock(File.class);
    File libDir = createNiceMock(File.class);
    File fileEntry = createNiceMock(File.class);

    JarFile viewJarFile = createNiceMock(JarFile.class);
    Enumeration<JarEntry> enumeration = createMock(Enumeration.class);
    JarEntry jarEntry = createNiceMock(JarEntry.class);
    InputStream is = createMock(InputStream.class);
    FileOutputStream fos = createMock(FileOutputStream.class);

    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(10);
    resourceTypeEntity.setName("MY_VIEW{1.0.0}");

    ViewDAO vDAO = createMock(ViewDAO.class);
    ResourceDAO rDAO = createNiceMock(ResourceDAO.class);
    ResourceTypeDAO rtDAO = createNiceMock(ResourceTypeDAO.class);
    ViewInstanceDAO viDAO = createNiceMock(ViewInstanceDAO.class);

    ViewRegistry.setViewDAO(vDAO);
    ViewRegistry.setResourceDAO(rDAO);
    ViewRegistry.setResourceTypeDAO(rtDAO);
    ViewRegistry.setInstanceDAO(viDAO);

    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    viewDefinition.setResourceType(resourceTypeEntity);

    Set<ViewInstanceEntity> viewInstanceEntities = ViewInstanceEntityTest.getViewInstanceEntities(viewDefinition);
    viewDefinition.setInstances(viewInstanceEntities);

    Map<File, ViewConfig> viewConfigs =
        Collections.singletonMap(viewArchive, viewDefinition.getConfiguration());

    long resourceId = 99L;
    for (ViewInstanceEntity viewInstanceEntity : viewInstanceEntities) {
      ResourceEntity resourceEntity = new ResourceEntity();
      resourceEntity.setId(resourceId);
      resourceEntity.setResourceType(resourceTypeEntity);
      viewInstanceEntity.setResource(resourceEntity);
    }

    Map<String, File> files = new HashMap<String, File>();

    files.put("/var/lib/ambari-server/resources/views/work", extractedArchiveDir);
    files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}", archiveDir);
    files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/view.xml", entryFile);
    files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/WEB-INF/classes", classesDir);
    files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/WEB-INF/lib", libDir);

    Map<File, FileOutputStream> outputStreams = new HashMap<File, FileOutputStream>();
    outputStreams.put(entryFile, fos);

    Map<File, JarFile> jarFiles = new HashMap<File, JarFile>();
    jarFiles.put(viewArchive, viewJarFile);

    // set expectations
    expect(configuration.getViewsDir()).andReturn(viewDir);
    expect(viewDir.getAbsolutePath()).andReturn("/var/lib/ambari-server/resources/views");

    expect(viewDir.listFiles()).andReturn(new File[]{viewArchive});

    expect(viewArchive.isDirectory()).andReturn(false);
    expect(viewArchive.getAbsolutePath()).andReturn("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}").anyTimes();

    expect(archiveDir.exists()).andReturn(false);
    expect(archiveDir.getAbsolutePath()).andReturn(
        "/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}").anyTimes();
    expect(archiveDir.mkdir()).andReturn(true);
    expect(archiveDir.toURI()).andReturn(new URI("file:./"));

    expect(viewJarFile.entries()).andReturn(enumeration);
    expect(viewJarFile.getInputStream(jarEntry)).andReturn(is);

    expect(enumeration.hasMoreElements()).andReturn(true);
    expect(enumeration.hasMoreElements()).andReturn(false);
    expect(enumeration.nextElement()).andReturn(jarEntry);

    expect(jarEntry.getName()).andReturn("view.xml");
    expect(jarEntry.isDirectory()).andReturn(false);

    expect(is.available()).andReturn(1);
    expect(is.available()).andReturn(0);

    expect(is.read()).andReturn(10);
    fos.write(10);

    fos.close();
    is.close();

    expect(extractedArchiveDir.exists()).andReturn(false);
    expect(extractedArchiveDir.mkdir()).andReturn(true);

    expect(classesDir.exists()).andReturn(true);
    expect(classesDir.toURI()).andReturn(new URI("file:./"));

    expect(libDir.exists()).andReturn(true);

    expect(libDir.listFiles()).andReturn(new File[]{fileEntry});
    expect(fileEntry.toURI()).andReturn(new URI("file:./"));

    expect(vDAO.findByName("MY_VIEW{1.0.0}")).andReturn(viewDefinition);

    expect(vDAO.findAll()).andReturn(Collections.<ViewEntity>emptyList());

    expect(rtDAO.findByName("MY_VIEW{1.0.0}")).andReturn(null);
    rtDAO.create(EasyMock.anyObject(ResourceTypeEntity.class));
    EasyMock.expectLastCall().anyTimes();

    expect(viDAO.merge(EasyMock.anyObject(ViewInstanceEntity.class))).andReturn(null).times(2);

    // replay mocks
    replay(configuration, viewDir, extractedArchiveDir, viewArchive, archiveDir, entryFile, classesDir,
        libDir, fileEntry, viewJarFile, enumeration, jarEntry, is, fos, rDAO, rtDAO, vDAO, viDAO);

    ViewRegistry registry = ViewRegistry.getInstance();
    registry.setHelper(new TestViewRegistryHelper(viewConfigs, files, outputStreams, jarFiles));

    Set<ViewInstanceEntity> instanceEntities = registry.readViewArchives(configuration);

    Assert.assertEquals(2, instanceEntities.size());

    // verify mocks
    verify(configuration, viewDir, extractedArchiveDir, viewArchive, archiveDir, entryFile, classesDir,
        libDir, fileEntry, viewJarFile, enumeration, jarEntry, is, fos, rDAO, rtDAO, vDAO, viDAO);
  }

  @Test
  public void testReadViewArchives_exception() throws Exception {
    Configuration configuration = createNiceMock(Configuration.class);
    File viewDir = createNiceMock(File.class);
    File extractedArchiveDir = createNiceMock(File.class);
    File viewArchive = createNiceMock(File.class);
    File archiveDir = createNiceMock(File.class);
    File entryFile  = createNiceMock(File.class);
    File classesDir = createNiceMock(File.class);
    File libDir = createNiceMock(File.class);
    File fileEntry = createNiceMock(File.class);

    JarFile viewJarFile = createNiceMock(JarFile.class);
    Enumeration<JarEntry> enumeration = createMock(Enumeration.class);
    JarEntry jarEntry = createNiceMock(JarEntry.class);
    InputStream is = createMock(InputStream.class);
    FileOutputStream fos = createMock(FileOutputStream.class);

    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(10);
    resourceTypeEntity.setName("MY_VIEW{1.0.0}");

    ViewDAO vDAO = createMock(ViewDAO.class);

    ViewRegistry.setViewDAO(vDAO);

    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    viewDefinition.setResourceType(resourceTypeEntity);

    Set<ViewInstanceEntity> viewInstanceEntities = ViewInstanceEntityTest.getViewInstanceEntities(viewDefinition);
    viewDefinition.setInstances(viewInstanceEntities);

    Map<File, ViewConfig> viewConfigs =
        Collections.singletonMap(viewArchive, viewDefinition.getConfiguration());

    long resourceId = 99L;
    for (ViewInstanceEntity viewInstanceEntity : viewInstanceEntities) {
      ResourceEntity resourceEntity = new ResourceEntity();
      resourceEntity.setId(resourceId);
      resourceEntity.setResourceType(resourceTypeEntity);
      viewInstanceEntity.setResource(resourceEntity);
    }

    Map<String, File> files = new HashMap<String, File>();

    files.put("/var/lib/ambari-server/resources/views/work", extractedArchiveDir);
    files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}", archiveDir);
    files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/view.xml", entryFile);
    files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/WEB-INF/classes", classesDir);
    files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/WEB-INF/lib", libDir);

    Map<File, FileOutputStream> outputStreams = new HashMap<File, FileOutputStream>();
    outputStreams.put(entryFile, fos);

    Map<File, JarFile> jarFiles = new HashMap<File, JarFile>();
    jarFiles.put(viewArchive, viewJarFile);

    // set expectations
    expect(configuration.getViewsDir()).andReturn(viewDir);
    expect(viewDir.getAbsolutePath()).andReturn("/var/lib/ambari-server/resources/views");

    expect(viewDir.listFiles()).andReturn(new File[]{viewArchive});

    expect(viewArchive.isDirectory()).andReturn(false);
    expect(viewArchive.getAbsolutePath()).andReturn("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}");

    expect(archiveDir.exists()).andReturn(false);
    expect(archiveDir.getAbsolutePath()).andReturn(
        "/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}").anyTimes();
    expect(archiveDir.mkdir()).andReturn(true);
    expect(archiveDir.toURI()).andReturn(new URI("file:./"));

    expect(viewJarFile.entries()).andReturn(enumeration);
    expect(viewJarFile.getInputStream(jarEntry)).andReturn(is);

    expect(enumeration.hasMoreElements()).andReturn(true);
    expect(enumeration.hasMoreElements()).andReturn(false);
    expect(enumeration.nextElement()).andReturn(jarEntry);

    expect(jarEntry.getName()).andReturn("view.xml");
    expect(jarEntry.isDirectory()).andReturn(false);

    expect(is.available()).andReturn(1);
    expect(is.available()).andReturn(0);

    expect(is.read()).andReturn(10);
    fos.write(10);

    fos.close();
    is.close();

    expect(extractedArchiveDir.exists()).andReturn(false);
    expect(extractedArchiveDir.mkdir()).andReturn(true);

    expect(classesDir.exists()).andReturn(true);
    expect(classesDir.toURI()).andReturn(new URI("file:./"));

    expect(libDir.exists()).andReturn(true);

    expect(libDir.listFiles()).andReturn(new File[]{fileEntry});
    expect(fileEntry.toURI()).andReturn(new URI("file:./"));

    expect(vDAO.findAll()).andReturn(Collections.<ViewEntity>emptyList());

    // replay mocks
    replay(configuration, viewDir, extractedArchiveDir, viewArchive, archiveDir, entryFile, classesDir,
        libDir, fileEntry, viewJarFile, enumeration, jarEntry, is, fos, vDAO);

    ViewRegistry registry = ViewRegistry.getInstance();
    registry.setHelper(new TestViewRegistryHelper(viewConfigs, files, outputStreams, jarFiles));

    Set<ViewInstanceEntity> instanceEntities = registry.readViewArchives(configuration);

    Assert.assertEquals(0, instanceEntities.size());

    // verify mocks
    verify(configuration, viewDir, extractedArchiveDir, viewArchive, archiveDir, entryFile, classesDir,
        libDir, fileEntry, viewJarFile, enumeration, jarEntry, is, fos, vDAO);
  }

  @Test
  public void testListener() throws Exception {
    ViewRegistry registry = ViewRegistry.getInstance();

    TestListener listener = new TestListener();
    registry.registerListener(listener, "MY_VIEW", "1.0.0");

    EventImpl event = EventImplTest.getEvent("MyEvent", Collections.<String, String>emptyMap(), view_xml1);

    registry.fireEvent(event);

    Assert.assertEquals(event, listener.getLastEvent());

    listener.clear();

    event = EventImplTest.getEvent("MyEvent", Collections.<String, String>emptyMap(), view_xml2);

    registry.fireEvent(event);

    Assert.assertNull(listener.getLastEvent());
  }

  @Test
  public void testAddGetDefinitions() throws Exception {
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();

    ViewRegistry registry = ViewRegistry.getInstance();

    registry.addDefinition(viewDefinition);

    Assert.assertEquals(viewDefinition, registry.getDefinition("MY_VIEW", "1.0.0"));

    Collection<ViewEntity> viewDefinitions = registry.getDefinitions();

    Assert.assertEquals(1, viewDefinitions.size());

    Assert.assertEquals(viewDefinition, viewDefinitions.iterator().next());
  }

  @Test
  public void testAddGetInstanceDefinitions() throws Exception {
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = ViewInstanceEntityTest.getViewInstanceEntity();

    ViewRegistry registry = ViewRegistry.getInstance();

    registry.addDefinition(viewDefinition);

    registry.addInstanceDefinition(viewDefinition, viewInstanceDefinition);

    Assert.assertEquals(viewInstanceDefinition, registry.getInstanceDefinition("MY_VIEW", "1.0.0", "INSTANCE1"));

    Collection<ViewInstanceEntity> viewInstanceDefinitions = registry.getInstanceDefinitions(viewDefinition);

    Assert.assertEquals(1, viewInstanceDefinitions.size());

    Assert.assertEquals(viewInstanceDefinition, viewInstanceDefinitions.iterator().next());
  }

  @Test
  public void testGetSubResourceDefinitions() throws Exception {
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewRegistry registry = ViewRegistry.getInstance();

    ResourceConfig config = ResourceConfigTest.getResourceConfigs().get(0);
    Resource.Type type1 = new Resource.Type("myType");

    ResourceProvider provider1 = createNiceMock(ResourceProvider.class);
    viewDefinition.addResourceProvider(type1, provider1);

    viewDefinition.addResourceConfiguration(type1, config);
    registry.addDefinition(viewDefinition);
    Set<SubResourceDefinition> subResourceDefinitions = registry.getSubResourceDefinitions("MY_VIEW", "1.0.0");

    Assert.assertEquals(1, subResourceDefinitions.size());
    Assert.assertEquals("myType", subResourceDefinitions.iterator().next().getType().name());
  }

  @Test
  public void testAddInstanceDefinition() throws Exception {
    ViewRegistry registry = ViewRegistry.getInstance();

    ViewEntity viewEntity = ViewEntityTest.getViewEntity();
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);

    ViewInstanceEntity viewInstanceEntity = new ViewInstanceEntity(viewEntity, instanceConfig);

    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(10);
    resourceTypeEntity.setName(viewEntity.getName());

    viewEntity.setResourceType(resourceTypeEntity);

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setId(20L);
    resourceEntity.setResourceType(resourceTypeEntity);
    viewInstanceEntity.setResource(resourceEntity);

    registry.addDefinition(viewEntity);
    registry.addInstanceDefinition(viewEntity, viewInstanceEntity);

    Collection<ViewInstanceEntity> viewInstanceDefinitions = registry.getInstanceDefinitions(viewEntity);

    Assert.assertEquals(1, viewInstanceDefinitions.size());

    Assert.assertEquals(viewInstanceEntity, viewInstanceDefinitions.iterator().next());
  }

  @Test
  public void testInstallViewInstance() throws Exception {

    ViewDAO viewDAO = createNiceMock(ViewDAO.class);
    ViewInstanceDAO viewInstanceDAO = createNiceMock(ViewInstanceDAO.class);
    UserDAO userDAO = createNiceMock(UserDAO.class);
    MemberDAO memberDAO = createNiceMock(MemberDAO.class);
    PrivilegeDAO privilegeDAO = createNiceMock(PrivilegeDAO.class);
    SecurityHelper securityHelper = createNiceMock(SecurityHelper.class);
    ResourceDAO rDAO = createNiceMock(ResourceDAO.class);
    ResourceTypeDAO rtDAO = createNiceMock(ResourceTypeDAO.class);

    ViewRegistry.init(viewDAO, viewInstanceDAO, userDAO, memberDAO, privilegeDAO, securityHelper, rDAO, rtDAO);

    ViewRegistry registry = ViewRegistry.getInstance();

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(xml_valid_instance);
    ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));

    expect(viewInstanceDAO.merge(viewInstanceEntity)).andReturn(null);
    expect(viewInstanceDAO.findByName("MY_VIEW{1.0.0}", viewInstanceEntity.getInstanceName())).andReturn(viewInstanceEntity);

    replay(viewDAO, viewInstanceDAO, securityHelper);

    registry.addDefinition(viewEntity);
    registry.installViewInstance(viewInstanceEntity);

    Collection<ViewInstanceEntity> viewInstanceDefinitions = registry.getInstanceDefinitions(viewEntity);

    Assert.assertEquals(1, viewInstanceDefinitions.size());

    Assert.assertEquals(viewInstanceEntity, viewInstanceDefinitions.iterator().next());

    verify(viewDAO, viewInstanceDAO, securityHelper);
  }

  @Test
  public void testInstallViewInstance_invalid() throws Exception {

    ViewDAO viewDAO = createNiceMock(ViewDAO.class);
    ViewInstanceDAO viewInstanceDAO = createNiceMock(ViewInstanceDAO.class);
    UserDAO userDAO = createNiceMock(UserDAO.class);
    MemberDAO memberDAO = createNiceMock(MemberDAO.class);
    PrivilegeDAO privilegeDAO = createNiceMock(PrivilegeDAO.class);
    SecurityHelper securityHelper = createNiceMock(SecurityHelper.class);
    ResourceDAO rDAO = createNiceMock(ResourceDAO.class);
    ResourceTypeDAO rtDAO = createNiceMock(ResourceTypeDAO.class);

    ViewRegistry.init(viewDAO, viewInstanceDAO, userDAO, memberDAO, privilegeDAO, securityHelper, rDAO, rtDAO);

    ViewRegistry registry = ViewRegistry.getInstance();

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(xml_invalid_instance);
    ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));

    replay(viewDAO, viewInstanceDAO, securityHelper);

    registry.addDefinition(viewEntity);
    try {
      registry.installViewInstance(viewInstanceEntity);
      Assert.fail("expected an IllegalStateException");
    } catch (IllegalStateException e) {
      // expected
    }
    verify(viewDAO, viewInstanceDAO, securityHelper);
  }

  @Test
  public void testInstallViewInstance_unknownView() throws Exception {

    ViewDAO viewDAO = createNiceMock(ViewDAO.class);
    ViewInstanceDAO viewInstanceDAO = createNiceMock(ViewInstanceDAO.class);
    UserDAO userDAO = createNiceMock(UserDAO.class);
    MemberDAO memberDAO = createNiceMock(MemberDAO.class);
    PrivilegeDAO privilegeDAO = createNiceMock(PrivilegeDAO.class);
    SecurityHelper securityHelper = createNiceMock(SecurityHelper.class);
    ResourceDAO rDAO = createNiceMock(ResourceDAO.class);
    ResourceTypeDAO rtDAO = createNiceMock(ResourceTypeDAO.class);

    ViewRegistry.init(viewDAO, viewInstanceDAO, userDAO, memberDAO, privilegeDAO, securityHelper, rDAO, rtDAO);

    ViewRegistry registry = ViewRegistry.getInstance();

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(xml_valid_instance);
    ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));
    viewInstanceEntity.setViewName("BOGUS_VIEW");

    replay(viewDAO, viewInstanceDAO, securityHelper);

    registry.addDefinition(viewEntity);
    try {
      registry.installViewInstance(viewInstanceEntity);
      Assert.fail("expected an IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
    verify(viewDAO, viewInstanceDAO, securityHelper);
  }

  @Test
  public void testUpdateViewInstance() throws Exception {

    ViewDAO viewDAO = createNiceMock(ViewDAO.class);
    ViewInstanceDAO viewInstanceDAO = createNiceMock(ViewInstanceDAO.class);
    UserDAO userDAO = createNiceMock(UserDAO.class);
    MemberDAO memberDAO = createNiceMock(MemberDAO.class);
    PrivilegeDAO privilegeDAO = createNiceMock(PrivilegeDAO.class);
    SecurityHelper securityHelper = createNiceMock(SecurityHelper.class);
    ResourceDAO rDAO = createNiceMock(ResourceDAO.class);
    ResourceTypeDAO rtDAO = createNiceMock(ResourceTypeDAO.class);

    ViewRegistry.init(viewDAO, viewInstanceDAO, userDAO, memberDAO, privilegeDAO, securityHelper, rDAO, rtDAO);

    ViewRegistry registry = ViewRegistry.getInstance();

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(xml_valid_instance);
    ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));
    ViewInstanceEntity updateInstance = getViewInstanceEntity(viewEntity, config.getInstances().get(0));

    expect(viewInstanceDAO.merge(viewInstanceEntity)).andReturn(null);
    expect(viewInstanceDAO.merge(viewInstanceEntity)).andReturn(viewInstanceEntity);
    expect(viewInstanceDAO.findByName("MY_VIEW{1.0.0}", viewInstanceEntity.getInstanceName())).andReturn(viewInstanceEntity);

    replay(viewDAO, viewInstanceDAO, securityHelper);

    registry.addDefinition(viewEntity);
    registry.installViewInstance(viewInstanceEntity);

    registry.updateViewInstance(updateInstance);

    Collection<ViewInstanceEntity> viewInstanceDefinitions = registry.getInstanceDefinitions(viewEntity);

    Assert.assertEquals(1, viewInstanceDefinitions.size());

    Assert.assertEquals(viewInstanceEntity, viewInstanceDefinitions.iterator().next());

    verify(viewDAO, viewInstanceDAO, securityHelper);
  }

  @Test
  public void testUpdateViewInstance_invalid() throws Exception {

    ViewDAO viewDAO = createNiceMock(ViewDAO.class);
    ViewInstanceDAO viewInstanceDAO = createNiceMock(ViewInstanceDAO.class);
    UserDAO userDAO = createNiceMock(UserDAO.class);
    MemberDAO memberDAO = createNiceMock(MemberDAO.class);
    PrivilegeDAO privilegeDAO = createNiceMock(PrivilegeDAO.class);
    SecurityHelper securityHelper = createNiceMock(SecurityHelper.class);
    ResourceDAO rDAO = createNiceMock(ResourceDAO.class);
    ResourceTypeDAO rtDAO = createNiceMock(ResourceTypeDAO.class);

    ViewRegistry.init(viewDAO, viewInstanceDAO, userDAO, memberDAO, privilegeDAO, securityHelper, rDAO, rtDAO);

    ViewRegistry registry = ViewRegistry.getInstance();

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(xml_valid_instance);
    ViewConfig invalidConfig = ViewConfigTest.getConfig(xml_invalid_instance);
    ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));
    ViewInstanceEntity updateInstance = getViewInstanceEntity(viewEntity, invalidConfig.getInstances().get(0));

    expect(viewInstanceDAO.merge(viewInstanceEntity)).andReturn(null);
    expect(viewInstanceDAO.findByName("MY_VIEW{1.0.0}", viewInstanceEntity.getInstanceName())).andReturn(viewInstanceEntity);

    replay(viewDAO, viewInstanceDAO, securityHelper);

    registry.addDefinition(viewEntity);
    registry.installViewInstance(viewInstanceEntity);

    try {
      registry.updateViewInstance(updateInstance);
      Assert.fail("expected an IllegalStateException");
    } catch (IllegalStateException e) {
      // expected
    }
    verify(viewDAO, viewInstanceDAO, securityHelper);
  }

  @Test
  public void testRemoveInstanceData() throws Exception {

    ViewDAO viewDAO = createNiceMock(ViewDAO.class);
    ViewInstanceDAO viewInstanceDAO = createNiceMock(ViewInstanceDAO.class);
    UserDAO userDAO = createNiceMock(UserDAO.class);
    MemberDAO memberDAO = createNiceMock(MemberDAO.class);
    PrivilegeDAO privilegeDAO = createNiceMock(PrivilegeDAO.class);
    SecurityHelper securityHelper = createNiceMock(SecurityHelper.class);
    ResourceDAO rDAO = createNiceMock(ResourceDAO.class);
    ResourceTypeDAO rtDAO = createNiceMock(ResourceTypeDAO.class);

    ViewRegistry.init(viewDAO, viewInstanceDAO, userDAO, memberDAO, privilegeDAO, securityHelper, rDAO, rtDAO);

    ViewRegistry registry = ViewRegistry.getInstance();

    ViewInstanceEntity viewInstanceEntity = ViewInstanceEntityTest.getViewInstanceEntity();

    viewInstanceEntity.putInstanceData("foo", "value");

    ViewInstanceDataEntity dataEntity = viewInstanceEntity.getInstanceData("foo");

    viewInstanceDAO.removeData(dataEntity);
    expect(viewInstanceDAO.merge(viewInstanceEntity)).andReturn(viewInstanceEntity);
    replay(viewDAO, viewInstanceDAO, securityHelper);

    registry.removeInstanceData(viewInstanceEntity, "foo");

    Assert.assertNull(viewInstanceEntity.getInstanceData("foo"));
    verify(viewDAO, viewInstanceDAO, securityHelper);
  }

  @Test
  public void testIncludeDefinitionForAdmin() {
    ViewRegistry viewRegistry = ViewRegistry.getInstance();
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);
    SecurityHelper securityHelper = createNiceMock(SecurityHelper.class);
    AmbariGrantedAuthority adminAuthority = createNiceMock(AmbariGrantedAuthority.class);
    PrivilegeEntity privilegeEntity = createNiceMock(PrivilegeEntity.class);
    PermissionEntity permissionEntity = createNiceMock(PermissionEntity.class);

    viewRegistry.setSecurityHelper(securityHelper);

    Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
    authorities.add(adminAuthority);

    securityHelper.getCurrentAuthorities();
    EasyMock.expectLastCall().andReturn(authorities);
    expect(adminAuthority.getPrivilegeEntity()).andReturn(privilegeEntity);
    expect(privilegeEntity.getPermission()).andReturn(permissionEntity);
    expect(permissionEntity.getId()).andReturn(PermissionEntity.AMBARI_ADMIN_PERMISSION);
    replay(securityHelper, adminAuthority, privilegeEntity, permissionEntity);

    Assert.assertTrue(viewRegistry.includeDefinition(viewEntity));

    verify(securityHelper, adminAuthority, privilegeEntity, permissionEntity);
  }

  @Test
  public void testIncludeDefinitionForUserNoInstances() {
    ViewRegistry viewRegistry = ViewRegistry.getInstance();
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);
    SecurityHelper securityHelper = createNiceMock(SecurityHelper.class);

    viewRegistry.setSecurityHelper(securityHelper);

    Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

    Collection<ViewInstanceEntity> instances = new ArrayList<ViewInstanceEntity>();

    securityHelper.getCurrentAuthorities();
    EasyMock.expectLastCall().andReturn(authorities);
    expect(viewEntity.getInstances()).andReturn(instances);
    replay(securityHelper, viewEntity);

    Assert.assertFalse(viewRegistry.includeDefinition(viewEntity));

    verify(securityHelper, viewEntity);
  }

  @Test
  public void testIncludeDefinitionForUserHasAccess() {
    ViewRegistry viewRegistry = ViewRegistry.getInstance();
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);
    SecurityHelper securityHelper = createNiceMock(SecurityHelper.class);
    ViewInstanceEntity instanceEntity = createNiceMock(ViewInstanceEntity.class);
    ResourceEntity resourceEntity = createNiceMock(ResourceEntity.class);
    AmbariGrantedAuthority viewUseAuthority = createNiceMock(AmbariGrantedAuthority.class);
    PrivilegeEntity privilegeEntity = createNiceMock(PrivilegeEntity.class);
    PermissionEntity permissionEntity = createNiceMock(PermissionEntity.class);

    viewRegistry.setSecurityHelper(securityHelper);

    Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
    authorities.add(viewUseAuthority);

    Collection<ViewInstanceEntity> instances = new ArrayList<ViewInstanceEntity>();
    instances.add(instanceEntity);

    expect(viewEntity.getInstances()).andReturn(instances);
    expect(instanceEntity.getResource()).andReturn(resourceEntity);
    expect(viewUseAuthority.getPrivilegeEntity()).andReturn(privilegeEntity).anyTimes();
    expect(privilegeEntity.getPermission()).andReturn(permissionEntity).anyTimes();
    expect(privilegeEntity.getResource()).andReturn(resourceEntity).anyTimes();
    expect(permissionEntity.getId()).andReturn(PermissionEntity.VIEW_USE_PERMISSION).anyTimes();
    securityHelper.getCurrentAuthorities();
    EasyMock.expectLastCall().andReturn(authorities).anyTimes();
    replay(securityHelper, viewEntity, instanceEntity, viewUseAuthority, privilegeEntity, permissionEntity);

    Assert.assertTrue(viewRegistry.includeDefinition(viewEntity));

    verify(securityHelper, viewEntity, instanceEntity, viewUseAuthority, privilegeEntity, permissionEntity);
  }

  @Before
  public void before() throws Exception {
    clear();
  }

  @AfterClass
  public static void afterClass() {
    clear();
  }

  public static void clear() {
    ViewRegistry.getInstance().clear();
    ViewRegistry.setViewDAO(null);
  }

  public class TestViewRegistryHelper extends ViewRegistry.ViewRegistryHelper {
    private final Map<File, ViewConfig> viewConfigs;
    private final Map<String, File> files;
    private final Map<File, FileOutputStream> outputStreams;
    private final Map<File, JarFile> jarFiles;

    public TestViewRegistryHelper(Map<File, ViewConfig> viewConfigs, Map<String, File> files, Map<File,
        FileOutputStream> outputStreams, Map<File, JarFile> jarFiles) {
      this.viewConfigs = viewConfigs;
      this.files = files;
      this.outputStreams = outputStreams;
      this.jarFiles = jarFiles;
    }

    @Override
    public ViewConfig getViewConfigFromArchive(File archiveFile) throws MalformedURLException, JAXBException {
      return viewConfigs.get(archiveFile);
    }

    public ViewConfig getViewConfigFromExtractedArchive(String archivePath)
        throws JAXBException, FileNotFoundException {
      for (File viewConfigKey: viewConfigs.keySet()) {
        if (viewConfigKey.getAbsolutePath().equals(archivePath)) {
          return viewConfigs.get(viewConfigKey);
        }
      }
      return null;
    }

    @Override
    public File getFile(String path) {
      return files.get(path);
    }

    @Override
    public FileOutputStream getFileOutputStream(File file) throws FileNotFoundException {
      return outputStreams.get(file);
    }

    @Override
    public JarFile getJarFile(File file) throws IOException {
      return jarFiles.get(file);
    }
  }

  private static class TestListener implements Listener {
    private Event lastEvent = null;

    @Override
    public void notify(Event event) {
      lastEvent = event;
    }

    public Event getLastEvent() {
      return lastEvent;
    }

    public void clear() {
      lastEvent = null;
    }
  }

  public static ViewEntity getViewEntity(ViewConfig viewConfig, Configuration ambariConfig,
                                     ClassLoader cl, String archivePath) throws Exception{
    ViewRegistry registry = ViewRegistry.getInstance();

    return registry.createViewDefinition(viewConfig, ambariConfig, cl, archivePath);
  }

  public static ViewInstanceEntity getViewInstanceEntity(ViewEntity viewDefinition, InstanceConfig instanceConfig) throws Exception {
    ViewRegistry registry = ViewRegistry.getInstance();

    ViewInstanceEntity viewInstanceDefinition =
        new ViewInstanceEntity(viewDefinition, instanceConfig);

    for (PropertyConfig propertyConfig : instanceConfig.getProperties()) {
      viewInstanceDefinition.putProperty(propertyConfig.getKey(), propertyConfig.getValue());
    }

    registry.bindViewInstance(viewDefinition, viewInstanceDefinition);
    return viewInstanceDefinition;
  }
}
