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

var App = require('app');
require('utils/configs/ha_config_initializer_class');
require('utils/configs/hosts_based_initializer_mixin');

/**
 * @typedef {topologyLocalDB} extendedTopologyLocalDB
 * @property {string[]} installedServices list of installed service names
 */

/**
 * Setting for <code>rename</code>-initializer
 * Used for configs which should be renamed
 * Replace some part if their names with <code>namespaceId</code> (provided by user on the wizard's 1st step)
 *
 * @param {string} toReplace
 * @returns {{type: string, toReplace: string}}
 */
function getRenameWithNamespaceConfig(toReplace) {
    return {
        type: 'rename',
        toReplace: toReplace
    };
}

/**
 * Initializer for configs that are updated when NameNode HA-mode is activated
 *
 * @class {MultipleNNConfigInitializer}
 */
App.MultipleNNConfigInitializer = App.HaConfigInitializerClass.create(App.HostsBasedInitializerMixin, {

    initializers: function () {

        return {
            'dfs.ha.namenodes.${dfs.nameservices}': getRenameWithNamespaceConfig('${dfs.nameservices}'),
            'dfs.namenode.rpc-address.${dfs.nameservices}.nn3': [this.getHostWithPortConfig('NAMENODE', false, '', '', '8020', false), getRenameWithNamespaceConfig('${dfs.nameservices}')],
            'dfs.namenode.http-address.${dfs.nameservices}.nn3': [this.getHostWithPortConfig('NAMENODE', false, '', '', '50070', false), getRenameWithNamespaceConfig('${dfs.nameservices}')],
            'dfs.namenode.https-address.${dfs.nameservices}.nn3': [this.getHostWithPortConfig('NAMENODE', false, '', '', '50470', false), getRenameWithNamespaceConfig('${dfs.nameservices}')]
        };
    }.property(),

    uniqueInitializers: {
        'xasecure.audit.destination.hdfs.dir': '_initXasecureAuditDestinationHdfsDir'
    },

    initializerTypes: [{ name: 'rename', method: '_initWithRename' }],

    /**
     * Initializer for configs that should be renamed
     * Some part of their names should be replaced with <code>namespaceId</code> (user input this value on the wizard's 1st step)
     * Affects both - name and displayName
     * <b>Important! It's not the same as <code>_updateInitializers</code>!</b>
     * Main diff - this initializer used for configs
     * with names that come with some "predicates" in their names. <code>_updateInitializers</code> is used to determine needed
     * config name that depends on other config values or someting else
     *
     * @param {configProperty} configProperty
     * @param {extendedTopologyLocalDB} localDB
     * @param {nnHaConfigDependencies} dependencies
     * @param {object} initializer
     * @returns {object}
     * @private
     * @method _initWithRename
     */
    _initWithRename: function (configProperty, localDB, dependencies, initializer) {
        var replaceWith = dependencies.namespaceId;
        var toReplace = initializer.toReplace;
        Em.assert('`dependencies.namespaceId` should be not empty string', !!replaceWith);
        var name = Em.getWithDefault(configProperty, 'name', '');
        var displayName = Em.getWithDefault(configProperty, 'displayName', '');
        name = name.replace(toReplace, replaceWith);
        displayName = displayName.replace(toReplace, replaceWith);
        Em.setProperties(configProperty, {
            name: name,
            displayName: displayName
        });
        return configProperty;
    },

    /**
     * Unique initializer for <code>xasecure.audit.destination.hdfs.dir</code>
     *
     * @param {configProperty} configProperty
     * @param {extendedTopologyLocalDB} localDB
     * @param {nnHaConfigDependencies} dependencies
     * @param {object} initializer
     * @method _initXasecureAuditDestinationHdfsDir
     * @return {object}
     * @private
     */
    _initXasecureAuditDestinationHdfsDir: function(configProperty, localDB, dependencies, initializer) {
        if (localDB.installedServices.contains('RANGER')) {
            var oldValue = dependencies.serverConfigs.findProperty('type', 'ranger-env').properties['xasecure.audit.destination.hdfs.dir'];
            // Example of value - hdfs://c6401.ambari.apache.org:8020/ranger/audit
            // Replace hostname and port with Namespace
            var valueArray = oldValue.split("/");
            valueArray[2] = dependencies.namespaceId;
            var newValue = valueArray.join("/");
            Em.setProperties(configProperty, {
                value: newValue,
                recommendedValue: newValue
            });
        }
        return configProperty;
    }

});