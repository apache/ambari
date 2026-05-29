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

import { useState, useEffect, useContext, useMemo } from 'react';
import { useParams } from 'react-router-dom';
import { Container, Form, Row, Col, Button, Dropdown } from 'react-bootstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPowerOff } from '@fortawesome/free-solid-svg-icons';
import { AppContext } from '../../store/context';
import VersionsApi from '../../api/versionsApi';
import Spinner from '../../components/Spinner';
import Table from '../../components/Table';
import { ColumnDef } from '@tanstack/react-table';

interface RepositoryVersion {
    RepositoryVersions: {
        display_name: string;
    };
}

interface StackVersion {
    ClusterStackVersions: {
        stack: string;
        state: string;
    };
    repository_versions: RepositoryVersion[];
}

interface SortingState {
    id: string;
    desc: boolean;
}

const HostStackVersions = () => {
    const { hostName } = useParams<{ hostName: string }>();
    const { clusterName } = useContext(AppContext);
    const [stackVersions, setStackVersions] = useState<StackVersion[]>([]);
    const [filteredStackVersions, setFilteredStackVersions] = useState<StackVersion[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [stackFilter, setStackFilter] = useState('');
    const [nameFilter, setNameFilter] = useState('');
    const [statusFilter, setStatusFilter] = useState('');
    const [sorting, setSorting] = useState<SortingState[]>([]);

    const uniqueStacks = useMemo(() => [...new Set(stackVersions.map(item => item.ClusterStackVersions.stack))], [stackVersions]);
    const uniqueNames = useMemo(() => [...new Set(stackVersions.map(item => item.repository_versions[0].RepositoryVersions.display_name))], [stackVersions]);
    const uniqueStatuses = useMemo(() => ['Installed', 'Installing', 'Install Failed', 'Out of Sync', 'Current', 'Upgrading', 'Upgrade Failed'].map(status => status.toUpperCase()), []);
    const fetchData = async () => {
        try {
            const response = await VersionsApi.getServices(clusterName);
            const filteredItems = response.items.filter((item: StackVersion) => {
                return item.ClusterStackVersions.state !== 'NOT_REQUIRED';
            });
            setStackVersions(filteredItems);
            setFilteredStackVersions(filteredItems);
        } catch (error) {
            console.error('Error fetching data:', error);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, [hostName, clusterName]);

    useEffect(() => {
        const filteredData = stackVersions.filter(item => {
            const matchesStack = stackFilter === '' || item.ClusterStackVersions.stack === stackFilter;
            const matchesName = nameFilter === '' || item.repository_versions[0].RepositoryVersions.display_name === nameFilter;
            const matchesStatus = statusFilter === '' || item.ClusterStackVersions.state === statusFilter;
            return matchesStack && matchesName && matchesStatus;
        });
        setFilteredStackVersions(filteredData);
    }, [stackFilter, nameFilter, statusFilter, stackVersions]);

    const columns: any[] = [
        {
            header: 'Stack',
            accessorKey: 'ClusterStackVersions.stack',
            width: "25%",
            cell: ({ row }: any) => row.original.ClusterStackVersions.stack,
        },
        {
            header: 'Name',
            accessorKey: 'repository_versions.0.RepositoryVersions.display_name',
            width: "25%",
            cell: ({ row }: any) => row.original.repository_versions[0].RepositoryVersions.display_name,
        },
        {
            header: 'Status',
            accessorKey: 'ClusterStackVersions.state',
            width: "25%",
            cell: ({ row }: any) => {
                const stackVersion = row.original;
                return stackVersion.ClusterStackVersions.state === 'CURRENT' ? (
                    <Button variant="success" disabled>
                        {stackVersion.ClusterStackVersions.state}
                    </Button>
                ) : (
                    stackVersion.ClusterStackVersions.state
                );
            },
        },
        {
            header: '',
            accessorKey: 'install',
            cell: ({ }) => (
                <Button variant="secondary" disabled>
                    <FontAwesomeIcon className={'mx-1'} icon={faPowerOff} />
                    Install
                </Button>
            ),
        },
    ];

    return (
      <div className="mx-5">
        <Container className="p-4 bg-white">
          <h2 className="table-title pb-2">Versions</h2>
          {isLoading ? (
            <Spinner />
          ) : (
            <>
              <Form className="filter-container mb-3">
                <Row className="border-top border-bottom py-2 mx-1">
                  <Col xs={3}>
                    <Form.Group controlId="stackFilter">
                      <Dropdown>
                        <Dropdown.Toggle variant="default" id="dropdown-basic">
                          {stackFilter || "All"}
                        </Dropdown.Toggle>
                        <Dropdown.Menu>
                          <Dropdown.Item onClick={() => setStackFilter("")}>
                            All
                          </Dropdown.Item>
                          {uniqueStacks.map((stack, index) => (
                            <Dropdown.Item
                              key={index}
                              onClick={() => setStackFilter(stack)}
                            >
                              {stack}
                            </Dropdown.Item>
                          ))}
                        </Dropdown.Menu>
                      </Dropdown>
                    </Form.Group>
                  </Col>
                  <Col xs={3}>
                    <Form.Group controlId="nameFilter">
                      <Dropdown>
                        <Dropdown.Toggle variant="default" id="dropdown-basic">
                          {nameFilter || "All"}
                        </Dropdown.Toggle>
                        <Dropdown.Menu>
                          <Dropdown.Item onClick={() => setNameFilter("")}>
                            All
                          </Dropdown.Item>
                          {uniqueNames.map((name, index) => (
                            <Dropdown.Item
                              key={index}
                              onClick={() => setNameFilter(name)}
                            >
                              {name}
                            </Dropdown.Item>
                          ))}
                        </Dropdown.Menu>
                      </Dropdown>
                    </Form.Group>
                  </Col>
                  <Col xs={3}>
                    <Form.Group controlId="statusFilter">
                      <Dropdown>
                        <Dropdown.Toggle variant="default" id="dropdown-basic">
                          {statusFilter || "All"}
                        </Dropdown.Toggle>
                        <Dropdown.Menu>
                          <Dropdown.Item onClick={() => setStatusFilter("")}>
                            All
                          </Dropdown.Item>
                          {uniqueStatuses.map((status, index) => (
                            <Dropdown.Item
                              key={index}
                              onClick={() => setStatusFilter(status)}
                            >
                              {status}
                            </Dropdown.Item>
                          ))}
                        </Dropdown.Menu>
                      </Dropdown>
                    </Form.Group>
                  </Col>
                </Row>
              </Form>
              <Table
                columns={columns as ColumnDef<unknown, unknown>[]}
                data={filteredStackVersions}
                hover
                entityName="stack versions"
                sorting={sorting}
                onSortingChange={setSorting}
              />
            </>
          )}
        </Container>
      </div>
    );
};

export default HostStackVersions;