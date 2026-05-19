import Service from "./service.ts";

type KyuubiServiceData = {
  masterComponents: [];
  alertsCount: any;
  serviceState: string;
  hasCriticalAlerts?: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;
};
class KyuubiService extends Service {
  masterComponents: [];
  alertsCount: any;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;

  constructor(data: KyuubiServiceData) {
    //@ts-ignore
    super(data);
    this.alertsCount = data.alertsCount || 0;
    this.masterComponents = data.masterComponents || [];
    this.alertsCount = data.alertsCount || 0;
    this.serviceState = data.serviceState || "";
    this.hasCriticalAlerts = data.hasCriticalAlerts || false;
    this.isRestartRequiredForService =
      data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
  }

  updateConfig(updates: Partial<KyuubiService>) {
    Object.assign(this, updates);
  }

  getServiceObject(): KyuubiService {
    return this;
  }
}
export default KyuubiService;
