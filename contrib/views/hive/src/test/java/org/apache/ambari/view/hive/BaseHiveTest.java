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

package org.apache.ambari.view.hive;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.ViewResourceHandler;
import org.apache.hadoop.fs.FileUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;

public abstract class BaseHiveTest {
  protected ViewResourceHandler handler;
  protected ViewContext context;
  protected static File hiveStorageFile;
  protected static File baseDir;
  protected Map<String, String> properties;

  protected static String DATA_DIRECTORY = "./target/HiveTest";

  @BeforeClass
  public static void startUp() throws Exception {
    File baseDir = new File(DATA_DIRECTORY)
        .getAbsoluteFile();
    FileUtil.fullyDelete(baseDir);
  }

  @AfterClass
  public static void shutDown() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
    handler = createNiceMock(ViewResourceHandler.class);

    properties = new HashMap<String, String>();
    baseDir = new File(DATA_DIRECTORY)
        .getAbsoluteFile();
    hiveStorageFile = new File("./target/HiveTest/storage.dat")
        .getAbsoluteFile();

    setupDefaultContextProperties(properties);
    setupProperties(properties, baseDir);

    context = makeContext(properties, "ambari-qa", "MyHive");

    replay(handler, context);
  }

  public void setupDefaultContextProperties(Map<String, String> properties) {
    properties.put("dataworker.storagePath", hiveStorageFile.toString());
    properties.put("scripts.dir", "/tmp/.hiveQueries");
    properties.put("jobs.dir", "/tmp/.hiveJobs");
    properties.put("yarn.ats.url", "127.0.0.1:8188");
    properties.put("yarn.resourcemanager.url", "127.0.0.1:8088");
  }

  public ViewContext makeContext(Map<String, String> properties, String username, String instanceName) throws Exception {
    setupDefaultContextProperties(properties);
    setupProperties(properties, baseDir);

    ViewContext context = createNiceMock(ViewContext.class);
    expect(context.getProperties()).andReturn(properties).anyTimes();
    expect(context.getUsername()).andReturn(username).anyTimes();
    expect(context.getInstanceName()).andReturn(instanceName).anyTimes();
    return context;
  }

  protected void setupProperties(Map<String, String> properties, File baseDir) throws Exception {

  }

  @After
  public void tearDown() throws Exception {

  }

  protected static <T> T getService(Class<T> clazz,
                                    final ViewResourceHandler viewResourceHandler,
                                    final ViewContext viewInstanceContext) {
    Injector viewInstanceInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(ViewResourceHandler.class).toInstance(viewResourceHandler);
        bind(ViewContext.class).toInstance(viewInstanceContext);
      }
    });
    return viewInstanceInjector.getInstance(clazz);
  }
}
