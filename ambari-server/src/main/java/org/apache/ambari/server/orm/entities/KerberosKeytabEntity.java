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

package org.apache.ambari.server.orm.entities;

import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "kerberos_keytab")
@NamedQueries({
    @NamedQuery(name = "KerberosKeytabEntity.findAll", query = "SELECT kk FROM KerberosKeytabEntity kk"),
    @NamedQuery(name = "KerberosKeytabEntity.findByHost",
        query = "SELECT kk FROM KerberosKeytabEntity kk JOIN kk.kerberosPrincipalHostEntities he WHERE he.hostId=:hostId")
})
public class KerberosKeytabEntity {
    @Id
    @Column(name = "keytab_path", insertable = true, updatable = false, nullable = false)
    private String keytabPath = null;

    @OneToMany(mappedBy = "keytabEntity", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private Collection<KerberosPrincipalHostEntity> kerberosPrincipalHostEntities;

    public KerberosKeytabEntity(){

    }

    public KerberosKeytabEntity(String keytabPath){
        setKeytabPath(keytabPath);
    }

    public String getKeytabPath() {
        return keytabPath;
    }

    public void setKeytabPath(String keytabPath) {
        this.keytabPath = keytabPath;
    }

    public Collection<KerberosPrincipalHostEntity> getKerberosPrincipalHostEntities() {
        return kerberosPrincipalHostEntities;
    }

    public void setKerberosPrincipalHostEntities(Collection<KerberosPrincipalHostEntity> kerberosPrincipalHostEntities) {
        this.kerberosPrincipalHostEntities = kerberosPrincipalHostEntities;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KerberosKeytabEntity that = (KerberosKeytabEntity) o;

        return keytabPath.equals(that.keytabPath);
    }

    @Override
    public int hashCode() {
        return keytabPath.hashCode();
    }
}
