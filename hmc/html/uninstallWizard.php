<?php require_once "./head.inc" ?>
<html>
  <head>
    <?php require "./head.htmli" ?>
  </head>

  <body class="yui3-skin-sam">
    <?php require "./topnav.htmli"; ?>
    <?php 
     include_once ("../php/util/clusterState.php"); 
    // $clusterName = "m4v1";
    // $clusterState = needWipeOut($clusterName); 
    ?>
    
    <div id="ContentDivId"> 
      <?php require "./subnav.htmli"; ?>

      <!-- Uninstallation Wizard -->
      <div id="installationWizardDivId" style="display:block">
        <?php require "./utils.htmli"; ?>
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
          <?php require "./txnUtils.htmli"; ?>

        </div>
      </div>
      <!-- End of Uninstallation Wizard -->
    </div>            
    <?php require "./footer.htmli"; ?>

    <!-- Javascript Scaffolding -->
    <script type="text/javascript">

    var freshInstall = false;
    var nodesAction = "uninstall";
    var clusterName = "<?php echo $_GET['clusterName']; ?>";

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

    <?php require "./bootstrapJs.htmli"; ?>
    <!-- End of Javascript Scaffolding -->

  </body>
</html> 
