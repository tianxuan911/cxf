/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.sts.token.provider;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Properties;

import org.apache.cxf.jaxws.context.WebServiceContextImpl;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.cache.DefaultInMemoryTokenStore;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.token.provider.jwt.JWTTokenProvider;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.junit.Assert;

/**
 * Some unit tests for creating JWTTokens.
 */
public class JWTTokenProviderTest extends org.junit.Assert {
    
    private static TokenStore tokenStore = new DefaultInMemoryTokenStore();
    
    @org.junit.Test
    public void testCreateUnsignedJWT() throws Exception {
        TokenProvider jwtTokenProvider = new JWTTokenProvider();
        ((JWTTokenProvider)jwtTokenProvider).setSignToken(false);
        
        TokenProviderParameters providerParameters = createProviderParameters();
        
        assertTrue(jwtTokenProvider.canHandleToken(JWTTokenProvider.JWT_TOKEN_TYPE));
        TokenProviderResponse providerResponse = jwtTokenProvider.createToken(providerParameters);
        assertTrue(providerResponse != null);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        String token = (String)providerResponse.getToken();
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 2);
        
        // Validate the token
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(token);
        JwtToken jwt = jwtConsumer.getJwtToken();
        Assert.assertEquals("alice", jwt.getClaim(JwtConstants.CLAIM_SUBJECT));
        Assert.assertEquals(providerResponse.getTokenId(), jwt.getClaim(JwtConstants.CLAIM_JWT_ID));
        Assert.assertEquals(providerResponse.getCreated().getTime() / 1000L, 
                            jwt.getClaim(JwtConstants.CLAIM_ISSUED_AT));
        Assert.assertEquals(providerResponse.getExpires().getTime() / 1000L, 
                            jwt.getClaim(JwtConstants.CLAIM_EXPIRY));
    }
    
    @org.junit.Test
    public void testCreateSignedJWT() throws Exception {
        TokenProvider jwtTokenProvider = new JWTTokenProvider();
        ((JWTTokenProvider)jwtTokenProvider).setSignToken(true);
        
        TokenProviderParameters providerParameters = createProviderParameters();
        
        assertTrue(jwtTokenProvider.canHandleToken(JWTTokenProvider.JWT_TOKEN_TYPE));
        TokenProviderResponse providerResponse = jwtTokenProvider.createToken(providerParameters);
        assertTrue(providerResponse != null);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        String token = (String)providerResponse.getToken();
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
        
        // Validate the token
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(token);
        JwtToken jwt = jwtConsumer.getJwtToken();
        Assert.assertEquals("alice", jwt.getClaim(JwtConstants.CLAIM_SUBJECT));
        Assert.assertEquals(providerResponse.getTokenId(), jwt.getClaim(JwtConstants.CLAIM_JWT_ID));
        Assert.assertEquals(providerResponse.getCreated().getTime() / 1000L, 
                            jwt.getClaim(JwtConstants.CLAIM_ISSUED_AT));
        Assert.assertEquals(providerResponse.getExpires().getTime() / 1000L, 
                            jwt.getClaim(JwtConstants.CLAIM_EXPIRY));
        
        // Verify Signature
        Crypto crypto = providerParameters.getStsProperties().getSignatureCrypto();
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias(providerParameters.getStsProperties().getSignatureUsername());
        X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
        assertNotNull(certs);
        
        assertTrue(jwtConsumer.verifySignatureWith(certs[0], SignatureAlgorithm.RS256));
    }
    
    @org.junit.Test
    public void testCachedSignedJWT() throws Exception {
        TokenProvider jwtTokenProvider = new JWTTokenProvider();
        ((JWTTokenProvider)jwtTokenProvider).setSignToken(true);
        
        TokenProviderParameters providerParameters = createProviderParameters();
        
        assertTrue(jwtTokenProvider.canHandleToken(JWTTokenProvider.JWT_TOKEN_TYPE));
        TokenProviderResponse providerResponse = jwtTokenProvider.createToken(providerParameters);
        assertTrue(providerResponse != null);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        String token = (String)providerResponse.getToken();
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
        
        // Validate the token
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(token);
        JwtToken jwt = jwtConsumer.getJwtToken();
        Assert.assertEquals("alice", jwt.getClaim(JwtConstants.CLAIM_SUBJECT));
        Assert.assertEquals(providerResponse.getTokenId(), jwt.getClaim(JwtConstants.CLAIM_JWT_ID));
        Assert.assertEquals(providerResponse.getCreated().getTime() / 1000L, 
                            jwt.getClaim(JwtConstants.CLAIM_ISSUED_AT));
        Assert.assertEquals(providerResponse.getExpires().getTime() / 1000L, 
                            jwt.getClaim(JwtConstants.CLAIM_EXPIRY));
        
        // Check that the token is stored correctly in the cache
        String signature = token.substring(token.lastIndexOf(".") + 1);
        SecurityToken secToken = tokenStore.getToken(Integer.toString(Arrays.hashCode(signature.getBytes())));
        Assert.assertNotNull(secToken);
    }
    
    private TokenProviderParameters createProviderParameters() throws WSSecurityException {
        TokenProviderParameters parameters = new TokenProviderParameters();
        
        TokenRequirements tokenRequirements = new TokenRequirements();
        tokenRequirements.setTokenType(JWTTokenProvider.JWT_TOKEN_TYPE);
        parameters.setTokenRequirements(tokenRequirements);
        
        KeyRequirements keyRequirements = new KeyRequirements();
        parameters.setKeyRequirements(keyRequirements);

        parameters.setTokenStore(tokenStore);
        
        parameters.setPrincipal(new CustomTokenPrincipal("alice"));
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        WebServiceContextImpl webServiceContext = new WebServiceContextImpl(msgCtx);
        parameters.setWebServiceContext(webServiceContext);
        
        parameters.setAppliesToAddress("http://dummy-service.com/dummy");
        
        // Add STSProperties object
        StaticSTSProperties stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        parameters.setStsProperties(stsProperties);
        
        parameters.setEncryptionProperties(new EncryptionProperties());
        
        return parameters;
    }
    
    private Properties getEncryptionProperties() {
        Properties properties = new Properties();
        properties.put(
            "org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin"
        );
        properties.put("org.apache.wss4j.crypto.merlin.keystore.password", "stsspass");
        properties.put("org.apache.wss4j.crypto.merlin.keystore.file", "stsstore.jks");
        
        return properties;
    }
    
  
    
}
