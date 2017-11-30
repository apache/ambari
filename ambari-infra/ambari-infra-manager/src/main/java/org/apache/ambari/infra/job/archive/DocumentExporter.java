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

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.repeat.RepeatStatus;

public class DocumentExporter implements Tasklet, StepExecutionListener {

  private boolean complete = false;
  private final ItemStreamReader<Document> documentReader;
  private final DocumentDestination documentDestination;
  private final int writeBlockSize;

  public DocumentExporter(ItemStreamReader<Document> documentReader, DocumentDestination documentDestination, int writeBlockSize) {
    this.documentReader = documentReader;
    this.documentDestination = documentDestination;
    this.writeBlockSize = writeBlockSize;
  }

  @Override
  public void beforeStep(StepExecution stepExecution) {

  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    if (complete) {
      return ExitStatus.COMPLETED;
    }
		else {
      return ExitStatus.FAILED;
    }
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
    ExecutionContext executionContext = chunkContext.getStepContext().getStepExecution().getExecutionContext();
    documentReader.open(executionContext);

    DocumentItemWriter writer = null;
    int writtenCount = 0;
    try {
      Document document;
      while ((document = documentReader.read()) != null) {
        if (writer != null && writtenCount >= writeBlockSize) {
          writer.close();
          writer = null;
          writtenCount = 0;
          documentReader.update(executionContext);
        }

        if (writer == null)
          writer = documentDestination.open(document);

        writer.write(document);
        ++writtenCount;
      }
    }
    catch (Exception e) {
      if (writer != null) {
        writer.revert();
        writer = null;
      }
      throw e;
    }
    finally {
      if (writer != null)
        writer.close();
      documentReader.close();
    }

    complete = true;
    return RepeatStatus.FINISHED;
  }
}
