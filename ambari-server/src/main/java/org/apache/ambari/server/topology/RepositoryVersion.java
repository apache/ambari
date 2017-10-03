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

package org.apache.ambari.server.topology;


import org.apache.ambari.server.controller.internal.Stack;

public class RepositoryVersion {

  private final long repoVersionId;

  private final Stack stack;

  public RepositoryVersion(long repoVersionId,  Stack stack) {
    this.repoVersionId = repoVersionId;
    this.stack = stack;
  }

  public long getRepoVersionId() {
    return repoVersionId;
  }

  public Stack getStack() {
    return stack;
  }
}
