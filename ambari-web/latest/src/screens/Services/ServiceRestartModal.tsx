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
import { Alert, ButtonGroup, Form, ToggleButton } from "react-bootstrap";
import Modal from "../../components/Modal";
import type {
  ServiceRestartMode,
  ServiceRestartScope,
} from "./serviceRestartUtils";

type ServiceRestartConfig = {
  mode: ServiceRestartMode;
  batchSize: number;
  intervalTimeSeconds: number;
  tolerateSize: number;
};

type ServiceRestartModalProps = {
  isOpen: boolean;
  serviceDisplayName: string;
  scope: ServiceRestartScope;
  componentCount: number;
  blocked: boolean;
  isSubmitting: boolean;
  errorMessage: string;
  onClose: () => void;
  onSubmit: (config: ServiceRestartConfig) => void;
};

const SCOPE_LABELS: Record<ServiceRestartScope, string> = {
  ALL: "all master and slave components",
  MASTERS: "master components",
  SLAVES: "slave components",
};

export default function ServiceRestartModal({
  isOpen,
  serviceDisplayName,
  scope,
  componentCount,
  blocked,
  isSubmitting,
  errorMessage,
  onClose,
  onSubmit,
}: ServiceRestartModalProps) {
  const [mode, setMode] = useState<ServiceRestartMode>("ROLLING");
  const [batchSize, setBatchSize] = useState("10");
  const [intervalTimeSeconds, setIntervalTimeSeconds] = useState("120");
  const [tolerateSize, setTolerateSize] = useState("0");

  useEffect(() => {
    if (isOpen) {
      setMode("ROLLING");
      setBatchSize("10");
      setIntervalTimeSeconds("120");
      setTolerateSize("0");
    }
  }, [isOpen, scope]);

  const parsedBatchSize = Number(batchSize);
  const parsedInterval = Number(intervalTimeSeconds);
  const parsedTolerance = Number(tolerateSize);
  const errors: string[] = [];
  if (componentCount === 0) {
    errors.push(`No ${SCOPE_LABELS[scope]} are available to restart.`);
  }
  if (mode === "ROLLING") {
    if (
      !batchSize.trim()
      || !Number.isInteger(parsedBatchSize)
      || parsedBatchSize < 1
    ) {
      errors.push("Batch size must be a whole number greater than zero.");
    }
    if (
      !intervalTimeSeconds.trim()
      || !Number.isInteger(parsedInterval)
      || parsedInterval < 0
    ) {
      errors.push("Interval between batches must be a non-negative whole number.");
    }
    if (
      !tolerateSize.trim()
      || !Number.isInteger(parsedTolerance)
      || parsedTolerance < 0
    ) {
      errors.push("Failure tolerance must be a non-negative whole number.");
    }
  }

  const submit = () => {
    if (blocked || isSubmitting || errors.length) {
      return;
    }
    onSubmit({
      mode,
      batchSize: parsedBatchSize,
      intervalTimeSeconds: parsedInterval,
      tolerateSize: parsedTolerance,
    });
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={isSubmitting ? () => undefined : onClose}
      modalTitle={`Configure Restart ${serviceDisplayName}`}
      modalBody={(
        <>
          <Alert variant="info">
            Restart {componentCount} {SCOPE_LABELS[scope]} for {serviceDisplayName}.
          </Alert>
          <Form>
            <Form.Group className="mb-3">
              <Form.Label>Restart type</Form.Label>
              <div>
                <ButtonGroup aria-label="Restart type">
                  {(["ROLLING", "EXPRESS"] as ServiceRestartMode[]).map((value) => (
                    <ToggleButton
                      key={value}
                      id={`service-restart-${value.toLowerCase()}`}
                      type="radio"
                      variant="outline-primary"
                      name="service-restart-mode"
                      value={value}
                      checked={mode === value}
                      onChange={() => setMode(value)}
                      disabled={isSubmitting}
                    >
                      {value === "ROLLING" ? "Rolling" : "Express"}
                    </ToggleButton>
                  ))}
                </ButtonGroup>
              </div>
            </Form.Group>
            {mode === "ROLLING" ? (
              <>
                <Form.Group className="mb-3" controlId="service-restart-batch-size">
                  <Form.Label>Slave hosts per batch</Form.Label>
                  <Form.Control
                    type="number"
                    min="1"
                    value={batchSize}
                    onChange={(event) => setBatchSize(event.target.value)}
                    disabled={isSubmitting}
                  />
                </Form.Group>
                <Form.Group className="mb-3" controlId="service-restart-interval">
                  <Form.Label>Interval between batches (seconds)</Form.Label>
                  <Form.Control
                    type="number"
                    min="0"
                    value={intervalTimeSeconds}
                    onChange={(event) => setIntervalTimeSeconds(event.target.value)}
                    disabled={isSubmitting}
                  />
                </Form.Group>
                <Form.Group className="mb-3" controlId="service-restart-tolerance">
                  <Form.Label>Task failure tolerance</Form.Label>
                  <Form.Control
                    type="number"
                    min="0"
                    value={tolerateSize}
                    onChange={(event) => setTolerateSize(event.target.value)}
                    disabled={isSubmitting}
                  />
                </Form.Group>
              </>
            ) : null}
          </Form>
          {errors.length ? (
            <Alert variant="danger">
              {errors.map((error) => <div key={error}>{error}</div>)}
            </Alert>
          ) : null}
          {errorMessage ? <Alert variant="danger">{errorMessage}</Alert> : null}
        </>
      )}
      successCallback={submit}
      options={{
        modalSize: "modal-lg",
        buttonSize: "sm",
        cancelableViaIcon: true,
        cancelableViaBtn: true,
        okButtonText: isSubmitting ? "SUBMITTING..." : "RESTART",
        okButtonVariant: "warning",
        okButtonDisabled: blocked || isSubmitting || errors.length > 0,
      }}
    />
  );
}
