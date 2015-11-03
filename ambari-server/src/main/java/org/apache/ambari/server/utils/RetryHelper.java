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
package org.apache.ambari.server.utils;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.Cluster;
import org.eclipse.persistence.exceptions.DatabaseException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides utility methods to support operations retry
 * TODO injection as Guice singleon, static for now to avoid major modifications
 */
public class RetryHelper {
  private static ThreadLocal<Set<Cluster>> affectedClusters = new ThreadLocal<Set<Cluster>>(){
    @Override
    protected Set<Cluster> initialValue() {
      return new HashSet<>();
    }
  };

  private static int apiRetryAttempts = 0;
  private static int blueprintsRetryAttempts = 0;

  public static void init(int apiOperationsRetryAttempts, int blueprintOperationsRetryAttempts) {
    apiRetryAttempts = apiOperationsRetryAttempts;
    blueprintsRetryAttempts = blueprintOperationsRetryAttempts;
  }

  public static void addAffectedCluster(Cluster cluster) {
    if (apiRetryAttempts > 0 || blueprintsRetryAttempts > 0) {
      affectedClusters.get().add(cluster);
    }
  }

  public static Set<Cluster> getAffectedClusters() {
    return Collections.unmodifiableSet(affectedClusters.get());
  }

  public static void clearAffectedClusters() {
    if (apiRetryAttempts > 0 || blueprintsRetryAttempts > 0) {
      affectedClusters.get().clear();
    }
  }

  public static int getApiOperationsRetryAttempts() {
    return apiRetryAttempts;
  }

  public static boolean isDatabaseException(Throwable ex) {
    do {
      if (ex instanceof DatabaseException) {
        return true;
      }
      ex = ex.getCause();

    } while (ex != null);

    return false;
  }

  public static void invalidateAffectedClusters() {
    for (Cluster cluster : affectedClusters.get()) {
      cluster.invalidateData();
    }
  }
}
