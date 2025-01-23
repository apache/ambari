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
/* eslint-disable @typescript-eslint/no-explicit-any */
import { Collapse } from "react-bootstrap";
import SideItem from "./SideItem";
import { useContext, useEffect, useState } from "react";
import { SideItemLabels } from "./SideItemList";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faAngleDoubleLeft,
  faAngleDoubleRight,
} from "@fortawesome/free-solid-svg-icons";
import AppContent from "../context/AppContext";
import SidebarItem from "./SidebarItem"
import SidebarItemCollapsed from "../layout/SidebarItemCollapsed";
import RoutesList from "../router/RoutesList";
import getSideItemList from "./SideItemList";

type SideBarProps = {
  isRoot?: boolean;
  isSidebarCollapsed:boolean;
  setIsSidebarCollapsed: React.Dispatch<React.SetStateAction<boolean>>;
  clusterExists?: boolean;
};

const SideBar = ({ clusterExists, isSidebarCollapsed,setIsSidebarCollapsed }: SideBarProps) => {
  const SideItemList: SideItem[] = getSideItemList(clusterExists ?? false);
  const [openOptions, setOpenOptions] = useState<string[]>([
    SideItemLabels.CLUSTERMANAGEMENT,
  ]);
  const { selectedOption, setSelectedOption } = useContext(AppContent);
  const isElementOpen = (id: string) => {
    return openOptions.includes(id);
  };

  useEffect(() => {
    const currentHash = window.location.hash;
    const currentPath = currentHash.replace("#", "");
    const matchedRoute = RoutesList.find(
      (route: any) => route.path === currentPath
    );
    if (matchedRoute) {
      setSelectedOption(matchedRoute.name);
    }
  }, []);

  const handleSideItemClick = (itemId: string) => {
    if (isElementOpen(itemId)) {
      setOpenOptions(openOptions.filter((opt) => opt !== itemId));
    } else {
      setOpenOptions([...openOptions, itemId]);
    }
  };
  if (!isSidebarCollapsed) {
    return (
      <div
        className="bg-secondary h-100 d-flex flex-column justify-content-between"
        style={{ width: 230, position: "fixed", zIndex: 10 }}
      >
        <div>
          {SideItemList.map((ele) => {
            if (ele.children.length) {
              return (
                <>
                  <SidebarItem
                    isSelected={selectedOption === ele.id}
                    ele={ele}
                    isOpen={isElementOpen(ele.id)}
                    hasChildren={ele.children.length > 0}
                    onClick={() => {
                      handleSideItemClick(ele.id);
                    }}
                  />
                  <Collapse in={openOptions.includes(ele.id)}>
                    <div>
                      {ele.children.map((child) => {
                        return (
                          <SidebarItem
                            isSelected={selectedOption === child.id}
                            ele={child}
                            key={child.id}
                            onClick={() => {
                              setSelectedOption(child.id);
                            }}
                          />
                        );
                      })}
                    </div>
                  </Collapse>
                </>
              );
            } else {
              return (
                <SidebarItem
                  isSelected={selectedOption === ele.id}
                  onClick={() => {
                    setSelectedOption(ele.id);
                  }}
                  ele={ele}
                />
              );
            }
          })}
        </div>
        <div
          className="py-3 d-flex justify-content-center sidebar-collapse icon-primary"
          style={{ background: "#313d54" }}
          onClick={() => {
            setIsSidebarCollapsed(!isSidebarCollapsed);
          }}
        >
          <FontAwesomeIcon icon={faAngleDoubleLeft} />
        </div>
      </div>
    );
  } else {
    return (
      <div
        className="bg-secondary h-100 d-flex flex-column justify-content-between"
        style={{ width: 60 }}
      >
        <div>
          {SideItemList.map((ele) => {
            return (
              <SidebarItemCollapsed
                key={ele.id}
                isSelected={selectedOption === ele.id}
                ele={ele}
                isOpen={isElementOpen(ele.id)}
                childElements={ele.children}
                setSelectedOption={setSelectedOption}
                selectedOption={selectedOption}
              />
            );
          })}
        </div>
        <div
          className="py-3 d-flex justify-content-center sidebar-collapse icon-primary"
          style={{ background: "#313d54" }}
          onClick={() => {
            setIsSidebarCollapsed(!isSidebarCollapsed);
          }}
        >
          <FontAwesomeIcon icon={faAngleDoubleRight} />
        </div>
      </div>
    );
  }
};

export default SideBar;
