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

import { useContext, useEffect } from "react";
import { useLazyQuicklinks } from "../../hooks/useLazyQuicklinks";
import { ServiceContext } from "../../store/ServiceContext";
import Spinner from "../../components/Spinner";

interface OptimizedServiceQuicklinksProps {
  serviceName: string;
  selectedTab: string;
}

/**
 * Optimized ServiceQuicklinks component following Ember.js pattern
 * Only loads quicklinks when component is mounted and service is selected
 * Implements lazy loading and caching for optimal performance
 */
function OptimizedServiceQuicklinks({
  serviceName,
  selectedTab,
}: OptimizedServiceQuicklinksProps) {
  const { allServiceModels } = useContext(ServiceContext);
  const {
    quicklinks,
    isLoading,
    error,
    loadQuicklinks,
    refreshQuicklinks
  } = useLazyQuicklinks(serviceName);

  // Load quicklinks only when component mounts and quicklinks tab is selected
  useEffect(() => {
      loadQuicklinks();
  }, [selectedTab, serviceName, loadQuicklinks]);

  if (isLoading) {
    return (
      <div>
        <h2>Quick Links</h2>
        <div className="d-flex justify-content-center align-items-center" style={{ minHeight: "200px" }}>
          <Spinner />
        </div>
      </div>
    );
  }

  // Render error state
  if (error) {
    return (
      <div>
        <h2>Quick Links</h2>
        <div className="alert alert-warning" role="alert">
          <i className="fa fa-exclamation-triangle me-2"></i>
          {error}
          <button 
            className="btn btn-sm btn-outline-primary ms-3"
            onClick={refreshQuicklinks}
          >
            <i className="fa fa-refresh me-1"></i>
            Retry
          </button>
        </div>
      </div>
    );
  }

  // Render no links state
  if (!quicklinks || quicklinks.length === 0) {
    return (
      <div>
        <h2>Quick Links</h2>
        <div className="mt-4 px-2 text-light">No Links</div>
      </div>
    );
  }

  // Check if federation is enabled for HDFS
  const serviceModel = allServiceModels[serviceName.toLowerCase()];
  const isFederatedHDFS = serviceName.toLowerCase() === "hdfs" && 
    serviceModel?.federationNamespaces && serviceModel.federationNamespaces.length > 1;

  if (isFederatedHDFS) {
    // Check if we have namespace-specific quicklinks
    const hasNamespaceQuicklinks = quicklinks?.some((quicklink: any) => 
      quicklink.links?.some((link: any) => link.namespace)
    );

    if (hasNamespaceQuicklinks) {
      // Render federation-grouped quicklinks
      return (
        <div>
          <h2>Quick Links</h2>
          <div>
            {serviceModel.federationNamespaces?.map((namespace: any) => {
              const namespaceLinks = quicklinks?.filter((quicklink: any) => 
                quicklink.links?.some((link: any) => link.namespace === namespace.name)
              );

              if (!namespaceLinks || namespaceLinks.length === 0) return null;

              return (
                <div key={namespace.name} className="mb-4">
                  <div className="fs-14 text-light mb-3">
                    {namespace.name}
                  </div>
                  {namespaceLinks?.map((quicklink: {
                    hostName: string;
                    links: any[];
                    haState?: string;
                  }, index: number) => {
                    const namespaceSpecificLinks = quicklink.links.filter((link: any) => 
                      link.namespace === namespace.name
                    );
                    
                    if (namespaceSpecificLinks.length === 0) return null;

                    return (
                      <div key={`${quicklink.hostName}-${index}`}>
                        <div className="fs-12 fw-bold text-light mt-3 mb-2">
                          {quicklink.hostName}
                          {quicklink.haState ? " (" + quicklink.haState + ")" : ""}
                        </div>
                        <div>
                          {namespaceSpecificLinks.map(
                            (link: { label: string; url: string }, linkIndex: number) => {
                              return (
                                <div className="mt-2" key={`${link.label}-${linkIndex}`}>
                                  <a
                                    href={link.url}
                                    target="_blank"
                                    rel="noreferrer"
                                    className="custom-link fs-12"
                                    title={`Open ${link.label} for ${quicklink.hostName}`}
                                  >
                                    <i className="fa fa-external-link me-2"></i>
                                    {link.label}
                                  </a>
                                </div>
                              );
                            }
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              );
            })}
          </div>
        </div>
      );
    } else {
      // Fallback to regular quicklinks rendering for federation when namespace data is not available
      // Group quicklinks by namespace based on host mapping
      return (
        <div>
          <h2>Quick Links</h2>
          <div>
            {serviceModel.federationNamespaces?.map((namespace: any) => {
              const namespaceQuicklinks = quicklinks?.filter((quicklink: any) => 
                namespace.hosts?.includes(quicklink.hostName)
              );

              if (!namespaceQuicklinks || namespaceQuicklinks.length === 0) {
                return (
                  <div key={namespace.name} className="namespace-quicklinks mb-4">
                    <div className="fs-14 text-light mb-3">
                      {namespace.name}
                    </div>
                    <div className="fs-12 text-light">No quicklinks available</div>
                  </div>
                );
              }

              return (
                <div key={namespace.name} className="namespace-quicklinks mb-4">
                  <div className="fs-14 text-light mb-3">
                    Namespace: {namespace.name}
                  </div>
                  {namespaceQuicklinks.map((quicklink: {
                    hostName: string;
                    links: any[];
                    haState?: string;
                  }, index: number) => (
                    <div key={`${quicklink.hostName}-${index}`}>
                      <div className="fs-12 fw-bold text-light mt-3 mb-2">
                        {quicklink.hostName}
                        {quicklink.haState ? " (" + quicklink.haState + ")" : ""}
                      </div>
                      <div>
                        {quicklink.links.map(
                          (link: { label: string; url: string }, linkIndex: number) => (
                            <div className="mt-2" key={`${link.label}-${linkIndex}`}>
                              <a
                                href={link.url}
                                target="_blank"
                                rel="noreferrer"
                                className="custom-link fs-12"
                                title={`Open ${link.label} for ${quicklink.hostName}`}
                              >
                                <i className="fa fa-external-link me-2"></i>
                                {link.label}
                              </a>
                            </div>
                          )
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              );
            })}
          </div>
        </div>
      );
    }
  }

  // Render regular quicklinks (non-federated)
  return (
    <div>
      <h2>Quick Links</h2>
      <div>
        {quicklinks.map((quicklink: {
          hostName: string;
          links: any[];
          haState?: string;
        }, index: number) => (
          <div key={`${quicklink.hostName}-${index}`}>
            <div className="fs-12 fw-bold text-light mt-4 mb-3 text-nowrap d-flex">
              {quicklink.hostName && (
                <>
                  {quicklink.hostName}
                  {quicklink.haState && (
                    <div className="fs-12 fw-bold ms-1">
                      ({quicklink.haState})
                    </div>
                  )}
                </>
              )}
            </div>
            <div>
              {quicklink.links.map((link: { 
                label: string; 
                url: string; 
              }, linkIndex: number) => (
                <div key={`${link.label}-${linkIndex}`} className="mt-2">
                  <a
                    href={link.url}
                    target="_blank"
                    rel="noreferrer"
                    className="custom-link fs-12"
                    title={`Open ${link.label} for ${quicklink.hostName}`}
                  >
                    <i className="fa fa-external-link me-2"></i>
                    {link.label}
                  </a>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
      
    </div>
  );
}

export default OptimizedServiceQuicklinks;
