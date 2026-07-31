package com.moajjem.myduuo.util

import android.util.Base64
import java.nio.charset.StandardCharsets

data class ParsedPairingCode(
    val botToken: String,
    val groupId: String,
    val creatorName: String = "",
    val targetPartnerName: String = "",
    val creatorGender: String = ""
)

object PairingCodeManager {

    private const val PREFIX = "MYDUO_"
    private const val DELIMITER = "|||"

    /**
     * Generates a Base64 encoded pairing code containing BotToken, GroupId, Creator Name, Target Partner Name, and Creator Gender.
     */
    fun generatePairingCode(
        botToken: String,
        groupId: String,
        creatorName: String = "",
        targetPartnerName: String = "",
        creatorGender: String = ""
    ): String {
        if (botToken.isBlank() || groupId.isBlank()) return ""
        val raw = "${botToken.trim()}$DELIMITER${groupId.trim()}$DELIMITER${creatorName.trim()}$DELIMITER${targetPartnerName.trim()}$DELIMITER${creatorGender.trim()}"
        val encoded = Base64.encodeToString(raw.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        return "$PREFIX$encoded"
    }

    /**
     * Decodes a Base64 pairing code into ParsedPairingCode.
     * Returns null if the code is invalid or malformed.
     */
    fun decodePairingCode(input: String): ParsedPairingCode? {
        if (input.isBlank()) return null
        try {
            val trimmedInput = input.trim()

            // 1. Try to extract MYDUO_<base64> from string via regex (handles full shared messages)
            val myduoRegex = Regex("""MYDUO_([A-Za-z0-9+/=]+)""", RegexOption.IGNORE_CASE)
            val myduoMatch = myduoRegex.find(trimmedInput)

            val base64Payload = if (myduoMatch != null) {
                myduoMatch.groupValues[1]
            } else if (trimmedInput.startsWith(PREFIX, ignoreCase = true)) {
                trimmedInput.substring(PREFIX.length).trim()
            } else {
                // If pure base64 code pasted without prefix
                trimmedInput.replace(Regex("""[^A-Za-z0-9+/=]"""), "")
            }

            if (base64Payload.isBlank()) return null

            // 2. Decode Base64 payload
            val decodedBytes = Base64.decode(base64Payload, Base64.DEFAULT)
            val decodedStr = String(decodedBytes, StandardCharsets.UTF_8).trim()

            // 3. Check for primary delimiter "|||"
            if (decodedStr.contains(DELIMITER)) {
                val parts = decodedStr.split(DELIMITER)
                if (parts.size >= 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    val botToken = parts[0].trim()
                    val groupId = parts[1].trim()

                    var creatorName = ""
                    var targetPartnerName = ""
                    var creatorGender = ""

                    if (parts.size >= 5) {
                        creatorName = parts[2].trim()
                        targetPartnerName = parts[3].trim()
                        creatorGender = parts[4].trim()
                    } else if (parts.size == 4) {
                        creatorName = parts[2].trim()
                        creatorGender = parts[3].trim()
                    } else if (parts.size == 3) {
                        creatorName = parts[2].trim()
                    }

                    return ParsedPairingCode(botToken, groupId, creatorName, targetPartnerName, creatorGender)
                }
            }

            // 4. Fallback for legacy format with ':' (split at last colon since bot tokens contain a colon)
            val lastColonIndex = decodedStr.lastIndexOf(':')
            if (lastColonIndex > 0 && lastColonIndex < decodedStr.length - 1) {
                val botToken = decodedStr.substring(0, lastColonIndex).trim()
                val groupId = decodedStr.substring(lastColonIndex + 1).trim()
                if (botToken.isNotBlank() && groupId.isNotBlank()) {
                    return ParsedPairingCode(botToken, groupId)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
