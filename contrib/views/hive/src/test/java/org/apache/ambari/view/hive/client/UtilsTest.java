/**
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
package org.apache.ambari.view.hive.client;

import org.apache.hive.service.cli.thrift.TStatus;
import org.apache.hive.service.cli.thrift.TStatusCode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

public class UtilsTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testRemoveEmptyStrings() throws Exception {
    String[] arrayWithSomeEmptyStrings = new String[] { "", null, "string1", null, "", "string2", "" };
    String[] expectedStrings = Utils.removeEmptyStrings(arrayWithSomeEmptyStrings);

    assertEquals(2, expectedStrings.length);
    assertEquals("string1", expectedStrings[0]);
    assertEquals("string2", expectedStrings[1]);
  }

  @Test
  public void testVerifySuccessWithHiveInvalidQueryException() throws Exception{
    String msg = "Error in compiling";
    String comment = "H110 Unable to submit statement";

    TStatus status = createMockTStatus(10000,msg,TStatusCode.ERROR_STATUS);
    thrown.expect(HiveInvalidQueryException.class);
    thrown.expectMessage(msg);

    Utils.verifySuccess(status,comment);
  }

  @Test
  public void testVerifySuccessWithHiveErrorStatusException() throws Exception{
    String msg = "Error in compiling";
    String comment = "H110 Unable to submit statement";

    TStatus status = createMockTStatus(40000,msg,TStatusCode.ERROR_STATUS);
    thrown.expect(HiveErrorStatusException.class);
    thrown.expectMessage(String.format("%s. %s",comment,msg));

    Utils.verifySuccess(status,comment);
  }

  private TStatus createMockTStatus(int errorCode,String msg,TStatusCode tStatusCode){
    TStatus status = createNiceMock(TStatus.class);
    expect(status.getErrorCode()).andReturn(errorCode).anyTimes();
    expect(status.getStatusCode()).andReturn(tStatusCode).anyTimes();
    expect(status.getErrorMessage()).andReturn(msg).anyTimes();
    replay(status);
    return status;
  }
}