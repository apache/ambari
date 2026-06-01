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
import { faUser } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {useEffect, useState} from "react";
import {
  Container,
  Navbar,
  Nav,
  Dropdown,
  DropdownDivider,
  NavDropdown
} from "react-bootstrap";
import {
  decryptData,
  getFromLocalStorage,
  parseJSONData} from "./api/Utility.ts";
import { get }from "lodash";
import AmbariAboutModal from "./AmbariAboutModal";
import signOut from "./api/logout.ts";

type NavBarProps = {
  subPath: string;
  clusterName: string;
};

export default function NavBar({ subPath, clusterName }: NavBarProps) {
  const [showAmbariAboutModal, setShowAmbariAboutModal] = useState(false);
  const [loginUserName, setLoginUserName] = useState("");
  const [ambariLsVal, setAmbariLsVal] = useState(null);

  useEffect(() => {
    let ambariKey = getFromLocalStorage('ambari');
    if (ambariKey) {
      setAmbariLsVal(parseJSONData(decryptData(ambariKey)));
    }
  }, []);

  useEffect(() => {
    if (ambariLsVal) {
      const loginName = get(ambariLsVal, 'app.loginName');
      if (loginName) {
        setLoginUserName(loginName);
      }
    }
  }, [ambariLsVal]);

  return (
    <div>
      {showAmbariAboutModal ? (
        <AmbariAboutModal
          isOpen={showAmbariAboutModal}
          onClose={() => setShowAmbariAboutModal(false)}
        />
      ) : null}
      <Navbar collapseOnSelect expand="lg" className="bg-white">
        <Container className="d-flex justify-content-between">
          <Navbar.Brand
            className="text-black m-0 breadcrumb d-flex align-items-center"
            style={{ fontSize: 24 }}
          >
            {" "}
              Admin /
            <div className="navbar-text ms-1" style={{ fontSize: 24 }}>
              {subPath}
            </div>
          </Navbar.Brand>
          <div className="d-flex align-items-center ">
            <Nav.Link className="navbar-text navbar-size">
              {clusterName}
            </Nav.Link>
            <Dropdown>
              <Dropdown.Toggle
                variant="transparent"
                className="d-flex align-items-center border-0 ms-2"
              >
                <FontAwesomeIcon
                  icon={faUser}
                  className="me-1 navbar-text navbar-size"
                />
                <div className="navbar-text navbar-size">{loginUserName}</div>
              </Dropdown.Toggle>

              <Dropdown.Menu className="rounded-0">
                <Dropdown.Item
                  onClick={() => {
                    setShowAmbariAboutModal(true);
                  }}
                >
                  About
                </Dropdown.Item>
                <DropdownDivider />
                <NavDropdown.Item onClick={signOut}>Sign out</NavDropdown.Item>
              </Dropdown.Menu>
            </Dropdown>
          </div>
        </Container>
      </Navbar>
    </div>
  );
}
