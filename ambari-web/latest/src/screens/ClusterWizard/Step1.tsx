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
//@ts-nocheck
import { useContext, useEffect, useState } from "react";
import {
  Item,
  VersionDefinition,
  VersionDefinitionResponse,
} from "./types/VersionsDefinition";
import { useParams } from "react-router-dom";
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
  Card,
  CardBody,
} from "react-bootstrap";
import { TransformedOperatingSystem, TransformedRepo } from "./types/Os";
import { find, set, cloneDeep, get, isEmpty } from "lodash";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faAdd,
  faMinus,
  faPencil,
  faQuestionCircle,
  faUndo,
} from "@fortawesome/free-solid-svg-icons";
import VersionsApi from "../../api/versionsApi";
import toast from "react-hot-toast";
import AddVersionModal, {
  VersionDefinitionSource,
} from "../../components/AddVersionModal";
import LostNetworkModal from "../../components/LostNetworkModal";
import Table from "../../components/Table";
import DefaultButton from "../../components/DefaultButton";
import RedhatSatelliteUsageInfo from "../../components/RedhatSatelliteInfoModal";
import ConfirmationModal from "../../components/ConfirmationModal";
import WizardFooter from "../../components/StepWizard/WizardFooter";
import { ActionTypes } from "./clusterStore/types";
import { getStepData } from "../../Utils/Utility";
import wizardSteps from "./wizardSteps";
import { ContextWrapper } from ".";
import { AppContext } from "../../store/context";
import { isJdkCompatible } from "./versionSelection";
import { copyRepositoryCredentials } from "../../Utils/repositoryCredentials";

enum RepositoryType {
  PUBLIC = "public",
  LOCAL = "local",
}

enum OSOperations {
  GET = "get",
  EDITALL = "editall",
}

export default function Step1({ wizardName = "clusterCreation" }) {
  const [versionDefinitions, setVersionDefinitions] = useState<Item[]>([]);
  const [selectedVersion, setSelectedVersion] = useState<any>({});
  const [networkLost, setNetworkLost] = useState<boolean>(false);
  const [showRegistrationModal, setShowRegistrationModal] =
    useState<boolean>(false);
  const [selectedStack, setSelectedStack] = useState<VersionDefinition>(
    {} as VersionDefinition
  );
  //   const history = useHistory();
  const [selectedChoice, setSelectedChoice] = useState<string>(
    RepositoryType.PUBLIC
  );
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
  const [showJdkWarning, setShowJdkWarning] = useState(false);
  const [versionDefinitionSource, setVersionDefinitionSource] =
    useState<VersionDefinitionSource | undefined>();
  const [addedVersions, setAddedVersions] = useState<{
    [key: string]: { label: string; value: string }[];
  }>({});
  const [operatingSystems, setOperatingSystems] = useState<{
    [key: string]: TransformedOperatingSystem[];
  }>({});
  const [nextEnabled, setNextEnabled] = useState(false);
  const { Context } = useContext(ContextWrapper);
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: {
      currentStep,
      handleNextImperitive,
      jumpToStep,
      handleBackImperitive,
    },
  }: any = useContext(Context);
  const { ambariProperties, supports } = useContext(AppContext);

  const enableNext = () => {
    setNextEnabled(true);
  };

  const disableNext = () => {
    setNextEnabled(false);
  };

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
    async function getVersionDefinitions() {
      try {
        const stateData = get(
          state,
          `${wizardName}Steps.${currentStep.name}.data`,
          {},
        );
        const stacks = await VersionsApi.getStacks();
        const stackName = get(stateData, "selectedStack.stack_name")
          || stack
          || get(stacks, "items[0].Stacks.stack_name", "");
        if (!stackName) {
          throw new Error("No installable stack is available.");
        }
        const definitions: VersionDefinitionResponse =
          await VersionsApi.getVersionDefinitions(stackName);
        const sortedItems = [...(definitions.items || [])].sort((a: any, b: any) => {
          const versionA = parseFloat(a.VersionDefinition.id.split("-")[1]);
          const versionB = parseFloat(b.VersionDefinition.id.split("-")[1]);

          return versionB - versionA;
        });
        if (!sortedItems.length) {
          throw new Error(`No installable version definition is available for ${stackName}.`);
        }
        setVersionDefinitions(sortedItems);
        setNetworkIssues(sortedItems);

        if (isEmpty(stateData)) {
          setSelectedStack(sortedItems[0].VersionDefinition);
          selectNewVersion(sortedItems[0].VersionDefinition);
        }
      } catch (error: any) {
        toast.error(
          error?.response?.data?.message
            || error?.message
            || "Could not load version definitions.",
        );
        jumpToStep(0, true);
      }
    }
    void getVersionDefinitions();
  }, [wizardName]);

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
      const revertOs = editAllRepos("isEditable", true);
      setOperatingSystems(revertOs);
    }
  }, [redhatSatellite]);

  useEffect(() => {
    const stateData = get(
      state,
      `${wizardName}Steps.${currentStep.name}.data`,
      {}
    );
    if (!isEmpty(stateData)) {
      // Fix for TLHASD-1260: Ensure proper state restoration for base URL persistence
      setSelectedVersion(stateData?.selectedVersion);
      setSelectedStack(stateData?.selectedStack); // Fixed typo: was 'selelectedStack'
      setSelectedChoice(stateData?.selectedChoice ?? RepositoryType.PUBLIC);
      setSkipValidation(Boolean(stateData?.skipValidation));
      setRedhatSatellite(Boolean(stateData?.redhatSatellite));
      setOperatingSystems(stateData?.operatingSystems);
      setVersionDefinitionSource(stateData?.versionDefinitionSource);
      // Fix for TLHASD-1260: Restore addedVersions from XML uploads
      if (stateData?.addedVersions) {
        setAddedVersions(stateData.addedVersions);
      }
    }
  }, [state]);

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
            !redhatSatellite &&
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

  useEffect(() => {
    if (!isAllOsValidated() || versionValidationError) {
      disableNext();
    } else {
      enableNext();
    }
  }, [operatingSystems, versionNumber, versionValidationError, skipValidation, redhatSatellite]);

  useEffect(() => {
    async function getVersionOs() {
      const stateData = get(
        state,
        `${wizardName}Steps.${currentStep.name}.data`,
        {}
      );
      
      // Don't overwrite existing operating systems data if we have restored state
      if (!isEmpty(stateData) && stateData.operatingSystems?.[selectedVersion.id]) {
        return;
      }
      
      const versionOperatingSystems =
        await VersionsApi.getVersionOperatingSystems(
          selectedStack.stack_name,
          selectedStack.stack_version
        );
      let alreadyAddedOs: any = [];
      const allOs: TransformedOperatingSystem[] =
        versionOperatingSystems.operating_systems.map((os: any) => {
          const defaultRepos = os.repositories.map((repo: any) => {
            return {
              id: repo.Repositories.repo_id,
              defaultId: repo.Repositories.repo_id,
              baseUrl:
                repo.Repositories.base_url
                || repo.Repositories.default_base_url
                || "",
              name: repo?.Repositories?.repo_name,
              defaultUrl:
                repo.Repositories.default_base_url
                || repo.Repositories.base_url
                || "",
            };
          });
          const matchingOs = undefined;
          const isOsAdded = true;
          return {
            os: os.OperatingSystems.os_type,
            isAdded: isOsAdded,
            repos: defaultRepos,
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
      !operatingSystems?.[selectedVersion?.id]
    ) {
      getVersionOs();
    }
  }, [selectedVersion, state, wizardName, currentStep]);

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
        return osCopy?.[selectedVersion?.id]?.filter(
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

  //   const redirectToList = () => {
  //     history.push("/stackVersions");
  //   };

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
    const isNetworkLost = !!versions.find(
      (_version) => _version.VersionDefinition.stack_default === false
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
            disabled={getAddableOperatingSystems()?.length === 0}
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
          if (
            key === "baseUrl"
            && supports.disableCredentialsAutocompleteForRepoUrls === false
          ) {
            operatingSystemsCopy[selectedVersion.id].forEach(
              (os: TransformedOperatingSystem) => {
                os.repos.forEach((repo) => {
                  repo.baseUrl = copyRepositoryCredentials(repo.baseUrl, value);
                });
              },
            );
          }
          setOperatingSystems(operatingSystemsCopy);
        }
      } else {
        set(matchingOperatingSystem, key, value);
        setOperatingSystems(operatingSystemsCopy);
      }
    }
  }

  // async function saveVersion() {
  //     let createdVersionDefinition: any = {};

  //     try {
  //         createdVersionDefinition = await VersionsApi.readVersionInfo(
  //             {
  //                 VersionDefinition: {
  //                     available: selectedStack.id,
  //                     display_name: `${selectedStack.id}.${versionNumber}`,
  //                 },
  //             },
  //             {},
  //             false
  //         );
  //     } catch (err) {
  //         toast.error("Could not read version info");
  //     }
  //     const addedOs = operatingSystems?.[selectedVersion.id]?.filter(
  //         (os: TransformedOperatingSystem) => os.isAdded
  //     );
  //     let payload = {};

  //     payload = {
  //         ...{
  //             operating_systems: addedOs?.map((os: TransformedOperatingSystem) => {
  //                 return {
  //                     OperatingSystems: {
  //                         os_type: os.os,
  //                         ambari_managed_repositories: skipValidation,
  //                         stack_name: selectedStack.stack_name,
  //                         stack_version: selectedStack.stack_version,
  //                         ...({ version_defintion_id: selectedStack.id }),
  //                     },
  //                     repositories: os.repos.map((repo: TransformedRepo) => {
  //                         return {
  //                             Repositories: {
  //                                 applicable_services: [],
  //                                 base_url: repo.baseUrl,
  //                                 components: null,
  //                                 default_base_url: "",
  //                                 distribution: null,
  //                                 intial_base_url: repo.defaultUrl,
  //                                 initial__repo_id: selectedStack.id,
  //                                 mirrors_list: null,
  //                                 os_type: os.os,
  //                                 stack_name: selectedStack.stack_name,
  //                                 stack_version: selectedStack.stack_version,
  //                                 tags: [],
  //                                 unique: false,
  //                                 version_defintion_id: selectedStack.id,
  //                                 repo_id: repo.id,
  //                                 repo_name: repo.name,
  //                             },
  //                             hasError: false,
  //                             invalidBaseUrl: false,
  //                         };
  //                     }),
  //                     selected: true,
  //                 };
  //             }),
  //         }};
  //     try {
  //         await VersionsApi.saveRepoVersions(
  //             selectedStack.stack_name,
  //             selectedStack.stack_version,
  //             createdVersionDefinition?.resources?.[0]?.VersionDefinition?.id,
  //             payload
  //         );
  //         toast.success("Version saved successfully");
  //         //   redirectToList();
  //     } catch (err: any) {
  //         const errorMessage = err.response.data.message;
  //         if (
  //             errorMessage.includes(
  //                 "is already defined for another repository version"
  //             )
  //         ) {
  //             setShowRegistrationModal(true);
  //             await VersionsApi.deleteRepositoryVersion(
  //                 selectedStack.stack_name,
  //                 selectedStack.stack_version,
  //                 createdVersionDefinition?.resources?.[0]?.VersionDefinition?.id
  //             );
  //         }
  //         // toast.error("Could not save version");
  //     }
  // }

  async function validateRepos() {
    if (!isAllOsValidated()) {
      return;
    }
    if (skipValidation || redhatSatellite) {
      //   saveVersion();
      return true;
    } else {
      const operatingSystemsCopy = cloneDeep(operatingSystems);
      let allOsValidated = true;
      const versionValidationPromises = [];
      const allAddedOs = operatingSystemsCopy?.[selectedVersion.id].filter(
        (oSystem) => oSystem.isAdded
      );
      for (const oSystem of allAddedOs) {
        const repos = oSystem.repos;
        for (const repo of repos) {
          versionValidationPromises.push(
            VersionsApi.validateRepos(
              selectedStack.stack_name,
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
            const validationResult = validationResponses[osIndex * os.repos.length + repoIndex];
            if (validationResult?.status === "rejected") {
              allOsValidated = false;
              repo.hasError = true;
              // Show error message to user when validation fails (Fix for TLHASD-1257)
              const errorReason = validationResult.reason?.message || validationResult.reason || "Invalid Base URL";
              toast.error(`Repository validation failed for ${repo.name} (${os.os}): ${errorReason}`);
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
          //   saveVersion();
        }
        setOperatingSystems({
          ...operatingSystems,
          [selectedVersion.id]: osCopy[selectedVersion.id],
        });
        return allOsValidated;
      } catch (error) {
        console.log("Error", error);
      }
    }
  }

  const readVersionCallback = async (
    versionResources: any,
    source: VersionDefinitionSource,
  ) => {
    try {
      const addedVersionOperatingSystems =
        versionResources?.resources?.[0]?.operating_systems;
      const addedVersion = versionResources?.resources?.[0]?.VersionDefinition;

      const versionOperatingSystems =
        await VersionsApi.getVersionOperatingSystems(
          addedVersion.stack_name,
          addedVersion.stack_version
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
                id: repo?.Repositories?.repo_id,
                defaultId: repo?.Repositories?.repo_id,
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
      setVersionDefinitionSource(source);
      setShowAddVersionModal(false);
    } catch (err) {
      toast.error("Could not read version");
      console.log("Error", err);
    }
  };

  // const readOnlyRepoProperties = [
  //     {
  //         label: "Stack",
  //         value: `${savedRepositoryVersionDetails?.stack_name}-${savedRepositoryVersionDetails?.stack_version}`,
  //     },
  //     {
  //         label: "Name",
  //         value: savedRepositoryVersionDetails?.display_name,
  //     },
  //     {
  //         label: "Version",
  //         value: savedRepositoryVersionDetails?.repository_version,
  //     },
  // ];

  const saveVersionSelection = async () => {
    const allValidated = await validateRepos();
    if (!allValidated) {
      disableNext();
      return;
    }

    const operatingSystemsCopy = cloneDeep(operatingSystems);
    const allAddedOs = operatingSystemsCopy?.[selectedVersion.id]?.filter(
      (oSystem) => oSystem.isAdded,
    ) || [];
    const versionUpdatePromises = allAddedOs.flatMap((oSystem) =>
      oSystem.repos.map((repo) => VersionsApi.updateOSInfo(
        selectedStack.stack_name,
        selectedStack.stack_version,
        oSystem.os,
        repo.id,
        {
          Repositories: {
            base_url: repo.baseUrl,
            repo_name: repo.name,
            verify_base_url: !skipValidation && !redhatSatellite,
          },
        },
      )),
    );

    try {
      await Promise.all(versionUpdatePromises);
    } catch (error: any) {
      toast.error(
        error?.response?.data?.message
          || "Ambari could not save the repository configuration.",
      );
      return;
    }

    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: currentStep.name,
        data: {
          selectedVersion,
          selectedStack,
          selectedChoice,
          skipValidation,
          redhatSatellite,
          operatingSystems,
          addedVersions,
          versionDefinitionSource,
        },
      },
    });
    await Promise.resolve(flushStateToDb("next"));
    handleNextImperitive();
  };

  const continueAfterJdkValidation = () => {
    const compatible = isJdkCompatible(
      ambariProperties?.["java.version"],
      selectedStack?.min_jdk,
      selectedStack?.max_jdk,
    );
    if (!compatible) {
      setShowJdkWarning(true);
      return;
    }
    void saveVersionSelection();
  };

  return (
    <>
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
          wizardSteps;
          setShowRegistrationModal(false);
        }}
        onClose={() => {
          setShowRegistrationModal(false);
        }}
      />
      <ConfirmationModal
        modalTitle="Unsupported JDK Version"
        modalBody={`The selected ${selectedStack?.stack_name || "stack"} ${
          selectedStack?.stack_version || ""
        } version requires JDK ${selectedStack?.min_jdk || selectedStack?.max_jdk} through ${
          selectedStack?.max_jdk || selectedStack?.min_jdk
        }, but Ambari Server uses ${ambariProperties?.["java.version"]}.`}
        isOpen={showJdkWarning}
        successCallback={() => {
          setShowJdkWarning(false);
          void saveVersionSelection();
        }}
        onClose={() => setShowJdkWarning(false)}
        buttonVariant="danger"
        okButtonText="PROCEED ANYWAY"
      />
      <div className="step-title">Select Version</div>
      <div className="step-description mt-2">
        Select the software version and method of delivery for your cluster.
      </div>
      <Card className="mt-2">
        <CardBody>
          <Tab.Container className="p-0">
            <Col sm={12}>
              <Nav variant="pills">
                {versionDefinitions.map((definition) => {
                  return (
                    <Nav.Item
                      onClick={() => {
                        selectNewVersion(definition.VersionDefinition);
                        setSelectedStack(definition.VersionDefinition);
                      }}
                      className={`my-2 ${
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
          <Col className="mx-4 mt-2" style={{ maxHeight: "100%" }}>
            <div className="d-flex justify-content-between">
              <Dropdown>
                <DropdownButton
                  size="sm"
                  variant="outline-secondary"
                  id="dropdown-basic"
                  data-testid="version-dropdown"
                  title={
                    selectedVersion?.id || selectedVersion?.repository_version
                  }
                >
                  {addedVersions[selectedStack?.id]
                    ? addedVersions?.[selectedStack?.id]?.map((vers) => {
                        return (
                          <Dropdown.Item
                            key={vers.label}
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
                    data--testid="add-version-option"
                    onClick={() => {
                      setShowAddVersionModal(true);
                    }}
                  >
                    Add Version
                  </Dropdown.Item>
                </DropdownButton>
              </Dropdown>
            </div>
            <div className="border mt-2">
              <Table
                scrollable
                maxHeight="30vh"
                columns={getColumns()}
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
          <div className="mx-4 mt-4">
            <h5>Repositories</h5>
            <p>
              Using a Public Repository requires Internet connectivity. Using a
              Local Repository requires you have configured the software in a
              repository available in your network.
            </p>
            <Row className="align-items-center mt-4">
              <Col sm={4} className="d-flex mt-">
                <Form.Check
                  type="radio"
                  disabled={networkLost}
                  label="Use Public repository"
                  checked={selectedChoice === RepositoryType.PUBLIC}
                  onClick={() => {
                    setSelectedChoice(RepositoryType.PUBLIC);
                  }}
                />
                <div
                  className="ms-1 custom-link cursor-pointer"
                  onClick={() => {
                    handleModalVisibility(true);
                  }}
                >
                  Why is this not Selected?
                </div>
              </Col>
              <Col className="d-flex align-items-center">
                <Form.Check
                  checked={selectedChoice === RepositoryType.LOCAL}
                  type="radio"
                  label="Use Local repository"
                  onClick={() => {
                    setSelectedChoice(RepositoryType.LOCAL);
                  }}
                />
              </Col>
            </Row>
            <Alert className="mt-2" variant="info">
              Provide Base URLs for the Operating Systems you are configuring.
            </Alert>
            {selectedChoice === RepositoryType.LOCAL ? (
              <Alert variant="warning">
                Attention: Repository Base URLs of at least one OS are REQUIRED
                before you can proceed. Please make sure they are in correct
                format with its protocol.
              </Alert>
            ) : null}
          </div>
          <Row className=" mx-2">
            <div className="p-3">
              <Row className="align-items-center py-3 border-bottom">
                {osListHeaders.map((header) => {
                  return <Col md={header.columnCount}>{header.label}</Col>;
                })}
              </Row>
              {operatingSystems?.[selectedVersion.id]
                ?.filter(
                  (oSystem: TransformedOperatingSystem) => oSystem.isAdded
                )
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
                                    ></Form.Control>
                                  </Form>
                                ) : (
                                  repo.id
                                )}
                                {redhatSatellite && !repo.isEditing ? (
                                  <FontAwesomeIcon
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
                                {repo.isEditing &&
                                repo.id !== repo.defaultId ? (
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
                  disabled={redhatSatellite}
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
                            optionif you want to skip validation for Repository
                            Base URLs.
                          </Tooltip>
                        }
                      >
                        <FontAwesomeIcon
                          className="ms-2"
                          icon={faQuestionCircle}
                        />
                      </OverlayTrigger>
                    </div>
                  }
                ></Form.Check>
              </Form>
              <Form.Check
                type="checkbox"
                disabled={selectedChoice === RepositoryType.PUBLIC}
                checked={redhatSatellite}
                id="disableLocal"
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
                      <FontAwesomeIcon
                        className="ms-2"
                        icon={faQuestionCircle}
                      />
                    </OverlayTrigger>
                  </div>
                }
              ></Form.Check>
            </div>
          </Row>
        </CardBody>
      </Card>
      <WizardFooter
        lifted
        isNextEnabled={nextEnabled}
        step={currentStep}
        onNext={continueAfterJdkValidation}
        onCancel={() => {
          flushStateToDb("cancel");
        }}
        onBack={async () => {
          await Promise.resolve(flushStateToDb("back"));
          handleBackImperitive();
        }}
      />
    </>
  );
}
