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
package org.apache.ambari.server.controller.dependencies;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.ambari.server.controller.dependencies.ManagedDependencyCommand.CommandName;

/** Exact consumer preparation plan transported to HBase INSTALL and CONFIGURE commands. */
public record ManagedDependencyCommandBundle(
    int schemaVersion,
    long hostId,
    String consumerUser,
    String identityFingerprint,
    List<ManagedDependencyCommand> commands,
    List<UUID> preparationBindingIds,
    String immutableBundleHash) {
  public static final int CURRENT_SCHEMA_VERSION = 2;
  private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
  private static final Comparator<UUID> CANONICAL_UUID_ORDER =
      Comparator.comparing(UUID::toString);

  public ManagedDependencyCommandBundle {
    if (schemaVersion != 1 && schemaVersion != CURRENT_SCHEMA_VERSION) {
      throw invalid("unsupported schemaVersion " + schemaVersion);
    }
    if (hostId <= 0) {
      throw invalid("hostId must be positive");
    }
    consumerUser = requireNonBlank(consumerUser, "consumerUser");
    identityFingerprint = requireHash(identityFingerprint, "identityFingerprint");
    commands = sortedCommands(commands);
    validateCommands(hostId, consumerUser, identityFingerprint, commands);
    preparationBindingIds = canonicalPreparationBindingIds(schemaVersion, commands,
        preparationBindingIds);
    immutableBundleHash = requireHash(immutableBundleHash, "immutableBundleHash");
    if (!immutableBundleHash.equals(bundleHash(
        schemaVersion, hostId, consumerUser, identityFingerprint, commands,
        preparationBindingIds))) {
      throw invalid("immutableBundleHash does not match the selected commands");
    }
  }

  public static ManagedDependencyCommandBundle of(long hostId, String consumerUser,
      String identityFingerprint, List<ManagedDependencyCommand> commands) {
    List<ManagedDependencyCommand> sorted = sortedCommands(commands);
    return new ManagedDependencyCommandBundle(CURRENT_SCHEMA_VERSION, hostId,
        consumerUser, identityFingerprint, sorted,
        allBindingIds(sorted),
        bundleHash(CURRENT_SCHEMA_VERSION, hostId, consumerUser, identityFingerprint, sorted,
            allBindingIds(sorted)));
  }

  public static ManagedDependencyCommandBundle of(long hostId, String consumerUser,
      String identityFingerprint, List<ManagedDependencyCommand> commands,
      Set<UUID> preparationBindingIds) {
    List<ManagedDependencyCommand> sorted = sortedCommands(commands);
    List<UUID> selected = canonicalPreparationBindingIds(CURRENT_SCHEMA_VERSION, sorted,
        preparationBindingIds == null ? null : List.copyOf(preparationBindingIds));
    return new ManagedDependencyCommandBundle(CURRENT_SCHEMA_VERSION, hostId,
        consumerUser, identityFingerprint, sorted, selected,
        bundleHash(CURRENT_SCHEMA_VERSION, hostId, consumerUser, identityFingerprint, sorted,
            selected));
  }

  @Override
  public List<UUID> preparationBindingIds() {
    if (preparationBindingIds == null && schemaVersion == 1) {
      return allBindingIds(commands);
    }
    return preparationBindingIds;
  }

  public List<ManagedDependencyCommand> preparationCommands() {
    List<UUID> selectedIds = preparationBindingIds();
    if (selectedIds == null) {
      if (schemaVersion != 1) {
        throw invalid("preparationBindingIds is required for schema 2");
      }
      selectedIds = allBindingIds(commands);
    }
    Set<UUID> selected = Set.copyOf(selectedIds);
    return commands.stream().filter(command -> selected.contains(command.envelope().bindingId()))
        .toList();
  }

  static String bundleHash(int schemaVersion, long hostId, String consumerUser,
      String identityFingerprint, List<ManagedDependencyCommand> commands,
      List<UUID> preparationBindingIds) {
    StringBuilder canonical = new StringBuilder();
    append(canonical, "managed-dependency-command-bundle", Integer.toString(schemaVersion),
        Long.toString(hostId), consumerUser, identityFingerprint);
    for (ManagedDependencyCommand command : sortedCommands(commands)) {
      append(canonical, command.parameters().get("provider.service"),
          command.envelope().bindingId().toString(),
          Long.toString(command.envelope().epoch()),
          Long.toString(command.envelope().snapshotVersion()),
          command.envelope().immutableRequestHash());
    }
    if (schemaVersion >= CURRENT_SCHEMA_VERSION) {
      append(canonical, "preparation-binding-ids", Integer.toString(preparationBindingIds.size()));
      for (UUID bindingId : preparationBindingIds) {
        append(canonical, bindingId.toString());
      }
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return "sha256:" + HexFormat.of().formatHex(
          digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is required by the Java runtime", e);
    }
  }

  static String bundleHash(int schemaVersion, long hostId, String consumerUser,
      String identityFingerprint, List<ManagedDependencyCommand> commands) {
    return bundleHash(schemaVersion, hostId, consumerUser, identityFingerprint, commands,
        schemaVersion >= CURRENT_SCHEMA_VERSION ? allBindingIds(sortedCommands(commands)) : List.of());
  }

  private static List<UUID> allBindingIds(List<ManagedDependencyCommand> commands) {
    return commands.stream().map(command -> command.envelope().bindingId())
        .sorted(CANONICAL_UUID_ORDER).toList();
  }

  private static List<UUID> canonicalPreparationBindingIds(int schemaVersion,
      List<ManagedDependencyCommand> commands, List<UUID> ids) {
    if (schemaVersion == 1) {
      return allBindingIds(commands);
    }
    if (ids == null) {
      throw invalid("preparationBindingIds is required for schema 2");
    }
    if (ids.stream().anyMatch(Objects::isNull) || new HashSet<>(ids).size() != ids.size()) {
      throw invalid("preparationBindingIds must be duplicate-free");
    }
    Set<UUID> commandIds = commands.stream().map(command -> command.envelope().bindingId())
        .collect(java.util.stream.Collectors.toSet());
    if (!commandIds.containsAll(ids)) {
      throw invalid("preparationBindingIds must be a subset of command binding IDs");
    }
    return ids.stream().sorted(CANONICAL_UUID_ORDER).toList();
  }

  private static List<ManagedDependencyCommand> sortedCommands(
      List<ManagedDependencyCommand> commands) {
    Objects.requireNonNull(commands, "commands");
    if (commands.isEmpty() || commands.size() > 2) {
      throw invalid("a bundle requires one or two managed dependency commands");
    }
    ArrayList<ManagedDependencyCommand> copy = new ArrayList<>(commands);
    if (copy.stream().anyMatch(Objects::isNull)) {
      throw invalid("bundle commands must not be null");
    }
    copy.sort(Comparator.comparing(command -> command.parameters().get("provider.service")));
    return List.copyOf(copy);
  }

  private static void validateCommands(long hostId, String consumerUser,
      String identityFingerprint, List<ManagedDependencyCommand> commands) {
    Set<String> services = new HashSet<>();
    Set<java.util.UUID> bindingIds = new HashSet<>();
    Set<String> securityModes = new HashSet<>();
    Set<String> mappingProfiles = new HashSet<>();
    for (ManagedDependencyCommand command : commands) {
      if (!Set.of(CommandName.PREPARE_HDFS_CONSUMER,
          CommandName.PREPARE_ZOOKEEPER_CONSUMER).contains(command.name())) {
        throw invalid("bundles may contain only consumer PREPARE commands");
      }
      if (!services.add(command.parameters().get("provider.service"))) {
        throw invalid("a bundle may contain only one command per dependency type");
      }
      if (!bindingIds.add(command.envelope().bindingId())) {
        throw invalid("a bundle may contain each binding UUID only once");
      }
      securityModes.add(command.parameters().get("security.mode"));
      String mappingProfile = command.parameters().get("consumer.mapping.profile.fingerprint");
      if (mappingProfile != null) {
        mappingProfiles.add(mappingProfile);
      }
      if (!Long.toString(hostId).equals(command.parameters().get("host.id"))
          || !consumerUser.equals(command.parameters().get("consumer.user"))
          || !identityFingerprint.equals(command.parameters().get("identity.fingerprint"))) {
        throw invalid("bundle commands must target the same host and consumer identity");
      }
    }
    if (securityModes.size() != 1 || mappingProfiles.size() > 1
        || securityModes.contains(ManagedDependencySecurityMode.KERBEROS.name())
            && mappingProfiles.size() != 1
        || securityModes.contains(ManagedDependencySecurityMode.INSECURE.name())
            && !mappingProfiles.isEmpty()) {
      throw invalid("bundle commands must use one exact consumer security profile");
    }
  }

  private static void append(StringBuilder canonical, String... values) {
    for (String value : values) {
      String actual = value == null ? "" : value;
      canonical.append(actual.length()).append(':').append(actual);
    }
  }

  private static String requireNonBlank(String value, String field) {
    Objects.requireNonNull(value, field);
    if (value.isBlank()) {
      throw invalid(field + " must not be blank");
    }
    return value;
  }

  private static String requireHash(String value, String field) {
    requireNonBlank(value, field);
    if (!HASH.matcher(value).matches()) {
      throw invalid(field + " must use sha256:<64 lowercase hex> format");
    }
    return value;
  }

  private static IllegalArgumentException invalid(String message) {
    return new IllegalArgumentException(ManagedDependencyErrorCode.DEPENDENCY_COMMAND_INVALID
        + ": " + message);
  }
}
