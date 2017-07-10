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
package org.apache.ambari.server.sample.checks;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.checks.AbstractCheckDescriptor;
import org.apache.ambari.server.checks.CheckDescription;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrereqCheckType;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;

import com.google.common.collect.ImmutableMap;

public class SampleServiceCheck extends AbstractCheckDescriptor {

  public SampleServiceCheck() {
    super(new CheckDescription("SAMPLE_SERVICE_CHECK",
          PrereqCheckType.HOST,
          "Sample service check description.",
          new ImmutableMap.Builder<String, String>()
                          .put(AbstractCheckDescriptor.DEFAULT,
                              "Sample service check default property description.").build()));
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    prerequisiteCheck.setFailReason("Sample service check always fails.");
    prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
  }


}
