/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * 'License'); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {useContext, useEffect, useRef, useState, useCallback } from "react";
import {
  Badge,
  Button,
  ButtonGroup,
  Col,
  Dropdown,
  Form,
  Row,
} from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faEdit,
  faExternalLink,
  faDashboard,
  faBolt,
  faTimes,
  faWarning,
  IconDefinition,
  faExclamationTriangle,
  faBug,
  faQuestionCircle
} from "@fortawesome/free-solid-svg-icons";
import { cloneDeep, get, set } from "lodash";
import { Link, useNavigate } from "react-router-dom";
import { StackVersion, Item, Response, ClusterCheckPopupData } from "./types";
import VersionsApi from "../../../api/versionsApi";
import toast from "react-hot-toast";
import Spinner from "../../../components/Spinner";
import Modal from "../../../components/Modal";
import usePolling from "../../../hooks/usePolling";
import Tooltip from "../../../components/Tooltip";
import Select from "react-select";
import { AppContext } from "../../../store/context";
import modalManager from "../../../store/ModalManager";
import Table from "../../../components/Table";
import { initialOptions, initialUpgradeMethods, showAlertModal, translate, getUpgradeRequestStatus, translateWithVariables } from "../../../Utils/Utility";
import ClusterApi from "../../../api/clusterApi";
import OperationsProgress from "../../../components/OperationProgress";
import Upgrade from "./Upgrade";
import { RequestApi } from "../../../api/requestApi";
import RepoModal from "../../../components/RepoModal";

export const iconMapping: { [key: string]: IconDefinition } = {
  faDashboard: faDashboard,
  faBolt: faBolt,
};

export default function Versions() {
  const [options, setOptions] = useState(initialOptions);
  const [selectedOption, setSelectedOption] = useState(options[0]);
  const [loading, setLoading] = useState(false);
  const [services, setServices] = useState<string[]>([]);
  const [originalStacks, setOriginalStacks] = useState<StackVersion[]>([]);
  const [stacks, setStacks] = useState<StackVersion[]>([]);
  const [stackVersionError, setStackVersionError] = useState<any>(null);
  const [upgradeMethods, setUpgradeMethods] = useState(initialUpgradeMethods);
  const [methodType, setMethodType] = useState("");
  const [, setCompletionStatus] = useState(false);
  const [selectedStack, setSelectedStack] = useState<StackVersion>();
  const selectedStackRef = useRef<StackVersion>(selectedStack);
  const [currentUpgradeTypes, setCurrentUpgradeTypes] = useState<string[]>([]);
  const [isOperationInProgress, setIsOperationInProgress] = useState(false);
  const [operationsState, setOperationsState] = useState<any[]>([]);
  const [slaveComponentFailures, setSlaveComponentFailures] = useState(false);
  const [serviceCheckFailures, setServiceCheckFailures] = useState(false);
  const [isRestoring, setIsRestoring] = useState(true);
  const { clusterName, setUpgradeId, upgradeState, setIsPatchUpgrade, setUpgradeVersionDisplayName, allHostNames, upgradeVersionDisplayName, upgradeId, upgradeDirection } = useContext(AppContext);
  
  const packagesPayloadRef = useRef<any>({});
  
  const [versionModal, setVersionModal] = useState(false);
  const [installPackagesModal, setInstallPackagesModal] = useState(false);
  const [hostModal, setHostModal] = useState(false);
  const [upgradeModal, setUpgradeModal] = useState(false);
  const [upgradeCheckModal, setUpgradeCheckModal] = useState(false);
  const [upgradeConfirmationModal, setUpgradeConfirmationModal] = useState(false);
  const [manageVersionModal, setManageVersionsModal] = useState(false);
  const [alertModal, setAlertModal] = useState(false);

  const hostModalContent = useRef("");
  const hostModalTitle = useRef("");
  const hostModalData = useRef<any>({
    versionStatus: "",
    versionName: "",
  });
  // Remove the ref since we'll render content directly
  const upgradeMethodsRef = useRef(initialUpgradeMethods);
  const [isProceedButtonDisabled, setIsProceedButtonDisabled] = useState(true);
  const showRerunButton = useRef<Boolean>(true);
  const showUpgradeProceedButton = useRef<boolean>(true);
  
  // Cache for pre-upgrade check results to avoid redundant API calls
  const preCheckCacheRef = useRef<Map<string, any>>(new Map());
  
  // Refs for fast switching between upgrade methods
  const methodTypeRef = useRef("");

  const navigate = useNavigate();

  const {} = usePolling(fetchServices, 6000);

  const handleSelectChange = (selected: any) => {
    const option = options.find((o) => o.key === selected.value);
    if (option) {
      setSelectedOption(option);
    }
  };

  // Optimized method type selection handler using useCallback
  const handleMethodTypeSelection = useCallback((newMethodType: string) => {
    methodTypeRef.current = newMethodType;
    setMethodType(newMethodType);
    
    // Fast button state update using ref
    const selectedMethod = upgradeMethodsRef.current.find(method => method.type === newMethodType);
    if (selectedMethod) {
      const hasRequiredFailures = selectedMethod.precheckResultsMessage.includes("Required");
      const isStillLoading = selectedMethod.isCheckRequestInProgress;
      setIsProceedButtonDisabled(hasRequiredFailures && !isStillLoading);
    }
  }, []);

  async function fetchServices() {
    try {
      if (originalStacks.length === 0) {
        setLoading(true);
      }
      const response = await VersionsApi.getAllStacks(clusterName);
      const stacks = response.items;
      const currentStack = stacks.find(
        (stack: StackVersion) => stack.ClusterStackVersions.state === "CURRENT"
      );
      if (currentStack) {
        const currentRepositoryVersion =
          currentStack.ClusterStackVersions.repository_version;

        const filteredStacks = stacks.filter((stack: StackVersion) => {
          return (
            stack.ClusterStackVersions.repository_version >=
            currentRepositoryVersion
          );
        });
        setOriginalStacks(filteredStacks);
      } else {
        setOriginalStacks(stacks)
      }

      const services = Object.keys(
        response.items[0].ClusterStackVersions.repository_summary.services
      );
      setServices(services);
      setLoading(false);
    } catch (err) {
      toast.error("Failed to fetch data");
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchServices();
    restoreOperationsState();
  }, []);

  async function restoreOperationsState() {
    try {
      setIsRestoring(true);
      const savedData = await ClusterApi.getPersistData("versionOperations");
      if (savedData) {
        let savedOperationsState;
        if (typeof savedData === 'string') {
          savedOperationsState = JSON.parse(savedData);
        } else {
          savedOperationsState = savedData;
        }
        if (savedOperationsState && Array.isArray(savedOperationsState) && savedOperationsState.length > 0) {
          setOperationsState(savedOperationsState);
          
          const hasActiveOperations = savedOperationsState.some((op: any) => 
            op.status === 'IN_PROGRESS' || op.status === 'PENDING'
          );
          if (hasActiveOperations) {
            setIsOperationInProgress(true);
          }
        }
      }
    } catch (error) {
      console.error("Failed to restore operations state:", error);
    } finally {
      setIsRestoring(false);
    }
  }

  useEffect(() => {
    if (selectedOption.key !== "ALL") {
      setStacks(
        originalStacks.filter((stack) => {
          return selectedOption.values.includes(
            stack.ClusterStackVersions.state
          );
        })
      );
    } else {
      setStacks(originalStacks);
    }

    const updatedOptions = options.map((option) => {
      let count;
      if (option.key === "ALL") {
        count = originalStacks.length;
      } else {
        count = originalStacks.reduce((acc, stack) => {
          if (option.values.includes(stack.ClusterStackVersions.state)) {
            return acc + 1;
          }
          return acc;
        }, 0);
      }

      return { ...option, count };
    });

    setOptions(updatedOptions);
  }, [originalStacks, selectedOption]);

  useEffect(() => {
    const errorStack = originalStacks
      .filter(stack => stack.ClusterStackVersions.state === 'OUT_OF_SYNC')
      .find(stack => stack.repository_versions[0].RepositoryVersions.type === 'STANDARD');
    
    if (errorStack) {
      setStackVersionError({
        title: translate('admin.stackVersions.version.errors.outOfSync.title'),
        description: translate('admin.stackVersions.version.errors.outOfSync.desc'),
        stackFullName: errorStack.repository_versions[0].RepositoryVersions.stack_name + '-' + errorStack.repository_versions[0].RepositoryVersions.repository_version
      });
    } else {
      setStackVersionError(null);
    }
  }, [originalStacks]);

  useEffect(() => {
    selectedStackRef.current = selectedStack;
    // update the stacks with selectedStack
    if (selectedStack) {
      const updatedStacks = stacks.map((stack) => {
        if (
          stack.ClusterStackVersions.id ===
          selectedStack.ClusterStackVersions.id
        ) {
          return selectedStack;
        }
        return stack;
      });
      setStacks(updatedStacks);
    }
  }, [selectedStack])

  useEffect(() => {
    upgradeMethodsRef.current = upgradeMethods;
  }, [upgradeMethods]);

  useEffect(() => {
    if (methodType && upgradeMethods.length > 0) {
      const selectedMethod = upgradeMethods.find(method => method.type === methodType);
      if (selectedMethod) {
        // Enable proceed button if method has no required failures or is still loading
        const hasRequiredFailures = selectedMethod.precheckResultsMessage.includes("Required");
        const isStillLoading = selectedMethod.isCheckRequestInProgress;
        
        // Enable button if not loading and no required failures, or if no method is selected yet
        setIsProceedButtonDisabled(hasRequiredFailures && !isStillLoading);
      } else {
        // No method selected, disable button
        setIsProceedButtonDisabled(true);
      }
    } else {
      // No method type selected, disable button
      setIsProceedButtonDisabled(true);
    }
  }, [methodType, upgradeMethods]);

  if (loading) {
    return <Spinner />;
  }

  function renderOperationProgress() {
    return (
      <OperationsProgress
        operations={ operationsState as any }
        title="install packages"
        description="install packages"
        setCompletionStatus={ async () => {
          setCompletionStatus(true);
          setIsOperationInProgress(false);
          setOperationsState([]);
          
          const selectedStackCopy = cloneDeep(selectedStack);
          if (selectedStackCopy) {
            set(selectedStackCopy, "ClusterStackVersions.state", "INSTALLED");
            setSelectedStack(selectedStackCopy);
            selectedStackRef.current = selectedStackCopy;
          }
          
          // Reset operations state to empty array
          await ClusterApi.postPersistData(
            JSON.stringify({
              versionOperations: JSON.stringify([]),
            })
          );
        }}
        errorCallback={async (errorMsg) => {
          setIsOperationInProgress(false);
          setOperationsState([]);
          
          const selectedStackCopy = cloneDeep(selectedStack);
          if (selectedStackCopy) {
            set(
              selectedStackCopy,
              "ClusterStackVersions.state",
              "INSTALL_FAILED"
            );
            setSelectedStack(selectedStackCopy);
            selectedStackRef.current = selectedStackCopy;
          }
          
          // Reset operations state to empty array
          await ClusterApi.postPersistData(
            JSON.stringify({
              versionOperations: JSON.stringify([]),
            })
          );
          
          if (!alertModal) {
            setAlertModal(true);
            showAlertModal("Packages could not be installed", errorMsg);
          }
        }}
        dispatch={async (operationsState: any) => {
          setIsOperationInProgress(true);
          setOperationsState(operationsState);
          
          // Persist the current operations state
          await ClusterApi.postPersistData(
            JSON.stringify({
              versionOperations: JSON.stringify(operationsState),
            })
          );
        }}
      />
    );
  }

  function getStackHeader(stackData: StackVersion) {
    return (
      <div className={`p-2`}>
        <p className="text-center fs-16 fw-500">
          {stackData.repository_versions[0].RepositoryVersions.display_name}
        </p>
        <div className="mt-2 text-center">
          <small className="text-muted">
            (
            {
              stackData.repository_versions[0].RepositoryVersions
                .repository_version
            }
            )
          </small>
        </div>
        {stackData.repository_versions[0].RepositoryVersions.type === "PATCH" ? (
          <div className="text-center mt-2 fs-6 text-warning">
              <FontAwesomeIcon className="me-2" icon={faBug}/>
              Patch
          </div>
        ) : (
          <div className="mt-2 fs-6">&nbsp;</div>
        )}
        <div className="mt-2 mb-2 text-center">
          <a
            href="#"
            className="text-primary"
            onClick={(e) => {
              e.preventDefault();
              setSelectedStack(stackData);
              selectedStackRef.current = stackData;
              setVersionModal(true);
            }}
          >
            Show Details
          </a>
        </div>
        <div className="text-center">
          {getButtonName(stackData) === "installing" ||
          getButtonName(stackData) === "intermediateInstalling" ? (
            isRestoring ? (
              <Spinner />
            ) :(
              <Link to={""}>
                {renderOperationProgress()}
              </Link>
            )
          ) : getButtonName(stackData) === "upgradeInProgress" ? (
            <Button
              variant="light"
              size="sm"
              className="custom-link color-white"
              onClick={() => {
                setSelectedStack(stackData);
                selectedStackRef.current = stackData;
                modalManager.show(<Upgrade upgradeId={upgradeId} />);
              }}
            >
              {translate(getUpgradeStatus(stackData).statusText || "admin.stackUpgrade.state.inProgress")}
            </Button>
          ) : getButtonName(stackData) === "current" ? (
            <Dropdown as={ButtonGroup} className="upgrade-dropdown">
              <Button
                variant="success"
                className="text-uppercase"
                disabled
                size="sm"
              >
                CURRENT
              </Button>
            </Dropdown>

          ) : getButtonName(stackData) === "upgrade" ? (
            <Dropdown as={ButtonGroup} className="upgrade-dropdown">
              <Button
                variant="success"
                className="text-uppercase"
                size="sm"
                disabled={(upgradeState !== "NOT_REQUIRED" &&
                  upgradeState !== "COMPLETED")}
                onClick={() => handleUpgradeButton(stackData)}
              >
                Upgrade
              </Button>
              <Dropdown.Toggle
                size="sm"
                split
                variant="success"
                id="dropdown"
              />
              <Dropdown.Menu className="dropdown-menu-right">
                <Dropdown.Item
                disabled={upgradeState !== "NOT_REQUIRED" &&
                  upgradeState !== "COMPLETED"}
                  onClick={() => installPackagesPayloadSet(stackData)}
                >
                  Re-install
                </Dropdown.Item>
                <Dropdown.Item
                  disabled= {upgradeState !== "NOT_REQUIRED" &&
                  upgradeState !== "COMPLETED"}
                  onClick={() => {
                    showUpgradeProceedButton.current = false;
                    selectedStackRef.current = stackData;
                    setSelectedStack(stackData);
                    if (selectedStackRef.current) {
                      handleUpgradeClick(selectedStackRef.current);
                    } else {
                      toast.error("No stack selected for upgrade.");
                    }
                  }}
                >
                  Pre-upgrade check
                </Dropdown.Item>
              </Dropdown.Menu>
            </Dropdown>
          ) : (
            <Button
              variant="success"
              size="sm"
              className="text-uppercase fw-bold"
              onClick={() => handleUpgradeButton(stackData)}
            >
              {getButtonName(stackData) === "re-install"
                ? "RE-INSTALL"
                : getButtonName(stackData).toUpperCase()}
            </Button>
          )}
        </div>
      </div>
    );
  }

  async function installPackagesPayloadSet(stackData: StackVersion) {
    
    setOperationsState([]);
    setIsOperationInProgress(false);
    
    // Clear persistent storage
    await ClusterApi.postPersistData(
      JSON.stringify({
        versionOperations: JSON.stringify([]),
      })
    );
    
    setSelectedStack(stackData);
    selectedStackRef.current = stackData;
    const payload = {
      ClusterStackVersions: {
        stack: stackData.ClusterStackVersions.stack,
        version: stackData.ClusterStackVersions.version,
        repository_version:
          stackData.repository_versions[0].RepositoryVersions
            .repository_version,
      },
    };

    packagesPayloadRef.current = payload;
    setInstallPackagesModal(true);
  }

  function handleUpgradeButton(stackData: StackVersion) {
    setSelectedStack(stackData);
    selectedStackRef.current = stackData;
    switch (getButtonName(stackData)) {
      case "current":
        return;
      case "upgrade":
        showUpgradeProceedButton.current = true;
        handleUpgradeClick(stackData);
        return;
      default:
        installPackagesPayloadSet(stackData);
    }
  }

  async function handleUpgradeClick(stack: StackVersion) {
    const response = await VersionsApi.get_supported_upgradeTypes(
      stack.ClusterStackVersions.stack,
      stack.ClusterStackVersions.version,
      stack.repository_versions[0].RepositoryVersions.repository_version
    );
    const upgradeTypes =
      response.items[0].CompatibleRepositoryVersions.upgrade_types;

    // Store upgrade types in state instead of in the stack object
    setCurrentUpgradeTypes(upgradeTypes);

    // run upgrade method checks.
    const updateMethods = upgradeMethodsRef.current.map((method) => ({
      ...method,
      isCheckRequestInProgress: true,
      isCheckComplete: false,
    }));
    upgradeMethodsRef.current = updateMethods;
    setUpgradeMethods(updateMethods);

    setUpgradeModal(true);

    // Run the pre-upgrade checks after setting the initial modal content
    runUpgradeMethodCheck(stack);
  }

  async function runPreUpgradeCheckOnly(data: any) {
    const methodIndex = upgradeMethodsRef.current.findIndex(
      (method) => method.displayName === data.type
    );
    if (methodIndex !== -1) {
      const updatedMethods = [...upgradeMethodsRef.current];
      updatedMethods[methodIndex] = {
        ...updatedMethods[methodIndex],
        isCheckComplete: false,
        isCheckRequestInProgress: true,
        action: "",
      };
      upgradeMethodsRef.current = updatedMethods;
      setUpgradeMethods(updatedMethods);
    }

    // run pre upgrade check
    const response = await VersionsApi.runPreUpgradeCheck(
      clusterName,
      data.id,
      data.type
    );

    let failTitle = translate("popup.clusterCheck.Upgrade.fail.title");
    let failAlert = translate("popup.clusterCheck.Upgrade.fail.alert");
    const bypassedFailures =
      response?.items?.filter(
        (item: Item) => item.UpgradeChecks?.status === "BYPASS"
      ).length > 0;
    if (
      response.items.filter(
        (item: Item) => item.UpgradeChecks.status === "ERROR"
      ).length === 0 &&
      bypassedFailures
    ) {
      failTitle = translate(
        "popup.clusterCheck.Upgrade.bypassed-failures.title"
      );
      failAlert = translate(
        "popup.clusterCheck.Upgrade.bypassed-failures.alert"
      );
    }
    const header = translateWithVariables("popup.clusterCheck.Upgrade.header", {
      "0": selectedStackRef.current?.repository_versions[0]?.RepositoryVersions?.display_name || "Unknown"
    });

    const warningTitle = translate(
      "popup.clusterCheck.Upgrade.warning.title"
    );
    const warningAlert = translate(
      "popup.clusterCheck.Upgrade.warning.alert"
    );
    const configsMergeWarning = response.items.find(
      (item: Item) => item.UpgradeChecks.id === "CONFIG_MERGE"
    );
    const popupData = {
      href: "",
      items: response.items.filter(
        (item: Item) => item.UpgradeChecks.id !== "CONFIG_MERGE"
      ),
    };
    const configs = getConfigsWarnings(configsMergeWarning);

    showClusterCheckPopup(
      popupData,
      {
        header: header,
        failTitle: failTitle,
        failAlert: failAlert,
        warningTitle: warningTitle,
        warningAlert: warningAlert,
        primary: translate(
          "admin.stackVersions.version.upgrade.upgradeOptions.preCheck.rerun"
        ),
        secondary: translate("common.cancel"),
        bypassedFailures: bypassedFailures,
      },
      configs,
      data.type
    );
  }

  function showClusterCheckPopup(
    data: Response,
    popup: ClusterCheckPopupData,
    configs: any[],
    methodType: string
  ) {
    const fails = data.items.filter(
        (item: Item) => item.UpgradeChecks.status === "FAIL"
      ),
      warnings = data.items.filter(
        (item: Item) => item.UpgradeChecks.status === "WARNING"
      ),
      bypass = data.items.filter(
        (item: Item) => item.UpgradeChecks.status === "BYPASS"
      ),
      configsMergeConflicts = configs?.filter(
        (config) => config.wasModified === false
      ),
      configsRecommendations = configs?.filter(
        (config) => config.wasModified === true
      );

    let upgradeCheckModalContent1 = (
      <>
        {fails.length > 0 && (
          <div>
            <h2>{popup.failTitle}</h2>
            <div className="alert alert-warning">{popup.failAlert}</div>
            <div>{mapUpgradeChecks(fails)}</div>
          </div>
        )}
        {warnings.length > 0 && (
          <div>
            <h2>{popup.warningTitle}</h2>
            <div className="alert alert-warning">{popup.warningAlert}</div>
            <div>{mapUpgradeChecks(warnings)}</div>
          </div>
        )}
        {bypass.length > 0 && (
          <div>
            <h5>{popup.failTitle}</h5>
            <p>{popup.failAlert}</p>
            <div>{mapUpgradeChecks(bypass)}</div>
          </div>
        )}
        {configsMergeConflicts?.length > 0 && (
          <div>
            <h5>Configuration Merge Conflicts</h5>
            <ul>
              {configsMergeConflicts.map((config, index) => (
                <li key={index}>
                  {config.name}: {config.currentValue} (current) vs{" "}
                  {config.recommendedValue} (recommended)
                </li>
              ))}
            </ul>
            <p>Resolve the conflicts before proceeding with the upgrade.</p>
          </div>
        )}
        {configsRecommendations?.length > 0 && (
          <div>
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h5 className="mb-0">Recommended Configuration Changes: Manual Review</h5>
              <Button 
                variant="link" 
                size="sm" 
                className="p-0"
                onClick={() => openConfigurationInNewTab(configsRecommendations)}
              >
                <FontAwesomeIcon icon={faExternalLink} className="me-1" />
                Open
              </Button>
            </div>
            <div className="alert alert-warning">
              We've detected the need to update the following properties, but cannot do so automatically 
              since they have been customized. Please review these properties manually and update the 
              properties manually where necessary.
            </div>
            {renderConfigurationTable(configsRecommendations)}
          </div>
        )}
      </>
    );

    let upgradeCheckResult1 =
      fails.length > 0 || warnings.length > 0
        ? `${fails.length > 0 ? `${fails.length} Required` : ""}` +
          `${
            warnings.length > 0
              ? (fails.length > 0 ? ", " : "") + `${warnings.length} Warning`
              : ""
          }`
        : "Passed";
    if(upgradeCheckResult1 === "Passed") {
      upgradeCheckModalContent1 = <div>{translate("admin.stackVersions.version.upgrade.upgradeOptions.preCheck.allPassed.msg")}</div>
    }
    // set the content of modal & message in the method.
    const methodIndex = upgradeMethodsRef.current.findIndex(
      (method) => method.type === methodType
    );

    if (methodIndex !== -1) {
      const updatedMethods = [...upgradeMethodsRef.current];
      updatedMethods[methodIndex] = {
        ...updatedMethods[methodIndex],
        isCheckComplete: true,
        isCheckRequestInProgress: false,
        precheckResultsMessage: upgradeCheckResult1,
        preCheckResultsModalContent: upgradeCheckModalContent1,
      };
      upgradeMethodsRef.current = updatedMethods;
      setUpgradeMethods(updatedMethods);
    }
  }

  function mapUpgradeChecks(items: Item[]) {
    return items.map((item, index) => {
      const { failed_on, reason, check } = item.UpgradeChecks || {};
      return (
        <>
          <ul>
            <li key={index}>
              <div className="text-dark mb-2">
                <FontAwesomeIcon icon={faTimes} className="text-danger" />{" "}
                {check}
              </div>
              <pre className="scrollable-container py-2">
                <pre className="border-none">Reason: {reason}</pre>
                <br />
                <span>Failed on: {Array.isArray(failed_on) ? failed_on.join(', ') : failed_on}</span>
              </pre>
            </li>
          </ul>
        </>
      );
    });
  }

  function openConfigurationInNewTab(configs: any[]) {
    // Generate HTML content for the configuration table
    const htmlContent = `
      <!DOCTYPE html>
      <html lang="en">
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Recommended Configuration Changes - Manual Review</title>
        <style>
          body {
            font-family: Arial, sans-serif;
            margin: 20px;
            background-color: #ffffff;
          }
          h1 {
            color: #333;
            margin-bottom: 20px;
          }
          table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
          }
          th, td {
            padding: 12px;
            text-align: left;
            border: 1px solid #ddd;
            vertical-align: top;
          }
          th {
            background-color: #f5f5f5;
            font-weight: 600;
          }
          .config-value {
            font-family: 'Courier New', monospace;
            font-size: 12px;
            white-space: pre-wrap;
            word-break: break-all;
            max-width: 400px;
          }
        </style>
      </head>
      <body>
        <h1>Recommended Configuration Changes: Manual Review</h1>
        
        <table>
          <thead>
            <tr>
              <th style="width: 15%;">Config Type</th>
              <th style="width: 20%;">Property Name</th>
              <th style="width: 32.5%;">Current Value</th>
              <th style="width: 32.5%;">Recommended Value</th>
            </tr>
          </thead>
          <tbody>
            ${configs.map(config => `
              <tr>
                <td>${config.type}</td>
                <td>${config.name}</td>
                <td><div class="config-value">${config.currentValue}</div></td>
                <td><div class="config-value">${config.recommendedValue}</div></td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </body>
      </html>
    `;

    const newTab = window.open('about:blank', '_blank');
    
    if (newTab) {
      newTab.document.open();
      newTab.document.write(htmlContent);
      newTab.document.close();
    } else {
      toast.error('Unable to open new tab. Please check your browser settings.');
    }
  }

  function renderConfigurationTable(configs: any[]) {
    const configTableColumns = [
      {
        accessorKey: "type",
        header: "Config Type",
        width: "10%",
        cell: (info: any) => info.getValue(),
      },
      {
        accessorKey: "name", 
        header: "Property Name",
        width: "20%",
        cell: (info: any) => info.getValue(),
      },
      {
        accessorKey: "currentValue",
        header: "Current Value",
        width: "35%",
        cell: (info: any) => (
          <div className="upgrade-recommend-config" >
            <pre className="upgrade-recommend-config-pre">
              {info.getValue()}
            </pre>
          </div>
        ),
      },
      {
        accessorKey: "recommendedValue",
        header: "Recommended Value",
        width: "35%",
        cell: (info: any) => (
          <div className="upgrade-recommend-config" >
            <pre className="upgrade-recommend-config-pre">
              {info.getValue()}
            </pre>
          </div>
        ),
      },
    ];

    return (
      <div className="configuration-table-container">
        <Table 
          columns={configTableColumns} 
          data={configs} 
          showHeader={true} 
          scrollable={true}
          striped={true}
        />
      </div>
    );
  }

  function getConfigsWarnings(configsMergeWarning: any) {
    let configs = [];
    if (
      configsMergeWarning &&
      configsMergeWarning.UpgradeChecks?.status === "WARNING"
    ) {
      const configsMergeCheckData =
        configsMergeWarning.UpgradeChecks?.failed_detail;
      if (configsMergeCheckData && Array.isArray(configsMergeCheckData)) {
        configs = configsMergeCheckData.reduce((allConfigs, item) => {
          const isDeprecated = item.new_stack_value == null;
          const willBeRemoved = item.result_value == null;

          return allConfigs.concat({
            type: item.type,
            name: item.property,
            wasModified:
              !isDeprecated &&
              !willBeRemoved &&
              item.current === item.result_value,
            currentValue: item.current,
            recommendedValue: isDeprecated
              ? "Deprecated"
              : item.new_stack_value,
            isDeprecated: isDeprecated,
            resultingValue: willBeRemoved
              ? "Will be removed"
              : item.result_value,
            willBeRemoved: willBeRemoved,
          });
        }, []);
      }
    }
    return configs;
  }

  async function runUpgradeMethodCheck(stack: StackVersion) {
    const cacheKey = `${stack.ClusterStackVersions.id}-${stack.repository_versions[0].RepositoryVersions.repository_version}`;
    
    // Check if we have cached results
    if (preCheckCacheRef.current.has(cacheKey)) {
      const cachedResults = preCheckCacheRef.current.get(cacheKey);
      upgradeMethodsRef.current = cachedResults;
      setUpgradeMethods(cachedResults);
      return;
    }

    const updateMethods = upgradeMethodsRef.current.map((method) => ({
      ...method,
      isCheckRequestInProgress: method.allowed,
      isCheckComplete: false,
    }));
    upgradeMethodsRef.current = updateMethods;
    setUpgradeMethods(updateMethods);

    // Run all pre-upgrade checks in parallel for allowed methods
    const allowedMethods = upgradeMethodsRef.current.filter(method => method.allowed);
    const checkPromises = allowedMethods.map(method => 
      runPreUpgradeCheckOnly({
        id: stack.ClusterStackVersions.id,
        label: stack.repository_versions[0].RepositoryVersions.display_name,
        type: method.type,
      }).catch(error => {
        console.error(`Pre-upgrade check failed for ${method.type}:`, error);
        // Return a default failed result
        return {
          methodType: method.type,
          error: true,
          precheckResultsMessage: "Check Failed",
        };
      })
    );

    try {
      await Promise.all(checkPromises);
      // Cache the results
      preCheckCacheRef.current.set(cacheKey, upgradeMethodsRef.current);
    } catch (error) {
      console.error("Error running upgrade method checks:", error);
    }
  }

  function installPackages() {
    if (isOperationInProgress) {
      setInstallPackagesModal(false);
      return;
    }

    const initialOperations = [
      {
        id: "1",
        label: "installing",
        skippable: false,
        context: "install packages",
        callback: async () => {
          const reqData = await RequestApi.installPackages(
            clusterName,
            packagesPayloadRef.current
          );
          return reqData;
        },
      },
    ];
    
    setOperationsState(initialOperations);
    setIsOperationInProgress(true);
    
    // Update stack state to show installation in progress
    let selectedStackCopy = cloneDeep(selectedStack)
    if (selectedStackCopy) {
      set(selectedStackCopy, "ClusterStackVersions.state", "INSTALLING");
      setSelectedStack(selectedStackCopy);
      selectedStackRef.current = selectedStackCopy;
    }

    setAlertModal(false);
    setInstallPackagesModal(false);
  }

  function getUpgradeStatus(stackData: StackVersion) {
    const stackDisplayName = stackData.repository_versions[0].RepositoryVersions.display_name;
    
    if (upgradeState !== "COMPLETED" && upgradeState !== "NOT_REQUIRED" && upgradeVersionDisplayName && stackDisplayName === upgradeVersionDisplayName) {
      return {
        isUpgradeInProgress: true,
        upgradeState: upgradeState,
        statusText: getUpgradeRequestStatus(upgradeState, upgradeDirection == "DOWNGRADE"), // false for upgrade, true for downgrade
      };
    }
    
    return {
      isUpgradeInProgress: false,
      upgradeState: null,
      statusText: null,
    };
  }

  function getButtonName(stackData: StackVersion) {
    const stackState = stackData.ClusterStackVersions.state;
    if(getUpgradeStatus(stackData).isUpgradeInProgress) {
      return "upgradeInProgress";
    }
    switch (stackState) {
      case "CURRENT":
        return "current";
      case "INSTALL_FAILED":
        return "re-install";
      case "INSTALLED":
        return "upgrade";
      case "INSTALLING":
        return "installing";
      case "INTERMEDIATE":
        return "intermediateInstalling";
      default:
        return "install_packages";
    }
  }

  function getInstallPackageModalBody() {
    const displayName =
      selectedStackRef.current?.repository_versions?.[0]?.RepositoryVersions
        ?.display_name || "N/A";
    const isPatchSupported = selectedStackRef.current?.repository_versions?.[0]?.RepositoryVersions
      ?.type === "PATCH";
    
    if(!isPatchSupported) {
      return (
        <div>
          You are about to install packages for version 
          <strong>{displayName}</strong> on all hosts.
        </div>
      );
    } else {
      return (
        <div>
          <div>You are about to install packages for version <strong>{displayName}</strong> on all hosts which contain the following servcies.</div>
          <div className="mt-2">
            <ul>
              {Object.entries(selectedStackRef.current?.ClusterStackVersions.repository_summary.services || {}).map(([serviceName, serviceInfo]: [string, any]) => (
                <li className="mt-1" key={serviceName}>{serviceName}: <Badge className="ms-2" bg="light">{serviceInfo.version}</Badge></li>
              ))}
            </ul>

          </div>
        </div>
      )
    }
  }

  function getVersionModalBody() {
    if (!selectedStackRef.current || Object.keys(selectedStackRef.current).length === 0) {
      return null;
    }

    const hostStates = selectedStackRef.current.ClusterStackVersions.host_states;
    const installed = hostStates.INSTALLED.length;
    const current = hostStates.CURRENT.length;
    const notInstalled = (selectedStackRef.current.ClusterStackVersions.state === "NOT_REQUIRED") ? allHostNames.length : hostStates.INSTALL_FAILED.length + 
                        hostStates.OUT_OF_SYNC.length + 
                        hostStates.INSTALLING.length;
    const notInstalledHosts = (selectedStackRef.current.ClusterStackVersions.state === "NOT_REQUIRED") ? allHostNames : [...hostStates.INSTALL_FAILED, ...hostStates.OUT_OF_SYNC, ...hostStates.INSTALLING];

    return (
      <div className="m-n2">
        <div className="bg-info-subtle p-2">
          <div className="d-flex justify-content-between">
            <div></div>
            <h2 className="text-dark ms-4">
              {selectedStackRef.current?.repository_versions[0]?.RepositoryVersions
                ?.display_name || "NA"}
            </h2>
            <h2 className="mx-2">
                <Tooltip message="Click to Edit Repositories" placement="top">
                  <FontAwesomeIcon
                    className="fs-16 text-info"
                    onClick={() =>
                      modalManager.show(
                        <RepoModal
                          selectedStack={selectedStackRef.current}
                          isOpen
                          onClose={() => {
                            modalManager.hide();
                          }}
                        />
                      )
                    }
                    icon={faEdit}
                  />
                </Tooltip>
            </h2>
          </div>
          <div className="mt-2 text-center mb-2">
            <small className="text-muted">
              (
              {selectedStackRef.current?.repository_versions?.[0]?.RepositoryVersions
                ?.repository_version || "NA"}
              )
            </small>
          </div>
            {selectedStackRef.current.repository_versions[0].RepositoryVersions.type === "PATCH" ? (
            <div className="text-center mt-2 fs-6 text-warning">
                <FontAwesomeIcon className="me-2" icon={faBug}/>
                Patch
            </div>
          ) : (
            <div className="mt-2 fs-6">&nbsp;</div>
          )}
          <div className="text-center mt-2">
            {getButtonName(selectedStackRef.current) === "installing" ||
            getButtonName(selectedStackRef.current) === "intermediateInstalling" ? (
              isRestoring ? (
                <Spinner />
              ) :(
                <Link to={""}>
                  {renderOperationProgress()}
                </Link>
              )
            ) : getButtonName(selectedStackRef.current) === "upgradeInProgress" ? (
            <Button
              variant="light"
              size="sm"
              className="custom-link color-white"
              onClick={() => {
                modalManager.show(<Upgrade upgradeId={upgradeId} />);
              }}
            >
              {translate(getUpgradeStatus(selectedStackRef.current).statusText || "admin.stackUpgrade.state.inProgress")}
            </Button>  
            ) : getButtonName(selectedStackRef.current) === "upgrade" ? (
              <Dropdown as={ButtonGroup} className="upgrade-dropdown popup">
                <Button
                  variant="success"
                  size="sm"
                  className="text-uppercase"
                  disabled={!selectedStackRef.current || (upgradeState !== "NOT_REQUIRED" &&
                  upgradeState !== "COMPLETED")}
                  onClick={() => {
                    if (selectedStackRef.current) {
                      showUpgradeProceedButton.current = true;
                      handleUpgradeClick(selectedStackRef.current);
                    }
                  }}
                >
                  Upgrade
                </Button>
                <Dropdown.Toggle
                  split
                  size="sm"
                  variant="success"
                  disabled={!selectedStackRef.current}
                  id="dropdown-split-basic"
                />
                <Dropdown.Menu className="dropdown-menu-right">
                  <Dropdown.Item
                    disabled={!selectedStackRef.current || (upgradeState !== "NOT_REQUIRED" &&
                    upgradeState !== "COMPLETED")}
                    onClick={() => {
                      if (selectedStackRef.current) {
                        installPackagesPayloadSet(selectedStackRef.current);
                      }
                    }}
                  >
                    Re-install
                  </Dropdown.Item>
                  <Dropdown.Item
                    disabled={!selectedStackRef.current || (upgradeState !== "NOT_REQUIRED" &&
                    upgradeState !== "COMPLETED")}
                    onClick={() => {
                      showUpgradeProceedButton.current = false;
                      if (selectedStackRef.current) {
                        handleUpgradeClick(selectedStackRef.current);
                      } else {
                        toast.error("No stack selected for upgrade.");
                      }
                    }}
                  >
                    Pre-upgrade check
                  </Dropdown.Item>
                </Dropdown.Menu>
              </Dropdown>
            ) : (
              <Button
                className="text-uppercase"
                variant="success"
                size="sm"
                disabled={!selectedStackRef.current}
                onClick={() => {
                  if (selectedStackRef.current) {
                    handleUpgradeButton(selectedStackRef.current);
                  }
                }}
              >
                {getButtonName(selectedStackRef.current)}
              </Button>
            )}
          </div>
        </div>
        <div className="pb-3">
          <div className="text-center mt-3">Hosts</div>
          <div className="d-flex justify-content-around mt-2">
            <div>
              <Button
                variant="link"
                onClick={() =>
                  handleHostClick(
                    "notInstalled",
                    notInstalledHosts
                  )
                }
                disabled={notInstalled === 0}
              >
                {notInstalled}
              </Button>
              <div>not installed</div>
            </div>
            <div>
              <Button
                variant="link"
                onClick={() =>
                  handleHostClick("installed", hostStates.INSTALLED)
                }
                disabled={installed === 0}
              >
                {installed}
              </Button>
              <div>installed</div>
            </div>
            <div>
              <Button
                variant="link"
                onClick={() => handleHostClick("current", hostStates.CURRENT)}
                disabled={current === 0}
              >
                {current}
              </Button>
              <div>current</div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  function handleHostClick(status: string, hosts: string[]) {
    const versionStatus =
      status === "current"
        ? "Current"
        : status === "installed"
        ? "Installed"
        : "Not installed";
    const versionName =
      selectedStackRef.current?.repository_versions[0]?.RepositoryVersions?.display_name ||
      "N/A";
    const hostList = hosts.join("\n\n");

    hostModalContent.current = `${versionName} is ${versionStatus.toLowerCase() === "current" ? "applied": versionStatus.toLowerCase()} on ${
      hosts.length
    } hosts:\n\n\n${hostList}`;
    hostModalTitle.current = `Version Status: ${versionStatus}`;
    hostModalData.current = {
      versionStatus: versionStatus === "Not installed" ? "NOT_INSTALLED" : versionStatus.toUpperCase(),
      versionName: versionName,
    };
    setHostModal(true);
  }

  function getPreCheckResultsModalContent() {
    const method = upgradeMethodsRef.current.find(
      (method) => method.type === methodType
    );
    return get(method, "preCheckResultsModalContent", null);
  }

  function getPreCheckModalTitle() {
    return `Upgrade to ${selectedStackRef.current?.repository_versions[0]?.RepositoryVersions?.display_name || "Unknown"}`;
  }

  function getUpgradConfirmationModalBody() {
    const stackVersion =
      selectedStackRef.current?.repository_versions[0]?.RepositoryVersions?.display_name || "Unknown";
    if(methodType === "ROLLING") {
      return (<div>{translateWithVariables("admin.stackVersions.version.upgrade.upgradeOptions.RU.confirm.msg", {"0": stackVersion})}</div>)
    } else {
      return (<div>{translateWithVariables("admin.stackVersions.version.upgrade.upgradeOptions.EU.confirm.msg", {"0": stackVersion})}</div>)
    }
  }

  // Function to generate upgrade modal content
  function getUpgradeModalContent() {
    if (!selectedStackRef.current) return null;
    
    return (
      <div className="upgrade-modal">
        <div>
          You are about to perform an upgrade to{" "}
          <strong>
            {selectedStackRef.current?.repository_versions[0]?.RepositoryVersions?.display_name || "Unknown"}
          </strong>
        </div>
        <div className="pt-1">Choose the upgrade method:</div>

        <div className="upgrade-options-container">
          {upgradeMethods
            .filter(
              (method) =>
                method.allowed && currentUpgradeTypes.includes(method.type)
            )
            .map((method) => (
              <div
                key={method.type}
                className={`upgrade-method ${
                  methodType === method.type ? "selected" : ""
                }`}
                onClick={() => handleMethodTypeSelection(method.type)}
              >
                <FontAwesomeIcon
                  className="upgrade-method-icon"
                  icon={iconMapping[method.icon]}
                />
                <div className="upgrade-method-title">{method.displayName}</div>
                <div className="upgrade-method-description">
                  {method.description}
                </div>
                <div
                  className="upgrade-method-checks"
                  onClick={(e) => {
                    e.stopPropagation();
                    showRerunButton.current = true;
                    setUpgradeCheckModal(true);
                    setMethodType(method.type);
                  }}
                >
                  {method.isCheckRequestInProgress ? (
                    <span>Loading checks...</span>
                  ) : (
                    <>
                      {method.precheckResultsMessage.includes("Required") ? (
                        <FontAwesomeIcon
                          className="text-danger mx-1"
                          icon={faTimes}
                        />
                      ) : (
                        <FontAwesomeIcon
                          className="text-warning mx-1"
                          icon={faWarning}
                        />
                      )}
                      Checks: {method.precheckResultsMessage}
                    </>
                  )}
                </div>
              </div>
            ))}
        </div>

        <div className="upgrade-failure-tolerance">
          <div>Select optional upgrade failure tolerance:
            <Tooltip message={translate("admin.stackVersions.version.upgrade.upgradeOptions.tolerance.tooltip")}>
              <FontAwesomeIcon
                className="ms-1 custom-link"
                icon={faQuestionCircle}
              />
            </Tooltip>
          </div>
          <Form className="pt-2">
            <Form.Group controlId="serviceCheckFailures">
              <Form.Check
                type="checkbox"
                label="Skip all Service Check failures"
                checked={serviceCheckFailures}
                onChange={(e) => setServiceCheckFailures(e.target.checked)}
              />
            </Form.Group>
            <Form.Group controlId="slaveComponentFailures">
              <Form.Check
                type="checkbox"
                label="Skip all Slave Component failures"
                checked={slaveComponentFailures}
                onChange={(e) => setSlaveComponentFailures(e.target.checked)}
              />
            </Form.Group>
          </Form>
        </div>

        <div className="upgrade-warning">
          Cluster alerts will still be visible and recorded in Ambari but
          notifications (such as Email and SNMP) will be suppressed during the
          upgrade.
        </div>
      </div>
    );
  }

  const columns = [
    {
      accessorKey: "name",
      header: "",
      cell: (info: any) => info.row.original,
      width: "10%",
    },
  ];

  return (
    <>
      {stackVersionError && (
        <div className="alert alert-warning mt-3">
          <div>
            <div className="me-2">
              <FontAwesomeIcon icon={faExclamationTriangle} />
            </div>
            <div>
              <h4 className="display-inline me-1">{stackVersionError.title}</h4>
              <span>{stackVersionError.stackFullName}</span>
              <div>{stackVersionError.description}</div>
            </div>
          </div>
        </div>
      )}
      <div className="mt-4">
        <div className="d-flex">
             <Button
                  size="sm"
                  variant="success"
                  onClick={() => setManageVersionsModal(true)}
                >
                  <FontAwesomeIcon className="mx-2" icon={faExternalLink} />{" "}
                  Manage versions
                </Button>
               <Select
               className="ms-3"
                value={{
                  value: selectedOption.key,
                  label: `FILTER: ${selectedOption.key} (${selectedOption.count})`,
                }}
                onChange={handleSelectChange}
                options={options.map((option) => {
                  return {
                    label: `${option.key} (${option.count})`,
                    value: option.key,
                  };
                })}
              />
        </div>

        {stacks.length !== 0 ? (
          <Row className="position-relative">
            <Col md={3} className="mt-4">
              <div className="version-services-table w-300 bg-transparent position-relative">
                <Table columns={columns} data={services} showHeader={false} scrollable={false} />
              </div>
            </Col>
            <Col md={9} className="versions-slides">
              <div className="versions-slides-bar d-flex flex-nowrap">
                {stacks.map((stackData: StackVersion) => {
                  const isCurrent =
                    stackData.ClusterStackVersions.state === "CURRENT";
                  return (
                    <div
                      className={`version-box me-3${
                        isCurrent ? " current" : ""
                      }`}
                    >
                      {getStackHeader(stackData)}

                      {services.map((service: any) => {
                        const repoServices =
                          stackData.repository_versions[0].RepositoryVersions
                            .services;
                        const matchingService = repoServices.find(
                          (s: any) =>
                            s.name === service ||
                            s.name.toLowerCase() === service.toLowerCase()
                        );
                        if (matchingService?.versions?.[0]?.version) {
                          const version = matchingService.versions[0].version;
                          return (
                            <div className="service-version-info text-center mt-3">
                              <Badge className="bg-success position-relative fs-12 rounded-0">
                                {version}
                              </Badge>
                            </div>
                          );
                        }
                        const stackServices =
                          stackData.repository_versions[0].RepositoryVersions
                            .stack_services;
                        const matchingStackService = stackServices.find(
                          (s: any) =>
                            s.name === service ||
                            s.name.toLowerCase() === service.toLowerCase()
                        );
                        if (matchingStackService?.versions?.[0]) {
                          const version = matchingStackService.versions[0];
                          return (
                            <div className="service-version-info text-center mt-3">
                              <Badge bg="light" className="position-relative fs-12">
                                {version}
                              </Badge>
                            </div>
                          );
                        }
                        return (
                          <div className="service-version-info text-center mt-3">
                            <Badge bg="light" className="position-relative fs-12">
                              UNKNOWN
                            </Badge>
                          </div>
                        );
                      })}
                    </div>
                  );
                })}
              </div>
            </Col>
          </Row>
        ) : null}
      </div>

      {versionModal ? (
        <Modal
          isOpen={versionModal}
          onClose={() => setVersionModal(false)}
          modalTitle="Version Details"
          modalBody={getVersionModalBody()}
          options={{
            modalSize: "modal-sm",
            okButtonText: "DISMISS",
            cancelableViaIcon: true,
            cancelableViaBtn: false,
            modalBodyClassName: "p-0",
          }}
          successCallback={() => setVersionModal(false)}
        />
      ) : null}

      {installPackagesModal ? (
        <Modal
          isOpen={installPackagesModal}
          onClose={() => setInstallPackagesModal(false)}
          modalTitle="Confirmation"
          modalBody={getInstallPackageModalBody()}
          options={{
            cancelableViaBtn: true,
            cancelableViaIcon: true,
          }}
          successCallback={() => installPackages()}
        />
      ) : null}

      <Modal
        isOpen={hostModal}
        onClose={() => setHostModal(false)}
        modalTitle={hostModalTitle.current}
        modalBody={hostModalContent.current}
        options={{
          cancelableViaBtn: true,
          cancelableViaIcon: true,
          okButtonText: "GO TO HOSTS",
          cancelButtonText: "CLOSE",
        }}
        successCallback={() => {
          navigate(
            `/main/hosts/version/${hostModalData.current.versionName}/${hostModalData.current.versionStatus}`
          );
        }}
      />
      <Modal
        isOpen={upgradeModal}
        onClose={() => setUpgradeModal(false)}
        modalTitle="Upgrade Options"
        modalBody={getUpgradeModalContent()}
        className="upgrade-modal"
        options={{
          cancelableViaBtn: true,
          cancelableViaIcon: true,
          cancelableViaSuccessBtn: showUpgradeProceedButton.current,
          okButtonText: "PROCEED",
          cancelButtonText: "CANCEL",
          okButtonDisabled: isProceedButtonDisabled,
        }}
        successCallback={() => {
          if (showUpgradeProceedButton.current) {
            setUpgradeModal(false);
            setUpgradeConfirmationModal(true);
          }
        }}
      />
      {upgradeCheckModal ? (
        <Modal
          isOpen={upgradeCheckModal}
          onClose={() => setUpgradeCheckModal(false)}
          modalTitle={getPreCheckModalTitle()}
          modalBody={getPreCheckResultsModalContent()}
          options={{
            modalSize: "modal-lg",
            cancelableViaBtn: true,
            cancelableViaIcon: true,
            okButtonText: showRerunButton.current
              ? "RERUN PRE-UPGRADE CHECKS"
              : "Proceed anyway",
            cancelButtonText: "CANCEL",
            modalBodyClassName: "scrollable",
          }}
          successCallback={async () => {
            setUpgradeCheckModal(false);
            if (showRerunButton.current) {
              runPreUpgradeCheckOnly({
                id: selectedStackRef.current?.ClusterStackVersions.id,
                label:
                  selectedStackRef.current?.repository_versions[0].RepositoryVersions
                    .display_name,
                type: methodType,
              });
            } else {
              const payload = {
                Upgrade: {
                  repository_version_id:
                    selectedStackRef.current?.repository_versions[0].RepositoryVersions.id,
                  upgrade_type: methodType,
                  skip_failures: slaveComponentFailures.toString(),
                  skip_service_check_failures: serviceCheckFailures.toString(),
                  direction: "UPGRADE",
                },
              };
              try {
                const response = await VersionsApi.getUpgradeId(
                  payload,
                  clusterName
                );
                const upgradeId = response?.resources[0]?.Upgrade?.request_id;
                setUpgradeId(upgradeId);
                if(setIsPatchUpgrade) {
                  const isPatch = get(selectedStackRef.current, "repository_versions[0].RepositoryVersions.type", "STANDARD") === "PATCH";
                  setIsPatchUpgrade(isPatch);

                  await ClusterApi.postPersistData(
                    JSON.stringify({
                      isPatchUpgrade: JSON.stringify(isPatch)
                    })
                  )
                }
                if(setUpgradeVersionDisplayName) {
                  const versionDisplayName = get(selectedStackRef.current, "repository_versions[0].RepositoryVersions.display_name", "");
                  setUpgradeVersionDisplayName(versionDisplayName);
                  await ClusterApi.postPersistData(
                    JSON.stringify({
                      upgradeVersionDisplayName: JSON.stringify(versionDisplayName)
                    })
                  )
                }
                modalManager.show(<Upgrade upgradeId={upgradeId} />);
              } catch (error) {
                modalManager.show({
                  modalTitle: "Upgrade could not be started",
                  modalBody: (
                    <div>
                      {error instanceof Error ? error.message : String(error)}
                    </div>
                  ),
                  onClose: () => {
                    modalManager.hide();
                  },
                  successCallback: () => {
                    modalManager.hide();
                  },
                  options: {
                    cancelableViaIcon: true,
                    cancelableViaBtn: false,
                  },
                });
              }
            }
          }}
        />
      ) : null}
      {upgradeConfirmationModal && (
        <Modal
          modalTitle="Confirmation"
          isOpen={upgradeConfirmationModal}
          onClose={() => setUpgradeConfirmationModal(false)}
          modalBody={getUpgradConfirmationModalBody()}
          options={{
            cancelableViaBtn: true,
            cancelableViaIcon: true,
            okButtonText: "YES",
            cancelButtonText: "CANCEL",
          }}
          successCallback={() => {
            setUpgradeConfirmationModal(false);
            showRerunButton.current = false;
            setUpgradeCheckModal(true);
          }}
        />
      )}
      {manageVersionModal && (
        <Modal
          modalTitle="Manage Versions"
          isOpen={manageVersionModal}
          onClose={() => setManageVersionsModal(false)}
          modalBody="You are about to leave the Cluster Management interface and go to the Ambari Administration interface. You can return to cluster management by using the “Go to Dashboard” link in the Ambari Administration > Clusters section."
          options={{
            cancelableViaBtn: true,
            cancelableViaIcon: true,
            okButtonText: "OK",
            cancelButtonText: "CANCEL",
          }}
          successCallback={() => {
            // redirectToAdminView("stackVersions");
          }}
        />
      )}
    </>
  );
}
