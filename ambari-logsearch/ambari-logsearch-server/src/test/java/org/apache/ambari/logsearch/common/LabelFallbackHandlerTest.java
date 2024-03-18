/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch.common;

import org.apache.ambari.logsearch.conf.UIMappingConfig;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LabelFallbackHandlerTest {

  private LabelFallbackHandler underTest;

  @Before
  public void setUp() {
    final UIMappingConfig uiMappingConfig = new UIMappingConfig();
    uiMappingConfig.setLabelFallbackEnabled(true);
    underTest = new LabelFallbackHandler(uiMappingConfig);
  }

  @Test
  public void testFallbackIgnore() {
    // GIVEN
    String testInput = "my_field";
    // WHEN
    String result = underTest.fallbackIfRequired(testInput, "spec label", true, false, true, null, null);
    // THEN
    assertEquals("spec label", result);
  }

  @Test
  public void testFallbackUnderscore() {
    // GIVEN
    String testInput = "my_field";
    // WHEN
    String result = underTest.fallback(testInput, true, false, true);
    // THEN
    assertEquals("My Field", result);
  }

  @Test
  public void testFallbackUnderscoreWithNull() {
    // GIVEN
    // WHEN
    String result = underTest.fallback(null, true, false, true);
    // THEN
    assertNull(result);
  }

  @Test
  public void testFallbackCamelCase() {
    // GIVEN
    String testInput = "myField";
    // WHEN
    String result = underTest.fallback(testInput, false, true, true);
    // THEN
    assertEquals("My Field", result);
  }

  @Test
  public void testFallbackCamelCaseWithEmptyString() {
    // GIVEN
    String testInput = "";
    // WHEN
    String result = underTest.fallback(testInput, true, true, true);
    // THEN
    assertNull(result);
  }

  @Test
  public void testFallbackCamelCaseWithNull() {
    // GIVEN
    // WHEN
    String result = underTest.fallback(null, true, true, true);
    // THEN
    assertNull(result);
  }

  @Test
  public void testFallbackCamelCaseWith1Letter() {
    // GIVEN
    String testInput = "d";
    // WHEN
    String result = underTest.fallback(testInput, true, true, true);
    // THEN
    assertEquals("D", result);
  }

  @Test
  public void testFallbackWithRemovingPrefixes() {
    // GIVEN
    String testInput1 = "ws_request_id";
    String testInput2 = "std_request_username";
    // WHEN
    String result1 = underTest.fallback(testInput1, true, true, true, Arrays.asList("ws_", "std_"), null);
    String result2 = underTest.fallback(testInput2, true, true, true, Arrays.asList("ws_", "std_"), null);
    // THEN
    assertEquals("Request Id", result1);
    assertEquals("Request Username", result2);
  }

  @Test
  public void testFallbackWithRemovingSuffixes() {
    // GIVEN
    String testInput1 = "request_id_i";
    String testInput2 = "request_username_s";
    // WHEN
    String result1 = underTest.fallback(testInput1, true, true, true, null, Arrays.asList("_i", "_s"));
    String result2 = underTest.fallback(testInput2, true, true, true, null, Arrays.asList("_i", "_s"));
    // THEN
    assertEquals("Request Id", result1);
    assertEquals("Request Username", result2);
  }

  @Test
  public void testFallbackWithRemovingPrefixesWithoutAnyPrefix() {
    // GIVEN
    String testInput = "request_id";
    // WHEN
    String result = underTest.fallback(testInput, true, true, true, Arrays.asList("ws_", "std_"), null);
    // THEN
    assertEquals("Request Id", result);
  }

}
