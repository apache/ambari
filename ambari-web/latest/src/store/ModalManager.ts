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

import { ReactNode } from "react";
import { ModalProps } from "../components/Modal";

export type CustomModalProps = Omit<ModalProps, "isOpen"> | ReactNode;

class ModalManager {
  private listeners: ((props: CustomModalProps[]) => void)[] = [];
  private modalStack: CustomModalProps[] = [];

  show(props: CustomModalProps) {
    this.modalStack.push(props);
    this.notify();
  }

  hide() {
    const modal = this.modalStack.pop();
    if (this.isModalProps(modal) && modal.onClose) {
      modal.onClose();
    }
    this.notify();
  }

  subscribe(listener: (props: CustomModalProps[]) => void) {
    this.listeners.push(listener);
    return () => {
      this.listeners = this.listeners.filter((l) => l !== listener);
    };
  }

  private notify() {
    this.listeners.forEach((listener) => listener([...this.modalStack]));
  }

  private isModalProps(
    modal: CustomModalProps | undefined
  ): modal is Omit<ModalProps, "isOpen"> {
    return (modal as Omit<ModalProps, "isOpen">)?.onClose !== undefined;
  }
}

const modalManager = new ModalManager();
export default modalManager;