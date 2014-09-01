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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.apache.ambari.shell.support.TableRenderer.renderMultiValueMap;
import static org.apache.ambari.shell.support.TableRenderer.renderSingleMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.groovy.client.AmbariClient;
import org.apache.ambari.groovy.client.InvalidHostGroupHostAssociation;
import org.apache.ambari.shell.completion.Blueprint;
import org.apache.ambari.shell.completion.Host;
import org.apache.ambari.shell.flash.FlashService;
import org.apache.ambari.shell.model.AmbariContext;
import org.apache.ambari.shell.model.Hints;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import groovyx.net.http.HttpResponseException;

@RunWith(MockitoJUnitRunner.class)
public class ClusterCommandsTest {

  @InjectMocks
  private ClusterCommands clusterCommands;

  @Mock
  private AmbariClient client;
  @Mock
  private AmbariContext context;
  @Mock
  private HttpResponseException responseException;
  @Mock
  private FlashService flashService;

  @Test
  public void testIsClusterBuildCommandAvailable() {
    when(context.isConnectedToCluster()).thenReturn(false);
    when(context.isFocusOnClusterBuild()).thenReturn(false);
    when(context.areBlueprintsAvailable()).thenReturn(true);

    boolean result = clusterCommands.isClusterBuildCommandAvailable();

    assertTrue(result);
  }

  @Test
  public void testIsClusterBuildCommandAvailableAndFocusOnBuild() {
    when(context.isConnectedToCluster()).thenReturn(false);
    when(context.isFocusOnClusterBuild()).thenReturn(true);
    when(context.areBlueprintsAvailable()).thenReturn(true);

    boolean result = clusterCommands.isClusterBuildCommandAvailable();

    assertFalse(result);
  }

  @Test
  public void testIsClusterBuildCommandAvailableAndNoBlueprints() {
    when(context.isConnectedToCluster()).thenReturn(false);
    when(context.isFocusOnClusterBuild()).thenReturn(false);
    when(context.areBlueprintsAvailable()).thenReturn(false);

    boolean result = clusterCommands.isClusterBuildCommandAvailable();

    assertFalse(result);
  }

  @Test
  public void testBuildClusterForNonExistingBlueprint() {
    when(client.doesBlueprintExist("id")).thenReturn(false);

    String result = clusterCommands.buildCluster(new Blueprint("id"));

    verify(client).doesBlueprintExist("id");
    assertEquals("Not a valid blueprint id", result);
  }

  @Test
  public void testBuildCluster() {
    Map<String, String> hostNames = singletonMap("host1", "HEALTHY");
    Map<String, List<String>> map = singletonMap("group1", asList("comp1", "comp2"));
    when(client.doesBlueprintExist("id")).thenReturn(true);
    when(client.getBlueprintMap("id")).thenReturn(map);
    when(context.getFocusValue()).thenReturn("id");
    when(client.getHostNames()).thenReturn(hostNames);

    String result = clusterCommands.buildCluster(new Blueprint("id"));

    verify(client).doesBlueprintExist("id");
    verify(client).getBlueprintMap("id");
    verify(client).getHostGroups("id");
    assertEquals(String.format("%s\n%s", renderSingleMap(hostNames, "HOSTNAME", "STATE"),
      renderMultiValueMap(map, "HOSTGROUP", "COMPONENT")), result);
  }

  @Test
  public void testAssignForInvalidHostGroup() {
    Map<String, List<String>> map = singletonMap("group1", asList("host", "host2"));
    ReflectionTestUtils.setField(clusterCommands, "hostGroups", map);
    when(client.getHostNames()).thenReturn(singletonMap("host3", "HEALTHY"));

    String result = clusterCommands.assign(new Host("host3"), "group0");

    assertEquals("group0 is not a valid host group", result);
  }

  @Test
  public void testAssignForValidHostGroup() {
    Map<String, List<String>> map = new HashMap<String, List<String>>();
    map.put("group1", new ArrayList<String>());
    ReflectionTestUtils.setField(clusterCommands, "hostGroups", map);
    when(client.getHostNames()).thenReturn(singletonMap("host3", "HEALTHY"));

    String result = clusterCommands.assign(new Host("host3"), "group1");

    assertEquals("host3 has been added to group1", result);
  }

  @Test
  public void testAssignForInvalidHost() {
    Map<String, List<String>> map = new HashMap<String, List<String>>();
    map.put("group1", new ArrayList<String>());
    ReflectionTestUtils.setField(clusterCommands, "hostGroups", map);
    when(client.getHostNames()).thenReturn(singletonMap("host2", "HEALTHY"));

    String result = clusterCommands.assign(new Host("host3"), "group1");

    assertEquals("host3 is not a valid hostname", result);
  }

  @Test
  public void testCreateClusterForException() throws HttpResponseException {
    String blueprint = "blueprint";
    Map<String, List<String>> map = singletonMap("group1", asList("host", "host2"));
    ReflectionTestUtils.setField(clusterCommands, "hostGroups", map);
    when(context.getFocusValue()).thenReturn(blueprint);
    doThrow(responseException).when(client).createCluster(blueprint, blueprint, map);
    doThrow(responseException).when(client).deleteCluster(blueprint);

    String result = clusterCommands.createCluster(false);

    verify(client).createCluster(blueprint, blueprint, map);
    verify(client).getHostGroups(blueprint);
    verify(client).deleteCluster(blueprint);
    assertTrue(result.contains("Failed"));
  }

  @Test
  public void testCreateCluster() throws HttpResponseException {
    String blueprint = "blueprint";
    Map<String, List<String>> map = singletonMap("group1", asList("host", "host2"));
    ReflectionTestUtils.setField(clusterCommands, "hostGroups", map);
    when(context.getFocusValue()).thenReturn(blueprint);
    when(client.getClusterName()).thenReturn("cluster");

    String result = clusterCommands.createCluster(false);

    verify(client).createCluster(blueprint, blueprint, map);
    verify(context).resetFocus();
    assertFalse(result.contains("Failed"));
    assertTrue(result.contains("Successfully"));
  }

  @Test
  public void testDeleteClusterForException() throws HttpResponseException {
    when(context.getCluster()).thenReturn("cluster");
    when(responseException.getMessage()).thenReturn("msg");
    doThrow(responseException).when(client).deleteCluster("cluster");

    String result = clusterCommands.deleteCluster();

    verify(client).deleteCluster("cluster");
    verify(context).getCluster();
    verify(responseException).getMessage();
    assertEquals("Could not delete the cluster: msg", result);
  }

  @Test
  public void testDeleteCluster() throws HttpResponseException {
    when(context.getCluster()).thenReturn("cluster");
    when(responseException.getMessage()).thenReturn("msg");

    String result = clusterCommands.deleteCluster();

    verify(client).deleteCluster("cluster");
    verify(context).getCluster();
    assertEquals("Successfully deleted the cluster", result);
  }

  @Test
  public void testIsClusterPreviewCommandAvailable() {
    when(context.isFocusOnClusterBuild()).thenReturn(true);
    ReflectionTestUtils.setField(clusterCommands, "hostGroups", singletonMap("group1", asList("host1")));

    boolean result = clusterCommands.isClusterPreviewCommandAvailable();

    assertTrue(result);
  }

  @Test
  public void testIsClusterPreviewCommandAvailableForNoAssignments() {
    when(context.isFocusOnClusterBuild()).thenReturn(true);
    ReflectionTestUtils.setField(clusterCommands, "hostGroups", singletonMap("group1", emptyList()));

    boolean result = clusterCommands.isClusterPreviewCommandAvailable();

    assertFalse(result);
  }

  @Test
  public void testIsClusterResetCommandAvailable() {
    when(context.isFocusOnClusterBuild()).thenReturn(true);
    ReflectionTestUtils.setField(clusterCommands, "hostGroups", singletonMap("group1", asList("host1")));

    boolean result = clusterCommands.isClusterResetCommandAvailable();

    assertTrue(result);
  }

  @Test
  public void testAutoAssignForEmptyResult() throws InvalidHostGroupHostAssociation {
    Map<String, List<String>> hostGroups = singletonMap("group1", asList("host1"));
    ReflectionTestUtils.setField(clusterCommands, "hostGroups", hostGroups);
    when(context.getFocusValue()).thenReturn("blueprint");
    when(client.recommendAssignments("blueprint")).thenReturn(new HashMap<String, List<String>>());

    clusterCommands.autoAssign();

    Map<String, List<String>> result = (Map<String, List<String>>) ReflectionTestUtils.getField(clusterCommands, "hostGroups");
    assertEquals(hostGroups, result);
  }

  @Test
  public void testAutoAssign() throws InvalidHostGroupHostAssociation {
    Map<String, List<String>> hostGroups = singletonMap("group1", asList("host1"));
    Map<String, List<String>> newAssignments = singletonMap("group1", asList("host1"));
    ReflectionTestUtils.setField(clusterCommands, "hostGroups", hostGroups);
    when(context.getFocusValue()).thenReturn("blueprint");
    when(client.recommendAssignments("blueprint")).thenReturn(newAssignments);

    clusterCommands.autoAssign();

    Map<String, List<String>> result = (Map<String, List<String>>) ReflectionTestUtils.getField(clusterCommands, "hostGroups");
    assertEquals(newAssignments, result);
    verify(context).setHint(Hints.CREATE_CLUSTER);
  }
}
