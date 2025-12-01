/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.service;

import com.repository.SetupRepo;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Melleji Mollel
 */
@Service
public class LdapService {

    @Autowired
    SetupRepo setupRepo;

    private DirContext ldapContext;
    private static final Logger LOGGER = LoggerFactory.getLogger(LdapService.class);

    public void dowloadUserAccount(int userAccountStatus,String usernamee,String password,String domain2) {
        List<Map<String, Object>> domain = this.setupRepo.getDomainControllers(domain2);
        if (domain.size() == 1) {
            String domainId = domain.get(0).get("ID").toString();
            String dnName = domain.get(0).get("NAME").toString();
            String dnIpAddress = domain.get(0).get("DN_IP").toString();
            String dnPort = domain.get(0).get("DN_PORT").toString();
            String dnPrefix = domain.get(0).get("DN_PREFIX").toString();
            String dnSearchBase = domain.get(0).get("DN_SEARCH_BASE").toString();
            String dnUsername = usernamee;//domain.get(0).get("DN_USERNAME").toString();
            String dnPassword = password;// domain.get(0).get("DN_PASSWORD").toString();
            //dnPassword = password;//this.encry.decrypt(dnPassword);

            try {
                Hashtable<String, String> ldapEnv = new Hashtable<String, String>(11);
                ldapEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
                ldapEnv.put(Context.PROVIDER_URL, "ldap://" + dnIpAddress + ":" + dnPort);
                ldapEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
                //ldapEnv.put(Context.SECURITY_PRINCIPAL, "cn=administrateur,cn=users,dc=societe,dc=fr");
                //ldapEnv.put(Context.SECURITY_PRINCIPAL, "tpbbank\\michael.daniel");
                ldapEnv.put(Context.SECURITY_PRINCIPAL, dnPrefix + "\\" + dnUsername);
                ldapEnv.put(Context.SECURITY_CREDENTIALS, dnPassword);
                // ldapEnv.put(Context.SECURITY_CREDENTIALS, "mike.1234");
                //ldapEnv.put(Context.SECURITY_PROTOCOL, "ssl");
                //ldapEnv.put(Context.SECURITY_PROTOCOL, "simple");
                ldapContext = new InitialDirContext(ldapEnv);
                // Create the search controls         
                SearchControls searchCtls = new SearchControls();

                //Specify the attributes to return
                String[] returnedAtts = {"distinguishedName",
                    "sn", "cn", "OU",
                    "givenname",
                    "mail",
                    "telephonenumber", "canonicalName", "userAccountControl", "samAccountName", "accountExpires"};
                searchCtls.setReturningAttributes(returnedAtts);
                //Specify the search scope
                searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
                //specify the LDAP search filter
                String searchFilter = "(&(objectClass=user))";
                if (userAccountStatus == 1) {
                    //all active users with this filter:
                    searchFilter = "(&(objectClass=user)(objectCategory=person)(!(useraccountcontrol:1.2.840.113556.1.4.803:=2)))";
                } else if (userAccountStatus == 0) {
                    //all inactive users with this filter:
                    searchFilter = "(&(objectClass=user)(objectCategory=person)(useraccountcontrol:1.2.840.113556.1.4.803:=2))";
                }
                //Specify the Base for the search
                // String searchBase = "dc=tpbbank,dc=co,dc=tz";
                String searchBase = dnSearchBase;

                //initialize counter to total the results
                int totalResults = 0;
                int syncStatus;
                // Search for objects using the filter
                NamingEnumeration<SearchResult> answer = ldapContext.search(searchBase, searchFilter, searchCtls);
                //Loop through the search results
                while (answer.hasMoreElements()) {
                    SearchResult sr = (SearchResult) answer.next();
                    Attributes attrs = sr.getAttributes();
                    String branchName = sr.getNameInNamespace();
                    // int branchCode = getBranchNo(branchName);
                    String fullName = attrs.get("cn").get().toString();
                    String username = attrs.get("samAccountName").get().toString();
                    String email = username + "@" + dnName;
                    email = email.toLowerCase();
                    //syncStatus = this.userRepo.asyncUserAccount(username, fullName, email, Integer.valueOf(domainId), userAccountStatus);
                    LOGGER.info("Getting LDAP Account detail... Username: {},Fullname: {} Email: {} Branch: {} Account Status: {} ...", username, fullName, email, userAccountStatus);
                    totalResults++;
                }
                LOGGER.info("Total results: " + totalResults);
                ldapContext.close();
            } catch (NamingException e) {
                LOGGER.warn("LDAP error... ", e);
            }
        } else {
            LOGGER.info("Failed to get LDAP parameter... Service exit.");
        }
    }
    public boolean ldapAuth(String username, String password, String domain) {
        List<Map<String, Object>> domainDetails = this.setupRepo.getDomainControllers(domain);
        System.out.println("DOMAIN DETAILS: "+domainDetails.toString());
        if (domainDetails.size() == 1) {
            String dnIpAddress = domainDetails.get(0).get("DN_IP").toString();
            String dnPort = domainDetails.get(0).get("DN_PORT").toString();
            String dnPrefix = domainDetails.get(0).get("DN_PREFIX").toString();
            try {
                // Set up the environment for creating the initial context
                Hashtable<String, String> env = new Hashtable<>();
                env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
                env.put(Context.PROVIDER_URL, "ldap://" + dnIpAddress + ":" + dnPort);
                env.put(Context.SECURITY_AUTHENTICATION, "simple");
                env.put(Context.SECURITY_PRINCIPAL, dnPrefix + "\\" + username);
                //we have 2 \\ because it's a escape char
                env.put(Context.SECURITY_CREDENTIALS, password);
                // Create the initial context
                DirContext ctx = new InitialDirContext(env);
                boolean result = false;

                if (ctx != null) {
                    result = true;
                    ctx.close();
                }

                return result;
            } catch (NamingException e) {
                LOGGER.info("LDAP error...", e);
                System.out.println("Error Message:  "+e.getMessage()
                );
                return false;
            }
        } else {
            LOGGER.info("Failed to get LDAP parameter... Service exit.");
            return false;
        }
    }
}
