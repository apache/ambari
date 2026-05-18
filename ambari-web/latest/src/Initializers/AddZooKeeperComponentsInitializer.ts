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

import AddComponentConfigInitializer from "./AddComponentConfigInitializer.ts";
class AddZooKeeperComponentsInitializer extends AddComponentConfigInitializer {
    //@ts-ignore
    private initializeForProperties: any;
    constructor() {
        super();
        //@ts-ignore
        this.initializeForProperties = [
            'zookeeper.connect',
            'ha.zookeeper.quorum',
            'hbase.zookeeper.quorum',
            'instance.zookeeper.host',
            'templeton.zookeeper.hosts',
            'hive.cluster.delegation.token.store.zookeeper.connectString',
            'yarn.resourcemanager.zk-address',
            'hive.zookeeper.quorum',
            'storm.zookeeper.servers',
            'hadoop.registry.zk.quorum',
            'atlas.audit.hbase.zookeeper.quorum',
            'atlas.graph.index.search.solr.zookeeper-url',
            'atlas.graph.storage.hostname',
            'atlas.kafka.zookeeper.connect'
        ]
        this.defaultInitializers = Object.keys(this.defaultInitializers)
            .filter((key) => this.initializeForProperties.includes(key))
            .reduce((filteredInitializers, key) => {
                filteredInitializers[key] = this.defaultInitializers[key];
                return filteredInitializers;
            }, {} as Record<string, any>);

        this.defaultUniqueInitializers = Object.keys(this.defaultUniqueInitializers)
            .filter((key) => this.initializeForProperties.includes(key))
            .reduce((filteredInitializers, key) => {
                filteredInitializers[key] = this.defaultUniqueInitializers[key];
                return filteredInitializers;
            }, {} as Record<string, any>);
    }
}

export default AddZooKeeperComponentsInitializer;
