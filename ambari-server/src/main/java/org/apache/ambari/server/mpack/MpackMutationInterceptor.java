/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.mpack;

import java.util.Collection;
import java.util.concurrent.locks.Lock;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.ambari.server.orm.AmbariJpaLocalTxnInterceptor;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;

import com.google.inject.Inject;

/** Protects domain mutations, including callers which do not use the HTTP API. */
public class MpackMutationInterceptor implements MethodInterceptor {
  @Inject private MpackRuntime runtime;

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    Lock read = runtime.readLock();
    if (!read.tryLock()) {
      throw new MpackException(MpackException.Code.OPERATION_CONFLICT,
          "Definition publication is committing; retry this mutation");
    }
    try {
      if (AmbariJpaLocalTxnInterceptor.isTransactionActive()) {
        AmbariJpaLocalTxnInterceptor.holdLockUntilTransactionCompletion(read);
      }
      if (invocation.getMethod().getAnnotation(MpackMutation.class).value() == MpackMutation.Kind.TASK_CONTROL) {
        return invocation.proceed();
      }
      Object target = invocation.getThis();
      if (target instanceof Service service) {
        runtime.requireServiceReady(service.getDesiredStackId(), service.getName());
      } else if (target instanceof ServiceComponent component) {
        runtime.requireServiceReady(component.getDesiredStackId(), component.getServiceName());
      } else if (target instanceof Cluster cluster) {
        MpackMutation.Kind kind = invocation.getMethod().getAnnotation(MpackMutation.class).value();
        if (kind == MpackMutation.Kind.CONFIGURATION) {
          if (invocation.getArguments()[1] != null) {
            for (Object value : (Collection<?>) invocation.getArguments()[1]) {
              if (value != null) runtime.requireConfigurationReady(cluster.getDesiredStackVersion(), ((Config) value).getType());
            }
          }
        } else {
          runtime.requireServiceReady(cluster.getDesiredStackVersion(),
              kind == MpackMutation.Kind.STACK ? null : (String) invocation.getArguments()[0]);
          if (kind == MpackMutation.Kind.STACK && invocation.getArguments()[0] instanceof org.apache.ambari.server.state.StackId stack) {
            runtime.requireServiceReady(stack, null);
          }
        }
      } else if (target instanceof org.apache.ambari.server.state.Clusters clusters) {
        runtime.requireServiceReady(clusters.getCluster((String) invocation.getArguments()[0]).getDesiredStackVersion(), null);
      } else {
        throw new IllegalStateException("Unsupported definition mutation boundary");
      }
      return invocation.proceed();
    } finally {
      read.unlock();
    }
  }
}
