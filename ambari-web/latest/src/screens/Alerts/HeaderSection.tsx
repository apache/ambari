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

import { useState, useContext } from 'react';
import { Link } from "react-router-dom";
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPencil, faArrowLeft, faBriefcase } from "@fortawesome/free-solid-svg-icons";
import { Button, Form, Row, Col } from 'react-bootstrap';
import { getStatusClass, formatStatus } from '../../Utils/Utility';
import "../../styles/app.scss";
import { MergedAlert } from './types';
import { AlertsApi } from "../../api/alertsApi";
import { AppContext } from "../../store/context";
import { useAuth } from '../../hooks/useAuth';

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
}

const HeaderSection = ({ alertDefinition, statuses }: HeaderSectionProps) => {
    const { clusterName, upgradeIsRunning, upgradeSuspended } = useContext(AppContext);
    const [isEditing, setIsEditing] = useState(false);
    const [label, setLabel] = useState(alertDefinition.label);
    const [hasError, setHasError] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    
    // Authorization hooks - implementing Ember.js alert authorization patterns
    const { hasAuthorization } = useAuth();
    
    // Check specific authorizations for alert operations
    const canToggleAlerts = hasAuthorization('SERVICE.TOGGLE_ALERTS');
    
    // Check if upgrade is blocking operations (running but not suspended)
    const isUpgradeBlocking = upgradeIsRunning && !upgradeSuspended;

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
    };

    const handleCancelEdit = () => {
        setIsEditing(false);
        setLabel(alertDefinition.label);
        setHasError(false);
        setErrorMessage('');
    };

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const value = e.target.value;
        setLabel(value);
        setHasError(!validateInput(value));
    };

    const handleSaveEdit = async () => {
        if (!validateInput(label)) {
            setHasError(true);
            return;
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
            
            // Update local state
            alertDefinition.label = label;
            setIsEditing(false);
        } catch (error) {
            console.error('Failed to update alert definition:', error);
            setErrorMessage('Failed to update alert definition. Please try again.');
            setHasError(true);
        } finally {
            setIsSubmitting(false);
        }
    };

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
                                {canToggleAlerts && !isUpgradeBlocking && (
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
};

export default HeaderSection;
