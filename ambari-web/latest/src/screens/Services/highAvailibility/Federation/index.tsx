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

import { Dropdown } from "react-bootstrap";
import { ServiceActionEnums } from "../../../../enums/ServiceActionEnums";
import { useContext, useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import ValidateEnablement from "./validateEnablement";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faSitemap } from "@fortawesome/free-solid-svg-icons";
import { ServiceContext } from "../../../../store/ServiceContext";

function EnableNamenodeFederation({ isMappingOnly }:{ isMappingOnly?: boolean }) {  
  const [shouldStartEnableFlow, setShouldStartEnableFlow] = useState(false);
  const { componentName } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { allServiceModels } = useContext(ServiceContext);
  
  useEffect(() => {
    if (
      location.pathname.includes("federation") &&
      componentName === "NameNode"
    ) {
      setShouldStartEnableFlow(true);
    }
  }, []);

  // Check if HA is enabled - matches Ember.js App.get('isHaEnabled') logic
  const isHAEnabled = () => {
    const hdfsModel = allServiceModels["hdfs"];
    const hasSNameNode = hdfsModel?.["masterComponents"]?.some(
      (component: any) => {
        return (
          component.component_name === "SECONDARY_NAMENODE" ||
          component.componentName === "SECONDARY_NAMENODE"
        );
      }
    );
    return !hasSNameNode; // HA is enabled when there's no Secondary NameNode
  };

  return (
    <>
      {shouldStartEnableFlow ? <ValidateEnablement /> : null}
      {!isMappingOnly ? (
        <Dropdown.Item
          onClick={() => {
            if (!isHAEnabled()) {
              // Don't navigate if HA is not enabled (matches Ember.js disabled logic)
              return;
            }
            navigate(`/main/services/NameNode/federation/step1`);
          }}
          disabled={!isHAEnabled()}
        >
        <FontAwesomeIcon className="text-secondary me-2" icon={faSitemap} />
          {ServiceActionEnums.enableNamenodeFederation}
        </Dropdown.Item>
      ) : null}
    </>
  );
}

export default EnableNamenodeFederation;
