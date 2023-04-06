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

import org.apache.ambari.infra.job.CloseableIterator;
import org.apache.ambari.infra.job.ObjectSource;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.util.ClassUtils;

public class DocumentItemReader extends AbstractItemStreamItemReader<Document> implements CompletionPolicy {

  public final static String POSITION = "last-read";

  private final ObjectSource<Document> documentSource;
  private final int readBlockSize;

  private CloseableIterator<Document> documentIterator = null;
  private int count = 0;
  private boolean eof = false;
  private Document current = null;
  private Document previous = null;

  public DocumentItemReader(ObjectSource<Document> documentSource, int readBlockSize) {
    this.documentSource = documentSource;
    this.readBlockSize = readBlockSize;
    setName(ClassUtils.getShortName(DocumentItemReader.class));
  }

  @Override
  public Document read() throws Exception {
    if (documentIterator == null)
      openStream();
    Document next = getNext();
    if (next == null && count > readBlockSize) {
      openStream();
      next = getNext();
    }
    eof = next == null;
    if (eof && documentIterator != null)
      documentIterator.close();

    previous = current;
    current = next;
    return current;
  }

  private Document getNext() {
    ++count;
    return documentIterator.next();
  }

  private void openStream() {
    closeStream();
    documentIterator = documentSource.open(current, readBlockSize);
    count = 0;
  }

  private void closeStream() {
    if (documentIterator == null)
      return;
    try {
      documentIterator.close();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    documentIterator = null;
  }

  @Override
  public void open(ExecutionContext executionContext) {
    super.open(executionContext);
    current = null;
    previous = null;
    eof = false;
    documentIterator = null;
    if (!executionContext.containsKey(POSITION))
      return;

    current = (Document) executionContext.get(POSITION);
  }

  @Override
  public void update(ExecutionContext executionContext) throws ItemStreamException {
    super.update(executionContext);
    if (previous != null)
      executionContext.put(POSITION, previous);
  }

  @Override
  public void close() {
    closeStream();
  }

  @Override
  public boolean isComplete(RepeatContext context, RepeatStatus result) {
    return eof;
  }

  @Override
  public boolean isComplete(RepeatContext context) {
    return eof;
  }

  @Override
  public RepeatContext start(RepeatContext parent) {
    return new RepeatContextSupport(parent);
  }

  @Override
  public void update(RepeatContext context) {
    if (eof)
      context.setCompleteOnly();
  }
}
