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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;

import java.io.*;

public class LocalDocumentItemWriter implements DocumentItemWriter {
  private static final ObjectMapper json = new ObjectMapper();
  private static final String ENCODING = "UTF-8";

  private final File outFile;
  private final BufferedWriter bufferedWriter;
  private final FileAction fileAction;

  public LocalDocumentItemWriter(File outFile, FileAction fileAction) {
    this.fileAction = fileAction;
    this.outFile = outFile;
    try {
      this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), ENCODING));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    } catch (FileNotFoundException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void write(Document document) {
    try {
      bufferedWriter.write(json.writeValueAsString(document));
      bufferedWriter.newLine();
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void revert() {
    IOUtils.closeQuietly(bufferedWriter);
    outFile.delete();
  }

  @Override
  public void close() {
    try {
      bufferedWriter.close();
      fileAction.perform(outFile);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
