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

import { HawqStandbyMode, hawqStandbySteps } from "./context";
import {
  HawqProgressStep,
  HawqReviewStep,
  HawqSelectHostStep,
  HawqStep1,
} from "./Steps";

export function createHawqWizardSteps(mode: HawqStandbyMode) {
  if (mode === "add") {
    return {
      0: {
        label: "Get Started",
        completed: false,
        Component: <HawqStep1 />,
        canGoBack: false,
        isNextEnabled: true,
        name: hawqStandbySteps.GET_STARTED,
      },
      1: {
        label: "Select Host",
        completed: false,
        Component: <HawqSelectHostStep />,
        canGoBack: true,
        isNextEnabled: false,
        name: hawqStandbySteps.SELECT_HOST,
      },
      2: {
        label: "Review",
        completed: false,
        Component: <HawqReviewStep />,
        canGoBack: true,
        isNextEnabled: false,
        name: hawqStandbySteps.REVIEW,
      },
      3: {
        label: "Configure HAWQ",
        completed: false,
        Component: <HawqProgressStep />,
        canGoBack: false,
        isNextEnabled: false,
        name: hawqStandbySteps.CONFIGURE,
      },
    };
  }
  return {
    0: {
      label: "Get Started",
      completed: false,
      Component: <HawqStep1 />,
      canGoBack: false,
      isNextEnabled: true,
      name: hawqStandbySteps.GET_STARTED,
    },
    1: {
      label: "Review",
      completed: false,
      Component: <HawqReviewStep />,
      canGoBack: true,
      isNextEnabled: false,
      name: hawqStandbySteps.REVIEW,
    },
    2: {
      label: "Configure HAWQ",
      completed: false,
      Component: <HawqProgressStep />,
      canGoBack: false,
      isNextEnabled: false,
      name: hawqStandbySteps.CONFIGURE,
    },
  };
}
