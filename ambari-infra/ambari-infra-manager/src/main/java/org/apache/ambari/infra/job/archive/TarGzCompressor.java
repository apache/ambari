/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.infra.job.archive;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;

import java.io.*;

public class TarGzCompressor extends AbstractFileAction {
  @Override
  public File onPerform(File inputFile) {
    File tarGzFile = new File(inputFile.getParent(), inputFile.getName() + ".tar.gz");
    try (TarArchiveOutputStream tarArchiveOutputStream = new TarArchiveOutputStream(
            new GzipCompressorOutputStream(new FileOutputStream(tarGzFile)))) {
      TarArchiveEntry archiveEntry = new TarArchiveEntry(inputFile.getName());
      archiveEntry.setSize(inputFile.length());
      tarArchiveOutputStream.putArchiveEntry(archiveEntry);

      try (FileInputStream fileInputStream = new FileInputStream(inputFile)) {
        IOUtils.copy(fileInputStream, tarArchiveOutputStream);
      }

      tarArchiveOutputStream.closeArchiveEntry();
    }
    catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }

    return tarGzFile;
  }
}
