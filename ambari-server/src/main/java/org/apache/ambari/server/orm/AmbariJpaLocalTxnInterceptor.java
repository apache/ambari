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

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.google.inject.persist.UnitOfWork;
import com.google.inject.persist.jpa.AmbariJpaPersistService;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.eclipse.persistence.exceptions.EclipseLinkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import java.lang.reflect.Method;
import java.sql.SQLException;

public class AmbariJpaLocalTxnInterceptor implements MethodInterceptor {

  private static final Logger LOG = LoggerFactory.getLogger(AmbariJpaLocalTxnInterceptor.class);
  @Inject
  private final AmbariJpaPersistService emProvider = null;
  @Inject
  private final UnitOfWork unitOfWork = null;
  // Tracks if the unit of work was begun implicitly by this transaction.
  private final ThreadLocal<Boolean> didWeStartWork = new ThreadLocal<Boolean>();

  public Object invoke(MethodInvocation methodInvocation) throws Throwable {

    // Should we start a unit of work?
    if (!emProvider.isWorking()) {
      emProvider.begin();
      didWeStartWork.set(true);
    }

    Transactional transactional = readTransactionMetadata(methodInvocation);
    EntityManager em = this.emProvider.get();

    // Allow 'joining' of transactions if there is an enclosing @Transactional method.
    if (em.getTransaction().isActive()) {
      return methodInvocation.proceed();
    }

    final EntityTransaction txn = em.getTransaction();
    txn.begin();

    Object result;
    try {
      result = methodInvocation.proceed();

    } catch (Exception e) {
      //commit transaction only if rollback didn't occur
      if (rollbackIfNecessary(transactional, e, txn)) {
        txn.commit();
      }

      detailedLogForPersistenceError(e);

      //propagate whatever exception is thrown anyway
      throw e;
    } finally {
      // Close the em if necessary (guarded so this code doesn't run unless catch fired).
      if (null != didWeStartWork.get() && !txn.isActive()) {
        didWeStartWork.remove();
        unitOfWork.end();
      }
    }

    //everything was normal so commit the txn (do not move into try block above as it
    //  interferes with the advised method's throwing semantics)
    try {
      txn.commit();
    } catch (Exception e) {
      detailedLogForPersistenceError(e);
      throw e;
    } finally {
      //close the em if necessary
      if (null != didWeStartWork.get()) {
        didWeStartWork.remove();
        unitOfWork.end();
      }
    }

    //or return result
    return result;
  }

  private void detailedLogForPersistenceError(Exception e) {
    if (e instanceof PersistenceException) {
      PersistenceException rbe = (PersistenceException) e;
      Throwable cause = rbe.getCause();

      if (cause != null && cause instanceof EclipseLinkException) {
        EclipseLinkException de = (EclipseLinkException) cause;
        LOG.error("[DETAILED ERROR] Rollback reason: ", cause);
        Throwable internal = de.getInternalException();

        int exIndent = 1;
        if (internal != null && internal instanceof SQLException) {
          SQLException exception = (SQLException) internal;

          while (exception != null) {
            LOG.error("[DETAILED ERROR] Internal exception ("
                + exIndent
                + ") : ", exception); // Log the exception
            exception = exception.getNextException();
            exIndent++;
          }
        }
      }
    }
  }

  // TODO Cache this method's results.
  private Transactional readTransactionMetadata(MethodInvocation methodInvocation) {
    Transactional transactional;
    Method method = methodInvocation.getMethod();
    Class<?> targetClass = methodInvocation.getThis().getClass();

    transactional = method.getAnnotation(Transactional.class);
    if (null == transactional) {
      // If none on method, try the class.
      transactional = targetClass.getAnnotation(Transactional.class);
    }
    if (null == transactional) {
      // If there is no transactional annotation present, use the default
      transactional = Internal.class.getAnnotation(Transactional.class);
    }

    return transactional;
  }

  /**
   * Returns True if rollback DID NOT HAPPEN (i.e. if commit should continue).
   *
   * @param transactional The metadata annotaiton of the method
   * @param e             The exception to test for rollback
   * @param txn           A JPA Transaction to issue rollbacks on
   */
  private boolean rollbackIfNecessary(Transactional transactional, Exception e,
                                      EntityTransaction txn) {
    boolean commit = true;

    //check rollback clauses
    for (Class<? extends Exception> rollBackOn : transactional.rollbackOn()) {

      //if one matched, try to perform a rollback
      if (rollBackOn.isInstance(e)) {
        commit = false;

        //check ignore clauses (supercedes rollback clause)
        for (Class<? extends Exception> exceptOn : transactional.ignore()) {
          //An exception to the rollback clause was found, DON'T rollback
          // (i.e. commit and throw anyway)
          if (exceptOn.isInstance(e)) {
            commit = true;
            break;
          }
        }

        //rollback only if nothing matched the ignore check
        if (!commit) {
          txn.rollback();
        }
        //otherwise continue to commit

        break;
      }
    }

    return commit;
  }

  @Transactional
  private static class Internal {
  }
}
