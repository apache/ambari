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
function renderCluster(cluster) {
  var buffer = [];
  var i=0;
  buffer[i++]='<a href="/status-cluster.html?cluster='+cluster['clusterName']+'">'+cluster['clusterName']+'</a>';
  buffer[i++]='<a href="'+cluster['nodes']['url']+'">Nodes List</a>';
  buffer[i++]='<a href="'+cluster['software']['url']+'">Software Stack</a>';
  buffer[i++]='<a href="'+cluster['config']['url']+'">Configuration</a>';
  return buffer;
}

function renderCommand(command) {
  var buffer = [];
  var i=0;
  buffer[i++]='<a href="/status-command.html?cmd='+command['id']+'">'+command['id']+'</a>';
  buffer[i++]=command['cmd'];
  return buffer;
}

function basename(path) {
    return path.replace(/\\/g,'/').replace( /.*\//, '' );
}

function dirname(path) {
    return path.replace(/\\/g,'/').replace(/\/[^\/]*$/, '');;
}

