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

System.register(['./datasource', './query_ctrl', './config_ctrl'], function(exports_1) {
    var datasource_1, query_ctrl_1, config_ctrl_1;
    return {
        setters:[
            function (datasource_1_1) {
                datasource_1 = datasource_1_1;
            },
            function (query_ctrl_1_1) {
                query_ctrl_1 = query_ctrl_1_1;
            },
            function (config_ctrl_1_1) {
                config_ctrl_1 = config_ctrl_1_1;
            }],
        execute: function() {
            exports_1("Datasource", datasource_1.AmbariMetricsDatasource);
            exports_1("QueryCtrl", query_ctrl_1.AmbariMetricsQueryCtrl);
            exports_1("ConfigCtrl", config_ctrl_1.AmbariMetricsConfigCtrl);
        }
    }
});
//# sourceMappingURL=module.js.map