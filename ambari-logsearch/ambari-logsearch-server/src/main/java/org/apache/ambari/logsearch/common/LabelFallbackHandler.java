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
package org.apache.ambari.logsearch.common;

import org.apache.ambari.logsearch.conf.UIMappingConfig;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

@Named
public class LabelFallbackHandler {

  private final UIMappingConfig uiMappingConfig;

  @Inject
  public LabelFallbackHandler(UIMappingConfig uiMappingConfig) {
    this.uiMappingConfig = uiMappingConfig;
  }

  public String fallbackIfRequired(String field, String label, boolean replaceUnderscore,
                                   boolean replaceUppercaseInWord, boolean capitalizeAll) {
    if (isEnabled() && StringUtils.isBlank(label)) {
      return fallback(field,replaceUnderscore, replaceUppercaseInWord, capitalizeAll);
    }
    return label;
  }

  public String fallbackIfRequired(String field, String label, boolean replaceUnderscore,
                                   boolean replaceUppercaseInWord, boolean capitalizeAll, List<String> prefixesToRemove) {
    if (isEnabled() && StringUtils.isBlank(label)) {
      return fallback(field,replaceUnderscore, replaceUppercaseInWord, capitalizeAll, prefixesToRemove);
    }
    return label;
  }

  public String fallback(String field, boolean replaceUnderscore, boolean replaceUppercaseInWord, boolean capitalizeAll) {
    String result = null;
    if (StringUtils.isNotBlank(field)) {
     if (replaceUppercaseInWord) {
       result = capitalize(deCamelCase(field), false);
     }
     if (replaceUnderscore) {
       result = capitalize(deUnderScore(result != null ? result : field), capitalizeAll);
     }
    }
    return result;
  }

  public String fallback(String field, boolean replaceUnderscore, boolean replaceUppercaseInWord, boolean capitalizeAll, List<String> prefixesToRemove) {
    String fieldWithoutPrefix =  null;
    if (!CollectionUtils.isEmpty(prefixesToRemove)) {
      for (String prefix : prefixesToRemove) {
        if (StringUtils.isNotBlank(field) && field.startsWith(prefix)) {
          fieldWithoutPrefix = field.substring(prefix.length());
        }
      }
    }
    return fallback(fieldWithoutPrefix != null ? fieldWithoutPrefix : field, replaceUnderscore, replaceUppercaseInWord, capitalizeAll);
  }

  private String deUnderScore(String input) {
    return input.replaceAll("_", " ");
  }

  private String capitalize(String input, boolean capitalizeAll) {
    if (capitalizeAll) {
      return WordUtils.capitalizeFully(input);
    } else {
      Character firstLetter = Character.toUpperCase(input.charAt(0));
      return input.length() > 1 ? firstLetter + input.substring(1) : firstLetter.toString();
    }
  }

  private String deCamelCase(String input) {
    StringBuilder result = new StringBuilder();
    for(int i=0 ; i < input.length() ; i++) {
      char c = input.charAt(i);
      if(i != 0 && Character.isUpperCase(c)) {
        result.append(' ');
      }
      result.append(c);
    }
    return result.toString();
  }

  public boolean isEnabled() {
    return uiMappingConfig.isLabelFallbackEnabled();
  }

}
