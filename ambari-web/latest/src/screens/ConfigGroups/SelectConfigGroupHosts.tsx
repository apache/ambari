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

import {
  Alert,
  Button,
  Dropdown,
  Form,
  Modal as ReactModal,
} from "react-bootstrap";
import DefaultButton from "../../components/DefaultButton";
import Table from "../../components/Table";
import { useEffect, useState } from "react";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCheck } from "@fortawesome/free-solid-svg-icons";
import usePagination from "../../hooks/usePagination";
import Paginator from "../../components/Paginator";
import { cloneDeep, find, get, set } from "lodash";
import { HostType } from "./types";
import bytesToSize from "../../Utils/numberUtils";
import {
  componentsListToKeyMapping,
  hostDataOptionToKeyMapping,
} from "./constants";

type SelectConfigGroupHostsProps = {
  isOpen: boolean;
  onClose: () => void;
  successCallback: (hostsToBeAdded: string[]) => void;
  configGroupName: string;
  hostsList: HostType[];
};

export default function SelectConfigGroupHosts({
  isOpen,
  onClose,
  successCallback,
  configGroupName,
  hostsList,
}: SelectConfigGroupHostsProps) {
  const [hosts, setHosts] = useState<HostType[]>([]);
  const [selectedComponent, setSelectedComponent] =
    useState<string>("DataNode");
  const [selectedHostDataType, setSelectedHostDataType] =
    useState<string>("IP Address");
  const [selectedHostsClicked, setSelectedHostsClicked] =
    useState<boolean>(false);
  const [filterString, setFilterString] = useState<string>("");

  useEffect(() => {
    const currentHosts = hostsList.map((host) => {
      set(host, "isChecked", false);
      set(host, "isShown", true);
      let totalDiskCapacity =
        get(host, "Hosts.disk_info", []).reduce((total, disk: any) => {
          return total + parseFloat(disk.size);
        }, 0) || undefined;
      set(
        host,
        "Hosts.total_disk_capacity",
        bytesToSize(totalDiskCapacity, 0, undefined, 1024)
      );
      set(
        host,
        "Hosts.disk_info_size",
        get(host, "Hosts.disk_info", []).length
      );
      return host;
    });
    setHosts(currentHosts);
  }, []);

  useEffect(() => {
    if (hosts.length) {
      let hostsCopy = cloneDeep(hosts);
      hostsCopy.forEach((host: HostType) => {
        let isHostShown = host.isShown;
        if (selectedComponent) {
          if (
            get(host, "host_components", []).some(
              (component: any) =>
                get(component, "HostRoles.component_name", "") ===
                get(componentsListToKeyMapping, selectedComponent, "")
            )
          ) {
            isHostShown = true;
          } else {
            isHostShown = false;
          }
        }
        if (filterString) {
          if (isHostShown) {
            if (
              get(host, "Hosts.host_name", "")
                .toLowerCase()
                .includes(filterString) ||
              get(
                host,
                "Hosts." +
                  get(hostDataOptionToKeyMapping, selectedHostDataType, ""),
                ""
              )
                ?.toString()
                .includes(filterString)
            ) {
              isHostShown = true;
            } else {
              isHostShown = false;
            }
          }
        }
        host.isShown = isHostShown;
      });
      setHosts(hostsCopy);
    }
  }, [selectedComponent, filterString]);

  useEffect(() => {
    if (hosts.length) {
      let hostsCopy = cloneDeep(hosts);
      hostsCopy.forEach((host: HostType) => {
        if (selectedHostsClicked) {
          if (host.isChecked) {
            host.isShown = true;
          } else {
            host.isShown = false;
          }
        } else {
          host.isShown = true;
        }
      });
      setHosts(hostsCopy);
    }
  }, [selectedHostsClicked]);

  const getHostsWithProperty = (propertyName: string) => {
    return hosts.filter((host: HostType) => get(host, propertyName));
  };

  const {
    currentItems,
    changePage,
    currentPage,
    maxPage,
    itemsPerPage,
    setItemsPerPage,
  } = usePagination(getHostsWithProperty("isShown"));

  const isAllSelected = () => {
    if (!hosts.length) return false;
    return hosts.every((host) => host.isChecked);
  };

  const handleSelect = (host: HostType) => {
    const hostsCopy = cloneDeep(hosts);
    const correspondingHost = find(hostsCopy, function (_host) {
      return get(_host, "Hosts.host_name") === get(host, "Hosts.host_name");
    });
    if (correspondingHost) {
      correspondingHost.isChecked = !correspondingHost.isChecked;
    }
    setHosts(hostsCopy);
  };

  const handleSelectAll = () => {
    const newSelectValue = !isAllSelected();
    const hostsToBeDisplayed = getHostsWithProperty("isShown");
    let hostsCopy = cloneDeep(hosts);
    hostsCopy.forEach((host) => {
      if (
        hostsToBeDisplayed.some(
          (_host) =>
            get(_host, "Hosts.host_name") === get(host, "Hosts.host_name")
        )
      ) {
        host.isChecked = newSelectValue;
      }
    });
    setHosts(hostsCopy);
  };

  const columnsInHostsList = [
    {
      header: (
        <Form.Check
          type="checkbox"
          className="custom-checkbox"
          checked={isAllSelected()}
          onChange={handleSelectAll}
        />
      ),
      id: "selectAll",
      width: "1%",
      cell: (info: any) => {
        return (
          <Form.Check
            type="checkbox"
            className="custom-checkbox"
            checked={get(info, "row.original.isChecked")}
            onChange={() => handleSelect(get(info, "row.original"))}
          />
        );
      },
    },
    {
      header: "Host",
      accessorKey: "Hosts.host_name",
      width: "49%",
      id: "host",
    },
    {
      header: selectedHostDataType,
      accessorKey: `Hosts.${get(
        hostDataOptionToKeyMapping,
        selectedHostDataType
      )}`,
      id: selectedHostDataType,
    },
  ];

  return (
    <div>
      <ReactModal
        show={isOpen}
        onHide={onClose}
        size="lg"
        className="custom-modal-container modal-lg make-scrollable custom-scrollbar"
      >
        <ReactModal.Header closeButton className="text-muted">
          <h2>Select Configuration Group Hosts</h2>
        </ReactModal.Header>
        <Form
          onSubmit={() =>
            successCallback(
              getHostsWithProperty("isChecked").map((host) =>
                get(host, "Hosts.host_name", "")
              )
            )
          }
        >
          <ReactModal.Body>
            <Alert variant="info" className="text-muted fs-12 mb-4">
              Select hosts that should belong to this {configGroupName}{" "}
              Configuration Group. All hosts belonging to this group will have
              the same set of configurations.
            </Alert>
            <div className="d-flex justify-content-between mb-2">
              <div className="pt-2">
                <span
                  className="custom-link fs-12 me-1"
                  onClick={() => setSelectedHostsClicked(!selectedHostsClicked)}
                >
                  {hosts?.filter((host) => host.isChecked)?.length} out of{" "}
                  {hosts?.length} hosts selected
                </span>
                {selectedHostsClicked ? (
                  <span className="circle">
                    <FontAwesomeIcon
                      icon={faCheck}
                      className="text-white fs-10"
                    />
                  </span>
                ) : null}
              </div>
              <div className="d-flex">
                <Dropdown className="me-3" drop="down">
                  <div className="d-flex">
                    <Form.Control
                      type="text"
                      placeholder="Filter..."
                      className="fs-12 custom-form-control rounded-start-1"
                      value={filterString}
                      onChange={(e) => setFilterString(e.target.value)}
                    />
                    <Dropdown.Toggle
                      split
                      variant="transparent"
                      className="btn-dropdown rounded-end-1 ps-3 pe-3"
                    />
                  </div>
                  <Dropdown.Menu flip={false} className="rounded-0">
                    {Object.keys(hostDataOptionToKeyMapping).map((item) => (
                      <Dropdown.Item
                        key={item}
                        onClick={() => setSelectedHostDataType(item)}
                      >
                        {selectedHostDataType === item ? (
                          <span className="circle">
                            <FontAwesomeIcon
                              icon={faCheck}
                              className="text-white fs-10"
                            />
                          </span>
                        ) : null}{" "}
                        {item}
                      </Dropdown.Item>
                    ))}
                  </Dropdown.Menu>
                </Dropdown>
                <Dropdown drop="down">
                  <Dropdown.Toggle
                    variant="transparent"
                    className="btn-default ps-3 pe-3"
                  >
                    <span className="me-2">COMPONENTS</span>
                  </Dropdown.Toggle>
                  <Dropdown.Menu flip={false} className="rounded-0">
                    {Object.keys(componentsListToKeyMapping).map((item) => (
                      <Dropdown.Item
                        key={item}
                        onClick={() => setSelectedComponent(item)}
                      >
                        {selectedComponent === item ? (
                          <span className="circle">
                            <FontAwesomeIcon
                              icon={faCheck}
                              className="text-white fs-10"
                            />
                          </span>
                        ) : null}{" "}
                        {item}
                      </Dropdown.Item>
                    ))}
                  </Dropdown.Menu>
                </Dropdown>
              </div>
            </div>
            <div className="scrollable-table">
              <Table data={currentItems} columns={columnsInHostsList as any} />
            </div>
            <Paginator
              currentPage={currentPage}
              maxPage={maxPage}
              changePage={changePage}
              itemsPerPage={itemsPerPage}
              setItemsPerPage={setItemsPerPage}
              totalItems={hosts.length}
            />
          </ReactModal.Body>
          <ReactModal.Footer>
            <DefaultButton onClick={onClose}>CANCEL</DefaultButton>
            <Button type="submit" className="custom-btn text-white">
              OK
            </Button>
          </ReactModal.Footer>
        </Form>
      </ReactModal>
    </div>
  );
}
