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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class MpackManifestTest {
  static ObjectNode manifest() {
    ObjectNode root = MpackJson.object();
    root.put("schema_version", 1);
    root.put("type", "full-release");
    root.put("name", "nginx");
    root.put("version", "1.0.0.0");
    root.putArray("artifacts").addObject().put("name", "nginx-services")
        .put("type", "extension-definitions").put("source_dir", "extensions");
    return root;
  }

  static MpackManifest parse(ObjectNode root) {
    return MpackManifest.parse(root.toString().getBytes(StandardCharsets.UTF_8));
  }

  @Test
  public void acceptsAllFourArtifactTypes() {
    ObjectNode root = manifest();
    root.putArray("artifacts");
    int index = 0;
    for (String type : MpackManifest.ARTIFACT_TYPES) {
      ObjectNode artifact = root.withArray("artifacts").addObject()
          .put("name", "artifact-" + index).put("type", type)
          .put("source_dir", "source-" + index++);
      if (type.equals("stack-addon-service-definitions")) {
        artifact.putArray("service_versions_map").addObject()
            .put("service_name", "NGINX").put("service_version", "1.0")
            .putArray("applicable_stacks").addObject()
            .put("stack_name", "GENERIC").put("stack_version", "1.0");
      }
    }
    assertEquals(4, parse(root).artifacts().size());
  }

  @Test
  public void rejectsDuplicateJsonKeysAndTrailingValues() {
    assertThrows(MpackException.class, () -> MpackJson.read("{\"a\":1,\"a\":2}"));
    assertThrows(MpackException.class, () -> MpackJson.read("{} {}"));
    assertThrows(MpackException.class, () -> MpackJson.read("diagnostic noise\n{}"));
  }

  @Test
  public void rejectsMissingOrWrongSchemaType() {
    ObjectNode root = manifest();
    root.remove("schema_version");
    assertEquals(MpackException.Code.UNSUPPORTED_SCHEMA,
        assertThrows(MpackException.class, () -> parse(root)).getCode());
    root.put("schema_version", "1");
    assertThrows(MpackException.class, () -> parse(root));
    root.put("schema_version", 2);
    assertThrows(MpackException.class, () -> parse(root));
  }

  @Test
  public void rejectsUnknownArtifactRatherThanSkippingIt() {
    ObjectNode root = manifest();
    ((ObjectNode) root.withArray("artifacts").get(0)).put("type", "typo");
    assertEquals(MpackException.Code.INVALID_MANIFEST,
        assertThrows(MpackException.class, () -> parse(root)).getCode());
  }

  @Test
  public void rejectsOverlappingArtifacts() {
    ObjectNode root = manifest();
    root.withArray("artifacts").addObject().put("name", "nested")
        .put("type", "service-definitions").put("source_dir", "extensions/nested");
    assertThrows(MpackException.class, () -> parse(root));
  }

  @Test
  public void rejectsUnsafePathsAndAmbiguousVersions() {
    for (String value : new String[] {"../x", "/x", "x/../y", "x//y", "x\\y", "C:x", "x/", "x\u0000y"}) {
      assertThrows(MpackException.class, () -> MpackManifest.requirePath(value));
    }
    for (String value : new String[] {"01.0", "1.2alpha", "1..2", "1.0 ", "1.2.3.4.5.6"}) {
      assertThrows(MpackException.class, () -> MpackManifest.requireVersion(value));
    }
  }

  @Test
  public void comparesNumericVersionsWithoutLexicalOrderingOrOverflow() {
    assertTrue(MpackManifest.compareVersions("1.10", "1.9") > 0);
    assertEquals(0, MpackManifest.compareVersions("1.0", "1.0.0.0"));
    assertTrue(MpackManifest.compareVersions("9999999999.0", "2147483647.0") > 0);
  }

  @Test
  public void enforcesBothAmbariVersionBounds() {
    ObjectNode root = manifest();
    root.putObject("prerequisites").put("min_ambari_version", "3.0")
        .put("max_ambari_version", "3.1");
    MpackManifest parsed = parse(root);
    parsed.verifyAmbariVersion("3.1.0");
    assertEquals(MpackException.Code.VERSION_INCOMPATIBLE,
        assertThrows(MpackException.class, () -> parsed.verifyAmbariVersion("3.2")).getCode());
    assertThrows(MpackException.class, () -> parsed.verifyAmbariVersion("2.7"));
  }

  @Test
  public void validatesPinnedDependencyAndDigest() {
    ObjectNode root = manifest();
    String digest = "a".repeat(64);
    root.putArray("dependencies").addObject().put("name", "generic-base")
        .put("version", "1.0").put("digest", digest);
    MpackManifest.Dependency dependency = parse(root).dependencies().get(0);
    assertTrue(dependency.accepts("1.0", digest));
    assertFalse(dependency.accepts("1.1", digest));
    assertFalse(dependency.accepts("1.0", "b".repeat(64)));
  }

  @Test
  public void rejectsSelfDependencyAndInvalidRange() {
    ObjectNode root = manifest();
    ObjectNode dependency = root.putArray("dependencies").addObject().put("name", "nginx")
        .put("min_version", "2.0").put("max_version", "1.0");
    assertThrows(MpackException.class, () -> parse(root));
    dependency.put("name", "generic-base");
    assertThrows(MpackException.class, () -> parse(root));
  }

  @Test
  public void requiresExplicitHookRecoveryAndTimeoutPolicy() {
    ObjectNode root = manifest();
    ObjectNode hook = root.putArray("hooks").addObject().put("name", "after-install")
        .put("type", "python").put("script", "hooks/install.py");
    assertThrows(MpackException.class, () -> parse(root));
    hook.put("timeout_seconds", 60).put("idempotent", false);
    assertFalse(parse(root).hooks().get(0).idempotent());
    hook.put("timeout_seconds", 0);
    assertThrows(MpackException.class, () -> parse(root));
  }

  @Test
  public void canonicalDigestIgnoresObjectOrderingButNotValues() {
    assertEquals(MpackJson.digest(MpackJson.read("{\"a\":1,\"b\":2}")),
        MpackJson.digest(MpackJson.read("{\"b\":2,\"a\":1}")));
    assertFalse(MpackJson.digest(MpackJson.read("{\"a\":1}"))
        .equals(MpackJson.digest(MpackJson.read("{\"a\":2}"))));
  }
}
