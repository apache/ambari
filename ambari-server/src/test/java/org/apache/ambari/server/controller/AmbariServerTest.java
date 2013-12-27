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

package org.apache.ambari.server.controller;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.entities.MetainfoEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.fail;

public class AmbariServerTest {

  private static final Logger log = LoggerFactory.getLogger(AmbariServerTest.class);
  private Injector injector;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
  }

  @After
  public void teardown() throws AmbariException {
  }

  @Test
  public void testCheckDBVersion_Valid() throws Exception {
    MetainfoDAO metainfoDAO =  createMock(MetainfoDAO.class);
    MetainfoEntity metainfoEntity = new MetainfoEntity();
    String serverVersion = ambariMetaInfo.getServerVersion();
    metainfoEntity.setMetainfoName(Configuration.SERVER_VERSION_KEY);
    metainfoEntity.setMetainfoValue(serverVersion);
    expect(metainfoDAO.findByKey(Configuration.SERVER_VERSION_KEY)).
            andReturn(metainfoEntity);
    replay(metainfoDAO);
    AmbariServer ambariServer = new AmbariServer();
    ambariServer.metainfoDAO = metainfoDAO;
    ambariServer.ambariMetaInfo = ambariMetaInfo;
    ambariServer.checkDBVersion();
  }

  @Test
  public void testCheckDBVersion_Invalid() throws Exception {
    MetainfoDAO metainfoDAO =  createMock(MetainfoDAO.class);
    MetainfoEntity metainfoEntity = new MetainfoEntity();
    metainfoEntity.setMetainfoName(Configuration.SERVER_VERSION_KEY);
    metainfoEntity.setMetainfoValue("0.0.0"); // Incompatible version
    expect(metainfoDAO.findByKey(Configuration.SERVER_VERSION_KEY)).
            andReturn(metainfoEntity);
    replay(metainfoDAO);
    AmbariServer ambariServer = new AmbariServer();
    ambariServer.metainfoDAO = metainfoDAO;
    ambariServer.ambariMetaInfo = ambariMetaInfo;

    try {
      ambariServer.checkDBVersion();
      fail();
    } catch(AmbariException e) {
      // Expected
    }
  }


}
