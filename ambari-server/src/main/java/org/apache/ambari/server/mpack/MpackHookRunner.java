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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.mpack.MpackLifecycleState.HookReceipt;
import org.apache.ambari.server.mpack.MpackLifecycleState.HookState;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/** Executes authorized hooks without interpreting their diagnostic output as state. */
@Singleton
public class MpackHookRunner {
  private final MpackArchiveStore archives;

  @Inject
  public MpackHookRunner(MpackArchiveStore archives) {
    this.archives = archives;
  }

  public HookReceipt run(MpackLifecycleState.Operation operation, MpackLifecycleState.Release release,
      MpackManifest.Hook hook) {
    Path receipt = receiptPath(operation, release, hook);
    Process process = null;
    try {
      if (Files.exists(receipt, LinkOption.NOFOLLOW_LINKS)) {
        throw new MpackException(MpackException.Code.RECOVERY_REQUIRED,
            "A prior hook receipt requires reconciliation before another execution");
      }
      Path packageRoot = archives.packageRoot(archives.verifyPrepared(release.archiveDigest()), "mpack.json");
      Path script = packageRoot.resolve(hook.script());
      if (!Files.isRegularFile(script) || !script.toRealPath().startsWith(packageRoot.toRealPath())) {
        throw new MpackException(MpackException.Code.INVALID_ARCHIVE, "Hook script leaves its package");
      }
      Files.createDirectories(receipt.getParent());
      List<String> command = hook.type().equals("python")
          ? List.of("/usr/bin/ambari-python-wrap", script.toString())
          : List.of("/bin/bash", script.toString());
      ProcessBuilder builder = new ProcessBuilder(command).directory(packageRoot.toFile());
      builder.environment().put("AMBARI_MPACK_OPERATION_ID", operation.id());
      builder.environment().put("AMBARI_MPACK_PLAN_DIGEST", operation.planDigest());
      builder.environment().put("AMBARI_MPACK_ARCHIVE_DIGEST", release.archiveDigest());
      builder.environment().put("AMBARI_MPACK_HOOK_PHASE", hook.phase());
      builder.environment().put("AMBARI_MPACK_HOOK_SCOPE", hook.scope());
      builder.environment().put("PYTHONDONTWRITEBYTECODE", "1");
      builder.environment().put("AMBARI_MPACK_HOOK_ATTEMPT", Integer.toString(attempt(operation, release, hook)));
      builder.environment().put("AMBARI_MPACK_RECEIPT_PATH", receipt.toString());
      builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
      builder.redirectError(ProcessBuilder.Redirect.DISCARD);
      process = builder.start();
      if (!process.waitFor(hook.timeoutSeconds(), TimeUnit.SECONDS)) {
        terminate(process);
        return unknown(operation, release, hook, "TIMEOUT");
      }
      HookReceipt observed = reconcile(operation, release, hook);
      if (process.exitValue() != 0 && observed.state() == HookState.APPLIED) {
        return unknown(operation, release, hook, "PROCESS_RECEIPT_CONFLICT");
      }
      return observed;
    } catch (IOException e) {
      return unknown(operation, release, hook, "PROCESS_OR_STORAGE_FAILURE");
    } catch (InterruptedException e) {
      if (process != null) {
        terminate(process);
      }
      Thread.currentThread().interrupt();
      return unknown(operation, release, hook, "INTERRUPTED");
    }
  }

  public HookReceipt reconcile(MpackLifecycleState.Operation operation,
      MpackLifecycleState.Release release, MpackManifest.Hook hook) {
    Path receipt = receiptPath(operation, release, hook);
    try {
      if (!Files.isRegularFile(receipt, LinkOption.NOFOLLOW_LINKS) || Files.size(receipt) > 1024 * 1024) {
        return unknown(operation, release, hook, "MISSING_RECEIPT");
      }
      HookReceipt observed = MpackJson.decode(Files.readString(receipt), HookReceipt.class);
      if (!observed.operationId().equals(operation.id()) || !observed.planDigest().equals(operation.planDigest())
          || !observed.archiveDigest().equals(release.archiveDigest()) || !observed.phase().equals(hook.phase())
          || observed.attempt() != attempt(operation, release, hook)
          || observed.state() == HookState.RUNNING) {
        return unknown(operation, release, hook, "FOREIGN_OR_INCOMPLETE_RECEIPT");
      }
      MpackArchiveStore.forceFile(receipt);
      MpackArchiveStore.forceDirectory(receipt.getParent());
      return observed;
    } catch (IOException | MpackException e) {
      return unknown(operation, release, hook, "INVALID_RECEIPT");
    }
  }

  public static String key(MpackLifecycleState.Release release, MpackManifest.Hook hook) {
    return release.archiveDigest() + "/" + hook.phase();
  }

  private Path receiptPath(MpackLifecycleState.Operation operation,
      MpackLifecycleState.Release release, MpackManifest.Hook hook) {
    return archives.root().resolve("receipts").resolve(operation.id()).resolve(release.archiveDigest())
        .resolve(hook.phase() + "-" + attempt(operation, release, hook) + ".json");
  }

  public static int attempt(MpackLifecycleState.Operation operation,
      MpackLifecycleState.Release release, MpackManifest.Hook hook) {
    HookReceipt current = operation.hooks().get(key(release, hook));
    if (current != null) {
      return current.attempt();
    }
    return Math.addExact(operation.hookHistory().stream()
        .filter(receipt -> receipt.archiveDigest().equals(release.archiveDigest()) && receipt.phase().equals(hook.phase()))
        .mapToInt(HookReceipt::attempt).max().orElse(0), 1);
  }

  private static HookReceipt unknown(MpackLifecycleState.Operation operation,
      MpackLifecycleState.Release release, MpackManifest.Hook hook, String reason) {
    return new HookReceipt(1, operation.id(), operation.planDigest(), release.archiveDigest(), hook.phase(),
        attempt(operation, release, hook), HookState.UNKNOWN, MpackLifecycleState.EffectState.UNKNOWN,
        Map.of("reason", reason));
  }

  private static void terminate(Process process) {
    process.descendants().forEach(ProcessHandle::destroyForcibly);
    process.destroyForcibly();
  }
}
