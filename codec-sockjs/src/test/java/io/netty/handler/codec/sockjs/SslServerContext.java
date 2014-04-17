/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.sockjs;

import io.netty.util.internal.SystemPropertyUtil;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates a {@link SSLContext} for just server certificates.
 *
 * <p>To use this SslServerContext
 * <dl>
 *   <dt>Step 1. Generate Your Key
 *   <dd>
 *     {@code keytool -genkey -keystore mySrvKeystore -keyalg RSA}.
 *     Make sure that you set the key password to be the same the key file password.
 *   <dt>Step 2. Specify your key store file and password as system properties
 *   <dd>
 *     {@code -Dkeystore.file.path=<path to mySrvKeystore> -Dkeystore.file.password=<password>}
 *   <dt>Step 3. Enable SSL in NettySockJsServer by adding:
 *   <dd>
 *     {@code sb.option(SSL_CONTEXT, SslServerSslContext.getInstance().serverContext());}
 *   <dt>Step 4. Run NettySockJsServer as a Java application
 *   <dd>
 *     Once started, you can test the server using curl:
 *     {@code curl -k -3  -v https://localhost:8081/echo/123/123/xhr}
 * </dl>
 */
public final class SslServerContext {

    private static final Logger logger = Logger.getLogger(SslServerContext.class.getName());
    private static final String PROTOCOL = "TLS";
    private final SSLContext _serverContext;

    /**
     * Returns the singleton instance for this class
     */
    public static SslServerContext getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * SingletonHolder is loaded on the first execution of Singleton.getInstance() or the first access to
     * SingletonHolder.INSTANCE, not before.
     *
     * See http://en.wikipedia.org/wiki/Singleton_pattern
     */
    private interface SingletonHolder {
        SslServerContext INSTANCE = new SslServerContext();
    }

    /**
     * Constructor for singleton
     */
    private SslServerContext() {
        SSLContext serverContext = null;
        try {
            // Key store (Server side certificate)
            String algorithm = SystemPropertyUtil.get("ssl.KeyManagerFactory.algorithm");
            if (algorithm == null) {
                algorithm = "SunX509";
            }

            try {
                String keyStoreFilePath = SystemPropertyUtil.get("keystore.file.path");
                String keyStoreFilePassword = SystemPropertyUtil.get("keystore.file.password");

                KeyStore ks = KeyStore.getInstance("JKS");
                FileInputStream fin = new FileInputStream(keyStoreFilePath);
                ks.load(fin, keyStoreFilePassword.toCharArray());

                // Set up key manager factory to use our key store
                // Assume key password is the same as the key store file
                // password
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
                kmf.init(ks, keyStoreFilePassword.toCharArray());

                // Initialise the SSLContext to work with our key managers.
                serverContext = SSLContext.getInstance(PROTOCOL);
                serverContext.init(kmf.getKeyManagers(), null, null);
            } catch (Exception e) {
                throw new Error("Failed to initialize the server-side SSLContext", e);
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error initializing SslContextManager.", ex);
            System.exit(1);
        } finally {
            _serverContext = serverContext;
        }
    }

    /**
     * Returns the server context with server side key store
     */
    public SSLContext serverContext() {
        return _serverContext;
    }

}
