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
<html>
  <head>
    <?php require "./_head.php" ?>
  </head>

  <body class="yui3-skin-sam">
    <?php require "./_topnav.php"; ?>
    <?php include_once "../php/util/clusterState.php"; ?>
    
    <div id="content">
      <?php require "./_subnav.php"; ?>

      <!-- Uninstallation Wizard -->
      <div id="installationWizardDivId" style="display:block">
        <?php require "./_utils.php"; ?>
        <div class="pageSummary" style="margin-top:0px">
          <h2><?php echo $RES['uninstallWizard.pageSummary.header'] ?></h2>
          <p><?php echo $RES['uninstallWizard.pageSummary.body'] ?></p>
        </div>
        <div id="installationMainFormsDivId">    
          <div id="formStatusDivId" class="alert alert-error" style="display:none">					
          </div>        
          <div class="pageContent">
            <div id="addNodesCoreDivId" style="display:block">
              <fieldset>
                <form id="addNodesFilesFormId" enctype="multipart/form-data" method="post">
                <input type="hidden" name="ClusterDeployUser" id="clusterDeployUserId" value="root" placeholder="">
                <label for="clusterDeployUserIdentityFileId"><?php echo $RES['common.sshPrivateKeyFile.label'] ?></label>
                <input type="file" name="clusterDeployUserIdentityFile" id="clusterDeployUserIdentityFileId" value="" placeholder="">
                <div class="separator"></div>
                <div id="fileUploadWrapperDivId">
                  <iframe name="fileUploadTarget" id="fileUploadTargetId" src="about:blank" style="display:none"></iframe>
                </div>              
                <div class="separator"></div>
                <div id="confirmWipeOutDivId">
                  <label class="checkbox" for="confirmWipeOutId"><?php echo $RES['uninstallWizard.wipeout.label'] ?>
                    <input type="checkbox" id="confirmWipeOutCheckId" value="false">
                  </label>
                </div>
                <div class="separator"></div>
                <input type="button" class="btn btn-large" id="addNodesSubmitButtonId" value="<?php echo $RES['uninstallWizard.submit.label'] ?>">
              </fieldset>
            </div>
          </div>
          <?php require "./_txnUtils.php"; ?>

        </div>
      </div>
      <!-- End of Uninstallation Wizard -->
    </div>            
    <?php require "./_footer.php"; ?>

    <!-- Javascript Scaffolding -->
    <script type="text/javascript">

    var freshInstall = false;
    var nodesAction = "uninstall";
    var clusterName = "<?php echo $clusterName; ?>";

    var InstallationWizard = {

      AddNodes: 
      {},
      AddNodesProgress:
      {}
    };

    var jsFilesToLoad = [ 
        'js/utils.js', 
        'js/txnUtils.js',
        'js/uninstall.js', 
        'js/uninstallProgress.js', 
      ];
    </script>

    <?php require "./_bootstrapJs.php"; ?>
    <!-- End of Javascript Scaffolding -->

  </body>
</html> 
