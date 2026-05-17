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

type ConfigProperty = {
    value: string | number;
    recommendedValue?: string | number;
};

type TopologyLocalDB = Record<string, any>;
type Dependencies = Record<string, any>;
type Initializer = {
    type: string;
    isChecker: boolean;
    stackVersion?: string;
};

class ControlFlowInitializer {
    //@ts-ignore
    private initializerTypes = [
        { name: "namenode_ha_enabled", method: "_initNameNodeHACheck" },
        {
            name: "resourcemanager_ha_enabled",
            method: "_initResourceManagerHACheck",
        },
        { name: "hdp_stack_version_checker", method: "_initHDPStackVersionCheck" },
    ];

    public static getHDPStackVersionControl(
        minStackVersionNumber: string
    ): Initializer {
        return {
            type: "hdp_stack_version_checker",
            isChecker: true,
            stackVersion: minStackVersionNumber,
        };
    }

    //@ts-ignore
    private _initHDPStackVersionCheck(
        //@ts-ignore
        configProperty: ConfigProperty,
        //@ts-ignore
        localDB: TopologyLocalDB,
        //@ts-ignore
        dependencies: Dependencies,
        initializer: Initializer
    ): number {
        return this.compareVersions(
            this.getCurrentStackVersion(),
            initializer.stackVersion || ""
        ) > -1
            ? this.flowNext()
            : this.flowSkipAll();
    }

    public static getNameNodeHAControl(): Initializer {
        return { type: "namenode_ha_enabled", isChecker: true };
    }

    //@ts-ignore
    private _initNameNodeHACheck(
        //@ts-ignore
        configProperty: ConfigProperty,
        //@ts-ignore
        localDB: TopologyLocalDB,
        //@ts-ignore
        dependencies: Dependencies
    ): number {
        return this.isHaEnabled() ? this.flowNext() : this.flowSkipNext();
    }

    public static getResourceManagerHAControl(): Initializer {
        return { type: "resourcemanager_ha_enabled", isChecker: true };
    }

    //@ts-ignore
    private _initResourceManagerHACheck(
        //@ts-ignore
        configProperty: ConfigProperty,
        //@ts-ignore
        localDB: TopologyLocalDB,
        //@ts-ignore
        dependencies: Dependencies
    ): number {
        return this.isRMHaEnabled() ? this.flowNext() : this.flowSkipNext();
    }

    private flowNext(): number {
        return 0; // Represents "next"
    }

    private flowSkipNext(): number {
        return 1; // Represents "skipNext"
    }

    private flowSkipAll(): number {
        return 2; // Represents "skipAll"
    }

    private compareVersions(version1: string, version2: string): number {
        // Add logic to compare versions
        return version1.localeCompare(version2, undefined, { numeric: true });
    }

    private getCurrentStackVersion(): string {
        // Replace with logic to get the current stack version
        return "3.0.0";
    }

    private isHaEnabled(): boolean {
        // Replace with logic to check if HA is enabled
        return true;
    }

    private isRMHaEnabled(): boolean {
        // Replace with logic to check if ResourceManager HA is enabled
        return true;
    }
}

export default ControlFlowInitializer;