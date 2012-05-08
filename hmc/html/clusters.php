<html>
  <head>
    <title id="pageTitleId">Hortonworks Data Platform Portal</title>

    <!-- CSS -->
    <link type="text/css" rel="stylesheet" href="http://yui.yahooapis.com/3.4.1/build/cssreset/cssreset-min.css"> 
    <link type="text/css" rel="stylesheet" href="../css/common.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common2.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common3.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/clusters.css" media="screen"/>
    <!-- End CSS -->
  </head>

  <body class="yui3-skin-sam">
    <?php require "./header.html"; ?>

    <hr/>

    <h1> Cluster <?php echo $_GET['clusterName']; ?> </h1>

    <br/>

    <div id="contentDivId"> 

      <!-- The Main Event -->
      <div id="clustersCoreDivId">
        <div id="clustersNavigationLinksDivId">
          <ul id="clustersNavigationLinksListId">
            <li class="clustersNavigationLinkEntry">
              <a href="../../hdp/dashboard/ui/home.html">
                Monitoring Dashboard
              </a>
            </li>
            <li class="clustersNavigationLinkEntry">
              <a href="../html/manageServices.php?clusterName=<?php echo $_GET['clusterName']; ?>">
                Start/Stop/Reconfigure Services
              </a>
            </li>
            <li class="clustersNavigationLinkEntry">
              <a href="../html/uninstallCluster.php?clusterName=<?php echo $_GET['clusterName']; ?>">
                Uninstall
              </a>
            </li>
          </ul>
        </div>

        <br/>

        <div id="clustersHostRoleMappingDynamicRenderDivId">
        </div>
      </div>
      <!-- End of clustersCoreDivId -->

      <div id="txnProgressCoreDivId" style="display:none">

        <!-- Used to render informational/status messages (error, success reports and such like) -->
        <div id="txnProgressStatusDivId" class="formStatusBar" style="display:none">
          <div id="txnProgressStatusMessageDivId"></div>
          <div id="txnProgressStatusActionsDivId"></div>
        </div>

        <fieldset id="txnProgressFieldSetId">
          <div id="txnProgressDynamicRenderDivId"></div>
        </fieldset>
      </div>
      <!-- End of txnProgressCoreDivId -->

      <!-- The mechanism by which we black out the screen and affix the
           the spotlight on a smaller portion of it. -->
      <div id="blackScreenDivId" style="display:none"></div>
      <!-- The image we use to let users know something is loading, and 
           that they should wait. -->
      <div id="loadingDivId"> 
        <div id="loadingBlackScreenDivId"></div>
        <img id="loadingImgId" src="../images/loadingLarge.gif"/>
      </div>

      <!-- Placeholder for our informational YUI panel. -->
      <div id="informationalPanelContainerDivId"></div>

    </div>
    <!-- End of contentDivId -->

    <hr/>
    <?php require "./footer.html"; ?>

    <!-- Javascript Scaffolding -->
    <script type="text/javascript">
      /* Minimal data required to bootstrap clusters.js. */
      var clusterName = '<?php echo $_GET['clusterName']; ?>';

      var jsFilesToLoad = [ 
        '../js/utils.js',
        '../js/txnUtils.js',
        '../js/clusters.js' 
      ];
    </script>

    <?php require "./bootstrapJs.htmli"; ?>
    <!-- End of Javascript Scaffolding -->

  </body>
</html> 
