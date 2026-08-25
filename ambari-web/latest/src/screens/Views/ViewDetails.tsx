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

import { useEffect } from "react";
import { Alert, Button } from "react-bootstrap";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import ViewIframe from "../../components/ViewIframe/ViewIframe";
import Spinner from "../../components/Spinner";
import {
  findRegularViewInstance,
  findShortViewInstance,
  parseViewPath,
} from "../../Utils/viewUtils";
import { useViewInstances } from "./ViewInstancesContext";

export default function ViewDetails() {
  const {
    "*": wildcardPath = "",
    instanceName = "",
    shortName = "",
    viewName = "",
    viewVersion = "",
  } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { error, instances, isLoading, reload } = useViewInstances();
  const selectedInstance = shortName
    ? findShortViewInstance(instances, viewName, shortName)
    : findRegularViewInstance(instances, viewName, viewVersion, instanceName);

  useEffect(() => {
    document.body.classList.add("contrib-view", "contribview");
    return () => document.body.classList.remove("contrib-view", "contribview");
  }, []);

  if (isLoading) {
    return <div className="p-5"><Spinner /></div>;
  }

  if (error) {
    return (
      <Alert variant="danger" className="m-4">
        <Alert.Heading>Unable to load this View</Alert.Heading>
        <p>{error}</p>
        <Button variant="outline-danger" onClick={() => void reload()}>Retry</Button>
      </Alert>
    );
  }

  if (!selectedInstance) {
    return (
      <Alert variant="warning" className="m-4">
        <Alert.Heading>View not available</Alert.Heading>
        <p>The requested View is hidden, unavailable, or no longer installed.</p>
        <Button variant="outline-secondary" onClick={() => navigate("/main/view", { replace: true })}>
          Return to Views
        </Button>
      </Alert>
    );
  }

  return (
    <div className="view-details-container">
      <ViewIframe
        contextPath={selectedInstance.contextPath}
        title={`${selectedInstance.label} View`}
        viewPath={parseViewPath(location.search, wildcardPath)}
      />
    </div>
  );
}
