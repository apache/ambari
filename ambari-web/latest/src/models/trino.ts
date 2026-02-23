import Service from "./service";

type HostComponent = {
  componentName: string;
  hostName: string;
  //haNameSpace?: string;
  //clusterIdValue?: string;
};

type TrinoServiceData = {
  trinoCoordinators: HostComponent[] | [];
  masterComponents: [];
  slaveComponents: [];
  clientComponents: [];
  trinoClients: number;
  trinoWorkerInstalledCount: number;
  trinoWorkerStartedCount: number;
  trinoWorkerTotalCount: number;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts?: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;
};

class TrinoService extends Service {
  trinoCoordinators: HostComponent[] | [];
  masterComponents: [];
  slaveComponents: [];
  clientComponents: [];
  trinoClients: number;
  trinoWorkerInstalledCount: number;
  trinoWorkerStartedCount: number;
  trinoWorkerTotalCount: number;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;

  constructor(data: TrinoServiceData) {
    super(data as any);
    this.trinoCoordinators = data.trinoCoordinators || [];
    this.masterComponents = data.masterComponents || [];
    this.slaveComponents = data.slaveComponents || [];
    this.clientComponents = data.clientComponents || [];
    this.trinoClients = data.trinoClients || 0;
    this.trinoWorkerInstalledCount = data.trinoWorkerInstalledCount || 0;
    this.trinoWorkerStartedCount = data.trinoWorkerStartedCount || 0;
    this.trinoWorkerTotalCount = data.trinoWorkerTotalCount || 0;
    this.alertsCount = data.alertsCount || 0;
    this.serviceState = data.serviceState || "";
    this.hasCriticalAlerts = data.hasCriticalAlerts || false;
    this.isRestartRequiredForService = data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
  }

  updateConfig(updates: Partial<TrinoService>) {
    Object.assign(this, updates);
  }

  getServiceObject(): TrinoService {
    return this;
  }
}

export default TrinoService;
