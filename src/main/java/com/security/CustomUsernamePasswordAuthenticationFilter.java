/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 *
 * @author MELLEJI
 */
public class CustomUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    public CustomUsernamePasswordAuthenticationFilter() {
        super();
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        String username = request.getParameter(getUsernameParameter());
        String password = request.getParameter(getPasswordParameter());
        System.out.println("username: " + username);
//        System.out.println("password: " + password);
        String loginType = request.getParameter("loginType");
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username + "::" + loginType, password);
        // Allow subclasses to set the "details" property
        setDetails(request, token);
        return this.getAuthenticationManager().authenticate(token);
    }

}
