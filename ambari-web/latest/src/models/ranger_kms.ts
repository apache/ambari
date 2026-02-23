import Service from "./service.ts";

type RangerKMSServiceData = {
  masterComponents: [];
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;
};
class RangerKMSService extends Service {
  masterComponents: [];
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;

  constructor(data: RangerKMSServiceData) {
    //@ts-ignore
    super(data);
    this.masterComponents = data.masterComponents || [];
    this.alertsCount = data.alertsCount || 0;
    this.serviceState = data.serviceState || "";
    this.hasCriticalAlerts = data.hasCriticalAlerts || false;
    this.isRestartRequiredForService = data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
  }

  updateConfig(updates: Partial<RangerKMSService>) {
    Object.assign(this, updates);
  }

  getServiceObject(): RangerKMSService {
    return this;
  }
}
export default RangerKMSService;
