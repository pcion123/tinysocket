package com.vscodelife.demo.webserver.util;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.socketio.util.JwtUtil;

/**
 * Token 工具類
 * 負責生成和驗證各種類型的 JWT Token
 */
public final class TokenUtil {
    private static final Logger logger = LoggerFactory.getLogger(TokenUtil.class);

    // JWT 簽名密鑰
    private static final String JWT_SECRET_KEY = "mySecretKeyForJWTTokenGenerationAndValidation12345";

    // Token 類型常數
    public static final String TOKEN_TYPE_AUTH = "auth";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    // 預設過期時間（分鐘）
    public static final int DEFAULT_AUTH_TOKEN_EXPIRATION_MINUTES = 30;
    public static final int DEFAULT_REFRESH_TOKEN_EXPIRATION_MINUTES = 60 * 24 * 7; // 7天

    // 私有建構函數，防止實例化
    private TokenUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 生成認證 token（預設15分鐘有效期）
     * 
     * @param userId 用戶ID
     * @return JWT token 字符串
     * @throws RuntimeException 當 token 生成失敗時
     */
    public static String generateAuthToken(String userId) {
        return generateAuthToken(userId, DEFAULT_AUTH_TOKEN_EXPIRATION_MINUTES);
    }

    /**
     * 生成認證 token（指定有效期）
     * 
     * @param userId            用戶ID
     * @param expirationMinutes 過期時間（分鐘）
     * @return JWT token 字符串
     * @throws RuntimeException 當 token 生成失敗時
     */
    public static String generateAuthToken(String userId, int expirationMinutes) {
        try {
            // 創建自定義聲明
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", userId);
            claims.put("authTime", Instant.now().getEpochSecond());
            claims.put("tokenType", TOKEN_TYPE_AUTH);

            return generateJwsWithMinutes(userId, claims, expirationMinutes);
        } catch (Exception e) {
            logger.error("create auth token failed for userId: {}", userId, e);
            throw new RuntimeException("create auth token failed", e);
        }
    }

    /**
     * 生成刷新 token（預設7天有效期）
     * 
     * @param userId 用戶ID
     * @return JWT token 字符串
     * @throws RuntimeException 當 token 生成失敗時
     */
    public static String generateRefreshToken(String userId) {
        return generateRefreshToken(userId, DEFAULT_REFRESH_TOKEN_EXPIRATION_MINUTES);
    }

    /**
     * 生成刷新 token（指定有效期）
     * 
     * @param userId            用戶ID
     * @param expirationMinutes 過期時間（分鐘）
     * @return JWT token 字符串
     * @throws RuntimeException 當 token 生成失敗時
     */
    public static String generateRefreshToken(String userId, int expirationMinutes) {
        try {
            // 創建自定義聲明
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", userId);
            claims.put("createTime", Instant.now().getEpochSecond());
            claims.put("tokenType", TOKEN_TYPE_REFRESH);

            return generateJwsWithMinutes(userId, claims, expirationMinutes);
        } catch (Exception e) {
            logger.error("create refresh token failed for userId: {}", userId, e);
            throw new RuntimeException("create refresh token failed", e);
        }
    }

    /**
     * 生成指定分鐘數過期的 JWS Token
     * 
     * @param subject           主題（通常是用戶ID）
     * @param claims            自定義聲明
     * @param expirationMinutes 過期時間（分鐘）
     * @return JWT token 字符串
     * @throws RuntimeException 當 token 生成失敗時
     */
    public static String generateJwsWithMinutes(String subject, Map<String, Object> claims, int expirationMinutes) {
        try {
            Instant now = Instant.now();
            Instant expiration = now.plus(expirationMinutes, java.time.temporal.ChronoUnit.MINUTES);

            io.jsonwebtoken.JwtBuilder builder = io.jsonwebtoken.Jwts.builder()
                    .subject(subject)
                    .issuedAt(java.util.Date.from(now))
                    .expiration(java.util.Date.from(expiration))
                    .signWith(JwtUtil.createKeyFromString(JWT_SECRET_KEY));

            if (claims != null && !claims.isEmpty()) {
                builder.claims(claims);
                builder.subject(subject); // 重新設置 subject，因為 claims 會覆蓋
            }

            String token = builder.compact();
            logger.debug("Generated JWS token for subject: {}, expiration: {} minutes", subject, expirationMinutes);
            return token;

        } catch (Exception e) {
            logger.error("create custom expiration JWS failed for subject: {}", subject, e);
            throw new RuntimeException("create custom expiration JWS failed", e);
        }
    }

    /**
     * 從 token 中獲取所有聲明 (Claims)
     * 
     * @param token JWT token 字符串
     * @return Claims 對象，包含所有聲明信息，如果解析失敗則返回 null
     */
    public static io.jsonwebtoken.Claims getClaims(String token) {
        try {
            var signingKey = JwtUtil.createKeyFromString(JWT_SECRET_KEY);
            return JwtUtil.parseJws(token, signingKey);
        } catch (Exception e) {
            logger.warn("Get claims from token failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 從 token 中提取過期時間
     * 
     * @param token JWT token 字符串
     * @return 過期時間，如果提取失敗則返回 null
     */
    public static java.util.Date getExpiration(String token) {
        try {
            var claims = getClaims(token);
            return claims != null ? claims.getExpiration() : null;
        } catch (Exception e) {
            logger.warn("Extract expiration from token failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 從 token 中提取簽發時間
     * 
     * @param token JWT token 字符串
     * @return 簽發時間，如果提取失敗則返回 null
     */
    public static java.util.Date getIssuedAt(String token) {
        try {
            var claims = getClaims(token);
            return claims != null ? claims.getIssuedAt() : null;
        } catch (Exception e) {
            logger.warn("Extract issued at from token failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 檢查 token 是否已過期
     * 
     * @param token JWT token 字符串
     * @return true 如果已過期，false 如果未過期或檢查失敗
     */
    public static boolean isTokenExpired(String token) {
        try {
            var expiration = getExpiration(token);
            if (expiration == null) {
                return true; // 無法獲取過期時間，視為已過期
            }
            return expiration.before(new java.util.Date());
        } catch (Exception e) {
            logger.warn("Check token expiration failed: {}", e.getMessage());
            return true; // 檢查失敗，視為已過期
        }
    }

    /**
     * 檢查 token 是否即將過期（指定分鐘數內）
     * 
     * @param token         JWT token 字符串
     * @param beforeMinutes 提前多少分鐘視為即將過期
     * @return true 如果即將過期，false 否則
     */
    public static boolean isTokenExpiringSoon(String token, int beforeMinutes) {
        try {
            var expiration = getExpiration(token);
            if (expiration == null) {
                return true;
            }
            var checkTime = Instant.now().plus(beforeMinutes, java.time.temporal.ChronoUnit.MINUTES);
            return expiration.before(java.util.Date.from(checkTime));
        } catch (Exception e) {
            logger.warn("Check token expiring soon failed: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 驗證 token 是否有效
     * 
     * @param token JWT token 字符串
     * @return true 如果 token 有效，false 否則
     */
    public static boolean validateToken(String token) {
        try {
            // 使用自定義密鑰解析 JWS token
            var signingKey = JwtUtil.createKeyFromString(JWT_SECRET_KEY);
            JwtUtil.parseJws(token, signingKey);
            return true;
        } catch (Exception e) {
            logger.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 從 token 中提取用戶ID
     * 
     * @param token JWT token 字符串
     * @return 用戶ID，如果提取失敗則返回 null
     */
    public static String extractUserId(String token) {
        try {
            var signingKey = JwtUtil.createKeyFromString(JWT_SECRET_KEY);
            var claims = JwtUtil.parseJws(token, signingKey);
            return claims.getSubject();
        } catch (Exception e) {
            logger.warn("Extract userId from token failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 從 token 中提取 token 類型
     * 
     * @param token JWT token 字符串
     * @return token 類型，如果提取失敗則返回 null
     */
    public static String extractTokenType(String token) {
        try {
            var signingKey = JwtUtil.createKeyFromString(JWT_SECRET_KEY);
            var claims = JwtUtil.parseJws(token, signingKey);
            return (String) claims.get("tokenType");
        } catch (Exception e) {
            logger.warn("Extract token type from token failed: {}", e.getMessage());
            return null;
        }
    }
}
