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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.ambari.server.state.StackId;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/** An exact definition consumer, including contexts which have no cluster yet. */
public record MpackScope(String stackName, String stackVersion, String serviceName,
    @JsonDeserialize(as = LinkedHashSet.class) Set<String> configTypes) {
  public MpackScope {
    MpackManifest.requireName(stackName);
    MpackManifest.requireVersion(stackVersion);
    if (serviceName != null) {
      MpackManifest.requireName(serviceName);
    }
    // Plan digests include array order; retain it when decoding across JVM restarts.
    configTypes.forEach(Objects::requireNonNull);
    configTypes = Collections.unmodifiableSet(new LinkedHashSet<>(configTypes));
  }

  public boolean contains(StackId stack, String service) {
    return stack != null && stackName.equals(stack.getStackName()) && stackVersion.equals(stack.getStackVersion())
        && (serviceName == null || service == null || serviceName.equals(service));
  }
}
