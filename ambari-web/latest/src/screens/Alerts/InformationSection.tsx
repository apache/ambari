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

import React, { useEffect, useState, useContext } from "react";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faPencil,
  faPowerOff,
  faUndo,
} from "@fortawesome/free-solid-svg-icons";
import Modal from "../../components/Modal.tsx";
import FormattedTimestamp from "../../components/FormattedTimestamp.tsx";
import { Link } from "react-router-dom";
import { Row, Col, Container, Form, InputGroup } from "react-bootstrap";
import { MergedAlert } from "./types";
import { AlertsApi } from "../../api/alertsApi.ts";
import ClusterApi from "../../api/clusterApi.ts";
import { ambariApi } from "../../api/config/axiosConfig.ts";
import { AppContext } from "../../store/context.tsx";
import { useParams } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";

interface AlertDefinitionResponseItem {
  AlertDefinition: {
    id: number;
    repeat_tolerance: any;
    repeat_tolerance_enabled: boolean;
    help_url?: string;
    [key: string]: any;
  };
}

const InformationSection = ({
  alertDefinition,
  onAlertDefinitionUpdate,
}: {
  alertDefinition: MergedAlert;
  onAlertDefinitionUpdate?: (updatedDefinition: Partial<MergedAlert>) => void;
}) => {
  const { clusterName, upgradeIsRunning, upgradeSuspended } =
    useContext(AppContext);
  const params = useParams<{ alertId?: string }>();
  const [showToleranceModal, setShowToleranceModal] = useState(false);
  const [toleranceValue, setToleranceValue] = useState<number>(
    alertDefinition.repeat_tolerance || 1
  );
  const [isUpdatingTolerance, setIsUpdatingTolerance] = useState(false);
  const [toleranceError, setToleranceError] = useState<string | null>(null);
  const [localRepeatTolerance, setLocalRepeatTolerance] = useState<number>(
    alertDefinition.repeat_tolerance || 1
  );
  const [localRepeatToleranceEnabled, setLocalRepeatToleranceEnabled] =
    useState<boolean>(alertDefinition.repeat_tolerance_enabled || false);
  const [globalRepeatTolerance, setGlobalRepeatTolerance] =
    useState<string>("1");

  // Authorization hooks - implementing Ember.js alert authorization patterns
  const { hasAuthorization } = useAuth();

  // Check specific authorizations for alert operations
  const canToggleAlerts = hasAuthorization("SERVICE.TOGGLE_ALERTS");

  // Check if upgrade is blocking operations (running but not suspended)
  const isUpgradeBlocking = upgradeIsRunning && !upgradeSuspended;

  // Cache for global repeat tolerance to avoid repeated API calls
  const globalRepeatToleranceCache = React.useRef<{
    value: string | null;
    timestamp: number;
    clusterName: string;
  }>({
    value: null,
    timestamp: 0,
    clusterName: "",
  });

  useEffect(() => {
    console.log("InformationSection - Alert Definition changed:", {
      repeat_tolerance: alertDefinition.repeat_tolerance,
      repeat_tolerance_enabled: alertDefinition.repeat_tolerance_enabled,
      alert_definition_id: alertDefinition.alert_definition_id,
    });

    setLocalRepeatTolerance(alertDefinition.repeat_tolerance || 1);
    setLocalRepeatToleranceEnabled(
      alertDefinition.repeat_tolerance_enabled || false
    );

    // Fetch global alerts repeat tolerance setting with caching
    fetchGlobalRepeatToleranceWithCache();
  }, [alertDefinition]);

  const fetchGlobalRepeatToleranceWithCache = async () => {
    const now = Date.now();
    const CACHE_DURATION = 5 * 60 * 1000; // 5 minutes cache

    // Check if we have a valid cached value for this cluster
    if (
      globalRepeatToleranceCache.current.value !== null &&
      globalRepeatToleranceCache.current.clusterName === clusterName &&
      now - globalRepeatToleranceCache.current.timestamp < CACHE_DURATION
    ) {
      setGlobalRepeatTolerance(globalRepeatToleranceCache.current.value);
      return;
    }

    // Cache miss or expired, fetch from API
    try {
      const response = await ClusterApi.getCluster(clusterName);
      const desiredConfigs = response?.Clusters?.desired_configs;

      if (desiredConfigs && desiredConfigs["cluster-env"]) {
        const clusterEnvTag = desiredConfigs["cluster-env"].tag;
        const configUrl = `clusters/${clusterName}/configurations?type=cluster-env&tag=${clusterEnvTag}&fields=*`;

        const configResponse = await ambariApi.request({
          url: configUrl,
          method: "GET",
        });

        if (
          configResponse.data &&
          configResponse.data.items &&
          configResponse.data.items.length > 0
        ) {
          const clusterEnvConfig = configResponse.data.items[0];
          const globalTolerance =
            clusterEnvConfig.properties?.alerts_repeat_tolerance || "1";

          // Update cache
          globalRepeatToleranceCache.current = {
            value: globalTolerance,
            timestamp: now,
            clusterName: clusterName,
          };

          setGlobalRepeatTolerance(globalTolerance);
          return;
        }
      }

      // Fallback to default and cache it
      globalRepeatToleranceCache.current = {
        value: "1",
        timestamp: now,
        clusterName: clusterName,
      };
      setGlobalRepeatTolerance("1");
    } catch (error) {
      // Cache the default value to avoid repeated failed API calls
      globalRepeatToleranceCache.current = {
        value: "1",
        timestamp: now,
        clusterName: clusterName,
      };
      setGlobalRepeatTolerance("1");
    }
  };

  const refreshAlertDefinition = async () => {
    try {
      const alertDefinitionId = params.alertId;
      if (alertDefinitionId) {
        const alertDefinitionResponse = await AlertsApi.getAlertDefinition(
          clusterName,
          "AlertDefinition/component_name,AlertDefinition/description,AlertDefinition/enabled,AlertDefinition/repeat_tolerance,AlertDefinition/repeat_tolerance_enabled,AlertDefinition/id,AlertDefinition/ignore_host,AlertDefinition/interval,AlertDefinition/label,AlertDefinition/name,AlertDefinition/scope,AlertDefinition/service_name,AlertDefinition/source,AlertDefinition/help_url",
          Date.now()
        );

        const alertDefinitionDetails = alertDefinitionResponse.items.find(
          (item: AlertDefinitionResponseItem) =>
            item.AlertDefinition.id === parseInt(alertDefinitionId)
        );

        if (alertDefinitionDetails) {
          // Update local state
          setLocalRepeatTolerance(
            alertDefinitionDetails.AlertDefinition.repeat_tolerance
          );
          setLocalRepeatToleranceEnabled(
            alertDefinitionDetails.AlertDefinition.repeat_tolerance_enabled
          );

          // Update parent component data
          alertDefinition.repeat_tolerance =
            alertDefinitionDetails.AlertDefinition.repeat_tolerance;
          alertDefinition.repeat_tolerance_enabled =
            alertDefinitionDetails.AlertDefinition.repeat_tolerance_enabled;
        }
      }
    } catch (error) {
      console.error("Error refreshing alert definition:", error);
    }
  };

  const handleEditRepeatTolerance = () => {
    // Initialize modal with the correct value based on Ember logic:
    // If repeat_tolerance_enabled is true, use repeat_tolerance, otherwise use global default
    const initialValue = localRepeatToleranceEnabled
      ? localRepeatTolerance
      : parseInt(globalRepeatTolerance);
    setToleranceValue(initialValue || 1);
    setToleranceError(null);
    setShowToleranceModal(true);
  };

  const handleCancelEditTolerance = () => {
    setShowToleranceModal(false);
    setToleranceError(null);
  };

  const validateToleranceInput = (value: string | number): boolean => {
    const stringValue = value.toString();
    if (stringValue === "DEBUG") {
      setToleranceError(null);
      return true;
    }

    const intValue = parseInt(stringValue, 10);
    if (isNaN(intValue) || intValue < 1 || intValue > 99) {
      setToleranceError("Input should be an integer between 1 and 99.");
      return false;
    }

    setToleranceError(null);
    return true;
  };

  const handleToleranceInputChange = (value: string) => {
    setToleranceValue(value as any);
    validateToleranceInput(value);
  };

  const handleSaveRepeatTolerance = async () => {
    if (!validateToleranceInput(toleranceValue)) {
      return;
    }

    setIsUpdatingTolerance(true);
    setToleranceError(null);

    try {
      // Match Ember logic: enable repeat tolerance if value is different from global default
      const enableRepeatTolerance =
        toleranceValue.toString() !== globalRepeatTolerance;

      await AlertsApi.updateAlertDefinition(
        clusterName,
        alertDefinition.alert_definition_id,
        {
          "AlertDefinition/repeat_tolerance": toleranceValue.toString(),
          "AlertDefinition/repeat_tolerance_enabled": enableRepeatTolerance,
        }
      );

      // Update local state to reflect changes immediately
      setLocalRepeatTolerance(toleranceValue);
      setLocalRepeatToleranceEnabled(enableRepeatTolerance);

      // Update parent component data
      alertDefinition.repeat_tolerance = toleranceValue;
      alertDefinition.repeat_tolerance_enabled = enableRepeatTolerance;

      if (onAlertDefinitionUpdate) {
        onAlertDefinitionUpdate({
          repeat_tolerance: toleranceValue,
          repeat_tolerance_enabled: enableRepeatTolerance,
        });
      }

      // Refresh the alert definition data
      await refreshAlertDefinition();

      setShowToleranceModal(false);
    } catch (error) {
      console.error("Error updating repeat tolerance:", error);
      setToleranceError("Failed to update repeat tolerance. Please try again.");
    } finally {
      setIsUpdatingTolerance(false);
    }
  };

  const AlertDefinitionState = ({ alert }: { alert: MergedAlert }) => {
    const { clusterName } = useContext(AppContext);
    const [isEnabled, setIsEnabled] = useState(alert.enabled);
    const [showModal, setShowModal] = useState(false);
    const [isUpdating, setIsUpdating] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleToggleState = () => {
      setError(null);
      setShowModal(true);
    };

    const handleConfirmToggle = async () => {
      setIsUpdating(true);
      try {
        await AlertsApi.updateAlertDefinitionState(
          clusterName,
          alert.alert_definition_id,
          !isEnabled
        );

        // Update local state
        setIsEnabled(!isEnabled);

        // Update the parent component's data
        alertDefinition.enabled = !isEnabled;
        alertDefinition.state = !isEnabled ? "Enabled" : "Disabled";

        // Notify parent component about the change
        if (onAlertDefinitionUpdate) {
          onAlertDefinitionUpdate({
            enabled: !isEnabled,
            state: !isEnabled ? "Enabled" : "Disabled"
          });
        }

        setShowModal(false);
        setError(null);
      } catch (error) {
        console.error("Error updating alert definition state:", error);
        setError("Failed to update alert state. Please try again.");
      } finally {
        setIsUpdating(false);
      }
    };

    const handleCancelToggle = () => {
      setShowModal(false);
      setError(null);
    };

    return (
      <>
        {/* Only show toggle functionality if user has SERVICE.TOGGLE_ALERTS permission and upgrade is not blocking */}
        {canToggleAlerts && !isUpgradeBlocking ? (
          <div className="custom-link" onClick={handleToggleState}>
            <FontAwesomeIcon
              className={`ml-1 ${isUpdating ? "text-muted" : ""}`}
              icon={faPowerOff}
            />
            {isEnabled ? " Enabled" : " Disabled"}
          </div>
        ) : (
          <div>
            <FontAwesomeIcon className="ml-1" icon={faPowerOff} />
            {isEnabled ? " Enabled" : " Disabled"}
          </div>
        )}
        {error && <div className="text-danger mt-1">{error}</div>}
        <Modal
          isOpen={showModal}
          onClose={handleCancelToggle}
          modalTitle="Confirmation"
          modalBody={
            <>
              <p>
                {isEnabled
                  ? "You are about to Disable this alert definition"
                  : "You are about to Enable this alert definition"}
              </p>
              {error && <div className="text-danger">{error}</div>}
            </>
          }
          successCallback={handleConfirmToggle}
          options={{
            okButtonText: isUpdating
              ? "Processing..."
              : isEnabled
              ? "Confirm Disable"
              : "Confirm Enable",
            cancelButtonText: "Cancel",
            okButtonDisabled: isUpdating,
          }}
        />
      </>
    );
  };

  return (
    <Container className="col right-column">
      <div className="panel panel-default bg-white py-2 border">
        <div className="panel-heading">
          <h4 className="panel-title pt-3 py-1 px-3">Alert Info</h4>
        </div>
        <hr />
        <div className="properties-list background-text panel-body px-3">
          <Row className="p-1 pt-2">
            <Col md={5} className="property-name">
              State:
            </Col>
            <Col md={7}>
              <AlertDefinitionState alert={alertDefinition} />
            </Col>
          </Row>
          <Row className="p-1 pt-2">
            <Col md={5}>Service:</Col>
            <Col md={7} className="font-weight-bolder">
              <span>{alertDefinition.serviceDisplayName}</span>
            </Col>
          </Row>
          {alertDefinition.component_name && (
            <Row className="p-1 pt-2">
              <Col md={5} className="property-name">
                Component:
              </Col>
              <Col md={7}>{alertDefinition.component_name}</Col>
            </Row>
          )}
          <Row className="p-1 pt-2">
            <Col md={5} className="property-name">
              Type:
            </Col>
            <Col md={7}>
              <span
                className={`type-icon ${alertDefinition.typeIconClass || ""}`}
              ></span>
              {alertDefinition.source_type}
            </Col>
          </Row>
          <Row className="p-1 pt-2">
            <Col md={5} className="property-name">
              Groups:
            </Col>
            <Col md={7}>
              <span>{alertDefinition.groups}</span>
            </Col>
          </Row>
          <Row className="p-1 pt-2">
            <Col md={5} className="property-name">
              Last Changed:
            </Col>
            <Col md={7}>
              <FormattedTimestamp
                timestamp={alertDefinition.last_status_changed || null}
              />
            </Col>
          </Row>
          {!alertDefinition.source_type ||
          alertDefinition.source_type !== "AGGREGATE" ? (
            <Row className="p-1 pt-2">
              <Col md={5} className="property-name">
                Check Count:
              </Col>
              <Col md={7}>
                {localRepeatToleranceEnabled ? (
                  <span>{localRepeatTolerance}</span>
                ) : (
                  <span>
                    {globalRepeatTolerance}
                    {" (default)"}
                  </span>
                )}
                {/* Only show edit pencil icon if user has SERVICE.TOGGLE_ALERTS permission and upgrade is not blocking */}
                {canToggleAlerts && !isUpgradeBlocking && (
                  <Link
                    to="#"
                    className="custom-link"
                    onClick={(e) => {
                      e.preventDefault();
                      handleEditRepeatTolerance();
                    }}
                  >
                    <FontAwesomeIcon
                      size={"lg"}
                      className="ms-2"
                      icon={faPencil}
                    />
                  </Link>
                )}
              </Col>
            </Row>
          ) : null}
           {alertDefinition.help_url && 
               alertDefinition.help_url !== "null" && 
               alertDefinition.help_url.trim() !== "" ?<Row className="p-1 mb-3 pt-2">
            <Col md={5} className="property-name">
              Help URL:
            </Col>
            <Col md={7}>
              {alertDefinition.help_url && 
               alertDefinition.help_url !== "null" && 
               alertDefinition.help_url.trim() !== "" ? (
                <Link to={alertDefinition.help_url} className="custom-link">
                  {alertDefinition.help_url}
                </Link>
              ) :null}
            </Col>
          </Row>:null}
        </div>
      </div>

      {/* Repeat Tolerance Modal */}
      <Modal
        isOpen={showToleranceModal}
        onClose={handleCancelEditTolerance}
        modalTitle="Edit Alert Check Count"
        modalBody={
          <div>
            <h3>Alert Check Counts</h3>
            <div className="fs-12">
              Set the number of alert checks to perform before dispatching a
              notification. If during an alert check a state change occurs,
              Ambari will attempt to check this number of times before
              dispatching a notification. Increase this number if your
              environment experiences transient issues resulting in false
              alerts.
            </div>

            <Form.Group className="my-3">
              <Form.Label>Check Count</Form.Label>
              <div className="d-flex align-items-center">
                <InputGroup className="w-25">
                  <Form.Control
                    type="text"
                    value={toleranceValue}
                    onChange={(e) => handleToleranceInputChange(e.target.value)}
                    placeholder="Enter check count (1-99 or DEBUG)"
                    isInvalid={!!toleranceError}
                    disabled={isUpdatingTolerance}
                  />
                </InputGroup>
                <FontAwesomeIcon
                  icon={faUndo}
                  className="text-warning ms-2 cursor-pointer"
                  onClick={() => {
                    setToleranceValue(parseInt(globalRepeatTolerance) || 1);
                    setToleranceError(null);
                  }}
                />
              </div>

              {toleranceError && (
                <Form.Text className="text-danger">{toleranceError}</Form.Text>
              )}
              <Form.Text className="text-muted">
                The default value is {globalRepeatTolerance}, which means alerts
                are triggered
                {globalRepeatTolerance === "1"
                  ? " immediately"
                  : ` after ${globalRepeatTolerance} consecutive checks`}
                .
              </Form.Text>
            </Form.Group>
          </div>
        }
        successCallback={handleSaveRepeatTolerance}
        options={{
          okButtonText: isUpdatingTolerance ? "Saving..." : "Save",
          cancelButtonText: "Cancel",
          okButtonDisabled: isUpdatingTolerance || !!toleranceError,
        }}
      />
    </Container>
  );
};

export default InformationSection;
