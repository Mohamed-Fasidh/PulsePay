package com.pulsepay.security;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsepay.common.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service public class TokenService {
 private final ObjectMapper mapper; private final byte[] secret;
 public TokenService(ObjectMapper mapper, @Value("${pulsepay.jwt.secret:${JWT_SECRET:development-secret-that-must-be-replaced-32}}") String secret) { this.mapper=mapper; this.secret=secret.getBytes(StandardCharsets.UTF_8); }
 public String issue(MerchantPrincipal p, long seconds) { try { Map<String,Object> h=Map.of("alg","HS256","typ","JWT"); Map<String,Object> c=Map.of("sub",p.userId().toString(),"mid",p.merchantId().toString(),"role",p.role(),"exp",Instant.now().getEpochSecond()+seconds); String a=b64(mapper.writeValueAsBytes(h)), b=b64(mapper.writeValueAsBytes(c)); return a+"."+b+"."+b64(sign(a+"."+b)); } catch(Exception e){throw new IllegalStateException(e);} }
 public MerchantPrincipal parse(String token) { try { String[] p=token.split("\\."); if(p.length!=3 || !MessageDigest.isEqual(sign(p[0]+"."+p[1]), Base64.getUrlDecoder().decode(p[2]))) throw unauthorized(); Map<String,Object> c=mapper.readValue(Base64.getUrlDecoder().decode(p[1]),new TypeReference<>(){}); if(((Number)c.get("exp")).longValue()<Instant.now().getEpochSecond()) throw unauthorized(); return new MerchantPrincipal(UUID.fromString((String)c.get("sub")),UUID.fromString((String)c.get("mid")),(String)c.get("role")); }catch(ApiException e){throw e;}catch(Exception e){throw unauthorized();} }
 private byte[] sign(String input) throws Exception { Mac mac=Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(secret,"HmacSHA256")); return mac.doFinal(input.getBytes(StandardCharsets.UTF_8)); }
 private String b64(byte[] bytes){return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);} private ApiException unauthorized(){return new ApiException(HttpStatus.UNAUTHORIZED,"INVALID_TOKEN","Authentication token is invalid or expired.");}
}

