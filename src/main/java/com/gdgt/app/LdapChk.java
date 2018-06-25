package com.gdgt.app;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.authentication.AbstractLdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Michalis Kongtonk
 * Hello LdapChk!
 */
public class LdapChk {
    private static final Logger logger = LogManager.getLogger("LdapChk");

    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws JSONException {
        PropertiesConfiguration config = new PropertiesConfiguration();
        AbstractLdapAuthenticationProvider authProvider = null;
        LdapUserDetailsContextMapper ctxMapper = new LdapUserDetailsContextMapper();
        Authentication result = null;

        if (args.length == 0) {
            System.out.println("Properties file missing.");
            System.exit(1);
        } else {
            File ldapConfigFile = new File(args[0]);
            try {
                config.setDelimiterParsingDisabled(true);
                config.load(ldapConfigFile);
            } catch (ConfigurationException e) {
                e.printStackTrace();
            }
        }

        try {
            if (StringUtils.isEmpty(config.getString("LDAP_TYPE"))) {
                // Attempt AD
                logger.info("Using ActiveDirectory authentication with NT domain: " +
                        config.getString("NT_DOMAIN"));
                authProvider = new ActiveDirectoryLdapAuthenticationProvider(
                        config.getString("NT_DOMAIN"), config.getString("LDAP_URL"));
            } else {
                if (config.getString("LDAP_TYPE").toUpperCase().contentEquals("LDAP")) {
                    // Setup LDAP bind
                    logger.info("Using LDAP authentication with properties:" );
                    logger.info("LDAP_BIND_DN: " + config.getString("LDAP_BIND_DN"));
                    logger.info("LDAP_USER_SEARCH_BASE: " + config.getString("LDAP_USER_SEARCH_BASE"));
                    logger.info("LDAP_USER_SEARCH_FILTER: " + config.getString("LDAP_USER_SEARCH_FILTER"));
                    logger.info("LDAP_GROUP_SEARCH_BASE: " + config.getString("LDAP_GROUP_SEARCH_BASE"));
                    logger.info("LDAP_GROUP_SEARCH_FILTER: " + config.getString("LDAP_GROUP_SEARCH_FILTER"));
                    LdapContextSource ctxSource = new LdapContextSource();
                    ctxSource.setUrl(config.getString("LDAP_URL"));
                    ctxSource.setUserDn(config.getString("LDAP_BIND_DN"));
                    ctxSource.setPassword(config.getString("LDAP_BIND_PW"));
                    ctxSource.setReferral(config.getString("FOLLOW_REFERRALS"));
                    ctxSource.afterPropertiesSet();


                    // Attempt bind with LDAP
                    BindAuthenticator bindAuth = new BindAuthenticator(ctxSource);
                    bindAuth.setUserSearch(new FilterBasedLdapUserSearch(
                            config.getString("LDAP_USER_SEARCH_BASE"),
                            config.getString("LDAP_USER_SEARCH_FILTER"), ctxSource));

                    if (StringUtils.isNotEmpty(config.getString("LDAP_DN_PATTERN"))) {
                        bindAuth.setUserDnPatterns(new String[]{config.getString("LDAP_DN_PATTERN")});
                    }
                    bindAuth.afterPropertiesSet();

                    DefaultLdapAuthoritiesPopulator ldapAuthorities = new DefaultLdapAuthoritiesPopulator(ctxSource,
                            config.getString("LDAP_GROUP_SEARCH_BASE") != null ?
                                    config.getString("LDAP_GROUP_SEARCH_BASE") : "");

                    if (StringUtils.isNotEmpty(config.getString("LDAP_GROUP_SEARCH_FILTER"))) {
                        ldapAuthorities.setGroupSearchFilter(config.getString("LDAP_GROUP_SEARCH_FILTER"));
                    }

                    ldapAuthorities.setSearchSubtree(true);
                    ldapAuthorities.setConvertToUpperCase(false);
                    ldapAuthorities.setRolePrefix("");
                    authProvider = new LdapAuthenticationProvider(bindAuth, ldapAuthorities);
                }
            }

            // Generate a token for the credentials provided (AUTH_USER_NAME/AUTH_USER_PASSWORD)
            // and attempt to authenticate to LDAP/AD
            List<GrantedAuthority> grantedAuth = new ArrayList<>();
            grantedAuth.add(new SimpleGrantedAuthority("SOME_GRANTS"));
            final UserDetails principal = new User(
                    config.getString("AUTH_USER_NAME"),
                    config.getString("AUTH_USER_PASSWORD"), grantedAuth);
            final Authentication authToken = new UsernamePasswordAuthenticationToken(principal,
                    config.getString("AUTH_USER_PASSWORD"), grantedAuth);

            if (authProvider != null) {
                authProvider.setUserDetailsContextMapper(ctxMapper);
                result = authProvider.authenticate(authToken);
                logger.info("Authenticated: " + result.isAuthenticated());
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (result != null && result.isAuthenticated()) {
            Iterator<String> keys = config.getKeys();
            JSONArray json = new JSONArray();
            final List<String> IGNORE_KEY = Arrays
                    .asList("AUTH_USER_PASSWORD", "AUTH_USER_NAME", "FOLLOW_REFERRALS");
            while (keys.hasNext()) {
                String key = keys.next();
                if (StringUtils.isNotEmpty(config.getString(key)) && !IGNORE_KEY.contains(key)) {
                    json.put(new JSONObject()
                            .put("name", key)
                            .put("value", StringUtils.contains("LDAP_TYPE", key) ?
                                    config.getString(key).toUpperCase() : config.getString(key)));
                }
            }
            JSONObject item = new JSONObject().put("items", json);
            logger.info("Cloudera Manager REST API example below");
            logger.info("curl -X PUT -u admin:admin -H 'Content-Type:application/json' -d '"
                    + item.toString() + "' http://$(hostname -f):7180/api/v12/cm/config");
        }
    }
}

