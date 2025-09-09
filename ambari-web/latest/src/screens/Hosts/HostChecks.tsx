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

import { Accordion, Form } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faCheck,
  faExclamationTriangle,
  faExternalLink,
} from "@fortawesome/free-solid-svg-icons";
import Spinner from "../../components/Spinner";
import Modal from "../../components/Modal";
import { cloneDeep, forEach, get, isEmpty, set } from "lodash";
import {
  categoriesToReportMap,
  checkHostIssues,
  hostCheckReportConstants,
  restWarningCategories,
  restWarningCategoriesToIssuesKeyMap,
} from "./constants";
import { useEffect, useState } from "react";
import modalManager from "../../store/ModalManager";
import { initialWarningData } from "../../hooks/useHostChecks";
import { translate } from "../../Utils/Utility";

enum HostOption {
  ALL = "All Hosts",
}

type HostChecksModalProps = {
  isOpen: boolean;
  onClose: () => void;
  successCallback: () => void;
  hostCheckResult: any;
  loading: boolean;
};

export const getHostWithIssues = (hostCheckResult: any) => {
  const hostNameList: any[] = [];

  get(hostCheckResult, "warningsByHost")
    ?.slice(1)
    ?.forEach((_host: any) => {
      if (_host?.warnings?.length) {
        hostNameList.push({
          name: _host?.name,
          count: _host?.warnings?.length,
        });
      }
    });

  restWarningCategories.forEach((category: any) => {
    if (hostCheckResult?.[category]?.length) {
      hostCheckResult?.[category]?.[0]?.hostsNames?.forEach(
        (_hostName: any) => {
          const prevEntry = hostNameList.find(
            (host: any) => host?.name === _hostName
          );
          if (!prevEntry) {
            hostNameList.push({
              name: _hostName,
              count: 1,
            });
          } else {
            prevEntry.count++;
          }
        }
      );
    }
  });

  return hostNameList;
};

export const getTotalIssuesCount = (hostCheckResult: any) => {
  let totalIssues = 0;
  const warningNames = new Set<string>();
  hostCheckResult.warningsByHost
    .filter((_host: any) => _host.name === HostOption.ALL)
    .forEach((_host: any) => {
      _host.warnings.forEach((warning: any) => {
        if (warningNames.has(warning.name)) {
          return;
        } else {
          warningNames.add(warning.name);
          totalIssues++;
        }
      });
    });

  restWarningCategories.forEach((category: any) => {
    if (hostCheckResult?.[category]?.length) {
      totalIssues++;
    }
  });

  return totalIssues;
};

export default function HostChecks({
  isOpen,
  onClose,
  successCallback,
  hostCheckResult,
  loading,
}: HostChecksModalProps) {
  const [issues, setIssues] = useState<any>(checkHostIssues);
  const [filteredHostCheckRes, setFilteredHostCheckRes] =
    useState<any>(hostCheckResult);
  const [selectedHost, setSelectedHost] = useState(HostOption.ALL);

  const showHostListModal = (hostsList: any[]) => {
    const modalProps = {
      onClose: () => {},
      modalTitle: "List of hosts",
      modalBody: getHostListModalBody(hostsList),
      successCallback: () => {
        modalManager.hide();
      },
      options: {
        cancelableViaIcon: true,
        cancelableViaBtn: false,
      },
    };
    modalManager.show(modalProps);
  };

  const getHostListModalBody = (hostsList: any[]) => {
    return (
      <div>
        {hostsList.map((host: any) => {
          return <div className="fs-12 mb-1">{host}</div>;
        })}
      </div>
    );
  };

  const getWarningListItemStructure = (warning: any, customText: string) => {
    return (
      <div className="d-flex mb-1">
        <div className="fs-12 w-70">{warning.name}</div>
        <div className="fs-12 w-30">
          {customText}{" "}
          <span
            className="custom-link fs-12"
            onClick={() => {
              showHostListModal(warning.hosts);
            }}
          >
            {warning.hosts.length} {warning.hosts.length > 1 ? "hosts" : "host"}
          </span>
        </div>
      </div>
    );
  };

  useEffect(() => {
    if (
      JSON.stringify(filteredHostCheckRes) !==
      JSON.stringify(initialWarningData)
    ) {
      const issuesCopy = cloneDeep(checkHostIssues);
      const warningNames = new Set<string>();
      filteredHostCheckRes.warningsByHost
        .filter((_host: any) => _host.name === selectedHost)
        .forEach((_host: any) => {
          _host.warnings.forEach((warning: any) => {
            const category = warning.category;
            if (warningNames.has(warning.name)) {
              return;
            } else {
              warningNames.add(warning.name);
            }
            const issue = get(issuesCopy, category, {});
            issue.count = issue.count + 1;
            switch (category) {
              case "packages":
                issue.data.push(
                  <div className="d-flex mb-1">
                    <div className="fs-12 w-40">{warning.name}</div>
                    <div className="fs-12 w-30">{warning.version}</div>
                    <div className="fs-12 w-30">
                      Installed on{" "}
                      <span
                        className="custom-link fs-12"
                        onClick={() => {
                          showHostListModal(warning.hosts);
                        }}
                      >
                        {warning.hosts.length} host
                      </span>
                    </div>
                  </div>
                );
                break;
              case "processes":
              case "firewall":
                issue.data.push(
                  getWarningListItemStructure(warning, "Running on")
                );
                break;
              case "services":
                issue.data.push(
                  getWarningListItemStructure(warning, "Unhealthy on")
                );
                break;
              case "users":
              case "fileFolders":
                issue.data.push(
                  getWarningListItemStructure(warning, "Present on")
                );
                break;
              case "misc":
                issue.data.push(
                  getWarningListItemStructure(warning, "Umask on")
                );
                break;
              case "alternatives":
                issue.data.push(
                  <div className="d-flex mb-1">
                    <div className="fs-12 w-40">{warning.name}</div>
                    <div className="fs-12 w-30">{warning.target}</div>
                    <div className="fs-12 w-30">
                      On{" "}
                      <span
                        className="custom-link fs-12"
                        onClick={() => {
                          showHostListModal(warning.hosts);
                        }}
                      >
                        {warning.hosts.length} host
                      </span>
                    </div>
                  </div>
                );
                break;
              case "reverseLookup":
                issue.data.push(getWarningListItemStructure(warning, ""));
                break;
            }
          });
        });

      restWarningCategories.forEach((category) => {
        const issue = get(
          issuesCopy,
          get(restWarningCategoriesToIssuesKeyMap, category),
          {}
        );
        set(issue, "count", get(filteredHostCheckRes, category, []).length);
        set(
          issue,
          "data",
          get(filteredHostCheckRes, category, []).map((warning: any) => {
            return (
              <div>
                {forEach(warning.hosts, (host: any) => {
                  return (
                    <div className="d-flex mb-1">
                      <div className="fs-12 w-70">
                        {warning.name + " " + host}
                      </div>
                    </div>
                  );
                })}
              </div>
            );
          })
        );
        set(
          issuesCopy,
          get(restWarningCategoriesToIssuesKeyMap, category),
          issue
        );
      });
      setIssues(issuesCopy);
    }
  }, [JSON.stringify(filteredHostCheckRes)]);

  useEffect(() => {
    if (!isEmpty(hostCheckResult)) {
      if (selectedHost === HostOption.ALL) {
        setFilteredHostCheckRes(hostCheckResult);
      } else {
        const filteredHostCheckResCopy = cloneDeep(hostCheckResult);
        filteredHostCheckResCopy.warningsByHost =
          filteredHostCheckResCopy.warningsByHost.filter(
            (host: any) => host.name === selectedHost
          );
        restWarningCategories.forEach((category) => {
          filteredHostCheckResCopy[category].forEach((warning: any) => {
            warning.hosts = [selectedHost];
            warning.hostsLong = [selectedHost];
            warning.hostsNames = [selectedHost];
          });
        });
        setFilteredHostCheckRes(filteredHostCheckResCopy);
      }
    }
  }, [hostCheckResult, selectedHost]);

  const getIssue = (issue: string) => {
    return get(issues, issue);
  };

  const getHostNames = (result: any) => {
    const hostNames = get(result, "warningsByHost", []).map(
      (host: any) => host.name
    );
    return hostNames;
  };

  const contentInDetails = () => {
    let newContent = "";
    let hostNamesWithWarnings = getHostNames(filteredHostCheckRes);
    if (selectedHost === HostOption.ALL) {
      hostNamesWithWarnings = hostNamesWithWarnings.filter(
        (hostName: string) => hostName !== HostOption.ALL
      );
    }

    newContent +=
      hostCheckReportConstants[
        "installer.step3.hostWarningsPopup.report.header"
      ] + new Date();
    newContent +=
      hostCheckReportConstants[
        "installer.step3.hostWarningsPopup.report.hosts"
      ];
    newContent += hostNamesWithWarnings.join(" ");

    const isHostPresent = (hostNames: string[]) => {
      return hostNames.some((name) => hostNamesWithWarnings.includes(name));
    };

    const processContent = filteredHostCheckRes?.allWarnings?.filter(
      (item: any) =>
        item?.category === "processes" && isHostPresent(item?.hosts)
    );
    if (processContent.length) {
      newContent +=
        hostCheckReportConstants[
          "installer.step3.hostWarningsPopup.report.process"
        ];
      processContent.forEach((process: any, i: number) => {
        process?.hosts?.forEach((host: any, j: number) => {
          if (!!i || !!j) {
            newContent += ",";
          }
          newContent += `(${host},${process?.user},${process?.pid})`;
        });
      });
    }

    categoriesToReportMap.forEach((category) => {
      if (restWarningCategories.includes(category?.key)) {
        const catContent = filteredHostCheckRes?.[category?.key];
        if (catContent.length) {
          newContent += hostCheckReportConstants[category?.label];
          newContent += catContent?.[0]?.hostsNames?.join(category?.separator);
        }
      } else {
        const catContent = filteredHostCheckRes?.allWarnings?.filter(
          (item: any) =>
            item?.category === category?.key && isHostPresent(item?.hosts)
        );
        if (catContent.length) {
          newContent += hostCheckReportConstants[category?.label];
          if (category?.mapProperty) {
            newContent += catContent
              .map((warning: any) => warning[category?.mapProperty])
              .join(category?.separator);
          }
        }
      }
    });

    newContent += "</p>";
    return newContent;
  };

  const getModalBody = () => {
    if (loading) {
      return <Spinner />;
    }

    return (
      <div>
        <div className="fs-12 mb-1">
          Host Checks found{" "}
          <span className="fw-bold text-dark fs-12">
            {getTotalIssuesCount(hostCheckResult)} issues on{" "}
            {getHostWithIssues(hostCheckResult).length} host.
          </span>
        </div>
        <div className="fs-12 mb-4">
          After manually resolving the issues, click{" "}
          <span className="fw-bold text-dark fs-12">Rerun Checks.</span>
        </div>
        <div className="d-flex justify-content-between mb-2">
          <div className="d-flex">
            <Form.Label className="fw-bold text-dark pt-2 fs-12 me-5">
              Hosts
            </Form.Label>
            <Form.Select
              className="custom-form-control fs-12"
              value={selectedHost}
              onChange={(e) => setSelectedHost(e.target.value as HostOption)}
            >
              <option value={HostOption.ALL}>All Hosts</option>
              {getHostNames(hostCheckResult)
                .slice(1)
                .map((hostName: string) => (
                  <option key={hostName} value={hostName}>
                    {hostName}
                  </option>
                ))}
            </Form.Select>
          </div>
          <div
            className="custom-link fs-12 pt-2"
            onClick={() => {
              const content = contentInDetails();
              const newWindow = window.open("", "_blank");
              newWindow?.document.write(content);
            }}
          >
            <FontAwesomeIcon icon={faExternalLink} className="fs-12" /> Show
            Report
          </div>
        </div>
        <div>
          {Object.keys(issues).map((issue: string) => {
            return (
              <Accordion className="border-0" key={issue}>
                <Accordion.Item eventKey={issue} className="border-0">
                  <Accordion.Header className="p-0">
                    <div className="d-flex justify-content-between">
                      <div className="d-flex">
                        {getIssue(issue).data.length ? (
                          <FontAwesomeIcon
                            icon={faExclamationTriangle}
                            className="me-2 text-danger"
                          />
                        ) : (
                          <FontAwesomeIcon
                            icon={faCheck}
                            className="me-2 text-success"
                          />
                        )}
                        <div className="fs-12">
                          {getIssue(issue).displayName}
                        </div>
                      </div>
                      <div className="fs-12 px-1">
                        ({getIssue(issue).count})
                      </div>
                    </div>
                  </Accordion.Header>
                  <hr className="m-0" />
                  <Accordion.Body>
                    <div className="fs-12">
                      {getIssue(issue).data.length
                        ? getIssue(issue).dataMessage
                        : `There were no ${getIssue(
                            issue
                          ).displayName.toLowerCase()}`}
                    </div>
                    <hr />
                    <div>
                      {getIssue(issue).data.map((data: any) => {
                        return (
                          <div>
                            {data}
                            <hr className="p-0" />
                          </div>
                        );
                      })}
                    </div>
                  </Accordion.Body>
                </Accordion.Item>
              </Accordion>
            );
          })}
        </div>
      </div>
    );
  };

  return (
    <div>
      {isOpen ? (
        <Modal
          isOpen={isOpen}
          onClose={onClose}
          modalTitle={String(
            translate("installer.step3.warnings.popup.header")
          )}
          modalBody={getModalBody()}
          successCallback={successCallback}
          options={{
            okButtonDisabled: loading,
            okButtonText: String(
              translate("installer.step3.hostWarningsPopup.rerunChecks")
            ).toUpperCase(),
            okButtonVariant: "warning",
            cancelButtonText: String(translate("common.close")).toUpperCase(),
            cancelableViaIcon: true,
            cancelableViaBtn: true,
          }}
        />
      ) : null}
    </div>
  );
}
