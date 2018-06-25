package com.gdgt.app;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by michalis on 22/06/2016.
 */
public class LdapUserDetailsContextMapper implements UserDetailsContextMapper {
  private static final Logger logger = LogManager.getLogger("LdapUserDetailsContextMapper");

  @Override
  public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
                                        Collection<? extends GrantedAuthority> authorities) {

    List<GrantedAuthority> mappedAuthorities = new ArrayList<>();
    UserDetails user = new LdapUserDetailsMapper().mapUserFromContext(ctx, username, authorities);

    logger.info("DN: " + ctx.getNameInNamespace());
    logger.info("AUTH_USER_NAME: " + user.getUsername());
    logger.info("MemberOf: " + user.getAuthorities());
    logger.info("Enabled: " + user.isEnabled());
    logger.info("Account Not Locked: " + user.isAccountNonLocked());
    logger.info("Account Not Expired: " + user.isAccountNonExpired());
    logger.info("Credentials Not Expired: " + user.isCredentialsNonExpired());

    return new User(username, "", true, true, true, true, mappedAuthorities);
  }

  @Override
  public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
    throw new UnsupportedOperationException("LdapUserDetailsContextMapper only supports reading from a context");
  }
}
