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
package org.apache.ambari.server.controller.dependencies.security;

import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.ambari.server.controller.dependencies.ManagedDependencyNamespace;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyType;

/**
 * Immutable, provenance-free inputs for a managed HBase Kerberos overlay.
 * Selection references may describe a trusted service plan before a binding is materialized.
 */
public record ManagedHBaseKerberosOverlaySpec(
    int schemaVersion,
    String realm,
    String effectiveShortUser,
    String rolePrincipalPattern,
    String headlessPrincipal,
    String smokePrincipal,
    String smokeShortUser,
    String identityPlanFingerprint,
    SortedMap<ManagedDependencyType, ManagedBindingSnapshotRef> bindings,
    String znodeParent,
    String hbaseSuperuser) {
  public static final int SCHEMA_VERSION = 1;
  public static final String AUTH_TO_LOCAL_PROPERTY =
      "core-site/hadoop.security.auth_to_local";

  private static final Pattern REALM =
      Pattern.compile("[A-Z0-9](?:[A-Z0-9.-]{0,251}[A-Z0-9])?");
  private static final Pattern PLANNED_USER =
      Pattern.compile("hbase_mc_(?:[0-9a-f]{20}|c[0-9a-z]{1,13})");
  private static final Pattern SIMPLE_USER =
      Pattern.compile("[A-Za-z_][A-Za-z0-9._-]{0,127}");
  private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

  public ManagedHBaseKerberosOverlaySpec {
    if (schemaVersion != SCHEMA_VERSION) {
      throw new IllegalArgumentException("unsupported managed HBase overlay schema");
    }
    realm = requireRealm(realm);
    effectiveShortUser = requirePlannedUser(effectiveShortUser);
    String expectedRolePrincipal = effectiveShortUser + "/_HOST@" + realm;
    if (!expectedRolePrincipal.equals(rolePrincipalPattern)) {
      throw new IllegalArgumentException(
          "rolePrincipalPattern must match the approved unique HBase identity");
    }
    String expectedHeadlessPrincipal = effectiveShortUser + "@" + realm;
    if (!expectedHeadlessPrincipal.equals(headlessPrincipal)) {
      throw new IllegalArgumentException(
          "headlessPrincipal must match the approved unique HBase identity");
    }
    smokeShortUser = requireSimpleUser(smokeShortUser, "smokeShortUser");
    requireOneComponentPrincipal(smokePrincipal, realm, "smokePrincipal");
    if (effectiveShortUser.equals(smokeShortUser)) {
      throw new IllegalArgumentException(
          "the HBase service and smoke identities must remain distinct");
    }
    identityPlanFingerprint = requireHash(identityPlanFingerprint,
        "identityPlanFingerprint");
    bindings = immutableBindings(bindings);
    znodeParent = znodeParent == null ? "" : znodeParent.trim();
    ManagedBindingSnapshotRef zooKeeper = bindings.get(ManagedDependencyType.ZOOKEEPER);
    String expectedZnode = zooKeeper == null ? ""
        : ManagedDependencyNamespace.zooKeeper(zooKeeper.bindingId()).znode();
    if (!expectedZnode.equals(znodeParent)) {
      throw new IllegalArgumentException(
          "znodeParent must match the selected managed ZooKeeper namespace");
    }
    if (!effectiveShortUser.equals(hbaseSuperuser)) {
      throw new IllegalArgumentException(
          "hbaseSuperuser must be the approved unique HBase identity");
    }
  }

  public static ManagedHBaseKerberosOverlaySpec create(String realm,
      String effectiveShortUser, String smokePrincipal, String smokeShortUser,
      String identityPlanFingerprint,
      SortedMap<ManagedDependencyType, ManagedBindingSnapshotRef> bindings) {
    Objects.requireNonNull(bindings, "bindings");
    ManagedBindingSnapshotRef zooKeeper = bindings.get(ManagedDependencyType.ZOOKEEPER);
    String znode = zooKeeper == null ? ""
        : ManagedDependencyNamespace.zooKeeper(zooKeeper.bindingId()).znode();
    return new ManagedHBaseKerberosOverlaySpec(SCHEMA_VERSION, realm,
        effectiveShortUser, effectiveShortUser + "/_HOST@" + realm,
        effectiveShortUser + "@" + realm, smokePrincipal, smokeShortUser,
        identityPlanFingerprint, bindings, znode, effectiveShortUser);
  }

  public boolean hasManagedHdfs() {
    return bindings.containsKey(ManagedDependencyType.HDFS);
  }

  public boolean hasManagedZooKeeper() {
    return bindings.containsKey(ManagedDependencyType.ZOOKEEPER);
  }

  private static SortedMap<ManagedDependencyType, ManagedBindingSnapshotRef> immutableBindings(
      SortedMap<ManagedDependencyType, ManagedBindingSnapshotRef> source) {
    Objects.requireNonNull(source, "bindings");
    if (source.isEmpty() || source.size() > ManagedDependencyType.values().length) {
      throw new IllegalArgumentException("one or two managed dependency selections are required");
    }
    TreeMap<ManagedDependencyType, ManagedBindingSnapshotRef> copy = new TreeMap<>();
    for (var entry : source.entrySet()) {
      ManagedDependencyType type = Objects.requireNonNull(entry.getKey(), "binding type");
      ManagedBindingSnapshotRef reference = Objects.requireNonNull(entry.getValue(),
          "binding reference");
      if (type != reference.type()) {
        throw new IllegalArgumentException("binding map key must match its reference type");
      }
      copy.put(type, reference);
    }
    return Collections.unmodifiableSortedMap(copy);
  }

  private static String requireRealm(String value) {
    Objects.requireNonNull(value, "realm");
    if (!value.equals(value.trim()) || !value.equals(value.toUpperCase(Locale.ENGLISH))
        || !REALM.matcher(value).matches()) {
      throw new IllegalArgumentException("realm must be a canonical Kerberos realm");
    }
    return value;
  }

  private static String requirePlannedUser(String value) {
    Objects.requireNonNull(value, "effectiveShortUser");
    if (!PLANNED_USER.matcher(value).matches()) {
      throw new IllegalArgumentException(
          "effectiveShortUser must match a server-owned HBase identity plan");
    }
    return value;
  }

  private static String requireSimpleUser(String value, String field) {
    Objects.requireNonNull(value, field);
    if (!value.equals(value.trim()) || !SIMPLE_USER.matcher(value).matches()) {
      throw new IllegalArgumentException(field + " is not a supported local user");
    }
    return value;
  }

  private static void requireOneComponentPrincipal(String value, String expectedRealm,
      String field) {
    Objects.requireNonNull(value, field);
    int separator = value.indexOf('@');
    if (separator <= 0 || separator != value.lastIndexOf('@')
        || value.substring(0, separator).contains("/")
        || !SIMPLE_USER.matcher(value.substring(0, separator)).matches()
        || !expectedRealm.equals(value.substring(separator + 1))) {
      throw new IllegalArgumentException(
          field + " must be a supported same-realm one-component principal");
    }
  }

  private static String requireHash(String value, String field) {
    if (value == null || !HASH.matcher(value).matches()) {
      throw new IllegalArgumentException(field + " must be a SHA-256 fingerprint");
    }
    return value;
  }

  public record ManagedBindingSnapshotRef(
      ManagedDependencyType type,
      UUID bindingId) {
    public ManagedBindingSnapshotRef {
      type = Objects.requireNonNull(type, "type");
      bindingId = Objects.requireNonNull(bindingId, "bindingId");
    }
  }
}
