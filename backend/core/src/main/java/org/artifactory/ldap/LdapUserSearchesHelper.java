package org.artifactory.ldap;

import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.descriptor.security.ldap.SearchPattern;
import org.artifactory.security.ldap.NewFilterBasedLdapUserSearch;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author gidis
 */
public class LdapUserSearchesHelper {
    public static List<LdapUserSearch> getLdapUserSearches(ContextSource ctx, LdapSetting settings, boolean aol) {
        SearchPattern searchPattern = settings.getSearch();
        String[] searchBases;
        if (searchPattern.getSearchBase() == null) {
            searchBases = new String[]{""};
        } else {
            searchBases = searchPattern.getSearchBase().split(Pattern.quote("|"));
        }
        boolean useObjectInjectionProtection = isObjectInjectionProtection(settings, aol);
        ArrayList<LdapUserSearch> result = new ArrayList<>();
        for (String base : searchBases) {
            LdapUserSearch userSearch;
            String filter = searchPattern.getSearchFilter();
            BaseLdapPathContextSource baseLdapCtx = (BaseLdapPathContextSource) ctx;
            if (useObjectInjectionProtection) {
                NewFilterBasedLdapUserSearch search = new NewFilterBasedLdapUserSearch(base, filter, baseLdapCtx);
                search.setSearchSubtree(searchPattern.isSearchSubTree());
                userSearch = search;
            } else {
                FilterBasedLdapUserSearch search = new FilterBasedLdapUserSearch(base, filter, baseLdapCtx);
                search.setSearchSubtree(searchPattern.isSearchSubTree());
                userSearch = search;
            }
            result.add(userSearch);
        }
        return result;
    }

    public static boolean isObjectInjectionProtection(LdapSetting settings, boolean isAol) {
        if (isAol) {
            return true;
        }
        Boolean ldapPoisoningProtection = settings.getLdapPoisoningProtection();
        return ldapPoisoningProtection == null ? true : ldapPoisoningProtection;
    }
}
