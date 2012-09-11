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

App.InstallerStep7Controller = Em.ArrayController.extend({

  name: 'installerStep7Controller',

  content: [
    Em.Object.create({
      serviceName: 'HDFS',
      configCategories: [ 'General', 'NameNode', 'SNameNode', 'DataNode', 'Advanced' ],
      configs: [{
        name: 'dfs.prop1',
        displayName: 'Prop1',
        value: '',
        defaultValue: '100',
        category: 'General'
      }, {
        name: 'dfs.prop2',
        displayName: 'Prop2',
        value: '',
        defaultValue: '0',
        category: 'General'
      }, {
        name: 'dfs.adv.prop1',
        displayName: 'Adv Prop1',
        value: '',
        defaultValue: '100',
        category: 'Advanced'
      }, {
        name: 'dfs.adv.prop2',
        displayName: 'Adv Prop2',
        value: '',
        defaultValue: '0',
        category: 'Advanced'
      }, {
        name: 'dfs.namenode.dir',
        displayName: 'NameNode directories',
        value: '',
        defaultValue: '',
        category: 'NameNode'
      }, {
        name: 'dfs.namenode.prop1',
        displayName: 'NameNode Prop1',
        value: '',
        defaultValue: 'default (nn)',
        category: 'NameNode'
      }, {
        name: 'fs.checkpoint.dir',
        displayName: 'SNameNode directories',
        value: '',
        defaultValue: '',
        category: 'SNameNode'
      }, {
        name: 'fs.checkpoint.prop1',
        displayName: 'SNameNode Prop1',
        value: '',
        defaultValue: 'default (snn)',
        category: 'SNameNode'
      }, {
        name: 'dfs.data.dir',
        displayName: 'DataNode directories',
        value: '',
        defaultValue: '',
        category: 'DataNode'
      }, {
        name: 'dfs.data.prop1',
        displayName: 'DataNode Prop1',
        value: '',
        defaultValue: 'default (dn)',
        category: 'DataNode'
      }]
    }),
    Em.Object.create({
      serviceName: 'MapReduce',
      configCategories: [ 'General', 'JobTracker', 'TaskTracker', 'Advanced' ],
      configs: [{
        name: 'mapred.prop1',
        displayName: 'Prop1',
        value: '',
        defaultValue: '0',
        category: 'General'
      }, {
        name: 'jt.prop1',
        displayName: 'JT Prop1',
        value: '',
        defaultValue: '128',
        category: 'JobTracker'
      }, {
        name: 'tt.prop1',
        displayName: 'TT Prop1',
        value: '',
        defaultValue: '256',
        category: 'TaskTracker'
      }, {
        name: 'mapred.adv.prop1',
        displayName: 'Adv Prop1',
        value: '',
        defaultValue: '1024',
        category: 'Advanced'
      }]
    })
  ]

});
