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

import React, { useState } from 'react';
import { Container, Form, Button, Alert } from 'react-bootstrap';
import { useAuth } from '../../context/AuthContext';
import './Login.scss';

const Login: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { login } = useAuth();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!username.trim() || !password.trim()) {
      setErrorMessage('Username and password are required');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage('');

    try {
      const success = await login(username, password);
      
      if (!success) {
        setErrorMessage('Invalid username or password');
      }
    } catch (error) {
      setErrorMessage('An error occurred during login. Please try again.');
      console.error('Login error:', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="login-page">
      <div className="ambari-header">
        <div className="logo">Ambari</div>
      </div>
      
      <Container className="login-container">
        <div className="login-form-container">
          <h2>Sign in</h2>
          
          {errorMessage && (
            <Alert variant="danger" data-qa="login-error">
              {errorMessage}
            </Alert>
          )}
          
          <Form onSubmit={handleSubmit}>
            <Form.Group className="mb-3">
              <Form.Label data-qa="username-label">Username</Form.Label>
              <Form.Control
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                disabled={isSubmitting}
                data-qa="username-input"
                className="login-username"
              />
            </Form.Group>
            
            <Form.Group className="mb-3">
              <Form.Label data-qa="password-label">Password</Form.Label>
              <Form.Control
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={isSubmitting}
                data-qa="password-input"
                className="login-password"
                autoComplete="new-password"
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    handleSubmit(e);
                  }
                }}
              />
            </Form.Group>
            
            <Button
              variant="success"
              type="submit"
              disabled={isSubmitting}
              data-qa="login-button"
              className="sign-in-button"
            >
              {isSubmitting ? 'SIGNING IN...' : 'SIGN IN'}
            </Button>
          </Form>
        </div>
      </Container>
      
      <footer className="login-footer">
        <div className="license-text">
          Licensed under the Apache License, Version 2.0
          <br />
          <small>See third-party tools/resources that Ambari uses and their respective authors</small>
        </div>
      </footer>
    </div>
  );
};

export default Login;
