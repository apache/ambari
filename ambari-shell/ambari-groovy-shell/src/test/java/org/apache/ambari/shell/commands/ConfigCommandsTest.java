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
package org.apache.ambari.shell.commands;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.groovy.client.AmbariClient;
import org.apache.ambari.shell.completion.ConfigType;
import org.apache.ambari.shell.model.AmbariContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConfigCommandsTest {

  private static final String CORE_SITE = "core-site";

  @InjectMocks
  private ConfigCommands configCommands;

  @Mock
  private AmbariClient client;
  @Mock
  private AmbariContext context;

  @Test
  public void testShowConfig() {
    ConfigType configType = mock(ConfigType.class);
    Map<String, Map<String, String>> mockResult = mock(Map.class);
    when(configType.getName()).thenReturn(CORE_SITE);
    when(client.getServiceConfigMap(anyString())).thenReturn(mockResult);
    when(mockResult.get(CORE_SITE)).thenReturn(new HashMap<String, String>());

    configCommands.showConfig(configType);

    verify(client).getServiceConfigMap(CORE_SITE);
  }

  @Test
  public void testSetConfigForFile() throws IOException {
    ConfigType configType = mock(ConfigType.class);
    File file = new File("src/test/resources/core-site.xml");
    when(configType.getName()).thenReturn(CORE_SITE);

    configCommands.setConfig(configType, "", file);

    Map<String, String> config = new HashMap<String, String>();
    config.put("fs.trash.interval", "350");
    config.put("ipc.client.connection.maxidletime", "30000");
    verify(client).modifyConfiguration(CORE_SITE, config);
  }

  @Test
  public void testModifyConfig() throws IOException {
    ConfigType configType = mock(ConfigType.class);
    Map<String, Map<String, String>> mockResult = mock(Map.class);
    Map<String, String> config = new HashMap<String, String>();
    config.put("fs.trash.interval", "350");
    config.put("ipc.client.connection.maxidletime", "30000");
    when(configType.getName()).thenReturn(CORE_SITE);
    when(mockResult.get(CORE_SITE)).thenReturn(config);
    when(client.getServiceConfigMap(CORE_SITE)).thenReturn(mockResult);

    configCommands.modifyConfig(configType, "fs.trash.interval", "510");

    Map<String, String> config2 = new HashMap<String, String>();
    config2.put("fs.trash.interval", "510");
    config2.put("ipc.client.connection.maxidletime", "30000");
    verify(client).modifyConfiguration(CORE_SITE, config2);
  }

}
