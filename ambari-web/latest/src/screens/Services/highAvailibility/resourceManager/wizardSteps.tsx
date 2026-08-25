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

import { messages } from "../../../messages";
import Step1 from "./Step1";
import Step2 from "./Step2";
import Step3 from "./Step3";
import Step4 from "./Step4";

export enum enableResourceManagerSteps {
  GET_STARTED = "GET_STARTED",
  SELECT_HOSTS = "SELECT_HOSTS",
  REVIEW = "REVIEW",
  CONFIGURE_COMPONENTS = "CONFIGURE_COMPONENTS",
}

export default {
  1: {
    label: messages["admin.rm_highAvailability.wizard.step1.header"],
    completed: false,
    Component: <Step1/>,
    canGoBack: false,
    isNextEnabled: false,
    name: enableResourceManagerSteps.GET_STARTED,
    keysToRemove: [enableResourceManagerSteps.SELECT_HOSTS, enableResourceManagerSteps.REVIEW, enableResourceManagerSteps.CONFIGURE_COMPONENTS],
  },
  2: {
    label: messages["admin.rm_highAvailability.wizard.step2.header"],
    completed: false,
    Component: <Step2/>,
    canGoBack: true,
    isNextEnabled: false,
    name: enableResourceManagerSteps.SELECT_HOSTS,
    keysToRemove: [enableResourceManagerSteps.REVIEW, enableResourceManagerSteps.CONFIGURE_COMPONENTS],
  },
  3: {
    label: messages["admin.rm_highAvailability.wizard.step3.header"],
    completed: false,
    Component: <Step3/>,
    canGoBack: true,
    isNextEnabled: false,
    name: enableResourceManagerSteps.REVIEW,
    keysToRemove: [enableResourceManagerSteps.CONFIGURE_COMPONENTS],
  },
  4: {
    label: messages["admin.rm_highAvailability.wizard.step4.header"],
    completed: false,
    Component: <Step4/>,
    canGoBack: false,
    isNextEnabled: true,
    name: enableResourceManagerSteps.CONFIGURE_COMPONENTS,
    nextLabel: "COMPLETE",
  },
};
