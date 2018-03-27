/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.ambari.server.controller.internal;

import static java.util.Collections.singletonList;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationCategory.SSO_CONFIGURATION;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.SSO_ENABED_SERVICES;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(EasyMockRunner.class)
public class AmbariServerConfigurationHandlerTest extends EasyMockSupport {
  @Mock
  private AmbariConfigurationDAO ambariConfigurationDAO;
  private AmbariServerConfigurationHandler ambariServerConfigurationHandler;

  @Before
  public void setUp() throws Exception {
    ambariServerConfigurationHandler = new AmbariServerConfigurationHandler();
    AmbariServerConfigurationHandler.ambariConfigurationDAO = ambariConfigurationDAO;
  }

  @Test
  public void testCheckingIfSsoIsEnabledPerEachService() {
    expect(ambariConfigurationDAO.findByCategory(SSO_CONFIGURATION.getCategoryName())).andReturn(singletonList(ssoConfig("SERVICE1, SERVICE2"))).anyTimes();
    replayAll();
    assertTrue(ambariServerConfigurationHandler.getSsoEnabledSevices().contains("SERVICE1"));
    assertTrue(ambariServerConfigurationHandler.getSsoEnabledSevices().contains("SERVICE2"));
    assertFalse(ambariServerConfigurationHandler.getSsoEnabledSevices().contains("SERVICE3"));
  }

  private AmbariConfigurationEntity ssoConfig(String services) {
    AmbariConfigurationEntity entity = new AmbariConfigurationEntity();
    entity.setCategoryName(SSO_CONFIGURATION.getCategoryName());
    entity.setPropertyName(SSO_ENABED_SERVICES.key());
    entity.setPropertyValue(services);
    return entity;
  }
}