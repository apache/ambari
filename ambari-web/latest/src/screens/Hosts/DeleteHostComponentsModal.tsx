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

import { cloneDeep, get, set } from "lodash";
import { useEffect, useState } from "react";
import Modal from "../../components/Modal";

type DeleteHostComponentsModalProps = {
  isOpen: boolean;
  onClose: Function;
  hostsToDelete: any;
  hostsNotToDelete: any;
  successCallback: Function;
  modalMessages: any;
  okButtonText: string;
  okButtonVariant: string;
};

export default function DeleteHostComponentsModal({
  isOpen,
  onClose,
  hostsToDelete,
  hostsNotToDelete,
  successCallback,
  modalMessages,
  okButtonText,
  okButtonVariant,
}: DeleteHostComponentsModalProps) {
  const [skippedHosts, setSkippedHosts] = useState(hostsNotToDelete);

  useEffect(() => {
    setSkippedHosts(hostsNotToDelete);
  }, [hostsNotToDelete]);

  const toggleHost = (hostName: string) => {
    const skippedHostsCopy = cloneDeep(skippedHosts);
    skippedHostsCopy.forEach((host: any) => {
      if (get(host, "error.key") == hostName) {
        set(host, "isCollapsed", !get(host, "isCollapsed"));
      }
    });
    setSkippedHosts(skippedHostsCopy);
  };

  const getModalBody = () => {
    const modifyMessage = get(modalMessages, "bodyModifyMessage", "");
    const skipMessage = get(modalMessages, "bodySkipMessage", "");
    const hostsToModify = hostsToDelete.length ? hostsToDelete.join("\n") : "";
    return (
      <div>
        {hostsToModify ? (
          <div>
            <div>{modifyMessage}</div>
            <div>
              {hostsToModify}
              {"\n\n"}
            </div>
          </div>
        ) : null}
        {skippedHosts.length ? (
          <div>
            <div>{skipMessage}</div>
            <div>
              {skippedHosts.map((host: any) => {
                return (
                  <div>
                    <div
                      onClick={() => {
                        toggleHost(get(host, "error.key"));
                      }}
                      className="custom-link"
                    >
                      {get(host, "error.key")}
                    </div>
                    {!get(host, "isCollapsed", true) ? (
                      <div className="ps-4 mb-2">
                        {get(host, "error.message", "")}
                      </div>
                    ) : null}
                  </div>
                );
              })}
            </div>
          </div>
        ) : null}
      </div>
    );
  };

  const modalProps = {
    isOpen: isOpen,
    modalTitle: get(modalMessages, "header", ""),
    modalBody: getModalBody(),
    onClose: () => {
      onClose();
    },
    successCallback: () => {
      successCallback();
    },
    options: {
      buttonSize: "sm" as "sm" | "lg" | undefined,
      cancelableViaIcon: true,
      cancelableViaBtn: true,
      okButtonText: okButtonText,
      okButtonVariant: okButtonVariant,
      okButtonDisabled: !hostsToDelete.length,
    },
  };

  return isOpen ? <Modal {...modalProps} /> : <></>;
}
