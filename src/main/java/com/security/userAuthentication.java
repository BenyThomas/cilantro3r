/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.security;

import com.entities.Modules;
import com.repository.User_M;
import com.service.CorebankingService;
import com.service.XMLParserService;
import com.config.SYSENV;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.servlet.http.HttpSession;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import philae.api.BnUser;
import philae.api.LoginUser;
import philae.api.UlRequest;
import philae.api.UlResponse;

/**
 *
 * @author MELLEJI
 */
@Component
public class userAuthentication implements AuthenticationProvider {

    //@Autowired
    //@Qualifier("xapiAuthenticator")
    //  Authenticator xapiAuthenticator;
    // @Autowired
    //  UserRepository userRepo;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    HttpSession httpSession;

    @Autowired
    User_M userRepo;

    @Autowired
    CorebankingService corebanking;

    @Autowired
    SYSENV systemVariables;

    @Autowired
    XMLParserService xmlService;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(XMLParserService.class);
    private static final String INIT_VECTOR = "8765432112345678";

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        int isAuthenticated = -1;
        List<GrantedAuthority> grantedAuths = new ArrayList<>();
        String nameDomain = authentication.getName();
        //extract login domain from the login form
        String[] parts = nameDomain.split("::");
        if (parts.length == 1) {
            String username = parts[0];
            String password = authentication.getCredentials().toString();
            return new UsernamePasswordAuthenticationToken(username, password, grantedAuths);
        }
        String username = parts[0];
        String domain = parts[1];
        System.out.println("LOGIN TYPE: " + domain);
        String password = authentication.getCredentials().toString();

        if (domain.equalsIgnoreCase("rubikon")) {
            // and authenticate against the third-party system
            /*  Authenticator.setDefault(xapiAuthenticator);
            XAService_Service service = new XAService_Service();
            XAService port = service.getXAServicePort();
            UlRequest loginRequest = new UlRequest();
            loginRequest.setReference(String.valueOf(System.currentTimeMillis()));
            loginRequest.setUserName(username);
            loginRequest.setPassword(PassCreaterConfig.encryptXapi(loginRequest.getReference(), password));
            UlResponse responseLogin = port.loginUser(loginRequest);
            if (responseLogin != null) {
                LOGGER.info("Loging username: " + username + "[result: " + responseLogin.getResult() + ", message: " + responseLogin.getMessage() + ", domain: " + domain + "]");
                if (responseLogin.getResult() == 0) {
                    isAuthenticated = 1;
                }
            }*/
        } else if (domain.equalsIgnoreCase("domain")) {
            // and authenticate against the ldap system
            String userId, RoleId;
            if (ldapAuth(username, password)) {
                List<Map<String, Object>> userData = userRepo.getUserRole(username);
                if (userData != null) {
                    isAuthenticated = 1;
                    userId = userData.get(0).get("userID").toString();
                    RoleId = userData.get(0).get("roleID").toString();
                    List<Modules> modules = userRepo.getModules(userId, RoleId);
                    grantedAuths = modules.get(0).getAuthorities();

                    httpSession.setAttribute("username", username);
                    httpSession.setAttribute("modules", modules);
                    httpSession.setAttribute("roleId", RoleId);
                    httpSession.setAttribute("userId", userId);
                    httpSession.setAttribute("branchCode", userData.get(0).get("branch_no").toString());
                    //update the last login in users table;
                    userRepo.updateLastLoginEvent(userId);
                } else {
                    isAuthenticated = 0;
                    throw new BadCredentialsException("Domain:" + domain + "-This user does not exist on cilantro system");
                }
            } else {
                isAuthenticated = 0;
            }

        } else if (domain.equalsIgnoreCase("Enterprise")) {
            
            if (authenticateCBS(username, password)) {
                String userId, RoleId;
                List<Map<String, Object>> userData = userRepo.getUserRole(username);
//                LOGGER.info("CHECKING IF USER IS MAPPED: " + userData.size());
                if (userData.size() > 0) {
                    isAuthenticated = 1;
                    userId = userData.get(0).get("userID").toString();
                    RoleId = userData.get(0).get("roleID").toString();
                    LOGGER.info("USER_ID:{} ROLE_ID:{}",userId, RoleId);
                    List<Modules> modules = userRepo.getModules(userId, RoleId);
                    //add permission to spring security
                    grantedAuths = modules.get(0).getAuthorities();
                    httpSession.setAttribute("username", username);
                    httpSession.setAttribute("modules", modules);
                    httpSession.setAttribute("roleId", RoleId);
                    httpSession.setAttribute("userId", userId);
                    httpSession.setAttribute("cilantroRole", userData.get(0).get("roleName").toString().toUpperCase());
                    httpSession.setAttribute("branchCode", userData.get(0).get("branch_no").toString());
                    //update the last login in users table;
                    userRepo.updateLastLoginEvent(userId);
                } else {
                    LOGGER.info("BAD REQUEST: NOT AUTHENTICATED");
                    isAuthenticated = 0;
                    throw new BadCredentialsException("Domain:" + domain + " - system authentication failed");
                }
            } else {
                isAuthenticated = 0;
                throw new BadCredentialsException(" - User does not exist on cilantro system");
            }

        }
        if (isAuthenticated == 1) {
            return new UsernamePasswordAuthenticationToken(username, password, grantedAuths);
        } else {
            throw new BadCredentialsException("Domain:" + domain + " - system authentication failed");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }

    public boolean ldapAuth(String username, String password) {
        boolean result = false;

        try {
            // Set up the environment for creating the initial context
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, "ldap://" + systemVariables.DOMAIN_CONTROLLER_LDAP_AUTHENTICATION + ":" + systemVariables.DOMAIN_CONTROLLER_PORT);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, systemVariables.DOMAIN_CONTROLLER_NAME + "\\" + username);
            //we have 2 \\ because it's a escape char
            env.put(Context.SECURITY_CREDENTIALS, password);
            // Create the initial context
            DirContext ctx = new InitialDirContext(env);

            result = true;
            ctx.close();

        } catch (NamingException e) {
            System.out.println("LDAP ERROR: ");
        }

        return result;

    }

    public boolean authenticateCBS(String username, String password) {
        boolean resp = false;
        UlRequest loginRequest = new UlRequest();
        String reference = String.valueOf(System.currentTimeMillis());
        loginRequest.setReference(reference);
        loginRequest.setUserName(username);
        loginRequest.setPassword(encryptXapi(reference, password));

        //create a login request message
        LoginUser loginUser = new LoginUser();
        loginUser.setRequest(loginRequest);
        String loginXML = XMLParserService.jaxbGenericObjToXML(loginUser, Boolean.FALSE, Boolean.TRUE);
        if (systemVariables.ACTIVE_PROFILE.equalsIgnoreCase("prod")) {
            UlResponse loginWs = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCore(loginXML, "api:loginUser"), UlResponse.class);
            philae.ach.UlResponse LoginTACH = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCore(loginXML, "api:loginUser"), philae.ach.UlResponse.class);
            if (loginWs == null || LoginTACH == null) {
                System.out.println("Its null point user login");
                return resp;
            }
            if (loginWs.getResult() == 0) {
                BnUser user = loginWs.getUser();
                philae.ach.BnUser achUser = LoginTACH.getUser();
                httpSession.setAttribute("userCorebanking", user);
                httpSession.setAttribute("achUserCorebanking", achUser);
                resp = true;
            }
            return resp;
        } else {
//            if(systemVariables.ACTIVE_PROFILE.equalsIgnoreCase("uat") ){
                String simulatedLiin = "<?xml version=\"1.0\" ?><return><result>0</result><reference>1720602966124</reference><message>Success</message><availableBalance xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:nil=\"true\"></availableBalance><ledgerBalance xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:nil=\"true\"></ledgerBalance><txnId xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:nil=\"true\"></txnId><user><userId>1476</userId><staffNumber>1425</staffNumber><staffName>LINETT KEFFA</staffName><userName>LK1425</userName><roleId>69</roleId><role>Branch Operation Manager</role><branchId>-5</branchId><branchName>IT AND OPERATIONS</branchName><roles><role><roleId>59</roleId><userId>1476</userId><role>Bank Teller</role><userName>LK1425</userName><branchId>-26</branchId><buRoleId>4650</buRoleId><userRoleId>133788</userRoleId><supervisor>N</supervisor><branchCode>340</branchCode><branchName>MTWARA</branchName><limits><limit><creditLimit>500000000</creditLimit><currency>TZS</currency><debitLimit>10000000</debitLimit><roleId>59</roleId></limit></limits><drawers><drawer><drawerId>250</drawerId><drawerNumber>TELLER 1</drawerNumber><drawerAccount>1-340-00-1101-1101004</drawerAccount><openDate>2024-05-22T16:36:13+03:00</openDate><status>O</status><currencies><currency><code>USD</code><id>841</id><name>Us Dollar</name><points>2</points></currency><currency><code>GBP</code><id>863</id><name>British Pound</name><points>2</points></currency><currency><code>EUR</code><id>864</id><name>Euro</name><points>2</points></currency><currency><code>TZS</code><id>852</id><name>Tanzanian Shilling</name><points>2</points></currency></currencies></drawer></drawers></role><role><roleId>143</roleId><userId>1476</userId><role>Branch Credit Officer</role><userName>LK1425</userName><branchId>-26</branchId><buRoleId>4630</buRoleId><userRoleId>33359</userRoleId><supervisor>N</supervisor><branchCode>340</branchCode><branchName>MTWARA</branchName><limits></limits><drawers></drawers></role><role><roleId>69</roleId><userId>1476</userId><role>Branch Operation Manager</role><userName>LK1425</userName><branchId>-26</branchId><buRoleId>4620</buRoleId><userRoleId>37760</userRoleId><supervisor>Y</supervisor><branchCode>340</branchCode><branchName>MTWARA</branchName><limits></limits><drawers></drawers></role><role><roleId>-87</roleId><userId>1476</userId><role>Chief Cashier</role><userName>LK1425</userName><branchId>-26</branchId><buRoleId>4670</buRoleId><userRoleId>30104</userRoleId><supervisor>N</supervisor><branchCode>340</branchCode><branchName>MTWARA</branchName><limits></limits><drawers></drawers></role><role><roleId>79</roleId><userId>1476</userId><role>Customers Service Officer</role><userName>LK1425</userName><branchId>-66</branchId><buRoleId>5690</buRoleId><userRoleId>133830</userRoleId><supervisor>N</supervisor><branchCode>173</branchCode><branchName>EXECUTIVE BRANCH</branchName><limits></limits><drawers></drawers></role><role><roleId>79</roleId><userId>1476</userId><role>Customers Service Officer</role><userName>LK1425</userName><branchId>-26</branchId><buRoleId>4660</buRoleId><userRoleId>21040</userRoleId><supervisor>N</supervisor><branchCode>340</branchCode><branchName>MTWARA</branchName><limits></limits><drawers></drawers></role><role><roleId>89</roleId><userId>1476</userId><role>Information Technology Officer</role><userName>LK1425</userName><branchId>-5</branchId><buRoleId>5583</buRoleId><userRoleId>51934</userRoleId><supervisor>Y</supervisor><branchCode>060</branchCode><branchName>IT AND OPERATIONS</branchName><limits></limits><drawers></drawers></role></roles></user></return>";
                UlResponse loginWs = XMLParserService.jaxbXMLToObject(simulatedLiin, UlResponse.class);
                philae.ach.UlResponse LoginTACH = XMLParserService.jaxbXMLToObject(simulatedLiin, philae.ach.UlResponse.class);
                if (loginWs == null || LoginTACH == null) {
                    System.out.println("Its null point user login");
                    return resp;
                }
                if (loginWs.getResult() == 0) {
                    BnUser user = loginWs.getUser();
                    philae.ach.BnUser achUser = LoginTACH.getUser();
                    httpSession.setAttribute("userCorebanking", user);
                    httpSession.setAttribute("achUserCorebanking", achUser);
                    resp = true;
                }
                return resp;
//            }


//            String simulatedLogin = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><return><result>0</result><reference>1613101783040</reference><message>Success</message><availableBalance/><ledgerBalance/><txnId/><user><roles><role><limits/><drawers/><branchCode>060</branchCode><branchId>-5</branchId><branchName>IT AND OPERATIONS</branchName><buRoleId>5583</buRoleId><role>Information Technology Officer</role><roleId>89</roleId><supervisor>Y</supervisor><userId>1022</userId><userName>MM1135</userName><userRoleId>14639</userRoleId></role></roles><branchId>-5</branchId><branchName>IT AND OPERATIONS</branchName><role>Information Technology Officer</role><roleId>89</roleId><staffName>Melleji Mollel</staffName><staffNumber>1135</staffNumber><userId>1022</userId><userName>MM1135</userName></user></return>";
//
//            UlResponse loginWs = XMLParserService.jaxbXMLToObject(simulatedLogin, UlResponse.class);
//            philae.ach.UlResponse LoginTACH = XMLParserService.jaxbXMLToObject(simulatedLogin, philae.ach.UlResponse.class);
//
//            if (loginWs == null || LoginTACH == null) {
//                System.out.println("Its null point user login");
//                return resp;
//            }
//
//            if (loginWs.getResult() == 0) {
//                BnUser user = loginWs.getUser();
//                philae.ach.BnUser achUser = LoginTACH.getUser();
//                httpSession.setAttribute("userCorebanking", user);
//                httpSession.setAttribute("achUserCorebanking", achUser);
//                resp = true;
//                return resp;
//            } else {
//                return resp;
//            }
      }
    }

    public static String encryptXapi(String key, String value) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(padText(key, 16, true).getBytes("UTF-8"), "AES"), new IvParameterSpec(INIT_VECTOR.getBytes("UTF-8")));
            return Base64.encodeBase64String(cipher.doFinal(value.getBytes()));
        } catch (Exception ex) {
            LOGGER.error("Exception:", ex);
        }
        return null;
    }

    private static String padText(String text, int length, boolean right) {
        return String.format("%" + (right ? "-" : "") + length + "s", text);
    }
}
