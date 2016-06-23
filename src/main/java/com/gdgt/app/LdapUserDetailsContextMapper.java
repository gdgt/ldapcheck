package com.gdgt.app;

import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.security.core.userdetails.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by michalis on 22/06/2016.
 */
public class LdapUserDetailsContextMapper implements UserDetailsContextMapper {
    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<? extends GrantedAuthority> authorities) {

        List<GrantedAuthority> mappedAuthorities = new ArrayList<>();
        UserDetails user = new LdapUserDetailsMapper().mapUserFromContext(ctx, username, authorities);

        System.out.println("DN: " + ctx.getNameInNamespace());
        System.out.println("AUTH_USER_NAME: " + user.getUsername());
        System.out.println("MemberOf: " + user.getAuthorities());
        System.out.println("Enabled: " + user.isEnabled());
        System.out.println("Account Not Locked: " + user.isAccountNonLocked());
        System.out.println("Account Not Expired: " + user.isAccountNonExpired());
        System.out.println("Credentials Not Expired: " + user.isCredentialsNonExpired());

        return new User(username, "", true, true, true, true, mappedAuthorities);
    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        throw new UnsupportedOperationException("LdapUserDetailsContextMapper only supports reading from a context");
    }
}
