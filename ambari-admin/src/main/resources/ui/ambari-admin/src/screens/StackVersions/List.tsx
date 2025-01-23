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
/* eslint-disable @typescript-eslint/no-explicit-any */
import React, { useContext, useEffect, useState } from "react";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faFilter } from "@fortawesome/free-solid-svg-icons";
import { Badge, Dropdown, Form } from "react-bootstrap";
import { Link } from "react-router-dom";
import AppContent from "../../context/AppContext";
import usePagination from "../../hooks/usePagination";
import VersionsApi from "../../api/versions";
import Spinner from "../../components/Spinner";
import DefaultButton from "../../components/DefaultButton";
import ComboSearch from "../../components/ComboSearch";
import Table from "../../components/Table";
import Paginator from "../../components/Paginator";

enum RepoStatus {
  CURRENT = "CURRENT",
  INSTALLED = "INSTALLED",
}

const StackVersionsList = () => {
  const [repos, setRepos] = useState<
    unknown[] | ((prevState: never[]) => never[])
  >([]);

  const [loading, setLoading] = useState(false);
  const [showFilters, setShowFilters] = useState(false);
  const [filteredRepos, setFilteredRepos] = useState<
    unknown[] | ((prevState: never[]) => never[])
  >([]);
  const [clusterInformation, setClusterInformation] = useState({});
  const {
    cluster: { cluster_name: clusterName },
    setSelectedOption,
  } = useContext(AppContent);
  const {
    currentItems,
    changePage,
    currentPage,
    maxPage,
    itemsPerPage,
    setItemsPerPage,
  } = usePagination(filteredRepos);
  useEffect(() => {
    setSelectedOption("Versions");
    async function getReposData() {
      setLoading(true);
      let tempRepos: unknown[] | ((prevState: never[]) => never[]) = [];
      const allRepos = await VersionsApi.getRepos();

      const clusterData = await VersionsApi.getClusterInfo();
      const clusterInfo = clusterData.items?.[0];
      setClusterInformation(clusterInfo);
      const data = allRepos.items;
      for (const stack of data) {
        const stackVersions = stack.versions;
        for (const stackVersion of stackVersions) {
          tempRepos = [...tempRepos, ...stackVersion.repository_versions];
        }
      }
      for (const repo of tempRepos as any) {
        const stackVersion = await VersionsApi.versionsList(
          repo.RepositoryVersions.id,
          clusterName
        );
        const repoStackVersion = stackVersion?.items?.[0];
        repo.isPatch = repo.RepositoryVersions.type === "PATCH";
        repo.isMaint = repo.RepositoryVersions.type === "MAINT";
        repo.stackName = `${repo.RepositoryVersions.stack_name}-${repo.RepositoryVersions.stack_version}`;
        repo.status = repoStackVersion?.ClusterStackVersions.state;
        const hostStatus = repoStackVersion?.ClusterStackVersions?.host_states;
        const currentHosts = hostStatus?.["CURRENT"]?.length;
        const installedHosts = hostStatus?.["INSTALLED"]?.length;
        let totalHosts = 0;
        for (const status in hostStatus) {
          totalHosts += hostStatus[status].length;
        }
        repo.currentHosts = currentHosts;
        repo.installedHosts = installedHosts;
        repo.totalHosts = totalHosts;
        repo.stackVersionId = repoStackVersion?.ClusterStackVersions.id;
        repo.cluster =
          repo.status === RepoStatus.CURRENT ||
          repo.status === RepoStatus.INSTALLED
            ? clusterInfo?.Clusters?.cluster_name
            : "";
      }
      setLoading(false);
      setRepos(tempRepos);
      setFilteredRepos(tempRepos);
    }
    if (clusterName) getReposData();
  }, [clusterName]);
  const columns = [
    {
      header: "Stack",
      accessorKey: "stackName",
      id: "Stack Name",
    },
    {
      header: "Name",
      accessorKey: "RepositoryVersions.display_name",
      id: "name",
      cell: ({ row }: { row: any }) => {
        return (
          <Link
            className="custom-link"
            to={`/stackVersions/${row.original.stackName.split("-")?.[0]}/${
              row.original.RepositoryVersions.repository_version
            }/edit`}
          >
            {row.original.RepositoryVersions.display_name}
          </Link>
        );
      },
    },
    {
      header: "Type",
      accessorKey: "RepositoryVersions.type",
      id: "type",
    },
    {
      header: "Version",
      accessorKey: "RepositoryVersions.repository_version",
      id: "version",
    },
    {
      header: "Cluster",
      accessorKey: "cluster",
      id: "cluster",
      cell: ({ row }: { row: any }) => {
        return !row.original.cluster ? (
          <div>None</div>
        ) : (
          <a className="custom-link" href="/#/main/admin/stack/versions">
            {row.original.cluster}
          </a>
        );
      },
    },
    {
      header: "Status",
      id: "status",
      cell: ({ row }: { row: any }) => {
        const { status, currentHosts, totalHosts,installedHosts } = row.original;
        const requiredHosts=status==="CURRENT"?currentHosts:installedHosts;
        const statusString = `${status}:${requiredHosts}/${totalHosts}`;
        if (row.original.cluster) {
          return (
            <Badge bg="success" className="p-2 bold">
              {statusString}
            </Badge>
          );
        }
        return (
          <Dropdown>
            <Dropdown.Toggle
              id="dropdown-basic"
              variant="light"
              size="sm"
              style={{ fontSize: 12 }}
            >
              INSTALL ON
            </Dropdown.Toggle>
            <Dropdown.Menu>
              <Dropdown.Item>
                <div
                  onClick={() => {
                    window.location.replace("/#/main/admin/stack/versions");
                  }}
                >
                  {((clusterInformation as any)?.Clusters
                    ?.cluster_name as string) || ""}
                </div>
              </Dropdown.Item>
            </Dropdown.Menu>
          </Dropdown>
        );
      },
    },

    {
      header: "Hidden",
      id: "hidden",
      cell: ({ row }: { row: any }) => {
        return (
          <Form>
            <Form.Check
              inline
              className="hiddencheckbox"
              disabled={row.original.cluster}
              label=""
              onChange={(e) => {
                toggleHiddenFor(row.original, e.target.checked);
              }}
              type="checkbox"
              checked={row.original.RepositoryVersions.hidden}
              id={`${row.original.RepositoryVersions.id}`}
            />
          </Form>
        );
      },
    },
  ];

  const toggleHiddenFor = async (repo: any, checked: boolean) => {
    const repoIndex = (repos as any[]).findIndex(
      (repository) =>
        repository.RepositoryVersions.id === repo.RepositoryVersions.id
    );
    const newRepos: any[] = [...(repos as any[])];
    newRepos[repoIndex].RepositoryVersions.hidden = checked;
    setRepos(newRepos);
    await VersionsApi.saveRepoVersions(
      repo.stackName,
      repo.RepositoryVersions.repository_version,
      repo.RepositoryVersions.id,
      { RepositoryVersions: { hidden: checked } }
    );
  };

  if (loading) {
    return <Spinner />;
  }
  return (
    <div>
      <div className="d-flex w-100 justify-content-end">
        <DefaultButton
          onClick={() => {
            setShowFilters(!showFilters);
          }}
          className="d-flex align-items-center p-2"
        >
          <FontAwesomeIcon className="text-muted" icon={faFilter} />
        </DefaultButton>
        <Link to="/stackVersions/create">
          <DefaultButton className="ms-3">REGISTER VERSION</DefaultButton>
        </Link>
      </div>

      {showFilters ? (
        <div className="d-flex">
          <ComboSearch
            fields={[
              { label: "Stack", value: "stackName" },
              { label: "Name", value: "RepositoryVersions.display_name" },
              { label: "Type", value: "RepositoryVersions.type" },
              {
                label: "Version",
                value: "RepositoryVersions.repository_version",
              },
              { label: "Cluster", value: "cluster" },
            ]}
            valueMappings={{
              stack: "stackName",
              name: "RepositoryVersions.display_name",
              type: "RepositoryVersions.type",
              version: "RepositoryVersions.repository_version",
              cluster: "cluster",
            }}
            searchCallback={(
              filteredData: React.SetStateAction<
                any[] | ((prevState: never[]) => never[])
              >
            ) => {
              setFilteredRepos(filteredData);
            }}
            data={repos}
          />
        </div>
      ) : null}

      <Table
        striped={true}
        hover={true}
        data={currentItems}
        columns={columns}
      />
      {currentItems.map((item: any) => (
        <div key={item.id}>{item.name}</div>
      ))}
      <div className="d-flex justify-content-end mt-">
        <Paginator
          currentPage={currentPage}
          maxPage={maxPage}
          changePage={changePage}
          itemsPerPage={itemsPerPage}
          setItemsPerPage={setItemsPerPage}
          totalItems={filteredRepos.length}
        />
      </div>
    </div>
  );
};

export default StackVersionsList;
