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
<?php require_once "./_router.php" ?>
<!DOCTYPE html>
<html>
  <head>
    <?php require "./_head.php" ?>
  </head>

  <body class="yui3-skin-sam">
    <div id="container">
      <?php require "./_topnav.php"; ?>

      <div id="content">
        <?php require "./_subnav.php"; ?>
        <?php require "./_utils.php"; ?>
        <?php require "./_txnUtils.php"; ?>

        <!-- Service Management: The Main Event -->
        <div id="serviceManagementCoreDivId">
          <ul id="serviceManagementListId">
            <div id="serviceManagementDynamicRenderDivId">
            </div>
          </ul>
          <div id="serviceManagementGlobalActionsDivId" style="display:none">
            <button class="btn btn-large" id="serviceManagementGlobalActionsStartAllButtonId" name="startAll" style="margin-right:10px"><i class="iconic-play" style="margin-right:10px"></i>Start All</button>
            <button class="btn btn-large" id="serviceManagementGlobalActionsStopAllButtonId" name="stopAll"><i class="iconic-stop" style="margin-right:10px"></i>Stop All</button>
          </div>
        </div>
        <!-- End of serviceManagementCoreDivId -->
      </div>

      <?php require "./_footer.php"; ?>
    </div>

    <!-- Javascript Scaffolding -->
    <script type="text/javascript">
      /* Minimal data required to bootstrap manageServices.js. */
      var clusterName = '<?php echo $clusterName; ?>';

      var jsFilesToLoad = [ 
        'js/utils.js',
        'js/txnUtils.js',
        'js/configureServicesUtils.js',
        'js/manageServices.js',
        'js/manageServicesProgress.js'
      ];
    </script>

    <?php require "./_bootstrapJs.php"; ?>
    <!-- End of Javascript Scaffolding -->

  </body>
</html> 
