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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.ambari.groovy.client.AmbariClient;
import org.apache.ambari.shell.model.AmbariContext;
import org.apache.ambari.shell.model.Hints;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import groovyx.net.http.HttpResponseException;

@RunWith(MockitoJUnitRunner.class)
public class BlueprintCommandsTest {

  @InjectMocks
  private BlueprintCommands blueprintCommands;

  @Mock
  private AmbariClient ambariClient;
  @Mock
  private HttpResponseException responseException;
  @Mock
  private AmbariContext context;
  @Mock
  private ObjectMapper objectMapper;

  @Test
  public void testAddBlueprintForFileReadPrecedence() throws IOException {
    File file = new File("src/test/resources/testBlueprint.json");
    String json = IOUtils.toString(new FileInputStream(file));
    JsonNode jsonNode = mock(JsonNode.class);
    when(objectMapper.readTree(json.getBytes())).thenReturn(jsonNode);
    when(jsonNode.get("Blueprints")).thenReturn(jsonNode);
    when(jsonNode.get("blueprint_name")).thenReturn(jsonNode);
    when(jsonNode.asText()).thenReturn("blueprintName");

    String result = blueprintCommands.addBlueprint("url", file);

    verify(ambariClient).addBlueprint(json);
    verify(context).setHint(Hints.BUILD_CLUSTER);
    verify(context).setBlueprintsAvailable(true);
    assertEquals("Blueprint: 'blueprintName' has been added", result);
  }

  @Test
  public void testAddBlueprintForException() throws IOException {
    File file = new File("src/test/resources/testBlueprint.json");
    String json = IOUtils.toString(new FileInputStream(file));
    doThrow(responseException).when(ambariClient).addBlueprint(json);
    when(responseException.getMessage()).thenReturn("error");

    String result = blueprintCommands.addBlueprint("url", file);

    verify(ambariClient).addBlueprint(json);
    verify(responseException).getMessage();
    assertEquals("Cannot add blueprint: error", result);
  }

  @Test
  public void testAddBlueprintForDefaults() throws HttpResponseException {
    String result = blueprintCommands.addBlueprint();

    verify(ambariClient).addDefaultBlueprints();
    assertEquals("Default blueprints added", result);
  }

  @Test
  public void testAddBlueprintForUnspecifiedValue() throws HttpResponseException {
    String response = blueprintCommands.addBlueprint(null, null);

    assertEquals("No blueprint specified", response);
    verify(ambariClient, times(0)).addBlueprint(null);
  }

  @Test
  public void testAddBlueprintDefaultsForException() throws HttpResponseException {
    doThrow(responseException).when(ambariClient).addDefaultBlueprints();
    when(responseException.getMessage()).thenReturn("error");

    String result = blueprintCommands.addBlueprint();

    verify(responseException).getMessage();
    assertEquals("Failed to add the default blueprints: error", result);
  }

  @Test
  public void testAddBlueprintDefaultsForConnectionRefused() throws HttpResponseException {
    doThrow(new RuntimeException("Connection refused")).when(ambariClient).addDefaultBlueprints();
    when(responseException.getMessage()).thenReturn("error");

    String result = blueprintCommands.addBlueprint();

    assertEquals("Failed to add the default blueprints: Connection refused", result);
  }
}
