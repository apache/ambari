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

package org.apache.ambari.server.security;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

public class JacksonSecurityTest {

  @Test
  public void testAsyncParserEnforcesNumberLengthAcrossInputChunks() throws Exception {
    JsonFactory factory = JsonFactory.builder()
        .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(10).build())
        .build();

    try (JsonParser parser = factory.createNonBlockingByteArrayParser()) {
      ByteArrayFeeder feeder = (ByteArrayFeeder) parser.getNonBlockingInputFeeder();
      byte[] prefix = "{\"value\":".getBytes(UTF_8);
      feeder.feedInput(prefix, 0, prefix.length);
      assertEquals(JsonToken.START_OBJECT, parser.nextToken());
      assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
      assertEquals(JsonToken.NOT_AVAILABLE, parser.nextToken());

      byte[] firstDigits = "12345".getBytes(UTF_8);
      feeder.feedInput(firstDigits, 0, firstDigits.length);
      assertEquals(JsonToken.NOT_AVAILABLE, parser.nextToken());

      byte[] remainingAllowedDigits = "67890".getBytes(UTF_8);
      feeder.feedInput(remainingAllowedDigits, 0, remainingAllowedDigits.length);
      assertEquals(JsonToken.NOT_AVAILABLE, parser.nextToken());

      byte[] excessDigit = "1".getBytes(UTF_8);
      feeder.feedInput(excessDigit, 0, excessDigit.length);
      assertThrows(StreamConstraintsException.class, parser::nextToken);
    }
  }

  @Test
  public void testAsyncParserAcceptsNumbersWithinLimit() throws Exception {
    JsonFactory factory = JsonFactory.builder()
        .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(10).build())
        .build();

    try (JsonParser parser = factory.createNonBlockingByteArrayParser()) {
      ByteArrayFeeder feeder = (ByteArrayFeeder) parser.getNonBlockingInputFeeder();
      byte[] json = "{\"value\":123}".getBytes(UTF_8);
      feeder.feedInput(json, 0, json.length);
      feeder.endOfInput();
      assertEquals(JsonToken.START_OBJECT, parser.nextToken());
      assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
      assertEquals(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
      assertEquals(123, parser.getIntValue());
      assertEquals(JsonToken.END_OBJECT, parser.nextToken());
      assertNull(parser.nextToken());
    }
  }

  @Test
  public void testCaseInsensitivePropertiesStillHonorIgnoreRules() throws Exception {
    String attackerJson = "{\"admin\":{\"AdminKey\":\"attacker-controlled\",\"username\":\"operator\"}}";
    ObjectMapper mapper = new ObjectMapper();

    assertThrows(UnrecognizedPropertyException.class,
        () -> mapper.readValue(attackerJson, ConfigurationUpdate.class));

    String legitimateJson = "{\"admin\":{\"username\":\"operator\"}}";
    ConfigurationUpdate update = mapper.readValue(legitimateJson, ConfigurationUpdate.class);

    assertEquals("default", update.admin.adminKey);
    assertEquals("operator", update.admin.username);
  }

  public static class AdminConfiguration {
    public String adminKey = "default";
    public String username;
  }

  public static class ConfigurationUpdate {
    @JsonIgnoreProperties("adminKey")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
    public AdminConfiguration admin;
  }
}
