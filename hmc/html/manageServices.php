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
    <script src="/hmc/js/ext/jquery.min.js"></script>
  </head>

  <body class="yui3-skin-sam">
    <?php require "./topnav.htmli"; ?>

    <div id="ContentDivId">
      <?php require "./subnav.htmli"; ?>
      <?php require "./utils.htmli"; ?>

      <!-- Service Management: The Main Event -->
      <div id="serviceManagementCoreDivId">
        <ul id="serviceManagementListId">
          <div id="serviceManagementDynamicRenderDivId">
            <!--
            <li class="serviceManagementEntry serviceManagementEntryStopped">
              <div>
                <span class="serviceManagementEntryNameContainer">
                  <a href="javascript:void(null)" class="serviceManagementEntryName">HDFS</a>
                </span>
                <div class="serviceManagementEntryActionsContainer">
                  <a href="javascript:void(null)" title="Start" 
                     class="serviceManagementEntryAction serviceManagementEntryActionStart"></a>
                  <a href="javascript:void(null)" title="Stop" 
                     class="serviceManagementEntryAction serviceManagementEntryActionStop"></a>
                  <a href="javascript:void(null)" title="Reconfigure" 
                     class="serviceManagementEntryAction serviceManagementEntryActionReconfigure"></a>
                </div>
              </div>
            </li>
            <li class="serviceManagementEntry serviceManagementEntryStarted">
              <div>
                <span class="serviceManagementEntryNameContainer">
                  <a href="javascript:void(null)" class="serviceManagementEntryName">MapReduce</a>
                </span>
                <div class="serviceManagementEntryActionsContainer">
                  <a href="javascript:void(null)" title="Start" 
                     class="serviceManagementEntryAction serviceManagementEntryActionStart"></a>
                  <a href="javascript:void(null)" title="Stop" 
                     class="serviceManagementEntryAction serviceManagementEntryActionStop"></a>
                  <a href="javascript:void(null)" title="Reconfigure" 
                     class="serviceManagementEntryAction serviceManagementEntryActionReconfigure"></a>
                </div>
              </div>
            </li>
            <li class="serviceManagementEntry serviceManagementEntryStarted">
              <div>
                <span class="serviceManagementEntryNameContainer">
                  <a href="javascript:void(null)" class="serviceManagementEntryName">HBase</a>
                </span>
                <div class="serviceManagementEntryActionsContainer">
                  <a href="javascript:void(null)" title="Start" 
                     class="serviceManagementEntryAction serviceManagementEntryActionStart"></a>
                  <a href="javascript:void(null)" title="Stop" 
                     class="serviceManagementEntryAction serviceManagementEntryActionStop"></a>
                  <a href="javascript:void(null)" title="Reconfigure" 
                     class="serviceManagementEntryAction serviceManagementEntryActionReconfigure"></a>
                </div>
              </div>
            </li>
            -->
          </div>
        </ul>
        <div id="serviceManagementGlobalActionsDivId" style="display:none">
          <button class="btn btn-large" id="serviceManagementGlobalActionsStartAllButtonId" name="startAll" style="margin-right:10px"><i class="iconic-play" style="margin-right:10px"></i>Start All</button>
          <button class="btn btn-large" id="serviceManagementGlobalActionsStopAllButtonId" name="stopAll"><i class="iconic-stop" style="margin-right:10px"></i>Stop All</button>
        </div>

        <br/>

      </div>
      <!-- End of serviceManagementCoreDivId -->

      <?php require "./txnUtils.htmli"; ?>

    </div>
    <!-- End of contentDivId -->

    <?php require "./footer.htmli"; ?>

    <!-- Javascript Scaffolding -->
    <script type="text/javascript">
      /* Minimal data required to bootstrap manageServices.js. */
      var clusterName = '<?php echo $_GET['clusterName']; ?>';

      var jsFilesToLoad = [ 
        'js/utils.js',
        'js/txnUtils.js',
        'js/configureServicesUtils.js',
        'js/manageServices.js',
        'js/manageServicesProgress.js'
      ];
    </script>

    <?php require "./bootstrapJs.htmli"; ?>
    <!-- End of Javascript Scaffolding -->

  </body>
</html> 
