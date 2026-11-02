package com.pulsepay.security;
import jakarta.servlet.*; import jakarta.servlet.http.*; import java.io.IOException; import java.nio.charset.StandardCharsets; import java.security.MessageDigest; import java.util.HexFormat; import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; import org.springframework.security.core.authority.SimpleGrantedAuthority; import org.springframework.security.core.context.SecurityContextHolder; import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.filter.OncePerRequestFilter;
@Component public class JwtFilter extends OncePerRequestFilter {
 private final TokenService tokens; private final JdbcTemplate db; public JwtFilter(TokenService tokens,JdbcTemplate db){this.tokens=tokens;this.db=db;}
 @Override protected boolean shouldNotFilter(HttpServletRequest r){return r.getRequestURI().startsWith("/api/v1/auth/")||r.getRequestURI().startsWith("/actuator/")||r.getRequestURI().equals("/api/v1/status");}
 @Override protected void doFilterInternal(HttpServletRequest r,HttpServletResponse s,FilterChain c)throws ServletException,IOException {String h=r.getHeader("Authorization"); if(h!=null&&h.startsWith("Bearer ")){authenticate(tokens.parse(h.substring(7)));}else{String key=r.getHeader("X-Api-Key");if(key!=null&&!key.isBlank()){var principals=db.query("select k.merchant_id,(select mm.user_id from merchant_memberships mm where mm.merchant_id=k.merchant_id order by mm.created_at limit 1) from api_keys k where k.secret_hash=? and k.revoked_at is null",(rs,i)->new MerchantPrincipal(rs.getObject(2,UUID.class),rs.getObject(1,UUID.class),"API_KEY"),sha(key));if(!principals.isEmpty()){authenticate(principals.getFirst());db.update("update api_keys set last_used_at=now() where secret_hash=? and revoked_at is null",sha(key));}}} c.doFilter(r,s);}
 private void authenticate(MerchantPrincipal p){var a=new UsernamePasswordAuthenticationToken(p,null,java.util.List.of(new SimpleGrantedAuthority("ROLE_"+p.role())));SecurityContextHolder.getContext().setAuthentication(a);}
 private String sha(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
