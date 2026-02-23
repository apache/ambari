import Service from "./service.ts";

type TezServiceData = {
  tezClientsStarted: number;
  tezClientsInstalled: number;
  tezClientsTotal: number;
  clientComponents: [];
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts?: boolean;
  isClientOnlyService: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;
};
class TezService extends Service {
  tezClientsStarted: number;
  tezClientsInstalled: number;
  tezClientsTotal: number;
  clientComponents: [];
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isClientOnlyService: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;

  constructor(data: TezServiceData) {
    super(data as any);
    this.tezClientsStarted = data.tezClientsStarted || 0;
    this.tezClientsInstalled = data.tezClientsInstalled || 0;
    this.tezClientsTotal = data.tezClientsTotal || 0;
    this.clientComponents = data.clientComponents || [];
    this.alertsCount = data.alertsCount || 0;
    this.serviceState = data.serviceState || "";
    this.hasCriticalAlerts = data.hasCriticalAlerts || false;
    this.isClientOnlyService = true;
    this.isRestartRequiredForService = data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
  }

  updateConfig(updates: Partial<TezService>) {
    Object.assign(this, updates);
  }

  getServiceObject(): TezService {
    return this;
  }
}
export default TezService;
