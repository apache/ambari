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

package org.apache.ambari.server.resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class TestResources extends TestCase {
	
  private static ResourceManager resMan;
  private static final String RESOURCE_FILE_NAME = "resource.ext";
  private static final String RESOURCE_FILE_CONTENT = "CONTENT";
  Injector injector;
  private TemporaryFolder tempFolder = new TemporaryFolder();
  private File resourceFile;

  private class ResourceModule extends AbstractModule {
  @Override
    protected void configure() {
	  requestStaticInjection(TestResources.class);
	}
  }

  @Inject
  static void init(ResourceManager instance) {
    resMan = instance;
  }

  @Before
  public void setUp() throws IOException {
	tempFolder.create();
	
    System.setProperty(Configuration.AMBARI_CONF_VAR, tempFolder.getRoot().getAbsolutePath());
	Properties props = new Properties();
	props.setProperty(Configuration.SRVR_KSTR_DIR_KEY, tempFolder.getRoot().getAbsolutePath());
	props.setProperty(Configuration.RESOURCES_DIR_KEY, tempFolder.getRoot().getAbsolutePath());
	FileOutputStream out = new FileOutputStream(tempFolder.getRoot().getAbsolutePath() + File.separator + Configuration.CONFIG_FILE);
	props.store(out, "");
	out.close();
    resourceFile = tempFolder.newFile(RESOURCE_FILE_NAME);
    FileUtils.writeStringToFile(resourceFile, RESOURCE_FILE_CONTENT);
    injector = Guice.createInjector(new ResourceModule());
    resMan = injector.getInstance(ResourceManager.class);
  }
	
  @After
  public void tearDown() throws IOException {
    resourceFile.delete();
	tempFolder.delete();
  }
	
  @Test
  public void testGetResource() throws Exception {
    File resFile = resMan.getResource(resourceFile.getName());
    assertTrue(resFile.exists());
    String resContent = FileUtils.readFileToString(resFile);
    assertEquals(resContent, RESOURCE_FILE_CONTENT);
  }

}
