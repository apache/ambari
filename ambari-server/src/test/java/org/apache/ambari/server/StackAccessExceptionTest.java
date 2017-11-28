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
package org.apache.ambari.server;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StackAccessExceptionTest {

  @Test
  public void testConstructor() {
    StackAccessException ex = new StackAccessException(
      "stackName", "HDP",
      "stackVersion", "3.0.0",
      "repoVersion", "3.0.0.0-467");
    assertEquals("Stack data, stackName=HDP, stackVersion=3.0.0, repoVersion=3.0.0.0-467", ex.getMessage());
  }

  /**
   * These is an erroneous usage. However, it should be safe (throw no exception).
   */
  @Test
  public void testConstructorOddArgsShouldNotThrowException() {
    StackAccessException ex = new StackAccessException(
      "stackName", "HDP",
      "stackVersion", "3.0.0",
      "repoVersion");
    assertEquals("Stack data, stackName=HDP, stackVersion=3.0.0, repoVersion", ex.getMessage());
  }

  /**
   * These is an erroneous usage. However, it should be safe (throw no exception).
   */
  @Test
  public void testConstructorNoArgsShouldNotThrowException() {
    StackAccessException ex = new StackAccessException();
    assertEquals("Stack data", ex.getMessage());
  }


}