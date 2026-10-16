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
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.ambari.server.configuration.Configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/** A bundle is a verified transport container; its member releases retain their identities. */
@Singleton
public class MpackBundles {
  private final MpackArchiveStore archives;
  private final MpackResources resources;
  private final long expandedLimit;

  @Inject
  public MpackBundles(MpackArchiveStore archives, MpackResources resources, Configuration configuration) {
    this.archives = archives;
    this.resources = resources;
    expandedLimit = configuration.getMpackExpandedLimit();
  }

  public Map<String, Object> inspect(MpackArchiveStore.StoredArchive uploaded) {
    String manifest = manifestName(uploaded);
    if (manifest.equals("mpack.json")) {
      return describe(resources.inspect(uploaded.digest()));
    }
    Path root = archives.packageRoot(uploaded, "bundle.json");
    try {
      Path descriptor = root.resolve("bundle.json");
      if (Files.size(descriptor) > MpackManifest.MAX_MANIFEST_BYTES) {
        throw MpackJson.invalid("Bundle manifest exceeds the size limit");
      }
      JsonNode index = MpackJson.read(Files.readAllBytes(descriptor));
      MpackJson.fields(index, Set.of("schema_version", "packs", "$comment"));
      JsonNode version = index.get("schema_version");
      if (version == null || !version.isIntegralNumber() || !version.canConvertToInt() || version.intValue() != 1) {
        throw new MpackException(MpackException.Code.UNSUPPORTED_SCHEMA, "Bundle schema_version must be 1");
      }
      List<Map<String, Object>> members = new ArrayList<>();
      Set<String> identities = new HashSet<>();
      Set<String> memberPaths = new HashSet<>();
      long expanded = 0;
      for (JsonNode entry : MpackJson.array(index, "packs", true)) {
        if (members.size() >= 256) {
          throw new MpackException(MpackException.Code.UPLOAD_LIMIT, "A bundle may contain at most 256 packages");
        }
        MpackJson.fields(entry, Set.of("name", "version", "path", "digest"));
        String name = MpackManifest.requireName(MpackJson.string(entry, "name"));
        String packageVersion = MpackManifest.requireVersion(MpackJson.string(entry, "version"));
        String path = MpackManifest.requirePath(MpackJson.string(entry, "path"));
        String digest = MpackManifest.requireDigest(MpackJson.string(entry, "digest"));
        Path member = root.resolve(path);
        if (!path.startsWith("mpacks/") || !memberPaths.add(path) || !identities.add(name + "/" + packageVersion)
            || !Files.isRegularFile(member, LinkOption.NOFOLLOW_LINKS) || !member.toRealPath().startsWith(root.toRealPath())) {
          throw MpackJson.invalid("Bundle contains a duplicate, missing, or invalid member identity");
        }
        MpackArchiveStore.StoredArchive stored;
        try (InputStream input = Files.newInputStream(member)) {
          stored = archives.accept(input, digest);
        }
        if (!manifestName(stored).equals("mpack.json")) {
          throw MpackJson.invalid("Nested bundles are not supported");
        }
        try (Stream<Path> walk = Files.walk(stored.content(), FileVisitOption.FOLLOW_LINKS)) {
          for (Path file : (Iterable<Path>) walk.filter(Files::isRegularFile)::iterator) {
            long size = Files.size(file);
            if (size > expandedLimit - expanded) {
              throw new MpackException(MpackException.Code.UPLOAD_LIMIT, "Bundle members exceed the total expanded size limit");
            }
            expanded += size;
          }
        }
        MpackResources.PreparedPack prepared = resources.inspect(digest);
        if (!name.equals(prepared.manifest().name()) || !packageVersion.equals(prepared.manifest().version())) {
          throw new MpackException(MpackException.Code.INVALID_RECEIPT, "Bundle index and member manifest identities differ");
        }
        members.add(describe(prepared));
      }
      return Map.of("schema_version", 1, "archive_digest", uploaded.digest(), "bundle", true, "members", members);
    } catch (IOException e) {
      throw new MpackException(MpackException.Code.INVALID_ARCHIVE, "Bundle contents cannot be verified");
    }
  }

  private Map<String, Object> describe(MpackResources.PreparedPack prepared) {
    return Map.of("schema_version", 1, "archive_digest", prepared.archiveDigest(),
        "name", prepared.manifest().name(), "version", prepared.manifest().version(),
        "resources_digest", prepared.resourcesDigest(), "resource_count", prepared.contributions().size(),
        "extensions", resources.extensions(prepared), "stacks", resources.stacks(prepared));
  }

  private String manifestName(MpackArchiveStore.StoredArchive archive) {
    List<Path> roots = new ArrayList<>(List.of(archive.content()));
    try (Stream<Path> entries = Files.list(archive.content())) {
      List<Path> children = entries.toList();
      if (children.size() == 1 && Files.isDirectory(children.get(0), LinkOption.NOFOLLOW_LINKS)) {
        roots.add(children.get(0));
      }
    } catch (IOException e) {
      throw new MpackException(MpackException.Code.INVALID_ARCHIVE, "Cannot inspect the archive root");
    }
    List<String> manifests = new ArrayList<>();
    for (Path root : roots) {
      for (String name : List.of("mpack.json", "bundle.json")) {
        if (Files.isRegularFile(root.resolve(name), LinkOption.NOFOLLOW_LINKS)) {
          manifests.add(name);
        }
      }
    }
    if (manifests.size() != 1) {
      throw MpackJson.invalid("Archive requires exactly one package or bundle manifest");
    }
    return manifests.get(0);
  }
}
