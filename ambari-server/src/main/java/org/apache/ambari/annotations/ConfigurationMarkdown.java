/*
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
package org.apache.ambari.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.ambari.server.configuration.Configuration;
/**
 * The {@link ConfigurationMarkdown} is used to represent more complex
 * Markdown for {@link Configuration.ConfigurationProperty} fields. It wraps the traditional
 * {@link Markdown} along with extra metadata used to generate documentation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
public @interface ConfigurationMarkdown {
    /**
     * The base Markdown.
     *
     * @return
     */
    Markdown markdown();

    /**
     * The logic grouping that the configuration property belongs to.
     *
     * @return
     */
    Configuration.ConfigurationGrouping group();

    /**
     * All of the recommended values for the property based on cluster size.
     *
     * @return
     */
    ClusterScale[] scaleValues() default {};
}
