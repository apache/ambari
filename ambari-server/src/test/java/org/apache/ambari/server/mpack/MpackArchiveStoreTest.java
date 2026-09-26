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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MpackArchiveStoreTest {
  @Rule
  public TemporaryFolder temporary = new TemporaryFolder();
  private MpackArchiveStore store;

  record Member(String path, String content, String link) {
  }

  @Before
  public void setUp() throws Exception {
    store = new MpackArchiveStore(temporary.newFolder("store").toPath(),
        new MpackArchiveStore.Limits(1024 * 1024, 1024 * 1024, 100));
  }

  static byte[] archive(Member... members) throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (TarArchiveOutputStream tar = new TarArchiveOutputStream(new GzipCompressorOutputStream(bytes))) {
      for (Member member : members) {
        byte[] content = member.content().getBytes(StandardCharsets.UTF_8);
        TarArchiveEntry entry = member.link() == null ? new TarArchiveEntry(member.path(), true)
            : new TarArchiveEntry(member.path(), TarConstants.LF_SYMLINK);
        entry.setMode(0644);
        entry.setModTime(0);
        if (member.link() == null) {
          entry.setSize(content.length);
        } else {
          entry.setLinkName(member.link());
        }
        tar.putArchiveEntry(entry);
        if (member.link() == null) {
          tar.write(content);
        }
        tar.closeArchiveEntry();
      }
    }
    return bytes.toByteArray();
  }

  @Test
  public void storesExactBytesAndSupportsIdenticalUploadRetry() throws Exception {
    byte[] bytes = archive(new Member("pack/mpack.json", "{}", null),
        new Member("pack/services/hello.py", "print('hello')", null));
    String digest = DigestUtils.sha256Hex(bytes);
    MpackArchiveStore.StoredArchive first = store.accept(new ByteArrayInputStream(bytes), digest);
    MpackArchiveStore.StoredArchive retry = store.accept(new ByteArrayInputStream(bytes), digest);
    assertEquals(first, retry);
    assertEquals(digest, retry.digest());
    assertEquals("{}", Files.readString(store.packageRoot(first, "mpack.json").resolve("mpack.json")));
    try (Stream<Path> staging = Files.list(store.root().resolve("staging"))) {
      assertEquals(0, staging.count());
    }
  }

  @Test
  public void rejectsTraversalAndAbsoluteMembersWithoutWritingOutsideStorage() throws Exception {
    for (String path : List.of("../escape", "/escape", "pack/../../escape", "pack\\escape")) {
      byte[] bytes = archive(new Member(path, "unsafe", null));
      assertThrows(MpackException.class, () -> store.accept(new ByteArrayInputStream(bytes), null));
    }
    assertFalse(Files.exists(temporary.getRoot().toPath().resolve("escape")));
  }

  @Test
  public void rejectsDuplicateMembers() throws Exception {
    byte[] bytes = archive(new Member("mpack.json", "{}", null),
        new Member("mpack.json", "{\"changed\":true}", null));
    assertEquals(MpackException.Code.INVALID_ARCHIVE,
        assertThrows(MpackException.class,
            () -> store.accept(new ByteArrayInputStream(bytes), null)).getCode());
  }

  @Test
  public void rejectsSpecialTypesEvenWhenLibraryClassifiesTheirNamesAsFilesOrDirectories() throws Exception {
    for (byte type : new byte[]{TarConstants.LF_FIFO, TarConstants.LF_CHR, TarConstants.LF_BLK, (byte) 'Z'}) {
      for (String name : List.of("special", "special/")) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(new GzipCompressorOutputStream(bytes))) {
          tar.putArchiveEntry(new TarArchiveEntry(name, type));
          tar.closeArchiveEntry();
        }
        assertEquals(MpackException.Code.INVALID_ARCHIVE, assertThrows(MpackException.class,
            () -> store.accept(new ByteArrayInputStream(bytes.toByteArray()), null)).getCode());
      }
    }
  }

  @Test
  public void verifiesDigestBeforePublishingArchive() throws Exception {
    byte[] bytes = archive(new Member("mpack.json", "{}", null));
    assertEquals(MpackException.Code.DIGEST_MISMATCH,
        assertThrows(MpackException.class,
            () -> store.accept(new ByteArrayInputStream(bytes), "0".repeat(64))).getCode());
    assertFalse(Files.exists(store.root().resolve("archives").resolve(DigestUtils.sha256Hex(bytes))));
  }

  @Test
  public void boundsCompressedExpandedAndEntrySizes() throws Exception {
    byte[] bytes = archive(new Member("mpack.json", "{}", null),
        new Member("large", "x".repeat(1024), null));
    for (MpackArchiveStore.Limits limits : List.of(
        new MpackArchiveStore.Limits(8, 10000, 100),
        new MpackArchiveStore.Limits(10000, 64, 100),
        new MpackArchiveStore.Limits(10000, 10000, 1))) {
      MpackArchiveStore bounded = new MpackArchiveStore(temporary.newFolder().toPath(), limits);
      assertEquals(MpackException.Code.UPLOAD_LIMIT,
          assertThrows(MpackException.class,
              () -> bounded.accept(new ByteArrayInputStream(bytes), null)).getCode());
    }
  }

  @Test
  public void permitsContainedLinksAndRejectsEscapingOrCyclicLinks() throws Exception {
    byte[] valid = archive(new Member("pack/mpack.json", "{}", null),
        new Member("pack/lib/common.py", "value = 1", null),
        new Member("pack/scripts/common.py", "", "../lib/common.py"));
    MpackArchiveStore.StoredArchive result = store.accept(new ByteArrayInputStream(valid), null);
    assertEquals("value = 1", Files.readString(result.content().resolve("pack/scripts/common.py")));
    byte[] escaping = archive(new Member("pack/mpack.json", "{}", null),
        new Member("pack/escape", "", "../../outside"));
    assertThrows(MpackException.class, () -> store.accept(new ByteArrayInputStream(escaping), null));
    byte[] cyclic = archive(new Member("mpack.json", "{}", null),
        new Member("a", "", "b"), new Member("b", "", "a"));
    assertThrows(MpackException.class, () -> store.accept(new ByteArrayInputStream(cyclic), null));
  }

  @Test
  public void rejectsWritesBeneathLinkEntriesRegardlessOfMemberOrder() throws Exception {
    byte[] bytes = archive(new Member("mpack.json", "{}", null),
        new Member("real/file", "data", null), new Member("alias", "", "real"),
        new Member("alias/child", "", "file"));
    assertThrows(MpackException.class, () -> store.accept(new ByteArrayInputStream(bytes), null));
    assertFalse(Files.exists(store.root().resolve("archives").resolve(DigestUtils.sha256Hex(bytes))));
  }

  @Test
  public void doesNotSearchArbitrarilyForManifest() throws Exception {
    byte[] bytes = archive(new Member("a/b/mpack.json", "{}", null));
    MpackArchiveStore.StoredArchive result = store.accept(new ByteArrayInputStream(bytes), null);
    assertThrows(MpackException.class, () -> store.packageRoot(result, "mpack.json"));
  }

  @Test
  public void detectsModifiedStoredContentOnRetry() throws Exception {
    byte[] bytes = archive(new Member("mpack.json", "{}", null));
    MpackArchiveStore.StoredArchive result = store.accept(new ByteArrayInputStream(bytes), null);
    Files.writeString(result.content().resolve("mpack.json"), "{\"changed\":true}");
    assertEquals(MpackException.Code.DIGEST_MISMATCH,
        assertThrows(MpackException.class,
            () -> store.accept(new ByteArrayInputStream(bytes), null)).getCode());
  }

  @Test
  public void rejectsCorruptArchivesAndRetainsNoPublishedCandidate() throws Exception {
    assertThrows(MpackException.class,
        () -> store.accept(new ByteArrayInputStream("not gzip".getBytes(StandardCharsets.UTF_8)), null));
    try (Stream<Path> archives = Files.list(store.root().resolve("archives"))) {
      assertTrue(archives.findAny().isEmpty());
    }
  }
}
