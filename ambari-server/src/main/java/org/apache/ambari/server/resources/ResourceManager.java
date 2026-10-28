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
package org.apache.ambari.server.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Resource manager.
 */
@Singleton
public class ResourceManager {
  private static final Logger LOG = LoggerFactory.getLogger(ResourceManager.class);
  public static final String ARCHIVE_DIGEST_MANIFEST = ".resource-archive-digests.json";
  private static final Type ARCHIVE_DIGEST_MAP_TYPE =
      new TypeToken<TreeMap<String, String>>() { }.getType();

  @Inject Configuration configs;
  @Inject Gson gson;
  @Inject com.google.inject.Provider<org.apache.ambari.server.mpack.MpackSnapshots> mpackSnapshots;
  private FileTime archiveDigestManifestModifiedTime;
  private Object archiveDigestManifestFileKey;
  private long archiveDigestManifestSize = -1;
  private SortedMap<String, String> cachedResourceArchiveDigests = new TreeMap<>();
  /**
  * Returns resource file.
  * @param resourcePath relational path to file
  * @return resource file
  */
  public File getResource(String resourcePath) {
    if (resourcePath.startsWith("mpacks/")) {
      return getMpackResource(resourcePath);
    }
    String resDir = configs.getConfigsMap().get(Configuration.RESOURCES_DIR.getKey());
    String resourcePathIndep = resourcePath.replace("/", File.separator);
    File resourceFile = new File(resDir + File.separator + resourcePathIndep);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Resource requested from ResourceManager, resourceDir={}, resourcePath={}, fileExists={}", resDir, resourcePathIndep, resourceFile.exists());
    }
    return resourceFile;
  }

  private File getMpackResource(String resourcePath) {
    org.apache.ambari.server.mpack.MpackManifest.requirePath(resourcePath);
    String[] parts = resourcePath.split("/", 3);
    if (parts.length != 3) {
      throw new org.apache.ambari.server.mpack.MpackException(
          org.apache.ambari.server.mpack.MpackException.Code.NOT_FOUND, "Unknown snapshot resource");
    }
    org.apache.ambari.server.mpack.MpackSnapshots.Snapshot snapshot = mpackSnapshots.get().load(parts[1]);
    Path relative = Paths.get(parts[2]);
    String fileName = relative.getFileName().toString();
    Path parent = relative.getParent();
    if (parent == null || !(fileName.equals("archive.zip") || fileName.equals(".hash"))
        || !snapshot.archiveDigests().containsKey("mpacks/" + snapshot.id() + "/" + parent.toString().replace(File.separator, "/"))) {
      throw new org.apache.ambari.server.mpack.MpackException(
          org.apache.ambari.server.mpack.MpackException.Code.NOT_FOUND, "Unknown snapshot archive");
    }
    Path root = mpackSnapshots.get().resourceRoot(snapshot.id());
    Path resource = root.resolve(relative);
    try {
      if (!Files.isRegularFile(resource, LinkOption.NOFOLLOW_LINKS) || !resource.toRealPath().startsWith(root.toRealPath())) {
        throw new org.apache.ambari.server.mpack.MpackException(
            org.apache.ambari.server.mpack.MpackException.Code.NOT_FOUND, "Snapshot archive is unavailable");
      }
      return resource.toFile();
    } catch (IOException e) {
      throw new org.apache.ambari.server.mpack.MpackException(
          org.apache.ambari.server.mpack.MpackException.Code.STORAGE_FAILURE, "Snapshot archive cannot be read");
    }
  }

  /**
   * Returns SHA-256 digests for the resource archives exposed to agents.
   * Keys are resource-directory paths relative to {@code resources.dir}.
   */
  public synchronized SortedMap<String, String> getResourceArchiveDigests() {
    Path resourceRoot = Paths.get(configs.getResourceDirPath()).toAbsolutePath().normalize();
    Path manifest = resourceRoot.resolve(ARCHIVE_DIGEST_MANIFEST);
    if (!Files.isRegularFile(manifest, LinkOption.NOFOLLOW_LINKS)) {
      clearArchiveDigestManifestCache();
      return new TreeMap<>();
    }

    try {
      BasicFileAttributes attributes = Files.readAttributes(
          manifest, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      if (attributes.lastModifiedTime().equals(archiveDigestManifestModifiedTime)
          && attributes.size() == archiveDigestManifestSize
          && java.util.Objects.equals(attributes.fileKey(), archiveDigestManifestFileKey)) {
        return new TreeMap<>(cachedResourceArchiveDigests);
      }

      SortedMap<String, String> digests;
      try (Reader reader = Files.newBufferedReader(manifest, StandardCharsets.US_ASCII)) {
        digests = gson.fromJson(reader, ARCHIVE_DIGEST_MAP_TYPE);
      }
      if (digests == null) {
        throw new IllegalStateException("Resource archive digest manifest is empty");
      }
      for (Map.Entry<String, String> entry : digests.entrySet()) {
        String key = entry.getKey();
        Path resourceDirectory = Paths.get(key).normalize();
        String canonicalKey = resourceDirectory.toString().replace(File.separatorChar, '/');
        if (key.isEmpty() || key.contains("\\") || !key.equals(canonicalKey)
            || resourceDirectory.isAbsolute() || resourceDirectory.startsWith("..")
            || !isSha256(entry.getValue())) {
          throw new IllegalStateException(
              "Resource archive digest manifest contains an invalid entry: " + key);
        }
        Path archive = resourceRoot.resolve(resourceDirectory).resolve("archive.zip").normalize();
        if (!archive.startsWith(resourceRoot)
            || !Files.isRegularFile(archive, LinkOption.NOFOLLOW_LINKS)) {
          throw new IllegalStateException(
              "Resource archive digest manifest references a missing archive: " + key);
        }
        try (InputStream stream = Files.newInputStream(archive)) {
          if (!DigestUtils.sha256Hex(stream).equalsIgnoreCase(entry.getValue())) {
            throw new IllegalStateException(
                "Resource archive digest manifest is stale for: " + key);
          }
        }
      }
      cachedResourceArchiveDigests = new TreeMap<>(digests);
      archiveDigestManifestModifiedTime = attributes.lastModifiedTime();
      archiveDigestManifestSize = attributes.size();
      archiveDigestManifestFileKey = attributes.fileKey();
      return new TreeMap<>(cachedResourceArchiveDigests);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read resource archive digests", e);
    }
  }

  private void clearArchiveDigestManifestCache() {
    archiveDigestManifestModifiedTime = null;
    archiveDigestManifestFileKey = null;
    archiveDigestManifestSize = -1;
    cachedResourceArchiveDigests = new TreeMap<>();
  }

  private static boolean isSha256(String value) {
    return value != null && value.matches("[0-9a-fA-F]{64}");
  }
}
