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

import { Card, CardBody, Dropdown } from "react-bootstrap";
import { ReactNode } from "react";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faEllipsisV,
  faPencilAlt,
  faClone,
  faTrash,
} from "@fortawesome/free-solid-svg-icons";

interface ChartContainerProps {
  children: ReactNode;
  widgetHeader: string;
  onEdit?: () => void;
  onClone?: () => void;
  onDelete?: () => void;
  onViewDetails?: () => void;
  onShare?: () => void;
}

function WidgetContainer({
  children,
  widgetHeader,
  onEdit,
  onClone,
  onDelete,
  onViewDetails,
  onShare,
}: ChartContainerProps) {
  const handleEdit = () => {
    if (onEdit) onEdit();
  };

  const handleClone = () => {
    if (onClone) onClone();
  };

  const handleDelete = () => {
    if (onDelete) onDelete();
  };

  const handleViewDetails = () => {
    if (onViewDetails) onViewDetails();
  };

  const handleShare = () => {
    if (onShare) onShare();
  };

  return (
    <Card className="widget-card mh-100">
      <CardBody>
        <div className="d-flex align-items-center justify-content-between">
          <h4>{widgetHeader}</h4>
          <Dropdown>
            <Dropdown.Toggle
              as="div"
              id="widget-dropdown"
              className="cursor-pointer dropdown-no-arrow"
              variant="link"
            >
              <FontAwesomeIcon icon={faEllipsisV} />
            </Dropdown.Toggle>
            <Dropdown.Menu>
              {onEdit && (
                <Dropdown.Item onClick={handleEdit}>
                  <FontAwesomeIcon icon={faPencilAlt} className="me-2" /> Edit
                </Dropdown.Item>
              )}
              {onClone && (
                <Dropdown.Item onClick={handleClone}>
                  <FontAwesomeIcon icon={faClone} className="me-2" /> Clone
                </Dropdown.Item>
              )}
              {onDelete && (
                <Dropdown.Item onClick={handleDelete} className="text-danger">
                  <FontAwesomeIcon icon={faTrash} className="me-2" /> Delete
                </Dropdown.Item>
              )}
              {onViewDetails && (
                <Dropdown.Item onClick={handleViewDetails}>
                  <FontAwesomeIcon icon={faEllipsisV} className="me-2" /> View
                  Details
                </Dropdown.Item>
              )}
              {onShare && (
                <Dropdown.Item onClick={handleShare}>
                  <FontAwesomeIcon icon={faEllipsisV} className="me-2" /> Share
                </Dropdown.Item>
              )}
            </Dropdown.Menu>
          </Dropdown>
        </div>
        <div>{children}</div>
      </CardBody>
    </Card>
  );
}

export default WidgetContainer;
