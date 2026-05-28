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

//@ts-nocheck
import React, { useState, useEffect, useContext } from "react";
import {
  Badge,
  Button,
  Card,
  Dropdown,
  FormControl,
  InputGroup,
  OverlayTrigger,
  Tooltip,
} from "react-bootstrap";
import Modal from "../../components/Modal";
import dayjs from "dayjs";
import "../../../custom.scss";
import { faExchangeAlt, faSearch } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import ClusterApi from "../../api/clusterApi";
import KerberosApi from "../../api/kerberosApi";
import { ServiceConfigApi } from "../../api/serviceConfigApi";
import { AppContext } from "../../store/context";
import { toast } from "react-hot-toast";
import Config from "../CommonConfigs/Config";

interface VersionsListProps {
  serviceName: string;
  onVersionChange: any;
  configGroup:string;
  isComparing: boolean;
  setIsComparing: (isComparing: boolean) => void;
  setVersionCompared: (version: string) => void;
  versionToShow: string;
  setCurrentVersion?: (version: string) => void;
  firstVersion?: string; // Add firstVersion to know which version is already selected for comparison
  versionCompared?: string; // Add versionCompared to know the other comparison version
  onMakeCurrentComplete?: () => void; // Callback to notify parent when Make Current completes
}

export const VersionsList = ({
  serviceName,
  onVersionChange,
  configGroup,
  isComparing,
  setIsComparing,
  setVersionCompared,
  versionToShow,
  setCurrentVersion:setCurrentVersionParent,
  firstVersion,
  versionCompared,
  onMakeCurrentComplete
}: VersionsListProps) => {
  const [versionsData, setVersionsData] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [currentVersion, setCurrentVersion] = useState(versionToShow);
  const [previousVersion, setPreviousVersion] = useState(currentVersion);
  const [showModal, setShowModal] = useState(false);
  const [serviceConfigVersionNote, setServiceConfigVersionNote] = useState("");
  const [makeCurrentNote, setMakeCurrentNote] = useState("");
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const selectedServices = [
    "RANGER_KMS",
    "AMBARI_METRICS",
    "RANGER",
    "YARN",
    "ZOOKEEPER",
    "HIVE",
    "TEZ",
    "HBASE",
    "MAPREDUCE2",
    "SPARK3",
  ];

  const {clusterName} = useContext(AppContext);

  useEffect(()=>{
    setCurrentVersionParent?.(currentVersion)
  },[currentVersion])

  useEffect(() => {
    async function fetchVersionsListData() {
      try {
        const response = await ServiceConfigApi.getServiceConfig(
          clusterName,
          serviceName
        );
        const data = response.data.items.map((item) => {
          return {
            createdAt: item.createtime,
            user: item.user,
            version: item.service_config_version,
            stackID: item.stack_id,
            serviceConfigVersionNote: item.service_config_version_note,
            isCurrent: item.is_current,
            configGroup: item.group_name,
          };
        });
        setVersionsData(data);
      } catch (error) {
        console.error("Failed to load versions list data");
      }
    }
    fetchVersionsListData();
  }, []);

  useEffect(() => {
    const current = filteredVersions.find((item) => item.isCurrent);
    if (current) {
      if(!isComparing) {
        setCurrentVersion(current.version);
      }
      setPreviousVersion(current.version);
      onVersionChange(current.version);
    }
  }, [versionsData, configGroup]);

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
  };

  const filteredVersions = versionsData.filter((item) => {
    const searchString = `Version ${item.version} ${item.stackID} ${
      item.serviceConfigVersionNote || ""
    }`.toLowerCase();
    const matchesSearch = searchString.includes(searchTerm.toLowerCase().trim()) && item.configGroup===configGroup;
    
    // If in comparison mode, exclude versions that are already selected
    if (isComparing) {
      const isAlreadySelected = item.version === firstVersion || item.version === versionCompared;
      return matchesSearch && !isAlreadySelected;
    }
    
    return matchesSearch;
  });

  const handleVersionClick = (version) => {
    setCurrentVersion(version);
    onVersionChange(version);
  };

  const handleMakeCurrentBtnClick = async () => {
    setShowModal(false);
    try {
      const currentVersionData = versionsData.find(
        (item) => item.version === currentVersion
      );
      if (currentVersionData) {
        const noteToUse =
          makeCurrentNote || currentVersionData.serviceConfigVersionNote;
        setServiceConfigVersionNote(noteToUse);
        await updateClusterDetails(noteToUse);
        await fetchDesiredClusterConfigs();
        await setVersion();
        await fetchKerberosDescriptorProperties();
        await updateNewVersionAfterMakeCurrent();
        
        if (onMakeCurrentComplete) {
          onMakeCurrentComplete();
        }
      }
    } catch (error) {
      console.error("Failed to set current version", error);
    }
  };

  const updateNewVersionAfterMakeCurrent = async () => {
    const response = await ServiceConfigApi.getServiceConfig(
      clusterName,
      serviceName
    );
    const data = response.data.items.map((item) => {
      return {
        createdAt: item.createtime,
        user: item.user,
        version: item.service_config_version,
        stackID: item.stack_id,
        serviceConfigVersionNote: item.service_config_version_note,
        isCurrent: item.is_current,
      };
    });
    const latestVersion = data[0].version;
    setVersionsData(data);
    setCurrentVersion(latestVersion);
  };

  const updateClusterDetails = async (noteToUse) => {
    try {
      const updateClusterPayload = {
        Clusters: {
          desired_service_config_versions: {
            service_config_version: currentVersion,
            service_config_version_note: noteToUse,
            service_name: serviceName,
          },
        },
      };
      await ClusterApi.fetchClusterDetails(updateClusterPayload, clusterName);
    } catch (error) {
      console.error("Failed to update cluster details", error);
      throw error;
    }
  };

  const fetchDesiredClusterConfigs = async () => {
    try {
      await ClusterApi.getDesiredClusterConfigs(clusterName);
    } catch (error) {
      console.error("Failed to fetch desired cluster configs", error);
      throw error;
    }
  };

  const setVersion = async () => {
    try {
      await ServiceConfigApi.setIsCurrent(clusterName, selectedServices);
    } catch (error) {
      console.error("Failed to set current version", error);
      throw error;
    }
  };

  const fetchKerberosDescriptorProperties = async () => {
    try {
      await KerberosApi.getKerberosDescriptorProperties("true", clusterName);
    } catch (error) {
      console.error("Failed to fetch Kerberos descriptor properties", error);
      throw error;
    }
  };

  const handleCompareVersions = async (version: string) => {
    setIsComparing(true);
    setVersionCompared(version);
  };
  const renderTooltip = (props) => (
    <Tooltip id="version-diff-button-tooltip" {...props}>
      <span style={{ fontSize: "12px", display: "inline" }}>
        Compare this version with current
      </span>
    </Tooltip>
  );

  return (
    <div>
      <Dropdown
        className="ms-2 bg-white"
        show={isDropdownOpen}
        onToggle={() => setIsDropdownOpen(!isDropdownOpen)}
      >
        <Dropdown.Toggle
          id="version-dropdown-button"
          variant="white"
          className="rounded-1 border-1 hover-effect-version-dropdown"
        >
          Version:{" "}
          <strong style={{ fontWeight: "bold" }}>{currentVersion}</strong>
        </Dropdown.Toggle>
        <span id="make-current-btn" className="ms-1">
          {previousVersion !== currentVersion && !isComparing ? (
            <Button
              className="rounded-1 border-1 hover-effect-make-current-btn"
              onClick={() => {
                const currentVersionData = versionsData.find(
                  (item) => item.version === currentVersion
                );
                if (currentVersionData) {
                  const noteToUse =
                   `Created from service config version V${currentVersion}` 
                  setServiceConfigVersionNote(noteToUse);
                  setMakeCurrentNote(noteToUse);  
                }
                setShowModal(true);
              }}
            >
              MAKE CURRENT
            </Button>
          ) : (
            <></>
          )}
        </span>
        <Dropdown.Menu
          className="dropdown-menu-scrollable bg-white border-1 rounded-0"
          style={{ width: "620px", height: "405px" }}
        >
          <div className="dropdown-header">
            <InputGroup>
              <FormControl
                autoFocus
                className="ms-2 border-1 rounded-0 bg-transparent version-search"
                placeholder="Search"
                onChange={handleSearchChange}
                value={searchTerm}
              />
              <InputGroup.Text className="bg-transparent border-1 search-icon-box">
                <FontAwesomeIcon
                  icon={faSearch}
                  className="text-muted search-icon"
                  onClick={() => {
                    setIsDropdownOpen(false);
                  }}
                />
              </InputGroup.Text>
            </InputGroup>
          </div>
          <div
            // style={{ maxHeight: '60vh', maxWidth: '40vw', overflowY: 'auto' }}
            style={{ height: "340px", overflowY: "auto" }}
          >
            {filteredVersions.map((item) => (
              <div
                key={item.createdAt}
                className="d-flex align-items-center w-100"
              >
                <Card
                  key={item.createdAt}
                  className={`d-flex ms-4 mt-3 shadow rounded-0 position-relative version-card 
                            ${
                              item.version === currentVersion
                                ? "version-card-current"
                                : "version-card-default"
                            }`}
                >
                  <Dropdown.Item
                    eventKey={item.createdAt}
                    className="text-wrap"
                    style={{ maxWidth: "90%", height: "10vh" }}
                    onClick={() => handleVersionClick(item.version)}
                  >
                    <div>
                      <strong>Version {item.version}</strong>
                      <span className="ms-2 text-muted">{item.stackID}</span>
                    </div>
                    { 
                      <div
                        className="d-flex align-items-center text-muted mt-4 mb-0 bottom"
                      >
                        <span
                          className="text-muted position-absolute bottom mb-0 mt-3 smallText-versionsList text-truncate"
                          style={{ maxWidth: "400px" }}
                          title={item.serviceConfigVersionNote || ""}
                        >
                          {item.serviceConfigVersionNote? item.serviceConfigVersionNote:null}
                        </span>
                        <span id="item-is-current">
                          {item.isCurrent && (
                            <Badge
                              bg="transparent"
                              className="position-absolute bottom-0 end-0 m-0 mb-1 me-2"
                            >
                              <strong className="text-success">
                                Current
                              </strong>
                            </Badge>
                          )}
                        </span>
                      </div>
                    }
                    <div
                      className="position-absolute top-0 end-0 mt-2 me-2 text-muted smallText-versionsList"
                    >
                      <strong>{item.user}</strong> authored on{" "}
                      <strong>
                        {dayjs(item.createdAt).format(
                          "ddd, MMM DD, YYYY hh:mm A"
                        )}
                      </strong>
                    </div>
                  </Dropdown.Item>
                </Card>
                {item.version !== currentVersion && !isComparing && (
                  <OverlayTrigger placement="top" overlay={renderTooltip}>
                    <span className="pointer-transition">
                      {/*<FaExchangeAlt className="ms-2" onClick={() => handleCompareVersions(item.version)} />*/}
                      <InputGroup.Text className="bg-transparent border-1 search-icon-box ms-1">
                        <FontAwesomeIcon
                          icon={faExchangeAlt}
                          className="search-icon"
                          onClick={() => handleCompareVersions(item.version)}
                        />
                      </InputGroup.Text>
                    </span>
                  </OverlayTrigger>
                )}
              </div>
            ))}
          </div>
        </Dropdown.Menu>
      </Dropdown>
      <Modal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        modalTitle="Make Current Confirmation"
        modalBody={
          <div className="d-flex">
            <span className="text-muted me-3">Notes</span>
            <span className="align-items-center">
              <FormControl
                className="rounded-0 border-1"
                as="textarea"
                placeholder={serviceConfigVersionNote}
                value={makeCurrentNote}
                onChange={(e) => setMakeCurrentNote(e.target.value)}
                style={{ resize: "vertical", width: "25vw" }}
                onFocus={(e) => {
                  e.target.style.borderColor = "lightblue";
                  e.target.style.boxShadow = "0 0 5px rgba(0, 123, 255, 0.5)";
                }}
                onBlur={(e) => {
                  e.target.style.borderColor = "lightgrey";
                  e.target.style.boxShadow = "none";
                }}
              />
            </span>
          </div>
        }
        successCallback={handleMakeCurrentBtnClick}
        options={{
          okButtonText: "MAKE CURRENT",
          cancelButtonText: "CANCEL",
          extraButtons: [
            {
              text: "DISCARD",
              onClick: () => setShowModal(false),
              variant: "outline-secondary",
              className: "bg-transparent text-muted border-1 rounded-1 border-dark-subtle hover-effect-version-dropdown",
              order: 1
            }
          ]
        }}
      />
    </div>
  );
  //}
};

export default VersionsList;
