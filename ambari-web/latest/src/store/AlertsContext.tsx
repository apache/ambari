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

/**
 * AlertsContext - centralized alert data management with WebSocket updates.
 *
 * Single source of truth for alert groups/definitions/summary so consumers
 * (Navbar, ServiceSummary, Alerts, AlertDefinitionDetails, ServiceContext)
 * don't each independently poll the alerts APIs.
 *
 * Pattern, matching EmberJS:
 * - Initial load once: alert groups -> alert definitions -> alert summary -> unhealthy alerts
 * - WebSocket updates on /events/alerts refresh the summary and unhealthy alerts
 * - Polling only for unhealthy alerts, and only while on the /main/alerts route
 */

import React, { createContext, useState, useEffect, useCallback, useContext, useRef } from 'react';
import { AlertsApi } from '../api/alertsApi';
import { getCurrTimeInSec } from '../Utils/Utility';
import { AppContext } from './context';
import { useLocation } from 'react-router-dom';

interface AlertsContextType {
  alertGroups: any[];
  alertDefinitions: any[];
  alertSummary: any;
  unhealthyAlertInstances: any[];
  isLoading: boolean;
  refreshUnhealthyAlerts: () => Promise<void>;
}

const AlertsContext = createContext<AlertsContextType | undefined>(undefined);

const UNHEALTHY_ALERTS_POLL_INTERVAL = 10000; // 10 seconds - matching EmberJS

export const AlertsProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { clusterName, parsedSocketMessages } = useContext(AppContext);
  const location = useLocation();

  const [alertGroups, setAlertGroups] = useState<any[]>([]);
  const [alertDefinitions, setAlertDefinitions] = useState<any[]>([]);
  const [alertSummary, setAlertSummary] = useState<any>(null);
  const [unhealthyAlertInstances, setUnhealthyAlertInstances] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const initialLoadComplete = useRef(false);
  const pollIntervalRef = useRef<NodeJS.Timeout | null>(null);

  const isAlertsRoute = location.pathname.includes('/main/alerts');

  const loadAlertGroups = useCallback(async () => {
    if (!clusterName) return;

    try {
      const currTime = getCurrTimeInSec();
      const response = await AlertsApi.getAlerts(
        clusterName,
        'AlertGroup/default,AlertGroup/definitions,AlertGroup/id,AlertGroup/name,AlertGroup/targets',
        currTime
      );

      if (response?.items) {
        setAlertGroups(response.items);
      }
    } catch (error) {
      console.error('[AlertsContext] Error loading alert groups:', error);
    }
  }, [clusterName]);

  const loadAlertDefinitions = useCallback(async () => {
    if (!clusterName) return;

    try {
      const currTime = getCurrTimeInSec();
      const response = await AlertsApi.getAlertDefinition(
        clusterName,
        'AlertDefinition/component_name,AlertDefinition/description,AlertDefinition/enabled,AlertDefinition/id,AlertDefinition/label,AlertDefinition/name,AlertDefinition/service_name',
        currTime
      );

      if (response?.items) {
        const definitions = response.items.map((item: any) => ({
          ...item.AlertDefinition,
          label: item.AlertDefinition.label || item.AlertDefinition.name,
          component_name: item.AlertDefinition.component_name || 'N/A'
        }));
        setAlertDefinitions(definitions);
      }
    } catch (error) {
      console.error('[AlertsContext] Error loading alert definitions:', error);
    }
  }, [clusterName]);

  const loadAlertDefinitionSummary = useCallback(async () => {
    if (!clusterName) return;

    try {
      const currTime = getCurrTimeInSec();
      const response = await AlertsApi.getGroupFormattedAlertsNotifications(clusterName, currTime);

      setAlertSummary(response);
    } catch (error) {
      console.error('[AlertsContext] Error loading alert summary:', error);
    }
  }, [clusterName]);

  const loadUnhealthyAlertInstances = useCallback(async () => {
    if (!clusterName) return;

    try {
      const response = await AlertsApi.getAlertsListDetailed(clusterName);

      if (response?.items) {
        setUnhealthyAlertInstances(response.items);
      }
    } catch (error) {
      console.error('[AlertsContext] Error loading unhealthy alerts:', error);
    }
  }, [clusterName]);

  // Initial load - once per application session (like EmberJS's cluster_controller loadAlerts())
  const performInitialLoad = useCallback(async () => {
    if (!clusterName || initialLoadComplete.current) return;

    setIsLoading(true);

    try {
      await loadAlertGroups();
      await loadAlertDefinitions();
      await loadAlertDefinitionSummary();
      await loadUnhealthyAlertInstances();

      initialLoadComplete.current = true;
    } catch (error) {
      console.error('[AlertsContext] Error during initial load:', error);
    } finally {
      setIsLoading(false);
    }
  }, [clusterName, loadAlertGroups, loadAlertDefinitions, loadAlertDefinitionSummary, loadUnhealthyAlertInstances]);

  useEffect(() => {
    performInitialLoad();
  }, [performInitialLoad]);

  // Poll unhealthy alerts only while on the alerts route
  useEffect(() => {
    if (pollIntervalRef.current) {
      clearInterval(pollIntervalRef.current);
      pollIntervalRef.current = null;
    }

    if (isAlertsRoute && initialLoadComplete.current) {
      pollIntervalRef.current = setInterval(() => {
        loadUnhealthyAlertInstances();
      }, UNHEALTHY_ALERTS_POLL_INTERVAL);
    }

    return () => {
      if (pollIntervalRef.current) {
        clearInterval(pollIntervalRef.current);
        pollIntervalRef.current = null;
      }
    };
  }, [isAlertsRoute, loadUnhealthyAlertInstances]);

  // WebSocket updates for alert summary; refresh unhealthy alerts to stay in sync
  useEffect(() => {
    if (!initialLoadComplete.current || parsedSocketMessages.length === 0) {
      return;
    }

    const latestMessage = parsedSocketMessages[0];

    if (latestMessage?.destination === '/events/alerts' && latestMessage.summaries) {
      const clusterId = latestMessage.clusterId || Object.keys(latestMessage.summaries)[0];
      const clusterSummaries = latestMessage.summaries[clusterId];

      if (clusterSummaries) {
        const updatedSummary = { alerts_summary_grouped: Object.values(clusterSummaries) };
        setAlertSummary(updatedSummary);

        // Keep the detailed unhealthy alert list in sync with the new summary
        loadUnhealthyAlertInstances();
      }
    }
  }, [parsedSocketMessages, loadUnhealthyAlertInstances]);

  const value: AlertsContextType = {
    alertGroups,
    alertDefinitions,
    alertSummary,
    unhealthyAlertInstances,
    isLoading,
    refreshUnhealthyAlerts: loadUnhealthyAlertInstances,
  };

  return <AlertsContext.Provider value={value}>{children}</AlertsContext.Provider>;
};

export const useAlerts = (): AlertsContextType => {
  const context = useContext(AlertsContext);
  if (!context) {
    throw new Error('useAlerts must be used within an AlertsProvider');
  }
  return context;
};
