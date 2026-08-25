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

import { Alert, Button, Dropdown } from "react-bootstrap";
import { ServiceActionEnums } from "../../../../enums/ServiceActionEnums";
import { useContext, useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import ValidateEnablement from "./validateEnablement";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faRotate, faSitemap } from "@fortawesome/free-solid-svg-icons";
import { ServiceContext } from "../../../../store/ServiceContext";
import { AppContext } from "../../../../store/context";
import Spinner from "../../../../components/Spinner";
import useHdfsWorkflowCapabilities from "../useHdfsWorkflowCapabilities";

function EnableNamenodeFederation({ isMappingOnly }:{ isMappingOnly?: boolean }) {  
  const [shouldStartEnableFlow, setShouldStartEnableFlow] = useState(false);
  const { componentName } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { allServiceModels } = useContext(ServiceContext);
  const { allHostNames } = useContext(AppContext);
  const {
    capabilities,
    error: capabilityError,
    isLoading: isCapabilityLoading,
    retry: retryCapabilities,
  } = useHdfsWorkflowCapabilities();
  
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
    return Boolean(hdfsModel?.isNameNodeHaEnabled || (
      hdfsModel?.masterComponents?.length > 1 && !hasSNameNode
    ));
  };

  const canEnableFederation = isHAEnabled() && allHostNames.length >= 4;

  if (isMappingOnly && isCapabilityLoading) {
    return <div className="d-flex justify-content-center p-5"><Spinner /></div>;
  }
  if (isMappingOnly && capabilityError) {
    return (
      <Alert variant="danger">
        {capabilityError}
        <Button size="sm" className="ms-3" onClick={retryCapabilities}>
          <FontAwesomeIcon icon={faRotate} className="me-1" /> Retry
        </Button>
      </Alert>
    );
  }
  if (isMappingOnly && !capabilities.nameNodeFederation) {
    return (
      <Alert variant="danger">
        NameNode Federation is not supported by the active HDFS stack.
      </Alert>
    );
  }
  if (!isMappingOnly && isCapabilityLoading) {
    return <Dropdown.Item disabled>Checking HDFS Federation support...</Dropdown.Item>;
  }
  if (!isMappingOnly && capabilityError) {
    return (
      <Dropdown.Item onClick={retryCapabilities} title={capabilityError}>
        <FontAwesomeIcon className="text-secondary me-2" icon={faRotate} />
        Retry HDFS Federation capability check
      </Dropdown.Item>
    );
  }
  if (!capabilities.nameNodeFederation) return null;

  return (
    <>
      {shouldStartEnableFlow ? <ValidateEnablement /> : null}
      {!isMappingOnly ? (
        <Dropdown.Item
          onClick={() => {
            if (!canEnableFederation) {
              // Don't navigate if HA is not enabled (matches Ember.js disabled logic)
              return;
            }
            navigate(`/main/services/NameNode/federation/step1`);
          }}
          disabled={!canEnableFederation}
        >
        <FontAwesomeIcon className="text-secondary me-2" icon={faSitemap} />
          {ServiceActionEnums.enableNamenodeFederation}
        </Dropdown.Item>
      ) : null}
    </>
  );
}

export default EnableNamenodeFederation;
