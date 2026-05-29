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

import { useState } from "react";
import { Dropdown, Form, Badge, Button } from "react-bootstrap";
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faBell } from '@fortawesome/free-solid-svg-icons';
import { Link } from "react-router-dom";
import Table from "./Table";
import { Notifications } from "../screens/Alerts/types";
import { ColumnDef } from '@tanstack/react-table';

interface NotificationDropdownProps {
  notifications: Notifications[];
  onFilterChange?: (filter: string) => void;
  alertCounts: {
    all: number;
    critical: number;
    warning: number;
  };
}

const NotificationDropdown = ({ notifications, onFilterChange, alertCounts }: NotificationDropdownProps) => {
  const [showDropdown, setShowDropdown] = useState(false);
  const [selectedFilter, setSelectedFilter] = useState("all");

  const handleFilterChange = (value: string) => {
    setSelectedFilter(value);
    if (onFilterChange) {
      onFilterChange(value);
    }
  };

  const columns: ColumnDef<unknown, unknown>[] = [
    {
        id: 'status',
        header: "Status",
        cell: ({ row }) => {
            const typedRow = row.original as Notifications;
            return (
                <span
                    className={`status-${typedRow.Alert.state.toLowerCase()} d-inline-block rounded-circle`}
                    style={{ width: '10px', height: '10px' }}
                />
            );
        }
    },
    {
        id: 'content',
        header: "Content",
        cell: ({ row }) => {
            const typedRow = row.original as Notifications;
            return (
                <Link
                    to={`/main/alerts/${typedRow.Alert.definition_id}`}
                    style={{ textDecoration: 'none', color: 'inherit' }}
                >
                    <div>
                        <h4 className="name">{typedRow.Alert.label}</h4>
                        <p className="description text-truncate" style={{ fontSize: '14px' }}>
                            {typedRow.Alert.text}
                        </p>
                        <p className="timestamp text-end" style={{ fontSize: '10px' }}>
                            {new Date(typedRow.Alert.latest_timestamp).toLocaleString()}
                        </p>
                    </div>
                </Link>
            );
        }
    }
  ];

    return (
      <Dropdown
          show={showDropdown}
          onToggle={(isOpen) => setShowDropdown(isOpen)}
          className="position-relative"
          align="end"
      >
        <Dropdown.Toggle
            as="div"
            className="cursor-pointer d-flex align-items-center navbar-item"
            style={{ background: 'none', border: 'none' }}
        >
          <FontAwesomeIcon icon={faBell} className="navbar-text navbar-size" />
          <Badge bg="danger" className="position-absolute top-0 start-50 translate-middle rounded-circle">
            {alertCounts.all}
          </Badge>
        </Dropdown.Toggle>

        <Dropdown.Menu className="dropdown-menu p-0" style={{ width: '400px', marginTop: '10px' }}>
          <div className="notifications-header row p-2 m-0 pb-2">
            <div className="notifications-title fw-bold col d-flex align-items-center">
              Notifications ({alertCounts.all})
            </div>
            <div className="col d-flex align-items-center justify-content-end">
              <span>Show:&nbsp;</span>
              <Form.Select
                  size="sm"
                  className="ms-1"
                  value={selectedFilter}
                  onChange={(e) => handleFilterChange(e.target.value)}
                  style={{ width: 'auto' }}
              >
                <option value="all">All ({alertCounts.all})</option>
                <option value="critical">Critical ({alertCounts.critical})</option>
                <option value="warning">Warning ({alertCounts.warning})</option>
              </Form.Select>
            </div>
          </div>

          <hr className="m-0" />

          <div className="notifications-body p-2" style={{ maxHeight: '400px', overflowY: 'auto' }}>
            <Table
                columns={columns}
                data={notifications}
                className="alerts-table table table-hover"
                showHeader={false}
            />
          </div>

          <hr className="m-0" />

          <div className="notifications-footer p-2 text-end">
            <Link to="/main/alerts">
              <Button type="button" className="btn btn-primary">
                View All
              </Button>
            </Link>
          </div>
        </Dropdown.Menu>
      </Dropdown>
  );
};

export default NotificationDropdown;