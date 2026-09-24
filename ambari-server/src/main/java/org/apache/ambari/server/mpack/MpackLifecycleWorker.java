/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.mpack;

import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.AmbariService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;

/** Polls durable accepted work, so lost queue notifications cannot abandon an operation. */
@AmbariService
public class MpackLifecycleWorker extends AbstractScheduledService {
  private static final Logger LOG = LoggerFactory.getLogger(MpackLifecycleWorker.class);
  @Inject private MpackLifecycleService lifecycle;

  @Override
  protected void startUp() {
    lifecycle.recoverOnStartup();
  }

  @Override
  protected void runOneIteration() {
    try {
      lifecycle.advance();
    } catch (RuntimeException e) {
      // Keep the worker alive; durable work remains unresolved for the next iteration.
      LOG.error("Management pack reconciliation could not complete; durable state is retained", e);
    }
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedDelaySchedule(0, 1, TimeUnit.SECONDS);
  }
}
