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

import { useContext, useEffect, useRef, useState } from "react";
import {
  Button,
  ButtonGroup,
  Dropdown,
  OverlayTrigger,
  Tooltip,
} from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faEdit, faExternalLink } from "@fortawesome/free-solid-svg-icons";
import VersionsApi from "../../../api/VersionsApi";
import toast from "react-hot-toast";
import Spinner from "../../../components/Spinner";
import Table from "../../../components/Table";
import { get } from "lodash";
import Select from "react-select";
import Modal from "../../../components/Modal";
import usePolling from "../../../hooks/usePolling";
import { Link } from "react-router-dom";
import { AppContext } from "../../../store/context";
import { RequestApi } from "../../../api/requestApi";
import OperationsProgress from "../../../components/OperationProgress";
import { StackVersion } from "./types";

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

export default function Versions() {
  const [loading, setLoading] = useState(false);
  const [options, setOptions] = useState(initialOptions);
  const [selectedOption, setSelectedOption] = useState(options[0]);
  const [services, setServices] = useState<string[]>([]);
  const [originalStacks, setOriginalStacks] = useState<StackVersion[]>([]);
  const [stacks, setStacks] = useState<StackVersion[]>([]);
  const { clusterName } = useContext(AppContext);

  const [versionModal, setVersionModal] = useState(false);
  const [installPackagesModal, setInstallPackagesModal] = useState(false);
  const [manageVersionModal, setManageVersionsModal] = useState(false);
  const [repoModal, setRepoModal] = useState(false);
  const [hostModal, setHostModal] = useState(false);
  const [, setCompletionStatus] = useState(false);

  const [selectedStack, setSelectedStack] = useState<
    StackVersion | undefined
  >();
  const [operations, setOperations] = useState({});
  const [payload, setPayload] = useState({});

  const hostModalContent = useRef("");
  const hostModalTitle = useRef("");

  const {} = usePolling(fetchServices, 1000);

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

  const handleSelectChange = (selected: any) => {
    const option = options.find((o) => o.key === selected.value);
    if (option) {
      setSelectedOption(option);
    }
  };

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
            <Button variant="success">Upgrade</Button>
            <Dropdown.Toggle split variant="success" id="dropdown" />
            <Dropdown.Menu>
              <Dropdown.Item
                onClick={() => installPackagesPayloadSet(stackData)}
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
    switch (getButtonName(stackData)) {
      case "CURRENT":
        return;
      case "UPGRADE":
        return "show upgrade modal";
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
    if (selectedStack) {
      selectedStack.ClusterStackVersions.state = "INTERMEDIATE";
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
      selectedStack?.repository_versions?.[0]?.RepositoryVersions
        ?.display_name || "N/A";
    return (
      <div>
        You are about to install packages for version
        <strong>{displayName}</strong> on all hosts.
      </div>
    );
  }

  function getVersionModalBody() {
    if (!selectedStack || Object.keys(selectedStack).length === 0) {
      return null;
    }

    const hostStates = selectedStack.ClusterStackVersions.host_states;
    const installed = hostStates.INSTALLED.length;
    const current = hostStates.CURRENT.length;
    const notInstalled =
      Object.values(hostStates).flat().length - installed - current;

    return (
      <div>
        <div className="d-flex justify-content-center">
          <div className="fw-bold">
            {selectedStack?.repository_versions[0]?.RepositoryVersions
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
            {selectedStack?.repository_versions?.[0]?.RepositoryVersions
              ?.repository_version || "NA"}
            )
          </small>
        </div>
        <div className="text-center">
          {getButtonName(selectedStack) === "installing" ||
          getButtonName(selectedStack) === "intermediateInstalling" ? (
            <Link to={""}>
              <OperationsProgress
                operations={operations as any}
                title="install packages"
                description="install packages"
                setCompletionStatus={setCompletionStatus}
              />
            </Link>
          ) : getButtonName(selectedStack) === "UPGRADE" ? (
            <Dropdown as={ButtonGroup}>
              <Button variant="success">Upgrade</Button>
              <Dropdown.Toggle split variant="success" id="dropdown" />
              <Dropdown.Menu>
                <Dropdown.Item
                  onClick={() => installPackagesPayloadSet(selectedStack)}
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
              onClick={() => handleUpgradeButton(selectedStack)}
            >
              {getButtonName(selectedStack)}
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
      selectedStack?.repository_versions[0]?.RepositoryVersions?.display_name ||
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
    </>
  );
}
