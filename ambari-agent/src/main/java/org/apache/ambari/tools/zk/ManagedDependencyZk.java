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
package org.apache.ambari.tools.zk;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;

/** Nonrecursive ZooKeeper operations reserved for managed HBase dependencies. */
public final class ManagedDependencyZk {
  static final String PARENT = "/ambari-managed-hbase";
  private static final int SESSION_TIMEOUT_MILLIS = 5000;
  private static final int CONNECTION_TIMEOUT_MILLIS = 30000;
  private static final Pattern OWNER = Pattern.compile("[A-Za-z_][A-Za-z0-9._-]{0,127}");
  private static final Pattern POSITIVE_INTEGER = Pattern.compile("[1-9][0-9]{0,18}");

  private ManagedDependencyZk() {
  }

  public static void main(String[] args) throws Exception {
    // HBase's client classpath can supply Commons CLI 1.2 before this helper JAR.
    CommandLine command = new GnuParser().parse(options(), args);
    String result = execute(command);
    System.out.println(result);
  }

  static String execute(CommandLine command) throws Exception {
    Request request = Request.parse(command);
    ZooKeeper client = ZkConnection.open(
        request.connectionString(), SESSION_TIMEOUT_MILLIS, CONNECTION_TIMEOUT_MILLIS);
    try {
      switch (request.operation()) {
        case "PREPARE_INSECURE":
          prepareInsecure(client, request);
          return "{\"connected\":true,\"namespaceExists\":true,"
              + "\"parentAclPolicy\":\"INSECURE_PROVIDER_PREPARED\","
              + "\"subtreeAclPolicy\":\"INSECURE_BINDING_SCOPED\"}";
        case "VERIFY_INSECURE":
          verifyInsecure(client, request);
          return "{\"connected\":true,\"privateZnodeVerified\":true}";
        case "PREPARE_SECURE_LEDGER":
          prepareSecureLedger(client, request);
          return "{\"connected\":true,\"ledgerPrepared\":true}";
        case "CREATE_SECURE_CONTAINER":
          boolean created = createSecureContainer(client, request);
          return created
              ? "{\"connected\":true,\"containerCreated\":true,\"nodeExists\":false}"
              : "{\"connected\":true,\"containerCreated\":false,\"nodeExists\":true}";
        case "VERIFY_SECURE":
          verifySecure(client, request);
          return "{\"connected\":true,\"consumerSaslId\":\""
              + request.consumerSaslId()
              + "\",\"hbaseZnodeVerified\":true,\"privateContainerVerified\":true,"
              + "\"siblingAuthorityDenied\":true}";
        default:
          throw new IllegalArgumentException("Unsupported managed dependency ZooKeeper operation");
      }
    } finally {
      client.close();
    }
  }

  private static void prepareInsecure(ZooKeeper client, Request request)
      throws KeeperException, InterruptedException {
    ensureExactNode(client, PARENT, parentMarker(), ZooDefs.Ids.OPEN_ACL_UNSAFE);
    ensureExactNode(client, request.container(), bindingMarker(request), ZooDefs.Ids.OPEN_ACL_UNSAFE);
    ensureExactNode(client, request.znode(), new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE);
  }

  private static void verifyInsecure(ZooKeeper client, Request request)
      throws KeeperException, InterruptedException {
    requireExactNode(client, PARENT, parentMarker(), ZooDefs.Ids.OPEN_ACL_UNSAFE);
    requireExactNode(client, request.container(), bindingMarker(request), ZooDefs.Ids.OPEN_ACL_UNSAFE);
    requireExactNode(client, request.znode(), new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE);
    String probe = request.znode() + "/.ambari-managed-probe-" + request.probeId();
    boolean created = false;
    try {
      client.create(probe, request.probeId().toString().getBytes(StandardCharsets.UTF_8),
          ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
      created = true;
      byte[] observed = client.getData(probe, false, new Stat());
      if (!java.util.Arrays.equals(observed,
          request.probeId().toString().getBytes(StandardCharsets.UTF_8))) {
        throw new IllegalStateException("Managed dependency ZooKeeper probe data mismatch");
      }
    } finally {
      if (created) {
        client.delete(probe, -1);
      }
    }
  }

  private static void prepareSecureLedger(ZooKeeper client, Request request)
      throws KeeperException, InterruptedException {
    List<ACL> providerAcl = privateAcl(request.providerSaslId());
    ensureExactNode(client, PARENT, parentMarker(), providerAcl);
    ensureExactNode(client, PARENT + "/.bindings", ledgerParentMarker(), providerAcl);
    ensureExactNode(client, request.ledger(), ledgerMarker(request), providerAcl);
  }

  private static boolean createSecureContainer(ZooKeeper client, Request request)
      throws KeeperException, InterruptedException {
    try {
      client.create(request.container(), bindingMarker(request),
          privateAcl(request.consumerSaslId()), CreateMode.PERSISTENT);
      return true;
    } catch (KeeperException.NodeExistsException expected) {
      return false;
    }
  }

  private static void verifySecure(ZooKeeper client, Request request)
      throws KeeperException, InterruptedException {
    List<ACL> consumerAcl = privateAcl(request.consumerSaslId());
    Stat containerStat = requireExactNode(
        client, request.container(), bindingMarker(request), consumerAcl);
    ensureSupportedHBaseBaseNode(client, request);
    verifyProbe(client, request, consumerAcl);
    requireSiblingAuthorityDenied(client, request, consumerAcl, containerStat.getVersion());
  }

  private static void verifyProbe(ZooKeeper client, Request request, List<ACL> acl)
      throws KeeperException, InterruptedException {
    String probe = request.znode() + "/.ambari-managed-probe-" + request.probeId();
    boolean created = false;
    try {
      client.create(probe, request.probeId().toString().getBytes(StandardCharsets.UTF_8),
          acl, CreateMode.EPHEMERAL);
      created = true;
      byte[] observed = client.getData(probe, false, new Stat());
      if (!java.util.Arrays.equals(observed,
          request.probeId().toString().getBytes(StandardCharsets.UTF_8))) {
        throw new IllegalStateException("Managed dependency ZooKeeper probe data mismatch");
      }
    } finally {
      if (created) {
        client.delete(probe, -1);
      }
    }
  }

  private static void requireSiblingAuthorityDenied(ZooKeeper client, Request request,
      List<ACL> acl, int containerVersion) throws KeeperException, InterruptedException {
    String forbidden = PARENT + "/.ambari-managed-denied-" + request.probeId();
    boolean created = false;
    try {
      client.create(forbidden, new byte[0], acl, CreateMode.EPHEMERAL);
      created = true;
    } catch (KeeperException.NoAuthException expected) {
      // The provider-only parent must deny consumer sibling creation.
    } finally {
      if (created) {
        try {
          client.delete(forbidden, -1);
        } catch (KeeperException.NoNodeException ignored) {
          // The helper cleans only its own UUID probe; session expiry may race the delete.
        }
      }
    }
    if (created) {
      throw new IllegalStateException(
          "Managed dependency consumer unexpectedly has sibling creation authority");
    }
    try {
      client.delete(request.container(), Math.addExact(containerVersion, 1));
      throw new IllegalStateException(
          "Managed dependency consumer unexpectedly deleted its binding container");
    } catch (KeeperException.NoAuthException expected) {
      // DELETE is also checked on the provider-only parent ACL.
    } catch (KeeperException.BadVersionException | KeeperException.NotEmptyException authorized) {
      throw new IllegalStateException(
          "Managed dependency consumer unexpectedly has sibling deletion authority", authorized);
    }
  }

  private static void ensureExactNode(ZooKeeper client, String path, byte[] data,
      List<ACL> acls) throws KeeperException, InterruptedException {
    if (client.exists(path, false) == null) {
      try {
        client.create(path, data, acls, CreateMode.PERSISTENT);
      } catch (KeeperException.NodeExistsException ignored) {
        // A concurrent exact retry may have created it; validation below decides.
      }
    }
    requireExactNode(client, path, data, acls);
  }

  private static void ensureSupportedHBaseBaseNode(ZooKeeper client, Request request)
      throws KeeperException, InterruptedException {
    List<ACL> privateAcl = privateAcl(request.consumerSaslId());
    if (client.exists(request.znode(), false) == null) {
      try {
        client.create(request.znode(), new byte[0], privateAcl, CreateMode.PERSISTENT);
      } catch (KeeperException.NodeExistsException ignored) {
        // A concurrent HBase create is accepted only if its final policy is canonical.
      }
    }
    Stat dataStat = new Stat();
    byte[] actualData = client.getData(request.znode(), false, dataStat);
    Stat aclStat = new Stat();
    List<ACL> actualAcls = client.getACL(request.znode(), aclStat);
    if (actualData.length != 0 || dataStat.getEphemeralOwner() != 0
        || dataStat.getVersion() != 0
        || !isSupportedHBaseBaseAcl(
            actualAcls, request.consumerSaslId(), aclStat.getAversion())) {
      throw new IllegalStateException(
          "Managed dependency HBase base znode metadata conflicts");
    }
  }

  static boolean isSupportedHBaseBaseAcl(List<ACL> actual, String consumerSaslId,
      int aclVersion) {
    List<ACL> privateAcl = privateAcl(consumerSaslId);
    if (aclVersion == 0 && sameAclSet(actual, privateAcl)) {
      return true;
    }
    List<ACL> hbaseNativeAcl = List.of(
        new ACL(ZooDefs.Perms.ALL, new Id("sasl", consumerSaslId)),
        new ACL(ZooDefs.Perms.READ, ZooDefs.Ids.ANYONE_ID_UNSAFE));
    return aclVersion >= 0 && sameAclSet(actual, hbaseNativeAcl);
  }

  private static boolean sameAclSet(List<ACL> actual, List<ACL> expected) {
    return actual.size() == expected.size()
        && actual.containsAll(expected)
        && expected.containsAll(actual);
  }

  private static Stat requireExactNode(ZooKeeper client, String path, byte[] expectedData,
      List<ACL> expectedAcls) throws KeeperException, InterruptedException {
    Stat dataStat = new Stat();
    byte[] actualData = client.getData(path, false, dataStat);
    Stat aclStat = new Stat();
    List<ACL> actualAcls = client.getACL(path, aclStat);
    if (!java.util.Arrays.equals(expectedData, actualData)
        || !expectedAcls.equals(actualAcls)
        || dataStat.getEphemeralOwner() != 0
        || dataStat.getVersion() != 0
        || aclStat.getAversion() != 0) {
      throw new IllegalStateException("Managed dependency ZooKeeper node metadata conflicts");
    }
    return dataStat;
  }

  private static byte[] parentMarker() {
    return "ambari-managed-hbase-parent:v1".getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] ledgerParentMarker() {
    return "ambari-managed-hbase-ledger:v1".getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] ledgerMarker(Request request) {
    String marker = "ambari-managed-hbase-binding-ledger:v1\n"
        + "bindingId=" + request.bindingId() + "\n"
        + "providerClusterId=" + request.providerClusterId() + "\n"
        + "container=" + request.container() + "\n"
        + "hbaseZnode=" + request.znode() + "\n"
        + "consumerSaslId=" + request.consumerSaslId() + "\n";
    return marker.getBytes(StandardCharsets.UTF_8);
  }

  private static List<ACL> privateAcl(String saslId) {
    return List.of(new ACL(ZooDefs.Perms.ALL, new Id("sasl", saslId)));
  }

  private static byte[] bindingMarker(Request request) {
    String marker = "ambari-managed-hbase-binding:v1\n"
        + "bindingId=" + request.bindingId() + "\n"
        + "providerClusterId=" + request.providerClusterId() + "\n"
        + "ownerUser=" + request.ownerUser() + "\n";
    return marker.getBytes(StandardCharsets.UTF_8);
  }

  private static Options options() {
    return new Options()
        .addOption(required("o", "operation", "managed dependency operation"))
        .addOption(required("c", "connection-string", "ZooKeeper connection string"))
        .addOption(required("p", "parent", "reserved parent znode"))
        .addOption(required("n", "container", "binding handoff container"))
        .addOption(required("z", "znode", "binding znode"))
        .addOption(required("b", "binding-id", "binding UUID"))
        .addOption(required("i", "provider-cluster-id", "provider cluster ID"))
        .addOption(required("u", "owner-user", "consumer short user"))
        .addOption(optional("l", "ledger", "provider-only binding ledger znode"))
        .addOption(optional("a", "provider-sasl-id", "provider ZooKeeper SASL authorization ID"))
        .addOption(optional("s", "consumer-sasl-id", "consumer ZooKeeper SASL authorization ID"))
        .addOption(optional("r", "probe-id", "VERIFY operation UUID"));
  }

  private static Option required(String shortName, String longName, String description) {
    Option option = optional(shortName, longName, description);
    option.setRequired(true);
    return option;
  }

  private static Option optional(String shortName, String longName, String description) {
    Option option = new Option(shortName, longName, true, description);
    option.setArgName(longName);
    return option;
  }

  record Request(String operation, String connectionString, String container, String znode,
      String ledger, UUID bindingId, long providerClusterId, String ownerUser,
      String providerSaslId, String consumerSaslId, UUID probeId) {
    static Request parse(CommandLine command) {
      String operation = command.getOptionValue("operation");
      if (!List.of("PREPARE_INSECURE", "VERIFY_INSECURE", "PREPARE_SECURE_LEDGER",
          "CREATE_SECURE_CONTAINER", "VERIFY_SECURE").contains(operation)) {
        throw new IllegalArgumentException("Unsupported managed dependency ZooKeeper operation");
      }
      String connection = bounded(command.getOptionValue("connection-string"), 4096,
          "connection-string");
      if (!PARENT.equals(command.getOptionValue("parent"))) {
        throw new IllegalArgumentException("Managed dependency ZooKeeper parent is not reserved");
      }
      UUID bindingId = canonicalUuid(command.getOptionValue("binding-id"), "binding-id");
      String container = command.getOptionValue("container");
      if (!(PARENT + "/" + bindingId).equals(container)) {
        throw new IllegalArgumentException("Managed dependency ZooKeeper container is not derived from binding-id");
      }
      String znode = command.getOptionValue("znode");
      if (!(container + "/hbase").equals(znode)) {
        throw new IllegalArgumentException("Managed dependency ZooKeeper znode is not derived from binding-id");
      }
      String clusterId = command.getOptionValue("provider-cluster-id");
      if (!POSITIVE_INTEGER.matcher(clusterId).matches()) {
        throw new IllegalArgumentException("provider-cluster-id must be a positive integer");
      }
      long providerClusterId;
      try {
        providerClusterId = Long.parseLong(clusterId);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("provider-cluster-id exceeds the supported range", e);
      }
      String owner = command.getOptionValue("owner-user");
      if (owner == null || !OWNER.matcher(owner).matches()) {
        throw new IllegalArgumentException("owner-user is invalid");
      }
      UUID probeId = null;
      boolean secureProvider = List.of("PREPARE_SECURE_LEDGER", "CREATE_SECURE_CONTAINER")
          .contains(operation);
      boolean secureConsumer = "VERIFY_SECURE".equals(operation);
      boolean verify = "VERIFY_INSECURE".equals(operation) || secureConsumer;
      String ledger = command.getOptionValue("ledger");
      String providerSaslId = command.getOptionValue("provider-sasl-id");
      String consumerSaslId = command.getOptionValue("consumer-sasl-id");
      if (secureProvider) {
        if (!(PARENT + "/.bindings/" + bindingId).equals(ledger)
            || !validIdentity(providerSaslId) || !validIdentity(consumerSaslId)
            || !owner.equals(consumerSaslId)) {
          throw new IllegalArgumentException("Secure provider handoff arguments are invalid");
        }
      } else if (secureConsumer) {
        if (ledger != null || providerSaslId != null || !validIdentity(consumerSaslId)
            || !owner.equals(consumerSaslId)) {
          throw new IllegalArgumentException("Secure consumer verification arguments are invalid");
        }
      } else if (ledger != null || providerSaslId != null || consumerSaslId != null) {
        throw new IllegalArgumentException("Secure arguments are allowed only for secure operations");
      }
      if (verify) {
        probeId = canonicalUuid(command.getOptionValue("probe-id"), "probe-id");
      } else if (command.hasOption("probe-id")) {
        throw new IllegalArgumentException("probe-id is allowed only for verification");
      }
      return new Request(operation, connection, container, znode, ledger, bindingId,
          providerClusterId, owner, providerSaslId, consumerSaslId, probeId);
    }

    private static boolean validIdentity(String value) {
      return value != null && OWNER.matcher(value).matches();
    }

    private static String bounded(String value, int maximum, String field) {
      if (value == null || value.isBlank() || value.length() > maximum
          || value.chars().anyMatch(character -> Character.isISOControl(character)
              || Character.isWhitespace(character))) {
        throw new IllegalArgumentException(field + " is missing, unsafe, or too long");
      }
      return value;
    }

    private static UUID canonicalUuid(String value, String field) {
      try {
        UUID parsed = UUID.fromString(value);
        if (!parsed.toString().equals(value)) {
          throw new IllegalArgumentException(field + " must be a canonical UUID");
        }
        return parsed;
      } catch (RuntimeException e) {
        throw new IllegalArgumentException(field + " must be a canonical UUID", e);
      }
    }
  }
}
