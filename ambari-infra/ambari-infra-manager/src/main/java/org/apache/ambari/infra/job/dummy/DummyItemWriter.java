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
package org.apache.ambari.infra.job.dummy;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.apache.ambari.infra.conf.InfraManagerDataConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemWriter;

public class DummyItemWriter implements ItemWriter<String> {

  private static final Logger logger = LogManager.getLogger(DummyItemWriter.class);

  private StepExecution stepExecution;

  @Inject
  private InfraManagerDataConfig infraManagerDataConfig;

  @Override
  public void write(List<? extends String> values) throws Exception {
    logger.info("DummyItem writer called (values: {})... wait 1 seconds", values.toString());
    Thread.sleep(1000);
    String outputDirectoryLocation = String.format("%s%s%s%s", infraManagerDataConfig.getDataFolder(), File.separator, "dummyOutput-", new Date().getTime());
    Path pathToDirectory = Paths.get(outputDirectoryLocation);
    Path pathToFile = Paths.get(String.format("%s%s%s", outputDirectoryLocation, File.separator, "dummyOutput.txt"));
    Files.createDirectories(pathToDirectory);
    logger.info("Write location to step execution context...");
    stepExecution.getExecutionContext().put("stepOutputLocation", pathToFile.toAbsolutePath().toString());
    logger.info("Write location to job execution context...");
    stepExecution.getJobExecution().getExecutionContext().put("jobOutputLocation", pathToFile.toAbsolutePath().toString());
    logger.info("Write to file: {}", pathToFile.toAbsolutePath());
    Files.write(pathToFile, values.toString().getBytes());
  }

  @BeforeStep
  public void saveStepExecution(StepExecution stepExecution) {
    this.stepExecution = stepExecution;
  }
}
