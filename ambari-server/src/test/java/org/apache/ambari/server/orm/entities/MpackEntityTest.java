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

package org.apache.ambari.server.orm.entities;

import java.util.Objects;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests methods on {@link MpackEntity}.
 */
public class MpackEntityTest {
  /**
   * Tests {@link MpackEntity#hashCode()} and {@link MpackEntity#equals(Object)}
   */
  @Test
  public void testHashCodeAndEquals(){
    MpackEntity entity1 = new MpackEntity();
    MpackEntity entity2 = new MpackEntity();

    Assert.assertEquals(entity1.hashCode(), entity2.hashCode());
    Assert.assertTrue(Objects.equals(entity1, entity2));

    entity1.setId(new Long(1));
    entity2.setId(new Long(2));
    Assert.assertNotSame(entity1.hashCode(), entity2.hashCode());
    Assert.assertFalse(Objects.equals(entity1, entity2));

    entity2.setId(new Long(1));
    Assert.assertEquals(entity1.hashCode(), entity2.hashCode());
    Assert.assertTrue(Objects.equals(entity1, entity2));

    entity1.setMpackName("testMpack1");
    entity2.setMpackName("testMpack2");
    Assert.assertNotSame(entity1.hashCode(), entity2.hashCode());
    Assert.assertFalse(Objects.equals(entity1, entity2));

    entity2.setMpackName("testMpack1");
    Assert.assertEquals(entity1.hashCode(), entity2.hashCode());
    Assert.assertTrue(Objects.equals(entity1, entity2));

    entity1.setMpackVersion("3.0");
    entity2.setMpackVersion("3.1");
    Assert.assertNotSame(entity1.hashCode(), entity2.hashCode());
    Assert.assertFalse(Objects.equals(entity1, entity2));

    entity2.setMpackVersion("3.0");
    Assert.assertEquals(entity1.hashCode(), entity2.hashCode());
    Assert.assertTrue(Objects.equals(entity1, entity2));
  }
}
