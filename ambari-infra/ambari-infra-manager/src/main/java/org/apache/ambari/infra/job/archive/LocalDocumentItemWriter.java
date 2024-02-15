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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

public class LocalDocumentItemWriter implements DocumentItemWriter {
  private static final Logger logger = LogManager.getLogger(LocalDocumentItemWriter.class);

  private static final ObjectMapper json = new ObjectMapper();
  private static final String ENCODING = "UTF-8";

  private final File outFile;
  private final BufferedWriter bufferedWriter;
  private final ItemWriterListener itemWriterListener;
  private Document firstDocument = null;
  private Document lastDocument = null;

  public LocalDocumentItemWriter(File outFile, ItemWriterListener itemWriterListener) {
    this.itemWriterListener = itemWriterListener;
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

      if (firstDocument == null)
        firstDocument = document;

      lastDocument = document;
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void revert() {
    IOUtils.closeQuietly(bufferedWriter);
    if (!outFile.delete())
      logger.warn("File {} was not deleted. Exists: {}", outFile.getAbsolutePath(), outFile.exists());
  }

  @Override
  public void close() {
    try {
      bufferedWriter.close();
      if (itemWriterListener != null)
        itemWriterListener.onCompleted(new WriteCompletedEvent(outFile, firstDocument, lastDocument));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
