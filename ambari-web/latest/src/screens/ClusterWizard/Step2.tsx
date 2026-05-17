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

import { Button, Card, Form } from "react-bootstrap";
import DefaultButton from "../../components/DefaultButton";
import { useContext, useEffect, useRef, useState } from "react";
import Tooltip from "../../components/Tooltip";
import { SubmitHandler, useForm } from "react-hook-form";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCircleXmark } from "@fortawesome/free-solid-svg-icons";
import Modal from "../../components/Modal";
import { isHostname } from "./utils";
import { ActionTypes } from "./clusterStore/types";
import WizardFooter from "../../components/StepWizard/WizardFooter";
import { getStepData } from "../../Utils/Utility";
import { ContextWrapper } from ".";

type FormFields = {
  targetHosts: string;
  isSshRegistration: boolean;
  sshKey: string;
  sshUserAccount: string;
  sshPortNumber: number;
};

interface Step2Props {
  wizardName?: string;
  installedHosts?: string[];
}

export default function Step2({ installedHosts = [] }: Step2Props) {
  const [showManualRegistrationWarning, setShowManualRegistrationWarning] =
    useState(false);
  const { Context } = useContext(ContextWrapper);
  const {
    stepWizardUtilities: { currentStep, handleNextImperitive, handleBackImperitive },
  }: any = useContext(Context);
  const [sshFile, setSshFile] = useState<File | null>(null);
  const sshFileInputField = useRef<HTMLInputElement>(null);
  const [nextEnabled, setNextEnabled] = useState(false);
  const registerRef = useRef(null);
  const {
    register,
    handleSubmit,
    setError,
    clearErrors,
    setValue,
    watch,
    formState: { errors },
  } = useForm<FormFields>({
    defaultValues: {
      sshUserAccount: "root",
      sshPortNumber: 22,
    },
  });
  const [formData, setFormData] = useState<FormFields>();
  const [hostNameArr, setHostNameArr] = useState<string[]>([]);
  const [invalidHostNames, setInvalidHostNames] = useState<string[]>([]);
  const [inputtedAgainHostNames, setInputtedAgainHostNames] = useState<
    string[]
  >([]);
  const [showPatternExpressionModal, setShowPatternExpressionModal] =
    useState(false);
  const [showInvalidHostnameWarning, setShowInvalidHostnameWarning] =
    useState(false);
  const [showInstalledHostnameWarning, setShowInstalledHostnameWarning] =
    useState(false);
  const [showBeforeProceedModal, setShowBeforeProceedModal] = useState(false);
  const isSshRegistration = watch("isSshRegistration", true);
  const { state, dispatch, flushStateToDb }: any = useContext(Context);

  const enableNext = () => {
    setNextEnabled(true);
  };

  useEffect(() => {
    if (!isSshRegistration) {
      setShowManualRegistrationWarning(true);
    }
  }, [isSshRegistration]);

  useEffect(() => {
    const stepData = getStepData(state, currentStep.name, "");
    if (stepData) {
      // setValue("isSshRegistration", stepData?.isSshRegistration);
      // setValue("sshKey", stepData?.sshKey!);
      // setValue("targetHosts", stepData?.targetHosts?.join("\n"));
      // setValue("sshUserAccount", stepData?.sshUserAccount);
      // setValue("sshPortNumber", stepData?.sshPortNumber);
      // installedHosts= stepData?.installedHosts || [];
      // setHostNameArr(stepData?.targetHosts || []);
    }
  }, []);

  const handleFileChange = (e: any) => {
    const file = e.target.files[0];
    if (file) {
      setSshFile(file);
      const reader = new FileReader();
      reader.onload = (event) => {
        if (event.target && event.target.result) {
          setValue("sshKey", event.target.result as string);
          clearErrors("sshKey");
        }
      };
      reader.readAsText(file);
    }
  };

  const handleChooseFileClick = () => {
    if (sshFileInputField.current) {
      sshFileInputField.current.click();
    }
  };

  const isAllHostNamesValid = (hostNames: string[]) => {
    let tempArr: string[] = [];
    hostNames.forEach((hostName) => {
      if (!isHostname(hostName)) {
        tempArr.push(hostName);
      }
    });
    if (tempArr.length) {
      setInvalidHostNames(tempArr);
      return false;
    }
    return true;
  };

  const parseHostNamesAsPatternExpression = (hostNames: string[]) => {
    let tempArr: string[] = [];
    let isPatternExpressionPresent = false;
    hostNames.forEach((hostName) => {
      const patternMatch = hostName.match(/(.*)\[(\d+)-(\d+)\](.*)/);

      if (patternMatch) {
        const [_, prefix, start, end, suffix] = patternMatch;
        let hnlen = tempArr.length;
        for (let i = parseInt(start); i <= parseInt(end); i++) {
          isPatternExpressionPresent = true;
          tempArr.push(`${prefix}${i}${suffix}`);
        }
        if (hnlen === tempArr.length) {
          tempArr.push(hostName);
        }
      } else {
        tempArr.push(hostName);
      }
    });

    tempArr = Array.from(new Set(tempArr));

    setHostNameArr(tempArr);
    return isPatternExpressionPresent;
  };

  const updateHostNameArr = (targetHostsData: string) => {
    let targetHosts = targetHostsData
      .split(new RegExp("\\s+", "g"))
      .filter((host) => host.trim() !== "");
    let isPatternExpressionPresent =
      parseHostNamesAsPatternExpression(targetHosts);

    setHostNameArr((prevHostNameArr) => {
      let tempNotInstalledHostNameArr: string[] = [];
      let tempInputtedAgainHostNameArr: string[] = [];
      prevHostNameArr.forEach((hostName) => {
        if (installedHosts.includes(hostName)) {
          tempInputtedAgainHostNameArr.push(hostName);
        } else {
          tempNotInstalledHostNameArr.push(hostName);
        }
      });

      setInputtedAgainHostNames(tempInputtedAgainHostNameArr);

      if (!tempNotInstalledHostNameArr.length) {
        setError("targetHosts", {
          message: "All these hosts are already part of the cluster",
        });
        return prevHostNameArr;
      }

      if (isPatternExpressionPresent) {
        setShowPatternExpressionModal(true);
      } else {
        if (tempInputtedAgainHostNameArr.length) {
          setShowInstalledHostnameWarning(true);
        } else {
          if (isAllHostNamesValid(tempNotInstalledHostNameArr)) {
            if (!isSshRegistration) {
              setShowBeforeProceedModal(true);
            }
          } else {
            setShowInvalidHostnameWarning(true);
          }
        }
      }

      return tempNotInstalledHostNameArr;
    });
  };

  const onSubmit: SubmitHandler<FormFields> = (data) => {
    setFormData(data);
    const apiData = updateHostNameArr(data.targetHosts);
    console.log("API Data: ", apiData);
  };

  useEffect(() => {
    if (registerRef.current) enableNext();
  }, [registerRef.current]);

  const moveToNextStep = () => {
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: currentStep.name,
        data: {
          ...formData,
          targetHosts: hostNameArr,
          installedHosts: installedHosts,
        },
      },
    });
    flushStateToDb("next");
    handleNextImperitive();
  };

  return (
    <>
      <div>
        {showPatternExpressionModal ? (
          <Modal
            isOpen={showPatternExpressionModal}
            onClose={() => setShowPatternExpressionModal(false)}
            modalTitle="Host name pattern expressions"
            modalBody={hostNameArr.join("\n\n")}
            successCallback={() => {
              setShowPatternExpressionModal(false);
              if (inputtedAgainHostNames.length) {
                setShowInstalledHostnameWarning(true);
              } else {
                if (isAllHostNamesValid(hostNameArr)) {
                  if (!isSshRegistration) {
                    setShowBeforeProceedModal(true);
                  } else {
                    moveToNextStep();
                  }
                } else {
                  setShowInvalidHostnameWarning(true);
                }
              }
            }}
            options={{
              cancelableViaBtn: true,
              cancelableViaIcon: true,
            }}
          />
        ) : null}
        {showInstalledHostnameWarning ? (
          <Modal
            isOpen={showInstalledHostnameWarning}
            onClose={() => setShowInstalledHostnameWarning(false)}
            modalTitle="Warning"
            modalBody={
              `These hosts are already installed on the cluster and will be ignored: \n\n` +
              inputtedAgainHostNames.join(", ") +
              `\n\nDo you want to continue?`
            }
            successCallback={() => {
              setShowInstalledHostnameWarning(false);
              if (isAllHostNamesValid(hostNameArr)) {
                if (!isSshRegistration) {
                  setShowBeforeProceedModal(true);
                } else {
                  moveToNextStep();
                }
              } else {
                setShowInvalidHostnameWarning(true);
              }
            }}
            options={{
              cancelableViaBtn: true,
              cancelableViaIcon: true,
            }}
          />
        ) : null}
        {showInvalidHostnameWarning ? (
          <Modal
            isOpen={showInvalidHostnameWarning}
            onClose={() => setShowInvalidHostnameWarning(false)}
            modalTitle="Warning"
            modalBody={
              `The following hostnames are not valid FQDNs: \n\n` +
              invalidHostNames.join(", ") +
              `\n\nThis may cause problems during installation. Do you want to continue?`
            }
            successCallback={() => {
              setShowInvalidHostnameWarning(false);
              if (!isSshRegistration) {
                setShowBeforeProceedModal(true);
              } else {
                moveToNextStep();
              }
            }}
            options={{
              cancelableViaBtn: true,
              cancelableViaIcon: true,
              okButtonText: "CONTINUE",
            }}
          />
        ) : null}
        {showBeforeProceedModal ? (
          <Modal
            isOpen={showBeforeProceedModal}
            onClose={() => setShowBeforeProceedModal(false)}
            modalTitle="Before You Proceed"
            modalBody="You must install Ambari Agents on each host you want to manage before you proceed."
            successCallback={() => {
              setShowBeforeProceedModal(false);
              moveToNextStep();
            }}
            options={{
              cancelableViaBtn: true,
              cancelableViaIcon: true,
            }}
          />
        ) : null}
        {showManualRegistrationWarning ? (
          <Modal
            isOpen={showManualRegistrationWarning}
            onClose={() => setShowManualRegistrationWarning(false)}
            modalTitle="Warning"
            modalBody="By not using SSH to connect to the target hosts, you must manually install and start the Ambari Agent on each host in order for the wizard to perform the necessary configurations and software installs."
            successCallback={() => setShowManualRegistrationWarning(false)}
            options={{
              cancelableViaBtn: true,
              cancelableViaIcon: true,
            }}
          />
        ) : null}
        <h2 className="step-title">Install Options</h2>
        <p className="make-all-grey step-description">
          Enter the list of hosts to be included in the cluster and provide your
          SSH key.
        </p>
        <Card className="p-4">
          <Form onSubmit={handleSubmit(onSubmit)}>
            <div className="mb-4">
              <h2 className="mb-3">Target Hosts</h2>
              <Form.Label className="make-all-grey mb-2">
                Enter a list of hosts using the Fully Qualified Domain Name
                (FQDN), one per line. Or use{" "}
                <Tooltip
                  message="You can use pattern expressions to specify a number of target hosts. For example, to specify host01.domain thru host10.domain, enter host[01-10].domain in the target hosts textarea."
                  placement="right"
                  heading="Pattern Expressions"
                >
                  <span className="custom-link">Pattern Expressions</span>
                </Tooltip>
              </Form.Label>
              <div className="d-flex">
                <Form.Control
                  {...register("targetHosts", {
                    required: "You must specify at least one host name",
                  })}
                  as="textarea"
                  rows={4}
                  className={errors.targetHosts ? "w-75 border-danger" : "w-75"}
                  placeholder="host names"
                />
                {errors.targetHosts && (
                  <div className="text-danger mt-3 ms-3">
                    <FontAwesomeIcon icon={faCircleXmark} />{" "}
                    {errors.targetHosts.message}
                  </div>
                )}
              </div>
            </div>
            <div>
              <h2 className="mb-3">Host Registration Information</h2>
              <div className="d-flex justify-content-between w-75 mb-3">
                <div className="d-flex make-all-grey">
                  <Form.Check
                    type="radio"
                    id="ssh-registration-radio"
                    {...register("isSshRegistration")}
                    checked={isSshRegistration}
                    onChange={() =>
                      setValue("isSshRegistration", !isSshRegistration)
                    }
                    className="custom-checkbox"
                  />
                  <Form.Label htmlFor="ssh-registration-radio" className="pt-1 ps-2">
                    Provide your{" "}
                    <Tooltip
                      message="The SSH Private Key File is used to connect to the target hosts in your cluster to install the Ambari Agent."
                      placement="right"
                      heading="SSH Private Key"
                    >
                      <span className="custom-link">SSH private key</span>
                    </Tooltip>{" "}
                    to automatically register hosts
                  </Form.Label>
                </div>
                <div className="d-flex make-all-grey">
                  <Form.Check
                    type="radio"
                    id="manual-registration-radio"
                    checked={!isSshRegistration}
                    onChange={() =>
                      setValue("isSshRegistration", !isSshRegistration)
                    }
                    className="custom-checkbox"
                  />
                  <Form.Label htmlFor="manual-registration-radio" className="pt-1 ps-2">
                    Perform{" "}
                    <Tooltip
                      message="Manually registering the Ambari Agent on each host eliminates the need for SSH and should be performed prior to continuing cluster installation."
                      placement="right"
                      heading="Manual Registration"
                    >
                      <span className="custom-link">manual registration</span>
                    </Tooltip>{" "}
                    on hosts and do not use SSH
                  </Form.Label>
                </div>
              </div>
              <div className="d-flex mb-2">
                <DefaultButton
                  onClick={handleChooseFileClick}
                  disabled={!isSshRegistration}
                >
                  CHOOSE FILE
                </DefaultButton>
                <Form.Control
                  type="file"
                  ref={sshFileInputField}
                  className="d-none"
                  onChange={handleFileChange}
                  disabled={!isSshRegistration}
                />
                <p className="ms-4 pt-2">
                  {sshFile ? sshFile.name : "No file selected"}
                </p>
              </div>
              <div className="d-flex">
                <Form.Control
                  {...register("sshKey", {
                    required: isSshRegistration
                      ? "SSH Private Key is required"
                      : false,
                  })}
                  as="textarea"
                  rows={4}
                  className={
                    errors.sshKey
                      ? "w-75 code-textarea mb-3 border-danger"
                      : "w-75 code-textarea mb-3"
                  }
                  placeholder="ssh private key"
                  disabled={!isSshRegistration}
                />
                {errors.sshKey && (
                  <div className="text-danger mt-3 ms-3">
                    <FontAwesomeIcon icon={faCircleXmark} />{" "}
                    {errors.sshKey.message}
                  </div>
                )}
              </div>
              <div className="d-flex w-100 mb-3">
                <div className="d-flex w-75 justify-content-between">
                  <Form.Label className="pt-2">
                    <Tooltip
                      message="The user account used to install the Ambari Agent on the target host(s) via SSH. This user must be set up with passwordless SSH and sudo access on all the target host(s)"
                      placement="right"
                    >
                      <span className="custom-link">SSH User Account</span>
                    </Tooltip>
                  </Form.Label>
                  <Form.Control
                    {...register("sshUserAccount", {
                      required: isSshRegistration
                        ? "User name is required"
                        : false,
                    })}
                    type="text"
                    className={
                      errors.sshUserAccount ? "w-50 border-danger" : "w-50"
                    }
                    disabled={!isSshRegistration}
                  />
                </div>
                <div>
                  {errors.sshUserAccount && (
                    <div className="text-danger mt-3 ms-3">
                      <FontAwesomeIcon icon={faCircleXmark} />{" "}
                      {errors.sshUserAccount.message}
                    </div>
                  )}
                </div>
              </div>
              <div className="d-flex w-100">
                <div className="d-flex justify-content-between w-75">
                  <Form.Label className="pt-2">
                    <Tooltip message="SSH Port Number" placement="right">
                      <span className="custom-link">SSH Port Number</span>
                    </Tooltip>
                  </Form.Label>
                  <Form.Control
                    {...register("sshPortNumber", {
                      required: isSshRegistration
                        ? "SSH Port Number is required"
                        : false,
                    })}
                    type="text"
                    className={
                      errors.sshPortNumber ? "w-50 border-danger" : "w-50"
                    }
                    disabled={!isSshRegistration}
                  />
                </div>
                <div>
                  {errors.sshPortNumber && (
                    <div className="text-danger mt-3 ms-3">
                      <FontAwesomeIcon icon={faCircleXmark} />{" "}
                      {errors.sshPortNumber.message}
                    </div>
                  )}
                </div>
              </div>
            </div>
            <div className="mt-4">
              <Button
                variant="success"
                type="submit"
                ref={registerRef}
                className="visually-hidden"
              >
                REGISTER AND CONFIRM
              </Button>
            </div>
          </Form>
        </Card>
      </div>
      <WizardFooter
        isNextEnabled={nextEnabled}
        step={currentStep}
        onCancel={() => {
          flushStateToDb("cancel");
        }}
        onNext={() => {
          //@ts-ignore
          registerRef?.current?.click();
        }}
        onBack={() => {
          handleBackImperitive();
          flushStateToDb("back");
        }}
      />
    </>
  );
}
