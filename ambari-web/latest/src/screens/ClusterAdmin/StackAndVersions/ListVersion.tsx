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

import { JSX, useContext, useEffect, useRef, useState } from "react";
import {
  Button,
  ButtonGroup,
  Dropdown,
  Form,
  OverlayTrigger,
  Tooltip,
} from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faBolt,
  faDashboard,
  faEdit,
  faExternalLink,
  faTimes,
  faWarning,
  IconDefinition,
} from "@fortawesome/free-solid-svg-icons";
import VersionsApi from "../../../api/VersionsApi";
import toast from "react-hot-toast";
import Spinner from "../../../components/Spinner";
import Table from "../../../components/Table";
import { cloneDeep, get, set } from "lodash";
import Select from "react-select";
import Modal from "../../../components/Modal";
import usePolling from "../../../hooks/usePolling";
import { Link } from "react-router-dom";
import { AppContext } from "../../../store/context";
import { RequestApi } from "../../../api/requestApi";
import OperationsProgress from "../../../components/OperationProgress";
import { ClusterCheckPopupData, Item, Response, StackVersion } from "./types";
import { translate, translateWithVariables } from "../../../Utils/Utility";
import modalManager from "../../../store/ModalManager";
import ClusterApi from "../../../api/clusterApi";
import Upgrade from "./Upgrade";

const initialOptions = [
  { key: "ALL", values: ["ALL"], count: 0 },
  {
    key: "NOT INSTALLED",
    values: ["INSTALL_FAILED", "INSTALLING", "NOT_REQUIRED"],
    count: 0,
  },
  { key: "UPGRADE READY", values: ["UPGRADE_READY"], count: 0 },
  { key: "CURRENT", values: ["CURRENT"], count: 0 },
  { key: "INSTALLED", values: ["INSTALLED"], count: 0 },
  {
    key: "UPGRADE/DOWNGRADE IN PROGRESS",
    values: ["Upgrade/Downgrade in Progress"],
    count: 0,
  },
  { key: "READY TO FINALIZE", values: ["Ready to Finalize"], count: 0 },
];

const initialUpgradeMethods = [
  {
    displayName: translate(
      "admin.stackVersions.version.upgrade.upgradeOptions.RU.title"
    ),
    type: "ROLLING",
    icon: "faDashboard",
    description: translate(
      "admin.stackVersions.version.upgrade.upgradeOptions.RU.description"
    ),
    selected: false,
    allowed: true,
    isCheckComplete: false,
    isCheckRequestInProgress: false,
    precheckResultsMessage: "",
    preCheckResultsModalContent: null as JSX.Element | null,
    precheckResultsTitle: "",
    action: "",
    isWizardRestricted: false,
  },
  {
    displayName: translate(
      "admin.stackVersions.version.upgrade.upgradeOptions.EU.title"
    ),
    type: "NON_ROLLING",
    icon: "faBolt",
    description: translate(
      "admin.stackVersions.version.upgrade.upgradeOptions.EU.description"
    ),
    selected: false,
    allowed: true,
    isCheckComplete: false,
    isCheckRequestInProgress: false,
    precheckResultsMessage: "",
    preCheckResultsModalContent: null as JSX.Element | null,
    precheckResultsTitle: "",
    action: "",
    isWizardRestricted: false,
  },
  {
    displayName: translate(
      "admin.stackVersions.version.upgrade.upgradeOptions.HOU.title"
    ),
    type: "HOST_ORDERED",
    icon: "faBolt",
    description: "",
    selected: false,
    allowed: false,
    isCheckComplete: false,
    isCheckRequestInProgress: false,
    precheckResultsMessage: "",
    preCheckResultsModalContent: null as JSX.Element | null,
    precheckResultsTitle: "",
    action: "",
    cantBeStarted: true,
  },
];

const iconMapping: { [key: string]: IconDefinition } = {
  faDashboard: faDashboard,
  faBolt: faBolt,
};

export default function Versions() {
  const [loading, setLoading] = useState(false);
  const [options, setOptions] = useState(initialOptions);
  const [selectedOption, setSelectedOption] = useState(options[0]);
  const [services, setServices] = useState<string[]>([]);
  const [originalStacks, setOriginalStacks] = useState<StackVersion[]>([]);
  const [stacks, setStacks] = useState<StackVersion[]>([]);
  const [upgradeMethods, setUpgradeMethods] = useState(initialUpgradeMethods);
  const [methodType, setMethodType] = useState("");
  const [currentUpgradeTypes, setCurrentUpgradeTypes] = useState<string[]>([]);
  const { clusterName, setUpgradeId, upgradeState, setUpgradeVersionDisplayName } = useContext(AppContext);
  
  const [versionModal, setVersionModal] = useState(false);
  const [installPackagesModal, setInstallPackagesModal] = useState(false);
  const [manageVersionModal, setManageVersionsModal] = useState(false);
  const [repoModal, setRepoModal] = useState(false);
  const [hostModal, setHostModal] = useState(false);
  const [upgradeModal, setUpgradeModal] = useState(false);
  const [upgradeCheckModal, setUpgradeCheckModal] = useState(false);
  const [upgradeConfirmationModal, setUpgradeConfirmationModal] = useState(false);
  const [, setCompletionStatus] = useState(false);
  const [selectedStack, setSelectedStack] = useState<StackVersion>();
  const selectedStackRef = useRef<StackVersion>(selectedStack);
  const [operations, setOperations] = useState({});
  const [payload, setPayload] = useState({});

  const hostModalContent = useRef("");
  const hostModalTitle = useRef("");
  const upgradeModalContent = useRef<JSX.Element | null>(null);
  const upgradeMethodsRef = useRef(initialUpgradeMethods);
  const upgradeOkButton = useRef<Boolean>(true);
  const showRerunButton = useRef<Boolean>(true);
  const showUpgradeProceedButton = useRef<boolean>(true);

  const {} = usePolling(fetchServices, 6000);

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
        setOriginalStacks(stacks);
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
  }, []);

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

    // Update the upgrade modal content when upgrade methods change
    if (upgradeModal && selectedStackRef.current) {
      const modalContent = (
        <div className="upgrade-modal">
          <div>
            You are about to perform an upgrade to{" "}
            <strong>
              {selectedStackRef.current?.repository_versions[0]?.RepositoryVersions
                ?.display_name || "Unknown"}
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
                  onClick={() => {
                    setMethodType(method.type);
                    if (method.precheckResultsMessage.includes("Required")) {
                      upgradeOkButton.current = true;
                    } else {
                      upgradeOkButton.current = false;
                    }
                  }}
                >
                  <FontAwesomeIcon
                    className="upgrade-method-icon"
                    icon={iconMapping[method.icon]}
                  />
                  <div className="upgrade-method-title">
                    {method.displayName}
                  </div>
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
                  </div>
                </div>
              ))}
          </div>

          <div className="upgrade-failure-tolerance">
            <div>Select optional upgrade failure tolerance:</div>
            <Form className="pt-2">
              <Form.Group controlId="serviceCheck">
                <Form.Check
                  type="checkbox"
                  label="Skip all Service Check failures"
                />
              </Form.Group>
              <Form.Group controlId="slaveComponentFailures">
                <Form.Check
                  type="checkbox"
                  label="Skip all Slave Component failures"
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
      upgradeModalContent.current = modalContent;
    }
  }, [upgradeMethods, upgradeModal, methodType]);

  if (loading) {
    return <Spinner />;
  }

  const columns = [
    {
      accessorKey: "name",
      header: "",
      cell: (info: any) => info.row.original,
      width: "10%",
    },
    ...stacks.map((stackData: StackVersion, index: number) => {
      return {
        accessorKey: `stack-${index}`,
        header: getStackHeader(stackData),
        cell: (info: any) => {
          const service = get(
            stackData.ClusterStackVersions.repository_summary.services,
            info.row.original,
            { version: "UNKNOWN" }
          );
          return service.version;
        },
        id: `stack-${index}`,
      };
    }),
  ];

  const handleSelectChange = (selected: any) => {
    const option = options.find((o) => o.key === selected.value);
    if (option) {
      setSelectedOption(option);
    }
  };

  function getStackHeader(stackData: StackVersion) {
    return (
      <div>
        <div>
          {stackData.repository_versions[0].RepositoryVersions.display_name}
        </div>
        <div className="mt-2">
          <small className="text-muted mt-2">
            (
            {
              stackData.repository_versions[0].RepositoryVersions
                .repository_version
            }
            )
          </small>
        </div>
        <div className="mt-2">
          <small
            className="custom-link"
            onClick={() => {
              setSelectedStack(stackData);
              selectedStackRef.current = stackData;
              setVersionModal(true);
            }}
          >
            Show Details
          </small>
        </div>
        {getButtonName(stackData) === "installing" ||
        getButtonName(stackData) === "intermediateInstalling" ? (
          <Link to={""}>
            <OperationsProgress
              operations={operations as any}
              title="install packages"
              description="install packages"
              setCompletionStatus={setCompletionStatus}
            />
          </Link>
        ) : getButtonName(stackData) === "UPGRADE" ? (
          <Dropdown as={ButtonGroup}>
            <Button
              variant="success"
              className="text-uppercase"
              size="sm"
              disabled={upgradeState !== "NOT_REQUIRED" &&
                  upgradeState !== "COMPLETED"}
              onClick={() => handleUpgradeButton(stackData)}
            >
              Upgrade
            </Button>
            <Dropdown.Toggle split variant="success" id="dropdown" />
            <Dropdown.Menu>
              <Dropdown.Item
              disabled={upgradeState !== "NOT_REQUIRED" &&
                  upgradeState !== "COMPLETED"}
                onClick={() => installPackagesPayloadSet(stackData)}
              >
                Re-install
              </Dropdown.Item>
              <Dropdown.Item
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
            className="mt-2"
            variant="success"
            size="sm"
            onClick={() => handleUpgradeButton(stackData)}
          >
            {getButtonName(stackData)}
          </Button>
        )}
      </div>
    );
  }

  function installPackagesPayloadSet(stackData: StackVersion) {
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

    setPayload(payload);
    setInstallPackagesModal(true);
  }

  function handleUpgradeButton(stackData: StackVersion) {
    setSelectedStack(stackData);
    selectedStackRef.current = stackData;
    switch (getButtonName(stackData)) {
      case "CURRENT":
        return;
      case "UPGRADE":
        handleUpgradeClick(stackData);
        return;
      default:
        installPackagesPayloadSet(stackData);
    }
  }

  function installPackages() {
    const operations = [
      {
        id: "1",
        label: "installing",
        skippable: false,
        context: "install packages",
        callback: async () => {
          const reqData = await RequestApi.installPackages(
            clusterName,
            payload
          );
          return reqData;
        },
      },
    ];
    setOperations(operations);
    let selectedStackCopy = cloneDeep(selectedStack);
    if (selectedStackCopy) {
      set(selectedStackCopy, "ClusterStackVersions.state", "INTERMEDIATE");
      setSelectedStack(selectedStackCopy);
      selectedStackRef.current = selectedStackCopy;
    }
    setInstallPackagesModal(false);
  }

  function getButtonName(stackData: StackVersion) {
    const stackState = stackData.ClusterStackVersions.state;

    switch (stackState) {
      case "CURRENT":
        return "CURRENT";
      case "INSTALL_FAILED":
        return "RE-INSTALL";
      case "INSTALLED":
        return "UPGRADE";
      case "INSTALLING":
        return "installing";
      case "INTERMEDIATE":
        return "intermediateInstalling";
      default:
        return "INSTALL_PACKAGES";
    }
  }

  function getInstallPackageModalBody() {
    const displayName =
      selectedStackRef.current?.repository_versions?.[0]?.RepositoryVersions
        ?.display_name || "N/A";
    return (
      <div>
        You are about to install packages for version
        <strong>{displayName}</strong> on all hosts.
      </div>
    );
  }

  function getVersionModalBody() {
    if (!selectedStackRef.current || Object.keys(selectedStackRef.current).length === 0) {
      return null;
    }

    const hostStates = selectedStackRef.current.ClusterStackVersions.host_states;
    const installed = hostStates.INSTALLED.length;
    const current = hostStates.CURRENT.length;
    const notInstalled =
      Object.values(hostStates).flat().length - installed - current;

    return (
      <div>
        <div className="d-flex justify-content-center">
          <div className="fw-bold">
            {selectedStackRef.current?.repository_versions[0]?.RepositoryVersions
              ?.display_name || "NA"}
          </div>
          <small className="mx-2">
            <OverlayTrigger
              placement="top"
              delay={{ show: 250, hide: 400 }}
              overlay={<Tooltip>Click to Edit Repositories</Tooltip>}
            >
              <FontAwesomeIcon
                onClick={() => setRepoModal(true)}
                icon={faEdit}
              />
            </OverlayTrigger>
          </small>
        </div>
        <div className="mt-2 text-center">
          <small className="text-muted mt-2">
            (
            {selectedStackRef.current?.repository_versions?.[0]?.RepositoryVersions
              ?.repository_version || "NA"}
            )
          </small>
        </div>
        <div className="text-center">
          {getButtonName(selectedStackRef.current) === "installing" ||
          getButtonName(selectedStackRef.current) === "intermediateInstalling" ? (
            <Link to={""}>
              <OperationsProgress
                operations={operations as any}
                title="install packages"
                description="install packages"
                setCompletionStatus={setCompletionStatus}
              />
            </Link>
          ) : getButtonName(selectedStackRef.current) === "UPGRADE" ? (
            <Dropdown as={ButtonGroup}>
              <Button variant="success">Upgrade</Button>
              <Dropdown.Toggle split variant="success" id="dropdown" />
              <Dropdown.Menu>
                <Dropdown.Item
                  onClick={() => {
                    if(selectedStackRef.current)
                      installPackagesPayloadSet(selectedStackRef.current)
                    }}
                >
                  Re-install
                </Dropdown.Item>
                <Dropdown.Item>Pre-upgrade check</Dropdown.Item>
              </Dropdown.Menu>
            </Dropdown>
          ) : (
            <Button
              className="mt-2"
              variant="success"
              size="sm"
              onClick={() => {
                if(selectedStackRef.current)
                handleUpgradeButton(selectedStackRef.current)
              }}
            >
              {getButtonName(selectedStackRef.current)}
            </Button>
          )}
        </div>
        <div>
          <div className="text-center mt-3">Hosts</div>
          <div className="d-flex justify-content-between mt-2">
            <div>
              <Button
                variant="link"
                onClick={() =>
                  handleHostClick(
                    "notInstalled",
                    Object.values(hostStates)
                      .flat()
                      .filter(
                        (host) =>
                          !hostStates.CURRENT.includes(host) &&
                          !hostStates.INSTALLED.includes(host)
                      )
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

    hostModalContent.current = `${versionName} is ${
      versionStatus.toLowerCase() === "current"
        ? "applied"
        : versionStatus.toLowerCase()
    } on ${hosts.length} hosts:\n\n\n${hostList}`;
    hostModalTitle.current = `Version Status: ${versionStatus}`;
    setHostModal(true);
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

    // Initial modal content - will be updated when pre-check results are available
    const modalContent = (
      <div className="upgrade-modal">
        <div>
          You are about to perform an upgrade to{" "}
          <strong>
            {stack.repository_versions[0].RepositoryVersions.display_name}
          </strong>
        </div>
        <div className="pt-1">Choose the upgrade method:</div>

        <div className="upgrade-options-container">
          {upgradeMethods
            .filter(
              (method) => method.allowed && upgradeTypes.includes(method.type)
            )
            .map((method) => (
              <div
                key={method.type}
                className={`upgrade-method ${
                  methodType === method.type ? "selected" : ""
                }`}
                onClick={() => {
                  setMethodType(method.type);
                  if (method.precheckResultsMessage.includes("Required")) {
                    upgradeOkButton.current = true;
                  } else {
                    upgradeOkButton.current = false;
                  }
                }}
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
          <div>Select optional upgrade failure tolerance:</div>
          <Form className="pt-2">
            <Form.Group controlId="serviceCheck">
              <Form.Check
                type="checkbox"
                label="Skip all Service Check failures"
              />
            </Form.Group>
            <Form.Group controlId="slaveComponentFailures">
              <Form.Check
                type="checkbox"
                label="Skip all Slave Component failures"
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
    upgradeModalContent.current = modalContent;
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
      "0":
        selectedStackRef.current?.repository_versions[0]?.RepositoryVersions
          ?.display_name || "Unknown",
    });

    const warningTitle = translate("popup.clusterCheck.Upgrade.warning.title");
    const warningAlert = translate("popup.clusterCheck.Upgrade.warning.alert");
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

    const upgradeCheckModalContent1 = (
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
            <h5>Configuration Recommendations</h5>
            <ul>
              {configsRecommendations.map((config, index) => (
                <li key={index}>
                  {config.name}: {config.currentValue} (current) vs{" "}
                  {config.recommendedValue} (recommended)
                </li>
              ))}
            </ul>
            <p>
              Review the recommendations before proceeding with the upgrade.
            </p>
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
                <span>Failed on: {failed_on}</span>
              </pre>
            </li>
          </ul>
        </>
      );
    });
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

  function runUpgradeMethodCheck(stack: StackVersion) {
    const updateMethods = upgradeMethodsRef.current.map((method) => {
      if (method.allowed) {
        runPreUpgradeCheckOnly({
          id: stack.ClusterStackVersions.id,
          label: stack.repository_versions[0].RepositoryVersions.display_name,
          type: method.type,
        });
      } else {
        return {
          ...method,
          isCheckRequestInProgress: false,
          isCheckComplete: false,
          action: "",
        };
      }
      return method;
    });
    upgradeMethodsRef.current = updateMethods;
    setUpgradeMethods(updateMethods);
  }

  function getPreCheckResultsModalContent() {
    const method = upgradeMethodsRef.current.find(
      (method) => method.type === methodType
    );
    return get(method, "preCheckResultsModalContent", null);
  }

  function getPreCheckModalTitle() {
    return `Upgrade to ${selectedStackRef.current?.repository_versions[0].RepositoryVersions.display_name}`;
  }

  function getUpgradConfirmationModalBody() {
    const stackVersion =
      selectedStackRef.current?.repository_versions[0].RepositoryVersions.display_name;
    return `You are about to perform an ${methodType} Upgrade to ${stackVersion}. This will incur cluster downtime. Are you sure you want to proceed?`;
  }

  return (
    <>
      <div className="mt-4">
        {/* TODO: Only show Manage versions button if user has CLUSTER.UPGRADE_DOWNGRADE_STACK permission */}
        <Button
          size="sm"
          variant="success"
          onClick={() => setManageVersionsModal(true)}
        >
          <FontAwesomeIcon className="mx-2" icon={faExternalLink} /> Manage
          versions
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
        <Table data={services} columns={columns} />
      </div>

      <Modal
        isOpen={versionModal}
        onClose={() => setVersionModal(false)}
        modalTitle="Version Details"
        modalBody={getVersionModalBody()}
        options={{
          okButtonText: "DISMISS",
          cancelableViaIcon: true,
        }}
        successCallback={() => setVersionModal(false)}
      />

      <Modal
        isOpen={repoModal}
        onClose={() => setRepoModal(false)}
        modalTitle="Repositories"
        modalBody="Repository details will be shown here"
        options={{
          cancelableViaBtn: true,
          okButtonText: "SAVE",
        }}
        successCallback={() => {}}
      />

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
          "go to hosts";
        }}
      />
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
            setManageVersionsModal(false);
            // TODO: Redirect to admin view
            // redirectToAdminView();
          }}
        />
      )}

      <Modal
        isOpen={upgradeModal}
        onClose={() => setUpgradeModal(false)}
        modalTitle="Upgrade Options"
        modalBody={upgradeModalContent.current}
        options={{
          cancelableViaBtn: true,
          cancelableViaIcon: true,
          okButtonText: "PROCEED",
          cancelButtonText: "CANCEL",
          okButtonDisabled: !!upgradeOkButton.current,
        }}
        successCallback={() => {
          if (showUpgradeProceedButton) {
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
              const payload = JSON.stringify({
                Upgrade: {
                  repository_version_id:
                    selectedStackRef.current?.repository_versions[0].RepositoryVersions.id,
                  upgrade_type: methodType,
                  skip_failures: false,
                  skip_service_check_failures: false,
                  direction: "UPGRADE",
                },
              });
              try {
                const response = await VersionsApi.getUpgradeId(
                  payload,
                  clusterName
                );
                const upgradeId = response?.resources[0]?.Upgrade?.request_id;
                setUpgradeId(upgradeId);
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
    </>
  );
}
