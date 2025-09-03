package com.vscodelife.socketio.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 工具類
 * 提供 JWT 和 JWS 的產生與解析功能
 */
@Slf4j
public final class JwtUtil {

    // 預設過期時間（小時）
    private static final int DEFAULT_EXPIRATION_HOURS = 24;

    // 預設密鑰（生產環境中應該從配置文件讀取）
    private static final String DEFAULT_SECRET = "mySecretKeyForJWTTokenGenerationAndValidation12345";

    // 預設簽名密鑰
    private static final SecretKey DEFAULT_SIGNING_KEY = Keys.hmacShaKeyFor(DEFAULT_SECRET.getBytes());

    // 私有建構函數，防止實例化
    private JwtUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 產生 JWT Token
     * 
     * @param subject         主題（通常是用戶ID）
     * @param claims          自定義聲明
     * @param expirationHours 過期時間（小時）
     * @return JWT Token
     */
    public static String generateJwt(String subject, Map<String, Object> claims, int expirationHours) {
        try {
            Instant now = Instant.now();
            Instant expiration = now.plus(expirationHours, ChronoUnit.HOURS);

            JwtBuilder builder = Jwts.builder()
                    .subject(subject)
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(expiration))
                    .signWith(DEFAULT_SIGNING_KEY);

            // 添加自定義聲明
            if (claims != null && !claims.isEmpty()) {
                builder.claims(claims);
                // 重新設置 subject，因為 claims 會覆蓋
                builder.subject(subject);
            }

            return builder.compact();
        } catch (Exception e) {
            log.error("生成 JWT 時發生錯誤", e);
            throw new RuntimeException("Failed to generate JWT", e);
        }
    }

    /**
     * 產生 JWT Token（使用預設過期時間）
     * 
     * @param subject 主題
     * @param claims  自定義聲明
     * @return JWT Token
     */
    public static String generateJwt(String subject, Map<String, Object> claims) {
        return generateJwt(subject, claims, DEFAULT_EXPIRATION_HOURS);
    }

    /**
     * 產生 JWT Token（僅包含主題）
     * 
     * @param subject 主題
     * @return JWT Token
     */
    public static String generateJwt(String subject) {
        return generateJwt(subject, null, DEFAULT_EXPIRATION_HOURS);
    }

    /**
     * 產生 JWS Token（JSON Web Signature）
     * JWS 是經過簽名的 JWT
     * 
     * @param subject         主題
     * @param claims          自定義聲明
     * @param expirationHours 過期時間（小時）
     * @param signingKey      簽名密鑰
     * @return JWS Token
     */
    public static String generateJws(String subject, Map<String, Object> claims, int expirationHours,
            SecretKey signingKey) {
        try {
            Instant now = Instant.now();
            Instant expiration = now.plus(expirationHours, ChronoUnit.HOURS);

            JwtBuilder builder = Jwts.builder()
                    .subject(subject)
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(expiration))
                    .signWith(signingKey);

            if (claims != null && !claims.isEmpty()) {
                builder.claims(claims);
                builder.subject(subject);
            }

            return builder.compact();
        } catch (Exception e) {
            log.error("生成 JWS 時發生錯誤", e);
            throw new RuntimeException("Failed to generate JWS", e);
        }
    }

    /**
     * 產生 JWS Token（使用預設密鑰）
     * 
     * @param subject         主題
     * @param claims          自定義聲明
     * @param expirationHours 過期時間（小時）
     * @return JWS Token
     */
    public static String generateJws(String subject, Map<String, Object> claims, int expirationHours) {
        return generateJws(subject, claims, expirationHours, DEFAULT_SIGNING_KEY);
    }

    /**
     * 產生 JWS Token（使用預設過期時間和密鑰）
     * 
     * @param subject 主題
     * @param claims  自定義聲明
     * @return JWS Token
     */
    public static String generateJws(String subject, Map<String, Object> claims) {
        return generateJws(subject, claims, DEFAULT_EXPIRATION_HOURS, DEFAULT_SIGNING_KEY);
    }

    /**
     * 解析 JWT Token
     * 
     * @param token JWT Token
     * @return Claims 對象
     * @throws JwtException 如果 token 無效或過期
     */
    public static Claims parseJwt(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(DEFAULT_SIGNING_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token 已過期: {}", e.getMessage());
            throw new RuntimeException("JWT token has expired", e);
        } catch (UnsupportedJwtException e) {
            log.warn("不支援的 JWT Token: {}", e.getMessage());
            throw new RuntimeException("Unsupported JWT token", e);
        } catch (MalformedJwtException e) {
            log.warn("格式錯誤的 JWT Token: {}", e.getMessage());
            throw new RuntimeException("Malformed JWT token", e);
        } catch (SignatureException e) {
            log.warn("JWT Token 簽名驗證失敗: {}", e.getMessage());
            throw new RuntimeException("JWT signature validation failed", e);
        } catch (IllegalArgumentException e) {
            log.warn("JWT Token 為空或無效: {}", e.getMessage());
            throw new RuntimeException("JWT token is null or invalid", e);
        }
    }

    /**
     * 解析 JWS Token
     * 
     * @param token      JWS Token
     * @param signingKey 驗證用的簽名密鑰
     * @return Claims 對象
     * @throws JwtException 如果 token 無效或過期
     */
    public static Claims parseJws(String token, SecretKey signingKey) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWS Token 已過期: {}", e.getMessage());
            throw new RuntimeException("JWS token has expired", e);
        } catch (UnsupportedJwtException e) {
            log.warn("不支援的 JWS Token: {}", e.getMessage());
            throw new RuntimeException("Unsupported JWS token", e);
        } catch (MalformedJwtException e) {
            log.warn("格式錯誤的 JWS Token: {}", e.getMessage());
            throw new RuntimeException("Malformed JWS token", e);
        } catch (SignatureException e) {
            log.warn("JWS Token 簽名驗證失敗: {}", e.getMessage());
            throw new RuntimeException("JWS signature validation failed", e);
        } catch (IllegalArgumentException e) {
            log.warn("JWS Token 為空或無效: {}", e.getMessage());
            throw new RuntimeException("JWS token is null or invalid", e);
        }
    }

    /**
     * 解析 JWS Token（使用預設密鑰）
     * 
     * @param token JWS Token
     * @return Claims 對象
     */
    public static Claims parseJws(String token) {
        return parseJws(token, DEFAULT_SIGNING_KEY);
    }

    /**
     * 驗證 Token 是否有效
     * 
     * @param token JWT/JWS Token
     * @return true 如果有效，false 如果無效
     */
    public static boolean isTokenValid(String token) {
        try {
            parseJwt(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 驗證 Token 是否有效（使用指定密鑰）
     * 
     * @param token      JWT/JWS Token
     * @param signingKey 驗證用的簽名密鑰
     * @return true 如果有效，false 如果無效
     */
    public static boolean isTokenValid(String token, SecretKey signingKey) {
        try {
            parseJws(token, signingKey);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 從 Token 中提取主題
     * 
     * @param token JWT/JWS Token
     * @return 主題字符串
     */
    public static String getSubject(String token) {
        return parseJwt(token).getSubject();
    }

    /**
     * 從 Token 中提取過期時間
     * 
     * @param token JWT/JWS Token
     * @return 過期時間
     */
    public static Date getExpiration(String token) {
        return parseJwt(token).getExpiration();
    }

    /**
     * 檢查 Token 是否即將過期（1小時內）
     * 
     * @param token JWT/JWS Token
     * @return true 如果即將過期
     */
    public static boolean isTokenExpiringSoon(String token) {
        Date expiration = getExpiration(token);
        Date oneHourFromNow = Date.from(Instant.now().plus(1, ChronoUnit.HOURS));
        return expiration.before(oneHourFromNow);
    }

    /**
     * 生成安全的簽名密鑰
     * 
     * @return SecretKey 對象
     */
    public static SecretKey generateSecureKey() {
        return Jwts.SIG.HS256.key().build();
    }

    /**
     * 從字符串創建簽名密鑰
     * 
     * @param secret 密鑰字符串
     * @return SecretKey 對象
     */
    public static SecretKey createKeyFromString(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

}
