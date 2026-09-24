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

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static org.apache.ambari.server.mpack.MpackException.Code.INVALID_ARCHIVE;
import static org.apache.ambari.server.mpack.MpackException.Code.STORAGE_FAILURE;
import static org.apache.ambari.server.mpack.MpackException.Code.UPLOAD_LIMIT;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/** Bounded, isolated archive preparation. This class never activates definitions. */
@Singleton
public class MpackArchiveStore {
  public record Limits(long uploadBytes, long expandedBytes, int entries) {
    public Limits {
      if (uploadBytes < 1 || expandedBytes < 1 || entries < 1) {
        throw new IllegalArgumentException("Archive limits must be positive");
      }
    }
  }

  public record StoredArchive(String digest, Path directory, Path content) {
  }

  private record Link(Path path, String target, boolean symbolic) {
  }

  private final Path root;
  private final Limits limits;

  @Inject
  public MpackArchiveStore(Configuration configuration) {
    this(Path.of(configuration.getMpackManagedPath()),
        new Limits(configuration.getMpackUploadLimit(), configuration.getMpackExpandedLimit(),
            configuration.getMpackEntryLimit()));
  }

  public MpackArchiveStore(Path root, Limits limits) {
    this.root = root.toAbsolutePath().normalize();
    this.limits = limits;
  }

  public Path root() {
    return root;
  }

  public StoredArchive accept(InputStream input, String expectedDigest) {
    if (expectedDigest != null) {
      MpackManifest.requireDigest(expectedDigest);
    }
    Path temporary = null;
    try {
      Files.createDirectories(root.resolve("staging"));
      Files.createDirectories(root.resolve("archives"));
      temporary = Files.createTempDirectory(root.resolve("staging"), "upload-");
      Path archive = temporary.resolve("source.tar.gz");
      MessageDigest digest = DigestUtils.getSha256Digest();
      try (OutputStream output = Files.newOutputStream(archive, StandardOpenOption.CREATE_NEW)) {
        copy(new DigestInputStream(input, digest), output, limits.uploadBytes());
      }
      forceFile(archive);
      String actualDigest = HexFormat.of().formatHex(digest.digest());
      if (expectedDigest != null && !expectedDigest.equals(actualDigest)) {
        throw new MpackException(MpackException.Code.DIGEST_MISMATCH,
            "Uploaded content does not match the supplied SHA-256 digest");
      }
      Path content = temporary.resolve("content");
      Files.createDirectory(content);
      try {
        extract(archive, content);
      } catch (IOException | UncheckedIOException | IllegalArgumentException e) {
        throw new MpackException(INVALID_ARCHIVE, "Archive content cannot be safely extracted");
      }
      forceDirectories(content);
      forceDirectory(temporary);
      Path destination = root.resolve("archives").resolve(actualDigest);
      if (Files.exists(destination, NOFOLLOW_LINKS)) {
        StoredArchive stored = load(actualDigest);
        verifyTree(content, stored.content());
        return stored;
      }
      try {
        Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
        temporary = null;
      } catch (FileAlreadyExistsException | DirectoryNotEmptyException race) {
        StoredArchive stored = load(actualDigest);
        verifyTree(content, stored.content());
        return stored;
      }
      forceDirectory(destination.getParent());
      return new StoredArchive(actualDigest, destination, destination.resolve("content"));
    } catch (IOException e) {
      throw new MpackException(STORAGE_FAILURE, "Unable to prepare management pack storage");
    } finally {
      if (temporary != null) {
        removeStaging(temporary);
      }
    }
  }

  public StoredArchive load(String digest) {
    MpackManifest.requireDigest(digest);
    Path directory = root.resolve("archives").resolve(digest);
    Path archive = directory.resolve("source.tar.gz");
    try {
      if (!Files.isRegularFile(archive, NOFOLLOW_LINKS)
          || Files.isSymbolicLink(directory) || !Files.isDirectory(directory.resolve("content"), NOFOLLOW_LINKS)) {
        throw new MpackException(MpackException.Code.NOT_FOUND, "Uploaded archive is unavailable");
      }
      try (InputStream input = Files.newInputStream(archive)) {
        if (!digest.equals(DigestUtils.sha256Hex(input))) {
          throw new MpackException(MpackException.Code.DIGEST_MISMATCH,
              "Stored archive content has changed");
        }
      }
      return new StoredArchive(digest, directory, directory.resolve("content"));
    } catch (IOException e) {
      throw new MpackException(STORAGE_FAILURE, "Unable to read management pack storage");
    }
  }

  public StoredArchive verifyPrepared(String digest) {
    StoredArchive stored = load(digest);
    try (InputStream input = Files.newInputStream(stored.directory().resolve("source.tar.gz"))) {
      return accept(input, digest);
    } catch (IOException e) {
      throw new MpackException(STORAGE_FAILURE, "Unable to verify prepared archive contents");
    }
  }

  /** Accept either a root manifest or one containing directory, never a fuzzy search. */
  public Path packageRoot(StoredArchive archive, String manifestName) {
    if (!Set.of("mpack.json", "bundle.json").contains(manifestName)) {
      throw new IllegalArgumentException("Unknown archive manifest");
    }
    Path content = archive.content();
    if (Files.isRegularFile(content.resolve(manifestName), NOFOLLOW_LINKS)) {
      return content;
    }
    try (Stream<Path> entries = Files.list(content)) {
      List<Path> children = entries.toList();
      if (children.size() == 1 && Files.isDirectory(children.get(0), NOFOLLOW_LINKS)
          && Files.isRegularFile(children.get(0).resolve(manifestName), NOFOLLOW_LINKS)) {
        return children.get(0);
      }
    } catch (IOException e) {
      throw new MpackException(STORAGE_FAILURE, "Unable to read archive manifest location");
    }
    throw new MpackException(INVALID_ARCHIVE,
        "Archive must contain its manifest at the root or in a single containing directory");
  }

  private void extract(Path archive, Path content) throws IOException {
    Set<String> names = new HashSet<>();
    List<Link> links = new ArrayList<>();
    long expanded = 0;
    int count = 0;
    try (TarArchiveInputStream tar = new TarArchiveInputStream(new GzipCompressorInputStream(
        new BufferedInputStream(Files.newInputStream(archive))))) {
      TarArchiveEntry entry;
      while ((entry = tar.getNextTarEntry()) != null) {
        if (++count > limits.entries()) {
          throw new MpackException(UPLOAD_LIMIT, "Archive has too many entries");
        }
        String name = entry.getName();
        while (name.startsWith("./")) {
          name = name.substring(2);
        }
        if (entry.isDirectory() && (name.isEmpty() || name.equals("."))) {
          continue;
        }
        if (entry.isDirectory() && name.endsWith("/")) {
          name = name.substring(0, name.length() - 1);
        }
        requireArchivePath(name);
        if (!names.add(name)) {
          throw new MpackException(INVALID_ARCHIVE, "Duplicate archive member");
        }
        Path destination = content.resolve(name);
        if (entry.isDirectory()) {
          Files.createDirectories(destination);
        } else if (entry.isSymbolicLink() || entry.isLink()) {
          if (entry.getSize() != 0) {
            throw new MpackException(INVALID_ARCHIVE, "Archive links must not contain data");
          }
          links.add(new Link(destination, entry.getLinkName(), entry.isSymbolicLink()));
        } else if (entry.isFile() && !entry.isSparse()) {
          if (entry.getSize() < 0 || entry.getSize() > limits.expandedBytes() - expanded) {
            throw new MpackException(UPLOAD_LIMIT, "Expanded archive exceeds the size limit");
          }
          Files.createDirectories(destination.getParent());
          try (OutputStream output = Files.newOutputStream(destination, StandardOpenOption.CREATE_NEW)) {
            long written = copy(tar, output, limits.expandedBytes() - expanded);
            if (written != entry.getSize()) {
              throw new MpackException(INVALID_ARCHIVE, "Archive member size does not match its header");
            }
            expanded += written;
          }
          setMode(destination, entry.getMode());
          forceFile(destination);
        } else {
          throw new MpackException(INVALID_ARCHIVE, "Unsupported archive member type");
        }
      }
    }
    // Link creation is delayed so no later extraction write can follow a link.
    // No link may be the parent of another link, including forward references.
    Set<Path> linkPaths = new HashSet<>();
    links.forEach(link -> linkPaths.add(link.path()));
    for (Link link : links) {
      for (Path parent = link.path().getParent(); parent.startsWith(content);
          parent = parent.getParent()) {
        if (linkPaths.contains(parent)) {
          throw new MpackException(INVALID_ARCHIVE, "Archive links cannot contain other link entries");
        }
      }
    }
    links.sort(Comparator.comparing(Link::symbolic));
    for (Link link : links) {
      String target = link.target();
      if (target == null || target.isEmpty() || target.startsWith("/")
          || target.contains("\\") || target.contains(":")
          || target.chars().anyMatch(character -> character < 32 || character == 127)) {
        throw new MpackException(INVALID_ARCHIVE, "Invalid archive link target");
      }
      Path resolved = (link.symbolic() ? link.path().getParent() : content).resolve(target).normalize();
      if (!resolved.startsWith(content) || resolved.equals(content)) {
        throw new MpackException(INVALID_ARCHIVE, "Archive link escapes its content root");
      }
      Files.createDirectories(link.path().getParent());
      if (link.symbolic()) {
        Files.createSymbolicLink(link.path(), Path.of(target));
      } else {
        if (!Files.isRegularFile(resolved, NOFOLLOW_LINKS)) {
          throw new MpackException(INVALID_ARCHIVE, "Hard link must refer to an existing regular file");
        }
        Files.createLink(link.path(), resolved);
      }
    }
    for (Link link : links) {
      if (!link.path().toRealPath().startsWith(content.toRealPath())) {
        throw new MpackException(INVALID_ARCHIVE, "Archive link escapes its content root");
      }
    }
    try (Stream<Path> walk = Files.walk(content, FileVisitOption.FOLLOW_LINKS)) {
      long materializedBytes = 0;
      int materializedEntries = 0;
      for (Path path : (Iterable<Path>) walk::iterator) {
        if (path.equals(content)) {
          continue;
        }
        if (++materializedEntries > limits.entries()) {
          throw new MpackException(UPLOAD_LIMIT, "Expanded links exceed the entry limit");
        }
        if (Files.isRegularFile(path)) {
          long size = Files.size(path);
          if (size > limits.expandedBytes() - materializedBytes) {
            throw new MpackException(UPLOAD_LIMIT, "Materialized links exceed the expanded size limit");
          }
          materializedBytes += size;
        }
      }
    }
  }

  private void verifyTree(Path candidate, Path existing) throws IOException {
    List<Path> paths;
    try (Stream<Path> walk = Files.walk(candidate)) {
      paths = walk.filter(path -> !Files.isDirectory(path, NOFOLLOW_LINKS)).toList();
    }
    for (Path path : paths) {
      Path stored = existing.resolve(candidate.relativize(path));
      if (Files.isSymbolicLink(path)) {
        if (!Files.isSymbolicLink(stored)
            || !Files.readSymbolicLink(path).equals(Files.readSymbolicLink(stored))) {
          throw new MpackException(MpackException.Code.DIGEST_MISMATCH, "Stored archive link has changed");
        }
      } else if (!Files.isRegularFile(stored, NOFOLLOW_LINKS) || Files.mismatch(path, stored) != -1) {
        throw new MpackException(MpackException.Code.DIGEST_MISMATCH, "Stored archive resource has changed");
      }
    }
    try (Stream<Path> walk = Files.walk(existing)) {
      if (walk.filter(path -> !Files.isDirectory(path, NOFOLLOW_LINKS)).count() != paths.size()) {
        throw new MpackException(MpackException.Code.DIGEST_MISMATCH, "Stored archive resource set has changed");
      }
    }
  }

  private static long copy(InputStream input, OutputStream output, long limit) throws IOException {
    byte[] buffer = new byte[64 * 1024];
    long count = 0;
    int read;
    while ((read = input.read(buffer)) != -1) {
      if (read > limit - count) {
        throw new MpackException(UPLOAD_LIMIT, "Archive exceeds the configured size limit");
      }
      output.write(buffer, 0, read);
      count += read;
    }
    return count;
  }

  private static void requireArchivePath(String name) {
    try {
      MpackManifest.requirePath(name);
    } catch (MpackException e) {
      throw new MpackException(INVALID_ARCHIVE, "Archive contains an unsafe member path");
    }
  }

  private static void setMode(Path path, int mode) throws IOException {
    if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
      Set<PosixFilePermission> permissions = new HashSet<>(Set.of(
          PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
      if ((mode & 0100) != 0) {
        permissions.add(PosixFilePermission.OWNER_EXECUTE);
      }
      if ((mode & 0040) != 0) {
        permissions.add(PosixFilePermission.GROUP_READ);
      }
      if ((mode & 0004) != 0) {
        permissions.add(PosixFilePermission.OTHERS_READ);
      }
      Files.setPosixFilePermissions(path, permissions);
    }
  }

  static void forceFile(Path path) throws IOException {
    try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
      channel.force(true);
    }
  }

  static void forceDirectory(Path path) throws IOException {
    try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
      channel.force(true);
    }
  }

  private static void forceDirectories(Path root) throws IOException {
    try (Stream<Path> walk = Files.walk(root)) {
      for (Path path : walk.filter(value -> Files.isDirectory(value, NOFOLLOW_LINKS))
          .sorted(Comparator.reverseOrder()).toList()) {
        forceDirectory(path);
      }
    }
  }

  private void removeStaging(Path directory) {
    if (!directory.getParent().equals(root.resolve("staging"))) {
      throw new IllegalArgumentException("Cleanup is limited to this store's staging directory");
    }
    try (Stream<Path> walk = Files.walk(directory)) {
      for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
        Files.deleteIfExists(path);
      }
    } catch (IOException ignored) {
      // Abandoned staging directories remain eligible for explicit retention cleanup.
    }
  }
}
