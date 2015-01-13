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
package org.apache.ambari.server.state.stack.upgrade;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.ambari.server.state.RepositoryInfo;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Tests the {@link RepositoryVersionHelper} class
 */
public class RepositoryVersionHelperTest {

  private RepositoryVersionHelper helper;

  @Before
  public void before() throws Exception {
    final Injector injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        bind(Gson.class).toInstance(new Gson());
      }
    });
    helper = injector.getInstance(RepositoryVersionHelper.class);
  }

  @Test
  public void testSerializeOperatingSystems() throws Exception {
    final List<RepositoryInfo> repositories = new ArrayList<RepositoryInfo>();
    final RepositoryInfo repository = new RepositoryInfo();
    repository.setBaseUrl("baseurl");
    repository.setOsType("os");
    repository.setRepoId("repoId");
    repositories.add(repository);

    final String serialized = helper.serializeOperatingSystems(repositories);
    Assert.assertEquals("[{\"repositories\":[{\"Repositories/base_url\":\"baseurl\",\"Repositories/repo_id\":\"repoId\"}],\"OperatingSystems/os_type\":\"os\"}]", serialized);
  }
}
