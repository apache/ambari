/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch.service;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.apache.ambari.logsearch.dao.UserDao;
import org.apache.ambari.logsearch.web.model.User;
import org.apache.log4j.Logger;

import javax.inject.Inject;


@Service
public class UserService implements UserDetailsService {
  private static final Logger logger = Logger.getLogger(UserService.class);

  @Inject
  private UserDao userDao;

  @Override
  public User loadUserByUsername(final String username) throws UsernameNotFoundException {
    logger.debug(userDao + " loadUserByUsername " + username);
    return userDao.loadUserByUsername(username);
  }

}
