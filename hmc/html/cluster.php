<html>
  <head>
    <title id="pageTitleId">Hortonworks Data Platform Portal</title>

    <?php require "./css_includes.html"; ?>
  </head>

  <body>
    <?php require "./header.html"; ?>
    <hr/>

    <div name="ContentDiv" id="ContentDivId" class="yui3-skin-sam"> 

      <!-- Navigation bar -->
      <div name="navigationBarDiv" id="navigationBarDivId">
        <div name="navigationBarContentDiv" id="navigationBarContentDivId">
        <a href="index.php">Clusters</a>
        </div>
      <hr/>
      </div>
 
      <div name="displayServiceStatusCoreDiv" id="displayServiceStatusCoreDivId" style="display:block">
        <div name="displayServiceStatusContentDiv" id="displayServiceStatusContentDivId" >
        </div>
      </div>
    </div>

    <script src="http://yui.yahooapis.com/3.4.1/build/yui/yui-min.js"></script>
    <!-- <script src="../js/global.js"></script> -->
    <script src="../js/serviceStatus.js"></script>
    <script>
       getServicesStatus('<?php echo $_GET['clusterId']; ?>');
    </script>

    <?php echo("hello"); ?>
    <?php echo("<a href='displayInstallLogs.php?clusterId=" . $_GET['clusterId'] . "'>Installation logs</a>"); ?>
  </body>
  <hr/>
  <?php require "./footer.html"; ?>
</html>

