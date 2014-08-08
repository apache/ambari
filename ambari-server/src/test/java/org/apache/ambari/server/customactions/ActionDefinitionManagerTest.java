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

package org.apache.ambari.server.customactions;

import java.io.File;

import junit.framework.Assert;
import org.apache.ambari.server.actionmanager.ActionType;
import org.apache.ambari.server.actionmanager.TargetHostType;
import org.junit.Test;

public class ActionDefinitionManagerTest {

  private final String customActionDefinitionRoot = "./src/test/resources/custom_action_definitions/";

  @Test
  public void testReadCustomActionDefinitions() throws Exception {
    ActionDefinitionManager manager = new ActionDefinitionManager();
    manager.readCustomActionDefinitions(new File(customActionDefinitionRoot));

    Assert.assertEquals(2, manager.getAllActionDefinition().size());
    ActionDefinition ad = manager.getActionDefinition("customAction1");
    Assert.assertNotNull(ad);
    Assert.assertEquals("customAction1", ad.getActionName());
    Assert.assertEquals("A random test", ad.getDescription());
    Assert.assertEquals("threshold", ad.getInputs());
    Assert.assertEquals("TASKTRACKER", ad.getTargetComponent());
    Assert.assertEquals("MAPREDUCE", ad.getTargetService());
    Assert.assertEquals(60, (int)ad.getDefaultTimeout());
    Assert.assertEquals(TargetHostType.ALL, ad.getTargetType());
    Assert.assertEquals(ActionType.USER, ad.getActionType());

    ad = manager.getActionDefinition("customAction2");
    Assert.assertNotNull(ad);
    Assert.assertEquals("customAction2", ad.getActionName());
    Assert.assertEquals("A random test", ad.getDescription());
    Assert.assertEquals(null, ad.getInputs());
    Assert.assertEquals("TASKTRACKER", ad.getTargetComponent());
    Assert.assertEquals("MAPREDUCE", ad.getTargetService());
    Assert.assertEquals(60, (int)ad.getDefaultTimeout());
    Assert.assertEquals(null, ad.getTargetType());
    Assert.assertEquals(ActionType.USER, ad.getActionType());
  }
}

