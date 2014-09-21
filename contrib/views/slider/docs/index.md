# Slider Apps View

## Security Guide
*Slider Apps View* can optionally connect to a Kerberos secured cluster by following the below steps.

#### Step-1: Deploy a HDP cluster and secure it using *Kerberos*
After deploying a HDP cluster through Ambari, it can be secured by using the *Enable Security* button in *Admin > Seurity* page.

#### Step-2: Create *Kerberos* principal for view
We need to provide a *Kerberos* identity for the process in which the view is run. We shall identify the user as `view-principal`. Since views are generally hosted by Ambari server, typically this can be named as *ambari*.

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

#### Step-3: Configure *proxyuser* for created principal
Add the following configurations in *Custom core-site* section of *HDFS* service.

* hadoop.proxyuser.ambari.groups = *
* hadoop.proxyuser.ambari.hosts = `view-server-host`

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

#### Step-4: Create *Slider Apps View* with security parameters

From *Ambari-Admin* create a *Slider Apps View* with the below parameters populated

* slider.security.enabled = true
* yarn.resourcemanager.principal = `rm/_HOST@EXAMPLE.COM`
* dfs.namenode.kerberos.principal = `nn/_HOST@EXAMPLE.COM`
* view.kerberos.principal = `view-principal`
* view.kerberos.principal.keytab = `/etc/security/keytabs/view-principal.headless.keytab`