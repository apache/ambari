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

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

@RunWith(EasyMockRunner.class)
public class DocumentExporterTest extends EasyMockSupport {

  private DocumentExporter documentExporter;
  @Mock
  private ItemStreamReader<Document> reader;
  @Mock
  private DocumentDestination documentDestination;
  @Mock
  private DocumentItemWriter documentItemWriter;
  @Mock
  private DocumentItemWriter documentItemWriter2;

  private ExecutionContext executionContext;
  private ChunkContext chunkContext;
  private static final Document DOCUMENT = new Document(new HashMap<String, String>() {{ put("id", "1"); }});

  @Before
  public void setUp() throws Exception {
    StepExecution stepExecution = new StepExecution("exportDoc", new JobExecution(1L));
    chunkContext = new ChunkContext(new StepContext(stepExecution));
    executionContext = stepExecution.getExecutionContext();
    documentExporter = new DocumentExporter(reader, documentDestination, 2);
  }

  @After
  public void tearDown() throws Exception {
    verifyAll();
  }

  @Test
  public void testNothingToRead() throws Exception {
    reader.open(executionContext); expectLastCall();
    expect(reader.read()).andReturn(null);
    reader.close(); expectLastCall();
    replayAll();

    documentExporter.execute(null, chunkContext);
  }

  @Test
  public void testWriteLessDocumentsThanWriteBlockSize() throws Exception {
    reader.open(executionContext); expectLastCall();
    expect(reader.read()).andReturn(DOCUMENT);
    expect(documentDestination.open(DOCUMENT)).andReturn(documentItemWriter);
    documentItemWriter.write(DOCUMENT); expectLastCall();
    expect(reader.read()).andReturn(null);
    reader.close(); expectLastCall();
    documentItemWriter.close(); expectLastCall();
    replayAll();

    documentExporter.execute(null, chunkContext);
  }

  @Test
  public void testWriteMoreDocumentsThanWriteBlockSize() throws Exception {
    Document document2 = new Document(new HashMap<String, String>() {{ put("id", "2"); }});
    Document document3 = new Document(new HashMap<String, String>() {{ put("id", "3"); }});

    reader.open(executionContext); expectLastCall();
    expect(reader.read()).andReturn(DOCUMENT);
    expect(documentDestination.open(DOCUMENT)).andReturn(documentItemWriter);
    documentItemWriter.write(DOCUMENT); expectLastCall();
    expect(reader.read()).andReturn(document2);
    documentItemWriter.write(document2); expectLastCall();
    expect(reader.read()).andReturn(document3);
    documentItemWriter.close(); expectLastCall();
    expect(documentDestination.open(document3)).andReturn(documentItemWriter2);
    documentItemWriter2.write(document3); expectLastCall();
    expect(reader.read()).andReturn(null);
    reader.update(executionContext);
    reader.close(); expectLastCall();
    documentItemWriter2.close(); expectLastCall();
    replayAll();

    documentExporter.execute(null, chunkContext);
  }

  @Test(expected = IOException.class)
  public void testReadError() throws Exception {
    reader.open(executionContext); expectLastCall();
    expect(reader.read()).andReturn(DOCUMENT);
    expect(documentDestination.open(DOCUMENT)).andReturn(documentItemWriter);
    documentItemWriter.write(DOCUMENT); expectLastCall();
    expect(reader.read()).andThrow(new IOException("TEST"));
    documentItemWriter.revert(); expectLastCall();
    reader.close(); expectLastCall();
    replayAll();

    documentExporter.execute(null, chunkContext);
  }

  @Test(expected = UncheckedIOException.class)
  public void testWriteError() throws Exception {
    reader.open(executionContext); expectLastCall();
    expect(reader.read()).andReturn(DOCUMENT);
    expect(documentDestination.open(DOCUMENT)).andReturn(documentItemWriter);
    documentItemWriter.write(DOCUMENT); expectLastCall().andThrow(new UncheckedIOException(new IOException("TEST")));
    documentItemWriter.revert(); expectLastCall();
    reader.close(); expectLastCall();
    replayAll();

    documentExporter.execute(null, chunkContext);
  }
}