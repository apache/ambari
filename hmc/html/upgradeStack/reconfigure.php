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
<!DOCTYPE html>
<html>
<head>
  <?php require "../_head.php" ?>
</head>

<body class="yui3-skin-sam">
  <div id="container">
    <?php require "../_topnav.php"; ?>
    <?php require "../_utils.php"; ?>
    <?php require "../_txnUtils.php"; ?>

    <div id="content">
      <?php require '_stageMenu.php'; ?>

      <!-- Begin Wizard -->
      <div id="upgradeStackWizard">
        <div id="reconfigurePanel">
          <div class="pageSummary" style="margin-top:0px">
            <h2><?php echo $RES['upgradeStack.reconfigure.pageSummary.header'] ?></h2>
            <p><?php echo $RES['upgradeStack.reconfigure.pageSummary.body'] ?></p>
          </div>
          <div id="formStatusDivId" class="alert alert-error" style="display:none">
          </div>
          <div id="configureClusterAdvancedCoreDivId">
            <form id="configureClusterAdvancedFormId">
              <fieldset id="configureClusterAdvancedFieldSetId">
                <div id="configureClusterAdvancedDynamicRenderDivId"></div>
              </fieldset>
            </form>
            <div id="buttonAreaDivId" class="clearfix">
              <div id="buttonGroupDivId">
                <?php /*
                            <div id="backNextDivId" class="btn-group">
                              <a href="javascript:void 0" class="btn btn-large" id="configureClusterAdvancedBackButtonId"><?php echo $RES['initWizard.configureClusterAdvanced.back.label'] ?></a>
                              <a href="javascript:void 0" class="btn btn-large" id="configureClusterAdvancedNextButtonId"><?php echo $RES['initWizard.configureClusterAdvanced.next.label'] ?></a>
                            </div>
                            */ ?>
                <a href="javascript:void 0" class="btn btn-large btn-success" id="reconfigureSubmitButton"><?php echo $RES['upgradeStack.reconfigure.submit.label'] ?></a>
              </div>
            </div>
          </div>
        </div>
      </div>
      <!-- End Wizard -->
    </div>
    <?php require "../_footer.php"; ?>
  </div>
  <!-- Javascript Scaffolding -->
  <script type="text/javascript">

    var clusterName = '<?php echo $clusterName; ?>';

    var jsFilesToLoad = [
      'js/ext/sinon.min.js',
      'js/utils.js',
      'js/txnUtils.js',
      'js/configureServicesUtils.js',
      'js/upgradeStack/reconfigure.js'
    ];
  </script>

  <?php require "../_bootstrapJs.php"; ?>
  <!-- End of Javascript Scaffolding -->

</body>
</html> 
