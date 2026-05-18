/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

type SiteConfigs = Record<string, string>;
//@ts-ignore
type UniqueHostsListParser = {
    type: string;
    propertyName: string;
    method: (value: string) => string;
};

interface MasterComponent {
    component: string;
    hostName: string;
    isInstalled: boolean;
}

interface HiveHostsParams {
    fromDeleteHost: boolean;
    deleteHiveMetaStore: boolean;
    deleteHiveServer: boolean;
    deleteWebHCatServer: boolean;
    hiveMetastoreHost: string | null;
    contentHostName: string;
    bootstrapHostsMapping: (componentName: string) => MasterComponent[];
}

class UtilitiesForInitialziers {
    static updateHostsListValue(
        siteConfigs: SiteConfigs,
        propertyType: string,
        propertyName: string,
        hostsList: string,
        isArray: boolean
    ): string {
        const uniqueHostsListParsers = [
            {
                propertyName: "templeton.hive.properties",
                type: "webhcat-site",
                method: "getTempletonHiveHosts",
            },
        ];
        let value = hostsList;
        let propertyHosts = siteConfigs[propertyName] || "";
        let hostsToSet = hostsList;

        const parser = uniqueHostsListParsers.find(
            (property) =>
                property.type === propertyType && property.propertyName === propertyName
        );

        if (parser) {
            //@ts-ignore
            propertyHosts = parser.method(propertyHosts);
            //@ts-ignore
            hostsToSet = parser.method(hostsToSet);
        } else {
            if (isArray) {
                const pattern = /(^\[|]$)/g;
                propertyHosts = propertyHosts.replace(pattern, "");
                hostsToSet = hostsToSet.replace(pattern, "");
            }
            propertyHosts = propertyHosts.split(",").join(",");
            hostsToSet = hostsToSet.split(",").join(",");
        }

        if (siteConfigs[propertyName]) {
            const diffLength = propertyHosts
                .split(",")
                .filter((hostName) => !hostsToSet.split(",").includes(hostName)).length;

            if (
                diffLength === 0 &&
                propertyHosts.split(",").length === hostsToSet.split(",").length
            ) {
                value = siteConfigs[propertyName];
            }
        }

        siteConfigs[propertyName] = value;
        return value;
    }
    static getHiveHosts(params: HiveHostsParams): MasterComponent[] {
        const {
            fromDeleteHost,
            deleteHiveMetaStore,
            deleteHiveServer,
            deleteWebHCatServer,
            hiveMetastoreHost,
            // webhcatServerHost,
            // bootstrapHostsMapping,
        } = params;

        let removePerformed =
            fromDeleteHost ||
            deleteHiveMetaStore ||
            deleteHiveServer ||
            deleteWebHCatServer;

        const hiveMasterComponents = ["HIVE_METASTORE", "HIVE_SERVER"];
        let masterComponentsMap = hiveMasterComponents
            .map((componentName) =>
                UtilitiesForInitialziers.bootstrapHostsMapping(componentName)
            )
            .reduce((p, c) => p.concat(c), []);

        if (removePerformed) {
            masterComponentsMap = masterComponentsMap.map((masterComponent) => {
                //@ts-ignore
                masterComponent.isInstalled =
                    //@ts-ignore
                    masterComponent.hostName !== contentHostName;
                return masterComponent;
            });
        }

        if (hiveMetastoreHost) {
            masterComponentsMap.push({
                component: "HIVE_METASTORE",
                hostName: hiveMetastoreHost,
                isInstalled: !removePerformed,
            });
        }

        // if (webhcatServerHost) {
        //     masterComponentsMap.push({
        //         component: 'WEBHCAT_SERVER',
        //         hostName: webhcatServerHost,
        //         isInstalled: !removePerformed,
        //     });
        // }

        return masterComponentsMap;
    }

    //@ts-ignore
    static bootstrapHostsMapping(
        componentName: string,
        hostNames?: string[]
        //@ts-ignore
    ): HostMapping[] {
        if (!hostNames) {
            hostNames = [];
        }
        return hostNames.map((hostName) => ({
            component: componentName,
            hostName: hostName,
            isInstalled: true,
        }));
    }
}

export default UtilitiesForInitialziers;
