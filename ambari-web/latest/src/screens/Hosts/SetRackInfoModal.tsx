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

import { useEffect, useState } from "react";
import { Form } from "react-bootstrap";
import Modal from "../../components/Modal";
import { HostsApi } from "../../api/hostsApi";
import { cloneDeep, set } from "lodash";
import { useModal } from "../../store/ModalContext";
import { configValidator } from "../../Utils/validators";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCircleXmark } from "@fortawesome/free-solid-svg-icons";
import { IHost } from "../../models/host";

type SetRackInfoModalProps = {
  clusterName: string;
  data: any;
  callback: Function;
  hostNames?: string[];
};

export default function SetRackInfoModal({
  clusterName,
  data,
  callback,
  hostNames = [],
}: SetRackInfoModalProps) {
  const [currentRack, setCurrentRack] = useState("");
  const [isValid, setIsValid] = useState(true);
  const { hideModal } = useModal();

  useEffect(() => {
    setCurrentRack(data.Body.Hosts.rack_info);
  }, [data]);

  useEffect(() => {
    configValidator.isValidRackId(currentRack)
      ? setIsValid(true)
      : setIsValid(false);
  }, [currentRack]);

  const getSetRackModalBody = () => {
    return (
      <div>
        <Form>
          <Form.Label className="me-2">Rack:</Form.Label>
          <Form.Control
            value={currentRack}
            type="text"
            className={
              isValid
                ? "custom-form-control"
                : "custom-form-control border-danger"
            }
            onChange={(e) => {
              setCurrentRack(e.target.value);
            }}
          />
        </Form>
        {!isValid ? (
          <div className="text-danger mt-3">
            <FontAwesomeIcon icon={faCircleXmark} />
            {
              " Should start with a forward slash it may include alphanumeric chars, dots, dashes and forward slashes. Should be less than 255 symbols."
            }
          </div>
        ) : null}
      </div>
    );
  };

  const updateRack = async () => {
    set(data, "Body.Hosts.rack_info", currentRack);
    await HostsApi.updateHost(clusterName, data);
    callback((prevHosts: IHost[]) => {
      const prevHostsCopy = cloneDeep(prevHosts);
      prevHostsCopy.forEach((host: any) => {
        if(!hostNames.length || hostNames.includes(host.hostName)) {
          set(host, "rack", currentRack);
        }
      });
      return prevHostsCopy;
    });
  };

  return (
    <Modal
      isOpen={true}
      onClose={() => {
        hideModal();
      }}
      modalTitle="Set Rack"
      modalBody={getSetRackModalBody()}
      successCallback={() => {
        if (isValid) {
          updateRack();
          hideModal();
        }
      }}
      options={{
        okButtonDisabled: !isValid,
        modalSize: "modal-sm",
        cancelableViaIcon: true,
        cancelableViaBtn: true,
        okButtonVariant: "primary",
      }}
    />
  );
}
