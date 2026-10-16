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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.stack.StackResolutionContext;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/** Immutable definition projections and Agent archives; publication is a separate operation. */
@Singleton
public class MpackSnapshots {
  public record Resource(String path, String provider, String digest, long size, boolean executable) {
    public Resource {
      MpackManifest.requirePath(path);
      MpackManifest.requireDigest(digest);
      if (provider == null || provider.isEmpty() || size < 0) {
        throw MpackJson.invalid("Invalid snapshot resource");
      }
    }
  }

  public record Snapshot(int schemaVersion, String id, String builtinId,
      List<MpackDependencies.Reference> releases, List<StackResolutionContext.Binding> bindings,
      Map<String, Resource> resources, Map<String, String> archiveDigests) {
    public Snapshot {
      if (schemaVersion != 1) {
        throw new MpackException(MpackException.Code.UNSUPPORTED_SCHEMA, "Unknown snapshot schema");
      }
      MpackManifest.requireDigest(id);
      if (builtinId != null) {
        MpackManifest.requireDigest(builtinId);
      }
      releases = List.copyOf(releases);
      bindings = List.copyOf(bindings);
      resources = Map.copyOf(resources);
      archiveDigests = Map.copyOf(archiveDigests);
      resources.forEach((key, value) -> {
        if (!key.equals(value.path())) {
          throw MpackJson.invalid("Snapshot resource key does not match its identity");
        }
      });
      archiveDigests.forEach((key, digest) -> {
        MpackManifest.requirePath(key);
        MpackManifest.requireDigest(digest);
        if (!key.startsWith("mpacks/" + id + "/")) {
          throw MpackJson.invalid("Archive belongs to a different definition snapshot");
        }
      });
    }
  }

  private static final Set<String> GENERATED = Set.of("archive.zip", ".hash",
      ".archive.sha256", ".resource-archive-digests.json");
  private final MpackArchiveStore store;
  private final MpackResources resources;
  private final Map<String, Path> builtinRoots;
  private final Set<String> sharedArchiveRoots = new TreeSet<>();

  @Inject
  public MpackSnapshots(MpackArchiveStore store, MpackResources resources, Configuration configuration) {
    this.store = store;
    this.resources = resources;
    builtinRoots = new TreeMap<>();
    addRoot("stacks", configuration.getMetadataPath());
    addRoot("common-services", configuration.getCommonServicesPath());
    addRoot("extensions", configuration.getExtensionsPath());
    Path resourceRoot = Path.of(configuration.getResourceDirPath());
    for (String name : List.of("custom_actions", "host_scripts", "custom_resources", "hooks")) {
      addRoot(name, resourceRoot.resolve(name).toString());
      sharedArchiveRoots.add(name);
    }
    String hooks = MpackManifest.requirePath(configuration.getProperty(Configuration.HOOKS_FOLDER));
    addRoot(hooks, resourceRoot.resolve(hooks).toString());
    sharedArchiveRoots.add(hooks);
  }

  public Path directory(String snapshotId) {
    return store.root().resolve("snapshots").resolve(MpackManifest.requireDigest(snapshotId));
  }

  public Path resourceRoot(String snapshotId) {
    return directory(snapshotId).resolve("resources");
  }

  public Snapshot captureBuiltins(List<StackResolutionContext.Binding> bindings) {
    Map<String, Resource> files = new TreeMap<>();
    Map<String, Path> sources = new TreeMap<>();
    try {
      for (Map.Entry<String, Path> root : builtinRoots.entrySet()) {
        if (!Files.isDirectory(root.getValue())) {
          continue;
        }
        try (Stream<Path> walk = Files.walk(root.getValue(), FileVisitOption.FOLLOW_LINKS)) {
          for (Path file : walk.filter(Files::isRegularFile).sorted().toList()) {
            String name = file.getFileName().toString();
            if (GENERATED.contains(name) || name.endsWith(".pyc")) {
              continue;
            }
            String target = root.getKey() + "/" + relative(root.getValue(), file);
            try (InputStream input = Files.newInputStream(file)) {
              files.put(target, new Resource(target, "builtin", DigestUtils.sha256Hex(input),
                  Files.size(file), Files.isExecutable(file)));
              sources.put(target, file);
            }
          }
        }
      }
      return materialize(null, List.of(), bindings, files, sources);
    } catch (IOException e) {
      throw new MpackException(MpackException.Code.STORAGE_FAILURE, "Unable to capture distribution resources");
    }
  }

  public Snapshot compose(Snapshot builtin, List<MpackResources.PreparedPack> packs,
      List<StackResolutionContext.Binding> bindings) {
    Map<String, Resource> files = new TreeMap<>(builtin.resources());
    Map<String, Path> sources = new TreeMap<>();
    Map<String, String> owners = new TreeMap<>();
    builtin.resources().forEach((path, resource) -> {
      sources.put(path, resourceRoot(builtin.id()).resolve(path));
      owners.put(logicalIdentity(path), resource.provider());
    });
    List<MpackDependencies.Reference> releases = new ArrayList<>();
    for (MpackResources.PreparedPack pack : packs) {
      String provider = pack.manifest().identity();
      releases.add(new MpackDependencies.Reference(provider, pack.archiveDigest()));
      MpackArchiveStore.StoredArchive archive = store.load(pack.archiveDigest());
      for (MpackResources.Contribution contribution : pack.contributions()) {
        String previous = owners.putIfAbsent(logicalIdentity(contribution.target()), provider);
        if (previous != null && !previous.equals(provider)) {
          throw new MpackException(MpackException.Code.RESOURCE_CONFLICT,
              "A definition is already owned by another provider",
              Map.of("resource", logicalIdentity(contribution.target()), "provider", previous,
                  "candidate_provider", provider));
        }
        Resource file = new Resource(contribution.target(), provider, contribution.digest(),
            contribution.size(), contribution.executable());
        if (files.putIfAbsent(file.path(), file) != null) {
          throw new MpackException(MpackException.Code.RESOURCE_CONFLICT,
              "A resource path already belongs to another contribution", Map.of("resource", file.path()));
        }
        sources.put(file.path(), resources.verifiedSource(archive, contribution));
      }
    }
    releases.sort(Comparator.comparing(MpackDependencies.Reference::release));
    try {
      return materialize(builtin.id(), releases, bindings, files, sources);
    } catch (IOException e) {
      throw new MpackException(MpackException.Code.STORAGE_FAILURE, "Unable to prepare definition snapshot");
    }
  }

  public Snapshot load(String id) {
    try {
      Snapshot snapshot = MpackJson.decode(Files.readString(directory(id).resolve("snapshot.json")), Snapshot.class);
      if (!id.equals(snapshot.id()) || !id.equals(identity(snapshot.builtinId(), snapshot.releases(),
          snapshot.bindings(), snapshot.resources(), relativeArchiveDigests(snapshot)))) {
        throw new MpackException(MpackException.Code.INVALID_RECEIPT, "Snapshot identity does not match its contents");
      }
      return snapshot;
    } catch (IOException e) {
      throw new MpackException(MpackException.Code.STORAGE_FAILURE, "Definition snapshot is unavailable");
    }
  }

  public void verify(Snapshot snapshot) {
    try {
      Set<String> observed = new TreeSet<>();
      try (Stream<Path> walk = Files.walk(resourceRoot(snapshot.id()))) {
        for (Path path : walk.toList()) {
          if (Files.isSymbolicLink(path)) {
            throw new MpackException(MpackException.Code.DIGEST_MISMATCH,
                "Published snapshot projections must not contain symbolic links");
          }
          if (Files.isRegularFile(path) && !GENERATED.contains(path.getFileName().toString())) {
            observed.add(relative(resourceRoot(snapshot.id()), path));
          }
        }
      }
      if (!observed.equals(snapshot.resources().keySet())) {
        throw new MpackException(MpackException.Code.DIGEST_MISMATCH,
            "Snapshot resource set no longer matches the accepted manifest");
      }
      for (Resource resource : snapshot.resources().values()) {
        verifyFile(resourceRoot(snapshot.id()).resolve(resource.path()), resource);
      }
      for (Map.Entry<String, String> archive : snapshot.archiveDigests().entrySet()) {
        String path = archive.getKey().substring(("mpacks/" + snapshot.id() + "/").length());
        try (InputStream input = Files.newInputStream(resourceRoot(snapshot.id()).resolve(path).resolve("archive.zip"))) {
          if (!archive.getValue().equals(DigestUtils.sha256Hex(input))) {
            throw new MpackException(MpackException.Code.DIGEST_MISMATCH, "Snapshot Agent archive has changed");
          }
        }
      }
    } catch (IOException e) {
      throw new MpackException(MpackException.Code.STORAGE_FAILURE, "Unable to verify definition snapshot");
    }
  }

  private Snapshot materialize(String builtin, List<MpackDependencies.Reference> releases,
      List<StackResolutionContext.Binding> bindings, Map<String, Resource> files,
      Map<String, Path> sources) throws IOException {
    List<StackResolutionContext.Binding> orderedBindings = bindings.stream()
        .sorted(Comparator.comparing(binding -> MpackJson.canonical(MpackJson.tree(binding)))).toList();
    Files.createDirectories(store.root().resolve("snapshots"));
    Path staging = Files.createTempDirectory(store.root().resolve("snapshots"), ".prepare-");
    Path root = staging.resolve("resources");
    try {
      for (String name : List.of("stacks", "common-services", "extensions")) {
        Files.createDirectories(root.resolve(name));
      }
      for (Resource resource : files.values()) {
        Path target = root.resolve(resource.path());
        Files.createDirectories(target.getParent());
        Files.copy(sources.get(resource.path()), target);
        if (Files.getFileStore(target).supportsFileAttributeView("posix")) {
          Files.setPosixFilePermissions(target, java.nio.file.attribute.PosixFilePermissions.fromString(
              resource.executable() ? "rwxr-xr-x" : "rw-r--r--"));
        }
        verifyFile(target, resource);
        MpackArchiveStore.forceFile(target);
      }
      Map<String, String> relativeDigests = prepareArchives(root, files);
      String id = identity(builtin, releases, orderedBindings, files, relativeDigests);
      Path destination = directory(id);
      if (Files.exists(destination)) {
        Snapshot existing = load(id);
        verify(existing);
        return existing;
      }
      Map<String, String> digests = new TreeMap<>();
      relativeDigests.forEach((path, digest) -> digests.put("mpacks/" + id + "/" + path, digest));
      Snapshot snapshot = new Snapshot(1, id, builtin, releases, orderedBindings, files, digests);
      Path receipt = staging.resolve("snapshot.json");
      Files.writeString(receipt, MpackJson.canonical(MpackJson.tree(snapshot)), StandardOpenOption.CREATE_NEW);
      MpackArchiveStore.forceFile(receipt);
      try (Stream<Path> walk = Files.walk(staging)) {
        for (Path directory : walk.filter(Files::isDirectory).sorted(Comparator.reverseOrder()).toList()) {
          MpackArchiveStore.forceDirectory(directory);
        }
      }
      try {
        Files.move(staging, destination, StandardCopyOption.ATOMIC_MOVE);
      } catch (java.nio.file.FileAlreadyExistsException | java.nio.file.DirectoryNotEmptyException concurrent) {
        Snapshot existing = load(id);
        verify(existing);
        return existing;
      }
      MpackArchiveStore.forceDirectory(destination.getParent());
      return snapshot;
    } finally {
      if (Files.exists(staging)) {
        try (Stream<Path> walk = Files.walk(staging)) {
          for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
            Files.deleteIfExists(path);
          }
        }
      }
    }
  }

  private Map<String, String> prepareArchives(Path root, Map<String, Resource> files) throws IOException {
    Set<String> directories = new TreeSet<>();
    org.apache.ambari.server.stack.ModuleFileUnmarshaller unmarshaller =
        new org.apache.ambari.server.stack.ModuleFileUnmarshaller();
    for (String path : files.keySet()) {
      sharedArchiveRoots.stream().filter(directory -> path.startsWith(directory + "/"))
          .forEach(directories::add);
      String[] segments = path.split("/");
      if (segments.length == 4 && segments[0].equals("stacks") && segments[3].equals("metainfo.xml")) {
        try {
          String hooks = unmarshaller.unmarshal(org.apache.ambari.server.state.stack.StackMetainfoXml.class,
              root.resolve(path).toFile()).getHooksFolder();
          if (hooks != null && !hooks.isEmpty()) {
            MpackManifest.requirePath(hooks);
            if (files.keySet().stream().noneMatch(file -> file.startsWith(hooks + "/"))) {
              throw MpackJson.invalid("Declared stack hooks directory contains no resources");
            }
            directories.add(hooks);
          }
        } catch (jakarta.xml.bind.JAXBException | javax.xml.stream.XMLStreamException | org.xml.sax.SAXException e) {
          throw MpackJson.invalid("Unable to validate stack hook metadata");
        }
      }
      for (int i = 0; i < segments.length - 1; i++) {
        if (Set.of("package", "hooks", "custom_actions", "host_scripts", "custom_resources").contains(segments[i])) {
          directories.add(String.join("/", java.util.Arrays.copyOf(segments, i + 1)));
          break;
        }
      }
    }
    Map<String, String> digests = new TreeMap<>();
    for (String directory : directories) {
      Path archive = root.resolve(directory).resolve("archive.zip");
      MessageDigest directoryDigest = DigestUtils.getSha256Digest();
      try (ZipArchiveOutputStream zip = new ZipArchiveOutputStream(archive.toFile())) {
        for (Resource resource : files.values().stream().sorted(Comparator.comparing(Resource::path)).toList()) {
          if (!resource.path().startsWith(directory + "/")) {
            continue;
          }
          String name = resource.path().substring(directory.length() + 1);
          byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
          Path source = root.resolve(resource.path());
          int mode = Files.getFileStore(source).supportsFileAttributeView("posix")
              ? ((Number) Files.getAttribute(source, "unix:mode")).intValue() & 07777
              : resource.executable() ? 0755 : 0644;
          directoryDigest.update(ByteBuffer.allocate(8).putLong(nameBytes.length).array());
          directoryDigest.update(nameBytes);
          directoryDigest.update(ByteBuffer.allocate(4).putInt(mode).array());
          directoryDigest.update(ByteBuffer.allocate(8).putLong(resource.size()).array());
          ZipArchiveEntry entry = new ZipArchiveEntry(name);
          entry.setTime(0);
          entry.setUnixMode(mode);
          zip.putArchiveEntry(entry);
          try (InputStream input = Files.newInputStream(source)) {
            byte[] buffer = new byte[65536];
            int count;
            while ((count = input.read(buffer)) != -1) {
              zip.write(buffer, 0, count);
              directoryDigest.update(buffer, 0, count);
            }
          }
          zip.closeArchiveEntry();
        }
      }
      MpackArchiveStore.forceFile(archive);
      Path hash = archive.getParent().resolve(".hash");
      Files.writeString(hash, HexFormat.of().formatHex(directoryDigest.digest()), StandardOpenOption.CREATE_NEW);
      MpackArchiveStore.forceFile(hash);
      try (InputStream input = Files.newInputStream(archive)) {
        digests.put(directory, DigestUtils.sha256Hex(input));
      }
    }
    return digests;
  }

  private static String identity(String builtin, List<MpackDependencies.Reference> releases,
      List<StackResolutionContext.Binding> bindings, Map<String, Resource> files, Map<String, String> archives) {
    ObjectNode identity = MpackJson.object().put("schema_version", 1);
    identity.put("builtin_id", builtin);
    identity.set("releases", MpackJson.tree(releases));
    identity.set("bindings", MpackJson.tree(bindings));
    identity.set("resources", MpackJson.tree(files));
    identity.set("archives", MpackJson.tree(archives));
    return MpackJson.digest(identity);
  }

  private static Map<String, String> relativeArchiveDigests(Snapshot snapshot) {
    Map<String, String> result = new TreeMap<>();
    String prefix = "mpacks/" + snapshot.id() + "/";
    snapshot.archiveDigests().forEach((path, digest) -> result.put(path.substring(prefix.length()), digest));
    return result;
  }

  private static void verifyFile(Path path, Resource resource) throws IOException {
    if (!Files.isRegularFile(path) || Files.size(path) != resource.size()
        || Files.isExecutable(path) != resource.executable()) {
      throw new MpackException(MpackException.Code.DIGEST_MISMATCH, "Snapshot resource attributes have changed");
    }
    try (InputStream input = Files.newInputStream(path)) {
      if (!resource.digest().equals(DigestUtils.sha256Hex(input))) {
        throw new MpackException(MpackException.Code.DIGEST_MISMATCH, "Snapshot resource contents have changed");
      }
    }
  }

  private void addRoot(String name, String path) {
    if (path != null && !path.isEmpty()) {
      builtinRoots.put(name, Path.of(path).toAbsolutePath().normalize());
    }
  }

  private static String relative(Path root, Path path) {
    return root.relativize(path).toString().replace(path.getFileSystem().getSeparator(), "/");
  }

  private static String logicalIdentity(String path) {
    String[] segments = path.split("/");
    if (segments.length >= 6 && segments[3].equals("services")) {
      return String.join("/", java.util.Arrays.copyOf(segments, 5));
    }
    if (segments.length >= 4 && Set.of("stacks", "extensions", "common-services").contains(segments[0])) {
      return String.join("/", java.util.Arrays.copyOf(segments, 3));
    }
    return segments.length > 1 ? segments[0] + "/" + segments[1] : path;
  }
}
