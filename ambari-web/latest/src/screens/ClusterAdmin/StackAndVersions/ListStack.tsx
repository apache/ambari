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
import VersionsApi from "../../../api/versionsApi";
import toast from "react-hot-toast";
import Spinner from "../../../components/Spinner";
import Table from "../../../components/Table";
import { Alert, Badge, Button, Form, InputGroup } from "react-bootstrap";
import { AppContext } from "../../../store/context";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../../hooks/useAuth";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faEdit, faTrash } from "@fortawesome/free-solid-svg-icons";

interface Service {
  name: string;
  version: string;
  description: string;
  status: string;
}

type FallbackRepositoryRow = {
  osType: string;
  repository: any;
};

export default function ListStack() {
  const [services, setServices] = useState<Service[]>([]);
  const [serviceDetails, setServiceDetails] = useState<any[]>([]);
  const [fallbackRepositories, setFallbackRepositories] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [loadAttempt, setLoadAttempt] = useState(0);
  const [editingRepository, setEditingRepository] = useState<string | null>(null);
  const [repositoryUrl, setRepositoryUrl] = useState("");
  const [savingRepository, setSavingRepository] = useState<string | null>(null);
  const [repositorySaveError, setRepositorySaveError] = useState<{ key: string; message: string } | null>(null);
  const {
    clusterName,
    cluster,
    services: installedServices,
    serviceComponentInfo,
    supports,
    upgradeIsRunning,
    isNonWizardUser,
  } = useContext(AppContext);
  const navigate = useNavigate();

  // Authorization hooks - implementing Ember.js service authorization patterns
  const { hasAuthorization } = useAuth();
  
  // Check specific authorizations for service operations
  const canAddDeleteServices = hasAuthorization('SERVICE.ADD_DELETE_SERVICES');
  const canUpgradeDowngrade = hasAuthorization('CLUSTER.UPGRADE_DOWNGRADE_STACK');
  const canToggleKerberos = hasAuthorization('CLUSTER.TOGGLE_KERBEROS');
  const upgradeBlocksServiceChanges = upgradeIsRunning && !supports.opsDuringRollingUpgrade;

  useEffect(() => {
    const fetchServices = async () => {
      try {
        setLoading(true);
        setLoadError(null);
        const response = await VersionsApi.getServices(clusterName);
        
        const currentItem = response.items.find(
          (item: any) => item.ClusterStackVersions.state === "CURRENT"
        );
        
        if (!currentItem) {
          const [stackName, stackVersion] = String(cluster?.version || "").split("-");
          if (!stackName || !stackVersion) {
            throw new Error("No current stack version was returned by the server");
          }
          const installedServiceNames = new Set(
            installedServices.map((service: any) => service.ServiceInfo?.service_name)
          );
          const stackServiceDetails = serviceComponentInfo?.items || [];
          setServices(stackServiceDetails.map((service: any) => ({
            name: service.StackServices?.display_name || service.StackServices?.service_name,
            version: service.StackServices?.service_version || stackVersion,
            description: service.StackServices?.comments || "",
            status: installedServiceNames.has(service.StackServices?.service_name)
              ? "Installed"
              : "Add Service",
          })));
          setServiceDetails(stackServiceDetails.map((service: any) => ({
            name: service.StackServices?.service_name,
            display_name: service.StackServices?.display_name,
          })));
          const repositoryResponse = await VersionsApi.getStackOperatingSystems(stackName, stackVersion);
          setFallbackRepositories(repositoryResponse.items || []);
          setEditingRepository(null);
          setRepositorySaveError(null);
          return;
        }
        
        const serviceDetails =
          currentItem.repository_versions?.[0]?.RepositoryVersions
            ?.stack_services || [];
        const serviceSummary =
          currentItem.ClusterStackVersions.repository_summary?.services || {};
        const combinedServices: Service[] = serviceDetails.map(
          (service: any) => {
            return {
              name: service.display_name,
              version: service.versions[0],
              description: service.comment,
              status: serviceSummary[service.name]
                ? "Installed"
                : "Add Service",
            };
          }
        );

        setServices(combinedServices);
        setServiceDetails(serviceDetails); // Store service details for navigation
        setFallbackRepositories([]);
      } catch (err: any) {
        const message = err?.response?.data?.message || err?.message || "Stack data could not be loaded";
        setLoadError(message);
        toast.error(message);
      } finally {
        setLoading(false);
      }
    };

    void fetchServices();
  }, [clusterName, cluster?.version, installedServices, loadAttempt, serviceComponentInfo]);

  function fallbackRepositoryKey(row: FallbackRepositoryRow) {
    return `${row.osType}:${row.repository.Repositories.repo_id}`;
  }

  function editFallbackRepository(row: FallbackRepositoryRow) {
    setEditingRepository(fallbackRepositoryKey(row));
    setRepositoryUrl(row.repository.Repositories.base_url || "");
    setRepositorySaveError(null);
  }

  function cancelFallbackRepositoryEdit() {
    setEditingRepository(null);
    setRepositoryUrl("");
    setRepositorySaveError(null);
  }

  async function saveFallbackRepository(row: FallbackRepositoryRow, verifyBaseUrl: boolean) {
    const key = fallbackRepositoryKey(row);
    if (savingRepository || !repositoryUrl.trim()) return;
    const [stackName, stackVersion] = String(cluster?.version || "").split("-");
    if (!stackName || !stackVersion) {
      setRepositorySaveError({ key, message: "The current stack version could not be determined" });
      return;
    }

    setSavingRepository(key);
    setRepositorySaveError(null);
    try {
      await VersionsApi.saveStackRepository(
        stackName,
        stackVersion,
        row.osType,
        row.repository.Repositories.repo_id,
        repositoryUrl,
        verifyBaseUrl,
      );
      setFallbackRepositories((operatingSystems) => operatingSystems.map((os: any) => ({
        ...os,
        repositories: (os.repositories || []).map((repository: any) => (
          (os.OperatingSystems?.os_type || repository.Repositories?.os_type) === row.osType
            && repository.Repositories?.repo_id === row.repository.Repositories.repo_id
            ? {
                ...repository,
                Repositories: {
                  ...repository.Repositories,
                  base_url: repositoryUrl,
                },
              }
            : repository
        )),
      })));
      toast.success("Repository URL saved successfully");
      cancelFallbackRepositoryEdit();
    } catch (error: any) {
      setRepositorySaveError({
        key,
        message: error?.response?.data?.message || error?.message || "Repository URL validation failed",
      });
    } finally {
      setSavingRepository(null);
    }
  }

  if (loading) {
    return <Spinner />;
  }

  if (loadError) {
    return (
      <Alert variant="danger" className="mt-4 d-flex justify-content-between align-items-center">
        <span>{loadError}</span>
        <Button size="sm" variant="outline-danger" onClick={() => setLoadAttempt((attempt) => attempt + 1)}>
          Retry
        </Button>
      </Alert>
    );
  }

  const columns = [
    {
      header: () => <span className="fw-bold">Service</span>,
      accessorKey: "name",
      width: "10%",
    },
    {
      header: () => <span className="fw-bold">Version</span>,
      accessorKey: "version",
      width: "10%",
    },
    {
      header: () => <span className="fw-bold">Status</span>,
      accessorKey: "status",
      width: "10%",
      cell: (info: any) => {
        if (info.getValue() === "Installed") {
          return (
          <Badge bg="success">{info.getValue()}</Badge>
          );
        }
        const serviceName = serviceDetails.find(
          (service: any) => service.display_name === info.row.original.name
        )?.name || info.row.original.name;
        const canOpenAddService =
          canAddDeleteServices
          && canUpgradeDowngrade
          && supports.enableAddDeleteServices
          && !upgradeBlocksServiceChanges
          && !isNonWizardUser
          && (serviceName !== "KERBEROS" || canToggleKerberos);
        return (
          <Button 
            variant="link" 
            size="sm" 
            disabled={!canOpenAddService}
            onClick={() => {
              if (!canOpenAddService) {
                return;
              }
              localStorage.setItem("module06WizardReturnPath", "/main/admin/stack/services");
              if (serviceName === "KERBEROS") {
                navigate("/main/admin/kerberos/enable/step1");
                return;
              }
              localStorage.setItem("preselectedService", serviceName);
              navigate("/main/service/add/step1");
            }}
            title={!canOpenAddService ? "Adding this service is not available in the current permission, feature, or upgrade state." : ""}
          >
            {info.getValue()}
          </Button>
        );
      },
    },
    {
      header: () => <span className="fw-bold">Description</span>,
      accessorKey: "description",
      width: "70%",
    },
  ];

  return (
    <>
      <div className="mt-4">
        <h2>Stack</h2>
        <Table columns={columns} data={services}></Table>
      </div>
      {fallbackRepositories.length > 0 && (
        <div className="mt-4">
          <h3 className="fs-5">Repositories</h3>
          <Table
            columns={[
              {
                header: "OS",
                id: "os",
                cell: ({ row: { original } }: any) => original.osType,
              },
              {
                header: "Name",
                id: "name",
                cell: ({ row: { original } }: any) => original.repository.Repositories.repo_id,
              },
              {
                header: "Base URL",
                id: "baseUrl",
                cell: ({ row: { original } }: { row: { original: FallbackRepositoryRow } }) => {
                  const key = fallbackRepositoryKey(original);
                  const isEditing = editingRepository === key;
                  const isSaving = savingRepository === key;
                  if (!isEditing) {
                    return (
                      <div className="d-flex justify-content-between align-items-start gap-3">
                        <div>
                          <div>{original.repository.Repositories.base_url || "Not configured"}</div>
                          {original.repository.Repositories.mirrors_list && (
                            <small className="text-muted">Mirror list: {original.repository.Repositories.mirrors_list}</small>
                          )}
                        </div>
                        <Button
                          size="sm"
                          variant="link"
                          aria-label={`Edit ${original.repository.Repositories.repo_id}`}
                          title="Edit repository URL"
                          onClick={() => editFallbackRepository(original)}
                        >
                          <FontAwesomeIcon icon={faEdit} />
                        </Button>
                      </div>
                    );
                  }

                  return (
                    <div>
                      <InputGroup>
                        <Form.Control
                          value={repositoryUrl}
                          disabled={isSaving}
                          isInvalid={!repositoryUrl.trim()}
                          onChange={(event) => {
                            setRepositoryUrl(event.target.value);
                            setRepositorySaveError(null);
                          }}
                        />
                        <Button
                          variant="outline-secondary"
                          disabled={isSaving || !repositoryUrl}
                          aria-label={`Clear ${original.repository.Repositories.repo_id}`}
                          title="Clear repository URL"
                          onClick={() => setRepositoryUrl("")}
                        >
                          <FontAwesomeIcon icon={faTrash} />
                        </Button>
                      </InputGroup>
                      <div className="d-flex justify-content-end gap-2 mt-2">
                        <Button size="sm" variant="outline-secondary" disabled={isSaving} onClick={cancelFallbackRepositoryEdit}>
                          CANCEL
                        </Button>
                        <Button
                          size="sm"
                          variant="primary"
                          disabled={isSaving || !repositoryUrl.trim()}
                          onClick={() => void saveFallbackRepository(original, true)}
                        >
                          {isSaving ? "SAVING..." : "SAVE"}
                        </Button>
                      </div>
                      {repositorySaveError?.key === key && (
                        <Alert variant="warning" className="mt-2 mb-0">
                          <div>{repositorySaveError.message}</div>
                          <div className="d-flex flex-wrap gap-2 mt-2">
                            <Button size="sm" variant="warning" onClick={() => void saveFallbackRepository(original, false)}>
                              SAVE ANYWAY
                            </Button>
                            <Button size="sm" variant="outline-secondary" onClick={cancelFallbackRepositoryEdit}>
                              REVERT
                            </Button>
                            <Button size="sm" variant="outline-secondary" onClick={() => setRepositorySaveError(null)}>
                              CANCEL
                            </Button>
                          </div>
                        </Alert>
                      )}
                    </div>
                  );
                },
              },
            ]}
            data={fallbackRepositories.flatMap((os: any) =>
              (os.repositories || []).map((repository: any) => ({
                osType: os.OperatingSystems?.os_type || repository.Repositories?.os_type,
                repository,
              }))
            )}
          />
        </div>
      )}
    </>
  );
}
