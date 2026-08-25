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

import { State, Action, ActionTypes } from "./types";

export const initialState: State = { enableNamenodeFederationSteps: {} };

export const reducer = (state: State, action: Action): State => {
  switch (action.type) {
    case ActionTypes.STORE_INFORMATION: {
      const enableNamenodeFederationSteps = {
        ...state.enableNamenodeFederationSteps,
        [action.payload.step]: action.payload,
      };
      return { ...state, enableNamenodeFederationSteps };
    }
    case ActionTypes.SYNC_STATE:
      return { ...action.payload };
    case ActionTypes.REMOVE_KEY: {
      const newState = { ...state };
      const newSteps = { ...newState.enableNamenodeFederationSteps };
      delete newSteps[action.payload.key];
      return { ...newState, enableNamenodeFederationSteps: newSteps };
    }
    default:
      return state;
  }
};
