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
package org.apache.ambari.server.state;

import org.apache.ambari.server.api.util.StackExtensionHelper;
import org.apache.ambari.server.state.stack.ConfigurationXml;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class PropertyInfoTest {

  @Test
  public void testGetAttributesMap() throws Exception {
    Map<String, String> attributes;
    File configFile = new File("./src/test/resources/stacks/HDP/2.0.8/services/HDFS/configuration/hdfs-site.xml");
    ConfigurationXml configuration = StackExtensionHelper.unmarshal(ConfigurationXml.class, configFile);
    List<PropertyInfo> properties = configuration.getProperties();
    PropertyInfo dfsNameDir = properties.get(0);
    assertNotNull(dfsNameDir);
    assertEquals("dfs.name.dir", dfsNameDir.getName());
    attributes = dfsNameDir.getAttributesMap();
    assertEquals(1, attributes.size());
    assertTrue(attributes.containsKey("final"));
    assertEquals("true", attributes.get("final"));

    PropertyInfo dfsSupportAppend = properties.get(1);
    assertNotNull(dfsSupportAppend);
    assertEquals("dfs.support.append", dfsSupportAppend.getName());
    attributes = dfsSupportAppend.getAttributesMap();
    assertEquals(2, attributes.size());
    assertTrue(attributes.containsKey("final"));
    assertEquals("true", attributes.get("final"));
    assertTrue(attributes.containsKey("deletable"));
    assertEquals("false", attributes.get("deletable"));

    PropertyInfo dfsWebhdfsEnabled = properties.get(2);
    assertNotNull(dfsWebhdfsEnabled);
    assertEquals("dfs.webhdfs.enabled", dfsWebhdfsEnabled.getName());
    attributes = dfsWebhdfsEnabled.getAttributesMap();
    assertEquals(0, attributes.size());
  }
}