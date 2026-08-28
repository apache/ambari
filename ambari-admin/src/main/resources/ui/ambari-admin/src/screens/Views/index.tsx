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
import { Button, Col } from "react-bootstrap";
import DefaultButton from "../../components/DefaultButton";
import { useContext, useEffect, useState } from "react";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faCopy,
  faFilter,
  faPencil,
  faTrash,
  faUpRightFromSquare,
} from "@fortawesome/free-solid-svg-icons";
import ViewApi from "../../api/viewApi.ts";
import CreateInstance from "./CreateInstance.tsx";
import Paginator from "../../components/Paginator";
import Table from "../../components/Table";
import { get } from "lodash";
import usePagination from "../../hooks/usePagination.ts";
import ViewsInformationApi from "../../api/viewsApiInfo.ts";
import ConfirmationModal from "../../components/ConfirmationModal";
import Spinner from "../../components/Spinner";
import toast from "react-hot-toast";
import { Link } from "react-router-dom";
import ComboSearch from "../../components/ComboSearch";
import AppContent from "../../context/AppContext.ts";
import { latestShortViewUrl } from "../../utils/navigation.ts";
type ViewListType = {
  cluster_handle: number;
  cluster_type: string;
  context_path: string;
  description: string;
  icon64_path: null;
  icon_path: null;
  instance_name: string;
  label: string;
  static: boolean;
  version: string;
  view_name: string;
  visible: boolean;
  instance_data: object;
  properties: object;
};
export default function Views() {
  const [loading, setLoading] = useState(false);
  const [viewDetails, setViewDetails] = useState<any>([]);
  const [showCreateInstanceModal, setShowCreateInstanceModal] = useState(false);
  const [viewInstanceInfoToBeCloned, setViewInstanceInfoToBeCloned] =
    useState<any>({});

  const [showFilters, setShowFilters] = useState(false);
  const [filteredViews, setFilteredViews] = useState<
    unknown[] | ((prevState: never[]) => never[])
  >([]);

  const [views, setViews] =  useState<
  unknown[] | ((prevState: never[]) => never[])
>([]);

const {
  setSelectedOption
} = useContext(AppContent);

  const {
    currentItems,
    changePage,
    currentPage,
    maxPage,
    itemsPerPage,
    setItemsPerPage,
  } = usePagination(filteredViews);

  const [showDeleteModal, setShowDeleteModal] = useState(false);

  useEffect(() => {
    setSelectedOption("Views");
    getViewsList();
  }, []);
  const columnsInViewList = [
    {
      header: "Name",
      accessorKey: "instance_name",
      width:"30%",
    },
    {
      header: "URL",
      width: "25%",
      isLink: true,
      cell: (info: any) => {
        const viewName = get(info, "row.original.view_name", "");
        const shortUrl = get(info, "row.original.short_url", "");
        const href = latestShortViewUrl(viewName, shortUrl);
        return (
          <a
            className="custom-link"
            href={href}
            target="_blank"
            rel="noopener noreferrer"
          >
            /main/{viewName}/{shortUrl}{" "}
            <FontAwesomeIcon
              icon={faUpRightFromSquare}
              style={{ color: "#1291c1" }}
            />
          </a>
        );
      },
    },
    {
      header: "View Type",
      width: "20%",
      cell: (info: any) => {
        return (
          get(info, "row.original.view_name") +
          " {" +
          get(info, "row.original.version") +
          "}"
        );
      },
    },
    {
      header: "URL Name",
      width: "20%",
      accessorKey: "short_url_name",
    },
    {
      header: "Actions",
      width: "5%",
      cell: ({ row }: { row: any }) => {
        return (
          <div className="d-flex">
            <Link data-testid = "edit-icon"
              to={`/views/${row.original.view_name}/versions/${row.original.version}/instances/${row.original.instance_name}/edit`}
            >
              <FontAwesomeIcon
                icon={faPencil}
                className="me-3 cursor-pointer"
              />
            </Link>
            <Button
              className="btn-wrapping-icon"
              data-testid = "clone-icon"
              onClick={() => {
                setViewInstanceInfoToBeCloned(row.original);
                setShowCreateInstanceModal(true);
              }}
            >
              <FontAwesomeIcon
                icon={faCopy}
                className="me-3"
                style={{ cursor: "pointer" }}
              />
            </Button>
            <Button
              className="btn-wrapping-icon"
              data-testid = "delete-icon"
              onClick={() => {
                setShowDeleteModal(true);
                const data = {
                  viewName: `${row.original.view_name}`,
                  version: `${row.original.version}`,
                  instanceName: `${row.original.instance_name}`,
                  label: `${row.original.label}`,
                };
                setSelectedInstanceState(data);
              }}
            >
              <FontAwesomeIcon icon={faTrash} className="me-3 cursor-pointer" />
            </Button>
          </div>
        );
      },
    },
  ];
  async function getViewsList() {
    setLoading(true);
    const data: any = await ViewApi.viewsListAPI();
    const instanceDetails: ViewListType[] = (data?.items ?? []).flatMap(
      (item: { versions: any }) =>
        (item?.versions ?? []).flatMap((version: { instances: any }) =>
          (version?.instances ?? [])
            .map((instance: any) => {
              return get(instance, "ViewInstanceInfo");
            })
            .filter(Boolean)
        )
    );
    setViewDetails(data);
    // setViewsList(instanceDetails);
    setViews(instanceDetails);
    setFilteredViews(instanceDetails);
    setLoading(false);
  }
  const selectedInstance = {
    viewName: "",
    version: "",
    instanceName: "",
    label: "",
  };
  const [selectedInstanceState, setSelectedInstanceState] =
    useState(selectedInstance);
  const deleteInstance = async (
    viewName: string,
    version: string,
    instanceName: string
  ) => {
    try {
      await ViewsInformationApi.deleteInstance(
        viewName,
        version,
        instanceName
      );
      toast.success("Instance deleted successfully");
      getViewsList();
    } catch (error) {
      toast.error("Cannot delete instance");
      console.error("Failed to delete instance:", error);
    }
  };
  if (loading) {
    return <Spinner />;
  }
  return (
    <div>
      <div className="make-all-grey">
        <CreateInstance
          isOpen={showCreateInstanceModal}
          onClose={() => setShowCreateInstanceModal(false)}
          viewInstanceInfoToBeCloned={viewInstanceInfoToBeCloned}
          viewDetails={viewDetails}
          successCallback={() => {
            setViewInstanceInfoToBeCloned({});
            // getViewsList();
          }}
        />
        <Col className="d-flex justify-content-end">
          <DefaultButton onClick={() => setShowFilters(!showFilters)} data-testid="Filter button"className="me-2">
            <FontAwesomeIcon icon={faFilter} />
          </DefaultButton>
          <DefaultButton onClick={() => setShowCreateInstanceModal(true)}>
            CREATE INSTANCE
          </DefaultButton>
        </Col>
        {showFilters ? (
          <div className="d-flex">
            <ComboSearch
              fields={[
                { label: "Instance", value: "instance_name" },
                { label: "URL", value: "short_url" },
                { label: "View Type", value: "view_name" },
                {
                  label: "Name",
                  value: "short_url_name",
                },
              ]}
              valueMappings={{
                name: "short_url_name",
                viewType: "view_name",
                url: "short_url",
                instance: "instance_name",
              }}
              searchCallback={(
                filteredData: React.SetStateAction<
                  any[] | ((prevState: never[]) => never[])
                >
              ) => {
                setFilteredViews(filteredData);
              }}
              data={views}
            />
          </div>
        ) : null}
        <div className="scrollable">
          <Table
            hover
            striped
            columns={columnsInViewList}
            data={currentItems}
            entityName="views"
          />
        </div>
        {currentItems.map((item: any) => (
          <div key={item.id}>{item.name}</div>
        ))}
        <Paginator
          currentPage={currentPage}
          maxPage={maxPage}
          changePage={changePage}
          itemsPerPage={itemsPerPage}
          setItemsPerPage={setItemsPerPage}
          totalItems={filteredViews.length}
        />
        <ConfirmationModal
          isOpen={showDeleteModal}
          onClose={() => setShowDeleteModal(false)}
          modalTitle={"Delete View Instance"}
          modalBody={`Are you sure you want to delete view instance
  ? ${selectedInstanceState.label}`}
          successCallback={async () => {
            deleteInstance(
              selectedInstanceState.viewName,
              selectedInstanceState.version,
              selectedInstanceState.instanceName
            );
            setShowDeleteModal(false);
            // getViewsList();
          }}
        />
      </div>
    </div>
  );
}
