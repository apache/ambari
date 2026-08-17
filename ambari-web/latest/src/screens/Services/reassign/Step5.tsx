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

import { useContext, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { Alert } from "react-bootstrap";
import { ReassignContext } from "./store/context";
import { ActionTypes } from "./store/types";
import WizardFooter from "../../../components/StepWizard/WizardFooter";
import {
  componentsWithManualCommands,
  reassignSteps,
} from "./constants";
import { find, get } from "lodash";
import {
  getStepData,
  translate,
} from "../../../Utils/Utility";
import ConfigsApi from "../../../api/configsApi";
import { AppContext } from "../../../store/context";
import { ServiceContext } from "../../../store/ServiceContext";
import { showConfirmationPopup } from "../../Hosts/utils";
import { getDatabaseManualCommands } from "../../../Utils/reassignManualCommands";

function Step5() {
  const { componentName } = useParams<{ componentName: string }>();
  const { clusterName } = useContext(AppContext);
  const { allServiceModels } = useContext(ServiceContext);
  const [configs, setConfigs] = useState<any>({});
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleNextImperitive, jumpToStep },
  } = useContext(ReassignContext);

  const [isNextEnabled, setIsNextEnabled] = useState(false);

  const assignMastersData = getStepData(
    state,
    reassignSteps.ASSIGN_MASTER,
    "masterComponentHosts",
    "reassignSteps"
  );

  // Check if HA is enabled
  const isHaEnabled = get(
    allServiceModels,
    "hdfs.isNameNodeHaEnabled",
    false
  );

  // Helper to get active NameNode (not being moved) for HA scenarios
  const getActiveNameNodeHost = () => {
    const reassignHosts = getReassignHosts();
    const allNameNodes = assignMastersData.filter(
      (master: any) => master.component_name === "NAMENODE"
    );
    
    // Find the NameNode that is NOT the source or target
    const activeNN = allNameNodes.find(
      (nn: any) => 
        nn.hostName !== reassignHosts.source && 
        nn.hostName !== reassignHosts.target
    );
    
    return activeNN?.hostName || "";
  };

  useEffect(() => {
    getConfigsData();
    const timer = setTimeout(() => {
      setIsNextEnabled(true);
    }, 2000);

    return () => clearTimeout(timer);
  }, []);

  const getConfigsData = async () => {
    // Get both HDFS and YARN configs since we need both for different components
    const allHDFSConfigs = await ConfigsApi.getConfigValues(
      clusterName,
      "HDFS");
    const allYARNConfigs = await ConfigsApi.getConfigValues(
      clusterName,
      "YARN");
    
    // Merge configurations from both services
    const mergedConfigs = {
      items: [
        {
          configurations: [
            ...(allHDFSConfigs?.items?.[0]?.configurations || []),
            ...(allYARNConfigs?.items?.[0]?.configurations || [])
          ]
        }
      ]
    };
    
    setConfigs(mergedConfigs);
  }

  const getReassignHosts = () => {
    const sourceHost = find(
      assignMastersData,
      (master: any) =>
        master.component_name === componentName && master.isMoving&& master.movedHost
    )?.hostName;
    const targetHost = find(
      assignMastersData,
      (master: any) =>
        master.component_name === componentName && master.isMoving && master.movedHost
    )?.movedHost;

    return {
      source: sourceHost,
      target: targetHost,
    };
  };

  const getNameNodeDirectories = () => {
    let nameNodeDirs = "";
    get(configs, "items.[0].configurations", []).forEach((config: any) => {
      if (config.type === "hdfs-site") {
        nameNodeDirs =
          config.properties["dfs.namenode.name.dir"] || nameNodeDirs;
      }
    });

    return nameNodeDirs.split(",").map((dir: string) => dir.trim());
  };

  const getSecondaryNameNodeDirectories = () => {
    let secondaryNameNodeDirs = "";
    get(configs, "items.[0].configurations", []).forEach((config: any) => {
      if (config.type === "hdfs-site") {
        secondaryNameNodeDirs =
          config.properties["dfs.namenode.checkpoint.dir"] || secondaryNameNodeDirs;
      }
    });

    return secondaryNameNodeDirs.split(",").map((dir: string) => dir.trim());
  };

  const getManualCommands = () => {
    if (!componentsWithManualCommands.includes(componentName || "")) {
      return null;
    }

    const reassignHosts = getReassignHosts();
    const sourceHost = reassignHosts.source;
    const targetHost = reassignHosts.target;

    let hdfsUser = "hdfs";
    let hadoopGroup = "hadoop";
    get(configs, "items.[0].configurations", []).forEach((config: any) => {
      if (config.type === "hadoop-env") {
        hdfsUser = config.properties["hdfs_user"] || hdfsUser;
        hadoopGroup = config.properties["user_group"] || hadoopGroup;
      }
    });

    const databaseSteps = getDatabaseManualCommands({
      componentName: componentName || "",
      groupName: hadoopGroup,
      sourceHost,
      targetHost,
    });
    if (databaseSteps) {
      return { steps: databaseSteps };
    }

    if (componentName === "NAMENODE") {
      if (isHaEnabled) {
        // HA-enabled NameNode move
        const activeNameNodeHost = getActiveNameNodeHost();
        
        return {
          steps: [
            {
              number: 1,
              description: (
                <>
                  Login to the NameNode host <strong className="fw-bold text-dark">{activeNameNodeHost}</strong>.
                </>
              )
            },
            {
              number: 2,
              description: "Reset automatic failover information in ZooKeeper by running:",
              command: `sudo su ${hdfsUser} -l -c 'hdfs zkfc -formatZK'`
            },
            {
              number: 3,
              description: (
                <>
                  Login to the newly installed NameNode host <strong className="fw-bold text-dark">{targetHost}</strong>.
                  <br />
                  <Alert variant="warning" className="mt-2 mb-0">
                    <strong>Important!</strong> Be sure to login to the newly installed NameNode host.
                    <br />
                    This is a different host from the Steps 1 and 2 above.
                  </Alert>
                </>
              )
            },
            {
              number: 4,
              description: "Initialize the metadata by running:",
              command: `sudo su ${hdfsUser} -l -c 'hdfs namenode -bootstrapStandby'`
            }
          ]
        };
      } else {
        // Non-HA NameNode move
        const nameNodeDirs = getNameNodeDirectories();
        const directoriesPath = nameNodeDirs.join(",");
        const directoriesForChown = nameNodeDirs.join(" ");

        return {
          steps: [
            {
              number: 1,
              description: (
                <>
                  Copy the contents of <strong className="fw-bold text-dark">{directoriesPath}</strong> on the source host <strong className="fw-bold text-dark">{sourceHost}</strong> to <strong className="fw-bold text-dark">{directoriesPath}</strong> on the target host <strong className="fw-bold text-dark">{targetHost}</strong>.
                </>
              )
            },
            {
              number: 2,
              description: (
                <>
                  Login to the target host <strong className="fw-bold text-dark">{targetHost}</strong> and change permissions for the NameNode dirs by running:
                </>
              ),
              command: `chown -R ${hdfsUser}:hadoop ${directoriesForChown}`
            },
            {
              number: 3,
              description: "Create marker directory by running:",
              command: "mkdir -p /var/lib/hdfs/namenode/formatted"
            }
          ]
        };
      }
    } else if (componentName === "APP_TIMELINE_SERVER") {
      // Get yarn user from configs
      let yarnUser = "yarn";
      // Get timeline path dynamically from yarn-site config (fallback to default)
      let timelinePath = "/hadoop01/hadoop/yarn/timeline/timeline-state-store.ldb";
      const atsDir = "timeline-state-store.ldb";
      
      // Get the YARN user from yarn-env config and timeline path from yarn-site config
      get(configs, "items.[0].configurations", []).forEach((config: any) => {
        if (config.type === "yarn-env") {
          yarnUser = config.properties["yarn_user"] || yarnUser;
        }
        if (config.type === "yarn-site") {
          timelinePath = config.properties["yarn.timeline-service.leveldb-timeline-store.path"] || timelinePath;
        }
      });

      return {
        steps: [
          {
            number: 1,
            description: (
              <>
                Copy <strong className="fw-bold text-dark">{timelinePath}/{atsDir}</strong> from the source host <strong className="fw-bold text-dark">{sourceHost}</strong> to <strong className="fw-bold text-dark">{timelinePath}/{atsDir}</strong> on the target host <strong className="fw-bold text-dark">{targetHost}</strong>.
              </>
            )
          },
          {
            number: 2,
            description: (
              <>
                Login to the target host <strong className="fw-bold text-dark">{targetHost}</strong> and change permissions by running:
              </>
            ),
            command: `chown -R ${yarnUser}:hadoop ${timelinePath}/${atsDir}`
          },
          {
            number: 3,
            description: "Set the appropriate permissions by running:",
            command: `chmod -R 700 ${timelinePath}/${atsDir}`
          }
        ]
      };
    } else if (componentName === "SECONDARY_NAMENODE") {
      // Secondary NameNode move
      const secondaryNameNodeDirs = getSecondaryNameNodeDirectories();
      const directoriesPath = secondaryNameNodeDirs.join(",");
      const directoriesForChown = secondaryNameNodeDirs.join(" ");

      return {
        steps: [
          {
            number: 1,
            description: (
              <>
                Copy the contents of <strong className="fw-bold text-dark">{directoriesPath}</strong> on the source host <strong className="fw-bold text-dark">{sourceHost}</strong> to <strong className="fw-bold text-dark">{directoriesPath}</strong> on the target host <strong className="fw-bold text-dark">{targetHost}</strong>.
              </>
            )
          },
          {
            number: 2,
            description: (
              <>
                Login to the target host <strong className="fw-bold text-dark">{targetHost}</strong> and change permissions for the SNameNode dirs by running:
              </>
            ),
            command: `chown -R ${hdfsUser}:hadoop ${directoriesForChown}`
          }
        ]
      };
    }

    return null;
  };

  const handleNext = () => {
    showConfirmationPopup(
      translate("services.reassign.step5.confirmPopup.body") as string,
      () => {
        dispatch({
          type: ActionTypes.STORE_INFORMATION,
          payload: {
            step: reassignSteps.MANUAL_COMMANDS,
            data: {
              manualCommandsCompleted: true,
            },
          },
        });
        flushStateToDb("next");
        handleNextImperitive();
      }
    );
  };

  const handleBack = () => {
    flushStateToDb("back");
    jumpToStep(4);
  };

  const manualCommands = getManualCommands();

  return (
    <>
      <h3 className="step-title">
        {translate("services.reassign.step5.header")}
      </h3>

      {manualCommands ? (
        <Alert variant="info" className="mb-4">
          <div className="manual-commands-content">
            <ol className="list-unstyled m-0">
              {manualCommands.steps.map((step: any) => (
                <li key={step.number} className="d-flex align-items-start mb-3">
                  <span className="fw-bold me-2" style={{ minWidth: '20px' }}>{step.number}.</span>
                  <div className="flex-fill">
                    <div className={`${step.command ? 'mb-2' : ''}`}>
                      {step.description}
                    </div>
                    {step.command && (
                      <div className="bg-white p-2 border rounded font-monospace small text-muted mt-1">
                        {step.command}
                      </div>
                    )}
                  </div>
                </li>
              ))}
            </ol>
          </div>
        </Alert>
      ) : null}

      <WizardFooter
        step={currentStep}
        isNextEnabled={isNextEnabled}
        onNext={handleNext}
        onBack={handleBack}
        onCancel={() => {
          flushStateToDb("cancel");
        }}
      />
    </>
  );
}

export default Step5;
