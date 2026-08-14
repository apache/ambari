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

import { faRotateRight, faTrashCan } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { useContext, useEffect, useRef, useState } from "react";
import {
  Alert,
  Button,
  ButtonGroup,
  Card,
  Form,
  ProgressBar,
} from "react-bootstrap";
import Table from "../../components/Table";
import ClusterApi from "../../api/clusterApi";
import { get, isEmpty, startCase } from "lodash";
import WizardApi from "../../api/wizardApi";
import Modal from "../../components/Modal";
import Spinner from "../../components/Spinner";
import Paginator from "../../components/Paginator";
import usePagination from "../../hooks/usePagination";
import WizardFooter from "../../components/StepWizard/WizardFooter";
import { ActionTypes } from "./clusterStore/types";
import wizardSteps from "./wizardSteps";
import HostChecks, { getHostWithIssues } from "../Hosts/HostChecks";
import { useHostChecks } from "../../hooks/useHostChecks";
import { messages } from "../messages";
import { ContextWrapper } from ".";
import modalManager from "../../store/ModalManager";
import {
  addHostRegistrationTimeoutSecs,
  buildBootstrapPayload,
} from "../../Utils/hostWizard";

interface Host {
  name: string;
  bootStatus: string;
  isChecked: boolean;
  bootLog: string;
}

export enum BootStatus {
  PENDING = "PENDING",
  DONE = "DONE",
  REGISTERING = "REGISTERING",
  REGISTERED = "REGISTERED",
  FAILED = "FAILED",
  RUNNING = "RUNNING",
}

enum ShowOptions {
  ALL = "All",
  INSTALLING = "Installing",
  REGISTERING = "Registering",
  SUCCESS = "Success",
  FAIL = "Fail",
}

type Step3Props = {
  wizardName?: string;
};

export default function Step3({ wizardName = "clusterCreation" }: Step3Props) {
  const [selectedShowOption, setSelectedShowOption] = useState(ShowOptions.ALL);
  const [loading, setLoading] = useState(false);
  const [hosts, setHosts] = useState<Host[]>([]);
  const [otherRegHosts, setOtherRegHosts] = useState<string[]>([]);
  const [hostsToBeRemoved, setHostsToBeRemoved] = useState<string[]>([]);
  const [hostsToBeDisplayed, setHostsToBeDisplayed] = useState<Host[]>([]);
  const [_ambariProperties, setAmbariProperties] = useState<any>({});
  const [showRegLogModal, setShowRegLogModal] = useState(false);
  const [showSelectedHostsModal, setShowSelectedHostsModal] = useState(false);
  const [showOtherRegHostsModal, setShowOtherRegHostsModal] = useState(false);
  const [showRemoveHostsModal, setShowRemoveHostsModal] = useState(false);
  const [regLogInfo, setRegLogInfo] = useState<Host>({
    name: "",
    bootStatus: "",
    isChecked: false,
    bootLog: "",
  });
  const [showOptions, setShowOptions] = useState([
    { name: ShowOptions.ALL, count: 0 },
    { name: ShowOptions.INSTALLING, count: 0 },
    { name: ShowOptions.REGISTERING, count: 0 },
    { name: ShowOptions.SUCCESS, count: 0 },
    { name: ShowOptions.FAIL, count: 0 },
  ]);
  const [showHostCheck, setShowHostCheck] = useState(false);

  const { startHostCheck, isHostCheckRunning, hostCheckResult } = useHostChecks(
    false,
    true
  );
  const { Context } = useContext(ContextWrapper);
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: {
      currentStep,
      handleNextImperitive,
      handleBackImperitive,
    },
  }: any = useContext(Context);
  const [nextEnabled, setNextEnabled] = useState(false);
  const enableNext = () => {
    setNextEnabled(true);
  };
  const disableNext = () => {
    setNextEnabled(false);
  };

  const registrationStartedAt = useRef<any>(null);
  const registrationTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const bootstrapTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const bootstrapStarted = useRef(false);
  const isMounted = useRef(true);
  //TODO: Remove this hack
  const serviceCheckStarted = useRef(false);
  const hostsRef = useRef<Host[]>([]);

  const {
    currentItems,
    changePage,
    currentPage,
    maxPage,
    itemsPerPage,
    setItemsPerPage,
  } = usePagination(hostsToBeDisplayed);

  const bootStatusFilterMapping = {
    [ShowOptions.ALL]: [
      BootStatus.PENDING,
      BootStatus.DONE,
      BootStatus.REGISTERING,
      BootStatus.REGISTERED,
      BootStatus.FAILED,
      BootStatus.RUNNING,
    ],
    [ShowOptions.INSTALLING]: [BootStatus.PENDING, BootStatus.DONE],
    [ShowOptions.REGISTERING]: [BootStatus.REGISTERING],
    [ShowOptions.SUCCESS]: [BootStatus.REGISTERED],
    [ShowOptions.FAIL]: [BootStatus.FAILED],
  };

  useEffect(() => {
    getAmbariProperties();
  }, []);

  useEffect(() => {
    if (
      !isEmpty(get(state, `${wizardName}Steps.${wizardSteps[2].name}.data`, {}))
    ) {
      loadHosts();
    }
  }, [state]);

  useEffect(() => {
    hostsRef.current = hosts;
    if (hostsRef.current.length) {
      const installOptions = get(
        state,
        `${wizardName}Steps.${wizardSteps[2].name}.data`,
        {},
      );
      if (installOptions.isSshRegistration) {
        if (!bootstrapStarted.current && hostsRef.current.some(
          (host) => host.bootStatus === BootStatus.PENDING,
        )) {
          void startAutomaticBootstrap(installOptions);
        } else if (
          !registrationStartedAt.current
          && hostsRef.current.some((host) => host.bootStatus === BootStatus.DONE)
        ) {
          startRegistration();
        }
      } else if (!registrationStartedAt.current) {
        startRegistration();
      }

    }
    let showOptionsTemp = [...showOptions];
    showOptionsTemp.forEach((option) => {
      option.count = hostsRef.current.filter((host) =>
        bootStatusFilterMapping[option.name].includes(
          host.bootStatus as BootStatus
        )
      ).length;
    });
    setShowOptions(showOptionsTemp);
    setHostsToBeDisplayed(hosts);
    applyFilters();
  }, [hosts]);

  useEffect(() => () => {
    isMounted.current = false;
    if (registrationTimer.current) {
      clearTimeout(registrationTimer.current);
    }
    if (bootstrapTimer.current) {
      clearTimeout(bootstrapTimer.current);
    }
  }, []);

  useEffect(() => {
    applyFilters();
  }, [selectedShowOption]);

  useEffect(() => {
    if (!serviceCheckStarted.current) {
      beginHostsCheck();
    }
  }, [JSON.stringify(hosts.map((host) => host.bootStatus))]);

  const applyFilters = () => {
    changePage(1);
    let filteredHosts = hostsRef.current.filter((host) =>
      bootStatusFilterMapping[selectedShowOption].includes(
        host.bootStatus as BootStatus
      )
    );
    setHostsToBeDisplayed(filteredHosts);
  };

  const isRegistrationInProgress = () => {
    return !!hostsRef.current.find(
      (host) =>
        ![BootStatus.REGISTERED, BootStatus.FAILED].includes(
          host.bootStatus as BootStatus
        )
    );
  };

  const beginHostsCheck = () => {
    if (!isRegistrationInProgress() && hostsRef.current.length > 0) {
      startHostCheckLocal(hostsRef.current);
      serviceCheckStarted.current = true;
    }
  };

  const startRegistration = () => {
    if (registrationStartedAt.current === null) {
      registrationStartedAt.current = Date.now();
      isHostsRegistered();
    }
  };

  const failRegistrationHosts = () => {
    setHosts((current) => current.map((host) =>
      host.bootStatus === BootStatus.REGISTERING
        ? {
            ...host,
            bootStatus: BootStatus.FAILED,
            bootLog: `${host.bootLog || ""}Registration with the server failed.\n\n`,
          }
        : host,
    ));
  };

  const scheduleRegistrationPoll = (installOptions: any) => {
    const timeoutSecs = addHostRegistrationTimeoutSecs(
      Boolean(installOptions.isSshRegistration),
    );
    const bootstrapStillRunning = hostsRef.current.some(
      (host) => host.bootStatus === BootStatus.RUNNING,
    );
    const withinTimeout = registrationStartedAt.current !== null
      && Date.now() - registrationStartedAt.current < timeoutSecs * 1000;
    if (bootstrapStillRunning || withinTimeout) {
      if (registrationTimer.current) {
        clearTimeout(registrationTimer.current);
      }
      registrationTimer.current = setTimeout(() => void isHostsRegistered(), 3000);
    } else {
      failRegistrationHosts();
    }
  };

  const failBootstrapHosts = (message: string) => {
    if (!isMounted.current) {
      return;
    }
    setHosts((current) => current.map((host) =>
      [BootStatus.PENDING, BootStatus.RUNNING].includes(host.bootStatus as BootStatus)
        ? { ...host, bootStatus: BootStatus.FAILED, bootLog: message }
        : host,
    ));
  };

  const pollBootstrap = async (requestId: string) => {
    try {
      const response = await WizardApi.getBootstrapStatus(requestId);
      if (!isMounted.current) {
        return;
      }
      const statuses = Array.isArray(response.hostsStatus)
        ? response.hostsStatus
        : response.hostsStatus
          ? [response.hostsStatus]
          : [];
      setHosts((current) => current.map((host) => {
        const update = statuses.find((item: any) => item.hostName === host.name);
        if (!update) {
          return response.status === "ERROR" && host.bootStatus !== BootStatus.REGISTERED
            ? { ...host, bootStatus: BootStatus.FAILED, bootLog: response.log || "Bootstrap failed." }
            : host;
        }
        const status = [BootStatus.DONE, BootStatus.FAILED, BootStatus.RUNNING]
          .includes(update.status)
          ? update.status
          : host.bootStatus;
        return {
          ...host,
          bootStatus: status,
          bootLog: update.log || host.bootLog,
        };
      }));
      const terminal = response.status === "ERROR"
        || (statuses.length > 0 && statuses.every((item: any) =>
          [BootStatus.DONE, BootStatus.FAILED].includes(item.status),
        ));
      if (!terminal) {
        bootstrapTimer.current = setTimeout(() => void pollBootstrap(requestId), 3000);
      }
    } catch (error: any) {
      bootstrapStarted.current = false;
      failBootstrapHosts(
        error?.response?.data?.message || "Ambari could not bootstrap the selected hosts.",
      );
    }
  };

  const startAutomaticBootstrap = async (installOptions: any) => {
    bootstrapStarted.current = true;
    setHosts((current) => current.map((host) =>
      host.bootStatus === BootStatus.PENDING
        ? { ...host, bootStatus: BootStatus.RUNNING, bootLog: "Setting up Ambari Agent...\n\n" }
        : host,
    ));
    try {
      const response = await WizardApi.launchBootstrap(buildBootstrapPayload({
        agentUserAccount: installOptions.agentUserAccount,
        customizeAgentUserAccount: Boolean(installOptions.customizeAgentUserAccount),
        hosts: installOptions.targetHosts || [],
        sshKey: installOptions.sshKey,
        sshPortNumber: installOptions.sshPortNumber,
        sshUserAccount: installOptions.sshUserAccount,
      }));
      const requestId = String(response.requestId ?? "");
      if (!requestId || response.status === "ERROR") {
        bootstrapStarted.current = false;
        failBootstrapHosts(response.log || "Ambari could not start host bootstrap.");
        return;
      }
      await pollBootstrap(requestId);
    } catch (error: any) {
      bootstrapStarted.current = false;
      failBootstrapHosts(
        error?.response?.data?.message || "Ambari could not start host bootstrap.",
      );
    }
  };

  const isHostsRegistered = async () => {
    try {
      const response = await WizardApi.isHostsRegistered();
      if (response && isMounted.current) {
        isHostsRegisteredSuccessCallback(response);
      }
    } catch {
      if (!isMounted.current || !isRegistrationInProgress()) {
        return;
      }
      const installOptions = get(
        state,
        `${wizardName}Steps.${wizardSteps[2].name}.data`,
        {},
      );
      scheduleRegistrationPoll(installOptions);
    }
  };

  const isHostsRegisteredSuccessCallback = (data: any) => {
    if (isEmpty(otherRegHosts)) {
      const inputtedHosts = hostsRef.current.map((host) => host.name);
      const registeredHosts = data.items.map((item: any) =>
        get(item, "Hosts.host_name")
      );
      const installedHosts = get(
        state,
        `${wizardName}Steps.${wizardSteps[2].name}.data.installedHosts`,
        []
      );
      setOtherRegHosts(
        registeredHosts.filter(
          (host: string) =>
            !inputtedHosts.includes(host) && !installedHosts.includes(host)
        )
      );
    }
    let updatedHosts = hostsRef.current.map((_host) => {
      let updatedHost = { ..._host };
      if (updatedHost.bootStatus === BootStatus.DONE) {
        updatedHost.bootStatus = BootStatus.REGISTERING;
        updatedHost.bootLog =
          (updatedHost.bootLog || "") + "Registering with the server...\n\n";
        registrationStartedAt.current = Date.now();
      } else if (
        updatedHost.bootStatus === BootStatus.REGISTERING &&
        data.items.find(
          (item: any) =>
            get(item, "Hosts.host_name") === updatedHost.name &&
            get(item, "Hosts.host_status") !== "UNKNOWN"
        )
      ) {
        updatedHost.bootStatus = BootStatus.REGISTERED;
        updatedHost.bootLog =
          (updatedHost.bootLog || "") + "Registered with the server.\n\n";
      }
      return updatedHost;
    });

    hostsRef.current = updatedHosts;
    setHosts(updatedHosts);
    if (isRegistrationInProgress()) {
      const installOptions = get(
        state,
        `${wizardName}Steps.${wizardSteps[2].name}.data`,
        {},
      );
      scheduleRegistrationPoll(installOptions);
    }
  };

  const loadHosts = () => {
    const hostInfo = get(
      state,
      `${wizardName}Steps.${wizardSteps[2].name}.data.targetHosts`,
      []
    );
    let allHosts: any = [];
    const bootStatus = get(
      state,
      `${wizardName}Steps.${wizardSteps[2].name}.data.isSshRegistration`,
      "true"
    )
      ? "PENDING"
      : "DONE";
    hostInfo.forEach((host: string) => {
      allHosts.push({
        name: host,
        bootStatus: bootStatus,
        isChecked: false,
        bootLog: "",
      });
    });
    setHosts(allHosts);
  };

  const retryHostRegistration = () => {
    let updatedHosts = hostsRef.current.map((_host) => {
      let updatedHost = { ..._host };
      if (updatedHost.bootStatus === BootStatus.FAILED) {
        const automatic = get(
          state,
          `${wizardName}Steps.${wizardSteps[2].name}.data.isSshRegistration`,
          true,
        );
        updatedHost.bootStatus = automatic ? BootStatus.PENDING : BootStatus.DONE;
        updatedHost.bootLog = "Retrying ...\n\n";
      }
      return updatedHost;
    });
    setHosts(updatedHosts);
    registrationStartedAt.current = null;
    bootstrapStarted.current = false;
  };

  const startHostCheckLocal = (hostsList: Host[]) => {
    if (
      hostsList.length > 0 &&
      !hostsList.every((host) => host.bootStatus === BootStatus.FAILED)
    ) {
      disableNext();
      startHostCheck(getRegisteredHosts(hostsList));
    }
  };

  const removeHosts = () => {
    let tempOtherRegHosts = [...otherRegHosts];
    const updatedHosts = hostsRef.current.filter((host) => {
      if (hostsToBeRemoved.includes(host.name)) {
        if (host.bootStatus === BootStatus.REGISTERED) {
          tempOtherRegHosts.push(host.name);
        }
        return false;
      }
      return true;
    });
    setOtherRegHosts(tempOtherRegHosts);
    setHosts(updatedHosts);
    setHostsToBeRemoved([]);
  };

  const getAmbariProperties = async () => {
    setLoading(true);
    const response = await ClusterApi.loadAmbariProperties();
    setAmbariProperties(response);
    setLoading(false);
  };

  const handleClearSelection = () => {
    const updatedHosts = hostsRef.current.map((host) => {
      return {
        ...host,
        isChecked: false,
      };
    });
    setHosts(updatedHosts);
  };

  const isAllSelected = () => {
    if (!hostsToBeDisplayed.length) return false;
    return hostsToBeDisplayed.every((host) => host.isChecked);
  };

  const handleSelectAll = () => {
    const newSelectValue = !isAllSelected();
    const updatedHosts = hostsRef.current.map((host) => {
      if (hostsToBeDisplayed.includes(host)) {
        return {
          ...host,
          isChecked: newSelectValue,
        };
      }
      return host;
    });
    setHosts(updatedHosts);
  };

  const handleSelect = (host: Host) => {
    const updatedHosts = hostsRef.current.map((_host) => {
      if (_host.name === host.name) {
        return {
          ..._host,
          isChecked: !_host.isChecked,
        };
      }
      return _host;
    });
    setHosts(updatedHosts);
  };

  const isAnyFailed = () => {
    return (
      hostsRef.current.find((host) => host.bootStatus === BootStatus.FAILED) !==
      undefined
    );
  };

  const getNumberOfSelectedHosts = () => {
    return hostsRef.current.filter((host) => host.isChecked).length;
  };

  const getModalProps = () => {
    if (showRemoveHostsModal) {
      return {
        modalTitle: "Confirmation",
        modalBody: "Are you sure you want to remove the selected host(s)?",
        onClose: () => setShowRemoveHostsModal(false),
        isOpen: showRemoveHostsModal,
        successCallback: () => {
          removeHosts();
          setShowRemoveHostsModal(false);
        },
        options: {
          cancelableViaBtn: true,
          cancelableViaIcon: true,
        },
      };
    } else if (showSelectedHostsModal) {
      return {
        modalTitle: "Selected Hosts",
        modalBody: hosts
          .filter((host) => host.isChecked)
          .map((host) => host.name)
          .join("\n\n"),
        onClose: () => setShowSelectedHostsModal(false),
        isOpen: showSelectedHostsModal,
        successCallback: () => setShowSelectedHostsModal(false),
        options: {
          cancelableViaBtn: false,
          cancelableViaIcon: true,
        },
      };
    } else if (showOtherRegHostsModal) {
      return {
        modalTitle: `${otherRegHosts.length} Other Registered Hosts`,
        modalBody:
          `These are the hosts that have registered with the server, but do not appear in the list of hosts that you are adding.\n\n` +
          otherRegHosts.join("\n\n"),
        onClose: () => setShowOtherRegHostsModal(false),
        isOpen: showOtherRegHostsModal,
        successCallback: () => setShowOtherRegHostsModal(false),
        options: {
          cancelableViaBtn: false,
          cancelableViaIcon: true,
        },
      };
    } else {
      return {
        modalTitle: `Registration log for ${regLogInfo?.name}`,
        modalBody: regLogInfo?.bootLog,
        onClose: () => setShowRegLogModal(false),
        isOpen: showRegLogModal,
        successCallback: () => setShowRegLogModal(false),
        options: {
          cancelableViaBtn: false,
          cancelableViaIcon: true,
        },
      };
    }
  };

  const columnsInHostsList = [
    {
      header: (
        <Form.Check
          type="checkbox"
          id="select-all-hosts-step3"
          checked={isAllSelected()}
          onChange={handleSelectAll}
          className="custom-checkbox"
        />
      ),
      id: "selectAll",
      width: "1%",
      cell: (info: any) => {
        const hostName = get(info, "row.original.name");
        const checkboxId = `host-step3-checkbox-${hostName}`;
        return (
          <Form.Check
            type="checkbox"
            id={checkboxId}
            checked={get(info, "row.original.isChecked")}
            onChange={() => handleSelect(get(info, "row.original"))}
            className="custom-checkbox"
          />
        );
      },
    },
    {
      header: "Host",
      accessorKey: "name",
      id: "host",
    },
    {
      header: "Progress",
      width: "15%",
      cell: (info: any) => {
        let currentStatus = get(info, "row.original.bootStatus", "");
        if (currentStatus === BootStatus.REGISTERED) {
          return <ProgressBar variant="success" now={100} />;
        } else if (currentStatus === BootStatus.FAILED) {
          return <ProgressBar variant="danger" now={100} />;
        }
        return <ProgressBar striped variant="info" animated now={100} />;
      },
    },
    {
      header: "Status",
      cell: (info: any) => {
        let currentStatus = get(info, "row.original.bootStatus", "");
        let statusText =
          currentStatus === BootStatus.REGISTERED
            ? "Success"
            : startCase(currentStatus.toLowerCase());
        return (
          <span
            onClick={() => {
              setRegLogInfo(info.row.original);
              setShowRegLogModal(true);
            }}
            className={
              currentStatus === BootStatus.REGISTERED
                ? "text-success make-link"
                : currentStatus === BootStatus.FAILED
                ? "text-danger make-link"
                : "text-info make-link"
            }
          >
            {statusText}
          </span>
        );
      },
    },
    {
      header: "Action",
      cell: (info: any) => {
        return (
          <div>
            <Button
              className={
                isRegistrationInProgress() || isHostCheckRunning
                  ? "btn-wrapping-icon text-muted disabled-btn"
                  : "btn-wrapping-icon text-muted"
              }
              onClick={() => {
                if (!isRegistrationInProgress()) {
                  setHostsToBeRemoved([get(info, "row.original.name")]);
                  setShowRemoveHostsModal(true);
                }
              }}
              disabled={isRegistrationInProgress() || isHostCheckRunning}
            >
              <FontAwesomeIcon icon={faTrashCan} />
            </Button>
          </div>
        );
      },
      width: "5%",
    },
  ];

  const getUnregisteredHosts = (hostsList: Host[]) => {
    return hostsList.length - getRegisteredHosts(hostsList).length;
  };

  const getRegisteredHosts = (hostsList: Host[]) => {
    const hostNames = hostsList
      .filter((host: Host) => {
        return host.bootStatus === BootStatus.REGISTERED;
      })
      .map((host: Host) => {
        return {
          name: host.name,
        };
      });
    return hostNames;
  };

  const getHostCheckResultAlert = () => {
    if (!serviceCheckStarted.current) {
      return null;
    }

    if (isHostCheckRunning) {
      return (
        <Alert variant="info" className="mt-2">
          Please wait while the hosts are being checked for potential
          problems...
          <Spinner />
        </Alert>
      );
    }
    const hostWithIssues = getHostWithIssues(hostCheckResult).length;
    const registeredHosts = getRegisteredHosts(hostsRef.current).length;
    const unregisteredHosts = getUnregisteredHosts(hostsRef.current);

    if (!registeredHosts) {
      return (
        <Alert variant="warning" className="mt-2">
          {get(messages, "installer.step3.warnings.allFailed").replace(
            "{0}",
            unregisteredHosts
          )}
        </Alert>
      );
    }

    if (!nextEnabled) {
      enableNext();
    }

    if (hostWithIssues) {
      return (
        <Alert variant="warning" className="mt-2">
          <div>
            {get(messages, "installer.step3.warnings.fails").replace(
              "{0}",
              registeredHosts
            )}
            <span
              onClick={() => {
                setShowHostCheck(true);
              }}
              className="ps-2 custom-link"
            >
              {get(messages, "installer.step3.warnings.linkText")}
            </span>
          </div>
        </Alert>
      );
    } else if (unregisteredHosts) {
      return (
        <Alert variant="warning" className="mt-2">
          <div>
            {get(messages, "installer.step3.warnings.someWarnings")
              .replace("{0}", registeredHosts)
              .replace("{1}", unregisteredHosts)}
            <span
              onClick={() => {
                setShowHostCheck(true);
              }}
              className="ps-2 custom-link"
            >
              {get(messages, "installer.step3.warnings.linkText")}
            </span>
          </div>
        </Alert>
      );
    } else {
      return (
        <Alert variant="success" className="mt-2">
          {get(messages, "installer.step3.warnings.noWarnings").replace(
            "{0}",
            registeredHosts
          )}
          <span
            onClick={() => {
              setShowHostCheck(true);
            }}
            className="ps-2 custom-link"
          >
            {get(messages, "installer.step3.noWarnings.linkText")}
          </span>
        </Alert>
      );
    }
  };

  const moveToNextStep = () => {
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: currentStep.name,
        data: { hosts, otherRegHosts, hostsStatus: hostsRef.current },
      },
    });
    flushStateToDb("next");
    handleNextImperitive();
  };

  if (loading) {
    return <Spinner />;
  }

  return (
    <>
      <div>
        {showHostCheck ? (
          <HostChecks
            isOpen={showHostCheck}
            onClose={() => {
              setShowHostCheck(false);
            }}
            successCallback={() => {
              startHostCheck(getRegisteredHosts(hostsRef.current));
            }}
            loading={isHostCheckRunning}
            hostCheckResult={hostCheckResult}
          />
        ) : null}
        {showRemoveHostsModal ||
        showOtherRegHostsModal ||
        showRegLogModal ||
        showSelectedHostsModal ? (
          <Modal {...getModalProps()} />
        ) : null}
        <div>
          <h2 className="step-title">Confirm Hosts</h2>
          <p className="text-muted mb-1 step-description">
            Registering your hosts.
          </p>
          <p className="text-muted step-description ">
            Please confirm the host list and remove any hosts that you do not
            want to include in the cluster.
          </p>
          <Card className="p-4">
            <Card>
              <Card.Title className="d-flex justify-content-between align-items-center p-3">
                <div>
                  {getNumberOfSelectedHosts() > 0 ? (
                    <Button
                      className="text-white me-2"
                      onClick={() => {
                        setHostsToBeRemoved(
                          hosts
                            .filter((host) => host.isChecked)
                            .map((host) => host.name)
                        );
                        setShowRemoveHostsModal(true);
                      }}
                    >
                      <FontAwesomeIcon icon={faTrashCan} className="me-2" />
                      REMOVE SELECTED
                    </Button>
                  ) : null}
                  {isAnyFailed() ? (
                    <Button
                      variant="primary"
                      className="text-white"
                      onClick={() => retryHostRegistration()}
                    >
                      <FontAwesomeIcon icon={faRotateRight} className="me-2" />
                      RETRY FAILED
                    </Button>
                  ) : null}
                </div>
                <div>
                  <span className="me-2">Show:</span>
                  <ButtonGroup>
                    {showOptions.map((radio) => (
                      <div key={radio.name}>
                        <span className="me-1">|</span>
                        <Form.Label
                          className={`me-1 p-2 rounded-2 border-0 custom-radio-label ${
                            selectedShowOption === radio.name ? "active" : ""
                          }`}
                          onClick={() =>
                            setSelectedShowOption(radio.name as ShowOptions)
                          }
                        >
                          {radio.name}({radio.count})
                        </Form.Label>
                      </div>
                    ))}
                  </ButtonGroup>
                </div>
              </Card.Title>
              <Card.Body>
                <div className="scrollable-table">
                  <Table
                    data={currentItems}
                    columns={columnsInHostsList as any}
                  />
                  {!hostsToBeDisplayed.length ? (
                    <p>No hosts to display</p>
                  ) : null}
                </div>
                <Paginator
                  currentPage={currentPage}
                  maxPage={maxPage}
                  changePage={changePage}
                  itemsPerPage={itemsPerPage}
                  setItemsPerPage={setItemsPerPage}
                  totalItems={hostsToBeDisplayed.length}
                />
                {getNumberOfSelectedHosts() > 0 ? (
                  <div className="p-2 mt-2">
                    <span
                      className="custom-link me-2"
                      onClick={() => setShowSelectedHostsModal(true)}
                    >
                      {getNumberOfSelectedHosts()} host selected
                    </span>
                    <span className="me-2">|</span>
                    <span
                      className="custom-link"
                      onClick={() => handleClearSelection()}
                    >
                      clear selection
                    </span>
                  </div>
                ) : null}
                {!isEmpty(otherRegHosts) ? (
                  <Alert
                    variant="warning"
                    className="custom-link mt-2"
                    onClick={() => setShowOtherRegHostsModal(true)}
                  >{`${otherRegHosts.length} Other Registered Hosts`}</Alert>
                ) : null}
                {getHostCheckResultAlert()}
              </Card.Body>
            </Card>
          </Card>
        </div>
      </div>
      <WizardFooter
        step={currentStep}
        isNextEnabled={nextEnabled}
        onNext={() => {
          if (getHostWithIssues(hostCheckResult).length) {
            const modalProps = {
              modalTitle: get(
                messages,
                "installer.step3.hostWarningsPopup.hostHasWarnings.header",
                ""
              ),
              modalBody: get(
                messages,
                "installer.step3.hostWarningsPopup.hostHasWarnings",
                ""
              ),
              onClose: () => {},
              successCallback: () => {
                moveToNextStep();
                modalManager.hide();
              },
              options: {
                buttonSize: "sm" as "sm" | "lg" | undefined,
                cancelableViaIcon: true,
                cancelableViaBtn: true,
                okButtonVariant: "warning",
              },
            };
            modalManager.show(modalProps);
          } else {
            moveToNextStep();
          }
        }}
        onCancel={() => {
          flushStateToDb("cancel");
        }}
        isBackEnabled={serviceCheckStarted.current && !isHostCheckRunning}
        onBack={() => {
          flushStateToDb("back")
          handleBackImperitive();
        }}
      />
    </>
  );
}
