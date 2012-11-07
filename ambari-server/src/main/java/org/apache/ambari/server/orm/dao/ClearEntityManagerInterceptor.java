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

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

/**
 * Interceptors clears EntityManager before method call
 */
public class ClearEntityManagerInterceptor implements MethodInterceptor {
  private static final Logger log = LoggerFactory.getLogger(ClearEntityManagerInterceptor.class);

  @Inject
  Provider<EntityManager> entityManagerProvider;

  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    try {
      return methodInvocation.proceed();
    } finally {
      if (!entityManagerProvider.get().getTransaction().isActive()) {
        log.debug("Transaction is not active any more - clearing Entity Manager");
        entityManagerProvider.get().clear();
      }
    }
  }
}
