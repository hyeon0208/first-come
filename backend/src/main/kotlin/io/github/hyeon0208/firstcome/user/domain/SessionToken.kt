package io.github.hyeon0208.firstcome.user.domain

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.HexFormat

// 세션 쿠키에 담는 값. 추측할 수 없는 무작위 값이고, 서버에는 해시만 저장한다.
object SessionToken {
    private val random = SecureRandom()

    fun generate(): String =
        HexFormat.of().formatHex(ByteArray(32).also(random::nextBytes))

    fun hash(token: String): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.toByteArray()))
}
