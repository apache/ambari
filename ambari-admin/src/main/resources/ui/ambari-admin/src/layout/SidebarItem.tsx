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
import { faChevronDown, faChevronRight } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { MouseEventHandler } from "react";
import { Link } from "react-router-dom";

const SidebarItem = ({
    ele,
    onClick,
    isSelected,
    isOpen = false,
    hasChildren = false,
  }: {
    ele: any;
    onClick?: MouseEventHandler<HTMLDivElement>;
    isSelected: boolean;
    isOpen?: boolean;
    hasChildren?: boolean;
  }) => {
    return (
      <Link to={ele.path} className="text-decoration-none">
        <div
          className={`d-flex justify-content-between ${
            ele.className
          } sideitem align-items-center ${isSelected ? "selected-item" : ""}`}
          style={{
            ...(ele.style as any),
            fontSize: 14,
            cursor: "pointer",
            padding: "10px 5px 10px 20px",
            position: "relative",
          }}
          onClick={onClick}
        >
          <div className="d-flex">
            <div>{ele.icon}</div>
            <div className="ms-2">{ele.name}</div>
          </div>
          {hasChildren ? (
            <FontAwesomeIcon
              icon={isOpen ? faChevronDown : faChevronRight}
              className="me-1"
            />
          ) : null}
        </div>
      </Link>
    );
  };

  export default SidebarItem