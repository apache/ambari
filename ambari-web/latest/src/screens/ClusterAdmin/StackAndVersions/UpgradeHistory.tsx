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
import VersionsApi from "../../../api/versionsApi";
import { AppContext } from "../../../store/context";
import { get } from "lodash";
import Table from "../../../components/Table";
import { Badge, Button } from "react-bootstrap";
import { faCaretDown, faCaretRight } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import Spinner from "../../../components/Spinner";
import { StackVersion } from "./types";
import { getUpgradeDisplayName } from "../../../Utils/Utility";
import Upgrade from "./Upgrade";

type Upgrade = {
  associated_version: string;
  cluster_name: string;
  create_time: number;
  direction: "UPGRADE" | "DOWNGRADE";
  downgrade_allowed: boolean;
  end_time: number;
  exclusive: boolean;
  pack: string;
  progress_percent: number;
  request_context: string;
  request_id: number;
  request_status: string;
  skip_failures: boolean;
  skip_service_check_failures: boolean;
  start_time: number;
  suspended: boolean;
  type: string;
  upgrade_id: number;
  upgrade_type: string;
  versions: Record<
    string,
    {
      from_repository_id: number;
      from_repository_version: string;
      to_repository_id: number;
      to_repository_version: string;
    }
  >;
};

type UpgradeItem = {
  href: string;
  Upgrade: Upgrade;
};

const options = [
  { key: "All", values: ["ALL"] },
  { key: "Upgrade", values: [{ direction: ["UPGRADE"] }] },
  { key: "Downgrade", values: [{ direction: ["DOWNGRADE"] }] },
  {
    key: "Successful Upgrade",
    values: [{ direction: ["UPGRADE"] }, { request_status: ["COMPLETED"] }],
  },
  {
    key: "Aborted Upgrade",
    values: [{ direction: ["UPGRADE"] }, { request_status: ["ABORTED"] }],
  },
  {
    key: "Failed Upgrade",
    values: [{ direction: ["UPGRADE"] }, { request_status: ["FAILED"] }],
  },
  {
    key: "Successful Downgrade",
    values: [{ direction: ["DOWNGRADE"] }, { request_status: ["COMPLETED"] }],
  },
  {
    key: "Aborted Downgrade",
    values: [{ direction: ["DOWNGRADE"] }, { request_status: ["ABORTED"] }],
  },
  {
    key: "Failed Downgrade",
    values: [{ direction: ["DOWNGRADE"] }, { request_status: ["FAILED"] }],
  },
];

export default function UpgradeHistory() {
  const [selectedOption, setSelectedOption] = useState(options[0]);
  const [stacks, SetStacks] = useState<StackVersion[]>([]);
  const [originalUpgrades, setOriginalUpgrades] = useState<UpgradeItem[]>([]);
  const [upgrades, setUpgrades] = useState<UpgradeItem[]>([]);
  const [showModal, setShowModal] = useState(false);
  const [loading, setLoading] = useState(false);
  const { clusterName } = useContext(AppContext);
  const upgradeIdRef = useRef<number>(0);

  useEffect(() => {
    async function fetchUpgrades() {
      if (originalUpgrades.length === 0) setLoading(true);

      const stacksResponse = await VersionsApi.getAllStacks(clusterName);
      const stacksArray = get(stacksResponse, "items", []);
      SetStacks(stacksArray);
      const response = await VersionsApi.getUpgradeHistory(clusterName);
      setOriginalUpgrades(get(response, "items").reverse());
      setLoading(false);
    }
    fetchUpgrades();
  }, []);

  useEffect(() => {
    if (selectedOption.key !== "All") {
      setUpgrades(
        originalUpgrades.filter((upgrade) => {
          return selectedOption.values.every((condition) => {
            const [key, values] = Object.entries(condition)[0];
            return values.includes(upgrade.Upgrade[key as keyof Upgrade]);
          });
        })
      );
    } else {
      setUpgrades(originalUpgrades);
    }
  }, [originalUpgrades, selectedOption]);

  const handleSelectChange = (event: any) => {
    const option = options.find((o) => o.key === event.target.value);
    if (option) {
      setSelectedOption(option);
    }
  };

  function getUpgradeId(href: string) {
    const id = href.split("/").pop();
    return parseInt(id || "0", 0);
  }
  function getUpgradeType(upgrade: Upgrade) {
    const upgradeType = upgrade.upgrade_type;
    const stackVersion = stacks.find(
      (stack) =>
        stack.repository_versions[0].RepositoryVersions.repository_version ===
        upgrade.associated_version
    );
    const isPatch =
      stackVersion?.repository_versions[0].RepositoryVersions.type === "PATCH";
    if (isPatch) {
      return "PATCH";
    }

    return getUpgradeDisplayName(upgradeType);
  }

  const columns = [
    {
      header: () => <span>Direction</span>,
      accessorKey: "direction",
      width: "10%",
      cell: (info: any) => {
        const [isExpanded, setIsExpanded] = useState(false);
        const toggleExpand = () => {
          setIsExpanded(!isExpanded);
        };
        const versions = get(info, "row.original.Upgrade.versions");

        const handleOpenUpgrade = () => {
          const upgradeId = getUpgradeId(get(info, "row.original.href"));
          upgradeIdRef.current = upgradeId;
          setShowModal(true);
        };

        return (
          <>
            <div>
              {isExpanded ? (
                <FontAwesomeIcon
                  icon={faCaretDown}
                  onClick={() => toggleExpand()}
                />
              ) : (
                <FontAwesomeIcon
                  icon={faCaretRight}
                  onClick={() => toggleExpand()}
                />
              )}
              <Button
                variant="link"
                size="sm"
                onClick={() => handleOpenUpgrade()}
              >
                {get(info, "row.original.Upgrade.direction")}
              </Button>
            </div>

            {isExpanded && (
              <div className="mt-2 full-width">
                <table className="table">
                  <tbody>
                    {Object.entries(versions).map(
                      ([serviceName, versionInfo]) => (
                        <tr key={serviceName}>
                          <td className="w-30">{serviceName}</td>
                          <td className="w-30">
                            <Badge>
                              {get(versionInfo, "from_repository_version")}
                            </Badge>
                          </td>
                          <td className="w-5 text-center">→</td>
                          <td className="w-35">
                            <Badge>
                              {get(versionInfo, "to_repository_version")}
                            </Badge>
                          </td>
                        </tr>
                      )
                    )}
                  </tbody>
                </table>
              </div>
            )}
          </>
        );
      },
    },
    {
      header: () => <span>Type</span>,
      accessorKey: "type",
      width: "10%",
      cell: (info: any) => {
        return getUpgradeDisplayName(
          get(info, "row.original.Upgrade.upgrade_type")
        );
      },
    },
    {
      header: () => <span>Repository</span>,
      accessorKey: "repository",
      width: "10%",
      cell: (info: any) => {
        return get(info, "row.original.Upgrade.associated_version");
      },
    },
    {
      header: () => <span>Repository Type</span>,
      accessorKey: "repository_type",
      width: "10%",
      cell: (info: any) => {
        return getUpgradeType(get(info, "row.original.Upgrade"));
      },
    },
    {
      header: () => <span>Start Time</span>,
      accessorKey: "start_time",
      width: "15%",
      cell: (info: any) => {
        const startTime = new Date(
          get(info, "row.original.Upgrade.start_time")
        );
        return (
          startTime.toDateString() +
          " " +
          startTime.toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit",
          })
        );
      },
    },
    {
      header: () => <span>Duration</span>,
      accessorKey: "duration",
      width: "10%",
      cell: (info: any) => {
        const { start_time, end_time } = info.row.original.Upgrade;

        const durationMs = end_time - start_time;
        const hours = Math.floor(durationMs / (1000 * 60 * 60));
        const minutes = Math.floor(
          (durationMs % (1000 * 60 * 60)) / (1000 * 60)
        );
        const seconds = Math.floor((durationMs % (1000 * 60)) / 1000);
        return `${hours}h ${minutes}m ${seconds}s`;
      },
    },
    {
      header: () => <span>End Time</span>,
      accessorKey: "end_time",
      width: "15%",
      cell: (info: any) => {
        const endTime = new Date(get(info, "row.original.Upgrade.end_time"));
        return (
          endTime.toDateString() +
          " " +
          endTime.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
        );
      },
    },
    {
      header: () => <span>Status</span>,
      accessorKey: "status",
      width: "10%",
      cell: (info: any) => {
        return get(info, "row.original.Upgrade.request_status");
      },
    },
  ];

  if (loading) {
    return <Spinner />;
  }

  if (originalUpgrades.length === 0) {
    return (
      <div className="text-left mt-5">
        <h2>No Upgrade History Found</h2>
      </div>
    );
  }

  return (
    <>
      {showModal && (
        <Upgrade
          upgradeId={upgradeIdRef.current}
          onlyView={true}
          onClose={() => setShowModal(false)}
        />
      )}
      <div className="mt-5">
        <div className="d-flex justify-content-between">
          <h2>Upgrade History</h2>
          <select
            value={selectedOption.key}
            onChange={handleSelectChange}
            className="w-20 mx-2 mb-1"
          >
            {options.map((option) => (
              <option key={option.key} value={option.key}>
                {option.key}
              </option>
            ))}
          </select>
        </div>
        <Table columns={columns} data={upgrades}></Table>
      </div>
    </>
  );
}