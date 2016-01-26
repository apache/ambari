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

import org.junit.Assert;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * AdminSettingEntity unit tests.
 */
public class AdminSettingEntityTest {

  @Test
  public void testSetGetId() {
    long id = 1000;
    AdminSettingEntity entity = new AdminSettingEntity();
    entity.setId(id);
    assertEquals(id, entity.getId());
  }

  @Test
  public void testSetGetName() {
    String name = "motd";
    AdminSettingEntity entity = new AdminSettingEntity();
    entity.setName(name);
    assertEquals(name, entity.getName());
  }

  @Test
  public void testSetGetSettingType() {
    String settingType = "ambari-server";
    AdminSettingEntity entity = new AdminSettingEntity();
    entity.setSettingType(settingType);
    assertEquals(settingType, entity.getSettingType());
  }

  @Test
  public void testSetGetContent() {
    String content = "{tag:random-tag, text:random-text}";
    AdminSettingEntity entity = new AdminSettingEntity();
    entity.setContent(content);
    assertEquals(content, entity.getContent());
  }

  @Test
  public void testSetGetUpdatedBy() {
    String updatedBy = "ambari";
    AdminSettingEntity entity = new AdminSettingEntity();
    entity.setUpdatedBy(updatedBy);
    assertEquals(updatedBy, entity.getUpdatedBy());
  }

  @Test
  public void testSetGetUpdatedTimeStamp() {
    long updateTimeStamp = 1234567890;
    AdminSettingEntity entity = new AdminSettingEntity();
    entity.setUpdateTimestamp(updateTimeStamp);
    assertEquals(updateTimeStamp, entity.getUpdateTimestamp());
  }

  @Test
  public void testEquals() {
    AdminSettingEntity entity = new AdminSettingEntity();
    entity.setId(1);
    entity.setName("motd");
    entity.setContent("{tag:random-tag, text:random-text}");
    entity.setSettingType("ambari-server");
    entity.setUpdatedBy("ambari");
    entity.setUpdateTimestamp(1234567890);
    AdminSettingEntity newEntity = entity.clone();
    assertEquals(entity, newEntity);
  }

}
