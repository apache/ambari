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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.ambari.annotations.TransactionalLock;
import org.apache.ambari.annotations.TransactionalLock.LockArea;
import org.apache.ambari.annotations.TransactionalLock.LockType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

/**
 * The {@link TransactionalLockInterceptor} is a method level intercept which
 * will use the properties of {@link TransactionalLock} to acquire a
 * {@link ReadWriteLock} around a particular {@link LockArea}.
 * <p/>
 * It is mainly used to provide a lock around an method annotated with
 * {@link Transactional}. Consider the case where an action must happen after a
 * method has completed and the transaction has been committed.
 */
public class TransactionalLockInterceptor implements MethodInterceptor {

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(TransactionalLockInterceptor.class);

  /**
   * Used to ensure that methods which rely on the completion of
   * {@link Transactional} can detect when they are able to run.
   *
   * @see TransactionalLock
   */
  @Inject
  private final TransactionalLocks transactionLocks = null;

  /**
   * {@inheritDoc}
   */
  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {

    TransactionalLock annotation = methodInvocation.getMethod().getAnnotation(
        TransactionalLock.class);

    LockArea lockArea = annotation.lockArea();
    LockType lockType = annotation.lockType();

    ReadWriteLock rwLock = transactionLocks.getLock(lockArea);
    Lock lock = lockType == LockType.READ ? rwLock.readLock() : rwLock.writeLock();

    lock.lock();

    try {
      Object object = methodInvocation.proceed();
      return object;
    } finally {
      lock.unlock();
    }
  }
}