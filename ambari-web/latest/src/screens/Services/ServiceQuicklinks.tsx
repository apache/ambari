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

import { useContext, useEffect, useRef, useState } from "react";
import { ServiceContext } from "../../store/ServiceContext";
import ConfigsApi from "../../api/configsApi";
import { AppContext } from "../../store/context";
import Spinner from "../../components/Spinner";

function ServiceQuicklinks({
  serviceName,
  selectedTab,
}: {
  serviceName: string;
  selectedTab: string;
}) {
  const { clusterName } = useContext(AppContext);
  const { allServiceModels } = useContext(ServiceContext);
  const [overridenQuicklinks, setOverridenQuicklinks] = useState<any>([]);
  const [isServiceQuicklinksLoading, setIsServiceQuicklinksLoading] =
    useState(false);
  const [configData, setConfigData] = useState<any>({});
  const isSSLEnabledRef = useRef<boolean>(false);
  const [prevConfData, setPrevConfData] = useState<any>({});
  const [prevOverridenQuicklinks, setPrevOverridenQuicklinks] = useState<any>(
    []
  );

  const hdfsServiceName = "HDFS";
  const hdfsSiteFileName = "hdfs-site";
  const hdfsHTTPProperty = "dfs.http.policy";

  const stringifiedModel = JSON.stringify(
    allServiceModels?.[serviceName.toLowerCase()] || {}
  );

  //call this on mount
  const getUpdatedConfigValues = async (currentServiceName: string = serviceName) => {
    // if(isEmpty(updatedConfigs)||!updatedConfigs){
    const responseData = await ConfigsApi.getConfigValues(
      clusterName,
      currentServiceName
    );
    
    // Check if service has changed during async operation
    if (currentServiceName !== serviceName) {
      return; // Abort if service has changed
    }
    
    if (JSON.stringify(prevConfData) !== JSON.stringify(responseData)) {
      setConfigData(responseData);
      setPrevConfData(responseData);
    }
    // updateQuickLinks();
    return responseData;
  };
  const checkIfSSLIsEnabled = async () => {
    const responseData = await ConfigsApi.getConfigValues(
      clusterName,
      hdfsServiceName
    );
    const indexFoSSLCheck = responseData?.items.length - 1;
    const hadoopSSLEnabled = responseData?.items[
      indexFoSSLCheck
    ]?.configurations.find((conf: any) => conf.type === hdfsSiteFileName)
      ?.properties[hdfsHTTPProperty];
    if (hadoopSSLEnabled === "HTTPS_ONLY") {
      isSSLEnabledRef.current = true;
    }
    return hadoopSSLEnabled === "HTTPS_ONLY" ? true : false;
  };

  const memoizedGetHostName = (() => {
    const cache = new Map();
    return (url: string) => {
      if (cache.has(url)) {
        return cache.get(url);
      }
      try {
        const hostname = new URL(url).hostname;
        cache.set(url, hostname);
        return hostname;
      } catch (error) {
        return "";
      }
    };
  })();

  const reconstructURL = (link: any, hostAndPort: any) => {
    // Need to add logic for http or https based on config
    const isSSLEnabled = isSSLEnabledRef.current;
    const protocol = isSSLEnabled ? "https:" : "http:";

    try {
      const url = new URL(link.url);

      // Check if hostAndPort contains both host and port
      if (typeof hostAndPort === "string" && hostAndPort.includes(":")) {
        const [host, port] = hostAndPort.split(":");

        url.hostname = host;
        url.port = port;
      } else if (
        typeof hostAndPort === "string" &&
        !hostAndPort.includes(":")
      ) {
        if (/^\d+$/.test(hostAndPort)) {
          url.port = hostAndPort;
        } else {
          url.hostname = hostAndPort;
        }
      } else {
        console.log("Invalid hostAndPort value:", hostAndPort);
      }

      // Update the protocol (remove the trailing colon that URL.protocol includes)
      url.protocol = protocol;

      // Return the reconstructed URL
      return url.toString();
    } catch (error) {
      console.error("Error reconstructing URL:", error);
      return link.url; // Return original URL if parsing fails
    }
  };

  const transformQuicklinksToHostStructure = (flatLinks: any[]) => {
    // Early return for empty arrays
    if (!flatLinks || flatLinks.length === 0) {
      return [];
    }

    // Create a map to group links by hostname - use a Map instead of object for better performance
    const linksByHost = new Map<string, any[]>();

    // Single pass through the array
    for (let i = 0; i < flatLinks.length; i++) {
      const link = flatLinks[i];
      const hostName = link.hostName;

      // Skip invalid entries
      if (!hostName) continue;

      // Get or initialize the array for this hostname
      if (!linksByHost.has(hostName)) {
        linksByHost.set(hostName, []);
      }

      // Add link to the appropriate host group (no spread operator)
      linksByHost.get(hostName)!.push(link);
    }

    // Convert the map to the desired array structure in a single operation
    const result = Array.from(linksByHost.entries()).map(
      ([hostName, links]) => ({
        hostName,
        links,
      })
    );

    return result;
  };

  /**
   * Helper function to handle HA-specific property patterns
   * @param serviceName - The service name (e.g., 'HDFS')
   * @param serviceModel - The service model object
   * @param quicklink - The quicklink object to process
   * @returns Array of processed quicklinks with proper HA property patterns
   */
  const getHAPropertyPatterns = (
    serviceName: string,
    serviceModel: any,
    quicklink: any
  ) => {
    // Default to the original property
    const propertyToUpdate = quicklink.propertyToUpdate;

    let haProperties = [] as any[];

    // Handle HDFS HA mode
    if (
      serviceName.toLowerCase() === "hdfs" &&
      serviceModel.isNameNodeHaEnabled
    ) {
      // Get namespaces from the HDFS model
      const namespaces = serviceModel.namespaces || [];

      // For each namespace, create patterns for all NameNodes
      namespaces.forEach((namespace: any) => {
        const namespaceName = namespace.nameSpace;
        const hostNames = namespace.hostNames || [];

        // For each host in the namespace, create a property pattern
        hostNames.forEach((index: number) => {
          // NN identifiers start from 1 (nn1, nn2, etc.)
          const nnId = index + 1;

          // Create property patterns for this namespace and NN
          if (propertyToUpdate.includes("dfs.namenode.http-address")) {
            haProperties.push({
              ...quicklink,
              propertyToUpdate: `dfs.namenode.http-address.${namespaceName}.nn${nnId}`,
              haState: index === 0 ? "Active" : "Standby",
            });
          } else if (propertyToUpdate.includes("dfs.namenode.https-address")) {
            haProperties.push({
              ...quicklink,
              propertyToUpdate: `dfs.namenode.https-address.${namespaceName}.nn${nnId}`,
              haState: index === 0 ? "Active" : "Standby",
            });
          }
        });
      });
    }

    // Handle YARN HA mode
    // else if (
    //   serviceName.toLowerCase() === "yarn" &&
    // ) {
    //   // For YARN, the pattern is typically yarn.resourcemanager.webapp.address.rm1
    //   if (propertyToUpdate === "yarn.resourcemanager.webapp.address") {
    //     haProperties.push({
    //       ...quicklink,
    //       propertyToUpdate: "yarn.resourcemanager.webapp.address.rm1",
    //       haState: "Active",
    //     });
    //     haProperties.push({
    //       ...quicklink,
    //       propertyToUpdate: "yarn.resourcemanager.webapp.address.rm2",
    //       haState: "Standby",
    //     });
    //   } else if (
    //     propertyToUpdate === "yarn.resourcemanager.webapp.https.address"
    //   ) {
    //     haProperties.push({
    //       ...quicklink,
    //       propertyToUpdate: "yarn.resourcemanager.webapp.https.address.rm1",
    //       haState: "Active",
    //     });
    //     haProperties.push({
    //       ...quicklink,
    //       propertyToUpdate: "yarn.resourcemanager.webapp.https.address.rm2",
    //       haState: "Standby",
    //     });
    //   }
    // }

    // Return HA properties if found, otherwise return the original property
    return haProperties.length > 0
      ? haProperties
      : [{ ...quicklink, propertyToUpdate }];
  };

  /**
   * Get properties to be updated for quicklinks
   * @param serviceModel - The service model object
   * @returns Array of quicklinks with properties to update
   */
  const getPropertiesToBeUpdatedForQuicklinks = (serviceModel: any) => {
    // Get the basic quicklinks
    const basicLinks = serviceModel.quickLinks
      .map((quicklink: any) => {
        return quicklink.links.map((link: any) => ({
          label: link.label,
          fileName: link.fileName,
          url: link.url,
          propertyToUpdate: isSSLEnabledRef.current
            ? link.https_property
            : link.http_property,
        }));
      })
      .flat();

    if (
      serviceName.toLowerCase() !== "hdfs" &&
      !serviceModel.isNameNodeHaEnabled
    ) {
      return basicLinks;
    }

    // Process each link for HA if needed
    const processedLinks = [];

    for (const link of basicLinks) {
      const haLinks = getHAPropertyPatterns(serviceName, serviceModel, link);
      processedLinks.push(...haLinks);
    }

    return processedLinks;
  };

  const updateQuickLinks = async (currentServiceName: string = serviceName) => {
    // setIsServiceQuicklinksLoading(true);
    try {
      const serviceModel = allServiceModels[currentServiceName.toLowerCase()];
      if (!serviceModel || serviceModel?.quickLinks?.length === 0) {
        //setIsServiceQuicklinksLoading(false);
        return;
      }

      // Check SSL status once for all links
      const isSSLEnabled = await checkIfSSLIsEnabled();
      isSSLEnabledRef.current = isSSLEnabled;

      // Check if service has changed during async operations
      if (currentServiceName !== serviceName) {
        return; // Abort if service has changed
      }

      const quicklinksConfigProperties =
        getPropertiesToBeUpdatedForQuicklinks(serviceModel) || [];

      // Process all links in parallel instead of sequentially
      const processedLinksPromises = quicklinksConfigProperties.map(
        (quicklink: any) => {
          const configPropFileName = quicklink.fileName;
          const quickLinkPropToBeUpdated = quicklink.propertyToUpdate;
          const indexForConf = configData?.items.length - 1;

          const matchingConfig = configData?.items[
            indexForConf
          ]?.configurations?.find(
            (conf: any) => conf.type === configPropFileName
          );

          if (!matchingConfig) {
            // if (!noMatchingConfigFound) {
            //   setNoMatchingConfigFound(true);
            // }
            // setIsServiceQuicklinksLoading(false);
            if (
              JSON.stringify(prevOverridenQuicklinks) !==
              JSON.stringify(
                allServiceModels[currentServiceName.toLowerCase()]?.quickLinks || []
              )
            ) {
              setOverridenQuicklinks(
                allServiceModels[currentServiceName.toLowerCase()]?.quickLinks || []
              );
            }
            return null;
          }

          const propValue = matchingConfig.properties[quickLinkPropToBeUpdated];
          if (!propValue) {
            // if (!noMatchingConfigFound) {
            setIsServiceQuicklinksLoading(false);
            if (
              JSON.stringify(prevOverridenQuicklinks) !==
              JSON.stringify(
                allServiceModels[currentServiceName.toLowerCase()]?.quickLinks || []
              )
            ) {
              setPrevOverridenQuicklinks(
                allServiceModels[currentServiceName.toLowerCase()]?.quickLinks || []
              );
              setOverridenQuicklinks(
                allServiceModels[currentServiceName.toLowerCase()]?.quickLinks || []
              );
            }
            // setNoMatchingConfigFound(true);
            // }
            return null;
          }

          // Update URL
          quicklink.url = reconstructURL(quicklink, propValue);
          quicklink.hostName = memoizedGetHostName(quicklink.url);

          return quicklink;
        }
      );

      // Wait for all promises to resolve simultaneously
      const processedLinks = processedLinksPromises.filter(
        (link: any) => link !== null
      );

      if (processedLinks.length === 0) {
        return;
      }

      let transformedQuicklinks =
        transformQuicklinksToHostStructure(processedLinks);

      // Final check: only update state if service hasn't changed
      if (currentServiceName === serviceName && transformedQuicklinks.length > 0) {
        setIsServiceQuicklinksLoading(false);
        if (
          JSON.stringify(prevOverridenQuicklinks) !==
          JSON.stringify(transformedQuicklinks)
        ) {
          setPrevOverridenQuicklinks(transformedQuicklinks);
          setOverridenQuicklinks(transformedQuicklinks);
        }
      }
    } catch (error) {
      console.error("Error in updateQuickLinks:", error);
      // if (!noMatchingConfigFound) {
      //   setNoMatchingConfigFound(true);
      // }
    }
  };

  useEffect(() => {
    const currentService = serviceName;
    checkIfSSLIsEnabled();
    getUpdatedConfigValues(currentService);
    updateQuickLinks(currentService);
  }, []);

  useEffect(() => {
    const currentService = serviceName;
    getUpdatedConfigValues(currentService);
    updateQuickLinks(currentService);
  }, [selectedTab, serviceName, stringifiedModel, JSON.stringify(configData)]);

  function renderQuicklinks() {
    if (isServiceQuicklinksLoading) {
      return <Spinner></Spinner>;
    }
    if (!allServiceModels[serviceName.toLowerCase()]?.quickLinks?.length)
      return <p>No links</p>;
    return (
      <div>
        <div>
          {overridenQuicklinks?.map(
            (quicklink: {
              hostName: string;
              links: any[];
              haState?: string;
            }) => {
              return (
                <>
                  <div className="fs-12 fw-bold text-light mt-4 mb-3">
                    {quicklink.hostName}
                    {quicklink.haState ? " (" + quicklink.haState + ")" : ""}
                  </div>
                  <div>
                    {quicklink.links.map(
                      (link: { label: string; url: string }) => {
                        return (
                          <div className="mt-2">
                            <a
                              href={link.url}
                              target="_blank"
                              rel="noreferrer"
                              className="custom-link fs-12"
                            >
                              {link.label}
                            </a>
                          </div>
                        );
                      }
                    )}
                  </div>
                </>
              );
            }
          )}
        </div>
      </div>
    );
  }

  return (
    <div>
      <h2>Quick Links</h2>
      <div>{renderQuicklinks()}</div>
    </div>
  );
}
export default ServiceQuicklinks;
