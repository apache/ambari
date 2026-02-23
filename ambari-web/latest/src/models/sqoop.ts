import Service from "./service.ts";

type SqoopServiceData = {
  sqoopClientsStarted: number;
  sqoopClientsInstalled: number;
  sqoopClientsTotal: number;
  clientComponents: [];
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts?: boolean;
  isClientOnlyService: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;
};

class SqoopService extends Service {
  sqoopClientsStarted: number;
  sqoopClientsInstalled: number;
  sqoopClientsTotal: number;
  clientComponents: [];
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isClientOnlyService: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;

  constructor(data: SqoopServiceData) {
    super(data as any);
    this.sqoopClientsStarted = data.sqoopClientsStarted || 0;
    this.sqoopClientsInstalled = data.sqoopClientsInstalled || 0;
    this.sqoopClientsTotal = data.sqoopClientsTotal || 0;
    this.clientComponents = data.clientComponents || [];
    this.alertsCount = data.alertsCount || 0;
    this.serviceState = data.serviceState || "";
    this.hasCriticalAlerts = data.hasCriticalAlerts || false;
    this.isClientOnlyService = true;
    this.isRestartRequiredForService = data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
  }

  updateConfig(updates: Partial<SqoopService>) {
    Object.assign(this, updates);
  }

  getServiceObject(): SqoopService {
    return this;
  }
}

export default SqoopService;
