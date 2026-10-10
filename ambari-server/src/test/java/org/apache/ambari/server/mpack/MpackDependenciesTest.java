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
package org.apache.ambari.server.mpack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class MpackDependenciesTest {
  private MpackDependencies.Provider provider(String name, String version, String dependency) {
    ObjectNode root = MpackManifestTest.manifest().put("name", name).put("version", version);
    if (dependency != null) {
      root.putArray("dependencies").addObject().put("name", dependency).put("min_version", "1.0");
    }
    return new MpackDependencies.Provider(MpackManifestTest.parse(root), MpackJson.digest(root));
  }

  @Test
  public void pinsCompleteClosureInDependencyOrder() {
    MpackDependencies.Provider base = provider("generic-base", "1.0", null);
    MpackDependencies.Provider nginx = provider("nginx", "1.0", "generic-base");
    List<MpackDependencies.Resolution> result = MpackDependencies.resolve(List.of(nginx), List.of(base));
    assertEquals(List.of(base.identity(), nginx.identity()),
        result.stream().map(value -> value.provider().release()).toList());
    assertEquals(new MpackDependencies.Reference(base.identity(), base.digest()),
        result.get(1).dependencies().get("generic-base"));
  }

  @Test
  public void rejectsMissingAmbiguousAndCyclicProviders() {
    MpackDependencies.Provider nginx = provider("nginx", "1.0", "generic-base");
    assertEquals(MpackException.Code.DEPENDENCY_MISSING,
        assertThrows(MpackException.class, () -> MpackDependencies.resolve(List.of(nginx), List.of())).getCode());
    assertEquals(MpackException.Code.DEPENDENCY_AMBIGUOUS,
        assertThrows(MpackException.class, () -> MpackDependencies.resolve(List.of(nginx),
            List.of(provider("generic-base", "1.0", null), provider("generic-base", "2.0", null)))).getCode());
    assertEquals(MpackException.Code.DEPENDENCY_CYCLE,
        assertThrows(MpackException.class, () -> MpackDependencies.resolve(List.of(nginx),
            List.of(provider("generic-base", "1.0", "nginx")))).getCode());
  }

  @Test
  public void rejectsSameReleaseWithDifferentDigest() {
    MpackDependencies.Provider base = provider("generic-base", "1.0", null);
    MpackDependencies.Provider foreign = new MpackDependencies.Provider(base.manifest(), "0".repeat(64));
    assertEquals(MpackException.Code.RELEASE_CONFLICT,
        assertThrows(MpackException.class,
            () -> MpackDependencies.resolve(List.of(foreign), List.of(base))).getCode());
  }

  @Test
  public void protectsConsumersOutsideSubmittedBundleAndAllowsRetainedOldProvider() {
    MpackDependencies.Provider oldBase = provider("generic-base", "1.0", null);
    MpackDependencies.Provider newBase = provider("generic-base", "2.0", null);
    MpackDependencies.Provider nginx = provider("nginx", "1.0", "generic-base");
    List<MpackDependencies.Resolution> existing = MpackDependencies.resolve(List.of(nginx), List.of(oldBase));
    MpackException error = assertThrows(MpackException.class,
        () -> MpackDependencies.verifyRetainedReferences(existing, List.of(newBase, nginx), Set.of()));
    assertEquals(MpackException.Code.RESOURCE_IN_USE, error.getCode());
    assertEquals(List.of(Map.of("consumer", nginx.identity(), "provider", oldBase.identity(),
        "digest", oldBase.digest())), error.getDetails().get("blockers"));
    MpackDependencies.verifyRetainedReferences(existing, List.of(oldBase, newBase, nginx), Set.of());
    MpackDependencies.verifyRetainedReferences(existing, List.of(newBase), Set.of(nginx.identity()));
  }
}
