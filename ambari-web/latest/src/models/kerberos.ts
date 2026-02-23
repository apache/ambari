import Service from "./service.ts";

type KerberosServiceData = {
  clientComponents: [];
  kerberosClientsInstalled: number;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isClientOnlyService: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;
};
class KerberosService extends Service {
  clientComponents: [];
  kerberosClientsInstalled: number;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isClientOnlyService: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;

  constructor(data: KerberosServiceData) {
    //@ts-ignore
    super(data);
    this.clientComponents = data.clientComponents || [];
    this.kerberosClientsInstalled = data.kerberosClientsInstalled || 0;
    this.alertsCount = data.alertsCount || 0;
    this.serviceState = data.serviceState || "";
    this.hasCriticalAlerts = data.hasCriticalAlerts || false;
    this.isClientOnlyService = true;
    this.isRestartRequiredForService = data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
  }

  updateConfig(updates: Partial<KerberosService>) {
    Object.assign(this, updates);
  }

  getServiceObject(): KerberosService {
    return this;
  }
}
export default KerberosService;
