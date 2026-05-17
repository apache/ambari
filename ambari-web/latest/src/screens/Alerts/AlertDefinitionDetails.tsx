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

import { useState, useEffect, useContext } from 'react';
import Spinner from '../../components/Spinner.tsx';
import HeaderSection from './HeaderSection';
import AlertConfigSection from './AlertConfigSection';
import InformationSection from './InformationSection';
import AlertInstancesTable from './AlertInstancesTable';
import { Container } from "react-bootstrap";
import { useParams } from "react-router-dom";
import { AlertsApi } from "../../api/alertsApi.ts";
import { AppContext } from "../../store/context.tsx";
import { processData } from './alertUtils';
import { AlertStatusObject, MergedAlert } from './types.ts';

interface AlertDefinitionResponseItem {
    AlertDefinition: {
        id: number;
        repeat_tolerance: any;
        repeat_tolerance_enabled: boolean;
        help_url?: string;
        [key: string]: any;
    };
}

const AlertDefinitionDetails = () => {
    const { clusterName } = useContext(AppContext);
    const params = useParams<{ alertId?: string }>();
    const [isLoaded, setIsLoaded] = useState(false);
    const [alertDefinition, setAlertDefinition] = useState<MergedAlert | null>(null);
    const [statuses, setStatuses] = useState<AlertStatusObject[]>([]);
    const [errorMessage, setErrorMessage] = useState('');
    const [refreshTrigger, setRefreshTrigger] = useState(0);

    const handleAlertDefinitionUpdate = (updatedFields: Partial<MergedAlert>) => {
        if (alertDefinition) {
            setAlertDefinition(prev => prev ? { ...prev, ...updatedFields } : null);
            // Trigger refresh of alert instances when alert definition is updated
            setRefreshTrigger(prev => prev + 1);
        }
    };

    useEffect(() => {
        const fetchAlertDetails = async () => {
            try {
                const alertDefinitionId = params.alertId;
                if (alertDefinitionId) {
                    const [alertsResponse, summariesResponse, alertDefinitionResponse] = await Promise.all([
                        AlertsApi.getAlerts(
                            clusterName,
                            'AlertGroup/default,AlertGroup/definitions,AlertGroup/id,AlertGroup/name,AlertGroup/targets',
                            Date.now()
                        ),
                        AlertsApi.getGroupFormattedAlertsNotifications(clusterName, Date.now()),
                        AlertsApi.getAlertDefinition(
                            clusterName,
                            'AlertDefinition/component_name,AlertDefinition/description,AlertDefinition/enabled,AlertDefinition/repeat_tolerance,AlertDefinition/repeat_tolerance_enabled,AlertDefinition/id,AlertDefinition/ignore_host,AlertDefinition/interval,AlertDefinition/label,AlertDefinition/name,AlertDefinition/scope,AlertDefinition/service_name,AlertDefinition/source,AlertDefinition/help_url',
                            Date.now()
                        )
                    ]);

                    const processedAlerts = processData(alertsResponse, summariesResponse);
                    const alertDetails = processedAlerts.find(alert => alert.alert_definition_id === parseInt(alertDefinitionId));
                    const alertDefinitionDetails = alertDefinitionResponse.items.find((item: AlertDefinitionResponseItem) => 
                        item.AlertDefinition.id === parseInt(alertDefinitionId)
                    );

                    if (alertDetails && alertDefinitionDetails) {
                        setAlertDefinition({
                            ...alertDetails,
                            repeat_tolerance: alertDefinitionDetails.AlertDefinition.repeat_tolerance || 1,
                            repeat_tolerance_enabled: alertDefinitionDetails.AlertDefinition.repeat_tolerance_enabled || false,
                            help_url: alertDefinitionDetails.AlertDefinition.help_url
                        });
                        setStatuses(alertDetails.statuses);
                    } else {
                        setErrorMessage('Alert details not found');
                    }
                } else {
                    setErrorMessage('Alert ID is missing');
                }
                setIsLoaded(true);
            } catch (error) {
                setErrorMessage('Failed to fetch alert definition details');
                setIsLoaded(true);
            }
        };

        fetchAlertDetails();
    }, [params, clusterName]);

    if (!isLoaded) {
        return <Spinner />;
    }

    if (!alertDefinition) {
        return (
            <Container className="p-4">
                <div className="alert alert-danger">{errorMessage || 'Alert definition not found'}</div>
            </Container>
        );
    }

    return (
        <Container className="p-4">
            <div id="alert-definition-details">
                <HeaderSection alertDefinition={alertDefinition} statuses={statuses} />
                <div className="row gap-3">
                    <AlertConfigSection clusterName={clusterName} />
                    <InformationSection 
                        alertDefinition={alertDefinition} 
                        onAlertDefinitionUpdate={handleAlertDefinitionUpdate}
                    />
                </div>
                <div className="row mt-4">
                    <AlertInstancesTable 
                        clusterName={clusterName} 
                        alert_id={params.alertId || ''} 
                        refreshTrigger={refreshTrigger}
                        alertEnabled={alertDefinition?.enabled ?? true}
                    />
                </div>
            </div>
        </Container>
    );
};

export default AlertDefinitionDetails;
