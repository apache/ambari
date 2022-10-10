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
package org.apache.ambari.server.controller.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.ResourcePredicateEvaluator;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Resource provider for auth resources.
 */
public class AuthResourceProvider extends AbstractControllerResourceProvider implements ResourcePredicateEvaluator {

  /**
   * Create a new resource provider for the given management controller.
   */
  @AssistedInject
  AuthResourceProvider(@Assisted AmbariManagementController managementController) {
    super(Resource.Type.Auth, Collections.emptySet(), Collections.emptyMap(), managementController);
  }

  @Override
  public RequestStatus createResourcesAuthorized(Request request)
      throws SystemException,
      UnsupportedPropertyException,
      ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    // do nothing
    return getRequestStatus(null);
  }

  /**
   * ResourcePredicateEvaluator implementation. If property type is Auth/user_name,
   * we do a case insensitive comparison so that we can return the retrieved
   * username when it differs only in case with respect to the requested username.
   *
   * @param predicate the predicate
   * @param resource  the resource
   * @return
   */
  @Override
  public boolean evaluate(Predicate predicate, Resource resource) {
    return true;
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>();
  }
}
