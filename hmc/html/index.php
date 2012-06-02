<?php require_once "./head.inc" ?>
<html>
  <head>
    <title id="pageTitleId"><?php echo $RES['page.title'] ?></title>

    <!-- CSS -->
    <link type="text/css" rel="stylesheet" href="../css/bootstrap.css" media="screen"/> 
    <link type="text/css" rel="stylesheet" href="../css/common.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common2.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common3.css" media="screen"/>
    <link rel="shortcut icon" href="../images/logo-micro.gif">
    <!-- End CSS -->
  </head>

  <body>
    <?php require "./topnav.htmli"; ?>

    <div id="ContentDivId" class="yui3-skin-sam">
      <div id="welcomeDivId" style="display:none">
        <?php require "./welcome.inc" ?>           
      </div>
      <!-- List of clusters -->
      <div id="clustersListDivId" style="display:none">
      </div>
    </div>        
    
        <!--
        <div name="installationSideBarDiv" id="installationSideBarDivId">
          <div id="createClusterSideBarId">Create cluster</div>
          <div id="selectServicesSideBarId">Select services</div>
          <div id="masterHostsSideBarId">Select master hosts</div>
          <div id="mountPointsSideBarId">Select mount points</div>
          <div id="advancedConfigSideBarId">Advanced configuration</div>
          <div id="reviewSettingsSideBarId">Review your settings</div>
        </div>
        -->

    <script src="http://yui.yahooapis.com/3.5.1/build/yui/yui-min.js"></script>
    <script src="../js/utils.js"></script>
    <script src="../js/clustersList.js"></script>

    <?php require "./footer.htmli"; ?>
  </body>
</html> 
