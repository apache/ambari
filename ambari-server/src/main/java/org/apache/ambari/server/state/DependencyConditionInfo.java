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

package org.apache.ambari.server.state;

import java.util.Map;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.lang.NotImplementedException;

/**
 * Represents stack component dependency condition information.
 */
@XmlJavaTypeAdapter(DependencyConditionAdapter.class)
public abstract class DependencyConditionInfo {
    public abstract boolean isResolved(Map<String, Map<String, String>> properties);
}

class PropertyExistsDependencyCondition extends DependencyConditionInfo{

    protected final String configType;
    protected final String property;
    public PropertyExistsDependencyCondition( String configType, String property) {
        this.configType = Objects.requireNonNull(configType, "Config Type must not be null.");
        this.property = Objects.requireNonNull(property, "Property Name must not be null.");
    }

    @Override
    public boolean isResolved(Map<String, Map<String, String>> properties) {
        return (properties.get(configType).containsKey(property));
    }
}

class PropertyValueEqualsDependencyCondition extends PropertyExistsDependencyCondition {

    protected final String propertyValue;
    public PropertyValueEqualsDependencyCondition(String configType, String property, String propertyValue) {
        super(configType, property);
        this.propertyValue = Objects.requireNonNull(propertyValue, "Property value must not be null.");
    }

    @Override
    public boolean isResolved(Map<String, Map<String, String>> properties) {
            return (super.isResolved(properties) && propertyValue.equals(properties.get(configType).get(property)));
    }
}

class DependencyConditionAdapter extends XmlAdapter<DependencyConditionAdapter.AdaptedDependencyCondition, DependencyConditionInfo> {

    static class AdaptedDependencyCondition{
            @XmlElement
            private String configType;
            @XmlElement
            private String property;
            @XmlElement
            private String propertyValue;
            @XmlElement(name="condition-type")
            private String conditionType;
    }

    @Override
    public AdaptedDependencyCondition marshal(DependencyConditionInfo arg0) throws Exception {
        throw new NotImplementedException();
    }

    @Override
    public DependencyConditionInfo unmarshal(AdaptedDependencyCondition adaptedDependencyCondition) throws Exception {
        if (null == adaptedDependencyCondition) {
            return null;
        }
        DependencyConditionInfo dependencyConditionInfo = null;
            switch (adaptedDependencyCondition.conditionType) {
                case "IF-PROPERTY-EXISTS":
                    dependencyConditionInfo = new PropertyExistsDependencyCondition(adaptedDependencyCondition.configType, adaptedDependencyCondition.property);
                    break;
                case "PROPERTY-VALUE-EQUALS":
                    dependencyConditionInfo = new PropertyValueEqualsDependencyCondition(adaptedDependencyCondition.configType, adaptedDependencyCondition.property, adaptedDependencyCondition.propertyValue);
                    break;
                default:
                    throw new IllegalArgumentException("Specified condition type is not not supported " + adaptedDependencyCondition.conditionType);
            }
        return dependencyConditionInfo;
    }
}