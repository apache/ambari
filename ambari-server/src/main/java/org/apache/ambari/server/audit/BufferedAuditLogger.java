/**
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.audit;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * This is a decorator that adds buffering and running on separate thread (instead of the tread of the caller)
 * to an existing audit logger implementation.
 * It uses a bounded queue for holding audit events before they are logged.
 */
public class BufferedAuditLogger implements AuditLogger {

  private static final Logger LOG = LoggerFactory.getLogger(BufferedAuditLogger.class);

  private final ThreadPoolExecutor executor;

  private final AuditLogger auditLogger;

  /**
   * Constructor.
   * @param auditLogger the audit logger to extend
   */
  @Inject
  public BufferedAuditLogger(AuditLogger auditLogger) {

    this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS
    , new ArrayBlockingQueue<Runnable>(10000)
    , new ThreadPoolExecutor.CallerRunsPolicy());

    this.auditLogger = auditLogger;
  }

  /**
   * Logs audit log events
   *
   * @param event
   */
  @Override
  public void log(final AuditEvent event) {
    this.executor.execute(
      new Runnable() {
        @Override
        public void run() {
          try {
            auditLogger.log(event);
          }
          catch (Exception e) {
            LOG.error(e.getMessage());
          }
        }
      }

    );
  }
}
