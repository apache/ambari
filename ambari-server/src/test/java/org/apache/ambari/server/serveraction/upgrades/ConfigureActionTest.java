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
package org.apache.ambari.server.serveraction.upgrades;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.upgrade.ConfigureTask;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Tests upgrade-related server side actions
 */
public class ConfigureActionTest {
  private static final String HDP_2_2_1_0 = "2.2.1.0-2270";
  private static final String HDP_2_2_0_0 = "2.2.0.0-2041";
  private static final StackId HDP_21_STACK = new StackId("HDP-2.1.1");
  private static final StackId HDP_22_STACK = new StackId("HDP-2.2.0");

  private Injector m_injector;

  @Inject
  private OrmTestHelper m_helper;

  @Inject
  private RepositoryVersionDAO repoVersionDAO;

  @Inject
  private HostVersionDAO hostVersionDAO;

  @Inject
  private HostRoleCommandFactory hostRoleCommandFactory;

  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new InMemoryDefaultTestModule());
    m_injector.getInstance(GuiceJpaInitializer.class);
    m_injector.injectMembers(this);
  }

  @After
  public void teardown() throws Exception {
    m_injector.getInstance(PersistService.class).stop();
  }

  private void makeUpgradeCluster() throws Exception {
    String clusterName = "c1";
    String hostName = "h1";

    Clusters clusters = m_injector.getInstance(Clusters.class);
    clusters.addCluster(clusterName, HDP_21_STACK);

    StackDAO stackDAO = m_injector.getInstance(StackDAO.class);
    StackEntity stackEntity = stackDAO.find(HDP_21_STACK.getStackName(),
        HDP_21_STACK.getStackVersion());

    assertNotNull(stackEntity);

    Cluster c = clusters.getCluster(clusterName);
    c.setDesiredStackVersion(HDP_21_STACK);

    ConfigFactory cf = m_injector.getInstance(ConfigFactory.class);
    Config config = cf.createNew(c, "zoo.cfg", new HashMap<String, String>() {{
          put("initLimit", "10");
        }}, new HashMap<String, Map<String,String>>());
    config.setTag("version1");
    config.persist();

    c.addConfig(config);
    c.addDesiredConfig("user", Collections.singleton(config));

    // add a host component
    clusters.addHost(hostName);

    Host host = clusters.getHost(hostName);

    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6");
    host.setHostAttributes(hostAttributes);
    host.persist();

    String urlInfo = "[{'repositories':["
        + "{'Repositories/base_url':'http://foo1','Repositories/repo_name':'HDP','Repositories/repo_id':'HDP-2.1.1'}"
        + "], 'OperatingSystems/os_type':'redhat6'}]";

    m_helper.getOrCreateRepositoryVersion(HDP_21_STACK, HDP_2_2_0_0);
    repoVersionDAO.create(stackEntity, HDP_2_2_1_0, String.valueOf(System.currentTimeMillis()),
        "pack", urlInfo);

    c.createClusterVersion(HDP_21_STACK, HDP_2_2_0_0, "admin", RepositoryVersionState.UPGRADING);
    c.createClusterVersion(HDP_21_STACK, HDP_2_2_1_0, "admin", RepositoryVersionState.INSTALLING);

    c.transitionClusterVersion(HDP_21_STACK, HDP_2_2_0_0, RepositoryVersionState.CURRENT);
    c.transitionClusterVersion(HDP_21_STACK, HDP_2_2_1_0, RepositoryVersionState.INSTALLED);
    c.transitionClusterVersion(HDP_21_STACK, HDP_2_2_1_0, RepositoryVersionState.UPGRADING);
    c.transitionClusterVersion(HDP_21_STACK, HDP_2_2_1_0, RepositoryVersionState.UPGRADED);
    c.setCurrentStackVersion(HDP_21_STACK);

    c.mapHostVersions(Collections.singleton(hostName), c.getCurrentClusterVersion(),
        RepositoryVersionState.CURRENT);

    HostDAO hostDAO = m_injector.getInstance(HostDAO.class);

    HostVersionEntity entity = new HostVersionEntity();
    entity.setHostEntity(hostDAO.findByName(hostName));
    entity.setRepositoryVersion(repoVersionDAO.findByStackAndVersion(HDP_21_STACK, HDP_2_2_1_0));
    entity.setState(RepositoryVersionState.UPGRADED);
    hostVersionDAO.create(entity);
  }

  @Test
  public void testConfigActionUpgradeAcrossStack() throws Exception {
    makeUpgradeCluster();

    Cluster c = m_injector.getInstance(Clusters.class).getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setDesiredStackVersion(HDP_22_STACK);
    ConfigFactory cf = m_injector.getInstance(ConfigFactory.class);
    Config config = cf.createNew(c, "zoo.cfg", new HashMap<String, String>() {{
          put("initLimit", "10");
        }}, new HashMap<String, Map<String,String>>());
    config.setTag("version2");
    config.persist();

    c.addConfig(config);
    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());


    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("upgrade_direction", "upgrade");
    commandParams.put("version", HDP_2_2_1_0);
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY, "initLimit");
    commandParams.put(ConfigureTask.PARAMETER_VALUE, "11");

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null,
        null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(
        executionCommand));

    ConfigureAction action = m_injector.getInstance(ConfigureAction.class);
    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertEquals("version2", config.getTag());
    assertEquals("11", config.getProperties().get("initLimit"));

  }


}
