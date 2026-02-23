import Service from "./service";
import bytesToSize from "../Utils/numberUtils.ts";

type HostComponent = {
  componentName: string;
  hostName: string;
  haNameSpace?: string;
  clusterIdValue?: string;
  haStatus?: string;
};

type HDFSServiceData = {
  version: string;
  nameNode: HostComponent | null;
  snameNode: HostComponent | null;
  activeNameNodes: HostComponent[];
  standbyNameNodes: HostComponent[];
  nonActiveStandbyNamenodes: HostComponent[];
  datanodes: HostComponent[];
  zookeeperFailoverControllers: HostComponent[];
  dataNodesStarted: number;
  dataNodesInstalled: number;
  routersInstalled: number;
  dataNodesTotal: number;
  routersTotal: number;
  routersStarted: number;
  nfsGatewaysStarted: number;
  nfsGatewaysInstalled: number;
  nfsGatewaysTotal: number;
  namespaces:any;
  isNamespaceLoaded:boolean;
  journalNodes: HostComponent[];
  nameNodeStartTimeValues: { [key: string]: any };
  jvmMemoryHeapUsedValues: { [key: string]: any };
  jvmMemoryHeapMaxValues: { [key: string]: any };
  decommissionDataNodes: HostComponent[];
  liveDataNodes: HostComponent[];
  deadDataNodes: HostComponent[];
  capacityUsed: number;
  capacityTotal: number;
  liveRouters: HostComponent[];
  deadRouters: HostComponent[];
  capacityRemaining: number;
  capacityNonDfsUsed: number;
  dfsTotalBlocksValues: string;
  dfsCorruptBlocksValues: string;
  dfsMissingBlocksValues: string;
  dfsUnderReplicatedBlocksValues: string;
  dfsTotalFilesValues: string;
  workStatusValues: {};
  healthStatusValues: { [key: string]: string };
  safeModeStatus: string;
  upgradeFinalized: boolean;
  nameNodeRpcValues: { [key: string]: any };
  metricsNotAvailable: boolean;
  hostComponents: HostComponent[];
  isNameNodeHaEnabled?: boolean;
  percentDFSUsed: string;
  diskPartDFSUsed: string;
  percentDFSRemaining: string;
  diskPartDFSRemaining: string;
  percentNonDFSUsed: string;
  diskPartNonDFSUsed: string;
  diskPartNamenodeHeap: string;
  percentNamenodeHeap: string;
  namenodeUptime: string;
  masterComponents: [];
  slaveComponents: [];
  clientComponents: [];
  liveNodesDataNodes: number;
  deadNodesDataNodes: number;
  decommissionedNodesDataNodes: number;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;
};

class HDFSService extends Service {
  version: string;
  nameNode: HostComponent | null;
  snameNode: HostComponent | null;
  activeNameNodes: HostComponent[];
  standbyNameNodes: HostComponent[];
  datanodes: HostComponent[];
  zookeeperFailoverControllers: HostComponent[];
  isNameNodeHaEnabled: boolean;
  dataNodesStarted: number;
  dataNodesInstalled: number;
  routersInstalled: number;
  dataNodesTotal: number;
  routersTotal: number;
  routersStarted: number;
  nfsGatewaysStarted: number;
  nfsGatewaysInstalled: number;
  nfsGatewaysTotal: number;
  journalNodes: HostComponent[];
  nameNodeStartTimeValues: { [key: string]: any };
  jvmMemoryHeapUsedValues: { [key: string]: any };
  jvmMemoryHeapMaxValues: { [key: string]: any };
  decommissionDataNodes: HostComponent[];
  liveDataNodes: HostComponent[];
  deadDataNodes: HostComponent[];
  capacityUsed: number;
  capacityTotal: number;
  liveRouters: HostComponent[];
  deadRouters: HostComponent[];
  capacityRemaining: number;
  capacityNonDfsUsed: number;
  dfsTotalBlocksValues: string;
  dfsCorruptBlocksValues: string;
  dfsMissingBlocksValues: string;
  dfsUnderReplicatedBlocksValues: string;
  dfsTotalFilesValues: string;
  workStatusValues: {};
  healthStatusValues: { [key: string]: string };
  // upgradeStatusValues: { [key: string]: any };
  safeModeStatus: string;
  upgradeFinalized: boolean;
  nameNodeRpcValues: { [key: string]: any };
  metricsNotAvailable: boolean;
  hostComponents: HostComponent[];
  percentDFSUsed: string;
  diskPartDFSUsed: string;
  percentDFSRemaining: string;
  diskPartDFSRemaining: string;
  percentNonDFSUsed: string;
  diskPartNonDFSUsed: string;
  diskPartNamenodeHeap: string;
  percentNamenodeHeap: string;
  namenodeUptime: string;
  masterComponents: [];
  slaveComponents: [];
  clientComponents: [];
  liveNodesDataNodes: number;
  deadNodesDataNodes: number;
  decommissionedNodesDataNodes: number;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;
  isNamespaceLoaded: boolean;
  namespaces:any;
  federationNamespaces: any[];
  nameNodeMetricsByHost?: { [key: string]: any };
  nameNodeMetricsByNamespace?: { [key: string]: any };

  constructor(data: HDFSServiceData) {
    //@ts-ignore
    super(data);
    this.version = data.version || "";
    this.nameNode = data.nameNode || null;
    this.snameNode = data.snameNode || null;
    this.activeNameNodes = data.activeNameNodes || [];
    this.standbyNameNodes = data.standbyNameNodes || [];
    this.datanodes = data.datanodes || [];
    this.zookeeperFailoverControllers = data.zookeeperFailoverControllers || [];
    this.isNameNodeHaEnabled = data.isNameNodeHaEnabled || false;
    this.dataNodesStarted = data.dataNodesStarted || 0;
    this.dataNodesInstalled = data.dataNodesInstalled || 0;
    this.routersInstalled = data.routersInstalled || 0;
    this.dataNodesTotal = data.dataNodesTotal || 0;
    this.routersTotal = data.routersTotal || 0;
    this.routersStarted = data.routersStarted || 0;
    this.nfsGatewaysStarted = data.nfsGatewaysStarted || 0;
    this.nfsGatewaysInstalled = data.nfsGatewaysInstalled || 0;
    this.nfsGatewaysTotal = data.nfsGatewaysTotal || 0;
    this.journalNodes = data.journalNodes || [];
    this.nameNodeStartTimeValues = data.nameNodeStartTimeValues || {};
    this.jvmMemoryHeapUsedValues = data.jvmMemoryHeapUsedValues || {};
    this.jvmMemoryHeapMaxValues = data.jvmMemoryHeapMaxValues || {};
    this.decommissionDataNodes = data.decommissionDataNodes || [];
    this.liveDataNodes = data.liveDataNodes || [];
    this.deadDataNodes = data.deadDataNodes || [];
    this.capacityUsed = data.capacityUsed || 0;
    this.capacityTotal = data.capacityTotal || 0;
    this.liveRouters = data.liveRouters || [];
    this.deadRouters = data.deadRouters || [];
    this.capacityRemaining = data.capacityRemaining || 0;
    this.capacityNonDfsUsed = data.capacityNonDfsUsed || 0;
    this.dfsTotalBlocksValues = data.dfsTotalBlocksValues || "N/A";
    this.dfsCorruptBlocksValues = data.dfsCorruptBlocksValues || "N/A";
    this.dfsMissingBlocksValues = data.dfsMissingBlocksValues || "N/A";
    this.dfsUnderReplicatedBlocksValues =
      data.dfsUnderReplicatedBlocksValues || "N/A";
    this.dfsTotalFilesValues = data.dfsTotalFilesValues || "N/A";
    this.workStatusValues = data.workStatusValues || {};
    this.healthStatusValues = this.calculateHealthStatusValues(
      data.workStatusValues
    );
    // this.upgradeStatusValues = data.upgradeStatusValues || {};
    this.safeModeStatus = data.safeModeStatus || "N/A";
    this.upgradeFinalized = data.upgradeFinalized || false;
    this.nameNodeRpcValues = data.nameNodeRpcValues || {};
    this.metricsNotAvailable = data.metricsNotAvailable || false;
    this.hostComponents = data.hostComponents || [];
    this.percentDFSUsed = data.percentDFSUsed || "N/A";
    this.diskPartDFSUsed = data.diskPartDFSUsed || "N/A";
    this.percentDFSRemaining = data.percentDFSRemaining || "N/A";
    this.diskPartDFSRemaining = data.diskPartDFSRemaining || "N/A";
    this.percentNonDFSUsed = data.percentNonDFSUsed || "N/A";
    this.diskPartNonDFSUsed = data.diskPartNonDFSUsed || "N/A";
    this.diskPartNamenodeHeap = data.diskPartNamenodeHeap || "N/A";
    this.percentNamenodeHeap = data.percentNamenodeHeap || "N/A";
    this.namenodeUptime = data.namenodeUptime || "N/A";
    this.masterComponents = data.masterComponents || [];
    this.slaveComponents = data.slaveComponents || [];
    this.clientComponents = data.clientComponents || [];
    this.liveNodesDataNodes = data.liveNodesDataNodes || 0;
    this.deadNodesDataNodes = data.deadNodesDataNodes || 0;
    this.decommissionedNodesDataNodes = data.decommissionedNodesDataNodes || 0;
    this.alertsCount = data.alertsCount || 0;
    this.serviceState = data.serviceState || "";
    this.hasCriticalAlerts = data.hasCriticalAlerts || false;
    this.isRestartRequiredForService = data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
    this.isNamespaceLoaded = data.isNamespaceLoaded || false;
    this.namespaces = data.namespaces || [];
    this.federationNamespaces = [];
  }

  get isNnHaEnabled(): boolean {
    return (
      !this.snameNode &&
      this.hostComponents.filter(
        (component) => component.componentName === "NAMENODE"
      ).length > 1
    );
  }

  private calculateHealthStatusValues(workStatusValues: {
    [key: string]: any;
  }): { [key: string]: string } {
    const healthStatusMap: any = {
      STARTED: "green",
      STARTING: "green-blinking",
      INSTALLED: "red",
      STOPPING: "red-blinking",
      UNKNOWN: "yellow",
    };
    return Object.keys(workStatusValues || {}).reduce((acc, key) => {
      return {
        ...acc,
        [key]: healthStatusMap[workStatusValues[key]] || "yellow",
      };
    }, {});
  }

  //@ts-ignore
  private findHealthStatusMapValueForSingleHost = (hostState: any) => {
    const healthStatusMap: any = {
      STARTED: "green",
      STARTING: "green-blinking",
      INSTALLED: "red",
      STOPPING: "red-blinking",
      UNKNOWN: "yellow",
    };
    return healthStatusMap[hostState] || "yellow";
  };

  updateConfig(updates: Partial<HDFSService>) {
    Object.assign(this, updates);
  }

  getServiceObject(): HDFSService {
    return this;
  }
  findCapacityPercentage(capacity: number, capacityTotal: number) {
    let percent =
      capacityTotal && capacity && capacityTotal > 0
        ? ((capacity * 100) / capacityTotal).toFixed(2)
        : 0;
    if (isNaN(<number>percent) || <number>percent < 0) {
      percent = "N/A";
    }
    return `${percent}%`;
  }

  diskPart(capacity: number, capacityTotal: number) {
    return `${bytesToSize(capacity, 1, "parseFloat")} / ${bytesToSize(capacityTotal, 1, "parseFloat")}`;
  }

  timingFormat(time: any) {
    if (!time) {
      return null;
    }

    time = parseInt(time);
    const fullTime = time;
    let duration = "";

    if (time === 0) {
      return "0s";
    }

    const oneSecMs = 1000;
    const oneMinMs = 60000;
    const oneHourMs = 3600000;
    const oneDayMs = 86400000;
    let days, hours, minutes, seconds;

    [days, time] = this.extractTimeUnit(time, oneDayMs, "d");
    [hours, time] = this.extractTimeUnit(time, oneHourMs, "h");
    [minutes, time] = this.extractTimeUnit(time, oneMinMs, "m");
    duration += days + hours + minutes;
    if (fullTime < oneDayMs) {
      [seconds, time] = this.extractTimeUnit(time, oneSecMs, "s");
      duration += seconds;
      if (fullTime < oneSecMs) {
        duration += "1s";
      }
    }
    return duration.trim();
  }

  extractTimeUnit(time: any, unitValue: any, unitSuffix: any) {
    let result = "";
    if (time >= unitValue) {
      result = Math.floor(time / unitValue) + `${unitSuffix} `;
      time -= Math.floor(time / unitValue) * unitValue;
    }
    return [result, time];
  }

  findSafeModeStatus(safeModeStatusValue: any) {
    const safeMode = safeModeStatusValue;
    if (safeMode == null) {
      return "n/a";
    } else if (safeMode.length === 0) {
      return "Not in safe mode";
    } else {
      return "In safe mode";
    }
  }

  findUpgradeStatus(upgradeStatusValue: any, healthStatus: any) {
    const upgradeStatus = upgradeStatusValue;
    if (upgradeStatus) {
      return "No pending upgrade";
    } else if (upgradeStatus === false && healthStatus === "green") {
      return "Upgrade not finalized";
    } else {
      // upgrade status == null
      return "n/a";
    }
  }

  countKeysMatchingPattern(data: any): number {
    //console.log("Data inside:", data);
    let matchingKeys: string[] = [];
    try {
      data = JSON.parse(data);
      const pattern = /\.visa\.com:\d+$/;
      const keys = Object.keys(data);
      //console.log("All keys:", keys);
      matchingKeys = keys.filter((key) => pattern.test(key));
      //console.log("Matching keys:", matchingKeys);
    } catch (e) {
      console.error("Error parsing data:", e);
    }
    return matchingKeys.length;
  }
}

export default HDFSService;
