package com.rivavafi.universal.sms

import java.util.regex.Pattern

object SmsTransactionParser {

    private val CREDIT_WORDS = listOf("credited", "credit", "deposited", "received", "refund", "cashback", "salary")
    private val DEBIT_WORDS = listOf("debited", "debit", "spent", "paid", "withdrawn", "purchase", "sent")

    // Amount extraction: e.g. Rs. 500, INR 500.00
    private val amountRegex = Pattern.compile("(?i)(?:INR|Rs\\.?|₹)\\s*([0-9,]+(?:\\.\\d{1,2})?)")

    // Balance extraction
    private val balanceRegex = Pattern.compile("(?i)(?:available balance|avl bal|bal|balance).*?(?:INR|Rs\\.?|₹)\\s*([0-9,]+(?:\\.\\d{1,2})?)")

    // Reference extraction
    private val refRegex = Pattern.compile("(?i)(?:ref|reference|utr|txn|transaction id)[:\\s.-]*([A-Z0-9]+)")


    private val PENDING_WORDS = listOf("pending", "processing")
    private val FAILED_WORDS = listOf("failed", "declined", "reversed", "cancelled", "unsuccessful", "not successful")
    private val MARKETING_WORDS = listOf("loan", "offer", "discount", "apply now", "win", "reward", "hurry", "claim")

    private val upiRegex = Pattern.compile("(?i)(?:vpa|upi)[^a-z0-9]*([a-zA-Z0-9.-]+@[a-zA-Z]+)")
    private val acctRegex = Pattern.compile("(?i)(?:a/c|acct|account)[^0-9]*([0-9]{4})(?![0-9])")

    fun parseMessage(
        sender: String,
        body: String,
        timestamp: Long,
        platformSmsId: String?,
        mode: SmsTrackingMode
    ): ParsedSmsTransaction? {
        if (mode == SmsTrackingMode.OFF) return null

        val lowerBody = body.lowercase()

        // Skip OTPs and promotional messages
        if (lowerBody.contains("otp") || lowerBody.contains("verification code")) return null

        // Skip failed, pending or marketing messages
        if (FAILED_WORDS.any { lowerBody.contains(it) }) return null
        if (PENDING_WORDS.any { lowerBody.contains(it) }) return null
        if (MARKETING_WORDS.any { lowerBody.contains(it) } && !CREDIT_WORDS.any { lowerBody.contains(it) } && !DEBIT_WORDS.any { lowerBody.contains(it) }) return null

        val isCredit = CREDIT_WORDS.any { lowerBody.contains(it) }
        val isDebit = DEBIT_WORDS.any { lowerBody.contains(it) }

        // Discard ambiguous messages if strictly ambiguous
        if (isCredit && isDebit) {
            if (lowerBody.contains("credited") && !lowerBody.contains("debited")) {
                // assume credit
            } else if (lowerBody.contains("debited") && !lowerBody.contains("credited")) {
                // assume debit
            } else if (lowerBody.contains("refund") || lowerBody.contains("reversal")) {
                // assume credit for refund
            } else {
                return null
            }
        }
        if (!isCredit && !isDebit) return null

        val type = if (lowerBody.contains("credited")) "CREDIT" else if (lowerBody.contains("debited")) "DEBIT" else if (isCredit) "CREDIT" else "DEBIT"

        // Apply tracking mode filter
        if (mode == SmsTrackingMode.CREDIT_ONLY && type == "DEBIT") return null
        if (mode == SmsTrackingMode.DEBIT_ONLY && type == "CREDIT") return null

        // Extract amount
        var balance: Double? = null
        val balMatcher = balanceRegex.matcher(body)
        if (balMatcher.find()) {
            balance = balMatcher.group(1)?.replace(",", "")?.toDoubleOrNull()
        }

        var amountStr: String? = null
        val matcher = amountRegex.matcher(body)
        val amounts = mutableListOf<Double>()
        while (matcher.find()) {
            matcher.group(1)?.replace(",", "")?.toDoubleOrNull()?.let { amounts.add(it) }
        }

        val candidates = amounts.filter { it != balance }
        if (candidates.isEmpty()) {
            if (amounts.isNotEmpty() && amounts.first() > 0) {
                amountStr = amounts.first().toString()
            } else {
                return null
            }
        } else {
            amountStr = candidates.first().toString()
        }

        if (amountStr == null) return null
        val amount = amountStr.toDoubleOrNull() ?: return null
        if (amount <= 0) return null

        // Extract reference
        var refId: String? = null
        val refMatcher = refRegex.matcher(body)
        if (refMatcher.find()) {
            refId = refMatcher.group(1)
        }

        var upiId: String? = null
        val upiMatcher = upiRegex.matcher(body)
        if (upiMatcher.find()) {
            upiId = upiMatcher.group(1)
        }

        var acctLast4: String? = null
        val acctMatcher = acctRegex.matcher(body)
        if (acctMatcher.find()) {
            acctLast4 = acctMatcher.group(1)
        }

        val bankName = extractBankName(sender)
        val merchantName = extractMerchantName(body) ?: "Unknown"
        val (category, subcategory) = deduceCategory(merchantName, body, type)

        return ParsedSmsTransaction(
            amount = amount,
            type = type,
            merchantName = merchantName,
            bankName = bankName,
            category = category,
            subcategory = subcategory,
            date = timestamp,
            smsId = platformSmsId,
            rawMessage = body,
            availableBalance = balance,
            referenceId = refId,
            upiId = upiId,
            accountNumberLast4 = acctLast4,
            transactionId = refId,
            smsSender = sender
        )
    }


    private fun deduceCategory(merchant: String, body: String, type: String): Pair<String, String?> {
        val lowerMerchant = merchant.lowercase()
        val lowerBody = body.lowercase()

        val subcategory = when {
            lowerMerchant.contains("swiggy") || lowerMerchant.contains("zomato") || lowerMerchant.contains("mcdonalds") || lowerMerchant.contains("kfc") -> "Food"
            lowerMerchant.contains("uber") || lowerMerchant.contains("ola") || lowerMerchant.contains("rapido") || lowerMerchant.contains("irctc") || lowerMerchant.contains("makemytrip") -> "Transport"
            lowerMerchant.contains("amazon") || lowerMerchant.contains("flipkart") || lowerMerchant.contains("myntra") || lowerMerchant.contains("ajio") -> "Shopping"
            lowerMerchant.contains("jio") || lowerMerchant.contains("airtel") || lowerMerchant.contains("vi") || lowerMerchant.contains("recharge") -> "Recharge"
            lowerMerchant.contains("netflix") || lowerMerchant.contains("prime") || lowerMerchant.contains("spotify") || lowerMerchant.contains("hotstar") || lowerMerchant.contains("bookmyshow") -> "Entertainment"
            lowerMerchant.contains("apollo") || lowerMerchant.contains("pharmeasy") || lowerMerchant.contains("netmeds") || lowerMerchant.contains("hospital") -> "Health"
            lowerMerchant.contains("dmart") || lowerMerchant.contains("blinkit") || lowerMerchant.contains("zepto") || lowerMerchant.contains("instamart") || lowerMerchant.contains("bigbasket") -> "Groceries"
            lowerMerchant.contains("bescom") || lowerMerchant.contains("electricity") || lowerMerchant.contains("water") || lowerMerchant.contains("gas") -> "Utilities"
            lowerBody.contains("emi") || lowerBody.contains("loan") -> "Loan Payment"
            else -> null
        }

        val defaultCat = if (type == "CREDIT") "Credit" else "Debit"
        return Pair(defaultCat, subcategory)
    }


    private fun extractBankName(sender: String): String? {
        val s = sender.uppercase()
        if (s.contains("HDFC")) return "HDFC Bank"
        if (s.contains("SBI") || s.contains("SBIPSG")) return "SBI"
        if (s.contains("ICICI")) return "ICICI Bank"
        if (s.contains("AXIS")) return "Axis Bank"
        if (s.contains("KOTAK")) return "Kotak Bank"
        if (s.contains("PNB")) return "PNB"
        if (s.contains("BOB") || s.contains("BARB")) return "Bank of Baroda"
        if (s.contains("CANARA")) return "Canara Bank"
        if (s.contains("IDFC")) return "IDFC FIRST Bank"
        if (s.contains("INDUS")) return "IndusInd Bank"
        if (s.contains("FEDERAL")) return "Federal Bank"
        if (s.contains("YES")) return "Yes Bank"
        if (s.contains("AU")) return "AU Small Finance Bank"
        if (s.contains("BHIM") || s.contains("UPI")) return "BHIM UPI"
        if (s.contains("PAYTM")) return "Paytm"
        if (s.contains("PHONEPE") || s.contains("YBL")) return "PhonePe"
        if (s.contains("GPAY") || s.contains("GOOGLE")) return "Google Pay"
        if (s.contains("AMAZON")) return "Amazon Pay"
        if (s.contains("CRED")) return "CRED"
        if (s.contains("WHATSAPP")) return "WhatsApp Pay"
        return sender.takeIf { it.isNotBlank() }
    }


    private fun extractMerchantName(body: String): String? {
        // Improved extraction logic to find merchant names or VPA IDs
        val patterns = listOf(
            Pattern.compile("(?i)(?:vpa|upi)[^a-z0-9]*([a-zA-Z0-9.-]+@[a-zA-Z]+)"),
            Pattern.compile("(?i)(?:info|to|at|by|from)\\s+([A-Za-z0-9@.-]+(?:\\s+[A-Za-z0-9@.-]+){0,2})"),
            Pattern.compile("(?i)merchant[:\\s]*([A-Za-z0-9\\s]+)")
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val found = matcher.group(1)?.trim()
                if (!found.isNullOrBlank() && !found.equals("the", ignoreCase = true) && !found.equals("a", ignoreCase = true)) {
                    return found
                }
            }
        }
        return null
    }
}
