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

App.UpdateController = Em.Controller.extend({
  name:'updateController',
  isUpdated:false,
  getUrl:function (testUrl, url) {
    return (App.testMode) ? testUrl : App.apiPrefix + '/clusters/' + this.get('clusterName') + url;
  },

  updateServiceMetric:function(){

    var servicesUrl1 = this.getUrl('/data/dashboard/services.json', '/services?ServiceInfo/service_name!=MISCELLANEOUS&ServiceInfo/service_name!=DASHBOARD&fields=*,components/host_components/*');
    var servicesUrl2 = this.getUrl('/data/dashboard/serviceComponents.json', '/services?ServiceInfo/service_name!=MISCELLANEOUS&ServiceInfo/service_name!=DASHBOARD&fields=components/ServiceComponentInfo');


    self = this;
    this.set("isUpdated", false);

    var metricsJson = null;
    var serviceComponentJson = null;
    var metricsMapper = {
      map:function (data) {
        metricsJson = data;
      }
    };
    var serviceComponentMapper = {
      map:function (data) {
        serviceComponentJson = data;
        if (metricsJson != null && serviceComponentJson != null) {
          var hdfsSvc1 = null;
          var hdfsSvc2 = null;
          var mrSvc1 = null;
          var mrSvc2 = null;
          var hbaseSvc1 = null;
          var hbaseSvc2 = null;
          metricsJson.items.forEach(function (svc) {
            if (svc.ServiceInfo.service_name == "HDFS") {
              hdfsSvc1 = svc;
            }
            if (svc.ServiceInfo.service_name == "MAPREDUCE") {
              mrSvc1 = svc;
            }
            if (svc.ServiceInfo.service_name == "HBASE") {
              hbaseSvc1 = svc;
            }
          });
          serviceComponentJson.items.forEach(function (svc) {
            if (svc.ServiceInfo.service_name == "HDFS") {
              hdfsSvc2 = svc;
            }
            if (svc.ServiceInfo.service_name == "MAPREDUCE") {
              mrSvc2 = svc;
            }
            if (svc.ServiceInfo.service_name == "HBASE") {
              hbaseSvc2 = svc;
            }
          });
          var nnC1 = null;
          var nnC2 = null;
          var jtC1 = null;
          var jtC2 = null;
          var hbm1 = null;
          var hbm2 = null;
          if (hdfsSvc1) {
            hdfsSvc1.components.forEach(function (c) {
              if (c.ServiceComponentInfo.component_name == "NAMENODE") {
                nnC1 = c;
              }
            });
          }
          if (hdfsSvc2) {
            hdfsSvc2.components.forEach(function (c) {
              if (c.ServiceComponentInfo.component_name == "NAMENODE") {
                nnC2 = c;
              }
            });
          }
          if (mrSvc1) {
            mrSvc1.components.forEach(function (c) {
              if (c.ServiceComponentInfo.component_name == "JOBTRACKER") {
                jtC1 = c;
              }
            });
          }
          if (mrSvc2) {
            mrSvc2.components.forEach(function (c) {
              if (c.ServiceComponentInfo.component_name == "JOBTRACKER") {
                jtC2 = c;
              }
            });
          }
          if (hbaseSvc1) {
            hbaseSvc1.components.forEach(function (c) {
              if (c.ServiceComponentInfo.component_name == "HBASE_MASTER") {
                hbm1 = c;
              }
            });
          }
          if (hbaseSvc2) {
            hbaseSvc2.components.forEach(function (c) {
              if (c.ServiceComponentInfo.component_name == "HBASE_MASTER") {
                hbm2 = c;
              }
            });
          }
          if (nnC1 && nnC2) {
            nnC1.ServiceComponentInfo = nnC2.ServiceComponentInfo;
          }
          if (jtC1 && jtC2) {
            jtC1.ServiceComponentInfo = jtC2.ServiceComponentInfo;
          }
          if (hbm1 && hbm2) {
            hbm1.ServiceComponentInfo = hbm2.ServiceComponentInfo;
          }
          App.updateMapper.map(metricsJson);

        }
      }
    }
    App.HttpClient.get(servicesUrl1, metricsMapper, {
      complete:function (jqXHR, textStatus) {
        App.HttpClient.get(servicesUrl2, serviceComponentMapper, {
          complete:function (jqXHR, textStatus) {
            self.set("isUpdated", true);
          }
        });
      }
    });
  }


});
