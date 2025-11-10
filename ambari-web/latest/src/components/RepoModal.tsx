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

import { Col, Form, Row } from "react-bootstrap";
import Modal from "./Modal";
import Tooltip from "./Tooltip";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faQuestionCircle } from "@fortawesome/free-solid-svg-icons";
import { useState } from "react";
import VersionsApi from "../api/VersionsApi";
import toast from "react-hot-toast";

type PropTypes = {
  isOpen: boolean;
  onClose: () => void;
  selectedStack: any;
};

function RepoModal({ isOpen, onClose, selectedStack }: PropTypes) {
  const [repoUrls, setRepoUrls] = useState<{ [key: string]: string }>({});
  const [skipRepoValidation, setSkipRepoValidation] = useState(false);
  const [useRedhatSatellite, setUseRedhatSatellite] = useState(
    !(selectedStack?.repository_versions?.[0]?.operating_systems?.[0]?.OperatingSystems?.ambari_managed_repositories ?? false)
  );
  const [validatingRepos, setValidatingRepos] = useState(false);
  function handleRepoUrlChange(repoId: string, url: string) {
    setRepoUrls((prev) => ({
      ...prev,
      [repoId]: url,
    }));
  }
  function getRepoModalBody() {
    return (
      <div className="m-n2">
        <div className="alert alert-info mb-3">
          Provide Base URLs for the Operating Systems you are configuring.
        </div>
        <div className="repo-table">
          <Row className="align-items-center py-3 border-bottom">
            <Col md={2}>OS</Col>
            <Col md={2}>Name</Col>
            <Col md={8}>Base URL</Col>
          </Row>
          {selectedStack?.repository_versions?.[0]?.operating_systems?.map(
            (os: any) => (
              <div key={os.OperatingSystems.os_type}>
                {os.repositories.map((repo: any, index: number) => (
                  <Row
                    className="align-items-center py-3 border-bottom"
                    key={repo.Repositories.repo_id}
                  >
                    {index === 0 ? (
                      <Col md={2}>{os.OperatingSystems.os_type}</Col>
                    ) : (
                      <Col md={2}></Col>
                    )}
                    <Col md={2}>{repo.Repositories.repo_name}</Col>
                    <Col md={8}>
                      <Form.Control
                        type="text"
                        defaultValue={repo.Repositories.base_url}
                        disabled={useRedhatSatellite}
                        onChange={(e) =>
                          handleRepoUrlChange(
                            repo.Repositories.repo_id,
                            e.target.value
                          )
                        }
                      />
                    </Col>
                  </Row>
                ))}
              </div>
            )
          )}
        </div>
        <div className="mt-3">
          <Form>
            <Form.Check
              type="checkbox"
              id="skipValidation"
              checked={skipRepoValidation}
              onChange={(e) => setSkipRepoValidation(e.target.checked)}
              label={
                <div className="d-flex align-items-center">
                  Skip Repository Base URL validation (Advanced)
                  <Tooltip
                    message="Warning! This is for advanced users only. Use this option if you want to skip validation for Repository Base URLs."
                    placement="right"
                  >
                    <FontAwesomeIcon className="ms-2" icon={faQuestionCircle} />
                  </Tooltip>
                </div>
              }
            />
            <Form.Check
              type="checkbox"
              id="redhatSatellite"
              checked={useRedhatSatellite}
              onChange={(e) => setUseRedhatSatellite(e.target.checked)}
              label={
                <div className="d-flex align-items-center">
                  Use RedHat Satellite/Spacewalk
                  <Tooltip
                    message="Disable distributed repositories and use RedHat Satellite/Spacewalk channels instead"
                    placement="right"
                  >
                    <FontAwesomeIcon className="ms-2" icon={faQuestionCircle} />
                  </Tooltip>
                </div>
              }
            />
          </Form>
        </div>
      </div>
    );
  }
  async function saveRepositories(
    stack: string,
    stackVersion: string,
    repoVersionId: string
  ) {
    try {
      const operatingSystems =
        selectedStack?.repository_versions[0].operating_systems;
      if (!operatingSystems) return;

      const payload = {
        operating_systems: operatingSystems.map((os: any) => {
          return {
            OperatingSystems: {
              os_type: os.OperatingSystems.os_type,
              ambari_managed_repositories: !useRedhatSatellite,
            },
            repositories: os.repositories.map((repo: any) => {
              const repoId = repo.Repositories.repo_id;
              return {
                Repositories: {
                  base_url: repoUrls[repoId] || repo.Repositories.base_url,
                  repo_id: repoId,
                  repo_name: repo.Repositories.repo_name,
                },
              };
            }),
          };
        }),
      };

      await VersionsApi.saveRepoVersions(
        stack,
        stackVersion,
        repoVersionId,
        payload
      );
      toast.success("Repositories saved successfully");
      onClose();
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : String(err);
      toast.error(`Failed to save repositories: ${errorMessage}`);
    }
  }
  async function validateAndSaveRepos() {
    if (!selectedStack) return;

    setValidatingRepos(true);
    const stack = selectedStack.ClusterStackVersions.stack;
    const stackVersion = selectedStack.ClusterStackVersions.version;
    const repoVersionId =
      selectedStack.repository_versions[0].RepositoryVersions.id;

    try {
      // If skip validation is checked, proceed directly to saving
      if (skipRepoValidation) {
        await saveRepositories(stack, stackVersion, repoVersionId.toString());
        return;
      }

      // Validate each repository
      const operatingSystems =
        selectedStack.repository_versions[0].operating_systems;
      const validationPromises = [];

      for (const os of operatingSystems) {
        const osType = os.OperatingSystems.os_type;

        for (const repo of os.repositories) {
          const repoId = repo.Repositories.repo_id;
          const repoName = repo.Repositories.repo_name;
          const baseUrl = repoUrls[repoId] || repo.Repositories.base_url;

          validationPromises.push(
            VersionsApi.validateRepos(stack, stackVersion, osType, repoId, {
              base_url: baseUrl,
              repo_name: repoName,
            }).catch((err) => {
              // If validation fails, throw an error with details
              const errorMessage =
                err instanceof Error ? err.message : String(err);
              throw new Error(
                `Failed to validate repository ${repoName}: ${errorMessage}`
              );
            })
          );
        }
      }

      // Wait for all validations to complete
      await Promise.all(validationPromises);

      // If all validations pass, save the repositories
      await saveRepositories(stack, stackVersion, repoVersionId.toString());
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : String(err);
      toast.error(`Validation failed: ${errorMessage}`);
    } finally {
      setValidatingRepos(false);
    }
  }
  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      modalTitle="Repositories"
      modalBody={getRepoModalBody()}
      options={{
        cancelableViaBtn: true,
        okButtonText: validatingRepos ? "VALIDATING..." : "SAVE",
        modalSize: "modal-lg",
        okButtonDisabled: validatingRepos,
      }}
      successCallback={() => validateAndSaveRepos()}
    />
  );
}

export default RepoModal;