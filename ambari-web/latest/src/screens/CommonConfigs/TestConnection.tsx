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

import { Button, Spinner, Stack } from "react-bootstrap";
import { ConfigPropertiesType } from "./types";
import KerberosApi from "../../api/kerberosApi";
import { useContext, useEffect, useState } from "react";
import { AppContext } from "../../store/context";
import { get } from "lodash";
import ClusterApi from "../../api/clusterApi";
import { RequestApi } from "../../api/requestApi";
import usePolling from "../../hooks/usePolling";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faCircleCheck,
  faCircleXmark,
} from "@fortawesome/free-solid-svg-icons";
import Modal from "../../components/Modal";
import {
  collectSensitiveConfigValues,
  connectionSourceHosts,
  databaseConnectionParameters,
  resolveRequiredPropertyValues,
  sanitizeConnectionDiagnostics,
} from "./testConnectionUtils";

enum TestKdcResponses {
  SUCCESS = "REACHABLE",
  FAILURE = "UNREACHABLE",
}

interface TestConnectionProps {
  buttonLabel: string;
  serviceName: string;
  configProperties: ConfigPropertiesType;
  requiredProperties?: Record<string, string>;
  disabled?: boolean;
}

// Service-specific error modal titles
const getErrorModalTitle = (serviceName: string): string => {
  const serviceErrorTitles: Record<string, string> = {
    KERBEROS: "KDC Connection: Error",
    HIVE: "Hive Database Connection: Error", 
    RANGER: "Ranger Database Connection: Error",
    RANGER_KMS: "Ranger KMS Database Connection: Error",
  };
  
  return serviceErrorTitles[serviceName] || `${serviceName} Connection: Error`;
};

// Generate KDC-specific error messages
const generateKDCErrorMessage = (response: any, error: any, configProperties?: ConfigPropertiesType, serviceName?: string): any => {
  // Helper function to get KDC port from configuration
  const getKDCPort = (): string => {
    if (configProperties && serviceName) {
      // Try to get KDC port from various possible configuration locations
      const kdcPort = 
        configProperties[serviceName]?.["KDC"]?.properties?.["kdc_port"]?.value ||
        configProperties[serviceName]?.["Advanced kerberos-env"]?.properties?.["kdc_port"]?.value ||
        configProperties[serviceName]?.["kerberos-env"]?.properties?.["kdc_port"]?.value ||
        "88"; // Default Kerberos port as fallback
      return String(kdcPort);
    }
    return "88"; // Default Kerberos port
  };

  // Helper function to get KDC host from configuration
  const getKDCHost = (): string => {
    if (configProperties && serviceName) {
      const kdcHosts = configProperties[serviceName]?.["KDC"]?.properties?.["kdc_hosts"]?.value;
      if (kdcHosts) {
        // If multiple hosts, take the first one for display
        const hostList = Array.isArray(kdcHosts) ? kdcHosts : [kdcHosts];
        return hostList[0] || "Not specified";
      }
    }
    return "Not specified";
  };

  const kdcPort = getKDCPort();
  const kdcHost = getKDCHost();

  // If we have a response, try to extract meaningful error information
  if (response && response !== TestKdcResponses.SUCCESS) {
    return {
      error_log: "KDC Connection Failed",
      stderr: `Unable to connect to the specified KDC host. Please verify:\n` +
              `• KDC host is reachable and running\n` +
              `• Port ${kdcPort} (Kerberos) is open and accessible\n` +
              `• Network connectivity between Ambari server and KDC host\n` +
              `• KDC service is properly configured and started`,
      output_log: response === TestKdcResponses.FAILURE ? "Connection status: UNREACHABLE" : "KDC connection test failed",
      stdout: `KDC Host: ${kdcHost}\nKDC Port: ${kdcPort}\nStatus: ${response || 'FAILED'}`
    };
  }

  // If we have an exception error, provide error-specific messages
  if (error) {
    let errorDetails = "Unknown error occurred";
    let troubleshooting = "Please check the KDC configuration and try again.";

    // Parse common error types
    const errorMessage = error?.message || error?.toString() || "";
    
    if (errorMessage.includes("timeout") || errorMessage.includes("TIMEOUT")) {
      errorDetails = "Connection timeout while trying to reach KDC host";
      troubleshooting = `• Verify KDC host is reachable\n` +
                       `• Check if port ${kdcPort} is open\n` +
                       `• Ensure no firewall is blocking the connection`;
    } else if (errorMessage.includes("refused") || errorMessage.includes("REFUSED")) {
      errorDetails = "Connection refused by KDC host";
      troubleshooting = `• Verify KDC service is running\n` +
                       `• Check if KDC is listening on port ${kdcPort}\n` +
                       `• Ensure KDC host configuration is correct`;
    } else if (errorMessage.includes("unreachable") || errorMessage.includes("UNREACHABLE")) {
      errorDetails = "KDC host is unreachable";
      troubleshooting = `• Check network connectivity\n` +
                       `• Verify KDC hostname/IP address is correct\n` +
                       `• Ensure DNS resolution is working\n` +
                       `• Confirm port ${kdcPort} is not blocked by firewall`;
    } else if (errorMessage.includes("credentials") || errorMessage.includes("authentication")) {
      errorDetails = "Authentication failed with KDC";
      troubleshooting = `• Verify Kadmin credentials are correct\n` +
                       `• Check if the admin principal exists\n` +
                       `• Ensure the admin password is valid\n` +
                       `• Confirm KDC is accessible on port ${kdcPort}`;
    }

    return {
      error_log: "KDC Connection Error",
      stderr: `${errorDetails}\n\nTroubleshooting steps:\n${troubleshooting}`,
      output_log: "KDC connection test failed with exception",
      stdout: `KDC Host: ${kdcHost}\nKDC Port: ${kdcPort}\nError: ${errorMessage || "No additional error details available"}`
    };
  }

  // Fallback error message
  return {
    error_log: "KDC Connection Failed",
    stderr: `Unable to test KDC connection. Please verify your KDC configuration and ensure the KDC host is accessible on port ${kdcPort}.`,
    output_log: "Connection test failed",
    stdout: `KDC Host: ${kdcHost}\nKDC Port: ${kdcPort}\nStatus: Connection test failed`
  };
};

export default function TestConnection({
  buttonLabel,
  serviceName,
  configProperties,
  requiredProperties: themeRequiredProperties,
  disabled = false,
}: TestConnectionProps) {
  const [taskID, setTaskID] = useState<string | null>(null);
  const [requestId, setRequestId] = useState<string | null>(null);
  const [isConnecting, setIsConnecting] = useState(false);
  const [isConnectionSuccessful, setIsConnectionSuccessful] = useState(false);
  const [isValidated, setIsValidated] = useState(false);
  const [clickedOnce, setClickedOnce] = useState(false);
  const [errorMessage, setErrorMessage] = useState<any>(null);
  const [showErrorMessage, setShowErrorMessage] = useState(false);

  const { services, ambariProperties, clusterName } = useContext(AppContext);

  const setSafeErrorMessage = (diagnostics: Record<string, unknown>) => {
    setErrorMessage(
      sanitizeConnectionDiagnostics(
        diagnostics,
        collectSensitiveConfigValues(
          configProperties,
          serviceName,
          themeRequiredProperties,
        ),
      ),
    );
  };

  const failConnection = (error: unknown, fallbackMessage: string) => {
    const message = get(error, "response.data.message", get(error, "message", fallbackMessage));
    setTaskID(null);
    setRequestId(null);
    pausePolling();
    setIsConnecting(false);
    setIsConnectionSuccessful(false);
    setSafeErrorMessage({
      error_log: "Connection Test Failed",
      stderr: message || fallbackMessage,
      output_log: "Connection test did not complete",
      stdout: "Retry the connection test after resolving the reported error.",
    });
  };

  const getTaskStatus = async () => {
    if (requestId === null || taskID === null) {
      return;
    }

    try {
      const response = await RequestApi.getTaskStatus(requestId, taskID);
      updateTaskStatus(response);
    } catch (error) {
      failConnection(error, "Unable to read the connection test task status.");
    }
  };
  const updateTaskStatus = (response: any) => {
    const status = get(response, "Tasks.status", null);
    if (status) {
      if (status === "COMPLETED") {
        const structuredOut = get(
          response,
          "Tasks.structured_out.db_connection_check"
        );
        setTaskID(null);
        setRequestId(null);
        pausePolling();
        setIsConnecting(false);
        if (!structuredOut || Number(structuredOut.exit_code) !== 0) {
          const tasks = get(response, "Tasks", {});
          setIsConnectionSuccessful(false);
          setSafeErrorMessage({
            ...tasks,
            stderr:
              tasks.stderr ||
              structuredOut?.message ||
              "The completed task did not report a successful database check.",
          });
        } else {
          setIsConnectionSuccessful(true);
        }
      } else if (status === "FAILED" || status === "ABORTED" || status === "TIMEDOUT") {
        setTaskID(null);
        setRequestId(null);
        pausePolling();
        setIsConnecting(false);
        setIsConnectionSuccessful(false);
        setSafeErrorMessage(get(response, "Tasks", {}));
      }
    } else {
      failConnection(response, "The connection test task has no status.");
    }
  };
  const { pausePolling, resumePolling } = usePolling(getTaskStatus, 1000);

  useEffect(() => {
    pausePolling();
  }, [pausePolling]);

  useEffect(() => {
    if (taskID && requestId) {
      resumePolling();
    }
  }, [taskID, requestId, resumePolling]);

  const installedServicesInCluster = services.map(
    (service) => service.ServiceInfo.service_name
  );

  const isDBACreds = (service: string): boolean => {
    if (service === "RANGER") {
      const createDbUser = configProperties[service]?.["ranger-env"]?.properties?.["create_db_dbuser"]?.value;
      return String(createDbUser) === "true" || createDbUser === true;
    } else if (service === "RANGER_KMS") {
      const createDbUser = configProperties[service]?.["kms-env"]?.properties?.["create_db_user"]?.value;
      return String(createDbUser) === "true" || createDbUser === true;
    }
    return false;
  };

  const rangerUseDBA = isDBACreds("RANGER");
  const rangerKmsUseDBA = isDBACreds("RANGER_KMS");

  const legacyRequiredProperties: Record<
    string,
    Record<string, string>
  > = {
    HIVE: {
      "jdbc.driver.class":
        "hive-site/javax.jdo.option.ConnectionDriverName",
      "jdbc.driver.url": "hive-site/javax.jdo.option.ConnectionURL",
      "db.connection.source.host":
        "HIVE_METASTORE/hive_metastore_hosts",
      "db.type": "hive-env/hive_database_type",
      "db.connection.user":
        "hive-site/javax.jdo.option.ConnectionUserName",
      "db.connection.password":
        "hive-site/javax.jdo.option.ConnectionPassword",
    },
    RANGER: {
      "jdbc.driver.class": "ranger-admin-site/ranger.jpa.jdbc.driver",
      "jdbc.driver.url": "ranger-admin-site/ranger.jpa.jdbc.url",
      "db.connection.source.host": "RANGER_ADMIN/ranger_admin_hosts",
      "db.type": "admin-properties/DB_FLAVOR",
      "db.connection.user": `admin-properties/${
        rangerUseDBA ? "db_root_user" : "db_user"
      }`,
      "db.connection.password": `admin-properties/${
        rangerUseDBA ? "db_root_password" : "db_password"
      }`,
    },
    RANGER_KMS: {
      "jdbc.driver.class": "dbks-site/ranger.ks.jpa.jdbc.driver",
      "jdbc.driver.url": "dbks-site/ranger.ks.jpa.jdbc.url",
      "db.connection.source.host":
        "RANGER_KMS_SERVER/ranger_kms_server_hosts",
      "db.type": "kms-properties/DB_FLAVOR",
      "db.connection.user": `kms-properties/${
        rangerKmsUseDBA ? "db_root_user" : "db_user"
      }`,
      "db.connection.password": `kms-properties/${
        rangerKmsUseDBA ? "db_root_password" : "db_password"
      }`,
    },
  };

  const getMasterHosts = (service: string): unknown => {
    const serviceMasterMap: Record<string, string> = {
      OOZIE: "oozie_server_hosts",
      HDFS: "hadoop_host",
      HIVE: "HIVE_METASTORE:hive_metastore_hosts",
      KERBEROS: "kdc_hosts",
      RANGER: "RANGER_ADMIN:ranger_admin_hosts",
      RANGER_KMS: "RANGER_KMS_SERVER:ranger_kms_server_hosts",
    };

    const mapValue = serviceMasterMap[service];
    if (!mapValue?.includes(":")) return undefined;
    const [section, property] = mapValue.split(":");
    return configProperties[service]?.[section]?.properties?.[property]?.value;
  };

  const selectedRequiredProperties =
    themeRequiredProperties && Object.keys(themeRequiredProperties).length
      ? themeRequiredProperties
      : legacyRequiredProperties[serviceName] ?? {};
  const resolvedRequiredProperties = resolveRequiredPropertyValues(
    configProperties,
    serviceName,
    selectedRequiredProperties,
    getMasterHosts(serviceName),
  );

  useEffect(() => {
    if (serviceName === "KERBEROS") {
      const property =
        configProperties.KERBEROS?.KDC?.properties?.kdc_hosts;
      setIsValidated(
        Boolean(property && !property.errorMessage && !property.hasError),
      );
    } else {
      setIsValidated(resolvedRequiredProperties.valid);
    }
  }, [configProperties, resolvedRequiredProperties.valid, serviceName]);

  const createCustomAction = async () => {
    setIsConnecting(true);
    const isServiceInstalled = installedServicesInCluster.includes(serviceName);

    const params = {
      action: "check_host",
      context: "Check host",
      parameters: databaseConnectionParameters(
        resolvedRequiredProperties.values,
        ambariProperties,
        window.location.hostname,
      ),
    };

    const payload = {
      RequestInfo: {
        ...params,
      },
      "Requests/resource_filters": [
        { hosts: connectionSourceHosts(resolvedRequiredProperties.values) },
      ],
    };

    if (isServiceInstalled) {
      try {
        const response = await ClusterApi.createClusterCustomAction(
          clusterName,
          payload
        );
        if (response) {
          if (response.Requests.status === "Accepted") {
            await onCreateActionSuccess(response);
          } else {
            failConnection(response, "The connection test request was not accepted.");
          }
        } else {
          failConnection(response, "The connection test request returned no response.");
        }
      } catch (error) {
        failConnection(error, "Unable to create the connection test request.");
      }
    } else {
      try {
        const response = await ClusterApi.createCustomAction(payload);
        if (response) {
          if (response.Requests.status === "Accepted") {
            await onCreateActionSuccess(response);
          } else {
            failConnection(response, "The connection test request was not accepted.");
          }
        } else {
          failConnection(response, "The connection test request returned no response.");
        }
      } catch (error) {
        failConnection(error, "Unable to create the connection test request.");
      }
    }
  };

  const onCreateActionSuccess = async (response: any) => {
    const requestId = response.Requests.id;
    if (!requestId) {
      failConnection(response, "The connection test response did not include a request ID.");
      return;
    }

    setRequestId(requestId);
    try {
      const requestResponse = await RequestApi.getTaskId(requestId);
      const taskIdFromResponse = get(requestResponse, "items.0.Tasks.id", null);

      if (!taskIdFromResponse) {
        failConnection(
          requestResponse,
          "The connection test request did not include a task ID."
        );
        return;
      }
      setTaskID(taskIdFromResponse);
    } catch (error) {
      failConnection(error, "Unable to load the connection test task.");
    }
  };

  const testKDCConnection = async () => {
    setIsConnecting(true);
    try {
      const response = await KerberosApi.testKdcConnection(
        configProperties[serviceName]["KDC"].properties["kdc_hosts"].value
      );
      if (response === TestKdcResponses.SUCCESS) {
        setIsConnectionSuccessful(true);
      } else {
        // Use enhanced KDC error message generation
        const enhancedErrorMessage = generateKDCErrorMessage(response, null, configProperties, serviceName);
        setSafeErrorMessage(enhancedErrorMessage);
        setIsConnectionSuccessful(false);
      }
    } catch (error) {
      // Use enhanced KDC error message generation for exceptions
      const enhancedErrorMessage = generateKDCErrorMessage(null, error, configProperties, serviceName);
      setSafeErrorMessage(enhancedErrorMessage);
      setIsConnectionSuccessful(false);
    } finally {
      setIsConnecting(false);
    }
  };

  const handleButtonClick = () => {
    setErrorMessage(null);
    setIsConnectionSuccessful(false);
    setShowErrorMessage(false);
    if (serviceName === "KERBEROS") {
      testKDCConnection();
    } else {
      createCustomAction();
    }
    setClickedOnce(true);
  };

  return (
    <>
      <Modal
        isOpen={showErrorMessage}
        onClose={() => {
          setShowErrorMessage(false);
        }}
        modalTitle={getErrorModalTitle(serviceName)}
        options={{ cancelableViaBtn: false, okButtonText: "Close" }}
        successCallback={() => setShowErrorMessage(false)}
        modalBody={
          <>
            <div className="task-logs">
              <Stack direction="vertical" className="mt-2">
                <small className="text-muted">
                  stderr:{errorMessage?.error_log}
                </small>
                <pre className="mt-2">{errorMessage?.stderr}</pre>
              </Stack>
              <Stack direction="vertical" className="mt-2">
                <small className="text-muted">
                  stdout:{errorMessage?.output_log}
                </small>
                <pre className="mt-2">{errorMessage?.stdout}</pre>
              </Stack>
            </div>
          </>
        }
      />
      <Stack direction="horizontal" gap={3} className="align-items-center">
        <Button
          onClick={handleButtonClick}
          disabled={disabled || !isValidated || isConnecting}
        >
          {buttonLabel}
        </Button>
        {isConnecting && (
          <div className="d-flex align-items-center">
            <p className="mb-0">Testing Connection</p>
            <Spinner animation="border" size="sm" className="ms-2" />
          </div>
        )}
        {isConnectionSuccessful && !isConnecting && (
          <>
            <p className="mb-0 text-success">Connection OK</p>
            <FontAwesomeIcon icon={faCircleCheck} className="text-success" />
          </>
        )}
        {isConnectionSuccessful === false && !isConnecting && clickedOnce && (
          <>
            <p
              className="mb-0 text-danger cursor-pointer"
              onClick={() => {
                setShowErrorMessage(true);
              }}
            >
              Connection Failed
            </p>
            <FontAwesomeIcon
              icon={faCircleXmark}
              onClick={() => {
                setShowErrorMessage(true);
              }}
              className="text-danger cursor-pointer"
            />
          </>
        )}
      </Stack>
    </>
  );
}
