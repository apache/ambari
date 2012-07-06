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
<?php require_once '../_router.php' ?>
<html>
<head>
  <?php require "../_head.php" ?>
  <script src="/hmc/js/ext/jquery.min.js"></script>
</head>

<body class="yui3-skin-sam">
  <?php require "../_topnav.php"; ?>

  <div id="content">
    <?php require '_stageMenu.php'; ?>

    <!-- Begin Wizard -->
    <div id="upgradeStackWizard">
      <?php require "../_utils.php"; ?>
      <div id="deployPanel">
        <div class="pageSummary" style="margin-top:0px">
          <h2><?php echo $RES['upgradeStack.deploy.pageSummary.header'] ?></h2>
          <p><?php echo $RES['upgradeStack.deploy.pageSummary.body'] ?></p>
        </div>
        <div id="formStatusDivId" class="alert alert-error" style="display:none">
        </div>
        <form id="deployFormId">
          <fieldset id="deployFieldSetId">
            <!--<legend>Review your settings</legend>-->
            <div id="deployDynamicRenderDivId"></div>
          </fieldset>
        </form>
        <!--<a href="reconfigure.php" class="btn btn-large" id="backToReconfigureButton"><?php echo $RES['upgradeStack.deploy.backButton.label'] ?></a>-->
        <a href="javascript:void 0" class="btn btn-large btn-success" id="deploySubmitButton" style="margin:0 0 40px 320px"><?php echo $RES['upgradeStack.deploy.submit.label'] ?></a>
      </div>
      <?php require "../_txnUtils.php"; ?>
    </div>
    <!-- End Wizard -->
  </div>
  <?php require "../_footer.php"; ?>

  <!-- Javascript Scaffolding -->
  <script type="text/javascript">

    var clusterName = '<?php echo $clusterName; ?>';

    var jsFilesToLoad = [
      'js/ext/sinon.min.js',
      'js/utils.js',
      'js/txnUtils.js',
      'js/configureServicesUtils.js',
      'js/upgradeStack/deploy.js'
    ];
  </script>

  <?php require "../_bootstrapJs.php"; ?>
  <!-- End of Javascript Scaffolding -->

</body>
</html> 
