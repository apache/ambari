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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;

import org.apache.ambari.infra.job.JobContextRepository;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.repeat.RepeatStatus;

@RunWith(EasyMockRunner.class)
public class DocumentExporterTest extends EasyMockSupport {

  private static final long JOB_EXECUTION_ID = 1L;
  private static final long STEP_EXECUTION_ID = 1L;
  private static final Document DOCUMENT_2 = new Document(new HashMap<String, Object>() {{
    put("id", "2");
  }});
  private static final Document DOCUMENT_3 = new Document(new HashMap<String, Object>() {{
    put("id", "3");
  }});
  private static final StepContribution ANY_STEP_CONTRIBUTION = new StepContribution(new StepExecution("any", new JobExecution(1L)));

  private DocumentExporter documentExporter;
  @Mock
  private ItemStreamReader<Document> reader;
  @Mock
  private DocumentDestination documentDestination;
  @Mock
  private DocumentItemWriter documentItemWriter;
  @Mock
  private DocumentItemWriter documentItemWriter2;
  @Mock
  private DocumentItemWriter documentItemWriter3;
  @Mock
  private JobContextRepository jobContextRepository;

  private ChunkContext chunkContext;
  private static final Document DOCUMENT = new Document(new HashMap<String, Object>() {{ put("id", "1"); }});

  @Before
  public void setUp() {
    chunkContext = chunkContext(false);
    documentExporter = documentExporter(2);
  }

  private DocumentExporter documentExporter(int writeBlockSize) {
    return new DocumentExporter(reader, documentDestination, writeBlockSize, jobContextRepository);
  }

  private ChunkContext chunkContext(boolean terminate) {
    StepExecution stepExecution = new StepExecution("exportDoc", new JobExecution(JOB_EXECUTION_ID));
    stepExecution.setId(STEP_EXECUTION_ID);
    if (terminate)
      stepExecution.setTerminateOnly();
    return new ChunkContext(new StepContext(stepExecution));
  }

  @After
  public void tearDown() {
    verifyAll();
  }

  @Test
  public void testNothingToRead() throws Exception {
    reader.open(executionContext(chunkContext)); expectLastCall();
    expect(reader.read()).andReturn(null);
    reader.close(); expectLastCall();
    replayAll();

    documentExporter.execute(ANY_STEP_CONTRIBUTION, chunkContext);
  }

  private ExecutionContext executionContext(ChunkContext chunkContext) {
    return chunkContext.getStepContext().getStepExecution().getExecutionContext();
  }

  @Test
  public void testWriteLessDocumentsThanWriteBlockSize() throws Exception {
    reader.open(executionContext(chunkContext)); expectLastCall();
    expect(reader.read()).andReturn(DOCUMENT);
    expect(documentDestination.open(DOCUMENT)).andReturn(documentItemWriter);
    documentItemWriter.write(DOCUMENT); expectLastCall();
    expect(reader.read()).andReturn(null);
    reader.close(); expectLastCall();
    documentItemWriter.close(); expectLastCall();
    replayAll();

    assertThat(documentExporter.execute(ANY_STEP_CONTRIBUTION, chunkContext), is(RepeatStatus.FINISHED));
  }

  @Test
  public void testWriteMoreDocumentsThanWriteBlockSize() throws Exception {
    reader.open(executionContext(chunkContext)); expectLastCall();
    expect(reader.read()).andReturn(DOCUMENT);
    expect(documentDestination.open(DOCUMENT)).andReturn(documentItemWriter);
    documentItemWriter.write(DOCUMENT); expectLastCall();
    expect(reader.read()).andReturn(DOCUMENT_2);
    documentItemWriter.write(DOCUMENT_2); expectLastCall();
    expect(reader.read()).andReturn(DOCUMENT_3);
    documentItemWriter.close(); expectLastCall();
    jobContextRepository.updateExecutionContext(chunkContext.getStepContext().getStepExecution());
    expect(jobContextRepository.getStepExecution(JOB_EXECUTION_ID, STEP_EXECUTION_ID)).andReturn(chunkContext.getStepContext().getStepExecution());
    expect(documentDestination.open(DOCUMENT_3)).andReturn(documentItemWriter2);
    documentItemWriter2.write(DOCUMENT_3); expectLastCall();
    expect(reader.read()).andReturn(null);
    reader.update(executionContext(chunkContext));
    reader.close(); expectLastCall();
    documentItemWriter2.close(); expectLastCall();
    replayAll();

    assertThat(documentExporter.execute(ANY_STEP_CONTRIBUTION, chunkContext), is(RepeatStatus.FINISHED));
  }

  @Test(expected = IOException.class)
  public void testReadError() throws Exception {
    reader.open(executionContext(chunkContext)); expectLastCall();
    expect(reader.read()).andReturn(DOCUMENT);
    expect(documentDestination.open(DOCUMENT)).andReturn(documentItemWriter);
    documentItemWriter.write(DOCUMENT); expectLastCall();
    expect(reader.read()).andThrow(new IOException("TEST"));
    documentItemWriter.revert(); expectLastCall();
    reader.close(); expectLastCall();
    replayAll();

    documentExporter.execute(ANY_STEP_CONTRIBUTION, chunkContext);
  }

  @Test(expected = UncheckedIOException.class)
  public void testWriteError() throws Exception {
    reader.open(executionContext(chunkContext)); expectLastCall();
    expect(reader.read()).andReturn(DOCUMENT);
    expect(documentDestination.open(DOCUMENT)).andReturn(documentItemWriter);
    documentItemWriter.write(DOCUMENT); expectLastCall().andThrow(new UncheckedIOException(new IOException("TEST")));
    documentItemWriter.revert(); expectLastCall();
    reader.close(); expectLastCall();
    replayAll();

    documentExporter.execute(ANY_STEP_CONTRIBUTION, chunkContext);
  }

  @Test
  public void testStopAndRestartExportsAllDocuments() throws Exception {
    ChunkContext stoppingChunkContext = chunkContext(true);
    DocumentExporter documentExporter = documentExporter(1);

    reader.open(executionContext(chunkContext)); expectLastCall();
    expect(reader.read()).andReturn(DOCUMENT);

    expect(documentDestination.open(DOCUMENT)).andReturn(documentItemWriter);
    documentItemWriter.write(DOCUMENT); expectLastCall();
    expect(reader.read()).andReturn(DOCUMENT_2);
    expect(jobContextRepository.getStepExecution(JOB_EXECUTION_ID, STEP_EXECUTION_ID)).andReturn(chunkContext.getStepContext().getStepExecution());
    documentItemWriter.close(); expectLastCall();
    reader.update(executionContext(this.chunkContext));
    jobContextRepository.updateExecutionContext(this.chunkContext.getStepContext().getStepExecution());

    expect(documentDestination.open(DOCUMENT_2)).andReturn(documentItemWriter2);
    documentItemWriter2.write(DOCUMENT_2); expectLastCall();
    expect(reader.read()).andReturn(DOCUMENT_3);
    expect(jobContextRepository.getStepExecution(JOB_EXECUTION_ID, STEP_EXECUTION_ID)).andReturn(stoppingChunkContext.getStepContext().getStepExecution());
    documentItemWriter2.revert(); expectLastCall();
    reader.close(); expectLastCall();

    reader.open(executionContext(chunkContext));
    expect(reader.read()).andReturn(DOCUMENT_3);
    expect(documentDestination.open(DOCUMENT_3)).andReturn(documentItemWriter3);
    documentItemWriter3.write(DOCUMENT_3); expectLastCall();
    documentItemWriter3.close(); expectLastCall();

    expect(reader.read()).andReturn(null);
    reader.close(); expectLastCall();
    replayAll();

    RepeatStatus repeatStatus = documentExporter.execute(ANY_STEP_CONTRIBUTION, this.chunkContext);
    assertThat(repeatStatus, is(RepeatStatus.CONTINUABLE));
    repeatStatus = documentExporter.execute(ANY_STEP_CONTRIBUTION, this.chunkContext);
    assertThat(repeatStatus, is(RepeatStatus.FINISHED));
  }
}