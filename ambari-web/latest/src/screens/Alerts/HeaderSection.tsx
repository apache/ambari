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

import { forwardRef, useContext, useEffect, useImperativeHandle, useState } from 'react';
import { Link } from "react-router-dom";
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPencil, faArrowLeft, faBriefcase } from "@fortawesome/free-solid-svg-icons";
import { Button, Form, Row, Col } from 'react-bootstrap';
import { getStatusClass, formatStatus } from '../../Utils/Utility';
import "../../styles/app.scss";
import { AlertEditorHandle, MergedAlert } from './types';
import { AlertsApi } from "../../api/alertsApi";
import { AppContext } from "../../store/context";
import useAuthorizationPolicy from '../../hooks/useAuthorizationPolicy';

import {AlertStatus} from "./alertStatus";

interface StatusItem {
    status: AlertStatus;
    count: number;
    maintenance_count?: number;
    last_status_changed?: string | null;
    latest_text?: string;
}

interface HeaderSectionProps {
    alertDefinition: MergedAlert;
    statuses: StatusItem[];
    onDirtyChange?: (dirty: boolean) => void;
    onAlertDefinitionUpdate?: (updatedDefinition: Partial<MergedAlert>) => void;
}

const HeaderSection = forwardRef<AlertEditorHandle, HeaderSectionProps>(({
    alertDefinition,
    statuses,
    onDirtyChange,
    onAlertDefinitionUpdate,
}, ref) => {
    const { clusterName } = useContext(AppContext);
    const [isEditing, setIsEditing] = useState(false);
    const [label, setLabel] = useState(alertDefinition.label);
    const [hasError, setHasError] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');
    const [saveError, setSaveError] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    
    // Authorization hooks - implementing Ember.js alert authorization patterns
    const { isAuthorized } = useAuthorizationPolicy();
    
    // Check specific authorizations for alert operations
    const canToggleAlerts = isAuthorized('SERVICE.TOGGLE_ALERTS');

    useEffect(() => {
        if (!isEditing) setLabel(alertDefinition.label);
    }, [alertDefinition.label, isEditing]);

    // Validation regex for alert definition name
    const validNameRegex = /^[a-zA-Z0-9_\-\s\[\]%]+$/;
    const maxLength = 255;

    const validateInput = (value: string): boolean => {
        if (value.trim() === '') {
            setErrorMessage('Alert definition name cannot be empty');
            return false;
        }
        
        if (value.length > maxLength) {
            setErrorMessage(`Value should be less than ${maxLength} symbols`);
            return false;
        }
        
        if (!validNameRegex.test(value)) {
            setErrorMessage('Invalid input. Only alphanumerics, underscores, hyphens, percentage, brackets and spaces are allowed. Value should be less than 255 symbols.');
            return false;
        }
        
        setErrorMessage('');
        return true;
    };

    const handleEditClick = () => {
        setIsEditing(true);
        setSaveError('');
        onDirtyChange?.(true);
    };

    const handleCancelEdit = () => {
        setIsEditing(false);
        setLabel(alertDefinition.label);
        setHasError(false);
        setErrorMessage('');
        setSaveError('');
        onDirtyChange?.(false);
    };

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const value = e.target.value;
        setLabel(value);
        setHasError(!validateInput(value));
        setSaveError('');
    };

    const handleSaveEdit = async (): Promise<boolean> => {
        if (!validateInput(label)) {
            setHasError(true);
            return false;
        }

        setIsSubmitting(true);
        
        try {
            await AlertsApi.updateAlertDefinition(
                clusterName,
                alertDefinition.alert_definition_id,
                {
                    "AlertDefinition/label": label
                }
            );
            
            onAlertDefinitionUpdate?.({ label });
            setIsEditing(false);
            setSaveError('');
            onDirtyChange?.(false);
            return true;
        } catch (error) {
            console.error('Failed to update alert definition:', error);
            setSaveError('Failed to update alert definition. Please try again.');
            return false;
        } finally {
            setIsSubmitting(false);
        }
    };

    useImperativeHandle(ref, () => ({
        save: handleSaveEdit,
        discard: handleCancelEdit,
    }));

    return (
        <Row className="alert-definition-details-header gap-4 mb-4">
            <Col md={8} className="my-2">
                <Row className="definition-name">
                    {isEditing ? (
                        <>
                            <Col md={8}>
                                <Form.Control
                                    type="text"
                                    value={label}
                                    className={`form-control ${hasError ? 'is-invalid' : ''}`}
                                    onChange={handleInputChange}
                                    disabled={isSubmitting}
                                />
                                {hasError && (
                                    <div className="invalid-feedback">
                                        {errorMessage}
                                    </div>
                                )}
                                {saveError && <div className="text-danger mt-1">{saveError}</div>}
                            </Col>
                            <Col md={4} className="edit-buttons align-content-center align-items-end">
                                <Button 
                                    variant="secondary" 
                                    onClick={handleCancelEdit} 
                                    className="mx-2"
                                    disabled={isSubmitting}
                                >
                                    Cancel
                                </Button>
                                <Button 
                                    variant="primary" 
                                    onClick={handleSaveEdit} 
                                    disabled={hasError || isSubmitting}
                                >
                                    {isSubmitting ? 'Saving...' : 'Save'}
                                </Button>
                            </Col>
                            <Col className="mt-2">
                                <Link to={`/main/alerts`} className="custom-link">
                                    <FontAwesomeIcon size={'lg'} icon={faArrowLeft} /> Back
                                </Link>
                            </Col>
                        </>
                    ) : (
                        <Row>
                            <h2>
                                {alertDefinition.label}
                                {/* Only show edit pencil icon if user has SERVICE.TOGGLE_ALERTS permission and upgrade is not blocking */}
                                {canToggleAlerts && (
                                    <FontAwesomeIcon
                                        size={'lg'}
                                        className={'mx-2 cursor-pointer'}
                                        icon={faPencil}
                                        onClick={handleEditClick}
                                    />
                                )}
                            </h2>
                            <Link to={`/main/alerts`} className="custom-link">
                                <FontAwesomeIcon size={'lg'} icon={faArrowLeft} /> Back
                            </Link>
                        </Row>
                    )}
                </Row>
            </Col>
            <Col md={3} className="status d-flex">
                <div className="status-container d-flex flex-wrap ml-auto">
                    {statuses.length > 0 ? (
                        statuses.map((statusItem, index: number) => (
                            <div key={`${statusItem.status}-${index}`} className="status-row d-flex align-items-center">
                                {/* Show maintenance mode alerts with grayed-out styling and maintenance icon */}
                                {statusItem.maintenance_count && statusItem.maintenance_count > 0 ? (
                                    <Button
                                        className={`alert-item alert-status-box-detailed alert-maintenance ${getStatusClass(
                                            statusItem.status
                                        )} status-maintenance ${index > 0 ? 'mx-1' : ''}`}
                                    >
                                        <FontAwesomeIcon
                                            icon={faBriefcase}
                                            className="fs-12 me-2 text-white"
                                        />
                                        {formatStatus(statusItem.status, statusItem.maintenance_count)}
                                    </Button>
                                ) : null}
                                {/* Show regular alerts with normal styling */}
                                {statusItem.count > 0 ? (
                                    <Button
                                        className={`alert-item alert-status-box-detailed ${getStatusClass(
                                            statusItem.status
                                        )} ${index > 0 || (statusItem.maintenance_count && statusItem.maintenance_count > 0) ? 'mx-1' : ''}`}
                                    >
                                        {formatStatus(statusItem.status, statusItem.count)}
                                    </Button>
                                ) : null}
                            </div>
                        ))
                    ) : (
                        <Button className="alert-item alert-status-box status-none">
                            None
                        </Button>
                    )}
                </div>
            </Col>
        </Row>
    );
});

HeaderSection.displayName = 'HeaderSection';

export default HeaderSection;
