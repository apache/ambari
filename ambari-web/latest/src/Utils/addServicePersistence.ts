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

import ClusterApi from "../api/clusterApi";

export const CANCEL_ADD_SERVICE_WIZARD_EVENT =
  "ambari:cancel-add-service-wizard";

type HashReloadLocation = Pick<Location, "hash" | "reload">;

export const reloadAtHashRoute = (
  route: string,
  location: HashReloadLocation = window.location,
) => {
  location.hash = route.startsWith("/") ? route : `/${route}`;
  location.reload();
};

export const buildClearedAddServiceState = (
  initialState: object,
  requestSequence = 0
) =>
  JSON.stringify({
    ADD_SERVICE: JSON.stringify({
      ...initialState,
      requestSequence,
    }),
    CLUSTER_STATE: JSON.stringify({}),
  });

export const clearAddServiceWizardState = (
  initialState: object,
  requestSequence = 0
) =>
  ClusterApi.postPersistData(
    buildClearedAddServiceState(initialState, requestSequence)
  );
