import Service from "./service";

type HostComponent = {
  componentName: string;
  hostName: string;
};

type RangerServiceData = {
  rangerTagSyncsStarted: number;
  rangerTagSyncsInstalled: number;
  rangerTagSyncsTotal: number;
  masterComponents: [];
  slaveComponents: [];
  rangerAdmins: HostComponent[];
  rangerHDFSPluginProperties: string;
  rangerHbasePluginProperties: string;
  rangerHivePluginProperties: string;
  rangerYarnPluginProperties: string;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;
};

class RangerService extends Service {
  rangerTagSyncsStarted: number;
  rangerTagSyncsInstalled: number;
  rangerTagSyncsTotal: number;
  masterComponents: [];
  slaveComponents: [];
  rangerAdmins: HostComponent[];
  rangerHDFSPluginProperties: string;
  rangerHbasePluginProperties: string;
  rangerHivePluginProperties: string;
  rangerYarnPluginProperties: string;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;

  constructor(data: RangerServiceData) {
    //@ts-ignore
    super(data);
    this.rangerTagSyncsStarted = data.rangerTagSyncsStarted || 0;
    this.rangerTagSyncsInstalled = data.rangerTagSyncsInstalled || 0;
    this.rangerTagSyncsTotal = data.rangerTagSyncsTotal || 0;
    this.masterComponents = data.masterComponents || [];
    this.slaveComponents = data.slaveComponents || [];
    this.rangerAdmins = data.rangerAdmins || [];
    this.rangerHDFSPluginProperties = data.rangerHDFSPluginProperties || "";
    this.rangerHbasePluginProperties = data.rangerHbasePluginProperties || "";
    this.rangerHivePluginProperties = data.rangerHivePluginProperties || "";
    this.rangerYarnPluginProperties = data.rangerYarnPluginProperties || "";
    this.alertsCount = data.alertsCount || 0;
    this.serviceState = data.serviceState || "";
    this.hasCriticalAlerts = data.hasCriticalAlerts || false;
    this.isRestartRequiredForService = data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
  }

  updateConfig(updates: Partial<RangerService>) {
    Object.assign(this, updates);
  }

  getServiceObject(): RangerService {
    return this;
  }
}

export default RangerService;
