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

import java.util.Optional;

import org.springframework.batch.core.JobParameters;

public abstract class JobProperties<TParameters extends Validatable> {

  private SchedulingProperties scheduling;
  private boolean enabled;

  public SchedulingProperties getScheduling() {
    return scheduling;
  }

  public Optional<SchedulingProperties> scheduling() {
    if (scheduling != null && scheduling.isEnabled())
      return Optional.of(scheduling);
    return Optional.empty();
  }

  public void setScheduling(SchedulingProperties scheduling) {
    this.scheduling = scheduling;
  }

  public abstract TParameters merge(JobParameters jobParameters);

  public void validate(String jobName) {
    try {
      merge(new JobParameters()).validate();
    }
    catch (Exception ex) {
      throw new JobConfigurationException(String.format("Configuration of job %s is invalid: %s!", jobName, ex.getMessage()), ex);
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
