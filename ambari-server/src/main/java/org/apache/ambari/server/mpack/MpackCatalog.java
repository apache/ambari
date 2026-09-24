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

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.ambari.server.orm.dao.MpackRecordDAO;
import org.apache.ambari.server.orm.entities.MpackRecordEntity.Kind;
import org.apache.commons.codec.digest.DigestUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/** Typed access to private lifecycle records, including identity validation. */
@Singleton
public class MpackCatalog {
  public static final String CONTROL = "control/global";
  private final MpackRecordDAO dao;

  public record Versioned<T>(String key, long revision, T value) {
  }

  @Inject
  public MpackCatalog(MpackRecordDAO dao) {
    this.dao = dao;
  }

  public Versioned<MpackLifecycleState.Control> control() {
    return read(CONTROL, Kind.CONTROL, MpackLifecycleState.Control.class, false);
  }

  public Versioned<MpackLifecycleState.Release> release(String identity) {
    Versioned<MpackLifecycleState.Release> result = read(releaseKey(identity), Kind.RELEASE,
        MpackLifecycleState.Release.class, true);
    if (!identity.equals(result.value().id())) {
      throw foreign();
    }
    return result;
  }

  public List<Versioned<MpackLifecycleState.Release>> releases() {
    return dao.list(Kind.RELEASE).stream().map(value -> {
      MpackLifecycleState.Release release = MpackJson.decode(value.payload(), MpackLifecycleState.Release.class);
      if (!releaseKey(release.id()).equals(value.id())) {
        throw foreign();
      }
      return new Versioned<>(value.id(), value.revision(), release);
    }).toList();
  }

  public Versioned<MpackLifecycleState.Plan> plan(String id) {
    Versioned<MpackLifecycleState.Plan> result = read("plan/" + id, Kind.PLAN,
        MpackLifecycleState.Plan.class, true);
    com.fasterxml.jackson.databind.node.ObjectNode body = (com.fasterxml.jackson.databind.node.ObjectNode)
        MpackJson.tree(result.value());
    body.remove("digest");
    if (!id.equals(result.value().id()) || !result.value().digest().equals(MpackJson.digest(body))) {
      throw foreign();
    }
    return result;
  }

  public Versioned<MpackLifecycleState.Operation> operation(String id) {
    Versioned<MpackLifecycleState.Operation> result = read("operation/" + id, Kind.OPERATION,
        MpackLifecycleState.Operation.class, true);
    if (!id.equals(result.value().id())) {
      throw foreign();
    }
    MpackLifecycleState.Plan plan = plan(result.value().planId()).value();
    if (!plan.digest().equals(result.value().planDigest())) {
      throw foreign();
    }
    if (result.value().phase() == MpackLifecycleState.Phase.SUCCEEDED) {
      java.util.Set<String> required = new java.util.HashSet<>();
      List<MpackLifecycleState.Release> members = plan.mutation().action() == MpackLifecycleState.Action.UNINSTALL
          ? plan.mutation().releaseIds().stream().map(releaseId -> release(releaseId).value()).toList()
          : plan.releases().stream().filter(release -> plan.mutation().action() == MpackLifecycleState.Action.ENABLE
              ? plan.initializingReleases().contains(release.id())
              : plan.mutation().archiveDigests().contains(release.archiveDigest())).toList();
      members.forEach(release -> required.addAll(MpackLifecycleState.requiredHooks(plan, release)));
      if (!plan.candidateSnapshot().equals(result.value().effectiveSnapshot())
          || !required.equals(result.value().hooks().keySet())) {
        throw foreign();
      }
    }
    return result;
  }

  public List<Versioned<MpackLifecycleState.Operation>> operations() {
    return dao.list(Kind.OPERATION).stream().map(value -> {
      MpackLifecycleState.Operation operation = MpackJson.decode(value.payload(), MpackLifecycleState.Operation.class);
      if (!value.id().equals("operation/" + operation.id())) {
        throw foreign();
      }
      return operation(operation.id());
    }).toList();
  }

  public Versioned<MpackLifecycleState.Idempotency> idempotency(int ownerId, String key) {
    return read(idempotencyKey(ownerId, key), Kind.IDEMPOTENCY,
        MpackLifecycleState.Idempotency.class, false);
  }

  public void apply(List<MpackRecordDAO.Change> changes) {
    dao.apply(changes);
  }

  public static MpackRecordDAO.Change change(String key, Kind kind, long revision, Object value) {
    return new MpackRecordDAO.Change(key, kind, revision, MpackJson.canonical(MpackJson.tree(value)));
  }

  public static String releaseKey(String identity) {
    return "release/" + DigestUtils.sha256Hex(identity.getBytes(StandardCharsets.UTF_8));
  }

  public static String idempotencyKey(int ownerId, String key) {
    if (key == null || key.isEmpty() || key.length() > 200
        || key.chars().anyMatch(value -> value < 33 || value > 126)) {
      throw MpackJson.invalid("An ASCII idempotency key of 1 through 200 characters is required");
    }
    return "idempotency/" + DigestUtils.sha256Hex((ownerId + ":mpack-lifecycle:" + key)
        .getBytes(StandardCharsets.UTF_8));
  }

  private <T> Versioned<T> read(String key, Kind kind, Class<T> type, boolean required) {
    MpackRecordDAO.Value value = dao.find(key);
    if (value == null) {
      if (required) {
        throw new MpackException(MpackException.Code.NOT_FOUND, "Management pack resource was not found");
      }
      return null;
    }
    if (value.kind() != kind) {
      throw foreign();
    }
    return new Versioned<>(key, value.revision(), MpackJson.decode(value.payload(), type));
  }

  private static MpackException foreign() {
    return new MpackException(MpackException.Code.INVALID_RECEIPT,
        "Stored record does not match its requested identity or content digest");
  }
}
