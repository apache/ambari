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
package org.apache.ambari.server.state.stack.upgrade;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.state.RepositoryInfo;
import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;

/**
 * Tests the {@link RepositoryVersionHelper} class
 */
public class RepositoryVersionHelperTest {

  private RepositoryVersionHelper helper;

  @Test
  public void testSerializeOperatingSystems() throws Exception {
    Gson gson = new Gson();
    Field field = RepositoryVersionHelper.class.getDeclaredField("gson");
    field.setAccessible(true);

    RepositoryVersionHelper helper = new RepositoryVersionHelper();
    field.set(helper, gson);

    final List<RepositoryInfo> repositories = new ArrayList<>();
    final RepositoryInfo repository = new RepositoryInfo();
    repository.setBaseUrl("baseurl");
    repository.setOsType("os");
    repository.setRepoId("repoId");
    repository.setUnique(true);
    repository.setAmbariManagedRepositories(true);
    repositories.add(repository);

    final String serialized = helper.serializeOperatingSystems(repositories);
    Assert.assertEquals("[{\"OperatingSystems/ambari_managed_repositories\":true,\"repositories\":[{\"Repositories/base_url\":\"baseurl\",\"Repositories/repo_id\":\"repoId\",\"Repositories/unique\":true,\"Repositories/tags\":[]}],\"OperatingSystems/os_type\":\"os\"}]", serialized);
  }
}
