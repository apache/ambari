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
    <link rel="stylesheet" href="/stylesheets/vendor.css">
</head>
<body>

<div class="container-fluid" id="messageContainer" style="display:none;">
    <h1>Welcome to the Zeppelin View</h1>
    <h3>Service check failed</h3>

    <table class="table">
        <tbody>
        <tr>
            <td>zeppelin service is not running</td>
        </tr>
        </tbody>
    </table>

</div>

<iframe id='zeppelinIFrame' width="100%" seamless="seamless" style="border: 0px;"></iframe>
<script>
var $ = jQuery = parent.jQuery;
var iframe = document.querySelector('#zeppelinIFrame');
var messageContainer = document.querySelector('#messageContainer');
var port = "${port}";
var publicName = "${publicName}";
var serviceCheckResponse = $.parseJSON('${serviceCheckResponse}');

if (serviceCheckResponse.status === "SUCCESS") {
    messageContainer.style.display = "none";
    iframe.style.display = "block";
    iframe.src = serviceCheckResponse.url;
    iframe.height = window.innerHeight;
} else {
    messageContainer.style.display = "block";
    iframe.style.display = "none";
}

$(window).resize(function () {
    iframe.height = window.innerHeight;
});
</script>
</body>
</html>
