/*
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.util.List;

import jakarta.persistence.OptimisticLockException;

import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.entities.MpackRecordEntity.Kind;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;

public class MpackRecordDAOTest {
  private Injector injector;
  private MpackRecordDAO dao;

  @Before
  public void setUp() {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.getInstance(UnitOfWork.class).begin();
    dao = injector.getInstance(MpackRecordDAO.class);
  }

  @After
  public void tearDown() {
    injector.getInstance(UnitOfWork.class).end();
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void persistsAcceptanceAndIdempotencyTogether() {
    dao.apply(List.of(
        new MpackRecordDAO.Change("operation/one", Kind.OPERATION, -1, "{\"phase\":\"ACCEPTED\"}"),
        new MpackRecordDAO.Change("idempotency/one", Kind.IDEMPOTENCY, -1, "{\"operation\":\"one\"}")));
    assertEquals(0, dao.find("operation/one").revision());
    assertEquals("{\"operation\":\"one\"}", dao.find("idempotency/one").payload());
    assertEquals(1, dao.list(Kind.OPERATION).size());
  }

  @Test
  public void staleRevisionRollsBackEveryRecordInTheTransaction() {
    dao.apply(List.of(new MpackRecordDAO.Change("z/control", Kind.CONTROL, -1, "{}")));
    assertThrows(OptimisticLockException.class, () -> dao.apply(List.of(
        new MpackRecordDAO.Change("a/operation", Kind.OPERATION, -1, "{}"),
        new MpackRecordDAO.Change("z/control", Kind.CONTROL, 1, "{\"changed\":true}"))));
    injector.getInstance(UnitOfWork.class).end();
    injector.getInstance(UnitOfWork.class).begin();
    assertNull(dao.find("a/operation"));
    assertEquals(0, dao.find("z/control").revision());
    assertEquals("{}", dao.find("z/control").payload());
  }

  @Test
  public void preventsKindChangesAndDuplicateCreation() {
    dao.apply(List.of(new MpackRecordDAO.Change("release/one", Kind.RELEASE, -1, "{}")));
    assertThrows(IllegalStateException.class, () -> dao.apply(List.of(
        new MpackRecordDAO.Change("release/one", Kind.OPERATION, 0, "{}"))));
    assertThrows(OptimisticLockException.class, () -> dao.apply(List.of(
        new MpackRecordDAO.Change("release/one", Kind.RELEASE, -1, "{}"))));
    assertEquals(Kind.RELEASE, dao.find("release/one").kind());
  }
}
