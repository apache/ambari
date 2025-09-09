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
import toast from "react-hot-toast";
import Spinner from "../../../components/Spinner";
import Table from "../../../components/Table";
import { Badge, Button } from "react-bootstrap";
import { AppContext } from "../../../store/context";
import VersionsApi from "../../../api/VersionsApi";

interface Service {
  name: string;
  version: string;
  description: string;
  status: string;
}

export default function ListStack() {
  const [services, setServices] = useState<Service[]>([]);
  const [loading, setLoading] = useState(false);
  const { clusterName } = useContext(AppContext);

  useEffect(() => {
    const fetchServices = async () => {
      try {
        setLoading(true);
        const response = await VersionsApi.getServices(clusterName);
        const serviceDetails =
          response.items[0].repository_versions[0].RepositoryVersions
            .stack_services;
        const serviceSummary =
          response.items[0].ClusterStackVersions.repository_summary.services;
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
            disabled={info.getValue() === "Installed"}
            onClick={() => {
              //TODO: will be added once we have add service flow
              // navigate("/main/service/add/step1");
            }}
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
