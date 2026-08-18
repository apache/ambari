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

import { Alert, Button, Col, Form, InputGroup, Row } from "react-bootstrap";
import Modal from "./Modal";
import Tooltip from "./Tooltip";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faQuestionCircle, faRotateLeft, faTrash } from "@fortawesome/free-solid-svg-icons";
import { useEffect, useState } from "react";
import VersionsApi from "../api/versionsApi";
import toast from "react-hot-toast";

type PropTypes = {
  isOpen: boolean;
  onClose: () => void;
  selectedStack: any;
  canSave?: boolean;
};

function repositoryUrlMap(selectedStack: any): Record<string, string> {
  const operatingSystems = selectedStack?.repository_versions?.[0]?.operating_systems || [];
  return Object.fromEntries(
    operatingSystems.flatMap((os: any) => (os.repositories || []).map((repo: any) => [
      `${os.OperatingSystems.os_type}:${repo.Repositories.repo_id}`,
      repo.Repositories.base_url || "",
    ])),
  );
}

function RepoModal({ isOpen, onClose, selectedStack, canSave = true }: PropTypes) {
  const originalRepoUrls = repositoryUrlMap(selectedStack);
  const [repoUrls, setRepoUrls] = useState<Record<string, string>>(originalRepoUrls);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  const [validationFailed, setValidationFailed] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [skipRepoValidation, setSkipRepoValidation] = useState(false);
  const [useRedhatSatellite, setUseRedhatSatellite] = useState(
    !(selectedStack?.repository_versions?.[0]?.operating_systems?.[0]?.OperatingSystems?.ambari_managed_repositories ?? false)
  );
  const [validatingRepos, setValidatingRepos] = useState(false);

  useEffect(() => {
    if (!isOpen) return;
    setRepoUrls(repositoryUrlMap(selectedStack));
    setValidationErrors({});
    setValidationFailed(false);
    setSaveError(null);
    setSkipRepoValidation(false);
    setUseRedhatSatellite(
      !(selectedStack?.repository_versions?.[0]?.operating_systems?.[0]?.OperatingSystems?.ambari_managed_repositories ?? false),
    );
  }, [isOpen, selectedStack]);

  function handleRepoUrlChange(repoKey: string, url: string) {
    setRepoUrls((prev) => ({
      ...prev,
      [repoKey]: url,
    }));
    setValidationErrors((current) => {
      const updated = { ...current };
      delete updated[repoKey];
      return updated;
    });
    setValidationFailed(false);
    setSaveError(null);
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
          {selectedStack?.repository_versions?.[0]?.operating_systems?.map((os: any) => (
            <div key={os.OperatingSystems.os_type}>
              {(os.repositories || []).map((repo: any, index: number) => {
                  const repoKey = `${os.OperatingSystems.os_type}:${repo.Repositories.repo_id}`;
                  return (
                  <Row
                    className="align-items-center py-3 border-bottom"
                    key={repoKey}
                  >
                    {index === 0 ? (
                      <Col md={2}>{os.OperatingSystems.os_type}</Col>
                    ) : (
                      <Col md={2}></Col>
                    )}
                    <Col md={2}>{repo.Repositories.repo_name}</Col>
                    <Col md={8}>
                      <InputGroup>
                        <Form.Control
                          type="text"
                          value={repoUrls[repoKey] || ""}
                          disabled={useRedhatSatellite || validatingRepos}
                          isInvalid={Boolean(validationErrors[repoKey])}
                          onChange={(e) => handleRepoUrlChange(repoKey, e.target.value)}
                        />
                        <Tooltip message="Restore the original URL">
                          <Button
                            variant="outline-secondary"
                            disabled={validatingRepos || repoUrls[repoKey] === originalRepoUrls[repoKey]}
                            onClick={() => handleRepoUrlChange(repoKey, originalRepoUrls[repoKey])}
                            aria-label={`Restore ${repo.Repositories.repo_name}`}
                          >
                            <FontAwesomeIcon icon={faRotateLeft} />
                          </Button>
                        </Tooltip>
                        <Tooltip message="Clear this URL">
                          <Button
                            variant="outline-secondary"
                            disabled={validatingRepos || !repoUrls[repoKey]}
                            onClick={() => handleRepoUrlChange(repoKey, "")}
                            aria-label={`Clear ${repo.Repositories.repo_name}`}
                          >
                            <FontAwesomeIcon icon={faTrash} />
                          </Button>
                        </Tooltip>
                        <Form.Control.Feedback type="invalid">
                          {validationErrors[repoKey]}
                        </Form.Control.Feedback>
                      </InputGroup>
                    </Col>
                  </Row>
                  );
              })}
            </div>
          ))}
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
        {validationFailed && (
          <Alert variant="warning" className="mt-3 mb-0">
            One or more repository URLs failed validation. Correct the marked URLs, revert them, or explicitly save anyway.
          </Alert>
        )}
        {saveError && (
          <Alert variant="danger" className="mt-3 mb-0">
            {saveError}
          </Alert>
        )}
      </div>
    );
  }
  async function saveRepositories(
    stack: string,
    stackVersion: string,
    repoVersionId: string
  ) {
    const operatingSystems = selectedStack?.repository_versions?.[0]?.operating_systems;
    if (!operatingSystems) {
      throw new Error("Repository details are not available");
    }

    const payload = {
      operating_systems: operatingSystems.map((os: any) => ({
        OperatingSystems: {
          os_type: os.OperatingSystems.os_type,
          ambari_managed_repositories: !useRedhatSatellite,
        },
        repositories: os.repositories.map((repo: any) => {
          const repoId = repo.Repositories.repo_id;
          const repoKey = `${os.OperatingSystems.os_type}:${repoId}`;
          return {
            Repositories: {
              base_url: repoUrls[repoKey],
              repo_id: repoId,
              repo_name: repo.Repositories.repo_name,
            },
          };
        }),
      })),
    };

    await VersionsApi.saveRepoVersions(stack, stackVersion, repoVersionId, payload);
    toast.success("Repositories saved successfully");
    onClose();
  }
  async function validateAndSaveRepos() {
    if (!selectedStack || !canSave) return;

    setValidatingRepos(true);
    setSaveError(null);
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

      setValidationErrors({});
      setValidationFailed(false);
      const operatingSystems =
        selectedStack.repository_versions[0].operating_systems;
      const validationRequests: Array<{ key: string; name: string; request: Promise<any> }> = [];

      for (const os of operatingSystems) {
        const osType = os.OperatingSystems.os_type;

        for (const repo of os.repositories) {
          const repoId = repo.Repositories.repo_id;
          const repoName = repo.Repositories.repo_name;
          const repoKey = `${osType}:${repoId}`;
          const baseUrl = repoUrls[repoKey];

          validationRequests.push({
            key: repoKey,
            name: repoName,
            request: VersionsApi.validateRepos(stack, stackVersion, osType, repoId, {
              base_url: baseUrl,
              repo_name: repoName,
            }),
          });
        }
      }

      const results = await Promise.allSettled(
        validationRequests.map(({ request }) => request)
      );
      const errors: Record<string, string> = {};
      results.forEach((result, index) => {
        if (result.status === "rejected") {
          const request = validationRequests[index];
          const reason = result.reason;
          errors[request.key] = reason?.response?.data?.message
            || reason?.message
            || `Failed to validate ${request.name}`;
        }
      });
      if (Object.keys(errors).length) {
        setValidationErrors(errors);
        setValidationFailed(true);
        toast.error(`${Object.keys(errors).length} repository URL(s) failed validation`);
        return;
      }

      await saveRepositories(stack, stackVersion, repoVersionId.toString());
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : String(err);
      setSaveError(errorMessage);
      toast.error(`Repository update failed: ${errorMessage}`);
    } finally {
      setValidatingRepos(false);
    }
  }
  const hasEmptyUrl = !useRedhatSatellite
    && Object.values(repoUrls).some((url) => !url.trim());

  const revertChanges = () => {
    setRepoUrls(originalRepoUrls);
    setValidationErrors({});
    setValidationFailed(false);
    setSaveError(null);
    setSkipRepoValidation(false);
  };

  const saveWithoutValidation = async () => {
    if (!selectedStack || !canSave || validatingRepos || hasEmptyUrl) return;
    setValidatingRepos(true);
    try {
      await saveRepositories(
        selectedStack.ClusterStackVersions.stack,
        selectedStack.ClusterStackVersions.version,
        selectedStack.repository_versions[0].RepositoryVersions.id.toString(),
      );
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : String(err);
      setSaveError(errorMessage);
      toast.error(`Failed to save repositories: ${errorMessage}`);
    } finally {
      setValidatingRepos(false);
    }
  };

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
        okButtonDisabled: validatingRepos || !canSave || hasEmptyUrl,
        extraButtons: [
          {
            text: "REVERT",
            variant: "secondary",
            disabled: validatingRepos,
            onClick: revertChanges,
          },
          ...(validationFailed ? [{
            text: "SAVE ANYWAY",
            variant: "warning",
            disabled: validatingRepos || !canSave || hasEmptyUrl,
            onClick: () => void saveWithoutValidation(),
          }] : []),
        ],
      }}
      successCallback={() => validateAndSaveRepos()}
    />
  );
}

export default RepoModal;
