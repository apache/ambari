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
package org.apache.ambari.server.actionmanager;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import junit.framework.Assert;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.ActionDefinitionDAO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TestCustomActionDBAccessorImpl {
  private static final Logger log = LoggerFactory.getLogger(TestCustomActionDBAccessorImpl.class);
  CustomActionDBAccessor db;
  private Injector injector;
  @Inject
  private ActionDefinitionDAO actions;

  @Before
  public void setup() throws AmbariException {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
    db = injector.getInstance(CustomActionDBAccessorImpl.class);
  }

  @After
  public void tearDown() throws AmbariException {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testActionLifeCycle() throws Exception {
    try {
      db.createActionDefinition(
          "a1", ActionType.SYSTEM, "a,b,,c", "desc", TargetHostType.ANY, "HDFS", "DATANODE", Short.parseShort("70"));
      Assert.fail("createActionDefinition must throw exception.");
    } catch (AmbariException ex) {
      Assert.assertTrue(ex.getMessage().contains("Empty parameter cannot be specified as an input parameter"));
    }

    db.createActionDefinition(
        "a1", ActionType.SYSTEM, "fileName", "desc", TargetHostType.ANY, "HDFS", "DATANODE", Short.parseShort("70"));
    ActionDefinition ad = db.getActionDefinition("a1");
    assertContent(
        ad, "a1", ActionType.SYSTEM, "fileName", "desc", TargetHostType.ANY, "HDFS", "DATANODE", Short.parseShort("70"));

    ad = db.getActionDefinition("a2");
    Assert.assertNull(ad);

    try {
      db.createActionDefinition(
          "a1", ActionType.SYSTEM, "fileName", "desc", TargetHostType.ANY, "HDFS", "DATANODE", Short.parseShort("70"));
      Assert.fail("updateActionDefinition must throw exception.");
    } catch (AmbariException ex) {
      Assert.assertTrue(ex.getMessage().contains("Action definition a1 already exists"));
    }

    db.createActionDefinition(
        "a2", ActionType.SYSTEM, "dirName", "desc2", TargetHostType.ANY, "HDFS", "DATANODE", Short.parseShort("70"));
    ad = db.getActionDefinition("a2");
    assertContent(ad, "a2", ActionType.SYSTEM, "dirName", "desc2", TargetHostType.ANY,
        "HDFS", "DATANODE", Short.parseShort("70"));

    db.updateActionDefinition("a2", ActionType.USER, "desc3", TargetHostType.ALL, Short.parseShort("100"));
    ad = db.getActionDefinition("a2");
    assertContent(ad, "a2", ActionType.USER, "dirName", "desc3", TargetHostType.ALL,
        "HDFS", "DATANODE", Short.parseShort("100"));

    List<ActionDefinition> ads = db.getActionDefinitions();
    Assert.assertEquals(2, ads.size());

    db.deleteActionDefinition("a1");
    ads = db.getActionDefinitions();
    Assert.assertEquals(1, ads.size());

    db.deleteActionDefinition("a1");

    try {
      db.updateActionDefinition("a1", ActionType.USER, "desc3", TargetHostType.ALL, Short.parseShort("100"));
      Assert.fail("updateActionDefinition must throw exception.");
    } catch (AmbariException ex) {
      Assert.assertTrue(ex.getMessage().contains("Action definition a1 does not exist"));
    }
  }

  private void assertContent(ActionDefinition ad, String actionName, ActionType actionType, String inputs,
                             String description, TargetHostType targetType, String serviceType, String componentType,
                             Short defaultTimeout) {
    Assert.assertEquals(actionName, ad.getActionName());
    Assert.assertEquals(actionType, ad.getActionType());
    Assert.assertEquals(description, ad.getDescription());
    Assert.assertEquals(inputs, ad.getInputs());
    Assert.assertEquals(serviceType, ad.getTargetService());
    Assert.assertEquals(componentType, ad.getTargetComponent());
    Assert.assertEquals(defaultTimeout, ad.getDefaultTimeout());
    Assert.assertEquals(targetType, ad.getTargetType());
  }
}
