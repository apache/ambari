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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.UUID;

public record ManagedDependencyNamespace(String rootUri, String walUri, String znode) {
  private static final String BASE_DIRECTORY = "/apps/ambari-managed/hbase/";
  public static final String ZOOKEEPER_PARENT = "/ambari-managed-hbase";
  public static final String ZOOKEEPER_LEDGER_PARENT = ZOOKEEPER_PARENT + "/.bindings";
  public static final String ZOOKEEPER_HBASE_CHILD = "hbase";

  public ManagedDependencyNamespace {
    rootUri = normalize(rootUri);
    walUri = normalize(walUri);
    znode = normalize(znode);
    boolean hasHdfsValue = !rootUri.isEmpty() || !walUri.isEmpty();
    boolean hasCompleteHdfs = !rootUri.isEmpty() && !walUri.isEmpty();
    boolean hasZooKeeperValue = !znode.isEmpty();
    if (hasHdfsValue == hasZooKeeperValue || hasHdfsValue && !hasCompleteHdfs) {
      throw new IllegalArgumentException("namespace must contain either HDFS paths or a ZooKeeper znode");
    }
    if (!rootUri.isEmpty() && rootUri.equals(walUri)) {
      throw new IllegalArgumentException("root and WAL namespaces must be distinct");
    }
    if (!rootUri.isEmpty() && uriOverlaps(rootUri, walUri)) {
      throw new IllegalArgumentException("root and WAL namespaces must not contain one another");
    }
    if (!znode.isEmpty() && (!znode.startsWith("/") || znode.endsWith("/") || znode.contains("//"))) {
      throw new IllegalArgumentException("ZooKeeper namespace must be an absolute normalized path");
    }
  }

  public static ManagedDependencyNamespace hdfs(UUID bindingId, String defaultFs) {
    Objects.requireNonNull(bindingId, "bindingId");
    try {
      URI endpoint = new URI(Objects.requireNonNull(defaultFs, "defaultFs"));
      URI base = new URI(endpoint.getScheme(), endpoint.getAuthority(),
          BASE_DIRECTORY + bindingId, null, null);
      return new ManagedDependencyNamespace(base + "/root", base + "/wal", "");
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("invalid HDFS authority", e);
    }
  }

  public static ManagedDependencyNamespace zooKeeper(UUID bindingId) {
    return new ManagedDependencyNamespace("", "",
        zooKeeperContainer(bindingId) + "/" + ZOOKEEPER_HBASE_CHILD);
  }

  public static String zooKeeperContainer(UUID bindingId) {
    return ZOOKEEPER_PARENT + "/" + Objects.requireNonNull(bindingId, "bindingId");
  }

  public static String zooKeeperLedger(UUID bindingId) {
    return ZOOKEEPER_LEDGER_PARENT + "/" + Objects.requireNonNull(bindingId, "bindingId");
  }

  public static boolean zooKeeperParentConflicts(String configuredParent) {
    String normalized = normalize(configuredParent);
    return !normalized.isEmpty() && pathOverlaps(ZOOKEEPER_PARENT, normalized);
  }

  public boolean overlaps(ManagedDependencyNamespace other) {
    Objects.requireNonNull(other, "other");
    if (!znode.isEmpty() && !other.znode.isEmpty()) {
      return pathOverlaps(znode, other.znode);
    }
    if (!rootUri.isEmpty() && !other.rootUri.isEmpty()) {
      return uriOverlaps(rootUri, other.rootUri)
          || uriOverlaps(rootUri, other.walUri)
          || uriOverlaps(walUri, other.rootUri)
          || uriOverlaps(walUri, other.walUri);
    }
    return false;
  }

  private static boolean uriOverlaps(String left, String right) {
    URI leftUri = URI.create(left);
    URI rightUri = URI.create(right);
    return Objects.equals(leftUri.getScheme(), rightUri.getScheme())
        && Objects.equals(leftUri.getAuthority(), rightUri.getAuthority())
        && pathOverlaps(leftUri.getPath(), rightUri.getPath());
  }

  private static boolean pathOverlaps(String left, String right) {
    return left.equals(right)
        || left.startsWith(right.endsWith("/") ? right : right + "/")
        || right.startsWith(left.endsWith("/") ? left : left + "/");
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim();
  }
}
