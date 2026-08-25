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
import React, { useEffect, useState } from "react";
import SidebarItem from "./SidebarItem";
import SideItem from "./SideItem.tsx";
import SidebarItemCollapsed from "./SidebarItemCollapsed";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faAngleDoubleLeft,
  faAngleDoubleRight,
  faBriefcase,
  faSync
} from "@fortawesome/free-solid-svg-icons";
import { SideItemLabels, getSideItemList } from "./SideItemList";
import { AppContext } from "../../store/context.tsx";
import { useContext } from "react";
import useAuthorizationPolicy from "../../hooks/useAuthorizationPolicy";
import { Collapse } from "react-bootstrap";
import { ServiceContext } from "../../store/ServiceContext.tsx";
import {
  serviceNameModelMapping,
  serviceNameDisplayMapping,
} from "../../constants.ts";
import { displayOrder } from "../../screens/ClusterWizard/constants";
import { isEmpty } from "lodash";
import { useLocation } from "react-router-dom";
import { getClusterStackName } from "../../Utils/stackMetadata";
type SideBarProps = {
  isRoot?: boolean;
  isSidebarCollapsed: boolean;
  setIsSidebarCollapsed: React.Dispatch<React.SetStateAction<boolean>>;
  clusterExists?: boolean;
};

const SideBar = ({
  isSidebarCollapsed,
  setIsSidebarCollapsed,
}: SideBarProps) => {
  const {
    clusterName,
    cluster,
    services: contextServices,
    supports,
  } = useContext(AppContext);
  const [openOptions, setOpenOptions] = useState<string[]>([SideItemLabels.SERVICES]);
  const [selectedOption, setSelectedOption] = useState<string>("");
  const { allServiceModels  } = useContext(ServiceContext);
  const location = useLocation();
  
  // Authorization hooks - implementing Ember.js menu authorization patterns
  const { havePermissions, isAuthorized } = useAuthorizationPolicy();
  
  // Get authorization-aware sidebar items using computed upgrade properties
  const authorizedSideItemList = getSideItemList(
    havePermissions,
    isAuthorized,
    supports,
    getClusterStackName(cluster),
  );
  const [services, setServices] = useState<
    {
      name: string;
      state: string;
      alertsCountDisplay?: string;
      noAlerts?: boolean;
      hasCriticalAlerts?: boolean;
      isClientOnlyService?: boolean;
      isInPassiveForService?: boolean;
      isRestartRequiredForService?: boolean;
    }[]
  >([]);

  const isElementOpen = (id: string) => {
    return openOptions.includes(id);
  };

  const handleSideItemClick = (itemId: string) => {
    if (isElementOpen(itemId)) {
      setOpenOptions(openOptions.filter((opt) => opt !== itemId));
    } else {
      setOpenOptions([...openOptions, itemId]);
    }
  };

  useEffect(() => {
    if (!clusterName || isEmpty(allServiceModels)) {
      setServices([]);
      return;
    }

    const processServices = () => {
      
      const processedServices = contextServices
        ?.map((service: any) => {
          const serviceName = service?.ServiceInfo?.service_name;
        // Type assertion to tell TypeScript this is a valid key
          const modelKey = serviceName && serviceNameModelMapping[serviceName as keyof typeof serviceNameModelMapping];
          const currentServiceModel = allServiceModels[modelKey];

          // Skip if model doesn't exist
          if (!currentServiceModel) {
            console.warn(
              `No model found for service ${serviceName} with key ${modelKey}`
            );
            return null;
          }

          // Create the service object with proper state handling
          const serviceData = {
            name:
              serviceNameDisplayMapping[
                serviceName as keyof typeof serviceNameDisplayMapping
              ] || serviceName,
            serviceName: serviceName, // Keep original service name for sorting
            state:
              currentServiceModel["serviceState"] &&
              currentServiceModel["serviceState"] !== ""
                ? currentServiceModel["serviceState"]
                : "UNKNOWN",
            alertsCountDisplay:
              currentServiceModel["alertsCount"] > 0
                ? currentServiceModel["alertsCount"]
                : undefined,
            noAlerts: currentServiceModel["alertsCount"] === 0,
            hasCriticalAlerts:
              currentServiceModel["hasCriticalAlerts"] || false,
            isClientOnlyService: currentServiceModel["isClientOnlyService"] || false,
            isInPassiveForService: currentServiceModel["isInPassiveForService"] || false,
            isRestartRequiredForService: currentServiceModel["isRestartRequiredForService"] || false
          };

          return serviceData;
        })
        .filter(Boolean)
        // Sort services according to Ember.js displayOrder (matching App.StackService.displayOrder)
        .sort((a: any, b: any) => {
          const aIndex = displayOrder.indexOf(a.serviceName);
          const bIndex = displayOrder.indexOf(b.serviceName);
          
          // If service is in displayOrder, use its index; otherwise, put it at the end
          const aOrder = aIndex !== -1 ? aIndex : displayOrder.length;
          const bOrder = bIndex !== -1 ? bIndex : displayOrder.length;
          
          return aOrder - bOrder;
        }) as {
          name: string;
          serviceName: string;
          state: string;
          alertsCountDisplay?: string;
          noAlerts?: boolean;
          hasCriticalAlerts?: boolean;
          isClientOnlyService?: boolean;
          isInPassiveForService?: boolean;
          isRestartRequiredForService?: boolean;
        }[];; // Filter out any null values

      setServices(processedServices);
    };

    processServices();
  }, [JSON.stringify(allServiceModels), clusterName, contextServices]);

  const getStateColor = (
    state: string,
    isClientOnlyService: boolean = false,
  ): string | undefined => {
    if (isClientOnlyService) {
      return undefined; // Return undefined for client-only services to make the icon transparent
    }
    switch (state) {
          case "STARTED":
            return "#1eb475";
          case "STOPPED" :
          case "INSTALLED":
            return "#ef6162";
          default:
            return "gray";
    }   
  };

  const updatedSideItemList = authorizedSideItemList.map((item: SideItem) => {
    if (item.id === SideItemLabels.SERVICES) {
      return {
        ...item,
        children: services.map((service) => ({
          id: service.name,
          path: `/main/services/${service?.name?.replace(" ","_")?.toUpperCase()}/summary`,
          name: (
            <div className="d-flex align-items-center w-100 pe-3">
                <div
                  className="rounded-circle me-2"
                  style={{
                    width: "8px",
                    height: "8px",
                    backgroundColor: getStateColor(
                      service.state,
                      service.isClientOnlyService ?? false,
                    ),
                  }}
                ></div>
              {/* Service name with conditional color based on state */}
              <div className="flex-grow-1 text-truncate me-2" style={{ 
                color: service.isClientOnlyService ? "inherit" : (service.state === "STOPPED" || service.state === "INSTALLED") ? "#ef6162" : "inherit",marginRight: "10px"}}>{service.name}</div>
              {/* Maintenance icon (suitcase) - only show if service is in passive state */}
              {service.isInPassiveForService === true && (
                <FontAwesomeIcon icon={faBriefcase} className="me-2 " style={{ color: "#adb5bd" }} />
              )}

              {/* Refresh icon - only show if refresh is needed */}
              {service.isRestartRequiredForService === true && (
                <FontAwesomeIcon icon={faSync} className="me-2" style={{ color: "#adb5bd" }} />
              )}
              
              {service.alertsCountDisplay &&
                Number(service.alertsCountDisplay) > 0 && (
                  <div className="ms-auto">
                    <div
                      className={`badge ${service.noAlerts ? "d-none" : ""} ${
                        service.hasCriticalAlerts
                          ? "bg-danger"
                          : "bg-warning"
                      } rounded-circle d-flex align-items-center justify-content-center`}
                      style={{ width: "20px", height: "20px", fontSize: "10px" }}
                    >
                      {service.alertsCountDisplay}
                    </div>
                  </div>
                )}
            </div>
          ),
        })),
      };
    }
    return item;
  });

  useEffect(() => {
    const currentPath = location.pathname;
    
    const findMatchingItem = (items: any[]): string | null => {
      let bestMatch: { id: string; pathLength: number } | null = null;
      
      for (const item of items) {
        if (item.id === SideItemLabels.LOGO) {
          continue;
        }
        
        // Check if current path matches this item's path
        if (item.path && currentPath.startsWith(item.path)) {
          // Keep track of the best (longest) match
          if (!bestMatch || item.path.length > bestMatch.pathLength) {
            bestMatch = { id: item.id, pathLength: item.path.length };
          }
        }
        
        // Check children if they exist
        if (item.children && item.children.length > 0) {
          const childMatch = findMatchingItem(item.children);
          if (childMatch) {
            // If a child matches, also ensure parent is open
            if (!openOptions.includes(item.id)) {
              setOpenOptions(prev => [...prev, item.id]);
            }
            return childMatch;
          }
        }
      }
      
      return bestMatch ? bestMatch.id : null;
    };

    // Find matching item in the updated sidebar list
    const matchingItemId = findMatchingItem(updatedSideItemList);
    
    if (matchingItemId && matchingItemId !== selectedOption) {
      setSelectedOption(matchingItemId);
    }
    
    // Special handling for admin routes - ensure Cluster Admin is open when on admin pages
    if (currentPath.startsWith('/main/admin') && !openOptions.includes(SideItemLabels.CLUSTER_ADMIN)) {
      setOpenOptions(prev => [...prev, SideItemLabels.CLUSTER_ADMIN]);
    }
  }, [location.pathname, updatedSideItemList, selectedOption, openOptions]);

  if (!isSidebarCollapsed) {
    return (
      <div
        className="bg-secondary h-100 d-flex flex-column justify-content-between overflow-scroll no-scrollbar"
        style={{ width: 230, position: "fixed", zIndex: 10 }}
      >
        <div>
          {updatedSideItemList.map((ele) => {
            if (ele.children.length) {
              return (
                <div key={ele.id}>
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
                      {ele.children.map((child: any) => {
                        if (child.children && child.children.length > 0) {
                          return (
                            <div key={child.id}>
                              <SidebarItem
                                isSelected={selectedOption === child.id}
                                ele={child}
                                isOpen={isElementOpen(child.id)}
                                hasChildren={child.children.length > 0}
                                onClick={() => {
                                  handleSideItemClick(child.id);
                                }}
                              />
                              <Collapse in={openOptions.includes(child.id)}>
                                <div>
                                  {child.children.map((grandChild: any) => {
                                    return (
                                      <SidebarItem
                                        key={grandChild.id}
                                        isSelected={selectedOption === grandChild.id}
                                        ele={grandChild}
                                        onClick={() => {
                                          setSelectedOption(grandChild.id);
                                        }}
                                      />
                                    );
                                  })}
                                </div>
                              </Collapse>
                            </div>
                          );
                        } else {
                          return (
                            <SidebarItem
                              key={child.id}
                              isSelected={selectedOption === child.id}
                              ele={child}
                              onClick={() => {
                                setSelectedOption(child.id);
                              }}
                            />
                          );
                        }
                      })}
                    </div>
                  </Collapse>
                </div>
              );
            } else {
              return (
                <SidebarItem
                  key={ele.id}
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
          className="py-3 d-flex justify-content-center text-primary"
          style={{ background: "#313d54", cursor: "pointer" }}
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
          {authorizedSideItemList.map((ele) => {
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
          className="py-3 d-flex justify-content-center text-primary"
          style={{ background: "#313d54", cursor: "pointer" }}
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
