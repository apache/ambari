<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
# Slider Apps View

## Security Guide
*Slider Apps View* can optionally connect to a Kerberos secured cluster by following the below steps.

### Step-1: Deploy a HDP cluster and secure it using *Kerberos*
After deploying a HDP cluster through Ambari, it can be secured by using the *Enable Security* button in *Admin > Seurity* page.

### Step-2: Create *Kerberos* principal for view
We need to provide a *Kerberos* identity for the process in which the view is run. We shall identify the user as `view-principal`. **In this document `view-principal` can be changed to any suitable name.** Since views are generally hosted by Ambari server, typically this can be named as *ambari*.

On the machine where *KDC Server* is hosted, create user principal by running below command

```
kadmin.local -q "addprinc -randkey view-principal@EXAMPLE.COM"
```
Next, extract keytab file 

```
kadmin.local -q "xst -k /path/to/keytab/view-principal.headless.keytab view-principal@EXAMPLE.COM"
```
The keytab file should then be copied over to the keytabs location on the host where the view is hosted.

```
cp /path/to/keytab/view-principal.headless.keytab /etc/security/keytabs/
```

Change file permissions so that only necessary users can access it.

```
chmod 440 /etc/security/keytabs/view-principal.headless.keytab
```

If the view is hosted by *ambari-server*, its Kerberos identity can be set by running the below command.

```
ambari-server setup-security
```
During *setup-security* the `view-principal` user should be provided along with the keytab. These same values will be provided as view parameters in *Step-4*.


### Step-3: Configure *proxyuser* for created principal
Add the following configurations in *Custom core-site* section of *HDFS* service.

* hadoop.proxyuser.`view-principal`.groups = *
* hadoop.proxyuser.`view-principal`.hosts = `view-server-host`

This will in-turn show up in *core-site.xml* as

```
<property>
  <name>hadoop.proxyuser.view-principal.groups</name>
  <value>*</value>
</property>

<property>
  <name>hadoop.proxyuser.view-principal.hosts</name>
  <value>view-server-host.ambari.apache.org</value>
</property>
```
Restart HDFS and YARN services.

### Step-4: Create *Slider Apps View* with security parameters

From *Ambari-Admin* create a *Slider Apps View* with the below parameters populated

* slider.security.enabled = true
* view.kerberos.principal = `view-principal`
* view.kerberos.principal.keytab = `/etc/security/keytabs/view-principal.headless.keytab`

### Step-5 Create *Kerberos* principal for *slider.user*
We need to provide a *Kerberos* identity for the user identified in *slider.user* view parameter. 

The *slider.user* view parameter has the following interpretations:

* If the parameter is left blank, it means the user *yarn*. 
* If it is `${username}`, it is the user logged into Ambari. 
* Else, it is exact name of the user. 

We shall assume the user as `slider-user`. In a secured cluster this user has to actually exist on all the hosts. The user should also have an *uid* greater than 1000.

On the machine where *KDC Server* is hosted, create user principal by running below command

```
kadmin.local -q "addprinc -randkey slider-user@EXAMPLE.COM"
```
Next, extract keytab file 

```
kadmin.local -q "xst -k /path/to/keytab/slider-user.headless.keytab slider-user@EXAMPLE.COM"
```
The keytab file should then be copied over to the keytabs location on the host where the view is hosted.

```
cp /path/to/keytab/slider-user.headless.keytab /etc/security/keytabs/
```

Change file permissions so that only necessary users can access it.

**Make sure that `slider-user` keytab is at /etc/security/keytabs/`slider-user`.headless.keytab**

### Step-6 Create *Kerberos* principals for App launched by  *slider.user*
Slider Apps contain services, and they might need their own identities when talking to HDFS and YARN. To support such Apps, keytabs have to be created that are required specifically for the Apps. Each keytab should contain the identity of the principal on all hosts where the application can run.

By default, the following keytabs have to be created for specific Apps. This user has to exist on all hosts where containers are run:
#### HBase
For each host `host-name` in the cluster, do the following 

* 
```
kadmin.local -q "addprinc -randkey slider-user/[host-name]@EXAMPLE.COM"
```
Next, extract identities on all hosts into a single keytab file 

* 
```
kadmin.local -q "xst -k /path/to/keytab/slider-user.HBASE.service.keytab slider-user/[host-name]@EXAMPLE.COM"
```

The keytab file containing multiple identities should then be copied over to the keytabs location on the host where the view is hosted.

```
cp /path/to/keytab/slider-user.HBASE.service.keytab /etc/security/keytabs/
```

Change file permissions so that only necessary users can access it.

#### Storm
For each host `host-name` in the cluster, do the following 

* 
```
kadmin.local -q "addprinc -randkey slider-user/[host-name]@EXAMPLE.COM"
```
Next, extract all identities into a single keytab file 

* 
```
kadmin.local -q "xst -k /path/to/keytab/slider-user.STORM.nimbus.keytab slider-user/[host-name]@EXAMPLE.COM"
kadmin.local -q "xst -k /path/to/keytab/slider-user.STORM.client.keytab slider-user@EXAMPLE.COM"
```

The keytab file containing multiple identities should then be copied over to the keytabs location on the host where the view is hosted.

```
cp /path/to/keytab/slider-user.STORM.nimbus.keytab /etc/security/keytabs/
cp /path/to/keytab/slider-user.STORM.client.keytab /etc/security/keytabs/
```

Change file permissions so that only necessary users can access it.
