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

import javax.inject.Inject;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class DummyJobConfiguration {
  @Inject
  private StepBuilderFactory steps;

  @Inject
  private JobBuilderFactory jobs;
  
  @Bean(name = "dummyStep")
  protected Step dummyStep(ItemReader<DummyObject> reader,
                       ItemProcessor<DummyObject, String> processor,
                       @Qualifier("dummyItemWriter") ItemWriter<String> writer) {
    return steps.get("dummyStep").listener(new DummyStepListener()).<DummyObject, String> chunk(2)
      .reader(reader).processor(processor).writer(writer).build();
  }

  @Bean(name = "dummyJob")
  public Job job(@Qualifier("dummyStep") Step dummyStep) {
    return jobs.get("dummyJob").listener(new DummyJobListener()).start(dummyStep).build();
  }

  @Bean
  public ItemReader<DummyObject> dummyItemReader() {
    FlatFileItemReader<DummyObject> csvFileReader = new FlatFileItemReader<>();
    csvFileReader.setResource(new ClassPathResource("dummy/dummy.txt"));
    csvFileReader.setLinesToSkip(1);
    LineMapper<DummyObject> lineMapper = dummyLineMapper();
    csvFileReader.setLineMapper(lineMapper);
    return csvFileReader;
  }

  @Bean
  public ItemProcessor<DummyObject, String> dummyItemProcessor() {
    return new DummyItemProcessor();
  }

  @Bean(name = "dummyItemWriter")
  public ItemWriter<String> dummyItemWriter() {
    return new DummyItemWriter();
  }

  private LineMapper<DummyObject> dummyLineMapper() {
    DefaultLineMapper<DummyObject> lineMapper = new DefaultLineMapper<>();

    LineTokenizer dummyTokenizer = dummyTokenizer();
    lineMapper.setLineTokenizer(dummyTokenizer);

    FieldSetMapper<DummyObject> dummyFieldSetMapper = dummyFieldSetMapper();
    lineMapper.setFieldSetMapper(dummyFieldSetMapper);

    return lineMapper;
  }

  private FieldSetMapper<DummyObject> dummyFieldSetMapper() {
    BeanWrapperFieldSetMapper<DummyObject> studentInformationMapper = new BeanWrapperFieldSetMapper<>();
    studentInformationMapper.setTargetType(DummyObject.class);
    return studentInformationMapper;
  }

  private LineTokenizer dummyTokenizer() {
    DelimitedLineTokenizer studentLineTokenizer = new DelimitedLineTokenizer();
    studentLineTokenizer.setDelimiter(",");
    studentLineTokenizer.setNames(new String[]{"f1", "f2"});
    return studentLineTokenizer;
  }
}
