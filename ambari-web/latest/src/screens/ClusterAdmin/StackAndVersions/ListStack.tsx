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
import { Badge, Button} from "react-bootstrap";
import { AppContext } from "../../../store/context";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../../hooks/useAuth";

interface Service {
  name: string;
  version: string;
  description: string;
  status: string;
}

export default function ListStack() {
  const [services, setServices] = useState<Service[]>([]);
  const [serviceDetails, setServiceDetails] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const { clusterName } = useContext(AppContext);
  const navigate = useNavigate();

  // Authorization hooks - implementing Ember.js service authorization patterns
  const { hasAuthorization } = useAuth();
  
  // Check specific authorizations for service operations
  const canAddDeleteServices = hasAuthorization('SERVICE.ADD_DELETE_SERVICES');

  useEffect(() => {
    const fetchServices = async () => {
      try {
        setLoading(true);
        const response = await VersionsApi.getServices(clusterName);
        
        const currentItem = response.items.find(
          (item: any) => item.ClusterStackVersions.state === "CURRENT"
        );
        
        if (!currentItem) {
          toast.error("No current stack version found");
          setLoading(false);
          return;
        }
        
        const serviceDetails =
          currentItem.repository_versions[0].RepositoryVersions
            .stack_services;
        const serviceSummary =
          currentItem.ClusterStackVersions.repository_summary.services;
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
        setLoading(false);
      } catch (err) {
        toast.error("Failed to fetch data");
        setLoading(false);
      }
    };

    fetchServices();
  }, []);

  if (loading) {
    return <Spinner />;
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
      cell: (info: any) =>
        info.getValue() === "Installed" ? (
          <Badge bg="success">{info.getValue()}</Badge>
        ) : (
          <Button 
            variant="link" 
            size="sm" 
            disabled={info.getValue() === "Installed" || !canAddDeleteServices} 
            onClick={() => {
              if (canAddDeleteServices) {
                // Get the actual service name from the service data
                const serviceName = serviceDetails.find(
                  (service: any) => service.display_name === info.row.original.name
                )?.name;
                
                // Store preselected service in localStorage temporarily
                localStorage.setItem('preselectedService', serviceName || info.row.original.name);
                
                // Navigate to Add Service wizard
                navigate("/main/service/add/step1");
              }
            }}
            title={!canAddDeleteServices ? "You do not have permission to add services. Required permission: SERVICE.ADD_DELETE_SERVICES" : ""}
          >
            {info.getValue()}
          </Button>
        ),
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
    </>
  );
}
