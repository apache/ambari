/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.audit;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.ambari.server.audit.event.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * This is a decorator that adds buffering and running on separate thread (instead of the tread of the caller)
 * to an existing audit logger implementation.
 * It uses a bounded queue for holding audit events before they are logged.
 */
@Singleton
public class BufferedAuditLogger implements AuditLogger {

  private static final Logger LOG = LoggerFactory.getLogger(BufferedAuditLogger.class);

  private final int bufferCapacity;

  private final double capacityWaterMark;

  private final AuditLogger auditLogger;

  private final ExecutorService pool;

  public final static String InnerLogger = "BufferedAuditLogger";
  public final static String Capacity = "BufferedAuditLogger.capacity";


  /**
   * The queue that holds the audit events to be logged in case there are
   * peeks when the producers logs audit events at a higher pace than
   * this audit logger can consume.
   */
  protected final BlockingQueue<AuditEvent> auditEventWorkQueue;

  private class Consumer implements Runnable {
    @Override
    public void run() {
      while (true) {
        try {
          AuditEvent auditEvent = auditEventWorkQueue.take();
          auditLogger.log(auditEvent);
        } catch (InterruptedException ex) {
          LOG.error("Logging of audit events interrupted ! There are {} audit events left unlogged !", auditEventWorkQueue.size());

          pool.shutdownNow();
          Thread.currentThread().interrupt();

        } catch (Exception ex) {
          LOG.error("Error caught during logging audit events: " + ex);
        }

      }
    }
  }


  /**
   * Constructor.
   *
   * @param auditLogger the audit logger to extend
   */
  @Inject
  public BufferedAuditLogger(@Named(InnerLogger) AuditLogger auditLogger, @Named(Capacity) int bufferCapacity) {
    this.bufferCapacity = bufferCapacity;
    this.capacityWaterMark = 0.2; // 20 percent of full capacity

    this.auditEventWorkQueue = new LinkedBlockingQueue<>(bufferCapacity);
    this.auditLogger = auditLogger;

    this.pool = Executors.newSingleThreadExecutor();
    pool.execute(new Consumer());

  }


  /**
   * Logs audit log events
   *
   * @param event
   */
  @Override
  public void log(final AuditEvent event) {

    try {

      this.auditEventWorkQueue.put(event);

      int remainingCapacity = this.auditEventWorkQueue.remainingCapacity();

      LOG.debug("Work queue load:  [{}/{}]", bufferCapacity - remainingCapacity, bufferCapacity);

      if (remainingCapacity < bufferCapacity * capacityWaterMark)
        LOG.warn("Work queue remaining capacity: {} is below {}%", remainingCapacity, capacityWaterMark * 100);

    } catch (InterruptedException ex) {
      LOG.error("Interrupted while waiting to queue audit event: " + event.getAuditMessage());
    }
  }
}
