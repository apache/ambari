<?php
/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
*/
?>
<?php require_once "./head.inc" ?>
<html>
  <head>
    <?php require "./head.htmli" ?>
  </head>

  <body class="yui3-skin-sam">
    <?php require "./topnav.htmli"; ?>
      
    <div id="ContentDivId">
      <?php require "./subnav.htmli"; ?>
      <?php require "./utils.htmli"; ?>            

      <!-- List of clusters -->
      <div id="clustersListDivId" style="display:none">
      </div>

      <div id="clusterHostRoleMappingDivId" style="display:none">
        <div id="clusterHostRoleMappingDynamicRenderDivId">
        </div>      
      </div>

    </div>        
    <!-- End of contentDivId -->
    
    <?php require "./footer.htmli"; ?>

    <!-- Javascript Scaffolding -->
    <script type="text/javascript">

      var jsFilesToLoad = [ 
        'js/utils.js',
        'js/clustersList.js' 
      ];
    </script>

    <?php require "./bootstrapJs.htmli"; ?>
    <!-- End of Javascript Scaffolding -->
  </body>
</html> 
