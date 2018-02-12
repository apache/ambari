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


import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.google.inject.Inject;
import com.google.inject.persist.jpa.AmbariJpaPersistService;

/**
 * AOP interceptor to provide session borders
 */
public class AmbariLocalSessionInterceptor implements MethodInterceptor {

  @Inject
  private AmbariJpaPersistService emProvider;

  private final ThreadLocal<Boolean> didWeStartWork = new ThreadLocal<>();

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    if (!emProvider.isWorking()) {
      emProvider.begin();
      didWeStartWork.set(true);

      try {
        return invocation.proceed();
      } finally {
        if (null != didWeStartWork.get()) {
          didWeStartWork.remove();
          emProvider.end();
        }
      }

    } else {
      //if session was in progress just proceed without additional checks
      return invocation.proceed();
    }




  }
}
