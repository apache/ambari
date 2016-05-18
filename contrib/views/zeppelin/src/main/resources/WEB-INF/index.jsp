<!DOCTYPE html>
<%--
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
--%>
<html lang="en">
<head>
    <meta charset="utf-8"/>
</head>
<body>

<iframe id='zeppelinIFrame' width="100%" seamless="seamless" style="border: 0px;"></iframe>
<script>
    var $ = jQuery = parent.jQuery;
    var iframe = document.querySelector('#zeppelinIFrame');
    var port = "${port}";
    var publicName = "${publicname}";


    $.getJSON('/api/v1/clusters', function (data) {
        $.getJSON('/api/v1/clusters/' +
                data['items'][0]['Clusters']['cluster_name'] +
                '/hosts?fields=Hosts%2Fpublic_host_name%2Chost_components%2FHostRoles%2Fcomponent_name',
                function (data) {
                    for (var i = 0; i < data['items'].length; i++) {
                        for (var j = 0; j < data['items'][i]['host_components'].length; j++) {
                            if (data['items'][i]['host_components'][j]['HostRoles']['component_name'] == 'ZEPPELIN_MASTER') {
                                var url = '//' + data['items'][i]['host_components'][j]['HostRoles']['host_name'] + ':' + port;
                                iframe.src = url;
                                iframe.height = window.innerHeight;
                                return;
                            }
                        }
                    }
                });
    });

    $(window).resize(function () {
        iframe.height = window.innerHeight;
    });
</script>
</body>
</html>