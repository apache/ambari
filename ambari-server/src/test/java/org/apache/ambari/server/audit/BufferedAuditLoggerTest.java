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

import java.util.Collections;
import java.util.List;

import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.event.OperationStatusAuditEvent;
import org.apache.ambari.server.configuration.Configuration;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.Mock;
import org.easymock.MockType;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class BufferedAuditLoggerTest {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.NICE)
  private AuditEvent auditEvent;

  @Mock(type = MockType.STRICT)
  private AuditLogger auditLogger;

  @Mock(type = MockType.STRICT)
  private Configuration configuration;


  @Before
  public void setUp() throws Exception {
    reset(auditEvent, auditLogger);

  }

  @Test(timeout = 300)
  public void testLog() throws Exception {
    // Given
    Capture<AuditEvent> capturedArgument = newCapture();
    auditLogger.log(capture(capturedArgument));

    EasyMock.expect(configuration.getBufferedAuditLoggerCapacity()).andReturn(50);
    replay(configuration);

    BufferedAuditLogger bufferedAuditLogger = new BufferedAuditLogger(auditLogger, configuration);

    replay(auditLogger, auditEvent);


    // When
    bufferedAuditLogger.log(auditEvent);

    Thread.sleep(100);
    // Then
    verify(auditLogger, configuration);


    assertThat(capturedArgument.getValue(), equalTo(auditEvent));
  }

  @Test(timeout = 300)
  public void testConsumeAuditEventsFromInternalBuffer() throws Exception {
    // Given
    EasyMock.expect(configuration.getBufferedAuditLoggerCapacity()).andReturn(5);
    replay(configuration);
    BufferedAuditLogger bufferedAuditLogger = new BufferedAuditLogger(auditLogger, configuration);

    List<AuditEvent> auditEvents = Collections.nCopies(50, auditEvent);

    auditLogger.log((AuditEvent)anyObject(AuditEvent.class));
    expectLastCall().times(50);

    replay(auditLogger, auditEvent);

    // When
    for (AuditEvent event : auditEvents) {
      bufferedAuditLogger.log(event);
    }

    // Then
    while (!bufferedAuditLogger.auditEventWorkQueue.isEmpty()) {
      Thread.sleep(100);
    }

    verify(auditLogger, auditEvent, configuration);
  }

  @Test(timeout = 3000)
  public void testMultipleProducersLogging() throws Exception {
    // Given
    int nProducers = 100;

    EasyMock.expect(configuration.getBufferedAuditLoggerCapacity()).andReturn(10000);
    replay(configuration);

    final BufferedAuditLogger bufferedAuditLogger = new BufferedAuditLogger(new AuditLoggerDefaultImpl(), configuration);

    ImmutableList.Builder<Thread> producersBuilder = ImmutableList.builder();

    for (int i = 0; i < nProducers; i++) {
      final Integer reqId = i * 10000;
      final AuditEvent event =
        OperationStatusAuditEvent.builder()
          .withStatus("IN PROGRESS")
          .withTimestamp(DateTime.now())
          .withRequestId(reqId.toString())
          .build();

      producersBuilder.add(new Thread(new Runnable() {
        final int nAuditEventsPerProducer = 100;

        @Override
        public void run() {
          for (int j = 0; j < nAuditEventsPerProducer; j++) {
            bufferedAuditLogger.log(event);
          }

        }
      }

      ));
    }

    List<Thread> producers = producersBuilder.build();



    // When
    for (Thread producer : producers) {
      producer.start(); // nProducers threads creating nAuditEventsPerProducer events each in parallel
    }

    // Then
    while (!bufferedAuditLogger.auditEventWorkQueue.isEmpty()) {
      Thread.sleep(100);
    }

    verify(configuration);

  }
}
