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

import { HostsBasedInitializer } from "./HostsBasedInitializer.ts";

type ConfigProperty = {
    name: string;
    value: string | number;
    filename: string;
    recommendedValue?: string | number;
};

type InitializerType = {
    name: string;
    method: string;
};

type LocalDB = Record<string, any>;
type Dependencies = Record<string, any>;

class ConfigInitializer extends HostsBasedInitializer {
    constructor() {
        super(); // Call the superclass constructor
        this.initializerTypes = [
            // ...super.initializerTypes, // Include initializerTypes from the superclass
            { name: "host_with_port", method: "_initAsHostWithPort" },
            { name: "hosts_with_port", method: "_initAsHostsWithPort" },
            { name: "host_with_component", method: "_initAsHostWithComponent" },
            { name: "hosts_with_components", method: "_initAsHostsWithComponents" },
            {
                name: "hosts_list_with_component",
                method: "_initAsHostsListWithComponent",
            },
        ];
    }

    private _initializerFlowCode = {
        next: 0,
        skipNext: 1,
        skipAll: 2,
    };

    private defaultInitializers: Record<string, any> = {};
    private defaultUniqueInitializers: Record<string, any> = {};
    public initializerTypes: InitializerType[] = [];

    public flowNext(): number {
        return this._initializerFlowCode.next;
    }

    public flowSkipNext(): number {
        return this._initializerFlowCode.skipNext;
    }

    public flowSkipAll(): number {
        return this._initializerFlowCode.skipAll;
    }

    public initialValue(
        configProperty: ConfigProperty,
        localDB: LocalDB,
        dependencies: Dependencies
    ): ConfigProperty {
        const configName = configProperty.name;
        const initializer = this.defaultInitializers[configName];

        if (initializer) {
            return this._defaultInitializer(configProperty, localDB, dependencies);
        }

        const uniqueInitializer = this.defaultUniqueInitializers[configName];
        if (uniqueInitializer) {
            uniqueInitializer(configProperty, localDB, dependencies);
        }

        configProperty.recommendedValue = configProperty.value;
        return configProperty;
    }

    private _defaultInitializer(
        configProperty: ConfigProperty,
        localDB: LocalDB,
        dependencies: Dependencies
    ): ConfigProperty {
        const initializer = this.defaultInitializers[configProperty.name];
        if (initializer) {
            const initializerArray = Array.isArray(initializer)
                ? initializer
                : [initializer];
            for (const init of initializerArray) {
                const initializerType = this.initializerTypes.find(
                    (type) => type.name === init.type
                );
                if (initializerType) {
                    const method = HostsBasedInitializer[
                        initializerType.method as keyof typeof HostsBasedInitializer
                        ] as any;
                    if (typeof method === "function") {
                        configProperty = method(
                            configProperty,
                            localDB,
                            dependencies,
                            init
                        );
                    }
                }
            }
        }
        return configProperty;
    }
}

export default ConfigInitializer;
