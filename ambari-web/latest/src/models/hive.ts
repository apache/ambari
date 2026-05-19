import Service from "./service.ts";

// type HostComponent = {
//   componentName: string;
//   hostNames: string;
// };

type HiveServiceData = {
  masterComponents: [];
  clientComponents: [];
  hiveClientsInstalled: number;
  hiveServer2JDBCURL: string;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;
};
class HiveService extends Service {
  masterComponents: [];
  clientComponents: [];
  hiveClientsInstalled: number;
  hiveServer2JDBCURL: string;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;

  constructor(data: HiveServiceData) {
    //@ts-ignore
    super(data);
    this.masterComponents = data.masterComponents || [];
    this.clientComponents = data.clientComponents || [];
    this.hiveClientsInstalled = data.hiveClientsInstalled || 0;
    this.hiveServer2JDBCURL = data.hiveServer2JDBCURL || "";
    this.alertsCount = data.alertsCount || 0;
    this.serviceState = data.serviceState || "";
    this.hasCriticalAlerts = data.hasCriticalAlerts || false;
    this.isRestartRequiredForService = data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
  }

  updateConfig(updates: Partial<HiveService>) {
    Object.assign(this, updates);
  }

  getServiceObject(): HiveService {
    return this;
  }
}
export default HiveService;
