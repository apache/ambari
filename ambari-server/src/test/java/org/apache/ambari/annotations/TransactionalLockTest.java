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
package org.apache.ambari.annotations;

import java.util.Properties;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.ambari.annotations.TransactionalLock.LockArea;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.AmbariJpaLocalTxnInterceptor;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.TransactionalLockInterceptor;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.easymock.EasyMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.Transactional;
import com.google.inject.persist.jpa.AmbariJpaPersistModule;
import com.google.inject.persist.jpa.AmbariJpaPersistService;

import junit.framework.Assert;

/**
 * Tests {@link TransactionalLock} and associated classes.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(
    value = { HostRoleCommandDAO.class, AmbariJpaLocalTxnInterceptor.class,
        TransactionalLockInterceptor.class, AmbariJpaPersistModule.class,
        AmbariJpaPersistService.class })
@PowerMockIgnore("javax.management.*")
public class TransactionalLockTest {

  /**
   * Tests that {@link LockArea} is correctly enabled/disabled.
   */
  @Test
  public void testLockAreaEnabled() throws Exception {
    final Properties ambariProperties = new Properties();
    ambariProperties.put(Configuration.SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED, "true");
    Configuration configuration = new Configuration(ambariProperties);

    LockArea lockArea = LockArea.HRC_STATUS_CACHE;
    lockArea.clearEnabled();

    Assert.assertTrue(lockArea.isEnabled(configuration));
  }

  /**
   * Tests that {@link LockArea} is correctly enabled/disabled.
   */
  @Test
  public void testLockAreaEnabledDisabled() throws Exception {
    final Properties ambariProperties = new Properties();
    ambariProperties.put(Configuration.SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED, "false");
    Configuration configuration = new Configuration(ambariProperties);

    LockArea lockArea = LockArea.HRC_STATUS_CACHE;
    lockArea.clearEnabled();

    Assert.assertFalse(lockArea.isEnabled(configuration));
  }

  /**
   * Tests that the {@link Transactional} and {@link TransactionalLock}
   * annotations cause the interceptors to be called in the correct order.
   *
   * @throws Throwable
   */
  @Test
  public void testTransactionLockOrdering() throws Throwable {
    AmbariJpaLocalTxnInterceptor ambariJPAInterceptor = PowerMock.createNiceMock(
        AmbariJpaLocalTxnInterceptor.class);

    TransactionalLockInterceptor lockInterceptor = PowerMock.createNiceMock(
        TransactionalLockInterceptor.class);

    PowerMockito.whenNew(AmbariJpaLocalTxnInterceptor.class).withAnyArguments().thenReturn(
        ambariJPAInterceptor);

    PowerMockito.whenNew(TransactionalLockInterceptor.class).withAnyArguments().thenReturn(
        lockInterceptor);

    Object object = new Object();

    EasyMock.expect(lockInterceptor.invoke(EasyMock.anyObject(MethodInvocation.class))).andReturn(
        object).once();

    EasyMock.expect(
        ambariJPAInterceptor.invoke(EasyMock.anyObject(MethodInvocation.class))).andReturn(
            object).once();

    EasyMock.replay(ambariJPAInterceptor, lockInterceptor);

    Injector injector = Guice.createInjector(new InMemoryDefaultTestModule());
    HostRoleCommandDAO hostRoleCommandDAO = injector.getInstance(HostRoleCommandDAO.class);
    hostRoleCommandDAO.create(new HostRoleCommandEntity());

    EasyMock.verify(lockInterceptor);
  }

}
