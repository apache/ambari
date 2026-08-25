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

import {
  faArrowUp,
  faMinus,
  faSitemap,
} from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { useContext } from "react";
import { Button, Dropdown } from "react-bootstrap";
import { useNavigate } from "react-router-dom";
import { ServiceActionEnums } from "../../../enums/ServiceActionEnums";
import { ServiceContext } from "../../../store/ServiceContext";
import { getHdfsNamespaces } from "./haWorkflowUtils";
import useHawqStandbyCapabilities from "./HawqStandby/useHawqStandbyCapabilities";
import useHdfsWorkflowCapabilities from "./useHdfsWorkflowCapabilities";

interface WorkflowActionsProps {
  canEnableHighAvailability: boolean;
  canRunHawqCustomCommands: boolean;
  canPersistWorkflow: boolean;
  serviceName: string;
}

export default function WorkflowActions({
  canEnableHighAvailability,
  canRunHawqCustomCommands,
  canPersistWorkflow,
  serviceName,
}: WorkflowActionsProps) {
  const navigate = useNavigate();
  const { allServiceModels } = useContext(ServiceContext);
  const normalizedServiceName = serviceName.toUpperCase();
  const isHdfs = normalizedServiceName === "HDFS";
  const isHawq = normalizedServiceName === "HAWQ";
  const canEnterHaWorkflow = canEnableHighAvailability && canPersistWorkflow;
  const canEnterHawqCustomWorkflow =
    canRunHawqCustomCommands && canPersistWorkflow;
  const namespaces = isHdfs
    ? getHdfsNamespaces(allServiceModels.hdfs)
    : [];
  const { capabilities: hdfsCapabilities } = useHdfsWorkflowCapabilities(
    isHdfs && canEnterHaWorkflow,
  );
  const {
    capabilities,
    error: hawqCapabilityError,
    isLoading: isHawqCapabilityLoading,
    retry: retryHawqCapabilities,
  } = useHawqStandbyCapabilities(
      isHawq && (canEnterHaWorkflow || canEnterHawqCustomWorkflow),
    );

  if (!canPersistWorkflow) return null;

  return (
    <>
      {isHawq &&
      (canEnterHaWorkflow || canEnterHawqCustomWorkflow) &&
      isHawqCapabilityLoading ? (
        <Dropdown.ItemText className="text-secondary">
          Loading HAWQ workflow capabilities...
        </Dropdown.ItemText>
      ) : null}
      {isHawq &&
      (canEnterHaWorkflow || canEnterHawqCustomWorkflow) &&
      hawqCapabilityError ? (
        <Dropdown.ItemText className="text-danger">
          {hawqCapabilityError}
          <Button
            type="button"
            variant="link"
            size="sm"
            className="ms-2 p-0 align-baseline"
            onClick={retryHawqCapabilities}
          >
            Retry
          </Button>
        </Dropdown.ItemText>
      ) : null}
      {isHdfs && canEnterHaWorkflow && hdfsCapabilities.routerFederation ? (
        <Dropdown.Item
          disabled={namespaces.length < 2}
          onClick={() =>
            navigate(
              "/main/services/NameNode/federation/routerBasedFederation/step1",
            )
          }
        >
          <FontAwesomeIcon className="text-secondary me-2" icon={faSitemap} />
          {ServiceActionEnums.addDfsRouter}
        </Dropdown.Item>
      ) : null}
      {isHawq && canEnterHaWorkflow && capabilities.canAdd ? (
        <Dropdown.Item
          onClick={() =>
            navigate("/main/services/highAvailability/Hawq/add/step1")
          }
        >
          <FontAwesomeIcon className="text-secondary me-2" icon={faSitemap} />
          {ServiceActionEnums.addHawqStandby}
        </Dropdown.Item>
      ) : null}
      {isHawq && canEnterHawqCustomWorkflow && capabilities.canRemove ? (
        <Dropdown.Item
          onClick={() =>
            navigate("/main/services/highAvailability/Hawq/remove/step1")
          }
        >
          <FontAwesomeIcon className="text-secondary me-2" icon={faMinus} />
          {ServiceActionEnums.removeHawqStandby}
        </Dropdown.Item>
      ) : null}
      {isHawq && canEnterHawqCustomWorkflow && capabilities.canActivate ? (
        <Dropdown.Item
          onClick={() =>
            navigate("/main/services/highAvailability/Hawq/activate/step1")
          }
        >
          <FontAwesomeIcon className="text-secondary me-2" icon={faArrowUp} />
          {ServiceActionEnums.activateHawqStandby}
        </Dropdown.Item>
      ) : null}
    </>
  );
}
