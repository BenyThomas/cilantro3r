/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.controller.auth;


import com.entities.MoFP.JwtResponse;
import com.entities.MoFP.LoginRequest;
import com.security.JwtUtils;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 * @author samichael
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping(value = "/login" , produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        LOGGER.info("{}", loginRequest);
        String mofpApiKey = "Y7hHwyxqQ9nqqtnRtL3j8q3ZoYnbGxd2qld9PunbxBplfVTE9Qy1vzpr0CHua33B2BQCOsvSFmA7Jnsue0upcI1RM0oVlu72" +
                "rb9RTZgXp3fv950NVcJw2ekGAc4HPkV3cu0AYw1XopALBmywQ08FmUKRYITE95kLwy53OZRM7LIp3Hgw5GZOQZEu9v7rackVhPVOZ2x" +
                "1f8yboDXPXkmPmih9mcOjhDnup8ugKMlbKhBfccSmA06F1Vrmmy21rD0B";
        String apiKey = request.getHeader("x-api-key");
        if (!Objects.equals(mofpApiKey, apiKey)) {
            JSONObject resp = new JSONObject();
            resp.put("error", "99");
            resp.put("message", "Invalid API KEY " + apiKey);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp.toString());
        }

        if (Objects.equals(mofpApiKey, apiKey) && (Objects.equals(loginRequest.getUsername(), "TANZTZT0")
                || Objects.equals(loginRequest.getUsername(), "TANZTZTZ")) &&
                Objects.equals(loginRequest.getPassword(), "p@ssw0rd")) {
            try {
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                String jwt = jwtUtils.generateJwtToken(authentication);
                Calendar cal = Calendar.getInstance();
                cal.setTime(new Date());
                cal.add(Calendar.MILLISECOND, 90000);
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                String expiryDate = formatter.format(cal.getTime());
                return ResponseEntity.ok(new JwtResponse("Y", "Successfully Login", jwt,
                        "N", expiryDate));
            } catch (Exception e) {
                e.printStackTrace();
                JSONObject resp = new JSONObject();
                resp.put("error", "99");
                resp.put("message", "Application Error Please contact Administrator.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp.toString());
            }
        } else {
            JSONObject resp = new JSONObject();
            resp.put("error", "99");
            resp.put("message", "Application Error Please contact Administrator.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp.toString());
        }
    }
}
