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
package org.apache.ambari.server.customactions;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionType;
import org.apache.ambari.server.actionmanager.TargetHostType;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages Action definitions read from XML files
 */
public class ActionDefinitionManager {
  public static final Short MIN_TIMEOUT = 60;
  private final static Logger LOG = LoggerFactory
      .getLogger(ActionDefinitionManager.class);
  private static final Map<Class<?>, JAXBContext> _jaxbContexts =
      new HashMap<Class<?>, JAXBContext>();
  private static final Short MAX_TIMEOUT = Short.MAX_VALUE-1;

  static {
    try {
      JAXBContext ctx = JAXBContext.newInstance(ActionDefinitionXml.class);
      _jaxbContexts.put(ActionDefinitionXml.class, ctx);
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }

  private final Map<String, ActionDefinition> actionDefinitionMap = new HashMap<String, ActionDefinition>();

  public ActionDefinitionManager() {
  }

  public static <T> T unmarshal(Class<T> clz, File file) throws JAXBException {
    Unmarshaller u = _jaxbContexts.get(clz).createUnmarshaller();

    return clz.cast(u.unmarshal(file));
  }

  private <E extends Enum<E>> E safeValueOf(Class<E> enumm, String s, StringBuilder reason) {
    if (s == null || s.length() == 0) {
      return null;
    }

    try {
      return Enum.valueOf(enumm, s);
    } catch (IllegalArgumentException iaex) {
      reason.append("Invalid value provided for " + enumm.getName());
      return null;
    }
  }

  public void readCustomActionDefinitions(File customActionDefinitionRoot) throws JAXBException, AmbariException {
    if (customActionDefinitionRoot == null
        || !customActionDefinitionRoot.exists()
        || !customActionDefinitionRoot.canRead()) {
      LOG.warn("Cannot read custom action definitions. " +
          customActionDefinitionRoot == null ? "" : "Check path " + customActionDefinitionRoot.getAbsolutePath());
    }

    File[] customActionDefinitionFiles
        = customActionDefinitionRoot.listFiles(AmbariMetaInfo.FILENAME_FILTER);

    if (customActionDefinitionFiles != null) {
      for (File definitionFile : customActionDefinitionFiles) {
        ActionDefinitionXml adx = null;
        try {
          adx = unmarshal(ActionDefinitionXml.class, definitionFile);
        } catch (UnmarshalException uex) {
          LOG.warn("Encountered badly formed action definition file - " + definitionFile.getAbsolutePath());
          continue;
        }
        for (ActionDefinitionSpec ad : adx.actionDefinitions()) {
          LOG.debug("Read action definition = " + ad.toString());
          StringBuilder errorReason =
              new StringBuilder("Error while parsing action definition. ").append(ad.toString()).append(" --- ");

          TargetHostType targetType = safeValueOf(TargetHostType.class, ad.getTargetType(), errorReason);
          ActionType actionType = safeValueOf(ActionType.class, ad.getActionType(), errorReason);

          Short defaultTimeout = MIN_TIMEOUT;
          if (ad.getDefaultTimeout() != null && !ad.getDefaultTimeout().isEmpty()) {
            defaultTimeout = Short.parseShort(ad.getDefaultTimeout());
          }

          if (isValidActionDefinition(ad, actionType, defaultTimeout, errorReason)) {
            String actionName = ad.getActionName();
            if (actionDefinitionMap.containsKey(actionName)) {
              LOG.warn("Ignoring action definition as a different definition by that name already exists. "
                  + ad.toString());
              continue;
            }

            actionDefinitionMap.put(ad.getActionName(), new ActionDefinition(ad.getActionName(), actionType,
                ad.getInputs(), ad.getTargetService(), ad.getTargetComponent(), ad.getDescription(), targetType, defaultTimeout));
            LOG.info("Added custom action definition for " + ad.getActionName());
          } else {
            LOG.warn(errorReason.toString());
          }
        }
      }
    }
  }

  private boolean isValidActionDefinition(ActionDefinitionSpec ad, ActionType actionType,
                                          Short defaultTimeout, StringBuilder reason) {
    if (isValidActionName(ad.getActionName(), reason)) {

      if (defaultTimeout < MIN_TIMEOUT || defaultTimeout > MAX_TIMEOUT) {
        reason.append("Default timeout should be between " + MIN_TIMEOUT + " and " + MAX_TIMEOUT);
        return false;
      }

      if (actionType == null || actionType == ActionType.SYSTEM_DISABLED) {
        reason.append("Action type cannot be " + actionType);
        return false;
      }

      if (ad.getDescription() == null || ad.getDescription().isEmpty()) {
        reason.append("Action description cannot be empty");
        return false;
      }

      if (ad.getTargetService() == null || ad.getTargetService().isEmpty()) {
        if (ad.getTargetComponent() != null && !ad.getTargetComponent().isEmpty()) {
          reason.append("Target component cannot be specified unless target service is specified");
          return false;
        }
      }

      if (ad.getInputs() != null && !ad.getInputs().isEmpty()) {
        String[] parameters = ad.getInputs().split(",");
        for (String parameter : parameters) {
          if (parameter.trim().isEmpty()) {
            reason.append("Empty parameter cannot be specified as an input parameter");
          }
        }
      }
    } else {
      return false;
    }

    return true;
  }

  public List<ActionDefinition> getAllActionDefinition() {
    return new ArrayList<ActionDefinition>(actionDefinitionMap.values());
  }

  public ActionDefinition getActionDefinition(String name) {
    return actionDefinitionMap.get(name);
  }

  public void addActionDefinition(ActionDefinition ad) throws AmbariException {
    if (!actionDefinitionMap.containsKey(ad.getActionName())) {
      actionDefinitionMap.put(ad.getActionName(), ad);
    } else {
      throw new AmbariException("Action definition by name " + ad.getActionName() + " already exists.");
    }
  }

  private boolean isValidActionName(String actionName, StringBuilder reason) {
    if (actionName == null || actionName.isEmpty()) {
      reason.append("Action name cannot be empty");
      return false;
    }
    String trimmedName = actionName.replaceAll("\\s+", "");
    if (actionName.length() > trimmedName.length()) {
      reason.append("Action name cannot contain white spaces");
      return false;
    }
    return true;
  }
}
