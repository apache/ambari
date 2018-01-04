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
package org.apache.ambari.infra.job;

import org.apache.htrace.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.batch.core.JobParameters;

import java.io.IOException;
import java.io.UncheckedIOException;

public abstract class JobProperties<T extends JobProperties<T>> {
  private final Class<T> clazz;

  protected JobProperties(Class<T> clazz) {
    this.clazz = clazz;
  }

  public T deepCopy() {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      String json = objectMapper.writeValueAsString(this);
      return objectMapper.readValue(json, clazz);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public abstract void apply(JobParameters jobParameters);

  public abstract void validate();
}
