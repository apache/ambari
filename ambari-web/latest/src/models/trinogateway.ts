import Service from "./service.ts";

// type HostComponent = {
//   componentName: string;
//   hostName: string;
// };


type TrinoGatewayServiceData = {
  masterComponents: [];
  trinoGateway: any;
  alertsCount: any;
  serviceState: string;
  hasCriticalAlerts?: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;
};
class TrinoGatewayService extends Service {
  masterComponents: [];
  trinoGateway: any;
  alertsCount: any;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;

  constructor(data: TrinoGatewayServiceData) {
    //@ts-ignore
    super(data);
    this.alertsCount = data.alertsCount || 0;
    this.masterComponents = data.masterComponents || [];
    this.trinoGateway = data.trinoGateway || null;
    this.alertsCount = data.alertsCount || 0;
    this.serviceState = data.serviceState || "";
    this.hasCriticalAlerts = data.hasCriticalAlerts || false;
    this.isRestartRequiredForService =
      data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
  }

  updateConfig(updates: Partial<TrinoGatewayService>) {
    Object.assign(this, updates);
  }

  getServiceObject(): TrinoGatewayService {
    return this;
  }
}
export default TrinoGatewayService;
