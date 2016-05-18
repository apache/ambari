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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.audit.kerberos;

import org.apache.ambari.server.audit.event.kerberos.CreatePrincipalKerberosAuditEvent;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class CreatePrincipalKerberosAuditEventTest {

  @Test
  public void testAuditMessage() throws Exception {
    // Given
    String testOperation = "Create principal";
    String testPrincipal = "testPrincipal";
    Long testRequestId = 100L;
    Long testTaskId = 99L;

    CreatePrincipalKerberosAuditEvent event = CreatePrincipalKerberosAuditEvent.builder()
      .withTimestamp(System.currentTimeMillis())
      .withOperation(testOperation)
      .withRequestId(testRequestId)
      .withPrincipal(testPrincipal)
      .withTaskId(testTaskId)
      .build();

    // When
    String actualAuditMessage = event.getAuditMessage();

    // Then
    String expectedAuditMessage = String.format("Operation(%s), Status(Success), RequestId(%d), TaskId(%d), Principal(%s)", testOperation, testRequestId, testTaskId, testPrincipal);

    assertThat(actualAuditMessage, equalTo(expectedAuditMessage));

  }

}
