package org.apache.ambari.infra.conf.security;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Optional;

import org.junit.Test;

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
public class CompositeSecretTest {
  @Test
  public void testGetPasswordReturnNullIfNoPasswordStoresWereAdded() {
    assertThat(new CompositeSecret().get().isPresent(), is(false));
  }

  @Test
  public void testGetPasswordReturnNullIfPasswordNotFoundInAnyStore() {
    assertThat(new CompositeSecret(Optional::empty, Optional::empty).get().isPresent(), is(false));
  }

  @Test
  public void testGetPasswordReturnPasswordFromFirstStoreIfExists() {
    assertThat(new CompositeSecret(() -> Optional.of("Pass"), Optional::empty).get().get(), is("Pass"));
  }

  @Test
  public void testGetPasswordReturnPasswordFromSecondStoreIfNotExistsInFirst() {
    assertThat(new CompositeSecret(Optional::empty, () -> Optional.of("Pass")).get().get(), is("Pass"));
  }
}