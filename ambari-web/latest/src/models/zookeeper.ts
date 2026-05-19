import Service from "./service.ts";

type HostComponent = {
  componentName: string;
  hostNames: string;
};

type ZkServiceData = {
  zookeeperServers: HostComponent[];
  masterComponents: [];
  clientComponents: [];
  zkClientsInstalled: number;
  alertsCount: any;
  serviceState: string;
  hasCriticalAlerts?: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;
};
class ZkService extends Service {
  masterComponents: [];
  clientComponents: [];
  zkClientsInstalled: number;
  alertsCount: any;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;


  constructor(data: ZkServiceData) {
    //@ts-ignore
    super(data);
    this.alertsCount = data.alertsCount || 0;
    this.masterComponents = data.masterComponents || [];
    this.clientComponents = data.clientComponents || [];
    this.zkClientsInstalled = data.zkClientsInstalled || 0;
    this.alertsCount = data.alertsCount || 0;
    this.serviceState = data.serviceState || "";
    this.hasCriticalAlerts = data.hasCriticalAlerts || false;
    this.isRestartRequiredForService = data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
  }

  updateConfig(updates: Partial<ZkService>) {
    Object.assign(this, updates);
  }

  getServiceObject(): ZkService {
    return this;
  }
}
export default ZkService;
