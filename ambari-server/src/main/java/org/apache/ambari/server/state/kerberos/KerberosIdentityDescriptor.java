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
package org.apache.ambari.server.state.kerberos;

import java.util.Map;

import org.apache.ambari.server.collections.Predicate;
import org.apache.ambari.server.collections.PredicateUtils;

/**
 * KerberosIdentityDescriptor is an implementation of an AbstractKerberosDescriptor that
 * encapsulates data related to a Kerberos identity - including its principal and keytab file details.
 * <p/>
 * A KerberosIdentityDescriptor has the following properties:
 * <ul>
 * <li>name</li>
 * <li>principal</li>
 * <li>keytab</li>
 * <li>password</li>
 * </ul>
 * <p/>
 * The following (pseudo) JSON Schema will yield a valid KerberosIdentityDescriptor
 * <pre>
 *   {
 *      "$schema": "http://json-schema.org/draft-04/schema#",
 *      "title": "KerberosIdentityDescriptor",
 *      "description": "Describes a Kerberos identity",
 *      "type": "object",
 *      "properties": {
 *        "name": {
 *          "description": "An identifying name for this identity. The name may reference another
 *                          KerberosIdentityDescriptor by declaring the path to it",
 *          "type": "string"
 *        },
 *        "principal": {
 *          "description": "Details about this identity's principal",
 *          "type": "{@link org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptor}",
 *        }
 *        "keytab": {
 *          "description": "Details about this identity's keytab",
 *          "type": "{@link org.apache.ambari.server.state.kerberos.KerberosKeytabDescriptor}",
 *          }
 *        }
 *        "password": {
 *          "description": "The password to use for this identity. If not set a secure random
 *                          password will automatically be generated",
 *          "type": "string"
 *        }
 *      }
 *   }
 * </pre>
 * <p/>
 * In this implementation,
 * {@link org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptor#name} will hold the
 * KerberosIdentityDescriptor#name value
 */
public class KerberosIdentityDescriptor extends AbstractKerberosDescriptor {

  /**
   * The path to the Kerberos Identity definitions this {@link KerberosIdentityDescriptor} references
   */
  private String reference = null;

  /**
   * The KerberosPrincipalDescriptor containing the principal details for this Kerberos identity
   */
  private KerberosPrincipalDescriptor principal = null;

  /**
   * The KerberosKeytabDescriptor containing the keytab details for this Kerberos identity
   */
  private KerberosKeytabDescriptor keytab = null;

  /**
   * A String containing the password for this Kerberos identity
   * <p/>
   * If this value is null or empty, a random password will be generated as necessary.
   */
  private String password = null;

  /**
   * An expression used to determine when this {@link KerberosIdentityDescriptor} is relevant for the
   * cluster. If the process expression is not <code>null</code> and evaluates to <code>false</code>
   * then this {@link KerberosIdentityDescriptor} will be ignored when processing identities.
   */
  private Predicate when = null;

  /**
   * Creates a new KerberosIdentityDescriptor
   *
   * @param name the name of this identity descriptor
   * @param reference an optional path to a referenced KerberosIdentityDescriptor
   * @param principal a KerberosPrincipalDescriptor
   * @param keytab a KerberosKeytabDescriptor
   * @param when a predicate
   */
  public KerberosIdentityDescriptor(String name, String reference, KerberosPrincipalDescriptor principal, KerberosKeytabDescriptor keytab, Predicate when) {
    setName(name);
    setReference(reference);
    setPrincipalDescriptor(principal);
    setKeytabDescriptor(keytab);
    setWhen(when);
  }

  /**
   * Creates a new KerberosIdentityDescriptor
   * <p/>
   * See {@link org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor} for the JSON
   * Schema that may be used to generate this map.
   *
   * @param data a Map of values use to populate the data for the new instance
   * @see org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor
   */
  public KerberosIdentityDescriptor(Map<?, ?> data) {
    // The name for this KerberosIdentityDescriptor is stored in the "name" entry in the map
    // This is not automatically set by the super classes.
    setName(getStringValue(data, "name"));

    setReference(getStringValue(data, "reference"));

    if (data != null) {
      Object item;

      setPassword(getStringValue(data, "password"));

      item = data.get(Type.PRINCIPAL.getDescriptorName());
      if (item instanceof Map) {
        setPrincipalDescriptor(new KerberosPrincipalDescriptor((Map<?, ?>) item));
      }

      item = data.get(Type.KEYTAB.getDescriptorName());
      if (item instanceof Map) {
        setKeytabDescriptor(new KerberosKeytabDescriptor((Map<?, ?>) item));
      }

      item = data.get("when");
      if (item instanceof Map) {
        setWhen(PredicateUtils.fromMap((Map<?, ?>) item));
      }
    }
  }

  /**
   * Gets the path to the referenced Kerberos identity definition
   *
   * @return the path to the referenced Kerberos identity definition or <code>null</code> if not set
   */
  public String getReference() {
    return reference;
  }

  /**
   * Sets the path to the referenced Kerberos identity definition
   *
   * @param reference the path to the referenced Kerberos identity definition or <code>null</code>
   *                  to indicate no reference
   */
  public void setReference(String reference) {
    this.reference = reference;
  }

  /**
   * Gets the KerberosPrincipalDescriptor related to this KerberosIdentityDescriptor
   *
   * @return the KerberosPrincipalDescriptor related to this KerberosIdentityDescriptor
   */
  public KerberosPrincipalDescriptor getPrincipalDescriptor() {
    return principal;
  }

  /**
   * Sets the KerberosPrincipalDescriptor related to this KerberosIdentityDescriptor
   *
   * @param principal the KerberosPrincipalDescriptor related to this KerberosIdentityDescriptor
   */
  public void setPrincipalDescriptor(KerberosPrincipalDescriptor principal) {
    this.principal = principal;

    if (this.principal != null) {
      this.principal.setParent(this);
    }
  }

  /**
   * Gets the KerberosKeytabDescriptor related to this KerberosIdentityDescriptor
   *
   * @return the KerberosKeytabDescriptor related to this KerberosIdentityDescriptor
   */
  public KerberosKeytabDescriptor getKeytabDescriptor() {
    return keytab;
  }

  /**
   * Sets the KerberosKeytabDescriptor related to this KerberosIdentityDescriptor
   *
   * @param keytab the KerberosKeytabDescriptor related to this KerberosIdentityDescriptor
   */
  public void setKeytabDescriptor(KerberosKeytabDescriptor keytab) {
    this.keytab = keytab;

    if (this.keytab != null) {
      this.keytab.setParent(this);
    }
  }

  /**
   * Gets the password for this this KerberosIdentityDescriptor
   *
   * @return A String containing the password for this this KerberosIdentityDescriptor
   * @see #password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Sets the password for this this KerberosIdentityDescriptor
   *
   * @param password A String containing the password for this this KerberosIdentityDescriptor
   * @see #password
   */
  public void setPassword(String password) {
    this.password = password;
  }


  /**
   * Gets the expression (or {@link Predicate}) to use to determine when to include this Kerberos
   * identity while processing Kerberos identities.
   * <p>
   * <code>null</code> indicates there is nothing to evaluate and this Kerberos identity is to always
   * be included when processing Kerberos identities.
   *
   * @return a predicate
   */
  public Predicate getWhen() {
    return when;
  }

  /**
   * Sets the expression (or {@link Predicate}) to use to determine when to include this Kerberos
   * identity while processing Kerberos identities.
   * <p>
   * <code>null</code> indicates there is nothing to evaluate and this Kerberos identity is to always
   * be included when processing Kerberos identities.
   *
   * @param when a predicate
   */
  public void setWhen(Predicate when) {
    this.when = when;
  }

  /**
   * Processes the expression indicating when this {@link KerberosIdentityDescriptor} is to be included
   * in the set of Kerberos identities to process.
   * <p>
   * <code>True</code> will be returned if the expression is <code>null</code> or if it evaluates
   * as such.
   *
   * @param context A Map of context values, including at least the list of services and available configurations
   * @return true if this {@link KerberosIdentityDescriptor} is to be included when processing the
   * Kerberos identities; otherwise false.
   */
  public boolean shouldInclude(Map<String, Object> context) {
    return (this.when == null) || this.when.evaluate(context);
  }

  /**
   * Updates this KerberosIdentityDescriptor with data from another KerberosIdentityDescriptor
   * <p/>
   * Properties will be updated if the relevant updated values are not null.
   *
   * @param updates the KerberosIdentityDescriptor containing the updated values
   */
  public void update(KerberosIdentityDescriptor updates) {
    if (updates != null) {
      setName(updates.getName());

      setReference(updates.getReference());

      setPassword(updates.getPassword());

      KerberosPrincipalDescriptor existingPrincipal = getPrincipalDescriptor();
      if (existingPrincipal == null) {
        setPrincipalDescriptor(updates.getPrincipalDescriptor());
      } else {
        existingPrincipal.update(updates.getPrincipalDescriptor());
      }

      KerberosKeytabDescriptor existingKeytabDescriptor = getKeytabDescriptor();
      if (existingKeytabDescriptor == null) {
        setKeytabDescriptor(updates.getKeytabDescriptor());
      } else {
        existingKeytabDescriptor.update(updates.getKeytabDescriptor());
      }

      Predicate updatedWhen = updates.getWhen();
      if(updatedWhen != null) {
        setWhen(updatedWhen);
      }
    }
  }

  /**
   * Creates a Map of values that can be used to create a copy of this KerberosIdentityDescriptor
   * or generate the JSON structure described in
   * {@link org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor}
   *
   * @return a Map of values for this KerberosIdentityDescriptor
   * @see org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor
   */
  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> dataMap = super.toMap();

    if (reference != null) {
      dataMap.put("reference", reference);
    }

    if (principal != null) {
      dataMap.put(Type.PRINCIPAL.getDescriptorName(), principal.toMap());
    }

    if (keytab != null) {
      dataMap.put(Type.KEYTAB.getDescriptorName(), keytab.toMap());
    }

    if (password != null) {
      dataMap.put("password", password);
    }

    if(when != null) {
      dataMap.put("when", PredicateUtils.toMap(when));
    }

    return dataMap;
  }

  @Override
  public int hashCode() {
    return super.hashCode() +
        ((getReference() == null)
            ? 0
            : getReference().hashCode()) +
        ((getPrincipalDescriptor() == null)
            ? 0
            : getPrincipalDescriptor().hashCode()) +
        ((getKeytabDescriptor() == null)
            ? 0
            : getKeytabDescriptor().hashCode()) +
        ((getWhen() == null)
            ? 0
            : getWhen().hashCode());
  }

  @Override
  public boolean equals(Object object) {
    if (object == null) {
      return false;
    } else if (object == this) {
      return true;
    } else if (object.getClass() == KerberosIdentityDescriptor.class) {
      KerberosIdentityDescriptor descriptor = (KerberosIdentityDescriptor) object;
      return super.equals(object) &&
          (
              (getReference() == null)
                  ? (descriptor.getReference() == null)
                  : getReference().equals(descriptor.getReference())
          ) &&
          (
              (getPrincipalDescriptor() == null)
                  ? (descriptor.getPrincipalDescriptor() == null)
                  : getPrincipalDescriptor().equals(descriptor.getPrincipalDescriptor())
          ) &&
          (
              (getKeytabDescriptor() == null)
                  ? (descriptor.getKeytabDescriptor() == null)
                  : getKeytabDescriptor().equals(descriptor.getKeytabDescriptor())
          ) &&
          (
              (getPassword() == null)
                  ? (descriptor.getPassword() == null)
                  : getPassword().equals(descriptor.getPassword())
          ) &&
          (
              (getWhen() == null)
                  ? (descriptor.getWhen() == null)
                  : getWhen().equals(descriptor.getWhen())
          );
    } else {
      return false;
    }
  }
}
