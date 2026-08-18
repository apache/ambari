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

import { useContext, useEffect, useRef, useState } from "react";
import { Alert, Button, Col, Container, Form, Row, Spinner } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faFileCode, faGlobe, faLayerGroup, faPlug } from "@fortawesome/free-solid-svg-icons";
import { useNavigate, useParams } from "react-router-dom";
import { AlertsApi } from "../../api/alertsApi";
import {
  buildAlertCreationPayload,
  INITIAL_ALERT_CREATION_FORM,
  validateAlertCreation,
  type AlertCreationForm,
  type CreatableAlertType,
} from "../../Utils/alertCreation";
import { AppContext } from "../../store/context";

const TYPE_OPTIONS: Array<{
  type: CreatableAlertType;
  label: string;
  icon: typeof faPlug;
}> = [
  { type: "PORT", label: "Port", icon: faPlug },
  { type: "WEB", label: "Web", icon: faGlobe },
  { type: "SCRIPT", label: "Script", icon: faFileCode },
  { type: "AGGREGATE", label: "Aggregate", icon: faLayerGroup },
];

function parseStep(value: string | undefined): number {
  const step = Number((value || "1").replace(/^step/, ""));
  return step >= 1 && step <= 3 ? step : 1;
}

const AlertDefinitionWizard = () => {
  const { clusterName, services, serviceComponentInfo } = useContext(AppContext);
  const { stepNumber } = useParams<{ stepNumber: string }>();
  const navigate = useNavigate();
  const step = parseStep(stepNumber);
  const [form, setForm] = useState<AlertCreationForm>({ ...INITIAL_ALERT_CREATION_FORM });
  const [aggregateDefinitions, setAggregateDefinitions] = useState<string[]>([]);
  const [errors, setErrors] = useState<string[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const submitInFlight = useRef(false);

  const installedServices: string[] = services
    .map((service: any) => service?.ServiceInfo?.service_name)
    .filter((name: unknown): name is string => typeof name === "string")
    .sort();
  const serviceNames: string[] = [...new Set([...installedServices, "CUSTOM"] as string[])];
  const stackService = serviceComponentInfo?.items?.find(
    (service: any) => service?.StackServices?.service_name === form.serviceName,
  );
  const components: string[] = (stackService?.components || [])
    .map((component: any) => component?.StackServiceComponents?.component_name)
    .filter((name: unknown): name is string => typeof name === "string")
    .sort();

  useEffect(() => {
    if (step > 1 && !form.type) {
      navigate("/main/alerts/add/1", { replace: true });
    } else if (step === 3 && validateAlertCreation(form).length > 0) {
      navigate("/main/alerts/add/2", { replace: true });
    }
  }, [form, navigate, step]);

  useEffect(() => {
    if (!clusterName) return;
    AlertsApi.getAlertDefinition(clusterName, "AlertDefinition/name,AlertDefinition/source", Date.now())
      .then((response) => {
        setAggregateDefinitions((response.items || [])
          .map((item: any) => item?.AlertDefinition?.name)
          .filter((name: unknown): name is string => typeof name === "string")
          .sort());
      })
      .catch(() => setAggregateDefinitions([]));
  }, [clusterName]);

  const update = <K extends keyof AlertCreationForm>(key: K, value: AlertCreationForm[K]) => {
    setForm((current) => ({ ...current, [key]: value }));
    setErrors([]);
  };

  const chooseType = (type: CreatableAlertType) => {
    setForm((current) => ({ ...current, type }));
    navigate("/main/alerts/add/2");
  };

  const continueToReview = () => {
    const validationErrors = validateAlertCreation(form);
    setErrors(validationErrors);
    if (validationErrors.length === 0) navigate("/main/alerts/add/3");
  };

  const submit = async () => {
    if (submitInFlight.current) return;
    const validationErrors = validateAlertCreation(form);
    setErrors(validationErrors);
    if (validationErrors.length > 0) return;
    if (!clusterName) {
      setErrors(["Cluster context is unavailable. Return to Alerts and retry."]);
      return;
    }
    submitInFlight.current = true;
    setIsSubmitting(true);
    try {
      await AlertsApi.createAlertDefinition(clusterName, buildAlertCreationPayload(form));
      navigate("/main/alerts");
    } catch {
      setErrors(["Ambari could not create the Alert Definition. Review the values and retry."]);
    } finally {
      submitInFlight.current = false;
      setIsSubmitting(false);
    }
  };

  const commonFields = (
    <>
      <Row className="g-3">
        <Col md={6}>
          <Form.Group controlId="alert-name">
            <Form.Label>Alert Name</Form.Label>
            <Form.Control value={form.label} maxLength={255} onChange={(event) => update("label", event.target.value)} />
          </Form.Group>
        </Col>
        <Col md={6}>
          <Form.Group controlId="alert-service">
            <Form.Label>Service</Form.Label>
            <Form.Select value={form.serviceName} onChange={(event) => {
              update("serviceName", event.target.value);
              update("componentName", "");
            }}>
              <option value="">Select service</option>
              {serviceNames.map((name) => <option key={name} value={name}>{name}</option>)}
            </Form.Select>
          </Form.Group>
        </Col>
        <Col md={6}>
          <Form.Group controlId="alert-component">
            <Form.Label>Component</Form.Label>
            <Form.Select
              value={form.componentName}
              disabled={form.serviceName === "CUSTOM"}
              onChange={(event) => update("componentName", event.target.value)}
            >
              <option value="">No component</option>
              {components.map((name) => <option key={name} value={name}>{name}</option>)}
            </Form.Select>
          </Form.Group>
        </Col>
        <Col md={3}>
          <Form.Group controlId="alert-scope">
            <Form.Label>Scope</Form.Label>
            <Form.Select value={form.scope} onChange={(event) => update("scope", event.target.value as AlertCreationForm["scope"])}>
              <option value="ANY">Any</option>
              <option value="HOST">Host</option>
              <option value="SERVICE">Service</option>
            </Form.Select>
          </Form.Group>
        </Col>
        <Col md={3}>
          <Form.Group controlId="alert-interval">
            <Form.Label>Check Interval</Form.Label>
            <Form.Control type="number" min={1} value={form.interval} onChange={(event) => update("interval", event.target.value)} />
          </Form.Group>
        </Col>
        <Col xs={12}>
          <Form.Group controlId="alert-description">
            <Form.Label>Description</Form.Label>
            <Form.Control as="textarea" rows={3} value={form.description} onChange={(event) => update("description", event.target.value)} />
          </Form.Group>
        </Col>
      </Row>
    </>
  );

  return (
    <Container className="p-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="mb-1">Create Alert Definition</h2>
          <div className="text-muted">Step {step} of 3</div>
        </div>
        <Button variant="outline-secondary" onClick={() => navigate("/main/alerts")}>Cancel</Button>
      </div>

      {errors.length > 0 && (
        <Alert variant="danger">
          {errors.map((error) => <div key={error}>{error}</div>)}
        </Alert>
      )}

      {step === 1 && (
        <Row className="g-3" aria-label="Alert type">
          {TYPE_OPTIONS.map((option) => (
            <Col md={6} key={option.type}>
              <Button
                variant="outline-secondary"
                className="w-100 d-flex align-items-center justify-content-start gap-3 p-4 text-start"
                onClick={() => chooseType(option.type)}
              >
                <FontAwesomeIcon icon={option.icon} size="2x" />
                <strong>{option.label}</strong>
              </Button>
            </Col>
          ))}
        </Row>
      )}

      {step === 2 && form.type && (
        <Form>
          {commonFields}
          <hr className="my-4" />
          <Row className="g-3">
            {(form.type === "PORT" || form.type === "WEB") && (
              <>
                <Col md={8}>
                  <Form.Group controlId="alert-uri">
                    <Form.Label>{form.type === "PORT" ? "Host or URI" : "HTTP URI"}</Form.Label>
                    <Form.Control value={form.uri} onChange={(event) => update("uri", event.target.value)} />
                  </Form.Group>
                </Col>
                <Col md={4}>
                  <Form.Group controlId="alert-port">
                    <Form.Label>Default Port</Form.Label>
                    <Form.Control type="number" min={1} max={65535} value={form.defaultPort} onChange={(event) => update("defaultPort", event.target.value)} />
                  </Form.Group>
                </Col>
              </>
            )}
            {form.type === "WEB" && (
              <Col md={4}>
                <Form.Group controlId="alert-timeout">
                  <Form.Label>Connection Timeout</Form.Label>
                  <Form.Control type="number" min={0.1} step={0.1} value={form.connectionTimeout} onChange={(event) => update("connectionTimeout", event.target.value)} />
                </Form.Group>
              </Col>
            )}
            {form.type === "SCRIPT" && (
              <Col xs={12}>
                <Form.Group controlId="alert-script-path">
                  <Form.Label>Script Path</Form.Label>
                  <Form.Control value={form.scriptPath} onChange={(event) => update("scriptPath", event.target.value)} />
                </Form.Group>
              </Col>
            )}
            {form.type === "AGGREGATE" && (
              <Col xs={12}>
                <Form.Group controlId="alert-reference">
                  <Form.Label>Referenced Alert Name</Form.Label>
                  <Form.Control list="alert-definition-names" value={form.aggregateAlertName} onChange={(event) => update("aggregateAlertName", event.target.value)} />
                  <datalist id="alert-definition-names">
                    {aggregateDefinitions.map((name) => <option key={name} value={name} />)}
                  </datalist>
                </Form.Group>
              </Col>
            )}
            {(form.type === "PORT" || form.type === "AGGREGATE") && (
              <>
                <Col md={6}>
                  <Form.Group controlId="alert-warning-threshold">
                    <Form.Label>Warning Threshold</Form.Label>
                    <Form.Control type="number" min={0} step="any" value={form.warningThreshold} onChange={(event) => update("warningThreshold", event.target.value)} />
                  </Form.Group>
                </Col>
                <Col md={6}>
                  <Form.Group controlId="alert-critical-threshold">
                    <Form.Label>Critical Threshold</Form.Label>
                    <Form.Control type="number" min={0} step="any" value={form.criticalThreshold} onChange={(event) => update("criticalThreshold", event.target.value)} />
                  </Form.Group>
                </Col>
              </>
            )}
            {form.type !== "SCRIPT" && (
              <>
                <Col md={4}><Form.Group controlId="alert-ok-text"><Form.Label>OK Text</Form.Label><Form.Control value={form.okText} onChange={(event) => update("okText", event.target.value)} /></Form.Group></Col>
                <Col md={4}><Form.Group controlId="alert-warning-text"><Form.Label>Warning Text</Form.Label><Form.Control value={form.warningText} onChange={(event) => update("warningText", event.target.value)} /></Form.Group></Col>
                <Col md={4}><Form.Group controlId="alert-critical-text"><Form.Label>Critical Text</Form.Label><Form.Control value={form.criticalText} onChange={(event) => update("criticalText", event.target.value)} /></Form.Group></Col>
              </>
            )}
          </Row>
          <div className="d-flex justify-content-between mt-4">
            <Button variant="outline-secondary" onClick={() => navigate("/main/alerts/add/1")}>Back</Button>
            <Button variant="primary" onClick={continueToReview}>Next</Button>
          </div>
        </Form>
      )}

      {step === 3 && form.type && (
        <>
          <h3 className="h5 mb-3">Review</h3>
          <pre className="border bg-light p-3 overflow-auto">{JSON.stringify(buildAlertCreationPayload(form), null, 2)}</pre>
          <div className="d-flex justify-content-between mt-4">
            <Button variant="outline-secondary" disabled={isSubmitting} onClick={() => navigate("/main/alerts/add/2")}>Back</Button>
            <Button variant="primary" disabled={isSubmitting} onClick={submit}>
              {isSubmitting ? <><Spinner size="sm" className="me-2" />Creating</> : "Create"}
            </Button>
          </div>
        </>
      )}
    </Container>
  );
};

export default AlertDefinitionWizard;
