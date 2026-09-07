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

import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { Col, Row } from "react-bootstrap";
import { Link } from "react-router-dom";
import { lowerCase, startCase } from "lodash";
import { statusIconMap } from "./constants";

interface NamespaceTopology {
  name: string;
  title?: string;
  hosts: string[];
}

interface HostComponent {
  HostRoles: {
    component_name: string;
    host_name: string;
  };
  state?: string;
  passiveState?: string;
  haStatus?: string;
}

interface ComponentGroup {
  componentName: string;
  hostComponents?: HostComponent[];
}

interface HDFSFederationTopologyProps {
  namespaces: NamespaceTopology[];
  masterComponents: ComponentGroup[];
  slaveComponents: ComponentGroup[];
}

const findHostComponent = (
  components: ComponentGroup[],
  componentName: string,
  hostName: string,
) => components
  .find((component) => component.componentName === componentName)
  ?.hostComponents?.find(
    (hostComponent) => hostComponent.HostRoles.host_name === hostName,
  );

const componentState = (component?: HostComponent) => {
  if (!component) return { label: "Unknown", icon: statusIconMap.unknown };
  if (component.passiveState && component.passiveState !== "OFF") {
    return { label: "Maintenance", icon: statusIconMap.Maintenance };
  }
  const normalizedState = lowerCase(component.state || "unknown");
  return {
    label: normalizedState === "installed" ? "Stopped" : startCase(normalizedState),
    icon: statusIconMap[normalizedState] || statusIconMap.unknown,
  };
};

function ComponentStatus({
  component,
  label,
}: {
  component?: HostComponent;
  label: string;
}) {
  const status = componentState(component);
  return (
    <div className="d-flex align-items-center gap-1 mt-1">
      {status.icon?.icon && (
        <FontAwesomeIcon
          icon={status.icon.icon}
          className={`text-${status.icon.color || "secondary"}`}
          title={`${label}: ${status.label}`}
        />
      )}
      <span className="text-uppercase fs-12">{label}</span>
      <span className="text-muted fs-12">{status.label}</span>
    </div>
  );
}

export default function HDFSFederationTopology({
  namespaces,
  masterComponents,
  slaveComponents,
}: HDFSFederationTopologyProps) {
  return (
    <div className="mb-3">
      {namespaces.map((namespace) => (
        <section key={namespace.name} className="mb-3">
          <h4 className="fs-6 mb-2">Namespace: {namespace.title || namespace.name}</h4>
          <Row className="g-3">
            {namespace.hosts.map((hostName) => {
              const nameNode = findHostComponent(
                masterComponents,
                "NAMENODE",
                hostName,
              );
              const zkfc = findHostComponent(
                slaveComponents,
                "ZKFC",
                hostName,
              );
              const nameNodeRole = nameNode?.haStatus
                ? `${nameNode.haStatus} NameNode`
                : "NameNode";
              return (
                <Col md={4} key={hostName}>
                  <Link to={`/main/hosts/${hostName}/summary`} className="custom-link fs-12">
                    {hostName}
                  </Link>
                  <ComponentStatus component={nameNode} label={nameNodeRole} />
                  <ComponentStatus component={zkfc} label="ZKFailoverController" />
                </Col>
              );
            })}
          </Row>
        </section>
      ))}
    </div>
  );
}
