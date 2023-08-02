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
package org.apache.ambari.server.utils;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

public class CustomStringUtilsTest {
  @Test
  public void testInsertAfter() {
    final String baseText = "abcdefghijklmnopqr";
    StringBuilder content = new StringBuilder(baseText);
    int res = CustomStringUtils.insertAfter(content, "abcdefghijklmnopqr", "xxx");
    assertEquals(0, res);
    assertEquals("abcdefghijklmnopqrxxx", content.toString());

    content = new StringBuilder(baseText);
    res = CustomStringUtils.insertAfter(content, "a", "x");
    assertEquals(0, res);
    assertEquals("axbcdefghijklmnopqr", content.toString());

    content = new StringBuilder(baseText);
    res = CustomStringUtils.insertAfter(content, "x", "y");
    assertEquals(-1, res);
    assertEquals(baseText, content.toString());
  }

  @Test
  public void testDeleteSubstring() {
    final String baseText = "abcdefghijklmnopqr";
    StringBuilder content = new StringBuilder(baseText);
    int res = CustomStringUtils.deleteSubstring(content, "a");
    assertEquals(0, res);
    assertEquals("bcdefghijklmnopqr", content.toString());

    content = new StringBuilder(baseText);
    res = CustomStringUtils.deleteSubstring(content, "r");
    assertEquals(17, res);
    assertEquals("abcdefghijklmnopq", content.toString());

    content = new StringBuilder(baseText);
    res = CustomStringUtils.deleteSubstring(content, "efgh");
    assertEquals(4, res);
    assertEquals("abcdijklmnopqr", content.toString());

    content = new StringBuilder(baseText);
    res = CustomStringUtils.deleteSubstring(content, baseText);
    assertEquals(0, res);
    assertEquals("", content.toString());

    content = new StringBuilder(baseText);
    res = CustomStringUtils.deleteSubstring(content, "x");
    assertEquals(-1, res);
    assertEquals(baseText, content.toString());
  }

  @Test
  public void testReplace() {
    final String baseText = "abcdefghijklmnopqr";
    StringBuilder content = new StringBuilder(baseText);
    int res = CustomStringUtils.replace(content, "abcdefghijklmnopqr", "xxx");
    assertEquals(0, res);
    assertEquals("xxx", content.toString());

    content = new StringBuilder(baseText);
    res = CustomStringUtils.replace(content, "abcdefghijklmnopqr", "xxx");
    assertEquals(0, res);
    assertEquals("xxx", content.toString());

    content = new StringBuilder(baseText);
    res = CustomStringUtils.replace(content, "fghijk", "xxx");
    assertEquals(5, res);
    assertEquals("abcdexxxlmnopqr", content.toString());

    content = new StringBuilder(baseText);
    res = CustomStringUtils.replace(content, "xxxx", "yyyy");
    assertEquals(-1, res);
    assertEquals(baseText, content.toString());
  }
}