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

package org.apache.ambari.server.controller.internal;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ClientConfigArchiveTest {

  private static final String COMPONENT_NAME = "HDFS_CLIENT";

  @TempDir
  private Path temporaryDirectory;

  @Test
  public void testComponentArchiveRoundTrip() throws Exception {
    String fileContents = "fs.defaultFS=hdfs://namenode:8020\n";
    createComponentArchive("core-site.xml", fileContents);

    createTarUtils("cluster").tarConfigFiles();

    Path mergedArchive = temporaryDirectory.resolve("cluster-configs" + Configuration.DEF_ARCHIVE_EXTENSION);
    try (InputStream fileInput = Files.newInputStream(mergedArchive);
         BufferedInputStream bufferedInput = new BufferedInputStream(fileInput);
         GzipCompressorInputStream gzipInput = new GzipCompressorInputStream(bufferedInput);
         TarArchiveInputStream tarInput = new TarArchiveInputStream(gzipInput)) {
      TarArchiveEntry entry = tarInput.getNextTarEntry();
      assertEquals(COMPONENT_NAME + "/core-site.xml", entry.getName());
      assertEquals(fileContents, new String(tarInput.readAllBytes(), UTF_8));
      assertNull(tarInput.getNextTarEntry());
    }
  }

  @Test
  public void testTruncatedComponentArchiveIsRejected() throws Exception {
    Files.write(componentArchivePath(), new byte[] {(byte) 0x1f, (byte) 0x8b, 0x08});

    assertThrows(SystemException.class, () -> createTarUtils("invalid").tarConfigFiles());
  }

  private ClientConfigResourceProvider.TarUtils createTarUtils(String outputName) {
    ServiceComponentHostResponse response = mock(ServiceComponentHostResponse.class);
    when(response.getComponentName()).thenReturn(COMPONENT_NAME);
    return new ClientConfigResourceProvider.TarUtils(temporaryDirectory.toString(), outputName,
        Collections.singletonList(response));
  }

  private void createComponentArchive(String entryName, String contents) throws IOException {
    byte[] contentBytes = contents.getBytes(UTF_8);
    try (OutputStream fileOutput = Files.newOutputStream(componentArchivePath());
         BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutput);
         GzipCompressorOutputStream gzipOutput = new GzipCompressorOutputStream(bufferedOutput);
         TarArchiveOutputStream tarOutput = new TarArchiveOutputStream(gzipOutput)) {
      TarArchiveEntry entry = new TarArchiveEntry(entryName);
      entry.setSize(contentBytes.length);
      tarOutput.putArchiveEntry(entry);
      tarOutput.write(contentBytes);
      tarOutput.closeArchiveEntry();
    }
  }

  private Path componentArchivePath() {
    return temporaryDirectory.resolve(COMPONENT_NAME + "-configs" + Configuration.DEF_ARCHIVE_EXTENSION);
  }
}
