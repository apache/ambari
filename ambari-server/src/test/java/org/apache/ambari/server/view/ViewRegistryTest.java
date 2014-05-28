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

import org.apache.ambari.server.api.resources.SubResourceDefinition;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.orm.dao.ViewDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewEntityTest;
import org.apache.ambari.server.orm.entities.ViewInstanceDataEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntityTest;
import org.apache.ambari.server.view.configuration.InstanceConfig;
import org.apache.ambari.server.view.configuration.InstanceConfigTest;
import org.apache.ambari.server.view.configuration.ResourceConfig;
import org.apache.ambari.server.view.configuration.ResourceConfigTest;
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.apache.ambari.server.view.events.EventImpl;
import org.apache.ambari.server.view.events.EventImplTest;
import org.apache.ambari.view.events.Event;
import org.apache.ambari.view.events.Listener;
import org.easymock.Capture;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

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

    ViewDAO vDAO = createMock(ViewDAO.class);

    ViewRegistry.setViewDAO(vDAO);

    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();

    Map<File, ViewConfig> viewConfigs =
        Collections.singletonMap(viewArchive, viewDefinition.getConfiguration());

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

    Capture<ViewEntity> captureViewEntity = new Capture<ViewEntity>();

    expect(vDAO.findByName("MY_VIEW{1.0.0}")).andReturn(null);
    vDAO.create(capture(captureViewEntity));

    expect(vDAO.findAll()).andReturn(Collections.<ViewEntity>emptyList());

    // replay mocks
    replay(configuration, viewDir, extractedArchiveDir, viewArchive, archiveDir, entryFile, classesDir,
        libDir, fileEntry, viewJarFile, enumeration, jarEntry, is, fos, vDAO);

    ViewRegistry registry = ViewRegistry.getInstance();
    registry.setHelper(new TestViewRegistryHelper(viewConfigs, files, outputStreams, jarFiles));

    Set<ViewInstanceEntity> instanceEntities = registry.readViewArchives(configuration);

    Assert.assertEquals(2, instanceEntities.size());
    Assert.assertEquals("MY_VIEW", captureViewEntity.getValue().getCommonName());

    // verify mocks
    verify(configuration, viewDir, extractedArchiveDir, viewArchive, archiveDir, entryFile, classesDir,
        libDir, fileEntry, viewJarFile, enumeration, jarEntry, is, fos, vDAO);
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

    ViewDAO vDAO = createMock(ViewDAO.class);

    ViewRegistry.setViewDAO(vDAO);

    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();

    Map<File, ViewConfig> viewConfigs =
        Collections.singletonMap(viewArchive, viewDefinition.getConfiguration());

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

    Capture<ViewEntity> captureViewEntity = new Capture<ViewEntity>();

    expect(vDAO.findByName("MY_VIEW{1.0.0}")).andReturn(null);
    vDAO.create(capture(captureViewEntity));
    expectLastCall().andThrow(new IllegalArgumentException("Expected exception."));

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

    registry.addDefinition(viewEntity);
    registry.addInstanceDefinition(viewEntity, viewInstanceEntity);

    Collection<ViewInstanceEntity> viewInstanceDefinitions = registry.getInstanceDefinitions(viewEntity);

    Assert.assertEquals(1, viewInstanceDefinitions.size());

    Assert.assertEquals(viewInstanceEntity, viewInstanceDefinitions.iterator().next());
  }

  @Test
  public void testRemoveInstanceData() throws Exception {

    ViewDAO viewDAO = createNiceMock(ViewDAO.class);
    ViewInstanceDAO viewInstanceDAO = createNiceMock(ViewInstanceDAO.class);

    ViewRegistry.init(viewDAO, viewInstanceDAO);

    ViewRegistry registry = ViewRegistry.getInstance();

    ViewInstanceEntity viewInstanceEntity = ViewInstanceEntityTest.getViewInstanceEntity();

    viewInstanceEntity.putInstanceData("foo", "value");

    ViewInstanceDataEntity dataEntity = viewInstanceEntity.getInstanceData("foo");

    viewInstanceDAO.removeData(dataEntity);
    expect(viewInstanceDAO.merge(viewInstanceEntity)).andReturn(viewInstanceEntity);
    replay(viewDAO, viewInstanceDAO);

    registry.removeInstanceData(viewInstanceEntity, "foo");

    Assert.assertNull(viewInstanceEntity.getInstanceData("foo"));
    verify(viewDAO, viewInstanceDAO);
  }

  @Before
  public void before() throws Exception {
    ViewRegistry.getInstance().clear();
    ViewRegistry.setViewDAO(null);
  }

  @AfterClass
  public static void afterClass() {
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

}
