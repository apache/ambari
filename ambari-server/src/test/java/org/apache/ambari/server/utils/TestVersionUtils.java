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
package org.apache.ambari.server.utils;

import org.apache.ambari.server.bootstrap.BootStrapImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import junit.framework.Assert;

public class TestVersionUtils {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testStackVersions() {
    Assert.assertTrue(VersionUtils.areVersionsEqual("2.2.0.0", "2.2.0.0", false));
    Assert.assertTrue(VersionUtils.areVersionsEqual("2.2.0.0-111", "2.2.0.0-999", false));

    Assert.assertEquals(-1, VersionUtils.compareVersions("2.2.0.0", "2.2.0.1"));
    Assert.assertEquals(-1, VersionUtils.compareVersions("2.2.0.0", "2.2.0.10"));
    Assert.assertEquals(-1, VersionUtils.compareVersions("2.2.2.0.20", "2.2.2.145"));

    Assert.assertEquals(1, VersionUtils.compareVersions("2.2.0.1", "2.2.0.0"));
    Assert.assertEquals(1, VersionUtils.compareVersions("2.2.0.10", "2.2.0.0"));
    Assert.assertEquals(1, VersionUtils.compareVersions("2.2.2.145", "2.2.2.20"));

    Assert.assertEquals(-1, VersionUtils.compareVersions("2.2.0.0-200", "2.2.0.1-100"));
    Assert.assertEquals(-1, VersionUtils.compareVersions("2.2.0.0-101", "2.2.0.10-20"));
    Assert.assertEquals(-1, VersionUtils.compareVersions("2.2.2.0.20-996", "2.2.2.145-846"));
    Assert.assertEquals(0, VersionUtils.compareVersions("2.2", "2.2.VER"));
    Assert.assertEquals(0, VersionUtils.compareVersions("2.2.VAR", "2.2.VER"));
    Assert.assertEquals(0, VersionUtils.compareVersions("2.2.3", "2.2.3.VER1.V"));
  }

  @Test
  public void testVersionCompareSuccess() {
    Assert.assertTrue(VersionUtils.areVersionsEqual("1.2.3", "1.2.3", false));
    Assert.assertTrue(VersionUtils.areVersionsEqual("1.2.3", "1.2.3", true));
    Assert.assertTrue(VersionUtils.areVersionsEqual("", "", true));
    Assert.assertTrue(VersionUtils.areVersionsEqual(null, null, true));
    Assert.assertTrue(VersionUtils.areVersionsEqual(BootStrapImpl.DEV_VERSION, "1.2.3", false));
    Assert.assertTrue(VersionUtils.areVersionsEqual(BootStrapImpl.DEV_VERSION, "", true));
    Assert.assertTrue(VersionUtils.areVersionsEqual(BootStrapImpl.DEV_VERSION, null, true));

    Assert.assertFalse(VersionUtils.areVersionsEqual("1.2.3.1", "1.2.3", false));
    Assert.assertFalse(VersionUtils.areVersionsEqual("2.1.3", "1.2.3", false));
    Assert.assertFalse(VersionUtils.areVersionsEqual("1.2.3.1", "1.2.3", true));
    Assert.assertFalse(VersionUtils.areVersionsEqual("2.1.3", "1.2.3", true));
    Assert.assertFalse(VersionUtils.areVersionsEqual("", "1.2.3", true));
    Assert.assertFalse(VersionUtils.areVersionsEqual("", null, true));
    Assert.assertFalse(VersionUtils.areVersionsEqual(null, "", true));
    Assert.assertFalse(VersionUtils.areVersionsEqual(null, "1.2.3", true));

    Assert.assertEquals(-1, VersionUtils.compareVersions("1.2.3", "1.2.4"));
    Assert.assertEquals(1, VersionUtils.compareVersions("1.2.4", "1.2.3"));
    Assert.assertEquals(0, VersionUtils.compareVersions("1.2.3", "1.2.3"));

    Assert.assertEquals(-1, VersionUtils.compareVersions("1.2.3", "1.2.4", 3));
    Assert.assertEquals(1, VersionUtils.compareVersions("1.2.4", "1.2.3", 3));
    Assert.assertEquals(0, VersionUtils.compareVersions("1.2.3", "1.2.3", 3));

    Assert.assertEquals(-1, VersionUtils.compareVersions("1.2.3.9", "1.2.4.6", 3));
    Assert.assertEquals(-1, VersionUtils.compareVersions("1.2.3", "1.2.4.6", 3));
    Assert.assertEquals(-1, VersionUtils.compareVersions("1.2", "1.2.4.6", 3));
    Assert.assertEquals(1, VersionUtils.compareVersions("1.2.4.8", "1.2.3.6.7", 3));
    Assert.assertEquals(0, VersionUtils.compareVersions("1.2.3", "1.2.3.4", 3));
    Assert.assertEquals(0, VersionUtils.compareVersions("1.2.3.6.7", "1.2.3.4", 3));
    Assert.assertEquals(1, VersionUtils.compareVersions("1.2.3.6.7", "1.2.3.4", 4));
    Assert.assertEquals(0, VersionUtils.compareVersions("1.2.3", "1.2.3.0", 4));
    Assert.assertEquals(-1, VersionUtils.compareVersions("1.2.3", "1.2.3.1", 4));
    Assert.assertEquals(1, VersionUtils.compareVersions("1.2.3.6.7\n", "1.2.3.4\n", 4)); //test version trimming

    Assert.assertEquals(1, VersionUtils.compareVersions("1.2.3.1", "1.2.3", true));
    Assert.assertEquals(1, VersionUtils.compareVersions("2.1.3", "1.2.3", true));
    Assert.assertEquals(-1, VersionUtils.compareVersions("", "1.2.3", true));
    Assert.assertEquals(1, VersionUtils.compareVersions("", null, true));
    Assert.assertEquals(-1, VersionUtils.compareVersions(null, "", true));
    Assert.assertEquals(-1, VersionUtils.compareVersions(null, "1.2.3", true));
  }

  @Test
  public void testVersionCompareError() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("version2 cannot be empty");
    VersionUtils.areVersionsEqual("1.2.3", "", false);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("version1 cannot be null");
    VersionUtils.areVersionsEqual(null, "", false);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("version2 cannot be null");
    VersionUtils.areVersionsEqual("", null, false);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("version1 cannot be empty");
    VersionUtils.compareVersions("", "1", 2);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("maxLengthToCompare cannot be less than 0");
    VersionUtils.compareVersions("2", "1", -1);
  }
}
