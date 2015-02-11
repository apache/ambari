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
package org.apache.ambari.server.checks;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.stack.PrereqCheckType;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.junit.Test;

/**
 * Unit tests for AbstractCheckDescriptor
 */
public class AbstractCheckDescriptorTest {

  private class TestCheckImpl extends AbstractCheckDescriptor {

    public TestCheckImpl(String id, PrereqCheckType type, String description) {
      super(id, type, description);
    }

    @Override
    public void perform(PrerequisiteCheck prerequisiteCheck,
        PrereqCheckRequest request) throws AmbariException {
    }
  }

  @Test
  public void testFormatEntityList() {
    AbstractCheckDescriptor check = new TestCheckImpl(null, PrereqCheckType.HOST, null);

    Assert.assertEquals("", check.formatEntityList(null));

    final List<String> failedOn = new ArrayList<String>();
    Assert.assertEquals("", check.formatEntityList(failedOn));

    failedOn.add("host1");
    Assert.assertEquals("host1 host", check.formatEntityList(failedOn));

    failedOn.add("host2");
    Assert.assertEquals("host1 and host2 hosts", check.formatEntityList(failedOn));

    failedOn.add("host3");
    Assert.assertEquals("host1, host2 and host3 hosts", check.formatEntityList(failedOn));

    check = new TestCheckImpl(null, PrereqCheckType.CLUSTER, null);
    Assert.assertEquals("host1, host2 and host3 clusters", check.formatEntityList(failedOn));

    check = new TestCheckImpl(null, PrereqCheckType.SERVICE, null);
    Assert.assertEquals("host1, host2 and host3 services", check.formatEntityList(failedOn));

    check = new TestCheckImpl(null, null, null);
    Assert.assertEquals("host1, host2 and host3", check.formatEntityList(failedOn));
  }

}
