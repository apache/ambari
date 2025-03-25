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
/* eslint-disable no-useless-escape */
/* eslint-disable no-unsafe-optional-chaining */
/* eslint-disable @typescript-eslint/ban-ts-comment */
/* eslint-disable @typescript-eslint/no-explicit-any */
import { useContext, useEffect, useState } from "react";
import {
  Item,
  VersionDefinition,
  VersionDefinitionResponse,
} from "./types/VersionDefinitions";
import { Link, useHistory, useParams } from "react-router-dom";
import {
  Col,
  Dropdown,
  DropdownButton,
  Nav,
  Row,
  Tab,
  Form,
  OverlayTrigger,
  Alert,
  Button,
  Tooltip,
  InputGroup,
} from "react-bootstrap";
import { TransformedOperatingSystem, TransformedRepo } from "./types/Os";
import find from "lodash/find";
import set from "lodash/set";
import cloneDeep from "lodash/cloneDeep";
import isEmpty from "lodash/isEmpty";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faAdd,
  faMinus,
  faPencil,
  faQuestionCircle,
  faUndo,
} from "@fortawesome/free-solid-svg-icons";
import toast from "react-hot-toast";
import AppContent from "../../context/AppContext";
import DefaultButton from "../../components/DefaultButton";
import Table from "../../components/Table";
import ConfirmationModal from "../../components/ConfirmationModal";
import Spinner from "../../components/Spinner";
import VersionsApi from "../../api/versions";
import AddVersionModal from "../../components/AddVersionModal";
import RedhatSatelliteUsageInfo from "../../components/RedhatSatelliteInfoModal";
import LostNetworkModal from "../../components/LostNetworkModal";

enum RepositoryType {
  PUBLIC = "public",
  LOCAL = "local",
}

enum OSOperations {
  GET = "get",
  EDITALL = "editall",
}

const Register = ({ readOnly }: { readOnly: boolean }): JSX.Element => {
  const [versionDefinitions, setVersionDefinitions] = useState<Item[]>([]);
  const [selectedVersion, setSelectedVersion] = useState<any>({});
  const [networkLost, setNetworkLost] = useState<boolean>(false);
  const [showRegistrationModal, setShowRegistrationModal] =
    useState<boolean>(false);
  const [selectedStack, setSelectedStack] = useState<VersionDefinition>(
    {} as VersionDefinition
  );
  const history = useHistory();
  const [selectedChoice, setSelectedChoice] = useState<string>(
    RepositoryType.PUBLIC
  );
  const {
    cluster: { cluster_name: clusterName },
    setSelectedOption,
  } = useContext(AppContent);
  const { stack, version } = useParams<any>();
  const [versionNumber, setVersionNumber] = useState(
    version?.substring(4, version.length) || ""
  );
  const [versionValidationError, setVersionValidationError] = useState(false);
  const [showNetworkModal, setShowNetworkModal] = useState<boolean>(false);
  const [skipValidation, setSkipValidation] = useState<boolean>(false);
  const [redhatSatellite, setRedhatSatellite] = useState<boolean>(false);
  const [repoInfo, setRepoInfo] = useState<{ status: string }>(
    {} as { status: string }
  );
  const [savedRepositoryVersionDetails, setSavedRepositoryVersionDetails] =
    useState<any>({});
  const [showRepoValidationBanner, setShowRepoValidationBanner] =
    useState<boolean>(false);
  const [showRedhatInfoModal, setShowRedhatInfoModal] =
    useState<boolean>(false);
  const [showAddVersionModal, setShowAddVersionModal] = useState(false);
  const [addedVersions, setAddedVersions] = useState<{
    [key: string]: { label: string; value: string }[];
  }>({});
  const [operatingSystems, setOperatingSystems] = useState<{
    [key: string]: TransformedOperatingSystem[];
  }>({});
  const [confirmDeregister, setConfirmDeregister] = useState<boolean>(false);

  console.log("Added Versions", addedVersions, selectedStack);

  const selectNewVersion = (
    version: VersionDefinition,
    newVersion?: { label: string; value: string },
    newVersionDefinition?: VersionDefinition
  ) => {
    if (version.id) {
      const addedVersionsCopy: {
        [key: string]: { label: string; value: any }[];
      } = cloneDeep(addedVersions);
      const defaultVersion = {
        label: `${version.id} (Default Version definition)`,
        value: version,
      };
      if (addedVersionsCopy[version.id]) {
        if (newVersion)
          addedVersionsCopy[version.id] = [
            ...addedVersionsCopy[version.id],
            newVersion,
          ];
      } else {
        if (newVersion) {
          addedVersionsCopy[version.id] = [defaultVersion, newVersion];
        } else {
          addedVersionsCopy[version.id] = [defaultVersion];
        }
      }
      setAddedVersions(addedVersionsCopy);
      setSelectedVersion(newVersionDefinition ? newVersionDefinition : version);
    }
  };

  useEffect(() => {
    setSelectedOption("Versions");
    async function getVersionDefinitions() {
      const definitions: VersionDefinitionResponse =
        await VersionsApi.getVersionDefinitions();
      const sortedItems = definitions.items.sort((a: any, b: any) => {
        const versionA = parseFloat(a.VersionDefinition.id.split("-")[1]);
        const versionB = parseFloat(b.VersionDefinition.id.split("-")[1]);

        return versionB - versionA;
      });
      setVersionDefinitions(sortedItems);
      setNetworkIssues(definitions.items);
      //@ts-ignore
      setSelectedStack(sortedItems[0]?.VersionDefinition);
      //@ts-ignore
      selectNewVersion(sortedItems[0]?.VersionDefinition);
    }
    getVersionDefinitions();
  }, []);

  useEffect(() => {
    async function getReposData() {
      const stackVersion = await VersionsApi.versionsList(
        savedRepositoryVersionDetails.id,
        clusterName
      );
      const repoStackVersion = stackVersion?.items?.[0];
      setRepoInfo({
        ...repoInfo,
        status: repoStackVersion?.ClusterStackVersions.state,
      });
    }
    if (!isEmpty(savedRepositoryVersionDetails)) {
      getReposData();
    }
  }, [savedRepositoryVersionDetails]);

  useEffect(() => {
    if (!versionNumber) {
      setVersionValidationError(false);
    } else {
      // Pattern for two numbers separated by a dot
      const twoNumbersPattern = /^\d+\.\d+$/;
      // Pattern for three numbers separated by a dot and a dash
      const threeNumbersPattern = /^\d+\.\d+-\d+$/;

      // Check if the string matches any of the patterns
      if (
        twoNumbersPattern.test(versionNumber) ||
        threeNumbersPattern.test(versionNumber)
      ) {
        setVersionValidationError(false);
      } else {
        setVersionValidationError(true);
      }
    }
  }, [versionNumber]);

  useEffect(() => {
    if (redhatSatellite) {
      setShowRedhatInfoModal(true);
      const updatedOs = editAllRepos("isEditable", false);
      setOperatingSystems(updatedOs);
    } else {
      const updatedOs = editAllRepos("isEditable", true);
      setOperatingSystems(updatedOs);
    }
  }, [redhatSatellite]);

  const getOsfromRepoDetails = (oSystem: string, alreadyAddedOs: any) => {
    let operatingSystem;
    for (const addedOs of alreadyAddedOs?.[0]?.repository_versions?.[0]
      ?.operating_systems) {
      if (addedOs.OperatingSystems.os_type === oSystem) {
        operatingSystem = addedOs;
      }
    }
    return operatingSystem;
  };

  useEffect(() => {
    async function getVersionOs() {
      const versionOperatingSystems =
        await VersionsApi.getVersionOperatingSystems(
          selectedStack.stack_version
        );
      let alreadyAddedOs: any = [];
      if (readOnly) {
        const repoDetails = await VersionsApi.getRepoDetails(stack, version);
        setSavedRepositoryVersionDetails(
          repoDetails?.items?.[0]?.repository_versions?.[0]?.RepositoryVersions
        );
        alreadyAddedOs = repoDetails.items;
      }
      const allOs: TransformedOperatingSystem[] =
        versionOperatingSystems.operating_systems.map((os: any) => {
          const defaultRepos = os.repositories.map((repo: any) => {
            return {
              id: repo.Repositories.repo_id,
              defaultId: repo.Repositories.repo_id,
              baseUrl: repo?.Repositories?.base_url,
              name: repo?.Repositories?.repo_name,
              defaultUrl: repo?.Repositories?.default_base_url,
            };
          });
          const matchingOs = readOnly
            ? getOsfromRepoDetails(os.OperatingSystems.os_type, alreadyAddedOs)
            : undefined;
          const isOsAdded = readOnly ? (matchingOs ? true : false) : true;
          return {
            os: os.OperatingSystems.os_type,
            isAdded: isOsAdded,
            repos: readOnly
              ? matchingOs
                ? matchingOs.repositories.map((repo: any) => {
                    return {
                      id: repo.Repositories.repo_id,
                      defaultId: repo.Repositories.repo_id,
                      baseUrl: repo?.Repositories?.base_url,
                      name: repo?.Repositories?.repo_name,
                      defaultUrl: repo?.Repositories?.base_url,
                    };
                  })
                : defaultRepos
              : defaultRepos,
          };
        });
      const operatingSystemsCopy = cloneDeep(operatingSystems);
      operatingSystemsCopy[selectedVersion.id] = [...allOs];
      setOperatingSystems(operatingSystemsCopy);
    }
    if (
      selectedStack &&
      selectedVersion &&
      selectedVersion.id &&
      !operatingSystems[selectedVersion.id]
    ) {
      getVersionOs();
    }
  }, [selectedVersion]);

  function getColumns() {
    return [
      {
        header: "",
        accessorKey: "display_name",
        id: "name",
      },
      {
        header: "",
        id: "versions",
        cell: (info: any) => {
          const allVersions = info.row.original.versions;
          return allVersions.join(",");
        },
      },
    ];
  }

  const getSystemsWithKeyValue = (
    key: string,
    value: any,
    osOperation: string
  ) => {
    const osCopy = cloneDeep(operatingSystems);
    switch (osOperation) {
      case OSOperations.GET:
        return osCopy?.[selectedVersion.id]?.filter(
          (oSystem: TransformedOperatingSystem) =>
            oSystem[key as keyof TransformedOperatingSystem] === value
        );
      case OSOperations.EDITALL:
        return osCopy?.[selectedVersion]?.map(
          (oSystem: TransformedOperatingSystem) => {
            //@ts-ignore
            oSystem[key as keyof TransformedOperatingSystem] = value;
            return oSystem;
          }
        );
    }
  };

  const redirectToList = () => {
    history.push("/stackVersions");
  };

  const deRegisterVersion = async () => {
    if (repoInfo?.status == "CURRENT" || repoInfo?.status == "INSTALLED") {
      toast.error("The version cannot be deregistered.");
    } else {
      try {
        await VersionsApi.deleteRepositoryVersion(
          savedRepositoryVersionDetails.stack_name,
          savedRepositoryVersionDetails.stack_version,
          savedRepositoryVersionDetails.id
        );
        toast.success("Version Deleted Successfully");
        redirectToList();
      } catch (err) {
        toast.error("Version Delete Error");
      }
    }
  };

  const getAddableOperatingSystems = () => {
    const addableSystems = getSystemsWithKeyValue(
      "isAdded",
      false,
      OSOperations.GET
    );
    return addableSystems || [];
  };

  const handleModalVisibility = (show: boolean) => {
    setShowNetworkModal(show);
  };

  const setNetworkIssues = (versions: any[]) => {
    const isNetworkLost = !versions.find(
      (_version) => !_version.VersionDefinition.stack_default
    );

    if (isNetworkLost) {
      setSelectedChoice(RepositoryType.LOCAL);
      // clearRepoVersions();
    }
    setNetworkLost(isNetworkLost);
  };

  const addBackOperatingSystem = (os: TransformedOperatingSystem) => {
    const osCopy = cloneDeep(operatingSystems);
    if (osCopy?.[selectedVersion.id]) {
      const matchingOs = osCopy?.[selectedVersion.id].find(
        (oSystem) => oSystem.os === os.os
      );
      if (matchingOs) {
        matchingOs.isAdded = true;
        setOperatingSystems(osCopy);
      }
    }
  };

  const osListHeaders = [
    {
      label: "OS",
      columnCount: 3,
    },
    {
      label: "name",
      columnCount: 3,
    },
    { label: "Base URL", columnCount: 5 },
    {
      columnCount: 1,
      label: (
        <Dropdown>
          <Dropdown.Toggle
            className="btn-default"
            as={Button}
            variant="secondary"
            size="sm"
          >
            <FontAwesomeIcon className="me-2" icon={faAdd} />
            Add
          </Dropdown.Toggle>

          <Dropdown.Menu>
            {getAddableOperatingSystems()?.map((oSystem) => {
              return (
                <Dropdown.Item
                  onClick={() => {
                    addBackOperatingSystem(oSystem);
                  }}
                  key={oSystem.os}
                  className="text-dark"
                >
                  {oSystem.os}
                </Dropdown.Item>
              );
            })}
          </Dropdown.Menu>
        </Dropdown>
      ),
    },
  ];

  const isAllOsValidated = () => {
    let allOsRemoved = true;
    const remotePattern =
      /^(?:(?:https?|ftp):\/{2})(?:\S+(?::\S*)?@)?(?:(?:(?:[\w\-.]))*)(?::[0-9]+)?(?:\/\S*)?$/;
    const localPattern =
      /^file:\/{2,3}([a-zA-Z][:|]\/){0,1}[\w~!*'();@&=\/\\\-+$,?%#.\[\]]+$/;
    if (operatingSystems?.[selectedVersion.id]) {
      for (const oSystem of operatingSystems?.[selectedVersion.id]) {
        if (oSystem.isAdded) {
          allOsRemoved = false;
        }
        for (const repo of oSystem.repos) {
          if (
            oSystem.isAdded &&
            !(
              remotePattern.test(repo.baseUrl) ||
              localPattern.test(repo.baseUrl)
            )
          ) {
            return false;
          }
        }
      }
      return allOsRemoved ? false : true;
    }
    return false;
  };

  function editAllRepos(key: string, value: any) {
    const operatingSystemsCopy = cloneDeep(operatingSystems);
    const allOs = operatingSystemsCopy?.[selectedVersion.id];
    allOs?.map((os: TransformedOperatingSystem) => {
      os.repos.map((repo) => {
        //@ts-ignore
        repo[key as keyof TransformedRepo] = value;
        return repo;
      });
      return os;
    });
    return operatingSystemsCopy;
  }

  function editOsOrRepo(
    operatingSystem: string,
    repoId: string,
    key: string,
    value: any
  ) {
    const operatingSystemsCopy = cloneDeep(operatingSystems);
    const matchingOperatingSystem = find(
      operatingSystemsCopy?.[selectedVersion.id],
      {
        os: operatingSystem,
      }
    );

    if (matchingOperatingSystem) {
      if (repoId) {
        const matchingRepo = find(matchingOperatingSystem.repos, {
          id: repoId,
        });

        if (matchingRepo) {
          //@ts-ignore
          matchingRepo[key as keyof TransformedRepo] =
            value as TransformedRepo[keyof TransformedRepo];
          matchingRepo.hasError = false;
          setOperatingSystems(operatingSystemsCopy);
        }
      } else {
        set(matchingOperatingSystem, key, value);
        setOperatingSystems(operatingSystemsCopy);
      }
    }
  }

  async function saveVersion() {
    let createdVersionDefinition: any = {};
    if (!readOnly)
      try {
        createdVersionDefinition = await VersionsApi.readVersionInfo(
          {
            VersionDefinition: {
              available: selectedStack.id,
              display_name: `${selectedStack.id}.${versionNumber}`,
            },
          },
          {},
          false
        );
      } catch (err) {
        toast.error("Could not read version info");
      }
    const addedOs = operatingSystems?.[selectedVersion.id]?.filter(
      (os: TransformedOperatingSystem) => os.isAdded
    );
    let payload = {};

    payload = {
      ...(readOnly && {
        RepositoryVersions: savedRepositoryVersionDetails,
      }),
      operating_systems: addedOs?.map((os: TransformedOperatingSystem) => {
        return {
          OperatingSystems: {
            os_type: os.os,
            ambari_managed_repositories: skipValidation,
            stack_name: selectedStack.stack_name,
            stack_version: selectedStack.stack_version,
            ...(readOnly && {
              repository_version_id: savedRepositoryVersionDetails.id,
            }),
            ...(!readOnly && { version_defintion_id: selectedStack.id }),
          },
          repositories: os.repos.map((repo: TransformedRepo) => {
            return {
              Repositories: {
                applicable_services: [],
                base_url: repo.baseUrl,
                components: null,
                default_base_url: "",
                distribution: null,
                intial_base_url: repo.defaultUrl,
                initial__repo_id: selectedStack.id,
                mirrors_list: null,
                os_type: os.os,
                stack_name: selectedStack.stack_name,
                stack_version: selectedStack.stack_version,
                tags: [],
                unique: false,
                version_defintion_id: selectedStack.id,
                repo_id: repo.id,
                repo_name: repo.name,
              },
              hasError: false,
              invalidBaseUrl: false,
            };
          }),
          selected: true,
        };
      }),
    };
    try {
      await VersionsApi.saveRepoVersions(
        selectedStack.stack_name,
        selectedStack.stack_version,
        readOnly
          ? savedRepositoryVersionDetails?.id
          : //@ts-ignore
            createdVersionDefinition?.resources?.[0]?.VersionDefinition?.id,
        payload
      );
      toast.success("Version saved successfully");
      redirectToList();
    } catch (err: any) {
      const errorMessage = err?.response?.data?.message;
      if (
        errorMessage?.includes(
          "is already defined for another repository version"
        )
      ) {
        setShowRegistrationModal(true);
        await VersionsApi.deleteRepositoryVersion(
          selectedStack.stack_name,
          selectedStack.stack_version,
          createdVersionDefinition?.resources?.[0]?.VersionDefinition?.id
        );
      }
      // toast.error("Could not save version");
    }
  }

  async function validateRepos() {
    if (!isAllOsValidated) {
      return;
    }
    if (skipValidation) {
      saveVersion();
    } else {
      const operatingSystemsCopy = cloneDeep(operatingSystems);
      let allOsValidated = false;
      const versionValidationPromises = [];
      const allAddedOs = operatingSystemsCopy?.[selectedVersion.id].filter(
        (oSystem) => oSystem.isAdded
      );
      for (const oSystem of allAddedOs) {
        const repos = oSystem.repos;
        for (const repo of repos) {
          versionValidationPromises.push(
            VersionsApi.validateRepos(
              selectedStack.stack_version,
              selectedStack.stack_version,
              oSystem.os,
              repo.id,
              {
                base_url: repo.baseUrl,
                repo_name: repo.name,
              }
            )
          );
        }
      }
      try {
        const validationResponses = await Promise.allSettled(
          versionValidationPromises
        );
        //In the response array map the response operation to corresponding repo via matching index
        //If the response at nth index is empty then the nth repo is valid add a key called hasError false
        //If the response at nth index is not empty then the nth repo is invalid add a key called hasError true
        const osWithValidationStatus = allAddedOs.map((os, osIndex) => {
          os.repos.map((repo, repoIndex) => {
            if (
              validationResponses[osIndex * os.repos.length + repoIndex]
                ?.status === "rejected"
            ) {
              allOsValidated = false;
              repo.hasError = true;
            } else {
              repo.hasError = false;
            }
            return repo;
          });
          return os;
        });
        const osCopy = cloneDeep(operatingSystems);
        osCopy[selectedVersion.id].map((oSystem) => {
          const matchingOs = osWithValidationStatus.find(
            (os) => os.os === oSystem.os
          );
          if (matchingOs) {
            oSystem.repos = matchingOs.repos;
          }
          return oSystem;
        });
        if (!allOsValidated) {
          setShowRepoValidationBanner(true);
        } else {
          setShowRepoValidationBanner(false);
          saveVersion();
        }
        setOperatingSystems({
          ...operatingSystems,
          [selectedVersion.id]: osCopy[selectedVersion.id],
        });
      } catch (error) {
        console.log("Error", error);
      }
    }
  }

  const readVersionCallback = async (versionResources: any) => {
    try {
      const addedVersionOperatingSystems =
        versionResources?.resources?.[0]?.operating_systems;
      const addedVersion = versionResources?.resources?.[0]?.VersionDefinition;

      const versionOperatingSystems =
        await VersionsApi.getVersionOperatingSystems(
          selectedStack.stack_version
        );
      const newVersion = {
        label: `${addedVersion.stack_name}-${addedVersion.repository_version}`,
        value: {
          ...addedVersion,
          id: addedVersion.repository_version,
          defaultId: addedVersion.repository_version,
        },
      };
      const stackVersion = addedVersion.stack_version;
      const belongingStack = versionDefinitions.find((definition) => {
        return definition.VersionDefinition.stack_version === stackVersion;
      });
      const addedOperatingSystems = addedVersionOperatingSystems.map(
        (os: any) => {
          return os.OperatingSystems.os_type;
        }
      );
      const allOs: TransformedOperatingSystem[] =
        versionOperatingSystems.operating_systems.map((os: any) => {
          const matchingOs =
            versionResources?.resources?.[0]?.operating_systems.find(
              (oS: any) => {
                return (
                  oS.OperatingSystems.os_type === os.OperatingSystems.os_type
                );
              }
            );
          return {
            os: os.OperatingSystems.os_type,
            isAdded: addedOperatingSystems.includes(os.OperatingSystems.os_type)
              ? true
              : false,
            repos: (addedOperatingSystems.includes(os.OperatingSystems.os_type)
              ? matchingOs
              : os
            ).repositories.map((repo: any) => {
              return {
                id: repo.Repositories.repo_id,
                defaultId: repo.Repositories.repo_id,
                baseUrl: repo?.Repositories?.base_url,
                name: repo?.Repositories?.repo_name,
                defaultUrl: repo?.Repositories?.default_base_url || "",
              };
            }),
          };
        });

      const operatingSystemsCopy = cloneDeep(operatingSystems);
      operatingSystemsCopy[addedVersion.repository_version] = [...allOs];
      setOperatingSystems(operatingSystemsCopy);

      setSelectedStack(belongingStack?.VersionDefinition as VersionDefinition);
      selectNewVersion(
        belongingStack?.VersionDefinition as VersionDefinition,
        newVersion,
        {
          ...addedVersion,
          id: addedVersion.repository_version,
        }
      );
      const addedVersionName = addedVersion.repository_version.split(".");
      setVersionNumber(
        addedVersionName.splice(2, addedVersionName.length).join(".")
      );
      setShowAddVersionModal(false);
    } catch (err) {
      toast.error("Could not read version");
      console.log("Error", err);
    }
  };

  const readOnlyRepoProperties = [
    {
      label: "Stack",
      value: `${savedRepositoryVersionDetails?.stack_name}-${savedRepositoryVersionDetails?.stack_version}`,
    },
    {
      label: "Name",
      value: savedRepositoryVersionDetails?.display_name,
    },
    {
      label: "Version",
      value: savedRepositoryVersionDetails?.repository_version,
    },
  ];

  if (!versionDefinitions.length || isEmpty(operatingSystems)) {
    return <Spinner />;
  }

  return (
    <>
      <ConfirmationModal
        isOpen={confirmDeregister}
        onClose={() => {
          setConfirmDeregister(false);
        }}
        modalTitle="Deregister Version"
        modalBody={`Are you sure you want to deregister the version ${savedRepositoryVersionDetails.stack_name}-${savedRepositoryVersionDetails.repository_version}?`}
        successCallback={deRegisterVersion}
      />
      <AddVersionModal
        isOpen={showAddVersionModal}
        onReadVersion={readVersionCallback}
        onClose={() => {
          setShowAddVersionModal(false);
        }}
      />
      <LostNetworkModal
        isOpen={showNetworkModal}
        onClose={() => {
          handleModalVisibility(false);
        }}
      ></LostNetworkModal>
      <RedhatSatelliteUsageInfo
        isOpen={showRedhatInfoModal}
        onCancel={() => {
          setShowRedhatInfoModal(false);
          setRedhatSatellite(false);
        }}
        onClose={() => {
          setShowRedhatInfoModal(false);
        }}
      />
      <ConfirmationModal
        modalTitle="Unable to Register"
        modalBody="You are attempting to register a version with a Base URL that is already in use with an existing registered version. You *must* review your Base URLs and confirm they are unique for the version you are trying to register."
        isOpen={showRegistrationModal}
        successCallback={() => {
          setShowRegistrationModal(false);
        }}
        onClose={() => {
          setShowRegistrationModal(false);
        }}
      />
      <div
        className="breadcrumb d-flex border-bottom pb-2"
        style={{ flexWrap: "nowrap" }}
      >
        <Link to="/stackVersions">
          {" "}
          <h3>Versions</h3>
        </Link>
        <div className="mx-2">
          <h3 className="text-muted">/</h3>
        </div>
        <div className="d-flex justify-content-between w-100 align-items-center">
          <div className="text-muted d-flex align-items-center">
            <h3>
              {readOnly
                ? savedRepositoryVersionDetails?.display_name
                : "Register Version"}
            </h3>
            {readOnly ? (
              <h5 className="ms-2">
                ({savedRepositoryVersionDetails?.stack_name}-
                {savedRepositoryVersionDetails?.repository_version})
              </h5>
            ) : null}
          </div>
          {readOnly ? (
            <Button
              variant="danger"
              size="sm"
              onClick={() => {
                setConfirmDeregister(true);
              }}
              disabled={
                repoInfo?.status == "CURRENT" || repoInfo?.status == "INSTALLED"
              }
            >
              DEREGISTER VERSION
            </Button>
          ) : null}
        </div>
      </div>
      <Row style={{ height: "35vh" }}>
        {readOnly ? (
          <Col
            sm={3}
            className="alert alert-info d-flex flex-column px-3 rounded-1 me-4"
            style={{ height: "40%" }}
          >
            {readOnlyRepoProperties.map((property) => {
              return (
                <div className="d-flex justify-content-between mt-3">
                  <div>{property.label}</div>
                  <div>{property.value}</div>
                </div>
              );
            })}
          </Col>
        ) : (
          <Tab.Container>
            <Col sm={2}>
              <Nav variant="pills" className="flex-column">
                {versionDefinitions.map((definition) => {
                  return (
                    <Nav.Item
                      data-testid="version-definition"
                      onClick={() => {
                        selectNewVersion(definition.VersionDefinition);
                        setSelectedStack(definition.VersionDefinition);
                      }}
                      className={`mt-3 ${
                        selectedStack.id === definition.VersionDefinition.id
                          ? "border-bottom border-2 border-success"
                          : "muted-text"
                      }`}
                    >
                      <Nav.Link
                        className={`nowrap text-decoration-none ${
                          selectedStack.id === definition.VersionDefinition.id
                            ? "text-dark"
                            : "muted-text"
                        } `}
                      >
                        {definition.VersionDefinition.id}
                      </Nav.Link>
                    </Nav.Item>
                  );
                })}
              </Nav>
            </Col>
          </Tab.Container>
        )}
        {/* <Col sm={1}></Col> */}
        <Col
          sm={readOnly ? 8 : 9}
          className="border p-3 ms-5"
          style={{ maxHeight: "100%", overflow: "hidden" }}
        >
          {readOnly ? null : (
            <div className="d-flex justify-content-between">
              <Dropdown>
                <DropdownButton
                  size="sm"
                  variant="info"
                  id="dropdown-basic"
                  data-testid="version-dropdown"
                  title={
                    selectedVersion.id || selectedVersion.repository_version
                  }
                >
                  {addedVersions[selectedStack.id]
                    ? addedVersions?.[selectedStack.id]?.map((vers, index) => {
                        return (
                          <Dropdown.Item
                            key={index}
                            className="text-dark"
                            onClick={() => {
                              setSelectedVersion(vers.value);
                            }}
                          >
                            {vers.label}
                          </Dropdown.Item>
                        );
                      })
                    : null}
                  <Dropdown.Item
                    className="text-dark"
                    onClick={() => {
                      setShowAddVersionModal(true);
                    }}
                    data-testid="add-version"
                  >
                    Add Version
                  </Dropdown.Item>
                </DropdownButton>
              </Dropdown>
              {readOnly ? null : (
                <Form.Group className="d-flex align-items-center">
                  <Form.Label className="nowrap">
                    {" "}
                    Name: {selectedStack.stack_name}-
                    {selectedStack.stack_version}.
                  </Form.Label>
                  <InputGroup className="d-flex flex-column w-100">
                    <Form.Control
                      data-testid="version-input"
                      type="text"
                      className={`w-100 ${
                        versionValidationError ? "border border-danger" : ""
                      }`}
                      placeholder="Version Number (0.0)"
                      value={versionNumber}
                      onChange={(e) => {
                        setVersionNumber(e.target.value);
                      }}
                    />
                    <div
                      className={`text-danger ${
                        versionValidationError ? "visible" : "invisible"
                      }`}
                    >
                      {selectedStack.stack_name}-{selectedStack.stack_version}.
                      {versionNumber} invalid
                    </div>
                  </InputGroup>
                </Form.Group>
              )}
            </div>
          )}
          <div
            className="border mt-2 overflow-auto"
            style={{ maxHeight: "100%" }}
          >
            <Table
              striped
              columns={getColumns()}
              data-testid="stack-services"
              data={
                (versionDefinitions?.find(
                  (definition) =>
                    definition.VersionDefinition.stack_version ===
                    selectedVersion.stack_version
                )?.VersionDefinition.stack_services as unknown[]) || []
              }
            />
          </div>
        </Col>
      </Row>
      <Row className="mt-4">
        <div className="d-flex ps-0">
          <Form.Check
            type="radio"
            disabled={networkLost}
            label="Use Public repository"
            checked={selectedChoice === RepositoryType.PUBLIC}
          />
          {networkLost ? (
            <div
              className="ms-2 custom-link cursor-pointer"
              onClick={() => {
                handleModalVisibility(true);
              }}
            >
              Why is this disabled?
            </div>
          ) : null}
        </div>

        <Form.Check
          checked={selectedChoice === RepositoryType.LOCAL}
          type="radio"
          label="Use Local repository"
        />
      </Row>
      <Row className="border mx-2">
        <div className="border-bottom bg-light">
          <div className="p-3 fs-6">Repositories</div>
        </div>
        <div className="p-3">
          <Alert variant="info" className="">
            Provide Base URLs for the Operating Systems you are configuring.
          </Alert>

          {showRepoValidationBanner ? (
            <Alert variant="warning" className="">
              Some of the repositories failed validation. Make changes to the
              base url or skip validation if you are sure that urls are correct
            </Alert>
          ) : null}
          <Row className="align-items-center py-3 border-bottom">
            {osListHeaders.map((header) => {
              return <Col md={header.columnCount}>{header.label}</Col>;
            })}
          </Row>
          {operatingSystems?.[selectedVersion.id]
            ?.filter((oSystem: TransformedOperatingSystem) => oSystem.isAdded)
            ?.map((oSystem: any) => {
              return (
                <Row
                  className="border-bottom py-4"
                  data-testid="operating-systems"
                >
                  <Col md={3}>{oSystem.os}</Col>
                  <Col md={8}>
                    {oSystem?.repos?.map((repo: any, index: any) => {
                      return (
                        <Row
                          className={`d-flex align-items-center nowrap ${
                            index > 0 ? "mt-4" : ""
                          }`}
                        >
                          {" "}
                          <Col
                            md={4}
                            className={`d-flex align-items-center ${
                              repo.hasError ? "text-danger" : ""
                            }`}
                          >
                            {repo.isEditing ? (
                              <Form
                                onSubmit={(e) => {
                                  e.preventDefault();
                                  editOsOrRepo(
                                    oSystem.os,
                                    repo.id,
                                    "isEditing",
                                    false
                                  );
                                }}
                              >
                                <Form.Control
                                  value={repo.id}
                                  onChange={(e) => {
                                    editOsOrRepo(
                                      oSystem.os,
                                      repo.id,
                                      "id",
                                      e.target.value
                                    );
                                  }}
                                  type="text"
                                  data-testid="repo-id-input"
                                ></Form.Control>
                              </Form>
                            ) : (
                              repo.id
                            )}
                            {redhatSatellite && !repo.isEditing ? (
                              <FontAwesomeIcon
                                data-testid="pencil-icon"
                                className="ms-2"
                                icon={faPencil}
                                onClick={() => {
                                  editOsOrRepo(
                                    oSystem.os,
                                    repo.id,
                                    "isEditing",
                                    true
                                  );
                                }}
                              />
                            ) : null}
                            {repo.isEditing && repo.id !== repo.defaultId ? (
                              <FontAwesomeIcon
                                icon={faUndo}
                                className="text-warning ms-2"
                                onClick={() => {
                                  editOsOrRepo(
                                    oSystem.os,
                                    repo.id,
                                    "id",
                                    repo.defaultId
                                  );
                                }}
                              ></FontAwesomeIcon>
                            ) : null}
                          </Col>
                          <Col md={8} className="d-flex align-items-center">
                            <Form.Control
                              onChange={(e) => {
                                editOsOrRepo(
                                  oSystem.os,
                                  repo.id,
                                  "baseUrl",
                                  e.target.value
                                );
                              }}
                              value={repo.baseUrl}
                              type="text"
                              className={`${
                                repo.hasError ? "border border-danger" : ""
                              }`}
                              disabled={repo.isEditable === false}
                              placeholder="Enter Base URL or remove this OS"
                              data-testid="repo-base-url-input"
                            ></Form.Control>
                            {repo.baseUrl !== repo.defaultUrl ? (
                              <FontAwesomeIcon
                                icon={faUndo}
                                className="text-warning ms-2 cursor-pointer"
                                onClick={() => {
                                  editOsOrRepo(
                                    oSystem.os,
                                    repo.id,
                                    "baseUrl",
                                    repo.defaultUrl
                                  );
                                }}
                              />
                            ) : (
                              <div className="ms-4"></div>
                            )}
                          </Col>
                        </Row>
                      );
                    })}
                  </Col>
                  <Col md={1} className="mt-2">
                    <div
                      className="text-danger cursor-pointer"
                      onClick={() => {
                        editOsOrRepo(oSystem.os, "", "isAdded", false);
                      }}
                    >
                      <FontAwesomeIcon
                        icon={faMinus}
                        className="me-2 cursor-pointer"
                      />
                      Remove
                    </div>
                  </Col>
                </Row>
              );
            })}
        </div>
        <div className="repo-configs">
          <Form>
            <Form.Check
              checked={skipValidation}
              type="checkbox"
              id="skipValidation"
              data-testid="skipValidationCheckbox"
              onChange={() => {
                setSkipValidation(!skipValidation);
              }}
              label={
                <div style={{ marginTop: 2 }}>
                  Skip Repository Base URL validation (Advanced)
                  <OverlayTrigger
                    placement="right"
                    delay={{ show: 250, hide: 400 }}
                    overlay={
                      <Tooltip>
                        Warning! This is for advanced users only. Use this
                        optionif you want to skip validation for Repository Base
                        URLs.
                      </Tooltip>
                    }
                  >
                    <FontAwesomeIcon className="ms-2" icon={faQuestionCircle} />
                  </OverlayTrigger>
                </div>
              }
            ></Form.Check>
          </Form>
          <Form.Check
            type="checkbox"
            checked={redhatSatellite}
            id="disableLocal"
            data-testid="redhatSatelliteCheckbox"
            onChange={() => {
              setRedhatSatellite(!redhatSatellite);
            }}
            label={
              <div style={{ marginTop: 2 }}>
                Use RedHat Satellite/Spacewalk
                <OverlayTrigger
                  placement="right"
                  delay={{ show: 250, hide: 400 }}
                  overlay={
                    <Tooltip>
                      Disable distributed repositories and use RedHat
                      Satellite/Spacewalk channels instead
                    </Tooltip>
                  }
                >
                  <FontAwesomeIcon className="ms-2" icon={faQuestionCircle} />
                </OverlayTrigger>
              </div>
            }
          ></Form.Check>
        </div>
      </Row>
      <div className="d-flex justify-content-end mt-2 me-2">
        <Link to="/stackVersions">
          <DefaultButton>CANCEL</DefaultButton>
        </Link>
        <Button
          variant="success"
          size="sm"
          className="ms-2"
          onClick={validateRepos}
          disabled={
            !isAllOsValidated() || !versionNumber || versionValidationError
          }
        >
          SAVE
        </Button>
      </div>
    </>
  );
};

export default Register;