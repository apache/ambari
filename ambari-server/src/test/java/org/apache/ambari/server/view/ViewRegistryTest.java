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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

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
import org.apache.ambari.server.orm.entities.PrincipalEntity;
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
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.apache.ambari.server.view.configuration.ViewConfigTest;
import org.apache.ambari.server.view.events.EventImpl;
import org.apache.ambari.server.view.events.EventImplTest;
import org.apache.ambari.view.ViewDefinition;
import org.apache.ambari.view.events.Event;
import org.apache.ambari.view.events.Listener;
import org.easymock.Capture;
import org.easymock.EasyMock;
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
      "        <masked>true</masked>" +
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

  // registry mocks
  private static final ViewDAO viewDAO = createMock(ViewDAO.class);
  private static final ViewInstanceDAO viewInstanceDAO = createNiceMock(ViewInstanceDAO.class);
  private static final UserDAO userDAO = createNiceMock(UserDAO.class);
  private static final MemberDAO memberDAO = createNiceMock(MemberDAO.class);
  private static final PrivilegeDAO privilegeDAO = createNiceMock(PrivilegeDAO.class);
  private static final ResourceDAO resourceDAO = createNiceMock(ResourceDAO.class);
  private static final ResourceTypeDAO resourceTypeDAO = createNiceMock(ResourceTypeDAO.class);
  private static final SecurityHelper securityHelper = createNiceMock(SecurityHelper.class);
  private static final Configuration configuration = createNiceMock(Configuration.class);
  private static final ViewInstanceHandlerList handlerList = createNiceMock(ViewInstanceHandlerList.class);


  @Before
  public void resetGlobalMocks() {
    ViewRegistry.initInstance(getRegistry(viewDAO, viewInstanceDAO, userDAO, memberDAO, privilegeDAO,
        resourceDAO, resourceTypeDAO, securityHelper, handlerList, null, null));

    reset(viewDAO, resourceDAO, viewInstanceDAO, userDAO, memberDAO,
        privilegeDAO, resourceTypeDAO, securityHelper, configuration, handlerList);
  }

  @Test
  public void testReadViewArchives() throws Exception {

    File viewDir = createNiceMock(File.class);
    File extractedArchiveDir = createNiceMock(File.class);
    File viewArchive = createNiceMock(File.class);
    File archiveDir = createNiceMock(File.class);
    File entryFile  = createNiceMock(File.class);
    File classesDir = createNiceMock(File.class);
    File libDir = createNiceMock(File.class);
    File metaInfDir = createNiceMock(File.class);
    File fileEntry = createNiceMock(File.class);

    JarInputStream viewJarFile = createNiceMock(JarInputStream.class);
    JarEntry jarEntry = createNiceMock(JarEntry.class);
    FileOutputStream fos = createMock(FileOutputStream.class);

    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(10);
    resourceTypeEntity.setName("MY_VIEW{1.0.0}");

    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    viewDefinition.setResourceType(resourceTypeEntity);

    Set<ViewInstanceEntity> viewInstanceEntities = ViewInstanceEntityTest.getViewInstanceEntities(viewDefinition);

    for (ViewInstanceEntity viewInstanceEntity : viewInstanceEntities) {
      viewInstanceEntity.putInstanceData("p1", "v1");
    }
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
    files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/META-INF", metaInfDir);

    Map<File, FileOutputStream> outputStreams = new HashMap<File, FileOutputStream>();
    outputStreams.put(entryFile, fos);

    Map<File, JarInputStream> jarFiles = new HashMap<File, JarInputStream>();
    jarFiles.put(viewArchive, viewJarFile);

    // set expectations
    expect(configuration.getViewsDir()).andReturn(viewDir);
    expect(viewDir.getAbsolutePath()).andReturn("/var/lib/ambari-server/resources/views");

    expect(configuration.getViewExtractionThreadPoolCoreSize()).andReturn(2).anyTimes();
    expect(configuration.getViewExtractionThreadPoolMaxSize()).andReturn(3).anyTimes();
    expect(configuration.getViewExtractionThreadPoolTimeout()).andReturn(10000L).anyTimes();

    expect(viewDir.listFiles()).andReturn(new File[]{viewArchive});

    expect(viewArchive.isDirectory()).andReturn(false);
    expect(viewArchive.getAbsolutePath()).andReturn("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}").anyTimes();

    expect(archiveDir.exists()).andReturn(false);
    expect(archiveDir.getAbsolutePath()).andReturn(
        "/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}").anyTimes();
    expect(archiveDir.mkdir()).andReturn(true);
    expect(archiveDir.toURI()).andReturn(new URI("file:./"));

    expect(metaInfDir.mkdir()).andReturn(true);

    expect(viewJarFile.getNextJarEntry()).andReturn(jarEntry);
    expect(viewJarFile.getNextJarEntry()).andReturn(null);

    expect(jarEntry.getName()).andReturn("view.xml");
    expect(jarEntry.isDirectory()).andReturn(false);

    expect(viewJarFile.read(anyObject(byte[].class))).andReturn(10);
    expect(viewJarFile.read(anyObject(byte[].class))).andReturn(-1);
    fos.write(anyObject(byte[].class), eq(0), eq(10));

    fos.flush();
    fos.close();
    viewJarFile.closeEntry();
    viewJarFile.close();

    expect(extractedArchiveDir.exists()).andReturn(false);
    expect(extractedArchiveDir.mkdir()).andReturn(true);

    expect(classesDir.exists()).andReturn(true);
    expect(classesDir.toURI()).andReturn(new URI("file:./"));

    expect(libDir.exists()).andReturn(true);

    expect(libDir.listFiles()).andReturn(new File[]{fileEntry});
    expect(fileEntry.toURI()).andReturn(new URI("file:./"));

    expect(viewDAO.findByName("MY_VIEW{1.0.0}")).andReturn(viewDefinition);

    expect(viewDAO.findAll()).andReturn(Collections.<ViewEntity>emptyList());

    // replay mocks
    replay(configuration, viewDir, extractedArchiveDir, viewArchive, archiveDir, entryFile, classesDir,
        libDir, metaInfDir, fileEntry, viewJarFile, jarEntry, fos, resourceDAO, viewDAO, viewInstanceDAO);

    TestViewArchiveUtility archiveUtility = new TestViewArchiveUtility(viewConfigs, files, outputStreams, jarFiles);

    ViewRegistry registry = getRegistry(viewDAO, viewInstanceDAO, userDAO, memberDAO, privilegeDAO,
        resourceDAO, resourceTypeDAO, securityHelper, handlerList, null, archiveUtility);

    registry.readViewArchives();

    ViewEntity view = null;

    // Wait for the view load to complete.
    long timeout = System.currentTimeMillis() + 10000L;
    while ((view == null || !view.getStatus().equals(ViewDefinition.ViewStatus.DEPLOYED))&&
        System.currentTimeMillis() < timeout) {
      view = registry.getDefinition("MY_VIEW", "1.0.0");
    }

    Assert.assertNotNull(view);
    Assert.assertEquals(ViewDefinition.ViewStatus.DEPLOYED, view.getStatus());

    Collection<ViewInstanceEntity> instanceDefinitions = registry.getInstanceDefinitions(view);
    Assert.assertEquals(2, instanceDefinitions.size());

    for (ViewInstanceEntity viewInstanceEntity : instanceDefinitions) {
      Assert.assertEquals("v1", viewInstanceEntity.getInstanceData("p1").getValue());
    }

    // verify mocks
    verify(configuration, viewDir, extractedArchiveDir, viewArchive, archiveDir, entryFile, classesDir,
        libDir, metaInfDir, fileEntry, viewJarFile, jarEntry, fos, resourceDAO, viewDAO, viewInstanceDAO);
  }

  @Test
  public void testReadViewArchives_exception() throws Exception {

    File viewDir = createNiceMock(File.class);
    File extractedArchiveDir = createNiceMock(File.class);
    File viewArchive = createNiceMock(File.class);
    File archiveDir = createNiceMock(File.class);
    File entryFile  = createNiceMock(File.class);
    File classesDir = createNiceMock(File.class);
    File libDir = createNiceMock(File.class);
    File metaInfDir = createNiceMock(File.class);
    File fileEntry = createNiceMock(File.class);

    JarInputStream viewJarFile = createNiceMock(JarInputStream.class);
    JarEntry jarEntry = createNiceMock(JarEntry.class);
    FileOutputStream fos = createMock(FileOutputStream.class);

    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(10);
    resourceTypeEntity.setName("MY_VIEW{1.0.0}");

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
    files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/META-INF", metaInfDir);

    Map<File, FileOutputStream> outputStreams = new HashMap<File, FileOutputStream>();
    outputStreams.put(entryFile, fos);

    Map<File, JarInputStream> jarFiles = new HashMap<File, JarInputStream>();
    jarFiles.put(viewArchive, viewJarFile);

    // set expectations
    expect(configuration.getViewsDir()).andReturn(viewDir);
    expect(viewDir.getAbsolutePath()).andReturn("/var/lib/ambari-server/resources/views");

    expect(configuration.getViewExtractionThreadPoolCoreSize()).andReturn(2).anyTimes();
    expect(configuration.getViewExtractionThreadPoolMaxSize()).andReturn(3).anyTimes();
    expect(configuration.getViewExtractionThreadPoolTimeout()).andReturn(10000L).anyTimes();

    expect(viewDir.listFiles()).andReturn(new File[]{viewArchive});

    expect(viewArchive.isDirectory()).andReturn(false);
    expect(viewArchive.getAbsolutePath()).andReturn("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}");

    expect(archiveDir.exists()).andReturn(false);
    expect(archiveDir.getAbsolutePath()).andReturn(
        "/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}").anyTimes();
    expect(archiveDir.mkdir()).andReturn(true);
    expect(archiveDir.toURI()).andReturn(new URI("file:./"));

    expect(metaInfDir.mkdir()).andReturn(true);

    expect(viewJarFile.getNextJarEntry()).andReturn(jarEntry);
    expect(viewJarFile.getNextJarEntry()).andReturn(null);

    expect(jarEntry.getName()).andReturn("view.xml");
    expect(jarEntry.isDirectory()).andReturn(false);

    expect(viewJarFile.read(anyObject(byte[].class))).andReturn(10);
    expect(viewJarFile.read(anyObject(byte[].class))).andReturn(-1);
    fos.write(anyObject(byte[].class), eq(0), eq(10));

    fos.flush();
    fos.close();
    viewJarFile.closeEntry();
    viewJarFile.close();

    expect(extractedArchiveDir.exists()).andReturn(false);
    expect(extractedArchiveDir.mkdir()).andReturn(true);

    expect(classesDir.exists()).andReturn(true);
    expect(classesDir.toURI()).andReturn(new URI("file:./"));

    expect(libDir.exists()).andReturn(true);

    expect(libDir.listFiles()).andReturn(new File[]{fileEntry});
    expect(fileEntry.toURI()).andReturn(new URI("file:./"));

    expect(viewDAO.findAll()).andReturn(Collections.<ViewEntity>emptyList());
    expect(viewDAO.findByName("MY_VIEW{1.0.0}")).andThrow(new IllegalArgumentException("Expected exception."));

    // replay mocks
    replay(configuration, viewDir, extractedArchiveDir, viewArchive, archiveDir, entryFile, classesDir,
        libDir, metaInfDir, fileEntry, viewJarFile, jarEntry, fos, viewDAO);

    TestViewArchiveUtility archiveUtility = new TestViewArchiveUtility(viewConfigs, files, outputStreams, jarFiles);

    ViewRegistry registry = getRegistry(viewDAO, viewInstanceDAO, userDAO, memberDAO, privilegeDAO,
        resourceDAO, resourceTypeDAO, securityHelper, handlerList, null, archiveUtility);

    registry.readViewArchives();

    ViewEntity view = null;

    // Wait for the view load to complete.
    long timeout = System.currentTimeMillis() + 10000L;
    while ((view == null || !view.getStatus().equals(ViewDefinition.ViewStatus.ERROR))&&
        System.currentTimeMillis() < timeout) {
      view = registry.getDefinition("MY_VIEW", "1.0.0");
    }

    Assert.assertNotNull(view);
    Assert.assertEquals(ViewDefinition.ViewStatus.ERROR, view.getStatus());

    // verify mocks
    verify(configuration, viewDir, extractedArchiveDir, viewArchive, archiveDir, entryFile, classesDir,
        libDir, metaInfDir, fileEntry, viewJarFile, jarEntry, fos, viewDAO);
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

    // fire an event for a different view
    event = EventImplTest.getEvent("MyEvent", Collections.<String, String>emptyMap(), view_xml2);

    registry.fireEvent(event);

    Assert.assertNull(listener.getLastEvent());

    // un-register the listener
    registry.unregisterListener(listener, "MY_VIEW", "1.0.0");

    event = EventImplTest.getEvent("MyEvent", Collections.<String, String>emptyMap(), view_xml1);

    registry.fireEvent(event);

    Assert.assertNull(listener.getLastEvent());
  }

  @Test
  public void testListener_allVersions() throws Exception {
    ViewRegistry registry = ViewRegistry.getInstance();

    TestListener listener = new TestListener();
    registry.registerListener(listener, "MY_VIEW", null); // all versions of MY_VIEW

    EventImpl event = EventImplTest.getEvent("MyEvent", Collections.<String, String>emptyMap(), view_xml1);

    registry.fireEvent(event);

    Assert.assertEquals(event, listener.getLastEvent());

    listener.clear();

    // fire an event for a different view
    event = EventImplTest.getEvent("MyEvent", Collections.<String, String>emptyMap(), view_xml2);

    registry.fireEvent(event);

    Assert.assertEquals(event, listener.getLastEvent());

    listener.clear();

    // un-register the listener
    registry.unregisterListener(listener, "MY_VIEW", null); // all versions of MY_VIEW

    event = EventImplTest.getEvent("MyEvent", Collections.<String, String>emptyMap(), view_xml1);

    registry.fireEvent(event);

    Assert.assertNull(listener.getLastEvent());

    event = EventImplTest.getEvent("MyEvent", Collections.<String, String>emptyMap(), view_xml2);

    registry.fireEvent(event);

    Assert.assertNull(listener.getLastEvent());
  }

  @Test
  public void testGetResourceProviders() throws Exception {

    ViewConfig config = ViewConfigTest.getConfig();

    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();

    ViewRegistry registry = ViewRegistry.getInstance();

    registry.setupViewDefinition(viewDefinition, config, getClass().getClassLoader());

    Map<Resource.Type, ResourceProvider> providerMap = registry.getResourceProviders();

    Assert.assertEquals(3, providerMap.size());

    Assert.assertTrue(providerMap.containsKey(Resource.Type.valueOf("MY_VIEW{1.0.0}/resource")));
    Assert.assertTrue(providerMap.containsKey(Resource.Type.valueOf("MY_VIEW{1.0.0}/subresource")));
    Assert.assertTrue(providerMap.containsKey(Resource.Type.valueOf("MY_VIEW{1.0.0}/resources")));
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
  public void testGetDefinition() throws Exception {
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();

    ViewRegistry registry = ViewRegistry.getInstance();

    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(10);
    resourceTypeEntity.setName(viewDefinition.getName());

    viewDefinition.setResourceType(resourceTypeEntity);

    registry.addDefinition(viewDefinition);

    viewDefinition.setStatus(ViewDefinition.ViewStatus.DEPLOYING);

    Assert.assertNull(registry.getDefinition(resourceTypeEntity));

    viewDefinition.setStatus(ViewDefinition.ViewStatus.DEPLOYED);

    Assert.assertEquals(viewDefinition, registry.getDefinition(resourceTypeEntity));
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
    ViewConfig config = ViewConfigTest.getConfig();

    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();

    ViewRegistry registry = ViewRegistry.getInstance();

    registry.setupViewDefinition(viewDefinition, config, getClass().getClassLoader());

    Set<SubResourceDefinition> subResourceDefinitions =
        registry.getSubResourceDefinitions(viewDefinition.getCommonName(), viewDefinition.getVersion());


    Assert.assertEquals(3, subResourceDefinitions.size());

    Set<String> names = new HashSet<String>();
    for (SubResourceDefinition definition : subResourceDefinitions) {
      names.add(definition.getType().name());
    }

    Assert.assertTrue(names.contains("MY_VIEW{1.0.0}/resources"));
    Assert.assertTrue(names.contains("MY_VIEW{1.0.0}/resource"));
    Assert.assertTrue(names.contains("MY_VIEW{1.0.0}/subresource"));
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

    ViewRegistry registry = ViewRegistry.getInstance();

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(xml_valid_instance);
    ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));

    expect(viewInstanceDAO.merge(viewInstanceEntity)).andReturn(null);
    expect(viewInstanceDAO.findByName("MY_VIEW{1.0.0}", viewInstanceEntity.getInstanceName())).andReturn(viewInstanceEntity);

    handlerList.addViewInstance(viewInstanceEntity);

    replay(viewDAO, viewInstanceDAO, securityHelper, handlerList);

    registry.addDefinition(viewEntity);
    registry.installViewInstance(viewInstanceEntity);

    Collection<ViewInstanceEntity> viewInstanceDefinitions = registry.getInstanceDefinitions(viewEntity);

    Assert.assertEquals(1, viewInstanceDefinitions.size());

    ViewInstanceEntity instanceEntity = viewInstanceDefinitions.iterator().next();
    Assert.assertEquals("v2-1", instanceEntity.getProperty("p2").getValue() );

    Assert.assertEquals(viewInstanceEntity, viewInstanceDefinitions.iterator().next());

    verify(viewDAO, viewInstanceDAO, securityHelper, handlerList);
  }

  @Test
  public void testInstallViewInstance_invalid() throws Exception {

    ViewRegistry registry = ViewRegistry.getInstance();

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(xml_invalid_instance);
    ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));

    replay(viewDAO, viewInstanceDAO, securityHelper, resourceTypeDAO);

    registry.addDefinition(viewEntity);
    try {
      registry.installViewInstance(viewInstanceEntity);
      Assert.fail("expected an IllegalStateException");
    } catch (IllegalStateException e) {
      // expected
    }
    verify(viewDAO, viewInstanceDAO, securityHelper, resourceTypeDAO);
  }

  @Test
  public void testInstallViewInstance_unknownView() throws Exception {

    ViewRegistry registry = ViewRegistry.getInstance();

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(xml_valid_instance);
    ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));
    viewInstanceEntity.setViewName("BOGUS_VIEW");

    replay(viewDAO, viewInstanceDAO, securityHelper, resourceTypeDAO);

    registry.addDefinition(viewEntity);
    try {
      registry.installViewInstance(viewInstanceEntity);
      Assert.fail("expected an IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
    verify(viewDAO, viewInstanceDAO, securityHelper, resourceTypeDAO);
  }

  @Test
  public void testUpdateViewInstance() throws Exception {

    ViewRegistry registry = ViewRegistry.getInstance();

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(xml_valid_instance);
    ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));
    ViewInstanceEntity updateInstance = getViewInstanceEntity(viewEntity, config.getInstances().get(0));

    expect(viewInstanceDAO.merge(viewInstanceEntity)).andReturn(viewInstanceEntity);
    expect(viewInstanceDAO.findByName("MY_VIEW{1.0.0}", viewInstanceEntity.getInstanceName())).andReturn(viewInstanceEntity);

    replay(viewDAO, viewInstanceDAO, securityHelper);

    registry.addDefinition(viewEntity);
    registry.installViewInstance(viewInstanceEntity);

    registry.updateViewInstance(updateInstance);

    Collection<ViewInstanceEntity> viewInstanceDefinitions = registry.getInstanceDefinitions(viewEntity);

    Assert.assertEquals(1, viewInstanceDefinitions.size());

    ViewInstanceEntity instanceEntity = viewInstanceDefinitions.iterator().next();
    Assert.assertEquals("v2-1", instanceEntity.getProperty("p2").getValue() );

    Assert.assertEquals(viewInstanceEntity, viewInstanceDefinitions.iterator().next());

    verify(viewDAO, viewInstanceDAO, securityHelper);
  }

  @Test
  public void testSetViewInstanceProperties() throws Exception {

    ViewRegistry registry = ViewRegistry.getInstance();

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(xml_valid_instance);
    ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));


    Map<String, String> instanceProperties = new HashMap<String, String>();
    instanceProperties.put("p1", "newV1");
    instanceProperties.put("p2", "newV2");

    registry.setViewInstanceProperties(viewInstanceEntity, instanceProperties, viewEntity.getConfiguration(), viewEntity.getClassLoader());

    Assert.assertEquals("newV1", viewInstanceEntity.getProperty("p1").getValue());
    Assert.assertEquals("bmV3VjI=", viewInstanceEntity.getProperty("p2").getValue());
  }

  @Test
  public void testUninstallViewInstance() throws Exception {

    ViewRegistry registry = ViewRegistry.getInstance();

    Configuration ambariConfig = new Configuration(new Properties());

    ViewConfig config = ViewConfigTest.getConfig(xml_valid_instance);
    ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));
    ResourceEntity resource = new ResourceEntity();
    resource.setId(3L);
    viewInstanceEntity.setResource(resource);
    PrivilegeEntity privilege1 = createNiceMock(PrivilegeEntity.class);
    PrivilegeEntity privilege2 = createNiceMock(PrivilegeEntity.class);
    List<PrivilegeEntity> privileges = Arrays.asList(privilege1, privilege2);

    PrincipalEntity principalEntity = createNiceMock(PrincipalEntity.class);

    expect(privilege1.getPrincipal()).andReturn(principalEntity);
    expect(privilege2.getPrincipal()).andReturn(principalEntity);

    principalEntity.removePrivilege(privilege1);
    principalEntity.removePrivilege(privilege2);

    expect(privilegeDAO.findByResourceId(3L)).andReturn(privileges);
    privilegeDAO.remove(privilege1);
    privilegeDAO.remove(privilege2);
    viewInstanceDAO.remove(viewInstanceEntity);

    handlerList.removeViewInstance(viewInstanceEntity);

    replay(viewInstanceDAO, privilegeDAO, handlerList, privilege1, privilege2, principalEntity);

    registry.addDefinition(viewEntity);
    registry.addInstanceDefinition(viewEntity, viewInstanceEntity);
    registry.uninstallViewInstance(viewInstanceEntity);

    Collection<ViewInstanceEntity> viewInstanceDefinitions = registry.getInstanceDefinitions(viewEntity);

    Assert.assertEquals(0, viewInstanceDefinitions.size());

    verify(viewInstanceDAO, privilegeDAO, handlerList, privilege1, privilege2, principalEntity);
  }

  @Test
  public void testUpdateViewInstance_invalid() throws Exception {

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
    ViewRegistry registry = ViewRegistry.getInstance();
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);
    AmbariGrantedAuthority adminAuthority = createNiceMock(AmbariGrantedAuthority.class);
    PrivilegeEntity privilegeEntity = createNiceMock(PrivilegeEntity.class);
    PermissionEntity permissionEntity = createNiceMock(PermissionEntity.class);

    Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
    authorities.add(adminAuthority);

    securityHelper.getCurrentAuthorities();
    EasyMock.expectLastCall().andReturn(authorities);
    expect(adminAuthority.getPrivilegeEntity()).andReturn(privilegeEntity);
    expect(privilegeEntity.getPermission()).andReturn(permissionEntity);
    expect(permissionEntity.getId()).andReturn(PermissionEntity.AMBARI_ADMIN_PERMISSION);

    expect(configuration.getApiAuthentication()).andReturn(true);
    replay(securityHelper, adminAuthority, privilegeEntity, permissionEntity, configuration);

    Assert.assertTrue(registry.includeDefinition(viewEntity));

    verify(securityHelper, adminAuthority, privilegeEntity, permissionEntity, configuration);
  }

  @Test
  public void testIncludeDefinitionForUserNoInstances() {
    ViewRegistry registry = ViewRegistry.getInstance();
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);

    Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

    Collection<ViewInstanceEntity> instances = new ArrayList<ViewInstanceEntity>();

    securityHelper.getCurrentAuthorities();
    EasyMock.expectLastCall().andReturn(authorities);
    expect(viewEntity.getInstances()).andReturn(instances);

    expect(configuration.getApiAuthentication()).andReturn(true);
    replay(securityHelper, viewEntity, configuration);

    Assert.assertFalse(registry.includeDefinition(viewEntity));

    verify(securityHelper, viewEntity, configuration);
  }

  @Test
  public void testIncludeDefinitionForUserHasAccess() {
    ViewRegistry registry = ViewRegistry.getInstance();
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);
    ViewInstanceEntity instanceEntity = createNiceMock(ViewInstanceEntity.class);
    ResourceEntity resourceEntity = createNiceMock(ResourceEntity.class);
    AmbariGrantedAuthority viewUseAuthority = createNiceMock(AmbariGrantedAuthority.class);
    PrivilegeEntity privilegeEntity = createNiceMock(PrivilegeEntity.class);
    PermissionEntity permissionEntity = createNiceMock(PermissionEntity.class);

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
    expect(configuration.getApiAuthentication()).andReturn(true);
    replay(securityHelper, viewEntity, instanceEntity, viewUseAuthority, privilegeEntity, permissionEntity, configuration);

    Assert.assertTrue(registry.includeDefinition(viewEntity));

    verify(securityHelper, viewEntity, instanceEntity, viewUseAuthority, privilegeEntity, permissionEntity, configuration);
  }

  @Test
  public void testIncludeDefinitionForNoApiAuthentication() {
    ViewRegistry registry = ViewRegistry.getInstance();
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);

    expect(configuration.getApiAuthentication()).andReturn(false);
    replay(securityHelper, viewEntity, configuration);

    Assert.assertTrue(registry.includeDefinition(viewEntity));

    verify(securityHelper, viewEntity, configuration);
  }

  @Test
  public void testExtractViewArchive() throws Exception {

    File viewDir = createNiceMock(File.class);
    File extractedArchiveDir = createNiceMock(File.class);
    File viewArchive = createNiceMock(File.class);
    File archiveDir = createNiceMock(File.class);
    File entryFile  = createNiceMock(File.class);
    File classesDir = createNiceMock(File.class);
    File libDir = createNiceMock(File.class);
    File metaInfDir = createNiceMock(File.class);
    File fileEntry = createNiceMock(File.class);

    JarInputStream viewJarFile = createNiceMock(JarInputStream.class);
    JarEntry jarEntry = createNiceMock(JarEntry.class);
    InputStream is = createMock(InputStream.class);
    FileOutputStream fos = createMock(FileOutputStream.class);
    ViewExtractor viewExtractor = createMock(ViewExtractor.class);

    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(10);
    resourceTypeEntity.setName("MY_VIEW{1.0.0}");

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

    files.put("/var/lib/ambari-server/resources/views/my_view-1.0.0.jar", viewArchive);
    files.put("/var/lib/ambari-server/resources/views/work", extractedArchiveDir);
    files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}", archiveDir);
    files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/view.xml", entryFile);
    files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/WEB-INF/classes", classesDir);
    files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/WEB-INF/lib", libDir);
    files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/META-INF", metaInfDir);

    Map<File, FileOutputStream> outputStreams = new HashMap<File, FileOutputStream>();
    outputStreams.put(entryFile, fos);

    Map<File, JarInputStream> jarFiles = new HashMap<File, JarInputStream>();
    jarFiles.put(viewArchive, viewJarFile);

    // set expectations
    expect(configuration.getViewsDir()).andReturn(viewDir);
    expect(viewDir.getAbsolutePath()).andReturn("/var/lib/ambari-server/resources/views");

    expect(configuration.getViewExtractionThreadPoolCoreSize()).andReturn(2).anyTimes();
    expect(configuration.getViewExtractionThreadPoolMaxSize()).andReturn(3).anyTimes();
    expect(configuration.getViewExtractionThreadPoolTimeout()).andReturn(10000L).anyTimes();

    expect(viewArchive.getAbsolutePath()).andReturn("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}").anyTimes();

    expect(archiveDir.exists()).andReturn(false);
    expect(archiveDir.getAbsolutePath()).andReturn(
        "/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}").anyTimes();

    Capture<ViewEntity> viewEntityCapture = new Capture<ViewEntity>();
    expect(viewExtractor.ensureExtractedArchiveDirectory("/var/lib/ambari-server/resources/views/work")).andReturn(true);
    expect(viewExtractor.extractViewArchive(capture(viewEntityCapture), eq(viewArchive), eq(archiveDir))).andReturn(null);

    // replay mocks
    replay(configuration, viewDir, extractedArchiveDir, viewArchive, archiveDir, entryFile, classesDir,
        libDir, metaInfDir, fileEntry, viewJarFile, jarEntry, is, fos, viewExtractor, resourceDAO, viewDAO, viewInstanceDAO);

    TestViewArchiveUtility archiveUtility = new TestViewArchiveUtility(viewConfigs, files, outputStreams, jarFiles);

    Assert.assertTrue(ViewRegistry.extractViewArchive("/var/lib/ambari-server/resources/views/my_view-1.0.0.jar",
        viewExtractor, archiveUtility, configuration, true));

    // verify mocks
    verify(configuration, viewDir, extractedArchiveDir, viewArchive, archiveDir, entryFile, classesDir,
        libDir, metaInfDir, fileEntry, viewJarFile, jarEntry, is, fos, viewExtractor, resourceDAO, viewDAO, viewInstanceDAO);
  }

  public static class TestViewArchiveUtility extends ViewArchiveUtility {
    private final Map<File, ViewConfig> viewConfigs;
    private final Map<String, File> files;
    private final Map<File, FileOutputStream> outputStreams;
    private final Map<File, JarInputStream> jarFiles;

    public TestViewArchiveUtility(Map<File, ViewConfig> viewConfigs, Map<String, File> files, Map<File,
        FileOutputStream> outputStreams, Map<File, JarInputStream> jarFiles) {
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
    public JarInputStream getJarFileStream(File file) throws IOException {
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

  public static ViewRegistry getRegistry(ViewDAO viewDAO, ViewInstanceDAO viewInstanceDAO,
                                         UserDAO userDAO, MemberDAO memberDAO,
                                         PrivilegeDAO privilegeDAO, ResourceDAO resourceDAO,
                                         ResourceTypeDAO resourceTypeDAO, SecurityHelper securityHelper,
                                         ViewInstanceHandlerList handlerList,
                                         ViewExtractor viewExtractor, ViewArchiveUtility archiveUtility) {

    ViewRegistry instance = new ViewRegistry();

    instance.viewDAO = viewDAO;
    instance.resourceDAO = resourceDAO;
    instance.instanceDAO = viewInstanceDAO;
    instance.userDAO = userDAO;
    instance.memberDAO = memberDAO;
    instance.privilegeDAO = privilegeDAO;
    instance.resourceTypeDAO = resourceTypeDAO;
    instance.securityHelper = securityHelper;
    instance.configuration = configuration;
    instance.handlerList = handlerList;
    instance.extractor = viewExtractor == null ? new ViewExtractor() : viewExtractor;
    instance.archiveUtility = archiveUtility == null ? new ViewArchiveUtility() : archiveUtility;
    instance.extractor.archiveUtility = instance.archiveUtility;

    return instance;
  }

  public static ViewEntity getViewEntity(ViewConfig viewConfig, Configuration ambariConfig,
                                     ClassLoader cl, String archivePath) throws Exception{

    ViewRegistry registry = getRegistry(viewDAO, viewInstanceDAO, userDAO, memberDAO, privilegeDAO,
        resourceDAO, resourceTypeDAO, securityHelper, handlerList, null, null);

    ViewEntity viewDefinition = new ViewEntity(viewConfig, ambariConfig, archivePath);

    registry.setupViewDefinition(viewDefinition, viewConfig, cl);

    return viewDefinition;
  }

  public static ViewInstanceEntity getViewInstanceEntity(ViewEntity viewDefinition, InstanceConfig instanceConfig) throws Exception {

    ViewRegistry registry = getRegistry(viewDAO, viewInstanceDAO, userDAO, memberDAO, privilegeDAO,
        resourceDAO, resourceTypeDAO, securityHelper, handlerList, null, null);

    ViewInstanceEntity viewInstanceDefinition =
        new ViewInstanceEntity(viewDefinition, instanceConfig);

    for (PropertyConfig propertyConfig : instanceConfig.getProperties()) {
      viewInstanceDefinition.putProperty(propertyConfig.getKey(), propertyConfig.getValue());
    }

    registry.bindViewInstance(viewDefinition, viewInstanceDefinition);
    return viewInstanceDefinition;
  }
}
