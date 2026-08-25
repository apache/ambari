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

import { faMinus, faPlus, faRotate } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { useContext, useEffect, useState } from "react";
import { Alert, Button, Col, Form, Row, Stack } from "react-bootstrap";
import AssignMastersApi from "../../../../api/assignMastersApi";
import { HostsApi } from "../../../../api/hostsApi";
import Spinner from "../../../../components/Spinner";
import { AppContext } from "../../../../store/context";
import {
  buildHostValidationPayload,
  ComponentAssignment,
} from "./workflowUtils";

interface HostRecord {
  hostName: string;
  maintenanceState: string;
  components: string[];
}

interface HostAssignmentProps {
  componentName: string;
  componentLabel: string;
  installedHosts: string[];
  initialAssignments?: ComponentAssignment[];
  additionalCount: number;
  allowCountChange?: boolean;
  services: string[];
  onChange: (assignments: ComponentAssignment[], unavailableHosts: string[]) => void;
}

const errorMessage = (error: any, fallback: string) =>
  error?.response?.data?.message || error?.message || fallback;

function responseAssignments(data: any, componentName: string): string[] {
  const recommendations = data?.resources?.[0]?.recommendations;
  const bindings = new Map<string, string[]>();
  (recommendations?.blueprint_cluster_binding?.host_groups || []).forEach(
    (group: any) =>
      bindings.set(
        group.name,
        (group.hosts || []).map((host: any) => host.fqdn).filter(Boolean),
      ),
  );
  return (recommendations?.blueprint?.host_groups || []).flatMap((group: any) =>
    (group.components || []).some(
      (component: any) => (component.name || component.component_name) === componentName,
    )
      ? bindings.get(group.name) || []
      : [],
  );
}

function HostAssignment({
  componentName,
  componentLabel,
  installedHosts,
  initialAssignments = [],
  additionalCount,
  allowCountChange = false,
  services,
  onChange,
}: HostAssignmentProps) {
  const {
    clusterName,
    cluster: { stack, versionNum },
  } = useContext(AppContext);
  const restoredHosts = initialAssignments
    .filter(
      (assignment) =>
        (assignment.component || assignment.component_name) === componentName &&
        !assignment.isInstalled,
    )
    .map((assignment) => assignment.hostName || assignment.selectedHost || "")
    .filter(Boolean);
  const [hosts, setHosts] = useState<HostRecord[]>([]);
  const [additionalHosts, setAdditionalHosts] = useState<string[]>(
    restoredHosts.length
      ? restoredHosts
      : Array.from({ length: additionalCount }, () => ""),
  );
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [recommendationError, setRecommendationError] = useState("");
  const [retryCount, setRetryCount] = useState(0);

  useEffect(() => {
    void loadHostsAndRecommendations();
  }, [retryCount]);

  useEffect(() => {
    if (!hosts.length) return;
    const assignments = [
      ...installedHosts.map((hostName) => ({
        component: componentName,
        component_name: componentName,
        hostName,
        selectedHost: hostName,
        isInstalled: true,
        isAvailable: hosts.some((host) => host.hostName === hostName),
      })),
      ...additionalHosts.map((hostName) => ({
        component: componentName,
        component_name: componentName,
        hostName,
        selectedHost: hostName,
        isInstalled: false,
        isAvailable: hosts.some((host) => host.hostName === hostName),
      })),
    ];
    const unavailableHosts = hosts
      .filter((host) => host.maintenanceState !== "OFF")
      .map((host) => host.hostName);
    onChange(assignments, unavailableHosts);
  }, [hosts, additionalHosts.join("|")]);

  async function loadHostsAndRecommendations() {
    setIsLoading(true);
    setLoadError("");
    setRecommendationError("");
    try {
      const data = await HostsApi.getHostComponentsDetails(
        clusterName,
        "fields=Hosts/host_name,Hosts/maintenance_state," +
          "host_components/HostRoles/component_name," +
          "host_components/HostRoles/host_name&minimal_response=true",
      );
      const loadedHosts: HostRecord[] = (data?.items || []).map((item: any) => ({
        hostName: item.Hosts?.host_name,
        maintenanceState: item.Hosts?.maintenance_state || "OFF",
        components: (item.host_components || [])
          .map((hostComponent: any) => hostComponent.HostRoles?.component_name)
          .filter(Boolean),
      }));
      if (!loadedHosts.length) throw new Error("Ambari returned no cluster hosts.");
      setHosts(loadedHosts);
      if (restoredHosts.length) {
        setIsLoading(false);
        return;
      }

      const existingAssignments: ComponentAssignment[] = loadedHosts.flatMap(
        (host) =>
          host.components.map((component) => ({
            component,
            hostName: host.hostName,
            isInstalled: true,
          })),
      );
      try {
        const payload = {
          ...buildHostValidationPayload(
            loadedHosts.map((host) => host.hostName),
            services,
            existingAssignments,
          ),
          recommend: "host_groups",
        };
        delete (payload as any).validate;
        const recommendation = await AssignMastersApi.postRecommendations(
          payload,
          stack,
          versionNum,
        );
        const recommended = responseAssignments(
          recommendation,
          componentName,
        ).filter((host) => !installedHosts.includes(host));
        setAdditionalHosts(
          chooseInitialHosts(loadedHosts, recommended, additionalCount),
        );
      } catch (error: any) {
        setRecommendationError(
          errorMessage(
            error,
            "Stack Advisor could not recommend hosts. Review the fallback assignment before continuing.",
          ),
        );
        setAdditionalHosts(
          chooseInitialHosts(loadedHosts, [], additionalCount),
        );
      }
    } catch (error: any) {
      setLoadError(
        errorMessage(error, "Ambari could not load the cluster hosts."),
      );
      setHosts([]);
    } finally {
      setIsLoading(false);
    }
  }

  function chooseInitialHosts(
    loadedHosts: HostRecord[],
    preferredHosts: string[],
    count: number,
  ) {
    const available = loadedHosts
      .filter(
        (host) =>
          host.maintenanceState === "OFF" &&
          !installedHosts.includes(host.hostName),
      )
      .map((host) => host.hostName);
    const ordered = [
      ...new Set([
        ...preferredHosts.filter((host) => available.includes(host)),
        ...available,
      ]),
    ];
    return Array.from({ length: count }, (_, index) => ordered[index] || "");
  }

  const selectableHosts = hosts.filter(
    (host) => host.maintenanceState === "OFF",
  );
  const availableAdditionalHostCount = selectableHosts.filter(
    (host) => !installedHosts.includes(host.hostName),
  ).length;
  if (isLoading) {
    return (
      <div className="d-flex justify-content-center p-4">
        <Spinner />
      </div>
    );
  }
  if (loadError) {
    return (
      <Alert variant="danger">
        {loadError}
        <Button
          size="sm"
          className="ms-3"
          onClick={() => setRetryCount((value) => value + 1)}
        >
          <FontAwesomeIcon icon={faRotate} className="me-1" /> Retry
        </Button>
      </Alert>
    );
  }

  return (
    <>
      {recommendationError ? (
        <Alert variant="warning">
          {recommendationError}
          <Button
            size="sm"
            variant="outline-secondary"
            className="ms-3"
            onClick={() => setRetryCount((value) => value + 1)}
          >
            <FontAwesomeIcon icon={faRotate} className="me-1" /> Retry Advisor
          </Button>
        </Alert>
      ) : null}
      <Stack gap={3}>
        {installedHosts.map((hostName, index) => (
          <Row key={`installed-${hostName}`} className="align-items-center">
            <Col md={4} className="bolder">
              Current {componentLabel}{installedHosts.length > 1 ? ` ${index + 1}` : ""}
            </Col>
            <Col md={6}>{hostName}</Col>
          </Row>
        ))}
        {additionalHosts.map((hostName, index) => (
          <Row key={`additional-${index}`} className="align-items-center">
            <Col md={4} className="bolder">
              Additional {componentLabel} {index + 1}
            </Col>
            <Col md={6}>
              <Form.Select
                aria-label={`Additional ${componentLabel} ${index + 1}`}
                value={hostName}
                onChange={(event) => {
                  const next = [...additionalHosts];
                  next[index] = event.target.value;
                  setAdditionalHosts(next);
                }}
              >
                <option value="">Select a host</option>
                {selectableHosts
                  .filter(
                    (host) =>
                      host.hostName === hostName ||
                      (!installedHosts.includes(host.hostName) &&
                        !additionalHosts.includes(host.hostName)),
                  )
                  .map((host) => (
                    <option key={host.hostName} value={host.hostName}>
                      {host.hostName}
                    </option>
                  ))}
              </Form.Select>
            </Col>
            {allowCountChange ? (
              <Col md={2} className="d-flex gap-2">
                {index === additionalHosts.length - 1 &&
                additionalHosts.length < availableAdditionalHostCount ? (
                  <Button
                    size="sm"
                    title={`Add ${componentLabel}`}
                    onClick={() => setAdditionalHosts([...additionalHosts, ""])}
                  >
                    <FontAwesomeIcon icon={faPlus} />
                  </Button>
                ) : null}
                {additionalHosts.length > 1 ? (
                  <Button
                    size="sm"
                    variant="outline-secondary"
                    title={`Remove ${componentLabel}`}
                    onClick={() =>
                      setAdditionalHosts(
                        additionalHosts.filter((_, itemIndex) => itemIndex !== index),
                      )
                    }
                  >
                    <FontAwesomeIcon icon={faMinus} />
                  </Button>
                ) : null}
              </Col>
            ) : null}
          </Row>
        ))}
      </Stack>
    </>
  );
}

export default HostAssignment;
