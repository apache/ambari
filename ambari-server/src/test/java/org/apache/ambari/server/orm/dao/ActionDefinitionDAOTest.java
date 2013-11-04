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
package org.apache.ambari.server.orm.dao;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import junit.framework.Assert;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.actionmanager.ActionType;
import org.apache.ambari.server.actionmanager.TargetHostType;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.entities.ActionEntity;
import org.apache.ambari.server.state.Service;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.RollbackException;
import java.util.List;

public class ActionDefinitionDAOTest {
  private Injector injector;
  private ActionDefinitionDAO actionDefinitionDAO;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    actionDefinitionDAO = injector.getInstance(ActionDefinitionDAO.class);
  }

  @After
  public void teardown() throws AmbariException {
    injector.getInstance(PersistService.class).stop();
  }

  private ActionEntity createActionDefinition(
      String actionName, ActionType actionType, String inputs, String targetService, Role targetComponent,
      Short defaultTimeout, String description, TargetHostType targetType, Boolean addToDAO) throws Exception {
    ActionEntity actionDefinitionEntity = new ActionEntity();

    actionDefinitionEntity.setActionName(actionName);
    actionDefinitionEntity.setActionType(actionType);
    actionDefinitionEntity.setDefaultTimeout(defaultTimeout);
    actionDefinitionEntity.setDescription(description);
    actionDefinitionEntity.setInputs(inputs);
    actionDefinitionEntity.setTargetComponent(targetComponent.toString());
    actionDefinitionEntity.setTargetService(targetService);
    actionDefinitionEntity.setTargetType(targetType);

    if (addToDAO) {
      actionDefinitionDAO.create(actionDefinitionEntity);
    }

    return actionDefinitionEntity;
  }

  @Test
  public void testFindAll() throws Exception {
    createActionDefinition("a1", ActionType.SYSTEM, "fileName", Service.Type.HDFS.toString(), Role.DATANODE,
        Short.parseShort("10"), "a1", TargetHostType.ANY, true);
    createActionDefinition("a2", ActionType.SYSTEM, "fileName", Service.Type.HDFS.toString(), Role.DATANODE,
        Short.parseShort("10"), "a2", TargetHostType.ANY, true);

    List<ActionEntity> actionDefinitionEntities = actionDefinitionDAO.findAll();

    Assert.assertNotNull(actionDefinitionEntities);
    Assert.assertEquals(2, actionDefinitionEntities.size());
  }

  @Test
  public void testFindByPK() throws Exception {
    createActionDefinition("c1", ActionType.SYSTEM, "fileName", Service.Type.HDFS.toString(), Role.DATANODE,
        Short.parseShort("10"), "a1", TargetHostType.ANY, true);
    createActionDefinition("c2", ActionType.SYSTEM, "fileName", Service.Type.HDFS.toString(), Role.DATANODE,
        Short.parseShort("10"), "a2", TargetHostType.ANY, true);

    ActionEntity actionDefinitionEntity = actionDefinitionDAO.findByPK("c1");

    Assert.assertNotNull(actionDefinitionEntity);
    Assert.assertEquals("c1", actionDefinitionEntity.getActionName());
  }

  @Test
  public void testDuplicate() throws Exception {
    createActionDefinition("b1", ActionType.SYSTEM, "fileName", Service.Type.HDFS.toString(), Role.DATANODE,
        Short.parseShort("10"), "a1", TargetHostType.ANY, true);
    try {
      createActionDefinition("b1", ActionType.SYSTEM, "fileName", Service.Type.HDFS.toString(), Role.DATANODE,
          Short.parseShort("10"), "a1", TargetHostType.ANY, true);
      Assert.fail("Should throw exception");
    } catch (RollbackException rbe) {
      Assert.assertTrue(rbe.getMessage().contains("duplicate"));
    }
  }

  @Test
  public void testUpdate() throws Exception {
    createActionDefinition("d1", ActionType.SYSTEM, "fileName", Service.Type.HDFS.toString(), Role.DATANODE,
        Short.parseShort("101"), "a1", TargetHostType.ANY, true);
    ActionEntity newOne = createActionDefinition("d1", ActionType.SYSTEM, "fileValue", Service.Type.HDFS.toString(),
        Role.DATANODE, Short.parseShort("101"), "a1", TargetHostType.ANY, false);
    actionDefinitionDAO.merge(newOne);
    ActionEntity actionDefinitionEntity = actionDefinitionDAO.findByPK("d1");
    Assert.assertEquals("fileValue", actionDefinitionEntity.getInputs());

    actionDefinitionDAO.remove(newOne);
    actionDefinitionEntity = actionDefinitionDAO.findByPK("d1");
    Assert.assertNull(actionDefinitionEntity);
  }

  @Test
  public void testDeleteNonExistent() throws Exception {
    ActionEntity newOne = createActionDefinition("d1", ActionType.SYSTEM, "fileValue", Service.Type.HDFS.toString(),
        Role.DATANODE, Short.parseShort("101"), "a1", TargetHostType.ANY, false);
    actionDefinitionDAO.remove(newOne);
  }
}
