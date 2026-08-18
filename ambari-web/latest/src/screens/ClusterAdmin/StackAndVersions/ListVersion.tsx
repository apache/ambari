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
import { messages } from "../../messages";
import {
  Alert,
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
  faArrowLeft,
  faQuestionCircle
} from "@fortawesome/free-solid-svg-icons";
import { cloneDeep, get, set } from "lodash";
import { RequestApi } from "../../../api/requestApi";
import { Link, useNavigate } from "react-router-dom";
import { StackVersion, Item, Response, ClusterCheckPopupData } from "./types";
import VersionsApi from "../../../api/versionsApi";
import toast from "react-hot-toast";
import Spinner from "../../../components/Spinner";
import Modal from "../../../components/Modal";
import OperationsProgress from "../../../components/OperationsProgress";
import usePolling from "../../../hooks/usePolling";
import Tooltip from "../../../components/Tooltip";
import Select from "react-select";
import { AppContext } from "../../../store/context";
import modalManager from "../../../store/ModalManager";
import Upgrade from "./Upgrade";
import Table from "../../../components/Table";
import RepoModal from "../../../components/RepoModal";
import { initialOptions, initialUpgradeMethods, showAlertModal, translate, getUpgradeRequestStatus, translateWithVariables } from "../../../Utils/Utility";
import stringUtilsObj from "../../../Utils/StringUtilsObj";
import { redirectToAdminView } from "../../../Utils/adminViewRedirect";
import ClusterApi from "../../../api/clusterApi";
import { useAuth } from "../../../hooks/useAuth";
import { HostsApi } from "../../../api/hostsApi";
import { persistedPayload } from "../../../Utils/persistedSettings";
import {
  canHideRepositoryVersion,
  compatibleRepositoryVersionNames,
  filterVisibleStackVersions,
  versionMatchesFilter,
  type VersionFilterKey,
} from "./versionUtils";
import useKDCSessionState from "../../../hooks/useKDCSessionState";
import PreUpgradeCheckItem from "./PreUpgradeCheckItem";

export const iconMapping: { [key: string]: IconDefinition } = {
  faDashboard: faDashboard,
  faBolt: faBolt,
};

export default function Versions() {
  const [options, setOptions] = useState(initialOptions);
  const [selectedOption, setSelectedOption] = useState(options[0]);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [services, setServices] = useState<string[]>([]);
  const [allLoadedStacks, setAllLoadedStacks] = useState<StackVersion[]>([]);
  const [originalStacks, setOriginalStacks] = useState<StackVersion[]>([]);
  const [stacks, setStacks] = useState<StackVersion[]>([]);
  const [stackVersionError, setStackVersionError] = useState<any>(null);
  const [upgradeMethods, setUpgradeMethods] = useState(initialUpgradeMethods);
  const [methodType, setMethodType] = useState("");
  const [, setCompletionStatus] = useState(false);
  const [selectedStack, setSelectedStack] = useState<StackVersion>();
  const selectedStackRef = useRef<StackVersion | undefined>(undefined);
  const [currentUpgradeTypes, setCurrentUpgradeTypes] = useState<string[]>([]);
  const [isOperationInProgress, setIsOperationInProgress] = useState(false);
  const [operationsState, setOperationsState] = useState<any[]>([]);
  const [slaveComponentFailures, setSlaveComponentFailures] = useState(false);
  const [serviceCheckFailures, setServiceCheckFailures] = useState(false);
  const [isRestoring, setIsRestoring] = useState(true);
  const { clusterName, setUpgradeId, upgradeState, setIsPatchUpgrade, setUpgradeVersionDisplayName, allHostNames, upgradeVersionDisplayName, upgradeId, upgradeDirection, upgradeIsRunning, upgradeSuspended, supports, isNonWizardUser } = useContext(AppContext);
  
  const packagesPayloadRef = useRef<any>({});
  
  // Authorization hooks - implementing Ember.js stack/version authorization patterns
  const { hasAuthorization, isAdmin, isOperator, user } = useAuth();
  
  // Check specific authorizations for stack/version operations
  const canUpgradeDowngrade = hasAuthorization('CLUSTER.UPGRADE_DOWNGRADE_STACK');
  const canManageStackVersions = hasAuthorization("AMBARI.MANAGE_STACK_VERSIONS");
  const canSaveRepositories =
    isAdmin()
    && !isOperator()
    && !isNonWizardUser
    && (!upgradeIsRunning || upgradeSuspended || Boolean(supports.opsDuringRollingUpgrade));
  const versionMutationBlocked =
    !canUpgradeDowngrade
    || isNonWizardUser
    || (upgradeState !== "NOT_REQUIRED" && upgradeState !== "COMPLETED");

  const [versionModal, setVersionModal] = useState(false);
  const [installPackagesModal, setInstallPackagesModal] = useState(false);
  const [hostModal, setHostModal] = useState(false);
  const [upgradeModal, setUpgradeModal] = useState(false);
  const [upgradeCheckModal, setUpgradeCheckModal] = useState(false);
  const [upgradeConfirmationModal, setUpgradeConfirmationModal] = useState(false);
  const [manageVersionModal, setManageVersionsModal] = useState(false);
  const [alertModal, setAlertModal] = useState(false);
  const [confirmRevertPatchUpradeModal, setConfirmRevertPatchUpgradeModal] = useState(false);
  const [confirmReinstallModal, setConfirmReinstallModal] = useState(false);
  const [confirmRemoveModal, setConfirmRemoveModal] = useState(false);
  const [hideVersion, setHideVersion] = useState<StackVersion>();
  const [hideInProgress, setHideInProgress] = useState(false);
  const [startInProgress, setStartInProgress] = useState(false);
  const [outOfSyncActionInProgress, setOutOfSyncActionInProgress] = useState(false);

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
  const preCheckGenerationRef = useRef(0);
  
  // Refs for fast switching between upgrade methods
  const methodTypeRef = useRef("");
  const isUpgradeInProgress = upgradeIsRunning && !upgradeSuspended;
  const navigate = useNavigate();
  const { getKDCSessionState } = useKDCSessionState(null);

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
      setIsProceedButtonDisabled(isStillLoading || hasRequiredFailures);
    }
  }, []);

  async function fetchServices() {
    try {
      if (originalStacks.length === 0) {
        setLoading(true);
      }
      setLoadError(null);
      const response = await VersionsApi.getAllStacks(clusterName);
      const loadedStacks: StackVersion[] = response.items || [];
      setAllLoadedStacks(loadedStacks);
      const currentStack = loadedStacks.find(
        (stack: StackVersion) => stack.ClusterStackVersions.state === "CURRENT"
      );
      let compatibleVersions = new Set<string>();
      if (currentStack && !supports.displayOlderVersions) {
        try {
          const compatibility = await VersionsApi.getCompatibleRepositoryVersions(
            currentStack.ClusterStackVersions.stack,
            currentStack.ClusterStackVersions.version,
          );
          compatibleVersions = compatibleRepositoryVersionNames(compatibility);
        } catch (error: any) {
          setLoadError(
            error?.response?.data?.message
              || error?.message
              || "Compatible repository versions could not be loaded. Cross-stack targets are hidden until retry."
          );
        }
      }
      setOriginalStacks(filterVisibleStackVersions(
        loadedStacks,
        currentStack,
        compatibleVersions,
        Boolean(supports.displayOlderVersions),
      ));

      setServices(Object.keys(
        currentStack?.ClusterStackVersions.repository_summary?.services
          || loadedStacks[0]?.ClusterStackVersions.repository_summary?.services
          || {},
      ));
    } catch (err: any) {
      const message = err?.response?.data?.message || err?.message || "Stack versions could not be loaded";
      setLoadError(message);
      toast.error(message);
    } finally {
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
      const currentStack = originalStacks.find(
        (stack) => stack.ClusterStackVersions.state === "CURRENT",
      );
      setStacks(originalStacks.filter((stack) => versionMatchesFilter(
        stack,
        selectedOption.key as VersionFilterKey,
        currentStack,
        upgradeState !== "NOT_REQUIRED" && upgradeState !== "COMPLETED"
          ? upgradeVersionDisplayName
          : undefined,
      )));
    } else {
      setStacks(originalStacks);
    }

    const updatedOptions = options.map((option) => {
      let count;
      if (option.key === "ALL") {
        count = originalStacks.length;
      } else {
        const currentStack = originalStacks.find(
          (stack) => stack.ClusterStackVersions.state === "CURRENT",
        );
        count = originalStacks.filter((stack) => versionMatchesFilter(
          stack,
          option.key as VersionFilterKey,
          currentStack,
          upgradeState !== "NOT_REQUIRED" && upgradeState !== "COMPLETED"
            ? upgradeVersionDisplayName
            : undefined,
        )).length;
      }

      return { ...option, count };
    });

    setOptions(updatedOptions);
  }, [originalStacks, selectedOption, upgradeState, upgradeVersionDisplayName]);

  useEffect(() => {
    const errorStack = originalStacks
      .filter(stack => stack.ClusterStackVersions.state === 'OUT_OF_SYNC')
      .find(stack => stack.repository_versions[0].RepositoryVersions.type === 'STANDARD');
    
    if (errorStack) {
      const outOfSyncHosts = [
        ...errorStack.ClusterStackVersions.host_states.INSTALL_FAILED,
        ...errorStack.ClusterStackVersions.host_states.OUT_OF_SYNC
      ];
      
      setStackVersionError({
        title: translate('admin.stackVersions.version.errors.outOfSync.title'),
        description: translate('admin.stackVersions.version.errors.outOfSync.desc'),
        stackFullName: errorStack.repository_versions[0].RepositoryVersions.stack_name + '-' + errorStack.repository_versions[0].RepositoryVersions.repository_version,
        repoId: errorStack.ClusterStackVersions.id,
        outOfSyncHosts: outOfSyncHosts
      });
    } else {
      setStackVersionError(null);
    }
  }, [originalStacks]);

  // useEffect(()=>{
  //   const matchingOption= options.find((opt) => opt.key === selectedOption.key);
  //   if(matchingOption){
  //     setSelectedOption(matchingOption);
  //   }
  // },[options])

  useEffect(()=>{
    const matchingOption= options.find((opt) => opt.key === selectedOption.key);
    if(matchingOption && matchingOption.count !== selectedOption.count){
      setSelectedOption(matchingOption);
    }
  },[options])



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
        setIsProceedButtonDisabled(isStillLoading || hasRequiredFailures);
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
    const isOutOfSync = stackData.ClusterStackVersions.state === 'OUT_OF_SYNC';
    
    return (
      <div className={`p-2 position-relative`}>
        {isOutOfSync && (
          <div className="position-absolute top-0 end-0 p-2">
            <Tooltip message="Out of Sync" placement="top">
              <FontAwesomeIcon 
                icon={faExclamationTriangle} 
                className="text-warning" 
              />
            </Tooltip>
          </div>
        )}
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
              disabled={!canUpgradeDowngrade}
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
              {stackData.ClusterStackVersions.supports_revert && !isNonWizardUser && (
                <>
                  <Dropdown.Toggle
                    size="sm"
                    split
                    variant="success"
                    id="dropdown-split-basic"
                  />
                  <Dropdown.Menu className="dropdown-menu-right">
                      <Dropdown.Item
                        disabled={versionMutationBlocked || startInProgress}
                        onClick={() => {
                          setSelectedStack(stackData)
                          selectedStackRef.current = stackData; 
                          setConfirmRevertPatchUpgradeModal(true)
                        }}
                      >
                        Revert
                      </Dropdown.Item>
                  </Dropdown.Menu>
                </>
              )}
            </Dropdown>

          ) : getButtonName(stackData) === "upgrade" ? (
            <Dropdown as={ButtonGroup} className="upgrade-dropdown">
              <Button
                variant="success"
                className="text-uppercase"
                size="sm"
                disabled={versionMutationBlocked || startInProgress}
                onClick={() => handleUpgradeButton(stackData)}
              >
                Upgrade
              </Button>
              <Dropdown.Toggle
                size="sm"
                split
                variant="success"
                id="dropdown"
                disabled={versionMutationBlocked || startInProgress}
              />
              <Dropdown.Menu className="dropdown-menu-right">
                <Dropdown.Item
                disabled={versionMutationBlocked || isOperationInProgress}
                  onClick={() => installPackagesPayloadSet(stackData)}
                >
                  Re-install
                </Dropdown.Item>
                {supports.preUpgradeCheck && <Dropdown.Item
                  disabled={versionMutationBlocked || startInProgress}
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
                </Dropdown.Item>}
              </Dropdown.Menu>
            </Dropdown>
          ) : getButtonName(stackData) === "installed" ? (
            <Button
              variant="success"
              className="text-uppercase"
              disabled
              size="sm"
            >
              INSTALLED
            </Button>
          ) : getButtonName(stackData) === "installError" ? (
            <div className="text-center text-warning">
              <FontAwesomeIcon icon={faWarning} className="me-2"/>
              <span className="fw-bold">{translate('admin.stackVersions.version.installError')}</span>
            </div>
          ) : (
            <Button
              variant="success"
              size="sm"
              className="text-uppercase fw-bold"
              onClick={() => handleUpgradeButton(stackData)}
              disabled={versionMutationBlocked || isOperationInProgress}
            >
              {getButtonName(stackData) === "re-install"
                ? "RE-INSTALL"
                : getButtonName(stackData).toUpperCase()}
            </Button>
          )}
        </div>
        {canUpgradeDowngrade && canHideRepositoryVersion(stackData) && (
          <div className="text-center mt-2">
            <Button
              variant="link"
              size="sm"
              disabled={hideInProgress || versionMutationBlocked}
              onClick={() => setHideVersion(stackData)}
            >
              Hide
            </Button>
          </div>
        )}
      </div>
    );
  }

  async function installPackagesPayloadSet(stackData: StackVersion) {
    if (versionMutationBlocked || isOperationInProgress) return;
    
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
    if (versionMutationBlocked || isOperationInProgress) return;
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

  async function hideRepositoryVersion() {
    const repository = hideVersion?.repository_versions?.[0]?.RepositoryVersions;
    if (!hideVersion || !repository || hideInProgress || versionMutationBlocked) return;

    setHideInProgress(true);
    try {
      await VersionsApi.hideRepositoryVersion(
        repository.stack_name,
        repository.stack_version,
        repository.id,
      );
      setOriginalStacks((current) => current.filter(
        (stack) => stack.ClusterStackVersions.id !== hideVersion.ClusterStackVersions.id,
      ));
      setHideVersion(undefined);
      toast.success("Repository version hidden");
    } catch (error: any) {
      toast.error(error?.response?.data?.message || error?.message || "Repository version could not be hidden");
    } finally {
      setHideInProgress(false);
    }
  }

  async function handleUpgradeClick(stack: StackVersion) {
    preCheckGenerationRef.current += 1;
    let upgradeTypes: string[];
    try {
      const response = await VersionsApi.get_supported_upgradeTypes(
        stack.ClusterStackVersions.stack,
        stack.ClusterStackVersions.version,
        stack.repository_versions[0].RepositoryVersions.repository_version
      );
      upgradeTypes = response?.items?.[0]?.CompatibleRepositoryVersions?.upgrade_types || [];
    } catch (error: any) {
      toast.error(error?.response?.data?.message || error?.message || "Supported upgrade methods could not be loaded");
      return;
    }

    // Store upgrade types in state instead of in the stack object
    setCurrentUpgradeTypes(upgradeTypes);
    setMethodType("");
    methodTypeRef.current = "";
    setSlaveComponentFailures(false);
    setServiceCheckFailures(false);

    // run upgrade method checks.
    const updateMethods = initialUpgradeMethods.map((method) => ({
      ...method,
      allowed: upgradeTypes.includes(method.type)
        && (method.type !== "HOST_ORDERED" || Boolean(supports.enabledWizardForHostOrderedUpgrade)),
      isCheckRequestInProgress: Boolean(supports.preUpgradeCheck) && upgradeTypes.includes(method.type),
      isCheckComplete: !supports.preUpgradeCheck,
      precheckResultsMessage: supports.preUpgradeCheck ? "" : "Not required",
    }));
    upgradeMethodsRef.current = updateMethods;
    setUpgradeMethods(updateMethods);

    setUpgradeModal(true);

    // Run the pre-upgrade checks after setting the initial modal content
    if (supports.preUpgradeCheck) {
      void runUpgradeMethodCheck(stack);
    }
  }

  async function runPreUpgradeCheckOnly(
    data: any,
    generation = preCheckGenerationRef.current,
  ) {
    const methodIndex = upgradeMethodsRef.current.findIndex(
      (method) => method.type === data.type
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

    let response;
    try {
      response = await VersionsApi.runPreUpgradeCheck(
        clusterName,
        data.id,
        data.type
      );
    } catch (error: any) {
      if (generation === preCheckGenerationRef.current) {
        const updatedMethods = upgradeMethodsRef.current.map((candidate) =>
          candidate.type === data.type
            ? {
                ...candidate,
                isCheckComplete: true,
                isCheckRequestInProgress: false,
                precheckResultsMessage: "Required: Check Failed",
                preCheckResultsModalContent: (
                  <Alert variant="danger">
                    {error?.response?.data?.message || error?.message || "Pre-upgrade checks could not be loaded"}
                  </Alert>
                ),
              }
            : candidate
        );
        upgradeMethodsRef.current = updatedMethods;
        setUpgradeMethods(updatedMethods);
      }
      throw error;
    }
    if (generation !== preCheckGenerationRef.current) {
      return;
    }

    let failTitle = translate("popup.clusterCheck.Upgrade.fail.title");
    let failAlert = translate("popup.clusterCheck.Upgrade.fail.alert");
    const bypassedFailures =
      response?.items?.filter(
        (item: Item) => item.UpgradeChecks?.status === "BYPASS"
      ).length > 0;
    if (
      response.items.filter(
        (item: Item) => item.UpgradeChecks.status === "FAIL"
      ).length === 0 &&
      bypassedFailures
    ) {
      failTitle = get(
        messages,
        "popup.clusterCheck.Upgrade.bypassed-failures.title"
      );
      failAlert = get(
        messages,
        "popup.clusterCheck.Upgrade.bypassed-failures.alert"
      );
    }
    const headerTemplate = get(messages, "popup.clusterCheck.Upgrade.header");
    const header = headerTemplate.replace(
      "{0}",
      selectedStackRef.current?.repository_versions[0]?.RepositoryVersions?.display_name || "Unknown"
    );

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
        primary: get(
          messages,
          "admin.stackVersions.version.upgrade.upgradeOptions.preCheck.rerun"
        ),
        secondary: get(messages, "common.cancel"),
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
            <div>{mapUpgradeChecks(fails, methodType)}</div>
          </div>
        )}
        {warnings.length > 0 && (
          <div>
            <h2>{popup.warningTitle}</h2>
            <div className="alert alert-warning">{popup.warningAlert}</div>
            <div>{mapUpgradeChecks(warnings, methodType)}</div>
          </div>
        )}
        {bypass.length > 0 && (
          <div>
            <h5>{popup.failTitle}</h5>
            <p>{popup.failAlert}</p>
            <div>{mapUpgradeChecks(bypass, methodType)}</div>
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

    const resultParts = [
      fails.length ? `${fails.length} Required` : "",
      warnings.length ? `${warnings.length} Warning` : "",
      bypass.length ? `${bypass.length} Bypassed` : "",
    ].filter(Boolean);
    let upgradeCheckResult1 = resultParts.join(", ") || "Passed";
    if(upgradeCheckResult1 === "Passed") {
      upgradeCheckModalContent1 = translate("admin.stackVersions.version.upgrade.upgradeOptions.preCheck.allPassed.msg")
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

  function mapUpgradeChecks(items: Item[], upgradeType: string) {
    const repositoryVersionId = selectedStackRef.current?.ClusterStackVersions.id || 0;
    return items.map((item, index) => {
      return (
        <ul key={`${item.UpgradeChecks.id}-${index}`} className="mb-0">
          <PreUpgradeCheckItem
            check={item.UpgradeChecks}
            repositoryVersionId={repositoryVersionId}
            upgradeType={upgradeType}
            onRecheck={runPreUpgradeCheckOnly}
          />
        </ul>
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
    const generation = preCheckGenerationRef.current;
    const checkPromises = allowedMethods.map(method =>
      runPreUpgradeCheckOnly({
        id: stack.ClusterStackVersions.id,
        label: stack.repository_versions[0].RepositoryVersions.display_name,
        type: method.type,
      }, generation).catch(() => undefined)
    );

    try {
      await Promise.all(checkPromises);
      // Cache the results
      if (generation === preCheckGenerationRef.current) {
        preCheckCacheRef.current.set(cacheKey, upgradeMethodsRef.current);
      }
    } catch (error) {
      console.error("Error running upgrade method checks:", error);
    }
  }

  async function claimUpgradeOwnership() {
    await ClusterApi.postPersistData(persistedPayload({
      "wizard-data": { userName: user?.user_name || "" },
    }));
  }

  async function startUpgrade() {
    if (!selectedStackRef.current || !methodType || startInProgress || versionMutationBlocked) return;
    const payload = {
      Upgrade: {
        repository_version_id: selectedStackRef.current.repository_versions[0].RepositoryVersions.id,
        upgrade_type: methodType,
        skip_failures: slaveComponentFailures.toString(),
        skip_service_check_failures: serviceCheckFailures.toString(),
        direction: "UPGRADE",
      },
    };

    setStartInProgress(true);
    try {
      // Persist the owner before creating the request so other open browsers
      // become read-only as soon as they receive the upgrade event.
      await claimUpgradeOwnership();
      const response = await VersionsApi.getUpgradeId(payload, clusterName);
      const newUpgradeId = response?.resources?.[0]?.Upgrade?.request_id;
      if (!newUpgradeId) throw new Error("Ambari did not return an upgrade request ID");
      setUpgradeId(newUpgradeId);

      const isPatch = get(
        selectedStackRef.current,
        "repository_versions[0].RepositoryVersions.type",
        "STANDARD",
      ) === "PATCH";
      if (setIsPatchUpgrade) setIsPatchUpgrade(isPatch);
      const versionDisplayName = get(
        selectedStackRef.current,
        "repository_versions[0].RepositoryVersions.display_name",
        "",
      );
      if (setUpgradeVersionDisplayName) setUpgradeVersionDisplayName(versionDisplayName);

      await Promise.all([
        ClusterApi.postPersistData(persistedPayload({ isPatchUpgrade: isPatch })),
        ClusterApi.postPersistData(persistedPayload({ upgradeVersionDisplayName: versionDisplayName })),
      ]).catch(() => {
        toast.error("The upgrade started, but its browser state could not be persisted");
      });
      setUpgradeCheckModal(false);
      setUpgradeConfirmationModal(false);
      modalManager.show(<Upgrade upgradeId={newUpgradeId} />);
    } catch (error) {
      modalManager.show({
        modalTitle: "Upgrade could not be started",
        modalBody: <div>{error instanceof Error ? error.message : String(error)}</div>,
        onClose: () => modalManager.hide(),
        successCallback: () => modalManager.hide(),
        options: { cancelableViaIcon: true, cancelableViaBtn: false },
      });
    } finally {
      setStartInProgress(false);
    }
  }

  function installPackages() {
    if (versionMutationBlocked) {
      return;
    }
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
      case "OUT_OF_SYNC":
        return "installError";
      case "INSTALL_FAILED":
        return "re-install";
      case "INSTALLED":
        return shouldShowUpgradeButton(stackData) ? "upgrade" : "installed";
      case "INSTALLING":
        return "installing";
      case "INTERMEDIATE":
        return "intermediateInstalling";
      default:
        return "install_packages";
    }
  }

  // Enhanced upgrade button logic similar to Ember.js implementation
  function shouldShowUpgradeButton(stackData: StackVersion): boolean {
    // Sort stacks by version like Ember.js does, then find first CURRENT (like Ember.js)
    const sortedStacks = [...originalStacks].sort((a, b) => {
      const versionA = a.repository_versions[0]?.RepositoryVersions.repository_version || '';
      const versionB = b.repository_versions[0]?.RepositoryVersions.repository_version || '';
      return stringUtilsObj.compareVersions(versionA, versionB);
    });
    
    const currentVersion = sortedStacks.find(
      (stack: StackVersion) => stack.ClusterStackVersions.state === "CURRENT"
    );
    
    if (!currentVersion) {
      return true; // If no current version, allow upgrade
    }

    const currentRepoVersion = currentVersion.repository_versions[0]?.RepositoryVersions;
    const targetRepoVersion = stackData.repository_versions[0]?.RepositoryVersions;
    
    if (!currentRepoVersion || !targetRepoVersion) {
      return false;
    }

    // Check if it's a different stack or higher version (like Ember.js)
    const isDifferentStack = currentRepoVersion.stack_name !== targetRepoVersion.stack_name;
    const isVersionHigher = stringUtilsObj.compareVersions(
      targetRepoVersion.repository_version,
      currentRepoVersion.repository_version
    ) > 0;

    if (!isDifferentStack && !isVersionHigher) {
      return false; // Don't show upgrade for same or lower versions of same stack
    }

    // Check service upgradeability (like Ember.js)
    const isStandardVersion = targetRepoVersion.type === "STANDARD";
    if (isStandardVersion) {
      return true; // Standard versions are always upgradeable
    }

    // For PATCH/MAINT versions, check if there are upgradeable services (like Ember.js)
    const repositorySummary = stackData.ClusterStackVersions?.repository_summary;
    if (repositorySummary?.services) {
      const hasUpgradeableServices = Object.entries(repositorySummary.services).some(([serviceName, serviceInfo]: [string, any]) => {
        // Check if service exists in cluster AND is marked as upgradeable (like Ember.js isUpgradable check)
        return services.includes(serviceName) && serviceInfo.upgrade === true;
      });
      
      return hasUpgradeableServices;
    }

    return false;
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

    const isOutOfSync = selectedStackRef.current.ClusterStackVersions.state === 'OUT_OF_SYNC';
    
    return (
      <div className="m-n2">
        <div className="bg-info-subtle p-2">
          <div className="d-flex justify-content-between">
            <div className="ms-2">
              {isOutOfSync && (
                <Tooltip message="Out of Sync" placement="right">
                  <FontAwesomeIcon 
                    icon={faExclamationTriangle} 
                    className="text-warning" 
                  />
                </Tooltip>
              )}
            </div>
            <h2 className="text-dark ms-4">
              {selectedStackRef.current?.repository_versions[0]?.RepositoryVersions
                ?.display_name || "NA"}
            </h2>
            <h2 className="mx-2">
              {/* Only show repository edit icon if user has CLUSTER.UPGRADE_DOWNGRADE_STACK permission */}
              {canManageStackVersions && !isNonWizardUser && !isUpgradeInProgress && (
                <Tooltip message="Click to Edit Repositories" placement="top">
                  <FontAwesomeIcon
                    className="fs-16 text-info"
                    onClick={() =>
                      modalManager.show(
                        <RepoModal
                          selectedStack={selectedStackRef.current}
                          isOpen
                          canSave={canSaveRepositories}
                          onClose={() => {
                            modalManager.hide();
                          }}
                        />
                      )
                    }
                    icon={faEdit}
                  />
                </Tooltip>
              )}
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
              disabled={!canUpgradeDowngrade}
            >
              {translate(getUpgradeStatus(selectedStackRef.current).statusText || "admin.stackUpgrade.state.inProgress")}
            </Button>  
            ) : getButtonName(selectedStackRef.current) === "installError" ? (
              <div className="text-center text-warning py-2">
                <FontAwesomeIcon icon={faWarning} className="me-2"/>
                <span className="fw-bold fs-5">{translate('admin.stackVersions.version.installError')}</span>
              </div>
            ) : getButtonName(selectedStackRef.current) === "upgrade" ? (
              <Dropdown as={ButtonGroup} className="upgrade-dropdown popup">
                <Button
                  variant="success"
                  size="sm"
                  className="text-uppercase"
                  disabled={!selectedStackRef.current || versionMutationBlocked || startInProgress}
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
                  disabled={!selectedStackRef.current || versionMutationBlocked || startInProgress}
                  id="dropdown-split-basic"
                />
                <Dropdown.Menu className="dropdown-menu-right">
                  <Dropdown.Item
                    disabled={!selectedStackRef.current || versionMutationBlocked || isOperationInProgress}
                    onClick={() => {
                      if (selectedStackRef.current) {
                        installPackagesPayloadSet(selectedStackRef.current);
                      }
                    }}
                  >
                    Re-install
                  </Dropdown.Item>
                  {supports.preUpgradeCheck && <Dropdown.Item
                    disabled={!selectedStackRef.current || versionMutationBlocked || startInProgress}
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
                  </Dropdown.Item>}
                </Dropdown.Menu>
              </Dropdown>
            ) : (
              <Button
                className="text-uppercase"
                variant="success"
                size="sm"
                disabled={!selectedStackRef.current || versionMutationBlocked || isOperationInProgress}
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

  function getRevertPatchUpgradeModalBody() {
    if (!selectedStackRef.current?.ClusterStackVersions.supports_revert) {
      return <p>Revert is not supported for this version.</p>;
    }
    
    const parentStackId = selectedStackRef.current?.repository_versions[0].RepositoryVersions.parent_id;
    
    const parentStack = allLoadedStacks.find(
      stack => stack.ClusterStackVersions.id === parentStackId
    );
    
    if (!parentStack) {
      return <p>Could not find the target stack version for revert.</p>;
    }
    
    const fromVersion = selectedStackRef.current?.repository_versions[0].RepositoryVersions.display_name;
    const toVersion = parentStack.repository_versions[0].RepositoryVersions.display_name;
    
    const servicesToBeReverted = [];
    const currentServices = selectedStackRef.current?.ClusterStackVersions.repository_summary.services;
    const targetServices = parentStack.ClusterStackVersions.repository_summary.services;
    
    for (const [serviceName, serviceInfo] of Object.entries(currentServices)) {
      if (targetServices[serviceName]) {
        servicesToBeReverted.push({
          displayName: serviceName,
          fromVersion: serviceInfo.version,
          toVersion: targetServices[serviceName].version
        });
      }
    }
   
    const revertTableColumns = [
      {
        accessorKey: "displayName",
        header: "",
        cell: (info: any) => info.getValue(),
      },
      {
        accessorKey: "toVersion",
        header: toVersion,
        cell: (info: any) => (
          <div className="service-version-info">
            <span className="badge bg-light">{info.getValue()}</span>
          </div>
        ),
      },
      {
        accessorKey: "arrow",
        header: "",
        cell: () => <FontAwesomeIcon className="me-2" icon={faArrowLeft}/>,
      },
      {
        accessorKey: "fromVersion",
        header: fromVersion,
        cell: (info: any) => (
          <div className="service-version-info">
            <span className="badge bg-light">{info.getValue()}</span>
          </div>
        ),
      },
    ];

    return (
      <>
        <div>{translate("admin.stackVersions.upgrade.patch.revert.confirmation")}</div>
        <div className="mt-1">
          <Table 
            columns={revertTableColumns} 
            data={servicesToBeReverted} 
            showHeader={true} 
            scrollable={false}
          />
        </div>
      </>
    );
  }

  async function revertPatchUpgrade() {
    if (versionMutationBlocked || startInProgress) return;
    const selectedStack = selectedStackRef.current;
    const parentStackId = selectedStack?.repository_versions?.[0]?.RepositoryVersions.parent_id;
    const parentStack = allLoadedStacks.find(
      (stack) => stack.ClusterStackVersions.id === parentStackId,
    );
    if (!selectedStack?.ClusterStackVersions.supports_revert || !parentStack) {
      toast.error("The target stack version for this revert could not be found");
      return;
    }

    const payload = {
      "Upgrade": {
        "revert_upgrade_id": selectedStack.ClusterStackVersions.revert_upgrade_id,
      }
    }

    setStartInProgress(true);
    try {
      await claimUpgradeOwnership();
      const response = await VersionsApi.getUpgradeId(
        payload,
        clusterName
      );
      const upgradeId = response?.resources[0]?.Upgrade?.request_id;
      if (!upgradeId) throw new Error("Ambari did not return a revert request ID");
      setUpgradeId(upgradeId);
      const isPatch = get(
        selectedStack,
        "repository_versions[0].RepositoryVersions.type",
        "STANDARD",
      ) === "PATCH";
      const targetDisplayName = get(
        parentStack,
        "repository_versions[0].RepositoryVersions.display_name",
        "",
      );
      if (setIsPatchUpgrade) setIsPatchUpgrade(isPatch);
      if (setUpgradeVersionDisplayName) setUpgradeVersionDisplayName(targetDisplayName);

      await Promise.all([
        ClusterApi.postPersistData(persistedPayload({ isPatchUpgrade: isPatch })),
        ClusterApi.postPersistData(persistedPayload({ upgradeVersionDisplayName: targetDisplayName })),
      ]).catch(() => {
        toast.error("The revert started, but its browser state could not be persisted");
      });
      modalManager.show(<Upgrade upgradeId={upgradeId} />);
    } catch (error) {
      modalManager.show({
        modalTitle: "Revert could not be started",
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
    } finally {
      setStartInProgress(false);
    }
  }

  // Handle reinstall of out-of-sync components
  async function handleReinstallOutOfSyncComponents() {
    if (!stackVersionError || isNonWizardUser || outOfSyncActionInProgress) return;
    
    const outOfSyncHosts = stackVersionError.outOfSyncHosts;
    
    if (!outOfSyncHosts || outOfSyncHosts.length === 0) {
      toast.error("No out-of-sync hosts found.");
      return;
    }
    
    setOutOfSyncActionInProgress(true);
    try {
      await getKDCSessionState(async () => {
        await HostsApi.updateHostComponents(
          clusterName,
          `HostRoles/host_name.in(${outOfSyncHosts.join(',')})&HostRoles/state=INSTALL_FAILED`,
          {
            context: translate("hosts.host.maintainance.reinstallFailedComponents.context"),
            HostRoles: { state: 'INSTALLED' },
            query: `HostRoles/host_name.in(${outOfSyncHosts.join(',')})&HostRoles/state=INSTALL_FAILED`
          }
        );
        toast.success("Reinstall request submitted successfully");
        setConfirmReinstallModal(false);
        await fetchServices();
      });
    } catch (error: any) {
      toast.error(error?.response?.data?.message || error?.message || "Failed to reinstall components");
    } finally {
      setOutOfSyncActionInProgress(false);
    }
  }

  // Handle remove of out-of-sync components
  async function handleRemoveOutOfSyncComponents() {
    if (!stackVersionError || isNonWizardUser || outOfSyncActionInProgress) return;
    
    const outOfSyncHosts = stackVersionError.outOfSyncHosts;
    
    if (!outOfSyncHosts || outOfSyncHosts.length === 0) {
      toast.error("No out-of-sync hosts found.");
      return;
    }
    
    setOutOfSyncActionInProgress(true);
    try {
      await getKDCSessionState(async () => {
        await HostsApi.deleteHostComponents(
          {
            RequestInfo: {
              query: `HostRoles/host_name.in(${outOfSyncHosts.join(',')})&HostRoles/state=INSTALL_FAILED`
            }
          },
          clusterName
        );
        toast.success("Remove request submitted successfully");
        setConfirmRemoveModal(false);
        await fetchServices();
      });
    } catch (error: any) {
      toast.error(error?.response?.data?.message || error?.message || "Failed to remove components");
    } finally {
      setOutOfSyncActionInProgress(false);
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
          {currentUpgradeTypes.length === 0 && (
            <Alert variant="warning">The server did not return a supported upgrade method for this target.</Alert>
          )}
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
      {loadError && (
        <Alert variant="danger" className="mt-3 d-flex justify-content-between align-items-center">
          <span>{loadError}</span>
          <Button size="sm" variant="outline-danger" onClick={() => void fetchServices()}>
            Retry
          </Button>
        </Alert>
      )}
      {stackVersionError && (
        <div className="alert alert-warning mt-3">
          <div className="d-flex align-items-start">
            <div className="me-3">
              <FontAwesomeIcon icon={faExclamationTriangle} />
            </div>
            <div className="flex-grow-1">
              <h4 className="d-inline me-2 mb-2">
                {stackVersionError.title}
              </h4>
              <span className="badge bg-secondary">{stackVersionError.stackFullName}</span>
              <div className="mt-2">{stackVersionError.description}</div>
            </div>
            {canManageStackVersions && !isNonWizardUser && !isUpgradeInProgress && (
              <div className="d-flex gap-2 ms-3">
                <Button
                  variant="warning"
                  size="sm"
                  disabled={outOfSyncActionInProgress}
                  onClick={() => setConfirmReinstallModal(true)}
                >
                  {translate('common.reinstall')}
                </Button>
                <Button
                  variant="warning"
                  size="sm"
                  disabled={outOfSyncActionInProgress}
                  onClick={() => setConfirmRemoveModal(true)}
                >
                  {translate('common.remove')}
                </Button>
              </div>
            )}
          </div>
        </div>
      )}
      <div className="mt-4">
        <div className="d-flex">
           {/* Only show Manage versions button if user has CLUSTER.UPGRADE_DOWNGRADE_STACK permission */}
           {canManageStackVersions && !isUpgradeInProgress && !isNonWizardUser && (
             <Button
                  size="sm"
                  variant="success"
                  onClick={() => setManageVersionsModal(true)}
                >
                  <FontAwesomeIcon className="mx-2" icon={faExternalLink} />{" "}
                  Manage versions
                </Button>
           )}
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
            okButtonDisabled: isProceedButtonDisabled || startInProgress,
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
              void runPreUpgradeCheckOnly({
                id: selectedStackRef.current?.ClusterStackVersions.id,
                label:
                  selectedStackRef.current?.repository_versions[0].RepositoryVersions
                    .display_name,
                type: methodType,
              }).catch(() => undefined);
            } else {
              await startUpgrade();
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
            if (supports.preUpgradeCheck) {
              showRerunButton.current = false;
              setUpgradeCheckModal(true);
            } else {
              void startUpgrade();
            }
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
            redirectToAdminView("stackVersions");
          }}
        />
      )}
      {hideVersion && (
        <Modal
          modalTitle="Hide Repository Version"
          isOpen
          onClose={() => {
            if (!hideInProgress) setHideVersion(undefined);
          }}
          modalBody={`Hide ${hideVersion.repository_versions[0]?.RepositoryVersions.display_name || "this repository version"}? It can be restored from Manage Versions.`}
          options={{
            cancelableViaBtn: true,
            cancelableViaIcon: !hideInProgress,
            okButtonText: hideInProgress ? "HIDING..." : "HIDE",
            cancelButtonText: "CANCEL",
            okButtonDisabled: hideInProgress,
          }}
          successCallback={hideRepositoryVersion}
        />
      )}

      { confirmRevertPatchUpradeModal && (
        <Modal 
          isOpen={confirmRevertPatchUpradeModal}
          onClose={() => setConfirmRevertPatchUpgradeModal(false)}
          modalTitle={translate("popup.confirmation.commonHeader")}
          modalBody={getRevertPatchUpgradeModalBody()}
          options={{
            okButtonText: get(messages, "admin.stackUpgrade.revertPatch.okButton"),
            cancelableViaIcon: true,
            cancelableViaBtn: true,
            modalSize: "modal-sm",
            okButtonDisabled: startInProgress,
          }}
          successCallback={() => {
            setConfirmRevertPatchUpgradeModal(false);
            revertPatchUpgrade();
          }}
        />
        )
      }

      {/* Reinstall Out of Sync Components Confirmation Modal */}
      {confirmReinstallModal && stackVersionError && (
        <Modal
          isOpen={confirmReinstallModal}
          onClose={() => setConfirmReinstallModal(false)}
          modalTitle={translate("admin.stackVersions.version.errors.outOfSync.reinstall.title")}
          modalBody={
            <div>
              <p>
                {translate("hosts.host.maintainance.reinstallFailedComponents.context")}
              </p>
              <p>
                <strong>Affected Hosts ({stackVersionError.outOfSyncHosts.length}):</strong>
              </p>
              <div>
                {stackVersionError.outOfSyncHosts.map((host: string, index: number) => (
                  <div key={index}>{host}</div>
                ))}
              </div>
            </div>
          }
          options={{
            cancelableViaBtn: true,
            cancelableViaIcon: true,
            okButtonText: translate("common.reinstall"),
            cancelButtonText: translate("common.cancel"),
            okButtonDisabled: outOfSyncActionInProgress,
          }}
          successCallback={() => {
            handleReinstallOutOfSyncComponents();
          }}
        />
      )}

      {/* Remove Out of Sync Components Confirmation Modal */}
      {confirmRemoveModal && stackVersionError && (
        <Modal
          isOpen={confirmRemoveModal}
          onClose={() => setConfirmRemoveModal(false)}
          modalTitle={translate("admin.stackVersions.version.errors.outOfSync.remove.title")}
          modalBody={
            <div>
              <p>
                {translate("hosts.host.maintainance.removeFailedComponents.context")}
              </p>
              <div className="alert alert-danger">
                <FontAwesomeIcon icon={faExclamationTriangle} className="me-2" />
                <strong>Warning:</strong> This action will permanently remove failed components from the selected hosts.
              </div>
              <p>
                <strong>Affected Hosts ({stackVersionError.outOfSyncHosts.length}):</strong>
              </p>
              <div>
                {stackVersionError.outOfSyncHosts.map((host: string, index: number) => (
                  <div key={index}>{host}</div>
                ))}
              </div>
            </div>
          }
          options={{
            cancelableViaBtn: true,
            cancelableViaIcon: true,
            okButtonText: translate("common.remove"),
            cancelButtonText: translate("common.cancel"),
            okButtonDisabled: outOfSyncActionInProgress,
          }}
          successCallback={() => {
            handleRemoveOutOfSyncComponents();
          }}
        />
      )}
    </>
  );
}
