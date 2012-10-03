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

package org.apache.ambari.server.state;

import junit.framework.Assert;

import org.apache.ambari.server.state.ConfigVersion;
import org.apache.ambari.server.state.DeployState;
import org.apache.ambari.server.state.StackVersion;
import org.apache.ambari.server.state.State;
import org.junit.Test;

public class DeployStateTest {

  @Test
  public void testEquals() {
    State s1 = new State();
    State s2 = new State();

    Assert.assertTrue(s1.equals(s2));

    s1.setConfigVersion(new ConfigVersion("x.y.z"));
    Assert.assertFalse(s1.equals(s2));
    Assert.assertFalse(s2.equals(s1));

    s2.setConfigVersion(new ConfigVersion("x.y.foo"));
    Assert.assertFalse(s1.equals(s2));
    Assert.assertFalse(s2.equals(s1));

    s2.setConfigVersion(new ConfigVersion("x.y.z"));
    Assert.assertTrue(s1.equals(s2));

    s2.setStackVersion(new StackVersion("1.x"));
    Assert.assertFalse(s1.equals(s2));
    Assert.assertFalse(s2.equals(s1));

    s1.setStackVersion(new StackVersion("1.x"));
    Assert.assertTrue(s1.equals(s2));

    s1.setStackVersion(new StackVersion("2.x"));
    Assert.assertFalse(s1.equals(s2));
    Assert.assertFalse(s2.equals(s1));

    s1.setState(DeployState.INSTALLED);
    Assert.assertFalse(s1.equals(s2));

  }

}
