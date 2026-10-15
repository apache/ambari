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

import { map } from "lodash";
import { useEffect, useState } from "react";
import { Alert } from "react-bootstrap";
import Modal from "../../components/Modal";
import { translate } from "../../Utils/Utility";

export type RecoverHostComponent = {
  HostRoles: { component_name: string; state: string };
};

export type RecoverHost = {
  hostName: string;
  allHostComponents: RecoverHostComponent[];
};

export type SkippedRecoverHost = {
  hostName: string;
  reason: string;
};

type RecoverHostsModalProps = {
  isOpen: boolean;
  onClose: () => void;
  eligibleHosts: Pick<RecoverHost, "hostName">[];
  skippedHosts: SkippedRecoverHost[];
  successCallback: () => void;
};

export default function RecoverHostsModal({
  isOpen,
  onClose,
  eligibleHosts,
  skippedHosts,
  successCallback,
}: RecoverHostsModalProps) {
  const [expandedHosts, setExpandedHosts] = useState<Record<string, boolean>>(
    {}
  );
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    setExpandedHosts({});
    setIsSubmitting(false);
  }, [skippedHosts]);

  const toggleHost = (hostName: string) => {
    setExpandedHosts((current) => ({
      ...current,
      [hostName]: !current[hostName],
    }));
  };

  const getModalBody = () => {
    return (
      <div>
        <div className="fs-12 mb-2">
          {translate("hosts.bulkOperation.recover.confirmation.body")}
        </div>
        <div className="scrollable-h15 border rounded-1 p-1">
          {map(eligibleHosts, (host) => (
            <div key={host.hostName} className="fs-12 p-1">
              {host.hostName}
            </div>
          ))}
        </div>
        {skippedHosts.length ? (
          <Alert variant="warning mt-2">
            <div className="fs-12 mb-2">
              {translate("hosts.bulkOperation.recover.confirmation.skip.body")}
            </div>
            <div className="scrollable-h15 border rounded-1 p-1">
              {map(skippedHosts, (skippedHost: SkippedRecoverHost) => (
                <div key={skippedHost.hostName} className="fs-12 p-1">
                  <div
                    onClick={() => toggleHost(skippedHost.hostName)}
                    className="custom-link"
                  >
                    {skippedHost.hostName}
                  </div>
                  {expandedHosts[skippedHost.hostName] ? (
                    <div className="ps-4 mb-2">{skippedHost.reason}</div>
                  ) : null}
                </div>
              ))}
            </div>
          </Alert>
        ) : null}
      </div>
    );
  };

  const modalProps = {
    isOpen: isOpen,
    modalTitle: translate("hosts.bulkOperation.recover.confirmation.header"),
    modalBody: getModalBody(),
    onClose: () => {
      onClose();
    },
    successCallback: () => {
      if (isSubmitting) {
        return;
      }
      setIsSubmitting(true);
      successCallback();
    },
    options: {
      buttonSize: "sm" as "sm" | "lg" | undefined,
      cancelableViaIcon: true,
      cancelableViaBtn: true,
      okButtonVariant: "warning",
      okButtonDisabled: !eligibleHosts.length || isSubmitting,
    },
  };

  return isOpen ? <Modal {...modalProps} /> : <></>;
}
