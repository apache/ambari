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

import React, { useState, useEffect, useCallback } from 'react';
import { Dropdown } from 'react-bootstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { 
  faUser, 
  faSignOutAlt, 
  faKey, 
  faUserCircle 
} from '@fortawesome/free-solid-svg-icons';
import { useHistory } from 'react-router-dom';
import './UserProfile.scss';

interface UserProfileProps {
  username?: string;
  roles?: string[];
  lastLogin?: string;
  onLogout: () => void;
  onChangePassword?: () => void;
}

interface UserData {
  username: string;
  fullName: string;
  email: string;
  roles: string[];
  lastLogin: string;
  created: string;
}

const UserProfile: React.FC<UserProfileProps> = ({
  username,
  roles = [],
  lastLogin,
  onLogout,
  onChangePassword
}) => {
  const [userData, setUserData] = useState<UserData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const history = useHistory();

  // This function simulates fetching user data from an API
  const simulateFetchUserData = useCallback(async (username: string): Promise<UserData> => {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          username,
          fullName: 'John Doe',
          email: `${username.toLowerCase()}@example.com`,
          roles: roles.length > 0 ? roles : ['Administrator'],
          lastLogin: lastLogin || new Date().toISOString(),
          created: '2024-01-15T10:30:00Z'
        });
      }, 500);
    });
  }, [roles, lastLogin]);

  useEffect(() => {
    const fetchUserData = async () => {
      setIsLoading(true);
      setError(null);
      
      try {
        // In a real implementation, this would be an API call to get user data
        // For now, we'll simulate fetching user data
        const data = await simulateFetchUserData(username || '');
        setUserData(data);
      } catch (err) {
        setError('Failed to load user profile data');
        console.error('Error fetching user data:', err);
      } finally {
        setIsLoading(false);
      }
    };
    
    fetchUserData();
  }, [username, simulateFetchUserData]);

  const handleLogout = () => {
    // Clear user data from localStorage
    localStorage.removeItem('ambari-user');
    
    // Call the onLogout callback
    onLogout();
    
    // Redirect to login page
    history.push('/login');
  };

  const handleChangePassword = () => {
    if (onChangePassword) {
      onChangePassword();
    }
  };

  const handleViewProfile = () => {
    // Redirect to user profile page
    history.push(`/users/${userData?.username}/edit`);
  };

  if (isLoading) {
    return <div className="user-profile-loading">Loading profile...</div>;
  }

  if (error || !userData) {
    return <div className="user-profile-error">Error loading profile</div>;
  }

  return (
    <div className="user-profile">
      <Dropdown>
        <Dropdown.Toggle variant="link" id="dropdown-user-profile" className="user-dropdown-toggle">
          <FontAwesomeIcon icon={faUserCircle} className="user-icon" />
          <span className="username">{userData.username}</span>
        </Dropdown.Toggle>

        <Dropdown.Menu align="end" className="user-dropdown-menu">
          <div className="user-info">
            <FontAwesomeIcon icon={faUser} className="user-avatar" />
            <div className="user-details">
              <h5>{userData.fullName}</h5>
              <p className="email">{userData.email}</p>
              <p className="roles">{userData.roles.join(', ')}</p>
            </div>
          </div>
          
          <Dropdown.Divider />
          
          <Dropdown.Item onClick={handleViewProfile}>
            <FontAwesomeIcon icon={faUser} className="menu-icon" />
            View Profile
          </Dropdown.Item>
          
          <Dropdown.Item onClick={handleChangePassword}>
            <FontAwesomeIcon icon={faKey} className="menu-icon" />
            Change Password
          </Dropdown.Item>
          
          <Dropdown.Item onClick={handleLogout}>
            <FontAwesomeIcon icon={faSignOutAlt} className="menu-icon" />
            Logout
          </Dropdown.Item>
        </Dropdown.Menu>
      </Dropdown>
    </div>
  );
};

export default UserProfile;
