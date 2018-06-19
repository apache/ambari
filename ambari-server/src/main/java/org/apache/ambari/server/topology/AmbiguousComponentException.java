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
package org.apache.ambari.server.topology;

import java.util.Set;

import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.commons.lang3.tuple.Pair;

public class AmbiguousComponentException extends IllegalStateException {

  AmbiguousComponentException(Set<Pair<StackId, ServiceInfo>> serviceMatches) {
    super(formatResolutionProblemMessage(serviceMatches));
  }

  private static String formatResolutionProblemMessage(Set<Pair<StackId, ServiceInfo>> serviceMatches) {
    if (serviceMatches.isEmpty()) {
      return "No service found";
    }

    return "Multiple services found: " + serviceMatches;
  }

}
