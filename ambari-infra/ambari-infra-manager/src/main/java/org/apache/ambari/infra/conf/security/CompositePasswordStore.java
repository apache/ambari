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
package org.apache.ambari.infra.conf.security;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class CompositePasswordStore implements PasswordStore {
  private List<PasswordStore> passwordStores;

  public CompositePasswordStore(PasswordStore... passwordStores) {
    this.passwordStores = new ArrayList<>(asList(passwordStores));
  }

  @Override
  public String getPassword(String propertyName) {
    for (PasswordStore passwordStore : passwordStores) {
      String password = passwordStore.getPassword(propertyName);
      if (password != null)
        return password;
    }
    return null;
  }
}
