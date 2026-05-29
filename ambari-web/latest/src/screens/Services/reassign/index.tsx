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

import { DropdownItem } from "react-bootstrap";
import { useContext, useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import ValidateMove from "./ValidateMove";
import { ServiceContext } from "../../../store/ServiceContext";
import { AppContext } from "../../../store/context";
import { mastersNotShown, serviceNameModelMapping } from "../../../constants";
import { camelCase, map, startCase, filter } from "lodash";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faShuffle } from "@fortawesome/free-solid-svg-icons";

type ReassignComponentProps = {
  serviceName: string;
  isMappingOnly?: boolean;
};

function ReassignComponent({
  serviceName: serviceNameProp,
  isMappingOnly = false,
}: ReassignComponentProps) {
  const [shouldStartMoveFlow, setShouldStartMoveFlow] = useState(false);
  const { allServiceModels } = useContext(ServiceContext);
  const { serviceComponentInfo, allHostNames } = useContext(AppContext);
  const [allMasters, setAllMasters] = useState<string[]>([]);
  const { componentName } = useParams<{ componentName: string }>();
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    if (location.pathname.includes("reassign")) {
      setShouldStartMoveFlow(true);
    }

    // Get all master components for the service
    const serviceMasterComponents =
      allServiceModels[serviceNameModelMapping[serviceNameProp]]
        ?.masterComponents || [];

    // Filter components based on reassignAllowed property from stack service component info
    // This matches the Ember.js logic: App.StackServiceComponent.find().filterProperty('isReassignable')
    const reassignableComponents = filter(
      serviceMasterComponents,
      (masterComponent: any) => {
        const componentName = masterComponent.componentName;

        // Find the stack service component info for this component
        // Look through all services to find the one containing this component
        let stackComponent = null;

        if (serviceComponentInfo?.items) {
          for (const service of serviceComponentInfo.items) {
            if (service.components) {
              stackComponent = service.components.find(
                (comp: any) =>
                  comp?.StackServiceComponents?.component_name === componentName
              );
              if (stackComponent) break;
            }
          }
        }

        // Check if reassign is allowed and we have more than 1 host (like Ember.js)
        const isReassignable =
          stackComponent?.StackServiceComponents?.reassign_allowed === true;
        const hasMultipleHosts = allHostNames?.length > 1;

        return (
          isReassignable &&
          hasMultipleHosts &&
          !mastersNotShown.includes(componentName)
        );
      }
    );

    setAllMasters(map(reassignableComponents, "componentName"));
  }, [
    JSON.stringify(allServiceModels),
    location,
    serviceComponentInfo,
    allHostNames,
    serviceNameProp,
  ]);

  return (
    <>
      {shouldStartMoveFlow && componentName ? (
        <ValidateMove serviceName={serviceNameProp} />
      ) : null}
      {allMasters?.length && !isMappingOnly
        ? allMasters.map((master: any) => (
            <DropdownItem
              onClick={() => {
                navigate(`/main/service/reassign/${master}/step1`);
              }}
            >
              <FontAwesomeIcon icon={faShuffle} className="me-2" />
              Move {startCase(camelCase(master))}
            </DropdownItem>
          ))
        : null}
    </>
  );
}

export default ReassignComponent;
