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

package org.apache.ambari.view.phonelist;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.ambari.view.migration.EntityConverter;
import org.apache.ambari.view.migration.ViewDataMigrationContext;
import org.easymock.Capture;
import org.junit.jupiter.api.Test;

class DataMigratorTest {

  @Test
  void acceptsOnlyTheOriginalDataVersion() throws Exception {
    ViewDataMigrationContext migrationContext = createMock(ViewDataMigrationContext.class);
    expect(migrationContext.getOriginDataVersion()).andReturn(0).andReturn(1);
    replay(migrationContext);

    DataMigrator migrator = createMigrator(migrationContext);
    assertTrue(migrator.beforeMigration());
    assertFalse(migrator.beforeMigration());

    verify(migrationContext);
  }

  @Test
  void copiesPhonePropertiesAndDerivesSurname() throws Exception {
    ViewDataMigrationContext migrationContext = createMock(ViewDataMigrationContext.class);
    Capture<EntityConverter> converter = newCapture();
    migrationContext.copyAllObjects(eq(OriginPhoneUser.class), eq(PhoneUser.class), capture(converter));
    expectLastCall();
    replay(migrationContext);

    createMigrator(migrationContext).migrateEntity(OriginPhoneUser.class, PhoneUser.class);

    PhoneUser destination = new PhoneUser();
    converter.getValue().convert(new OriginPhoneUser("Ada Lovelace", "555-0100"), destination);
    assertEquals("Ada Lovelace", destination.getName());
    assertEquals("Lovelace", destination.getSurname());
    assertEquals("555-0100", destination.getPhone());

    PhoneUser destinationWithoutSurname = new PhoneUser();
    converter.getValue().convert(new OriginPhoneUser("Ada", "555-0101"), destinationWithoutSurname);
    assertEquals("<no surname>", destinationWithoutSurname.getSurname());

    verify(migrationContext);
  }

  @Test
  void rewritesAllInstanceDataWithTheDefaultSurname() throws Exception {
    ViewDataMigrationContext migrationContext = createMock(ViewDataMigrationContext.class);
    Map<String, Map<String, String>> originData = new LinkedHashMap<>();
    originData.put("alice", Map.of("Ada", "555-0100"));
    originData.put("bob", Map.of("Grace", "555-0101"));
    expect(migrationContext.getOriginInstanceDataByUser()).andReturn(originData);
    migrationContext.putCurrentInstanceData("alice", "Ada", "<no surname>;555-0100");
    expectLastCall();
    migrationContext.putCurrentInstanceData("bob", "Grace", "<no surname>;555-0101");
    expectLastCall();
    replay(migrationContext);

    createMigrator(migrationContext).migrateInstanceData();

    verify(migrationContext);
  }

  private static DataMigrator createMigrator(ViewDataMigrationContext migrationContext) throws Exception {
    DataMigrator migrator = new DataMigrator();
    Field field = DataMigrator.class.getDeclaredField("migrationContext");
    field.setAccessible(true);
    field.set(migrator, migrationContext);
    return migrator;
  }

  public static final class OriginPhoneUser {
    private final String name;
    private final String phone;

    private OriginPhoneUser(String name, String phone) {
      this.name = name;
      this.phone = phone;
    }

    public String getName() {
      return name;
    }

    public String getPhone() {
      return phone;
    }
  }
}
