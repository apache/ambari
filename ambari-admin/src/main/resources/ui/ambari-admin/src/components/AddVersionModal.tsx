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
/* eslint-disable @typescript-eslint/ban-ts-comment */
/* eslint-disable @typescript-eslint/ban-types */
import { useState } from "react";
import { Button, Form, FormControl, Modal } from "react-bootstrap";
import DefaultButton from "./DefaultButton";
import { ReadOptions } from "../constants";
import VersionsApi from "../api/versions";
import toast from "react-hot-toast";
import { get } from "lodash";

type ModalProps = {
  isOpen: boolean;
  onClose: () => void;
  onReadVersion: Function;
};

const AddVersionModal = ({ isOpen, onClose, onReadVersion }: ModalProps) => {
  const [uploadOption, setUploadOption] = useState(ReadOptions.FILE);
  const [file, setFile] = useState();
  const [fileUrl, setFileUrl] = useState("");
  const readVersionInfo = async () => {
    try {
      if (uploadOption === ReadOptions.FILE && file) {
        const reader = new FileReader();
        reader.onload = async function (event) {
          const fileContents = get(event, "target.result", undefined);
          const versionResources = await VersionsApi.readVersionInfo(
            fileContents,
            {
              "Content-Type": "text/xml",
            }
          );
          onReadVersion(versionResources, fileContents, null);
        };
        reader.readAsText(file);
      } else if (uploadOption === ReadOptions.URL && fileUrl) {
        // Fetch the content from the URL
        const versionResources = await VersionsApi.readVersionInfo({
          VersionDefinition: {
            version_url: fileUrl,
          },
        });
        onReadVersion(versionResources, null, fileUrl);
      }
    } catch (err) {
      toast.error("Could not read version defintion");
    }
  };
  const handleClose = () => {
    setFile(undefined);
    setFileUrl("");
    onClose();
  };
  return (
    <Modal show={isOpen} onHide={onClose} data-testid="add-version-modal">
      <Modal.Header>
        <Modal.Title>Add Version</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Form.Check
          checked={uploadOption === ReadOptions.FILE}
          type="radio"
          id={ReadOptions.FILE}
          onChange={() => {
            setUploadOption(ReadOptions.FILE);
          }}
          label="Upload Version Definition File"
          className="px-0"
        />
        <FormControl
          type="file"
          className="py-1"
          onChange={() => {
            //@ts-ignore
            setFile((event?.target as HTMLInputElement)?.files?.[0]);
          }}
          disabled={uploadOption !== ReadOptions.FILE}
        />
        <Form.Check
          id={ReadOptions.URL}
          checked={uploadOption === ReadOptions.URL}
          onChange={() => {
            setUploadOption(ReadOptions.URL);
          }}
          type="radio"
          label="Version Definition File URL"
          className="px-0 mt-3"
        />
        <FormControl
          type="text"
          className="py-1"
          disabled={uploadOption !== ReadOptions.URL}
          placeholder="Enter URL to Version Definition File"
          value={fileUrl}
          onChange={(e) => {
            setFileUrl(e.target.value);
          }}
        />
      </Modal.Body>
      <Modal.Footer>
        <DefaultButton size="sm" onClick={handleClose}>
          CANCEL
        </DefaultButton>
        <Button
          variant="success"
          size="sm"
          onClick={readVersionInfo}
          disabled={!file && !fileUrl}
        >
          READ VERSION INFO
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default AddVersionModal;
