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

import React, { forwardRef, useEffect, useImperativeHandle, useState } from 'react';
import { useParams } from 'react-router-dom';
import { AlertsApi } from '../../api/alertsApi';
import { Form, Button, Row, Col, InputGroup } from 'react-bootstrap';
import "../../styles/app.scss"
import useAuthorizationPolicy from '../../hooks/useAuthorizationPolicy';
import {
    buildAlertDefinitionUpdate,
    validateAlertDefinitionConfiguration,
} from '../../Utils/alertDefinitions';
import { AlertEditorHandle } from './types';

interface AlertConfigSectionProps {
    clusterName: string;
    onDirtyChange?: (dirty: boolean) => void;
}

interface AlertParameter {
    name: string;
    display_name: string;
    value: string | number;
    type: string;
    units?: string;
    threshold?: string;
    visibility?: string;
    description?: string;
}

interface AlertReportingItem {
    value?: number;
    text: string;
}

interface ReportingType {
    [key: string]: AlertReportingItem | string | undefined;
    type?: string;
    units?: string;
}

interface AlertSourceConfig {
    [key: string]: unknown;
    parameters?: AlertParameter[];
    reporting?: ReportingType;
    path?: string;
    type?: string;
    default_port?: number;
    uri?: string;
}

interface AlertDefinitionConfig {
    id: number;
    description: string;
    interval: number;
    source?: AlertSourceConfig;
    [key: string]: any;
}

const AlertConfigSection = forwardRef<AlertEditorHandle, AlertConfigSectionProps>(({
    clusterName,
    onDirtyChange,
}, ref) => {
    const { alertId } = useParams<{ alertId: string }>();
    const [canEdit, setCanEdit] = useState(false);
    const [configurations, setConfigurations] = useState<AlertDefinitionConfig>({
        id: 0,
        description: '',
        interval: 0
    });
    const [originalConfigurations, setOriginalConfigurations] = useState<AlertDefinitionConfig>({
        id: 0,
        description: '',
        interval: 0
    });
    const [loading, setLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');
    const [retryTrigger, setRetryTrigger] = useState(0);
    
    // Authorization hooks - implementing Ember.js alert authorization patterns
    const { isAuthorized } = useAuthorizationPolicy();
    
    // Check specific authorizations for alert operations
    const canToggleAlerts = isAuthorized('SERVICE.TOGGLE_ALERTS');

    useEffect(() => {
        const fetchAlertDefinition = async () => {
            setLoading(true);
            setErrorMessage('');
            try {
                const data = await AlertsApi.getAlertDefinitionById(clusterName, alertId || '', Date.now());
                const definition = data?.AlertDefinition || data?.items?.[0]?.AlertDefinition;
                if (!definition) throw new Error('Alert definition not found');
                setConfigurations(definition);
                setOriginalConfigurations(JSON.parse(JSON.stringify(definition)));
            } catch (error) {
                console.error('Error fetching alert definition:', error);
                setErrorMessage('Failed to load alert configuration.');
            } finally {
                setLoading(false);
            }
        };

        if (clusterName && alertId) {
            fetchAlertDefinition();
        }
    }, [clusterName, alertId, retryTrigger]);

    const handleEditConfigs = () => {
        setCanEdit(true);
        setErrorMessage('');
        onDirtyChange?.(true);
    };

    const handleCancelEditConfigs = () => {
        // Reset to original values
        setConfigurations(JSON.parse(JSON.stringify(originalConfigurations)));
        setCanEdit(false);
        setErrorMessage('');
        onDirtyChange?.(false);
    };

    const validationErrors = validateAlertDefinitionConfiguration(configurations);

    const handleSaveConfigs = async (): Promise<boolean> => {
        if (validationErrors.length > 0) {
            setErrorMessage(validationErrors.join(' '));
            return false;
        }
        
        setIsSaving(true);
        
        try {
            const payload = buildAlertDefinitionUpdate(configurations, originalConfigurations);
            
            // Only make the API call if there are changes
            if (Object.keys(payload).length > 0) {
                await AlertsApi.updateAlertDefinition(
                    clusterName,
                    configurations.id,
                    payload
                );
                
                // Update the original configurations to match the current ones
                setOriginalConfigurations(JSON.parse(JSON.stringify(configurations)));
                setCanEdit(false);
            } else {
                // No changes were made
                setCanEdit(false);
            }
            setErrorMessage('');
            onDirtyChange?.(false);
            return true;
        } catch (error) {
            console.error('Error saving configurations:', error);
            setErrorMessage('Error saving configurations. Please try again.');
            return false;
        } finally {
            setIsSaving(false);
        }
    };

    useImperativeHandle(ref, () => ({
        save: handleSaveConfigs,
        discard: handleCancelEditConfigs,
    }));

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>, key: string) => {
        const value = e.target.value;
        
        if (key === 'interval') {
            setConfigurations(prev => ({
                ...prev,
                interval: parseInt(value) || 0
            }));
        } else if (key === 'description') {
            setConfigurations(prev => ({
                ...prev,
                description: value
            }));
        } else if (configurations.source) {
            // Check if this is a parameter change
            const paramIndex = (configurations.source.parameters || []).findIndex(param => param.name === key);
            
            if (paramIndex !== -1) {
                // Handle parameter changes
                const updatedParams = [...(configurations.source.parameters || [])];
                const param = updatedParams[paramIndex];
                
                updatedParams[paramIndex] = {
                    ...param,
                    value: param.type === 'NUMERIC' || param.type === 'PERCENT'
                        ? (value === '' ? '' : Number(value))
                        : value
                };
                
                setConfigurations(prev => ({
                    ...prev,
                    source: {
                        ...prev.source!,
                        parameters: updatedParams
                    }
                }));
            } else if (key.includes('_value') && configurations.source.reporting) {
                // Handle reporting value changes
                const status = key.replace('_value', '');
                const currentReporting = { ...configurations.source.reporting };
                
                if (currentReporting[status] && typeof currentReporting[status] === 'object') {
                    const reportingItem = { 
                        ...(currentReporting[status] as AlertReportingItem),
                        value: value === '' ? 0 : Number(value)
                    };
                    
                    setConfigurations(prev => ({
                        ...prev,
                        source: {
                            ...prev.source!,
                            reporting: {
                                ...prev.source!.reporting!,
                                [status]: reportingItem
                            }
                        }
                    }));
                }
            } else if (key.includes('_text') && configurations.source.reporting) {
                // Handle reporting text changes
                const status = key.replace('_text', '');
                const currentReporting = { ...configurations.source.reporting };
                
                if (currentReporting[status]) {
                    const existingItem = currentReporting[status] as AlertReportingItem;
                    const reportingItem = { 
                        ...(typeof existingItem === 'object' ? existingItem : { value: undefined }),
                        text: value
                    };
                    
                    setConfigurations(prev => ({
                        ...prev,
                        source: {
                            ...prev.source!,
                            reporting: {
                                ...prev.source!.reporting!,
                                [status]: reportingItem
                            }
                        }
                    }));
                }
            }
        }
    };

    if (loading) {
        return <div>Loading...</div>;
    }

    if (!configurations.id && errorMessage) {
        return (
            <div className="col col-md-8">
                <div className="alert alert-danger">{errorMessage}</div>
                <Button variant="primary" onClick={() => setRetryTrigger((value) => value + 1)}>Retry</Button>
            </div>
        );
    }


    const fieldsToShow: Record<string, string> = {
        description: "Description",
        interval: "Check Interval"
    };

    return (
        <div className="col col-md-8 ">
            <div className="panel panel-default bg-white py-2 border">
                <div className="panel-heading">
                    <div className="row px-3">
                        <div className="col">
                            <h4 className="panel-title pt-3">Configuration</h4>
                        </div>
                        <div className="col">
                            {/* Only show EDIT button if user has SERVICE.TOGGLE_ALERTS permission and upgrade is not blocking */}
                            {!canEdit && canToggleAlerts && (
                                <Button
                                    variant="link"
                                    onClick={(e) => {
                                        e.preventDefault();
                                        handleEditConfigs();
                                    }}
                                    className="pull-right"
                                >
                                    EDIT
                                </Button>
                            )}
                        </div>
                    </div>
                </div>
                <hr/>
                <div className="panel-body px-3">
                    {errorMessage && (
                        <div className="alert alert-danger mb-3">
                            {errorMessage}
                        </div>
                    )}
                    
                    {Object.keys(fieldsToShow).map((key) => (
                        <Form.Group as={Row} className="my-3" key={key}>
                            <Form.Label column sm={2}>{fieldsToShow[key]}</Form.Label>
                            <Col sm={10}>
                                {fieldsToShow[key] === 'Check Interval' ? (
                                    <InputGroup className="mb-3">
                                        <Form.Control
                                            type="number"
                                            value={configurations[key]}
                                            onChange={(e) => handleChange(e, key)}
                                            aria-label="Check Interval"
                                            aria-describedby="basic-addon2"
                                            disabled={!canEdit || isSaving}
                                        />
                                        <InputGroup.Text id="basic-addon2">Minutes</InputGroup.Text>
                                    </InputGroup>
                                ) : (
                                    <Form.Control
                                        as="textarea"
                                        value={configurations[key]}
                                        onChange={(e) => handleChange(e, key)}
                                        aria-label={fieldsToShow[key]}
                                        aria-describedby="basic-addon1"
                                        disabled={!canEdit || isSaving}
                                    />
                                )}
                            </Col>
                        </Form.Group>
                    ))}

                    {configurations.source?.parameters?.map((param) => (
                        param.visibility !== 'HIDDEN' && (
                            <Form.Group as={Row} className="my-3" key={param.name}>
                                <Form.Label column sm={2}>{param.display_name}</Form.Label>
                                <Col sm={10}>
                                    <div className="d-flex align-items-center mb-3">
                                        {param.threshold && (
                                            <Button
                                                variant={
                                                    param.threshold === 'CRITICAL' ? 'danger' :
                                                        param.threshold === 'WARNING' ? 'warning' :
                                                            param.threshold === 'OK' ? 'success' :
                                                                'secondary'
                                                }
                                                className={`me-3 fw-bold border-0 ${param.threshold === 'CRITICAL' ? 'status-critical' :
                                                    param.threshold === 'WARNING' ? 'status-warning' :
                                                        param.threshold === 'OK' ? 'status-ok' :
                                                            ''}`}
                                                style={{ width: "120px", height: '40px', color: 'white' }}
                                                >
                                                {param.threshold}
                                            </Button>
                                        )}
                                        <InputGroup>
                                            <Form.Control
                                                type={param.type === 'NUMERIC' || param.type === 'PERCENT' ? 'number' : 'text'}
                                                value={param.value}
                                                onChange={(e) => handleChange(e, param.name)}
                                                aria-label={param.display_name}
                                                aria-describedby={`basic-addon-${param.name}`}
                                                disabled={!canEdit || isSaving || param.visibility === 'READ_ONLY'}
                                            />
                                            {param.units && (
                                                <InputGroup.Text id={`basic-addon-${param.name}`}>{param.units}</InputGroup.Text>
                                            )}
                                        </InputGroup>
                                    </div>
                                </Col>
                            </Form.Group>
                        )
                    ))}

                    {configurations.source?.reporting && Object.entries(configurations.source.reporting)
                        .filter(([key]) => key !== 'type' && key !== 'units')
                        .map(([status, statusValue], index) => {
                            // Ensure statusValue is treated as AlertReportingItem
                            const reportingItem = typeof statusValue === 'object' ? statusValue as AlertReportingItem : { text: String(statusValue) };
                            
                            return (
                                <Form.Group as={Row} className="my-3" key={status}>
                                    <Form.Label column sm={2}>{index === 0 ? 'Threshold' : ''}</Form.Label>
                                    <Col sm={10}>
                                        <div className="d-flex align-items-center mb-3">
                                            <Button
                                                variant={
                                                    status === 'critical' ? 'danger' :
                                                        status === 'warning' ? 'warning' :
                                                            status === 'ok' ? 'success' :
                                                                'secondary'
                                                }
                                                className={`me-3 fw-bold border-0 ${status === 'critical' ? 'status-critical' :
                                                    status === 'warning' ? 'status-warning' :
                                                        status === 'ok' ? 'status-ok' :
                                                            ''}`}
                                                style={{ width: "120px", height: '40px', color: 'white' }}
                                            >
                                                {status.toUpperCase()}
                                            </Button>
                                            {reportingItem.value !== undefined && (
                                                <InputGroup className="me-3">
                                                    <Form.Control
                                                        type="number"
                                                        value={reportingItem.value}
                                                        onChange={(e) => handleChange(e, `${status}_value`)}
                                                        aria-label={`${status} value`}
                                                        disabled={!canEdit || isSaving}
                                                    />
                                                    <InputGroup.Text>
                                                        {configurations.source?.reporting?.units ? String(configurations.source.reporting.units) : 'seconds'}
                                                    </InputGroup.Text>
                                                </InputGroup>
                                            )}
                                            <Form.Control
                                                type="text"
                                                value={reportingItem.text}
                                                onChange={(e) => handleChange(e, `${status}_text`)}
                                                aria-label={`${status} text`}
                                                disabled={!canEdit || isSaving}
                                            />
                                        </div>
                                    </Col>
                                </Form.Group>
                            );
                        })}

                    {canEdit && (
                        <div className="d-flex justify-content-end align-items-center mt-3 mb-1">
                            <Button
                                variant="light"
                                onClick={handleCancelEditConfigs}
                                className="me-2 border"
                                style={{width: '100px', height: '40px', color: 'black'}}
                                disabled={isSaving}
                            >
                                Cancel
                            </Button>
                            <Button
                                variant="primary"
                                onClick={handleSaveConfigs}
                                disabled={validationErrors.length > 0 || isSaving}
                                style={{width: '100px', height: '40px'}}
                            >
                                {isSaving ? 'Saving...' : 'Save'}
                            </Button>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
});

AlertConfigSection.displayName = 'AlertConfigSection';

export default AlertConfigSection;
