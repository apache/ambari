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

// ambari-web/ui2/src/screens/Alerts/AlertDefinitionDetails.tsx
import { useContext, useEffect, useRef, useState } from 'react';
import Spinner from '../../components/Spinner';
import HeaderSection from './HeaderSection';
import AlertConfigSection from './AlertConfigSection';
import InformationSection from './InformationSection';
import AlertInstancesTable from './AlertInstancesTable';
import { Container } from "react-bootstrap";
import { useBlocker, useParams } from "react-router-dom";
import { AlertsApi } from "../../api/alertsApi";
import { AppContext } from "../../store/context";
import { AlertEditorHandle, AlertStatusObject, MergedAlert } from './types';
import { buildAlertDefinitionDetails } from '../../Utils/alertDefinitions';
import Modal from '../../components/Modal';
import { useAlerts } from '../../store/AlertsContext';

const AlertDefinitionDetails = () => {
    const { clusterName } = useContext(AppContext);
    // Alert groups + summary already loaded/kept in sync by AlertsContext - no need to refetch them here.
    const { alertGroups, alertSummary } = useAlerts();
    const params = useParams<{ alertId?: string }>();
    const [isLoaded, setIsLoaded] = useState(false);
    const [alertDefinition, setAlertDefinition] = useState<MergedAlert | null>(null);
    const [statuses, setStatuses] = useState<AlertStatusObject[]>([]);
    const [errorMessage, setErrorMessage] = useState('');
    const [refreshTrigger, setRefreshTrigger] = useState(0);
    const [retryTrigger, setRetryTrigger] = useState(0);
    const [labelDirty, setLabelDirty] = useState(false);
    const [configDirty, setConfigDirty] = useState(false);
    const [isSavingBeforeLeave, setIsSavingBeforeLeave] = useState(false);
    const [leaveError, setLeaveError] = useState('');
    const labelEditorRef = useRef<AlertEditorHandle>(null);
    const configEditorRef = useRef<AlertEditorHandle>(null);
    const hasUnsavedChanges = labelDirty || configDirty;
    const blocker = useBlocker(({ currentLocation, nextLocation }) =>
        hasUnsavedChanges && currentLocation.pathname !== nextLocation.pathname,
    );

    const handleAlertDefinitionUpdate = (updatedFields: Partial<MergedAlert>) => {
        if (alertDefinition) {
            setAlertDefinition(prev => prev ? { ...prev, ...updatedFields } : null);
            // Trigger refresh of alert instances when alert definition is updated
            setRefreshTrigger(prev => prev + 1);
        }
    };

    useEffect(() => {
        if (!hasUnsavedChanges) return;
        const handleBeforeUnload = (event: BeforeUnloadEvent) => {
            event.preventDefault();
            event.returnValue = '';
        };
        window.addEventListener('beforeunload', handleBeforeUnload);
        return () => window.removeEventListener('beforeunload', handleBeforeUnload);
    }, [hasUnsavedChanges]);

    const handleSaveAndLeave = async () => {
        if (blocker.state !== 'blocked') return;
        setIsSavingBeforeLeave(true);
        setLeaveError('');
        const results = await Promise.all([
            labelDirty ? labelEditorRef.current?.save() ?? Promise.resolve(false) : Promise.resolve(true),
            configDirty ? configEditorRef.current?.save() ?? Promise.resolve(false) : Promise.resolve(true),
        ]);
        setIsSavingBeforeLeave(false);
        if (results.every(Boolean)) {
            blocker.proceed();
        } else {
            setLeaveError('One or more changes could not be saved. Correct the errors and try again, or discard the changes.');
        }
    };

    const handleDiscardAndLeave = () => {
        if (blocker.state !== 'blocked') return;
        labelEditorRef.current?.discard();
        configEditorRef.current?.discard();
        setLeaveError('');
        blocker.proceed();
    };

    const handleCancelLeave = () => {
        if (blocker.state !== 'blocked') return;
        setLeaveError('');
        blocker.reset();
    };

    useEffect(() => {
        const fetchAlertDetails = async () => {
            setIsLoaded(false);
            setErrorMessage('');
            try {
                const alertDefinitionId = params.alertId;
                if (alertDefinitionId) {
                    // Alert groups + summary come from AlertsContext (shared, WebSocket-updated) -
                    // only the full per-definition detail (source/config/help_url) needs its own fetch.
                    const alertDefinitionResponse = await AlertsApi.getAlertDefinitionById(
                        clusterName,
                        alertDefinitionId,
                        Date.now(),
                    );
                    const definition = alertDefinitionResponse?.AlertDefinition ||
                        alertDefinitionResponse?.items?.[0]?.AlertDefinition;
                    if (!definition) throw new Error('Alert definition not found');
                    const details = buildAlertDefinitionDetails(
                        definition,
                        alertGroups || [],
                        alertSummary?.alerts_summary_grouped || [],
                    );
                    setAlertDefinition(details);
                    setStatuses(details.statuses);
                } else {
                    setErrorMessage('Alert ID is missing');
                }
            } catch (error) {
                setErrorMessage('Failed to fetch alert definition details');
                setAlertDefinition(null);
            } finally {
                setIsLoaded(true);
            }
        };

        fetchAlertDetails();
    }, [params.alertId, clusterName, retryTrigger, alertGroups, alertSummary]);

    if (!isLoaded) {
        return <Spinner />;
    }

    if (!alertDefinition) {
        return (
            <Container className="p-4">
                <div className="alert alert-danger">{errorMessage || 'Alert definition not found'}</div>
                <button className="btn btn-primary" onClick={() => setRetryTrigger((value) => value + 1)}>
                    Retry
                </button>
            </Container>
        );
    }

    return (
        <>
        <Container className="p-4">
            <div id="alert-definition-details">
                <HeaderSection
                    ref={labelEditorRef}
                    alertDefinition={alertDefinition}
                    statuses={statuses}
                    onDirtyChange={setLabelDirty}
                    onAlertDefinitionUpdate={handleAlertDefinitionUpdate}
                />
                <div className="row gap-3">
                    <AlertConfigSection
                        ref={configEditorRef}
                        clusterName={clusterName}
                        onDirtyChange={setConfigDirty}
                    />
                    <InformationSection 
                        alertDefinition={alertDefinition} 
                        onAlertDefinitionUpdate={handleAlertDefinitionUpdate}
                    />
                </div>
                <div className="row mt-4">
                    <AlertInstancesTable 
                        clusterName={clusterName} 
                        alert_id={params.alertId || ''} 
                        definitionName={alertDefinition.name}
                        refreshTrigger={refreshTrigger}
                        alertEnabled={alertDefinition?.enabled ?? true}
                    />
                </div>
            </div>
        </Container>
        <Modal
            isOpen={blocker.state === 'blocked'}
            onClose={handleCancelLeave}
            modalTitle="Unsaved Alert Definition Changes"
            modalBody={
                <div>
                    <p>Save the current label and configuration changes before leaving this page?</p>
                    {leaveError && <div className="alert alert-danger">{leaveError}</div>}
                </div>
            }
            successCallback={handleSaveAndLeave}
            options={{
                okButtonText: isSavingBeforeLeave ? 'Saving...' : 'Save and Leave',
                cancelButtonText: 'Cancel',
                cancelableViaIcon: !isSavingBeforeLeave,
                cancelableViaBtn: true,
                okButtonDisabled: isSavingBeforeLeave,
                extraButtons: [{
                    text: 'Discard and Leave',
                    onClick: handleDiscardAndLeave,
                    variant: 'danger',
                    disabled: isSavingBeforeLeave,
                }],
            }}
        />
        </>
    );
};

export default AlertDefinitionDetails;
