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
package org.apache.ambari.server.orm;

import com.google.inject.persist.Transactional;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.EntityTransaction;

import java.io.FileNotFoundException;
import java.io.IOException;

import static org.apache.ambari.server.orm.AmbariJpaLocalTxnInterceptor.rollbackIfNecessary;
import static org.easymock.EasyMock.expect;

public class AmbariJpaLocalTxnInterceptorTest extends EasyMockSupport {

  @Test
  public void canBeCommittedIfExceptionsToBeRolledBackOnIsEmpty() {
    Transactional transactional = createNiceMock(Transactional.class);
    EntityTransaction transaction = createStrictMock(EntityTransaction.class);

    expect(transaction.getRollbackOnly()).andReturn(false);
    expect(transactional.rollbackOn()).andReturn(new Class[0]);

    replayAll();

    boolean canCommit = rollbackIfNecessary(transactional, new RuntimeException(), transaction);
    Assert.assertTrue("Should be allowed to commit, since rollbackOn clause is empty", canCommit);

    verifyAll();
  }

  @Test
  public void canBeCommittedIfUnknownExceptionThrown() {
    Transactional transactional = createNiceMock(Transactional.class);
    EntityTransaction transaction = createStrictMock(EntityTransaction.class);

    expect(transaction.getRollbackOnly()).andReturn(false);
    expect(transactional.rollbackOn()).andReturn(new Class[] { IllegalArgumentException.class });

    replayAll();

    boolean canCommit = rollbackIfNecessary(transactional, new RuntimeException(), transaction);
    Assert.assertTrue("Should be allowed to commit, exception thrown does not match rollbackOn clause", canCommit);

    verifyAll();
  }

  @Test
  public void rolledBackForKnownException() {
    Transactional transactional = createNiceMock(Transactional.class);
    EntityTransaction transaction = createStrictMock(EntityTransaction.class);

    expect(transaction.getRollbackOnly()).andReturn(false);
    expect(transactional.rollbackOn()).andReturn(new Class[] {
      NullPointerException.class, IllegalArgumentException.class
    });
    expect(transactional.ignore()).andReturn(new Class[0]);
    transaction.rollback();

    replayAll();

    boolean canCommit = rollbackIfNecessary(transactional, new IllegalArgumentException("rolling back"), transaction);
    Assert.assertFalse("Should be rolled back, since exception matches rollbackOn clause", canCommit);

    verifyAll();
  }

  @Test
  public void rolledBackForSubclassOfKnownException() {
    Transactional transactional = createNiceMock(Transactional.class);
    EntityTransaction transaction = createStrictMock(EntityTransaction.class);

    expect(transaction.getRollbackOnly()).andReturn(false);
    expect(transactional.rollbackOn()).andReturn(new Class[] { RuntimeException.class });
    expect(transactional.ignore()).andReturn(new Class[0]);
    transaction.rollback();

    replayAll();

    boolean canCommit = rollbackIfNecessary(transactional, new IllegalArgumentException("rolling back"), transaction);
    Assert.assertFalse("Should be rolled back, since exception is subclass of the one in rollbackOn clause", canCommit);

    verifyAll();
  }

  @Test
  public void canBeCommittedIfIgnoredExceptionThrown() {
    Transactional transactional = createNiceMock(Transactional.class);
    EntityTransaction transaction = createStrictMock(EntityTransaction.class);

    expect(transaction.getRollbackOnly()).andReturn(false);
    expect(transactional.rollbackOn()).andReturn(new Class[] { IllegalArgumentException.class });
    expect(transactional.ignore()).andReturn(new Class[] { NumberFormatException.class });

    replayAll();

    boolean canCommit = rollbackIfNecessary(transactional, new NumberFormatException("rolling back"), transaction);
    Assert.assertTrue("Should be allowed to commit, since ignored exception was thrown", canCommit);

    verifyAll();
  }

  @Test
  public void canBeCommittedIfSubclassOfIgnoredExceptionThrown() {
    Transactional transactional = createNiceMock(Transactional.class);
    EntityTransaction transaction = createStrictMock(EntityTransaction.class);

    expect(transaction.getRollbackOnly()).andReturn(false);
    expect(transactional.rollbackOn()).andReturn(new Class[] { Exception.class });
    expect(transactional.ignore()).andReturn(new Class[] { IOException.class });

    replayAll();

    boolean canCommit = rollbackIfNecessary(transactional, new FileNotFoundException("rolling back"), transaction);
    Assert.assertTrue("Should be allowed to commit, since subclass of ignored exception was thrown", canCommit);

    verifyAll();
  }

  @Test
  public void rolledBackIfTransactionMarkedRollbackOnly() {
    Transactional transactional = createNiceMock(Transactional.class);
    EntityTransaction transaction = createStrictMock(EntityTransaction.class);

    expect(transaction.getRollbackOnly()).andReturn(true);
    transaction.rollback();

    replayAll();

    boolean canCommit = rollbackIfNecessary(transactional, null, transaction);
    Assert.assertFalse("Should be rolled back, since transaction was marked rollback-only", canCommit);

    verifyAll();
  }

}
