<?php

  define("OPT", "opts");
  define("STAGE", "stage");
  define("SERVICESTATE", "service_state");

  class NodeManifest {
    private $node;

    private $roles;

    function __construct($n) {
      $this->node = $n;
      $this->roles = array();
    }

    public function setRoleStage($r, $stage) {
      $this->setRoleState($r, STAGE, $stage);
    }

    public function setRoleState($r, $stateKey, $stateValue) {
      if (!isset($this->roles[$r])) {
        $this->roles[$r] = array();
        $this->roles[$r][$stateKey] = $stateValue;
      } else {
        $this->roles[$r][$stateKey] = $stateValue;
      }
    }

    public function setRoleOpt($r, $optKey, $optVal) {
      if (!isset($this->roles[$r])) {
        $this->roles[$r] = array();
      }
      if (!isset($this->roles[$r][OPT])) {
        $this->roles[$r][OPT] = array();
        $this->roles[$r][OPT][$optKey] = $optVal;
      } else {
        $this->roles[$r][OPT][$optKey] = $optVal;
      }
    }

    private function generateStageChain() {
      $chain = "";
      $stages = array();
      foreach($this->roles as $roleName => $val) {
        $stages[] = $val[STAGE];
      }
      asort($stages, SORT_NUMERIC);
      $first = true;
      foreach ($stages as $s) {
        if ($first) {
          $first = false;
        } else {
          $chain = $chain . " -> ";
        }
        $chain = $chain . "stage{" . $s . " :}";
      }
      return $chain;
    }

    private function generateOptList($optList) {
      $optChain = "";
      $first = true;
      foreach($optList as $key => $val) {
        if ($first) {
          $first = false;
        } else {
          $optChain = $optChain . ", ";
        }
        $optChain = $optChain . $key . " => " . $val ;
      }
      return $optChain;
    }

    private function generateRoleLine($r) {
      $roleLine = "";
      $roleLine = $roleLine . "class {'" . $r . "': ";
      $isFirst = true;
      if (isset($this->roles[$r][OPT])) {
        $roleLine = $roleLine . OPT . " => {";
        $roleLine = $roleLine . $this->generateOptList($this->roles[$r][OPT]);
        $roleLine = $roleLine . "}";
        $isFirst = false;
      }
      foreach($this->roles[$r] as $key => $value) {
        if ($key != OPT)  {
          if (!$isFirst) {
            $roleLine = $roleLine . ", ";
          } else {
            $isFirst = false;
          }
          $roleLine = $roleLine . $key . " => " . $value;
        }
      }
      $roleLine = $roleLine . "}";
      return $roleLine;
    }

    public function generateNodeManifest() {
      $manifest = "node /" . $this->node . "/ {\n";
      $manifest = $manifest . $this->generateStageChain();
      $manifest = $manifest . "\n";
      $stages = array();
      foreach($this->roles as $roleName => $val) {
        $stages[] = $val[STAGE];
      }
      asort($stages, SORT_NUMERIC);
      foreach($stages as $theStage) {
        //Print in the order of the stages
        foreach ($this->roles as $roleName => $roleVal) {
          if ($roleVal[STAGE] == $theStage) {
            $manifest = $manifest . $this->generateRoleLine($roleName);
            $manifest = $manifest . "\n";
          }
        }
      }
      $manifest = $manifest . "}\n";
      return $manifest;
    }


    public function setNamenodeRoleState($state, $stage) {
      $role = "hdp-hadoop::namenode";
      $this->setRoleState($role, SERVICESTATE, $state);
      $this->setRoleStage($role, $stage);
    }

    public function setSecondaryNamenodeRoleState($state, $stage) {
      $role = "hdp-hadoop::snamenode";
      $this->setRoleState($role, SERVICESTATE, $state);
      $this->setRoleStage($role, $stage);
    }

    public function setDatanodeRoleState($state, $stage) {
      $role = "hdp-hadoop::datanode";
      $this->setRoleState($role, SERVICESTATE, $state);
      $this->setRoleStage($role, $stage);
    }

    public function setJobTrackerRoleState($state, $stage) {
      $role = "hdp-hadoop::jobtracker";
      $this->setRoleState($role, SERVICESTATE, $state);
      $this->setRoleStage($role, $stage);
    }

    public function setTaskTrackerRoleState($state, $stage) {
      $role = "hdp-hadoop::tasktracker";
      $this->setRoleState($role, SERVICESTATE, $state);
      $this->setRoleStage($role, $stage);
    }

    public function setHadoopClientRoleState($state, $stage) {
      $role = "hdp-hadoop::client";
      $this->setRoleState($role, SERVICESTATE, $state);
      $this->setRoleStage($role, $stage);
    }

    public function setZooKeeperRoleState($state, $stage) {
      $role = "hdp-zookeeper";
      $this->setRoleState($role, SERVICESTATE, $state);
      $this->setRoleStage($role, $stage);
    }

    public function setZooKeeperClientRoleState($state, $stage) {
      $role = "hdp-zookeeper::client";
      $this->setRoleState($role, SERVICESTATE, $state);
      $this->setRoleStage($role, $stage);
    }

    public function setHbaseMasterRoleState($state, $stage) {
      $role = "hdp-hbase::master";
      $this->setRoleState($role, SERVICESTATE, $state);
      $this->setRoleStage($role, $stage);
    }

    public function setHbaseRegionServerRoleState($state, $stage) {
      $role = "hdp-hbase::regionserver";
      $this->setRoleState($role, SERVICESTATE, $state);
      $this->setRoleStage($role, $stage);
    }

    public function setHbaseClientRoleState($state, $stage) {
      $role = "hdp-hbase::client";
      $this->setRoleState($role, SERVICESTATE, $state);
      $this->setRoleStage($role, $stage);
    }
  }

/*** TEST CODE ********
  $nm = new NodeManifest("x.y.z");
  $nm->setRoleStage("namenode", "1");
  $nm->setRoleStage("datanode", "2");
  $nm->setRoleStage("namenode", "3");
  $nm->setRoleState("namenode", "service_state", "running");
  $nm->setRoleOpt("namenode", "deploy", "yes");
  $nm->setRoleOpt("namenode", "deploy", "no");
  $nm->setRoleOpt("namenode", "wait", "10");
  $nm->setRoleOpt("datanode", "running", "yes");
  $nm->setNamenodeRoleState("stopped", 4);
  $nm->setDatanodeRoleState("running", 5);
  $ret = $nm->generateNodeManifest();
  echo "$ret\n";
***/

?>
