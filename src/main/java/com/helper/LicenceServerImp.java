/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.helper;

import de.schlichtherle.license.KeyStoreParam;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author HP
 */
public class LicenceServerImp implements KeyStoreParam{
     @Override
            public InputStream getStream() throws IOException {
                final String resourceName = "";
                final InputStream in = getClass().getResourceAsStream(resourceName);
                if (in == null) {
                    System.err.println("Could not load file: " + resourceName);
                    throw new FileNotFoundException(resourceName);
                }
                return in;
            }

            @Override
            public String getAlias() {
                return "";
            }

            @Override
            public String getStorePwd() {
                return "";
            }

            @Override
            public String getKeyPwd() {
                return "";
            }
}
