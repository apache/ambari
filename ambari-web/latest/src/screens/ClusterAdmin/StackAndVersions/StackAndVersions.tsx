/**
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

import { Tab, Tabs } from "react-bootstrap";
import { useNavigate, useParams } from "react-router-dom";
import { useState } from "react";
import ListStack from "./ListStack";

function StackAndVersions() {
  const { tabName } = useParams();
  const [selectedTab, setSelectedTab] = useState(tabName);
  const navigate = useNavigate();

  return (
    <div className="py-4 mx-5">
      <Tabs
        className="ambari-tabs"
        activeKey={selectedTab}
        onSelect={(tab: any) => {
          navigate(`/main/admin/stack/${tab}`);
          setSelectedTab(tab);
        }}
      >
        <Tab title="Stack" eventKey={"services"}>
          <ListStack />
        </Tab>
        <Tab title="Versions" eventKey={"versions"}>
          <h1>Versions.</h1>
        </Tab>
      </Tabs>
    </div>
  );
}

export default StackAndVersions;
