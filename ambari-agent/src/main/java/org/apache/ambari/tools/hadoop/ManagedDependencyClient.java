/**
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
package org.apache.ambari.tools.hadoop;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.AclEntry;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.util.VersionInfo;

/** Stateless client observations. The Agent journal and Server own workflow state. */
public final class ManagedDependencyClient {
  private static final ObjectMapper JSON = new ObjectMapper();
  static final int PROBE_BYTES = 128 * 1024 + 1;
  private static final int MAX_MARKER_BYTES = 8192;

  private ManagedDependencyClient() {
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 4) {
      throw new IllegalArgumentException("Expected operation, configuration directory, identity, payload");
    }
    String operation = args[0];
    JsonNode identity = JSON.readTree(args[2]);
    validateIdentity(identity);
    JsonNode payload = JSON.readTree(args[3]);
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("schemaVersion", 1);
    response.put("identity", identity);
    response.put("operation", operation);
    try {
      Map<String, Object> result;
      if ("VERSION".equals(operation)) {
        String kind = requiredText(payload, "kind");
        // HBase is supplied by the installed client, not bundled into the Agent.
        String version = "HADOOP_CLIENT".equals(kind) ? VersionInfo.getVersion()
            : "HBASE_CLIENT".equals(kind)
                ? (String) Class.forName("org.apache.hadoop.hbase.util.VersionInfo")
                    .getMethod("getVersion").invoke(null)
                : null;
        if (version == null) {
          throw new IllegalArgumentException("Unsupported client software kind");
        }
        result = Map.of("kind", kind, "version", version);
      } else {
        Configuration config = new Configuration(false);
        config.addResource(new Path(args[1], "core-site.xml"));
        config.addResource(new Path(args[1], "hdfs-site.xml"));
        UserGroupInformation.setConfiguration(config);
        URI uri = URI.create(requiredText(payload, "path"));
        if (!"hdfs".equals(uri.getScheme()) || uri.getAuthority() == null
            || uri.getQuery() != null || uri.getFragment() != null || uri.getUserInfo() != null) {
          throw new IllegalArgumentException("Expected an explicit HDFS authority");
        }
        try (FileSystem fs = FileSystem.newInstance(uri, config)) {
          result = execute(fs, operation, identity, payload);
        }
      }
      response.put("status", "SUCCEEDED");
      response.put("result", result);
    } catch (ProbeFailure failure) {
      response.put("status", "FAILED");
      response.put("errorCode", failure.code);
    } catch (AccessControlException failure) {
      response.put("status", "FAILED");
      response.put("errorCode", "DEPENDENCY_AUTHORIZATION_FAILED");
    } catch (Exception failure) {
      response.put("status", "FAILED");
      response.put("errorCode", "DEPENDENCY_CLIENT_OBSERVATION_FAILED");
    }
    System.out.println(JSON.writeValueAsString(response));
  }

  static Map<String, Object> execute(FileSystem fs, String operation,
      JsonNode identity, JsonNode payload) throws Exception {
    Path path = new Path(requiredText(payload, "path"));
    switch (operation) {
      case "STAT":
        try {
          FileStatus status = fs.getFileLinkStatus(path);
          return Map.of("exists", true, "type", status.isSymlink() ? "SYMLINK"
                  : status.isDirectory() ? "DIRECTORY" : "FILE",
              "owner", status.getOwner(), "group", status.getGroup(),
              "mode", (int) status.getPermission().toShort());
        } catch (FileNotFoundException absent) {
          return Map.of("exists", false);
        }
      case "ACL":
        List<Map<String, Object>> entries = new ArrayList<>();
        for (AclEntry entry : fs.getAclStatus(path).getEntries()) {
          entries.add(Map.of("scope", entry.getScope().name(), "type", entry.getType().name(),
              "name", entry.getName() == null ? "" : entry.getName(),
              "permission", entry.getPermission().ordinal()));
        }
        return Map.of("entries", entries);
      case "COUNT":
        // Listing only the first child avoids a recursive traversal of provider data.
        return Map.of("empty", !fs.listStatusIterator(path).hasNext());
      case "MKDIR":
        requireMutationPath(path, identity, true);
        if (!fs.getFileStatus(path.getParent()).isDirectory()) {
          throw new IOException("Managed parent is not a directory");
        }
        FsPermission permission = permission(payload);
        fs.getConf().set("fs.permissions.umask-mode", "000");
        if (!fs.mkdirs(path, permission)) {
          throw new IOException("Managed directory creation failed");
        }
        return Map.of("created", true);
      case "SET_OWNER":
        requireMutationPath(path, identity, false);
        fs.setOwner(path, requiredText(payload, "owner"), requiredText(payload, "group"));
        return Map.of("applied", true);
      case "SET_PERMISSION":
        requireMutationPath(path, identity, false);
        fs.setPermission(path, permission(payload));
        return Map.of("applied", true);
      case "READ_MARKER":
        requireMarkerPath(path, identity);
        try (FSDataInputStream input = fs.open(path)) {
          byte[] bytes = input.readNBytes(MAX_MARKER_BYTES + 1);
          if (bytes.length > MAX_MARKER_BYTES) {
            throw new IOException("Oversized binding marker");
          }
          JsonNode marker = JSON.readTree(bytes);
          if (!marker.isObject()) {
            throw new IOException("Invalid binding marker");
          }
          return Map.of("marker", marker);
        }
      case "WRITE_MARKER":
        requireMarkerPath(path, identity);
        byte[] marker = JSON.writeValueAsBytes(payload.required("marker"));
        if (marker.length > MAX_MARKER_BYTES || !payload.get("marker").isObject()) {
          throw new IOException("Invalid binding marker");
        }
        try (FSDataOutputStream output = fs.create(path, new FsPermission((short) 0600),
            false, 4096, fs.getDefaultReplication(path), fs.getDefaultBlockSize(path), null)) {
          output.write(marker);
        }
        return Map.of("written", true);
      case "PROBE":
        requireMutationPath(path, identity, false);
        if (!path.toUri().getPath().endsWith("/root")) {
          throw new IllegalArgumentException("Probe must be inside the binding root");
        }
        return probe(fs, path, identity);
      default:
        throw new IllegalArgumentException("Unsupported client operation");
    }
  }

  private static Map<String, Object> probe(FileSystem fs, Path root, JsonNode identity)
      throws Exception {
    Path path = new Path(root, ".ambari-managed-probe-" + requiredText(identity, "operationId"));
    byte[] digest = MessageDigest.getInstance("SHA-256")
        .digest(requiredText(identity, "immutableRequestHash").getBytes(StandardCharsets.UTF_8));
    byte[] expected = new byte[PROBE_BYTES];
    for (int i = 0; i < expected.length; i++) {
      expected[i] = digest[i % digest.length];
    }
    boolean owned = false;
    try {
      if (!fs.exists(path)) {
        try (FSDataOutputStream output = fs.create(path, false)) {
          owned = true;
          output.write(expected);
        }
      }
      byte[] observed;
      try (FSDataInputStream input = fs.open(path)) {
        observed = input.readNBytes(PROBE_BYTES + 1);
      }
      if (!Arrays.equals(observed, expected)) {
        throw new ProbeFailure("DEPENDENCY_DATA_INTEGRITY_FAILED");
      }
      // Only an exact replay's verified content authorizes cleaning a preexisting probe.
      owned = true;
    } catch (ProbeFailure failure) {
      throw failure;
    } catch (AccessControlException failure) {
      throw failure;
    } catch (IOException failure) {
      throw new ProbeFailure("DEPENDENCY_DATANODE_READ_WRITE_FAILED");
    } finally {
      if (owned) {
        try {
          if (!fs.delete(path, false)) {
            throw new IOException("Probe was not deleted");
          }
        } catch (IOException failure) {
          throw new ProbeFailure("DEPENDENCY_PROBE_CLEANUP_FAILED");
        }
      }
    }
    return Map.of("readWriteVerified", true, "bytes", PROBE_BYTES, "cleaned", true);
  }

  private static void requireMutationPath(Path path, JsonNode identity, boolean parents) {
    String binding = requiredText(identity, "bindingId");
    String actual = path.toUri().getPath();
    String base = "/apps/ambari-managed/hbase/" + binding;
    if (actual.equals(base) || actual.equals(base + "/root") || actual.equals(base + "/wal")
        || (parents && List.of("/apps", "/apps/ambari-managed",
            "/apps/ambari-managed/hbase", "/apps/ambari-managed/.bindings").contains(actual))) {
      return;
    }
    throw new IllegalArgumentException("Path is outside the exact managed namespace");
  }

  private static void requireMarkerPath(Path path, JsonNode identity) {
    if (!path.toUri().getPath().equals("/apps/ambari-managed/.bindings/"
        + requiredText(identity, "bindingId") + ".json")) {
      throw new IllegalArgumentException("Path is outside the exact binding marker");
    }
  }

  private static FsPermission permission(JsonNode payload) {
    JsonNode mode = payload.required("mode");
    if (!mode.isInt() || mode.intValue() < 0 || mode.intValue() > 0777) {
      throw new IllegalArgumentException("Invalid permission bits");
    }
    return new FsPermission((short) mode.intValue());
  }

  private static void validateIdentity(JsonNode identity) {
    for (String name : List.of("bindingId", "operationId")) {
      String value = requiredText(identity, name);
      if (!UUID.fromString(value).toString().equals(value)) {
        throw new IllegalArgumentException("Invalid operation identity");
      }
    }
    if (!identity.path("epoch").isIntegralNumber() || identity.get("epoch").longValue() <= 0
        || !identity.path("immutableRequestHash").asText().matches("sha256:[0-9a-f]{64}")) {
      throw new IllegalArgumentException("Invalid operation lineage");
    }
  }

  private static String requiredText(JsonNode node, String name) {
    JsonNode value = node.required(name);
    if (!value.isTextual() || value.textValue().isEmpty()) {
      throw new IllegalArgumentException("Missing text field");
    }
    return value.textValue();
  }

  private static final class ProbeFailure extends IOException {
    private final String code;

    private ProbeFailure(String code) {
      this.code = code;
    }
  }
}
