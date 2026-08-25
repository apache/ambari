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

type Component = {
    componentName: string;
    displayName: string;
    staleConfigHosts: string[];
    allowToDelete: boolean;
  };
  
  export type ServiceData = {
    serviceName: string;
    displayName: string;
    passiveState: string;
    workStatus: string;
    rand: string;
    toolTipContent: string;
    quickLinks: any[];
    hostComponents: any[];
    serviceConfigsTemplate: any[];
    desiredRepositoryVersionId: number | null;
    installedClients: number | null;
    clientComponents: Component[];
    slaveComponents: Component[];
    masterComponents: Component[];
    masterComponentGroups: any;
    deleteInProgress: boolean;
    restartRequiredHostsAndComponents: { [key: string]: string[] };
    hasCriticalAlerts: boolean;
    alertsCount: number;
  };
  
  class Service {
    static statesMap = {
      init: 'INIT',
      installing: 'INSTALLING',
      install_failed: 'INSTALL_FAILED',
      stopped: 'INSTALLED',
      starting: 'STARTING',
      started: 'STARTED',
      stopping: 'STOPPING',
      uninstalling: 'UNINSTALLING',
      uninstalled: 'UNINSTALLED',
      wiping_out: 'WIPING_OUT',
      upgrading: 'UPGRADING',
      maintenance: 'MAINTENANCE',
      unknown: 'UNKNOWN',
    };
  
    static inProgressStates = [
      Service.statesMap.installing,
      Service.statesMap.starting,
      Service.statesMap.stopping,
      Service.statesMap.uninstalling,
      Service.statesMap.upgrading,
      Service.statesMap.wiping_out,
    ];
  
    static allowUninstallStates = [
      Service.statesMap.init,
      Service.statesMap.install_failed,
      Service.statesMap.stopped,
      Service.statesMap.unknown,
    ];
  
    static Health = {
      live: "LIVE",
      dead: "DEAD-RED",
      starting: "STARTING",
      stopping: "STOPPING",
      unknown: "DEAD-YELLOW",
  
      getKeyName(value: string): string {
        switch (value) {
          case this.live:
            return 'live';
          case this.dead:
            return 'dead';
          case this.starting:
            return 'starting';
          case this.stopping:
            return 'stopping';
          case this.unknown:
            return 'unknown';
          default:
            return 'none';
        }
      },
    };
  
    static extendedModel = {
      'HDFS': 'HDFSService',
      'ONEFS': 'ONEFSService',
      'HBASE': 'HBaseService',
      'YARN': 'YARNService',
      'MAPREDUCE2': 'MapReduce2Service',
      'STORM': 'StormService',
      'RANGER': 'RangerService',
      'FLUME': 'FlumeService',
    };
  
    static FIXTURES: ServiceData[] = [];
  
    serviceName: string;
    displayName: string;
    passiveState: string;
    workStatus: string;
    rand: string;
    toolTipContent: string;
    quickLinks: any[];
    hostComponents: any[];
    serviceConfigsTemplate: any[];
    desiredRepositoryVersionId: number | null;
    installedClients: number | null;
    clientComponents: Component[];
    slaveComponents: Component[];
    masterComponents: Component[];
    deleteInProgress: boolean;
    restartRequiredHostsAndComponents: { [key: string]: string[] };
    hasCriticalAlerts: boolean;
    alertsCount: number;
  
    constructor(data: ServiceData) {
      this.serviceName = data.serviceName || '';
      this.displayName = data.displayName || '';
      this.passiveState = data.passiveState || 'OFF';
      this.workStatus = data.workStatus || '';
      this.rand = data.rand || '';
      this.toolTipContent = data.toolTipContent || '';
      this.quickLinks = data.quickLinks || [];
      this.hostComponents = data.hostComponents || [];
      this.serviceConfigsTemplate = data.serviceConfigsTemplate || [];
      this.desiredRepositoryVersionId = data.desiredRepositoryVersionId;
      this.installedClients = data.installedClients;
      this.clientComponents = data.clientComponents || [];
      this.slaveComponents = data.slaveComponents || [];
      this.masterComponents = data.masterComponents || [];
      this.deleteInProgress = data.deleteInProgress || false;
      this.restartRequiredHostsAndComponents = data.restartRequiredHostsAndComponents || {};
      this.hasCriticalAlerts = data.hasCriticalAlerts || false;
      this.alertsCount = data.alertsCount || 0;
    }
  
    get serviceComponents(): string[] {
      return [
        ...this.clientComponents.map(component => component.componentName),
        ...this.slaveComponents.map(component => component.componentName),
        ...this.masterComponents.map(component => component.componentName),
      ];
    }
  
    get healthStatus(): string {
      const healthStatusMap = {
        STARTED: 'green',
        STARTING: 'green-blinking',
        INSTALLED: 'red',
        STOPPING: 'red-blinking',
        UNKNOWN: 'yellow',
      };
      return healthStatusMap[this.workStatus as keyof typeof healthStatusMap] || 'yellow';
    }
  
    get isStopped(): boolean {
      return this.workStatus === 'INSTALLED';
    }
  
    get isStarted(): boolean {
      return this.workStatus === 'STARTED';
    }

    get masterComponentGroups(): { name: string; title: string; hosts: string[]; components: string[]; clusterId: string }[] {
      // Default implementation or return an empty array
      return [];
    }
  
    get isInPassive(): boolean {
      return this.passiveState === 'ON';
    }
  
    get hasMultipleMasterComponentGroups(): boolean {
      return this.masterComponentGroups.length > 1;
    }
  
    get allowToDelete(): boolean {
      return (
        Service.allowUninstallStates.includes(this.workStatus) &&
        this.slaveComponents.every(component => component.allowToDelete) &&
        this.masterComponents.every(component => component.allowToDelete)
      );
    }
  
    get restartRequiredMessage(): string {
      let hostCount = 0;
      let hcCount = 0;
      let hostsMsg = '<ul>';
      for (const host in this.restartRequiredHostsAndComponents) {
        hostCount++;
        hostsMsg += `<li>${host}</li><ul>`;
        this.restartRequiredHostsAndComponents[host].forEach(component => {
          hcCount++;
          hostsMsg += `<li>${component}</li>`;
        });
        hostsMsg += '</ul>';
      }
      hostsMsg += '</ul>';
      return `Restart required for ${hcCount} components on ${hostCount} hosts: ${hostsMsg}`;
    }
  
    get serviceTypes(): string[] {
      const typeServiceMap = {
        GANGLIA: ['MONITORING'],
        HDFS: ['HA_MODE', 'FEDERATION', 'DFSRouter'],
        YARN: ['HA_MODE'],
        RANGER: ['HA_MODE'],
        HAWQ: ['HA_MODE'],
      };
      return typeServiceMap[this.serviceName as keyof typeof typeServiceMap] || [];
    }
  
    get isRestartRequired(): boolean {
      const serviceComponents = [
        ...this.clientComponents,
        ...this.slaveComponents,
        ...this.masterComponents,
      ];
      const hc: { [key: string]: string[] } = {};
      serviceComponents.forEach(component => {
        const displayName = component.displayName;
        component.staleConfigHosts.forEach(hostName => {
          if (!hc[hostName]) {
            hc[hostName] = [];
          }
          hc[hostName].push(displayName);
        });
      });
      this.restartRequiredHostsAndComponents = hc;
      return serviceComponents.some(component => component.staleConfigHosts.length > 0);
    }
  }
  
  export default Service;
