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
import { MouseEventHandler, useState } from "react";
import { Dropdown } from "react-bootstrap";
import SidebarItem from "./SidebarItem";

const SidebarItemCollapsed = ({
    ele,
    onClick,
    isSelected,
    childElements,
    selectedOption,
    setSelectedOption,
  }: {
    ele: any;
    onClick?: MouseEventHandler<HTMLDivElement>;
    isSelected: boolean;
    isOpen?: boolean;
    hasChildren?: boolean;
    childElements?: any[];
    selectedOption?: string;
    setSelectedOption?: any;
  }) => {
    const [showDropdown, setShowDropdown] = useState(false);
    if (childElements?.length) {
      return (
        <Dropdown
          drop="end"
          className="collapsed-sidebar-item"
          onMouseLeave={() => setShowDropdown(false)}
          onMouseOver={() => setShowDropdown(true)}
          // style={{ width: "166px" }}
        >
          <Dropdown.Toggle as="div" className="main-style" id="dropdown-basic">
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
              <div>{ele.icon}</div>
            </div>
          </Dropdown.Toggle>
          {showDropdown ? (
            <Dropdown.Menu
              show={showDropdown}
              style={{ width: "300px" }}
              className="bg-secondary d-flex flex-column justify-content-between rounded-0 py-0"
            >
              {childElements?.map((ele) => {
                return (
                  <SidebarItem
                    onClick={() => {
                      setSelectedOption(ele.id);
                    }}
                    ele={ele}
                    isSelected={selectedOption === ele.id}
                  />
                );
              })}
            </Dropdown.Menu>
          ) : null}
        </Dropdown>
      );
    } else {
      return (
        <div
          className={`d-flex justify-content-between ${
            ele.className
          } sideitem align-items-center ${isSelected ? "selected-item" : ""}`}
          style={{
            ...(ele.style as any),
            cursor: "pointer",
            padding: "10px 5px 10px 20px",
            position: "relative",
          }}
          onClick={() => {
            setSelectedOption(ele.id);
          }}
        >
          <div style={{ fontSize: 20 }}>{ele.icon}</div>
        </div>
      );
    }
  };

  export default SidebarItemCollapsed