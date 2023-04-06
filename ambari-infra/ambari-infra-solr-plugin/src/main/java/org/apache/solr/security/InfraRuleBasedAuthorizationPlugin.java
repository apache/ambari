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
package org.apache.solr.security;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.solr.common.util.CommandOperation;
import org.apache.solr.common.util.Utils;
import org.apache.solr.common.util.ValidatingJsonMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.solr.handler.admin.SecurityConfHandler.getListValue;
import static org.apache.solr.handler.admin.SecurityConfHandler.getMapValue;

/**
 * Modified copy of solr.RuleBasedAuthorizationPlugin to handle role - permission mappings with KereberosPlugin
 * Added 2 new JSON map: (precedence: user-host-regex > user-host)
 * 1. "user-host": user host mappings (array) for hostname validation
 * 2. "user-host-regex": user host regex mapping (string) for hostname validation
 */
public class InfraRuleBasedAuthorizationPlugin extends RuleBasedAuthorizationPlugin {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Map<String, Set<String>> usersVsRoles = new HashMap<>();
  private final Map<String, WildCardSupportMap> mapping = new HashMap<>();
  private final List<Permission> permissions = new ArrayList<>();
  private final Map<String, Set<String>> userVsHosts = new HashMap<>();
  private final Map<String, String> userVsHostRegex = new HashMap<>();

  private InfraKerberosHostValidator infraKerberosHostValidator = new InfraKerberosHostValidator();
  private InfraUserRolesLookupStrategy infraUserRolesLookupStrategy = new InfraUserRolesLookupStrategy();


  private static class WildCardSupportMap extends HashMap<String, List<Permission>> {
    final Set<String> wildcardPrefixes = new HashSet<>();

    @Override
    public List<Permission> put(String key, List<Permission> value) {
      if (key != null && key.endsWith("/*")) {
        key = key.substring(0, key.length() - 2);
        wildcardPrefixes.add(key);
      }
      return super.put(key, value);
    }

    @Override
    public List<Permission> get(Object key) {
      List<Permission> result = super.get(key);
      if (key == null || result != null) return result;
      if (!wildcardPrefixes.isEmpty()) {
        for (String s : wildcardPrefixes) {
          if (key.toString().startsWith(s)) {
            List<Permission> l = super.get(s);
            if (l != null) {
              result = result == null ? new ArrayList<>() : new ArrayList<>(result);
              result.addAll(l);
            }
          }
        }
      }
      return result;
    }
  }

  @Override
  public AuthorizationResponse authorize(AuthorizationContext context) {
    List<AuthorizationContext.CollectionRequest> collectionRequests = context.getCollectionRequests();
    if (context.getRequestType() == AuthorizationContext.RequestType.ADMIN) {
      InfraRuleBasedAuthorizationPlugin.MatchStatus flag = checkCollPerm(mapping.get(null), context);
      return flag.rsp;
    }

    for (AuthorizationContext.CollectionRequest collreq : collectionRequests) {
      //check permissions for each collection
      InfraRuleBasedAuthorizationPlugin.MatchStatus flag = checkCollPerm(mapping.get(collreq.collectionName), context);
      if (flag != InfraRuleBasedAuthorizationPlugin.MatchStatus.NO_PERMISSIONS_FOUND) return flag.rsp;
    }
    //check wildcard (all=*) permissions.
    InfraRuleBasedAuthorizationPlugin.MatchStatus flag = checkCollPerm(mapping.get("*"), context);
    return flag.rsp;
  }

  private InfraRuleBasedAuthorizationPlugin.MatchStatus checkCollPerm(Map<String, List<Permission>> pathVsPerms,
                                                                 AuthorizationContext context) {
    if (pathVsPerms == null) return InfraRuleBasedAuthorizationPlugin.MatchStatus.NO_PERMISSIONS_FOUND;

    String path = context.getResource();
    InfraRuleBasedAuthorizationPlugin.MatchStatus flag = checkPathPerm(pathVsPerms.get(path), context);
    if (flag != InfraRuleBasedAuthorizationPlugin.MatchStatus.NO_PERMISSIONS_FOUND) return flag;
    return checkPathPerm(pathVsPerms.get(null), context);
  }

  private InfraRuleBasedAuthorizationPlugin.MatchStatus checkPathPerm(List<Permission> permissions, AuthorizationContext context) {
    if (permissions == null || permissions.isEmpty()) return InfraRuleBasedAuthorizationPlugin.MatchStatus.NO_PERMISSIONS_FOUND;
    Principal principal = context.getUserPrincipal();
    loopPermissions:
    for (int i = 0; i < permissions.size(); i++) {
      Permission permission = permissions.get(i);
      if (PermissionNameProvider.values.containsKey(permission.name)) {
        if (context.getHandler() instanceof PermissionNameProvider) {
          PermissionNameProvider handler = (PermissionNameProvider) context.getHandler();
          PermissionNameProvider.Name permissionName = handler.getPermissionName(context);
          if (permissionName == null || !permission.name.equals(permissionName.name)) {
            continue;
          }
        } else {
          //all is special. it can match any
          if(permission.wellknownName != PermissionNameProvider.Name.ALL) continue;
        }
      } else {
        if (permission.method != null && !permission.method.contains(context.getHttpMethod())) {
          //this permissions HTTP method does not match this rule. try other rules
          continue;
        }
        if (permission.params != null) {
          for (Map.Entry<String, Function<String[], Boolean>> e : permission.params.entrySet()) {
            String[] paramVal = context.getParams().getParams(e.getKey());
            if(!e.getValue().apply(paramVal)) continue loopPermissions;
          }
        }
      }

      if (permission.role == null) {
        //no role is assigned permission.That means everybody is allowed to access
        return InfraRuleBasedAuthorizationPlugin.MatchStatus.PERMITTED;
      }
      if (principal == null) {
        log.info("request has come without principal. failed permission {} ",permission);
        //this resource needs a principal but the request has come without
        //any credential.
        return InfraRuleBasedAuthorizationPlugin.MatchStatus.USER_REQUIRED;
      } else if (permission.role.contains("*")) {
        return InfraRuleBasedAuthorizationPlugin.MatchStatus.PERMITTED;
      }

      for (String role : permission.role) {
        Set<String> userRoles = infraUserRolesLookupStrategy.getUserRolesFromPrincipal(usersVsRoles, principal);
        boolean validHostname = infraKerberosHostValidator.validate(principal, userVsHosts, userVsHostRegex);
        if (!validHostname) {
          log.warn("Hostname is not valid for principal {}", principal);
          return MatchStatus.FORBIDDEN;
        }
        if (userRoles != null && userRoles.contains(role)) return MatchStatus.PERMITTED;
      }
      log.info("This resource is configured to have a permission {}, The principal {} does not have the right role ", permission, principal);
      return InfraRuleBasedAuthorizationPlugin.MatchStatus.FORBIDDEN;
    }
    log.debug("No permissions configured for the resource {} . So allowed to access", context.getResource());
    return InfraRuleBasedAuthorizationPlugin.MatchStatus.NO_PERMISSIONS_FOUND;
  }

  @Override
  public void init(Map<String, Object> initInfo) {
    mapping.put(null, new InfraRuleBasedAuthorizationPlugin.WildCardSupportMap());
    Map<String, Object> map = getMapValue(initInfo, "user-role");
    for (Object o : map.entrySet()) {
      Map.Entry e = (Map.Entry) o;
      String roleName = (String) e.getKey();
      usersVsRoles.put(roleName, Permission.readValueAsSet(map, roleName));
    }
    List<Map> perms = getListValue(initInfo, "permissions");
    for (Map o : perms) {
      Permission p;
      try {
        p = Permission.load(o);
      } catch (Exception exp) {
        log.error("Invalid permission ", exp);
        continue;
      }
      permissions.add(p);
      add2Mapping(p);
      // adding user-host
      Map<String, Object> userHostsMap = getMapValue(initInfo, "user-host");
      for (Object userHost : userHostsMap.entrySet()) {
        Map.Entry e = (Map.Entry) userHost;
        String roleName = (String) e.getKey();
        userVsHosts.put(roleName, readValueAsSet(userHostsMap, roleName));
      }
      // adding user-host-regex
      Map<String, Object> userHostRegexMap = getMapValue(initInfo, "user-host-regex");
      for (Map.Entry<String, Object> entry : userHostRegexMap.entrySet()) {
        userVsHostRegex.put(entry.getKey(), entry.getValue().toString());
      }
    }
  }

  /**
   * read a key value as a set. if the value is a single string ,
   * return a singleton set
   *
   * @param m   the map from which to lookup
   * @param key the key with which to do lookup
   */
  static Set<String> readValueAsSet(Map m, String key) {
    Set<String> result = new HashSet<>();
    Object val = m.get(key);
    if (val == null) {
      if("collection".equals(key)){
        //for collection collection: null means a core admin/ collection admin request
        // otherwise it means a request where collection name is ignored
        return m.containsKey(key) ? singleton((String) null) : singleton("*");
      }
      return null;
    }
    if (val instanceof Collection) {
      Collection list = (Collection) val;
      for (Object o : list) result.add(String.valueOf(o));
    } else if (val instanceof String) {
      result.add((String) val);
    } else {
      throw new RuntimeException("Bad value for : " + key);
    }
    return result.isEmpty() ? null : Collections.unmodifiableSet(result);
  }

  //this is to do optimized lookup of permissions for a given collection/path
  private void add2Mapping(Permission permission) {
    for (String c : permission.collections) {
      InfraRuleBasedAuthorizationPlugin.WildCardSupportMap m = mapping.get(c);
      if (m == null) mapping.put(c, m = new InfraRuleBasedAuthorizationPlugin.WildCardSupportMap());
      for (String path : permission.path) {
        List<Permission> perms = m.get(path);
        if (perms == null) m.put(path, perms = new ArrayList<>());
        perms.add(permission);
      }
    }
  }


  @Override
  public void close() throws IOException { }

  enum MatchStatus {
    USER_REQUIRED(AuthorizationResponse.PROMPT),
    NO_PERMISSIONS_FOUND(AuthorizationResponse.OK),
    PERMITTED(AuthorizationResponse.OK),
    FORBIDDEN(AuthorizationResponse.FORBIDDEN);

    final AuthorizationResponse rsp;

    MatchStatus(AuthorizationResponse rsp) {
      this.rsp = rsp;
    }
  }



  @Override
  public Map<String, Object> edit(Map<String, Object> latestConf, List<CommandOperation> commands) {
    for (CommandOperation op : commands) {
      AutorizationEditOperation operation = ops.get(op.name);
      if (operation == null) {
        op.unknownOperation();
        return null;
      }
      latestConf = operation.edit(latestConf, op);
      if (latestConf == null) return null;

    }
    return latestConf;
  }

  private static final Map<String, AutorizationEditOperation> ops = unmodifiableMap(asList(AutorizationEditOperation.values()).stream().collect(toMap(AutorizationEditOperation::getOperationName, identity())));


  @Override
  public ValidatingJsonMap getSpec() {
    return Utils.getSpec("cluster.security.InfraRuleBasedAuthorization").getSpec();
  }
}