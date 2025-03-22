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

import React, { useState, useRef, useEffect } from 'react';
import { Form, Button, Alert } from 'react-bootstrap';
import './LoginForm.scss';

interface LoginFormProps {
  onSubmit: (username: string, password: string) => Promise<void>;
  isSubmitting: boolean;
  errorMessage?: string;
}

interface ValidationErrors {
  username?: string;
  password?: string;
}

const LoginForm: React.FC<LoginFormProps> = ({ 
  onSubmit, 
  isSubmitting, 
  errorMessage 
}) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [validationErrors, setValidationErrors] = useState<ValidationErrors>({});
  const [touched, setTouched] = useState({ username: false, password: false });
  const usernameInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    // Focus the username input when the component mounts
    if (usernameInputRef.current) {
      usernameInputRef.current.focus();
    }
  }, []);

  const validateForm = (): boolean => {
    const errors: ValidationErrors = {};
    
    if (!username.trim()) {
      errors.username = 'Username is required';
    }
    
    if (!password) {
      errors.password = 'Password is required';
    } else if (password.length < 4) {
      errors.password = 'Password must be at least 4 characters';
    }
    
    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Mark all fields as touched to show validation errors
    setTouched({ username: true, password: true });
    
    if (validateForm()) {
      await onSubmit(username, password);
    }
  };

  const handleBlur = (field: 'username' | 'password') => {
    setTouched({ ...touched, [field]: true });
    validateForm();
  };

  return (
    <div className="login-form">
      <h2>Sign in</h2>
      
      {errorMessage && (
        <Alert variant="danger" data-qa="login-error">
          {errorMessage}
        </Alert>
      )}
      
      <Form onSubmit={handleSubmit} noValidate>
        <Form.Group className="mb-3">
          <Form.Label data-qa="username-label">Username</Form.Label>
          <Form.Control
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            onBlur={() => handleBlur('username')}
            isInvalid={touched.username && !!validationErrors.username}
            disabled={isSubmitting}
            ref={usernameInputRef}
            data-qa="username-input"
            className="login-username"
          />
          <Form.Control.Feedback type="invalid">
            {validationErrors.username}
          </Form.Control.Feedback>
        </Form.Group>
        
        <Form.Group className="mb-3">
          <Form.Label data-qa="password-label">Password</Form.Label>
          <Form.Control
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            onBlur={() => handleBlur('password')}
            isInvalid={touched.password && !!validationErrors.password}
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
          <Form.Control.Feedback type="invalid">
            {validationErrors.password}
          </Form.Control.Feedback>
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
  );
};

export default LoginForm;
