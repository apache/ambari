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
import { useContext, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { get } from "lodash";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faFilter } from "@fortawesome/free-solid-svg-icons";
import Spinner from "../../../components/Spinner";
import Table from "../../../components/Table";
import DefaultButton from "../../../components/DefaultButton";
import Paginator from "../../../components/Paginator";
import usePagination from "../../../hooks/usePagination";
import RemoteClusterApi from "../../../api/remoteCluster.ts";
import ComboSearch from "../../../components/ComboSearch";
import AppContent from "../../../context/AppContext";

export default function RemoteClusters() {
  const [remoteClusters, setRemoteClusters] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showFilters, setShowFilters] = useState(false);
  const [filteredRemoteClusters, setFilteredRemoteClusters] = useState<
    unknown[] | ((prevState: never[]) => never[])
  >([]);
  const {
    currentItems,
    changePage,
    currentPage,
    maxPage,
    itemsPerPage,
    setItemsPerPage,
  } = usePagination(filteredRemoteClusters);

  const {
    setSelectedOption
  } = useContext(AppContent);

  useEffect(() => {
    setSelectedOption("Remote Clusters");
    async function getRemoteClusters() {
      setLoading(true);
      const data: any = await RemoteClusterApi.getRemoteClusters();
      setRemoteClusters(data);
      setFilteredRemoteClusters(data);
      setLoading(false);
    }
    getRemoteClusters();
  }, []);

  const columnViewList = [
    {
      header: "Cluster Name",
      accessorKey: "ClusterInfo.name",
      cell: (info: any) => {
        return (
          <div>
            <Link
              className="custom-link"
              to={`/remoteClusters/${get(
                info,
                "row.original.ClusterInfo.name"
              )}/edit`}
            >
              {get(info, "row.original.ClusterInfo.name")}
            </Link>
          </div>
        );
      },
    },
    {
      header: "Services",
      accessorKey: "ClusterInfo.services",
      cell: (info: any) => {
        const services = get(info, "row.original.ClusterInfo.services");
        const uniqueServices = [...new Set(services)];
        return uniqueServices.join(", ");
      },
    },
  ];

  if (loading) {
    return <Spinner />;
  }
  return (
    <>
      <div className="d-flex justify-content-end pb-3">
        <DefaultButton
          onClick={() => {
            setShowFilters(!showFilters);
          }}
          className="me-2"
        >
          <FontAwesomeIcon icon={faFilter} />
        </DefaultButton>
        <Link to={`/remoteClusters/create`}>
          <DefaultButton>Register Remote cluster</DefaultButton>
        </Link>
      </div>
      <div>
        {showFilters ? (
          <div className="d-flex">
            <ComboSearch
              fields={[
                { label: "Cluster Name", value: "ClusterInfo.name" },
                { label: "Services", value: "ClusterInfo.services" },
              ]}
              valueMappings={{
                clusterName: "ClusterInfo.name",
                services: "ClusterInfo.services",
              }}
              searchCallback={(
                filteredData: React.SetStateAction<
                  any[] | ((prevState: never[]) => never[])
                >
              ) => {
                setFilteredRemoteClusters(filteredData);
              }}
              data={remoteClusters}
            />
          </div>
        ) : null}

        <div className="scrollable">
          <Table
            columns={columnViewList}
            data={currentItems}
            entityName="Remote Clusters"
          />
        </div>
         <Paginator
          currentPage={currentPage}
          maxPage={maxPage}
          changePage={changePage}
          itemsPerPage={itemsPerPage}
          setItemsPerPage={setItemsPerPage}
          totalItems={filteredRemoteClusters.length}
        />
      </div>
    </>
  );
}
