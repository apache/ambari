/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
'use strict';
module.exports = function (components) {
  var nodes = components.filter(function (c) { return c.componentName === 'NAMENODE'; });
  if (nodes.length < 2 || nodes.length > 3) { throw new Error('Expected two or three NameNodes.'); }
  var active = nodes.filter(function (c) { return c.haStatus === 'active'; });
  if (active.length !== 1) { throw new Error('Exactly one reported Active NameNode is required. Refresh the page.'); }
  nodes.forEach(function (c) {
    if (c.workStatus !== 'STARTED' || c.staleConfigs || ['active', 'standby'].indexOf(c.haStatus) < 0) {
      throw new Error('All NameNodes must be running with known roles and no stale configurations.');
    }
    var zkfc = components.filter(function (z) { return z.componentName === 'ZKFC' && z.hostName === c.hostName; });
    if (zkfc.length !== 1 || zkfc[0].workStatus !== 'STARTED' || zkfc[0].staleConfigs) {
      throw new Error('Every NameNode must have a running ZKFC with no stale configurations.');
    }
  });
  return {source: active[0].hostName, targets: nodes.filter(function (c) { return c.haStatus === 'standby'; }).map(function (c) { return c.hostName; })};
};
