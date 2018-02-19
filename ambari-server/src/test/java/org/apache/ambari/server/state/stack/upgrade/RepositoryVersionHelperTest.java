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
package org.apache.ambari.server.state.stack.upgrade;

import org.apache.ambari.server.state.StackId;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@link RepositoryVersionHelper} class
 */
public class RepositoryVersionHelperTest {

  @Test
  public void testGPLRepoIsRequired(){
    String versionGreater1 = "2.7.0.3-75";
    String versionGreater2 = "2.7.0.3";
    String versionGreater3 = "2.7";
    String versionEquals1 = "2.6.4.0-75";
    String versionEquals2 = "2.6.4.0";
    String versionEquals3 = "2.6.4";
    String versionLower1 = "2.1.0.3-75";
    String versionLower2 = "2.1.0.3";
    String versionLower3 = "2.1";
    StackId hdpStackId = new StackId();
    hdpStackId.setStackId("HDP-x.x");

    Assert.assertEquals(true, RepositoryVersionHelper.shouldContainGPLRepo(hdpStackId, versionGreater1));
    Assert.assertEquals(true, RepositoryVersionHelper.shouldContainGPLRepo(hdpStackId, versionGreater2));
    Assert.assertEquals(true, RepositoryVersionHelper.shouldContainGPLRepo(hdpStackId, versionGreater3));

    Assert.assertEquals(true, RepositoryVersionHelper.shouldContainGPLRepo(hdpStackId, versionEquals1));
    Assert.assertEquals(true, RepositoryVersionHelper.shouldContainGPLRepo(hdpStackId, versionEquals2));
    Assert.assertEquals(true, RepositoryVersionHelper.shouldContainGPLRepo(hdpStackId, versionEquals3));

    Assert.assertEquals(false, RepositoryVersionHelper.shouldContainGPLRepo(hdpStackId, versionLower1));
    Assert.assertEquals(false, RepositoryVersionHelper.shouldContainGPLRepo(hdpStackId, versionLower2));
    Assert.assertEquals(false, RepositoryVersionHelper.shouldContainGPLRepo(hdpStackId, versionLower3));

    StackId nonHDPStackId = new StackId();
    hdpStackId.setStackId("NOTHDP-x.x");

    Assert.assertEquals(false, RepositoryVersionHelper.shouldContainGPLRepo(nonHDPStackId, versionGreater1));
    Assert.assertEquals(false, RepositoryVersionHelper.shouldContainGPLRepo(nonHDPStackId, versionGreater2));
    Assert.assertEquals(false, RepositoryVersionHelper.shouldContainGPLRepo(nonHDPStackId, versionGreater3));

    Assert.assertEquals(false, RepositoryVersionHelper.shouldContainGPLRepo(nonHDPStackId, versionEquals1));
    Assert.assertEquals(false, RepositoryVersionHelper.shouldContainGPLRepo(nonHDPStackId, versionEquals2));
    Assert.assertEquals(false, RepositoryVersionHelper.shouldContainGPLRepo(nonHDPStackId, versionEquals3));

    Assert.assertEquals(false, RepositoryVersionHelper.shouldContainGPLRepo(nonHDPStackId, versionLower1));
    Assert.assertEquals(false, RepositoryVersionHelper.shouldContainGPLRepo(nonHDPStackId, versionLower2));
    Assert.assertEquals(false, RepositoryVersionHelper.shouldContainGPLRepo(nonHDPStackId, versionLower3));
  }
}
