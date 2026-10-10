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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/** Normalizes V1 resource layout without duplicating StackManager inheritance. */
@Singleton
public class MpackResources {
  public record Contribution(String archiveDigest, String artifactName, String source,
      String target, String scope, String logicalIdentity, String digest, long size,
      boolean executable) {
  }

  public record PreparedPack(MpackManifest manifest, String archiveDigest,
      List<Contribution> contributions, String resourcesDigest) {
    public PreparedPack {
      contributions = List.copyOf(contributions);
    }
  }

  private static final Set<String> GENERATED = Set.of("archive.zip", ".hash",
      ".resource-archive-digests.json", ".archive.sha256");
  private final MpackArchiveStore store;

  @Inject
  public MpackResources(MpackArchiveStore store) {
    this.store = store;
  }

  public PreparedPack inspect(String archiveDigest) {
    MpackArchiveStore.StoredArchive archive = store.load(archiveDigest);
    Path packageRoot = store.packageRoot(archive, "mpack.json");
    try {
      Path manifestPath = packageRoot.resolve("mpack.json");
      if (Files.size(manifestPath) > MpackManifest.MAX_MANIFEST_BYTES) {
        throw MpackJson.invalid("Manifest exceeds the size limit");
      }
      MpackManifest manifest = MpackManifest.parse(Files.readAllBytes(manifestPath));
      List<Contribution> contributions = new ArrayList<>();
      Set<String> targets = new HashSet<>();
      for (MpackManifest.Artifact artifact : manifest.artifacts()) {
        Path source = packageRoot.resolve(artifact.sourceDir());
        verifyContained(packageRoot, source);
        if (!Files.isDirectory(source)) {
          throw MpackJson.invalid("Artifact source_dir must name an existing directory");
        }
        if (artifact.type().equals("stack-addon-service-definitions")) {
          for (MpackManifest.Addon addon : artifact.addons()) {
            Path addonSource = source.resolve(addon.serviceName()).resolve(addon.serviceVersion());
            if (!Files.isDirectory(addonSource)) {
              throw MpackJson.invalid("Addon mapping references a missing service directory");
            }
            for (MpackManifest.Target target : addon.targets()) {
              collect(archive, packageRoot, artifact, addonSource,
                  "stacks/" + target.identity() + "/services/" + addon.serviceName(), contributions, targets);
            }
          }
        } else {
          String prefix = switch (artifact.type()) {
            case "stack-definitions" -> "stacks";
            case "service-definitions" -> "common-services";
            case "extension-definitions" -> "extensions";
            default -> throw MpackJson.invalid("Unsupported artifact type");
          };
          collect(archive, packageRoot, artifact, source, prefix, contributions, targets);
        }
      }
      for (MpackManifest.Hook hook : manifest.hooks()) {
        Path script = packageRoot.resolve(hook.script());
        verifyContained(packageRoot, script);
        if (!Files.isRegularFile(script)) {
          throw MpackJson.invalid("Hook script must name an existing regular file");
        }
      }
      if (contributions.isEmpty()) {
        throw MpackJson.invalid("Management pack contributes no resources");
      }
      contributions.sort(Comparator.comparing(Contribution::target));
      return new PreparedPack(manifest, archiveDigest, contributions,
          MpackJson.digest(MpackJson.tree(contributions)));
    } catch (IOException | UncheckedIOException e) {
      throw new MpackException(MpackException.Code.INVALID_ARCHIVE,
          "Unable to resolve management pack resource contents");
    }
  }

  public Path verifiedSource(Contribution contribution) {
    return verifiedSource(store.load(contribution.archiveDigest()), contribution);
  }

  public List<java.util.Map<String, Object>> extensions(PreparedPack pack) {
    List<java.util.Map<String, Object>> result = new ArrayList<>();
    org.apache.ambari.server.stack.ModuleFileUnmarshaller unmarshaller =
        new org.apache.ambari.server.stack.ModuleFileUnmarshaller();
    MpackArchiveStore.StoredArchive archive = store.load(pack.archiveDigest());
    for (Contribution contribution : pack.contributions()) {
      String[] parts = contribution.target().split("/");
      if (parts.length == 4 && parts[0].equals("extensions") && parts[3].equals("metainfo.xml")) {
        try {
          org.apache.ambari.server.state.stack.ExtensionMetainfoXml info = unmarshaller.unmarshal(
              org.apache.ambari.server.state.stack.ExtensionMetainfoXml.class,
              verifiedSource(archive, contribution).toFile());
          result.add(java.util.Map.of("name", parts[1], "version", parts[2], "minimum_stacks",
              info.getStacks().stream().map(stack -> java.util.Map.of(
                  "stack_name", stack.getName(), "stack_version", stack.getVersion())).toList()));
        } catch (IOException | jakarta.xml.bind.JAXBException | javax.xml.stream.XMLStreamException | org.xml.sax.SAXException e) {
          throw MpackJson.invalid("Extension metadata cannot be inspected");
        }
      }
    }
    return List.copyOf(result);
  }

  public List<java.util.Map<String, String>> stacks(PreparedPack pack) {
    List<java.util.Map<String, String>> result = new ArrayList<>();
    org.apache.ambari.server.stack.ModuleFileUnmarshaller unmarshaller =
        new org.apache.ambari.server.stack.ModuleFileUnmarshaller();
    MpackArchiveStore.StoredArchive archive = store.load(pack.archiveDigest());
    for (Contribution contribution : pack.contributions()) {
      String[] parts = contribution.target().split("/");
      if (parts.length == 4 && parts[0].equals("stacks") && parts[3].equals("metainfo.xml")) {
        try {
          org.apache.ambari.server.state.stack.StackMetainfoXml info = unmarshaller.unmarshal(
              org.apache.ambari.server.state.stack.StackMetainfoXml.class, verifiedSource(archive, contribution).toFile());
          if (info.getVersion().isActive()) {
            result.add(java.util.Map.of("stack_name", parts[1], "stack_version", parts[2]));
          }
        } catch (IOException | jakarta.xml.bind.JAXBException | javax.xml.stream.XMLStreamException | org.xml.sax.SAXException e) {
          throw MpackJson.invalid("Stack metadata cannot be inspected");
        }
      }
    }
    return List.copyOf(result);
  }

  public Path verifiedSource(MpackArchiveStore.StoredArchive archive, Contribution contribution) {
    MpackManifest.requireDigest(contribution.archiveDigest());
    MpackManifest.requirePath(contribution.source());
    MpackManifest.requireDigest(contribution.digest());
    if (!archive.digest().equals(contribution.archiveDigest())) {
      throw new MpackException(MpackException.Code.INVALID_RECEIPT,
          "Resource contribution belongs to a different archive");
    }
    Path content = archive.content();
    Path source = content.resolve(contribution.source());
    try {
      verifyContained(content, source);
      if (!Files.isRegularFile(source) || Files.size(source) != contribution.size()
          || Files.isExecutable(source) != contribution.executable()) {
        throw new MpackException(MpackException.Code.DIGEST_MISMATCH,
            "Resource no longer matches its accepted contribution");
      }
      try (InputStream stream = Files.newInputStream(source)) {
        if (!contribution.digest().equals(DigestUtils.sha256Hex(stream))) {
          throw new MpackException(MpackException.Code.DIGEST_MISMATCH,
              "Resource digest no longer matches its accepted contribution");
        }
      }
      return source;
    } catch (IOException e) {
      throw new MpackException(MpackException.Code.STORAGE_FAILURE,
          "Unable to verify accepted management pack resources");
    }
  }

  private void collect(MpackArchiveStore.StoredArchive archive, Path packageRoot,
      MpackManifest.Artifact artifact, Path source, String targetPrefix,
      List<Contribution> contributions, Set<String> targets) throws IOException {
    try (Stream<Path> walk = Files.walk(source, FileVisitOption.FOLLOW_LINKS)) {
      for (Path file : walk.filter(Files::isRegularFile).sorted().toList()) {
        verifyContained(packageRoot, file);
        String name = file.getFileName().toString();
        if (GENERATED.contains(name) || name.endsWith(".pyc")) {
          throw MpackJson.invalid("Artifacts must not contain generated Agent archives or Python bytecode");
        }
        String relative = source.relativize(file).toString().replace(file.getFileSystem().getSeparator(), "/");
        String target = MpackManifest.requirePath(targetPrefix + "/" + relative);
        if (!targets.add(target)) {
          throw new MpackException(MpackException.Code.RESOURCE_CONFLICT,
              "Artifacts contribute the same target resource");
        }
        String sourcePath = archive.content().relativize(file).toString()
            .replace(file.getFileSystem().getSeparator(), "/");
        String[] parts = target.split("/");
        String scope = parts.length >= 4 ? parts[0] + "/" + parts[1] + "/" + parts[2] : "global";
        String identity = scope;
        if (parts.length >= 6 && "services".equals(parts[3])) {
          identity = scope + "/services/" + parts[4];
        }
        try (InputStream stream = Files.newInputStream(file)) {
          contributions.add(new Contribution(archive.digest(), artifact.name(), sourcePath,
              target, scope, identity, DigestUtils.sha256Hex(stream), Files.size(file), Files.isExecutable(file)));
        }
      }
    }
  }

  private static void verifyContained(Path root, Path path) throws IOException {
    if (!path.toRealPath().startsWith(root.toRealPath())) {
      throw new MpackException(MpackException.Code.INVALID_ARCHIVE,
          "Resource link leaves its package boundary");
    }
  }
}
