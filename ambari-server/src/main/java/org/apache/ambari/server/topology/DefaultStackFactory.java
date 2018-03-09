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

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.state.StackId;

/**
 * Default implementation of StackFactory.
 *
 * Calls the Stack constructor to create the Stack instance.
 *
 */
public class DefaultStackFactory implements StackFactory {

  @Inject
  private Provider<AmbariMetaInfo> metaInfo;

  @Override
  public Stack createStack(StackId stackId) throws StackAccessException {
    return new Stack(stackId, metaInfo.get());
  }
}
