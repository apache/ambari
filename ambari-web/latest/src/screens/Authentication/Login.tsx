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

import {
  Alert,
  Button,
  Card,
  Col,
  Container,
  Form,
  Row,
  Image,
} from "react-bootstrap";
import React, { useEffect, useState } from "react";
import AmbariLogo from "../../assets/img/ambari-logo.png";
import "../../../custom.scss";
import { LocalStorageOps } from "../../Utils/LocalStorageOps";
import { useUserContext } from "../../store/UserContext";

export const Login = () => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  // Use the UserContext for authentication
  const { login, isLoading, loginError } = useUserContext();

  useEffect(()=>{
    if (loginError) {
      setErrorMessage(loginError);
    }
  },[loginError])

  const handleSignIn = async (event: React.FormEvent) => {
    event.preventDefault();
    setErrorMessage("");

    try {
      // Use the UserContext login method
      const success = await login(username, password);
      console.log("Login success:", success);
      if (success) {
        // Redirect after successful login
        const hashExclusiveUrl = LocalStorageOps.getItem(
          "lastVisitedURL"
        )?.replace("/#", "");

        if (hashExclusiveUrl) {
          window?.location?.replace("/#" + hashExclusiveUrl);
        } else {
          window.location.replace("/#");
        }
        window.location.reload();
      } else {
        setErrorMessage(loginError || "Login failed. Please check your credentials.");
      }
    } catch (error: any) {
      setErrorMessage(loginError || "An unexpected error occurred during login.");
    }
  };
  // const handleLoginSuccess = async (params: LoginParams) => {
  //   try {
  //     const response = await LoginApi.handleSuccessfulLogin(params);
  //     await afterLoginSuccess(response.data, params as any);
  //     setErrorMessage("");
  //   } catch (error) {
  //     handleLoginError(error);
  //   }
  // };
  // const afterLoginSuccess = async (data: dataLoginType, params: LoginDataParamsType) => {
  //   try {
  //     await LoginApi.afterLoginSuccessCallback(data);
  //     // if (afterLoginSuccessCallbackResponse.status !== 200) {
  //     //   throw new Error("after login success callback failed");
  //     // }
  //     await setClusterData(params);
  //   } catch (error) {
  //     await setClusterData(params);
  //     handleLoginError(error);
  //   }
  // }
  // const setClusterData = async (params: LoginDataParamsType) => {
  //   try {
  //     const response = await LoginApi.setClusterDataCallback(params)
  //     if (!response.data.items) {
  //       throw new Error("Failed to set cluster data");
  //     }
  //     await loadAuthorizations(params as any);
  //     setErrorMessage("");
  //   } catch (error) {
  //     handleLoginError(error);
  //   }
  // }
  // const loadAuthorizations = async (params: LoginParams) => {
  //   try {
  //     const response = await LoginApi.loadAuthorizationsCallback(params);
  //     const authorizationIds = response.data.items.map(
  //       (item: any) => item.AuthorizationInfo.authorization_id
  //     );

  //     let ambariData = JSON.parse(Utility.decryptData(db.getItem("ambari") || "") || "");
  //     console.log("ambariData", ambariData)
  //     ambariData.app.auth = authorizationIds;
  //     db.setItem("ambari", Utility.encryptData(JSON.stringify(ambariData)));

  //     // After successful login and authorization, redirect to metrics dashboard
  //     window.location.href = '/#/main/dashboard/metrics';
  //   } catch (error) {
  //     handleLoginError(error);
  //   }
  // }
  return (
    <>
      <div
        className="w-100 d-flex align-items-center py-2 px-4"
        style={{ background: "#313d54" }}
      >
        <Image src={AmbariLogo} className="logo" height={30} />
        <h2 className="logo-text  fs-16 mt-2 ms-3" style={{ color: "#b8bec4" }}>
          Ambari
        </h2>
      </div>
      <Container fluid className=" h-100 w-100 mt-3">
        <Row className="justify-content-center w-100 d-flex justify-content-center align-items-start">
          <Col className="d-flex justify-content-center">
            <Card className="p-4 bg-transparent border-1 custom-width">
              <Form onSubmit={handleSignIn}>
                <Card.Title className="text-start">
                  <h2>Sign in</h2>
                </Card.Title>
                {errorMessage ? (
                  <Alert variant="danger" className="my-3">
                    {errorMessage}
                  </Alert>
                ) : (
                  ""
                )}
                <Form.Group className="my-3" controlId="loginFields">
                  <Form.Label className="fw-bold">Username</Form.Label>
                  <Form.Control
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    className="form-control mb-4 rounded-0"
                  />
                  <Form.Label className="fw-bold">Password</Form.Label>
                  <Form.Control
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="form-control mb-1 rounded-0"
                  />
                  <Button
                    variant="primary"
                    type="submit"
                    className="mt-3 text-white"
                    disabled={isLoading}
                  >
                    {isLoading ? "Signing In..." : "SIGN IN"}
                  </Button>
                </Form.Group>
              </Form>
            </Card>
          </Col>
        </Row>
      </Container>
    </>
  );
};
