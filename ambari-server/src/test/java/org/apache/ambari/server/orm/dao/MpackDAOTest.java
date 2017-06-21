/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.orm.dao;

import com.google.inject.Injector;
import com.google.inject.Guice;
import org.apache.ambari.server.orm.entities.MpackEntity;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import com.google.inject.persist.UnitOfWork;

import java.util.List;
import java.util.ArrayList;

/**
 * Tests {@link MpackDAO}.
 */
public class MpackDAOTest {
  private Injector m_injector;
  private MpackDAO m_dao;

  @Before
  public void init() {
    m_injector = Guice.createInjector(new InMemoryDefaultTestModule());
    m_injector.getInstance(GuiceJpaInitializer.class);
    m_injector.getInstance(UnitOfWork.class).begin();
    m_dao = m_injector.getInstance(MpackDAO.class);
  }

  @Test
  public void testCreateFind() {
    List<MpackEntity> eDefinitions = new ArrayList<>();

    // create 2 definitions
    for (int i = 1; i < 3; i++) {
      MpackEntity definition = new MpackEntity();
      definition.setMpackId(new Long(100)+i);
      definition.setMpackName("testMpack" + i);
      definition.setRegistryId(Long.valueOf(i));
      definition.setMpackVersion("3.0.0.0-12"+i);
      definition.setMpackUri("http://c6401.ambari.apache.org:8080/resources/mpacks-repo/testMpack" + i + "-3.0.0.0-123.tar.gz");
      eDefinitions.add(definition);
      m_dao.create(definition);
    }

    List<MpackEntity> definitions = m_dao.findAll();
    assertNotNull(definitions);
    assertEquals(2, definitions.size());
    definitions = m_dao.findByNameVersion("testMpack1","3.0.0.0-121");
    assertEquals(1, definitions.size());
    assertEquals(new Long(101),(Long)definitions.get(0).getMpackId());
    MpackEntity entity = m_dao.findById(new Long(102));
    assertEquals(entity.getMpackName(),"testMpack2");
    assertEquals(entity.getMpackVersion(),"3.0.0.0-122");

  }
}
