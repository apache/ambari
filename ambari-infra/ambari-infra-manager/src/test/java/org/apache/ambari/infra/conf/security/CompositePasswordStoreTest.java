package org.apache.ambari.infra.conf.security;

import org.junit.Test;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

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
public class CompositePasswordStoreTest {
  @Test
  public void testGetPasswordReturnNullIfNoPasswordStoresWereAdded() {
    assertThat(new CompositePasswordStore().getPassword("any"), is(nullValue()));
  }

  @Test
  public void testGetPasswordReturnNullIfPasswordNotFoundInAnyStore() {
    assertThat(new CompositePasswordStore((prop) -> null, (prop) -> null).getPassword("any"), is(nullValue()));
  }

  @Test
  public void testGetPasswordReturnPasswordFromFistStoreIfExists() {
    assertThat(new CompositePasswordStore((prop) -> "Pass", (prop) -> null).getPassword("any"), is("Pass"));
  }

  @Test
  public void testGetPasswordReturnPasswordFromSecondStoreIfNotExistsInFirst() {
    assertThat(new CompositePasswordStore((prop) -> null, (prop) -> "Pass").getPassword("any"), is("Pass"));
  }
}