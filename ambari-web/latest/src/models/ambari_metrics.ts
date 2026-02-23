import Service from "./service";

type HostComponent = {
  componentName: string;
  hostName: string;
};

type AmbariMetricsServiceData = {
  metricsMonitorsStartedCount: number;
  metricsMonitorsInstalledCount: number;
  metricsMonitorsTotalCount: number;
  masterComponents: [];
  slaveComponents: [];
  grafana: HostComponent;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;
};

class AmbariMetricsService extends Service {
  metricsMonitorsStartedCount: number;
  metricsMonitorsInstalledCount: number;
  metricsMonitorsTotalCount: number;
  masterComponents: [];
  slaveComponents: [];
  grafana: HostComponent;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;


  constructor(data: AmbariMetricsServiceData) {
    //@ts-ignore
    super(data);
    this.metricsMonitorsStartedCount = data.metricsMonitorsStartedCount || 0;
    this.metricsMonitorsInstalledCount =
      data.metricsMonitorsInstalledCount || 0;
    this.metricsMonitorsTotalCount = data.metricsMonitorsTotalCount || 0;
    this.masterComponents = data.masterComponents || [];
    this.slaveComponents = data.slaveComponents || [];
    this.grafana = data.grafana || null;
    this.alertsCount = data.alertsCount || 0;
    this.serviceState = data.serviceState || "";
    this.hasCriticalAlerts = data.hasCriticalAlerts || false;
    this.isRestartRequiredForService =
      data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
  }

  updateConfig(updates: Partial<AmbariMetricsService>) {
    Object.assign(this, updates);
  }

  getServiceObject(): AmbariMetricsService {
    return this;
  }
}

export default AmbariMetricsService;
