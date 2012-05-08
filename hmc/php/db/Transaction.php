<?php

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

include_once "OrchestratorDB.php";
include_once "../util/Logger.php";

class Transaction {

  public $txId;
  public $subTxId;
  public $parentSubTxId;

  private $logger;

  public function __construct($txId, $subTxId, $parentSubTxId) {
    $this->txId = $txId;
    $this->subTxId = $subTxId;
    $this->parentSubTxId = $parentSubTxId;
    $this->logger = new HMCLogger("Transaction:" . $this->toString());
  }

  public function toString() {
    return $this->txId."-".$this->subTxId."-".$this->parentSubTxId;
  }

  public function createSubTransaction() {
    return new Transaction($this->txId, ++$GLOBALS["SUB_TXN_ID"], $this->subTxId);
  }

  public function getNextSubTransaction() {
    return new Transaction($this->txId, ++$GLOBALS["SUB_TXN_ID"], $this->parentSubTxId);
  }
}

?>
