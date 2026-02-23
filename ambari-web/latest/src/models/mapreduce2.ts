import Service from "./service";

type HostComponent = {
  componentName: string;
  hostName: string;
  //haNameSpace?: string;
  //clusterIdValue?: string;
};

type MapReduce2ServiceData = {
  jobHistoryServer: HostComponent | null;
  masterComponents: [];
  clientComponents: [];
  mapReduce2Clients: number;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;
};

class MapReduce2Service extends Service {
  jobHistoryServer: HostComponent | null;
  masterComponents: [];
  clientComponents: [];
  mapReduce2Clients: number;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;

  constructor(data: MapReduce2ServiceData) {
    super(data as any);
    this.jobHistoryServer = data.jobHistoryServer || null;
    this.masterComponents = data.masterComponents || [];
    this.clientComponents = data.clientComponents || [];
    this.mapReduce2Clients = data.mapReduce2Clients || 0;
    this.alertsCount = data.alertsCount || 0;
    this.serviceState = data.serviceState || "";
    this.hasCriticalAlerts = data.hasCriticalAlerts || false;
    this.isRestartRequiredForService = data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
  }

  updateConfig(updates: Partial<MapReduce2Service>) {
    Object.assign(this, updates);
  }

  getServiceObject(): MapReduce2Service {
    return this;
  }
}

export default MapReduce2Service;