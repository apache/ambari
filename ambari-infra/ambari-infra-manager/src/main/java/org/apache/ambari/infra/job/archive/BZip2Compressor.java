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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.IOUtils;

public class BZip2Compressor extends AbstractFileAction {
  @Override
  protected File onPerform(File inputFile) {
    File bz2File = new File(inputFile.getParent(), inputFile.getName() + ".bz2");
    try (BZip2CompressorOutputStream bZip2CompressorOutputStream = new BZip2CompressorOutputStream(new FileOutputStream(bz2File))) {
      try (FileInputStream fileInputStream = new FileInputStream(inputFile)) {
        IOUtils.copy(fileInputStream, bZip2CompressorOutputStream);
      }
    }
    catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return bz2File;
  }
}
