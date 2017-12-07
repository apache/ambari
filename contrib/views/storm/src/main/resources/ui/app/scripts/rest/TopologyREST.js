/**
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at
*
     http://www.apache.org/licenses/LICENSE-2.0
*
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
**/

import fetch from 'isomorphic-fetch';
import {baseUrl} from '../utils/Constants';

const topology = 'topology';
const cluster = 'cluster';

const TopologyREST = {
  getSummary(entity,options) {
    options = options || {};
    options.method = options.method || 'GET';
    return this.requestCall(baseUrl+entity+'/summary', options);
  },
  getClusterConfig(options) {
    options = options || {};
    options.method = options.method || 'GET';
    return this.requestCall(baseUrl+'cluster/configuration', options);
  },
  getTopologyGraphData(id,windowSize,options) {
    options = options || {};
    options.method = options.method || 'GET';
    return this.requestCall(baseUrl+'topology/'+id+'/visualization?window='+windowSize, options);
  },
  getTopologyDetails(id,windowSize,systemFlag, options){
    options = options || {};
    options.method = options.method || 'GET';
    let url = baseUrl+'topology/'+id+'?window='+windowSize;
    if(systemFlag !== '' && systemFlag !== undefined){
      url += '&sys='+systemFlag;
    }
    return this.requestCall(url, options);
  },
  getTopologyComponentDetail(TopId, CompName,windowSize,systemFlag, options){
    options = options || {};
    options.method = options.method || 'GET';
    let url = baseUrl+'topology/'+TopId+'/component/'+CompName+'?window='+windowSize;
    if(systemFlag !== '' && systemFlag !== undefined){
      url += '&sys='+systemFlag;
    }
    return this.requestCall(url, options);
  },
  getLogConfig(id,options) {
    options = options || {};
    options.method = options.method || 'GET';
    return this.requestCall(baseUrl+'topology/'+id+'/logconfig', options);
  },
  postLogConfig(id,options){
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    return this.requestCall(baseUrl+'topology/'+id+'/logconfig', options);
  },
  postDebugTopology(id,type,percent,options){
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    return this.requestCall(baseUrl+'topology/'+id+'/debug/'+type+'/'+percent,options);
  },
  postActionOnTopology(id,type,waitTime,options){
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    let url = baseUrl+'topology/'+id+'/'+type;
    if(!!waitTime){
      url += '/'+waitTime;
    }
    return this.requestCall(url,options);
  },
  getTopologyLag(id,options) {
    options = options || {};
    options.method = options.method || 'GET';
    return this.requestCall(baseUrl+'topology/'+id+'/lag', options);
  },
  getProfiling(id,type,hostPort,options) {
    options = options || {};
    options.method = options.method || 'GET';
    return this.requestCall(baseUrl+'topology/'+id+'/profiling/'+type+'/'+hostPort, options);
  },
  requestCall(url, options){
    let urlPart = url.split('url=')[0];
    let stormUrlPart = url.split('url=')[1];
    urlPart += 'url=' + encodeURIComponent(stormUrlPart);
    url = urlPart;
    options.credentials = 'same-origin';
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  }
};

export default TopologyREST;
