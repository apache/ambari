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

package org.apache.ambari.view.pig.test;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.ViewResourceHandler;
import org.apache.ambari.view.pig.BasePigTest;
import org.apache.ambari.view.pig.HDFSTest;
import org.apache.ambari.view.pig.resources.files.FileService;
import org.apache.ambari.view.pig.resources.scripts.ScriptService;
import org.apache.ambari.view.pig.resources.scripts.models.PigScript;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.json.simple.JSONObject;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.ws.WebServiceException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.*;

/**
 * Tests without HDFS and predefined properties
 */
public class ScriptTestUnmanaged extends BasePigTest {
  private ScriptService scriptService;
  private File pigStorageFile;
  private File baseDir;

  @AfterClass
  public static void shutDown() throws Exception {
    FileService.setHdfsApi(null); //cleanup API connection
  }

  @Before
  public void setUp() throws Exception {
    handler = createNiceMock(ViewResourceHandler.class);
    context = createNiceMock(ViewContext.class);

    baseDir = new File(DATA_DIRECTORY)
        .getAbsoluteFile();
    pigStorageFile = new File("./target/BasePigTest/storage.dat")
        .getAbsoluteFile();
  }

  private Response doCreateScript(String title, String path) {
    return ScriptTest.doCreateScript(title, path, scriptService);
  }

  @Test(expected=WebServiceException.class)
  public void createScriptAutoCreateNoDefaultFS() {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("dataworker.storagePath", pigStorageFile.toString());
    properties.put("dataworker.scripts.path", "/tmp/.pigscripts");

    expect(context.getProperties()).andReturn(properties).anyTimes();
    expect(context.getUsername()).andReturn("ambari-qa").anyTimes();

    replay(handler, context);
    scriptService = getService(ScriptService.class, handler, context);

    doCreateScript("Test", null);
  }
}
