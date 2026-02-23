import Service from "./service";

type HostComponent = {
  componentName: string;
  hostName: string;
};

type SSMServiceData = {
  smartAgentsStartedCount: number;
  smartAgentsInstalledCount: number;
  smartAgentsTotalCount: number;
  masterComponents: [];
  slaveComponents: [];
  smartServers: HostComponent[];
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts?: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;
};

class SSMService extends Service {
  smartAgentsStartedCount: number;
  smartAgentsInstalledCount: number;
  smartAgentsTotalCount: number;
  masterComponents: [];
  slaveComponents: [];
  smartServers: HostComponent[];
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;

  constructor(data: SSMServiceData) {
    //@ts-ignore
    super(data);
    this.smartAgentsStartedCount = data.smartAgentsStartedCount || 0;
    this.smartAgentsInstalledCount = data.smartAgentsInstalledCount || 0;
    this.smartAgentsTotalCount = data.smartAgentsTotalCount || 0;
    this.masterComponents = data.masterComponents || [];
    this.slaveComponents = data.slaveComponents || [];
    this.smartServers = data.smartServers || null;
    this.alertsCount = data.alertsCount || 0;
    this.serviceState = data.serviceState || "";
    this.hasCriticalAlerts = data.hasCriticalAlerts || false;
    this.isRestartRequiredForService = data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
  }

  updateConfig(updates: Partial<SSMService>) {
    Object.assign(this, updates);
  }

  getServiceObject(): SSMService {
    return this;
  }
}

export default SSMService;
