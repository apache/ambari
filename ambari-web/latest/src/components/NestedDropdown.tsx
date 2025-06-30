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

import { Dropdown } from "react-bootstrap";
import React, { memo, ReactNode } from "react";
import { get } from "lodash";
import { getIcon } from "./icon";

type DropDirections = "up" | "down" | "start" | "end";

type Menu = {
  label: ReactNode;
  submenu?: Menu[];
  [key: string]: any;
};

type NestedDropdownProps = {
  menu: Menu;
  dropDirection?: DropDirections;
};

type SubmenuProps = {
  items?: Menu[];
  dropDirection?: DropDirections;
};

const Submenu = memo(({ items, dropDirection }: SubmenuProps) => {
  return (
    <Dropdown.Menu>
      {items &&
        items.map((item: any, index: number) => {
          return (
            <React.Fragment key={`menu-item-${item.label?.toString() || index}`}>
              {item.submenu && item.submenu.length && !get(item, "isDisabled", false) ? (
                <Dropdown drop={dropDirection}>
                  <Dropdown.Toggle className="custom-text-toggle">
                    {get(item, "icon", false)
                      ? getIcon(item.icon, get(item, "iconClass", ""))
                      : null}
                    <span>{item.label}</span>
                  </Dropdown.Toggle>
                  <Submenu items={item.submenu} dropDirection={dropDirection} />
                </Dropdown>
              ) : (
                get(item, "isVisible", true) && (
                  <Dropdown.Item
                    className={
                      get(item, "isDisabled", false) ? "disabled-btn" : ""
                    }
                    onClick={() => {
                      if (!get(item, "isDisabled", false)) {
                        item.onClick();
                      }
                    }}
                  >
                    {get(item, "icon", false)
                      ? getIcon(item.icon, get(item, "iconClass", ""))
                      : null}
                    {item.label}
                  </Dropdown.Item>
                )
              )}
            </React.Fragment>
          );
        })}
    </Dropdown.Menu>
  );
});

function NestedDropdown({ menu, dropDirection }: NestedDropdownProps) {
  dropDirection = dropDirection || "down";

  return (
    <Dropdown drop="down">
      <Dropdown.Toggle
        variant="primary"
        className="custom-btn text-white ps-3 pe-3"
      >
        <span className="me-2">{menu.label}</span>
      </Dropdown.Toggle>
      <Submenu items={menu.submenu} dropDirection={dropDirection} />
    </Dropdown>
  );
}

export default memo(NestedDropdown);
