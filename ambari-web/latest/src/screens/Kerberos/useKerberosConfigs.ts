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

import { useEffect, useState } from "react";
import { kerberosIdentities } from "./Kerberos_identitites";

type KerberosIdentity = {
    name: string;
    displayName: string;
    category: string;
    filename: string;
    index: number;
};

function useKerberosConfigs() {
    const [kerberosIdentitiesMap, setKerberosIdentitiesMap] = useState<Record<string, KerberosIdentity>>({});
    const configTagFromFilenameMap: Record<string, string> = {};

    useEffect(() => {
        const map: Record<string, KerberosIdentity> = {};
        kerberosIdentities.forEach(c => {
          map[configId(c.name, c.filename)] = c;
        });
        setKerberosIdentitiesMap(map);
    }, [kerberosIdentities]);

    const configId = (name: string, filename: string): string => {
        return name + "__" + getConfigTagFromFileName(filename);
    };

    const getConfigTagFromFileName = (filename: string): string => {
        if (configTagFromFilenameMap[filename]) 
            return configTagFromFilenameMap[filename];

        const ret = filename.endsWith('.xml') ? filename.slice(0, -4) : filename;
        configTagFromFilenameMap[filename] = ret;
        return ret;
    };

    return { configId, getConfigTagFromFileName, kerberosIdentitiesMap };
}

export default useKerberosConfigs;
