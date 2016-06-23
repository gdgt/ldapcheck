This assumes that you are familiar with LDAP, and have reviewed the documentation "Configuring External Authentication for Cloudera Manager" [1]

[1] ​http://www.cloudera.com/documentation/enterprise/latest/topics/cm_sg_external_auth.html

## 1. Download the attached file ldap-config.properties and the cmldap-v1.0.0-cm5-jar-with-dependencies.jar
The cmldap-v1.0.0-cm5-jar-with-dependencies.jar can be downloaded from https://github.com/gdgt/ldapcheck/releases
Also, a reference to ldap-config.properties can be downloaded from https://raw.githubusercontent.com/gdgt/ldapcheck/master/src/main/resources/ldap-config.properties

## 2. Edit the conf.properties
Below you'll see in Step 6 the reference for the sAMAccountName value to match the userPrincipalName value. This is  required when choosing AD as the authentication type.
#### 2.1. Set the "LDAP_TYPE" 
valid **LDAP** for LDAP and **none/empty** for Active Directory

#### 2.2. Set the "LDAP URL"
Sometimes this will be "ldap://test.com:389" and go directly to the ldap port.
In multi-domain environments, the above may not work, or if in an environment where the LDAP host is a sub-domain, like "ldap://prod.test.com" (but the userPrincipalName for the users is still user@test.com). In this case, bind to the GC instead. "ldap://prod.test.com:3268" or "ldap://test.com:3268".

#### 2.3. Set "LDAP_BIND_DN​" 
This can be the full bind dn: "cn=Administrator,cn=Users,dc=test,dc=com" or it can be the userPrincipalName: "Administrator@test.com". 

#### 2.4. Set "LDAP_BIND_PW" 

#### 2.5. Set "NT_DOMAIN"
Use the FQDN instead of the netBios name (this is because the user is searched by userPrincipalName in addition to the value you set for "LDAP User Search Filter"). 
So set this to "test.com" instead of "TEST". 

#### 2.6. Set "LDAP_USER_SEARCH_FILTER"
This is typically "sAMAccountName={0}". 
It may be different for your domain. Just note that it needs to match with the name that the users will log in as. Also, it must match with the part of the userPrincipalName that comes before "@test.com". 

#### 2.7. Set "LDAP_USER_SEARCH_BASE"
i.e., "DC=ad,DC=host,DC=cloudera,DC=com". 

#### 2.8. Set "LDAP_GROUP_SEARCH_FILTER"
This is typically "memberOf={0}" or 'member={0}'. 
This is the value that exists in the User's Object rather than the value in the Groups Object.

Example:

```
[cconner@cdh412-1 ~]$ ldapsearch -x -LLL -H ldap://test.com:389 -b "dc=test,dc=com" -D "Administrator@test.com" -w Password1 "(&(objectClass=user)(userPrincipalName=jdoe@test.com))" 
dn: CN=John Doe,CN=Users,DC=test,DC=com
....
distinguishedName: CN=John Doe,CN=Users,DC=test,DC=com 
memberOf: CN=hue_group,CN=Users,DC=test,DC=com
memberOf: CN=Domain Admins,CN=Users,DC=test,DC=com 
....
```

#### 2.9. Set LDAP_GROUP_SEARCH_BASE", i.e. "OU=Groups,DC=ad,DC=host,DC=cloudera,DC=com" 

## Example of ldap-config.properties file
````
LDAP_TYPE = LDAP
LDAP_URL = ldap://ad.host.cloudera.com:389
LDAP_BIND_DN = CN=Test1 Person,CN=Users,DC=ad,DC=host,DC=cloudera,DC=com
LDAP_BIND_PW = ldapBindPassword
NT_DOMAIN = ad.host.cloudera.com
LDAP_DN_PATTERN =
LDAP_USER_SEARCH_BASE = DC=ad,DC=host,DC=cloudera,DC=com
LDAP_USER_SEARCH_FILTER = userPrincipalName={0}
LDAP_GROUP_SEARCH_BASE = OU=Groups,DC=ad,DC=host,DC=cloudera,DC=com
LDAP_GROUP_SEARCH_FILTER = member={0}
LDAP_GROUPS_SEARCH_FILTER =
AUTH_USER_NAME = some-user-@ad.host.cloudera.com
AUTH_USER_PASSWORD = usersPassword
FOLLOW_REFERRALS = ignore
````


## 3. Execute the jar

Assuming you have `java` set in your PATH, execute your `cmldap-v1.0.0-cm5-jar-with-dependencies.jar` by issuing

`java -jar cmldap-v1.0.0-cm5-jar-with-dependencies.jar 'ldap-config.properties'`

Example output
```
Using LDAP authentication with properties:
LDAP_BIND_DN: CN=Test1 Person,CN=Users,DC=ad,DC=host,DC=cloudera,DC=com
LDAP_USER_SEARCH_BASE: DC=ad,DC=host,DC=cloudera,DC=com
LDAP_USER_SEARCH_FILTER: userPrincipalName={0}
LDAP_GROUP_SEARCH_BASE: OU=Groups,DC=ad,DC=host,DC=cloudera,DC=com
LDAP_GROUP_SEARCH_FILTER: member={0}
Jun 23, 2016 12:30:15 PM org.springframework.security.ldap.SpringSecurityLdapTemplate searchForSingleEntryInternal
INFO: Ignoring PartialResultException
Jun 23, 2016 12:30:16 PM org.springframework.ldap.core.LdapTemplate assureReturnObjFlagSet
INFO: The returnObjFlag of supplied SearchControls is not set but a ContextMapper is used - setting flag to true
DN: cn=cm-mko,ou=mko,dc=ad,dc=host,dc=cloudera,dc=com
AUTH_USER_NAME: some-user@ad.host.cloudera.com
MemberOf: [CM_USERS_GROUP, CM_ADMINS_GROUP]
Authenticated successful: true
Cloudera Manager REST API example below
curl -X PUT -u admin:admin -H 'Content-Type:application/json' -d '{"items":[{"name":"LDAP_TYPE","value":"LDAP"},{"name":"LDAP_URL","value":"ldap://ad.host.cloudera.com:389"},{"name":"LDAP_BIND_DN","value":"CN=Test1 Person,CN=Users,DC=ad,DC=host,DC=cloudera,DC=com"},{"name":"LDAP_BIND_PW","value":"bindPassword"},{"name":"NT_DOMAIN","value":"ad.host.cloudera.com"},{"name":"LDAP_USER_SEARCH_BASE","value":"DC=ad,DC=host,DC=cloudera,DC=com"},{"name":"LDAP_USER_SEARCH_FILTER","value":"userPrincipalName={0}"},{"name":"LDAP_GROUP_SEARCH_BASE","value":"OU=Groups,DC=ad,DC=host,DC=cloudera,DC=com"},{"name":"LDAP_GROUP_SEARCH_FILTER","value":"member={0}"}]}'
```