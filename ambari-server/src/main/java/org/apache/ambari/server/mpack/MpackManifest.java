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

import static org.apache.ambari.server.mpack.MpackJson.invalid;
import static org.apache.ambari.server.mpack.MpackJson.optionalString;
import static org.apache.ambari.server.mpack.MpackJson.string;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

/** V1 artifact format with an explicit, strictly validated lifecycle schema. */
public record MpackManifest(String name, String version, String description,
    List<Artifact> artifacts, List<Dependency> dependencies, List<Hook> hooks,
    String minAmbariVersion, String maxAmbariVersion, List<Target> minStackVersions,
    String canonicalJson) {
  public static final int SCHEMA_VERSION = 1;
  public static final int MAX_MANIFEST_BYTES = 1024 * 1024;
  public static final Set<String> ARTIFACT_TYPES = Set.of("stack-definitions",
      "service-definitions", "extension-definitions", "stack-addon-service-definitions");
  private static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{0,127}");
  private static final Pattern VERSION = Pattern.compile("(?:0|[1-9][0-9]{0,9})(?:\\.(?:0|[1-9][0-9]{0,9})){0,4}");
  private static final Pattern DIGEST = Pattern.compile("[a-f0-9]{64}");
  private static final Set<String> HOOK_PHASES = Set.of("before-install", "after-install",
      "before-upgrade", "after-upgrade", "before-uninstall", "after-uninstall");

  public record Target(String stackName, String stackVersion) {
    public Target {
      requireName(stackName);
      requireVersion(stackVersion);
    }

    public String identity() {
      return stackName + "/" + stackVersion;
    }
  }

  public record Addon(String serviceName, String serviceVersion, List<Target> targets) {
    public Addon {
      targets = List.copyOf(targets);
    }
  }

  public record Artifact(String name, String type, String sourceDir, List<Addon> addons) {
    public Artifact {
      addons = List.copyOf(addons);
    }
  }

  public record Dependency(String name, String version, String minVersion,
      String maxVersion, String digest) {
    public boolean accepts(String candidateVersion, String candidateDigest) {
      return (version == null || version.equals(candidateVersion))
          && (minVersion == null || compareVersions(candidateVersion, minVersion) >= 0)
          && (maxVersion == null || compareVersions(candidateVersion, maxVersion) <= 0)
          && (digest == null || digest.equals(candidateDigest));
    }
  }

  public record Hook(String phase, String type, String script, int timeoutSeconds,
      boolean idempotent, String scope) {
    public Hook(String phase, String type, String script, int timeoutSeconds, boolean idempotent) {
      this(phase, type, script, timeoutSeconds, idempotent, "SERVER");
    }
  }

  public MpackManifest {
    artifacts = List.copyOf(artifacts);
    dependencies = List.copyOf(dependencies);
    hooks = List.copyOf(hooks);
    minStackVersions = List.copyOf(minStackVersions);
  }

  public String identity() {
    return name + "/" + version;
  }

  public void verifyAmbariVersion(String ambariVersion) {
    requireVersion(ambariVersion);
    if ((minAmbariVersion != null && compareVersions(ambariVersion, minAmbariVersion) < 0)
        || (maxAmbariVersion != null && compareVersions(ambariVersion, maxAmbariVersion) > 0)) {
      throw new MpackException(MpackException.Code.VERSION_INCOMPATIBLE,
          "The management pack does not support this Ambari version");
    }
  }

  public static MpackManifest parse(byte[] bytes) {
    if (bytes.length > MAX_MANIFEST_BYTES) {
      throw invalid("Manifest exceeds the size limit");
    }
    JsonNode root = MpackJson.read(bytes);
    MpackJson.fields(root, Set.of("schema_version", "type", "name", "version", "description",
        "artifacts", "dependencies", "hooks", "prerequisites", "$comment"));
    if (root.has("$comment") && !root.get("$comment").isTextual()) {
      throw invalid("Manifest $comment must be a string");
    }
    JsonNode schema = root.get("schema_version");
    if (schema == null || !schema.isIntegralNumber() || !schema.canConvertToInt()
        || schema.intValue() != SCHEMA_VERSION) {
      throw new MpackException(MpackException.Code.UNSUPPORTED_SCHEMA,
          "Manifest schema_version must be the integer 1");
    }
    if (!"full-release".equals(string(root, "type"))) {
      throw invalid("Only full-release archives are supported");
    }
    String name = requireName(string(root, "name"));
    String version = requireVersion(string(root, "version"));
    String description = optionalString(root, "description");
    List<Artifact> artifacts = new ArrayList<>();
    Set<String> artifactNames = new HashSet<>();
    Set<String> sourceDirs = new HashSet<>();
    for (JsonNode artifact : MpackJson.array(root, "artifacts", true)) {
      MpackJson.fields(artifact, Set.of("name", "type", "source_dir", "service_versions_map"));
      String artifactName = requireName(string(artifact, "name"));
      String type = string(artifact, "type");
      String sourceDir = requirePath(string(artifact, "source_dir"));
      if (!ARTIFACT_TYPES.contains(type) || !artifactNames.add(artifactName)) {
        throw invalid("Unknown artifact type or duplicate artifact name");
      }
      for (String previous : sourceDirs) {
        if (previous.equals(sourceDir) || previous.startsWith(sourceDir + "/")
            || sourceDir.startsWith(previous + "/")) {
          throw invalid("Artifact source directories must not overlap");
        }
      }
      sourceDirs.add(sourceDir);
      List<Addon> addons = new ArrayList<>();
      boolean addon = "stack-addon-service-definitions".equals(type);
      if (!addon && artifact.has("service_versions_map")) {
        throw invalid("Only addon artifacts may specify service_versions_map");
      }
      Set<String> addonTargets = new HashSet<>();
      for (JsonNode mapping : MpackJson.array(artifact, "service_versions_map", addon)) {
        MpackJson.fields(mapping, Set.of("service_name", "service_version", "applicable_stacks"));
        String service = requireName(string(mapping, "service_name"));
        String serviceVersion = requireVersion(string(mapping, "service_version"));
        List<Target> targets = new ArrayList<>();
        for (JsonNode target : MpackJson.array(mapping, "applicable_stacks", true)) {
          MpackJson.fields(target, Set.of("stack_name", "stack_version"));
          Target value = new Target(string(target, "stack_name"), string(target, "stack_version"));
          if (!addonTargets.add(service + "/" + value.identity())) {
            throw invalid("More than one addon provider for the same service and target");
          }
          targets.add(value);
        }
        addons.add(new Addon(service, serviceVersion, targets));
      }
      artifacts.add(new Artifact(artifactName, type, sourceDir, addons));
    }
    List<Dependency> dependencies = new ArrayList<>();
    Set<String> dependencyNames = new HashSet<>();
    for (JsonNode dependency : MpackJson.array(root, "dependencies", false)) {
      MpackJson.fields(dependency, Set.of("name", "version", "min_version", "max_version", "digest"));
      String dependencyName = requireName(string(dependency, "name"));
      if (dependencyName.equals(name) || !dependencyNames.add(dependencyName)) {
        throw invalid("Self dependencies and duplicate dependencies are not allowed");
      }
      String exact = optionalVersion(dependency, "version");
      String min = optionalVersion(dependency, "min_version");
      String max = optionalVersion(dependency, "max_version");
      if (exact == null && min == null && max == null) {
        throw invalid("A dependency requires an explicit version constraint");
      }
      if (exact != null && (min != null || max != null)) {
        throw invalid("Use an exact dependency version or a range, not both");
      }
      if (min != null && max != null && compareVersions(min, max) > 0) {
        throw invalid("Dependency version range is empty");
      }
      String digest = optionalString(dependency, "digest");
      if (digest != null) {
        requireDigest(digest);
      }
      dependencies.add(new Dependency(dependencyName, exact, min, max, digest));
    }
    List<Hook> hooks = new ArrayList<>();
    Set<String> phases = new HashSet<>();
    for (JsonNode hook : MpackJson.array(root, "hooks", false)) {
      MpackJson.fields(hook, Set.of("name", "type", "script", "timeout_seconds", "idempotent", "scope"));
      String phase = string(hook, "name");
      String type = string(hook, "type");
      String script = requirePath(string(hook, "script"));
      if (!HOOK_PHASES.contains(phase) || !phases.add(phase)
          || !Set.of("python", "shell").contains(type)) {
        throw invalid("Unknown or duplicate hook phase, or unsupported hook type");
      }
      JsonNode timeout = hook.get("timeout_seconds");
      if (timeout == null || !timeout.isIntegralNumber() || !timeout.canConvertToInt()
          || timeout.intValue() < 1 || timeout.intValue() > 3600) {
        throw invalid("Hook timeout_seconds must be an integer between 1 and 3600");
      }
      JsonNode idempotent = hook.get("idempotent");
      if (idempotent == null || !idempotent.isBoolean()) {
        throw invalid("Hook idempotent must be an explicit boolean");
      }
      String scope = hook.has("scope") ? string(hook, "scope") : "SERVER";
      if (!Set.of("DEFINITIONS", "SERVER").contains(scope)) throw invalid("Unknown hook scope");
      hooks.add(new Hook(phase, type, script, timeout.intValue(), idempotent.booleanValue(), scope));
    }
    String min = null;
    String max = null;
    List<Target> minStacks = new ArrayList<>();
    if (root.has("prerequisites")) {
      JsonNode prerequisites = root.get("prerequisites");
      MpackJson.fields(prerequisites, Set.of("min_ambari_version", "max_ambari_version",
          "min_stack_versions"));
      min = optionalVersion(prerequisites, "min_ambari_version");
      max = optionalVersion(prerequisites, "max_ambari_version");
      if (min != null && max != null && compareVersions(min, max) > 0) {
        throw invalid("Ambari version range is empty");
      }
      for (JsonNode target : MpackJson.array(prerequisites, "min_stack_versions", false)) {
        MpackJson.fields(target, Set.of("stack_name", "stack_version"));
        Target minimum = new Target(string(target, "stack_name"), string(target, "stack_version"));
        if (minStacks.contains(minimum)) {
          throw invalid("Duplicate stack prerequisite");
        }
        minStacks.add(minimum);
      }
    }
    return new MpackManifest(name, version, description, artifacts, dependencies, hooks,
        min, max, minStacks, MpackJson.canonical(root));
  }

  public static String requireName(String value) {
    if (value == null || !NAME.matcher(value).matches()) {
      throw invalid("Invalid package, artifact, stack, or service identifier");
    }
    return value;
  }

  public static String requireVersion(String value) {
    if (value == null || !VERSION.matcher(value).matches()) {
      throw invalid("Versions require one to five decimal components without leading zeroes");
    }
    return value;
  }

  public static String requireDigest(String value) {
    if (value == null || !DIGEST.matcher(value).matches()) {
      throw invalid("A lowercase SHA-256 digest is required");
    }
    return value;
  }

  public static String requirePath(String value) {
    if (value == null || value.length() > 1024 || value.startsWith("/")
        || value.contains("\\") || value.contains(":")) {
      throw invalid("A relative POSIX resource path is required");
    }
    for (String part : value.split("/", -1)) {
      if (part.isEmpty() || part.equals(".") || part.equals("..")
          || part.chars().anyMatch(character -> character < 32 || character == 127)) {
        throw invalid("Resource paths must be normalized and contained in the archive");
      }
    }
    return value;
  }

  public static int compareVersions(String left, String right) {
    String[] a = requireVersion(left).split("\\.");
    String[] b = requireVersion(right).split("\\.");
    for (int i = 0; i < Math.max(a.length, b.length); i++) {
      BigInteger x = i < a.length ? new BigInteger(a[i]) : BigInteger.ZERO;
      BigInteger y = i < b.length ? new BigInteger(b[i]) : BigInteger.ZERO;
      int comparison = x.compareTo(y);
      if (comparison != 0) {
        return comparison;
      }
    }
    return 0;
  }

  private static String optionalVersion(JsonNode node, String field) {
    String value = optionalString(node, field);
    return value == null ? null : requireVersion(value);
  }
}
