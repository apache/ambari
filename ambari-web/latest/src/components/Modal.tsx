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
import { Button, Modal as ReactModal } from "react-bootstrap";
import DefaultButton from "./DefaultButton";
import { ReactNode } from "react";
import classNames from "classnames";

export type ModalProps = {
  isOpen: boolean;
  onClose: () => void;
  modalTitle: any;
  modalBody: ReactNode;
  className?: string;
  successCallback: () => void;
  options: {
    modalSize?: string;
    shouldShowFooter?: boolean;
    buttonSize?: "sm" | "lg" | undefined;
    okButtonText?: any;
    cancelButtonText?: any;
    cancelableViaIcon?: boolean;
    cancelableViaBtn?: boolean;
    cancelableViaSuccessBtn?: boolean;
    okButtonVariant?: string;
    okButtonDisabled?: boolean;
    modalBodyClassName?: string;
    extraButtons?: {
      text: string;
      onClick: () => void;
      variant?: string;
      order?: number;
      [key: string]: any;
    }[];
  };
};

export default function Modal({
  isOpen,
  onClose,
  modalTitle,
  modalBody,
  successCallback,
  options,
  className,
}: ModalProps) {
  const {
    modalSize = "modal-width", //Other options are modal-sm, modal-md, modal-lg - Change via prop
    shouldShowFooter = true,
    buttonSize = undefined, //Other options are sm, lg - Change via prop (undefined or any other string will be considered as default size i.e. medium)
    okButtonText = "OK",
    cancelButtonText = "CANCEL",
    cancelableViaIcon = false,
    cancelableViaBtn = true,
    okButtonVariant = "success",
    cancelableViaSuccessBtn = true,
    okButtonDisabled = false,
    modalBodyClassName = "",
    extraButtons = [],
  } = options;

  const sortedExtraButtons = Array.isArray(extraButtons)
    ? extraButtons.sort((a: any, b: any) => (a.order || 0) - (b.order || 0))
    : [];

  return (
    <ReactModal
      show={isOpen}
      onHide={onClose}
      className={classNames(
        "custom-modal-container",
        "make-scrollable",
        "custom-scrollbar",
        className,
        modalSize
      )}
      data-testid="confirmation-modal"
    >
      <ReactModal.Header closeButton={cancelableViaIcon}>
        <ReactModal.Title>
          <h2>{modalTitle}</h2>
        </ReactModal.Title>
      </ReactModal.Header>
      <ReactModal.Body className={classNames(modalBodyClassName)}>
        <div className="pre-wrap">{modalBody}</div>
      </ReactModal.Body>
      {!shouldShowFooter ? null : (
        <ReactModal.Footer className="d-flex justify-content-end">
          {cancelableViaBtn ? (
            <DefaultButton
              size={buttonSize}
              className="ps-3 pe-3 text-white"
              onClick={onClose}
              data-testid="confirm-cancel-btn"
            >
              {cancelButtonText}
            </DefaultButton>
          ) : null}
          {sortedExtraButtons.map((button: any) => (
            <Button
              key={button.text}
              className={classNames(
                "ps-3",
                "pe-3",
                "text-white",
                "custom-btn",
                button.className
              )}
              variant={button.variant}
              onClick={button.onClick}
              size={buttonSize}
              {...button}
            >
              {button.text}
            </Button>
          ))}
          {cancelableViaSuccessBtn ? (
            <Button
              className={classNames(
                "ps-3",
                "pe-3",
                "text-white",
                "custom-btn",
                { "disabled-btn": okButtonDisabled }
              )}
              variant={okButtonVariant}
              onClick={successCallback}
              size={buttonSize}
              data-testid="confirm-ok-btn"
              disabled={okButtonDisabled}
            >
              {okButtonText}
            </Button>
          ) : null}
        </ReactModal.Footer>
      )}
    </ReactModal>
  );
}
