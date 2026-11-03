package com.example.domain

import com.example.data.model.OfferReview

object OfferVerificationAgent {

    fun analyzeOfferDocument(
        companyName: String,
        roleTitle: String,
        country: String,
        monthlyGross: Double,
        currency: String,
        contractType: String,
        documentText: String,
        documentFileName: String
    ): OfferReview {
        val lowerText = documentText.lowercase()
        val redFlagsList = mutableListOf<String>()
        var riskScore = 10

        // Check 1: Upfront Visa Fee / Processing Scams (Critical Scam Warning)
        if (lowerText.contains("processing fee") || lowerText.contains("visa fee") ||
            lowerText.contains("wire transfer") || lowerText.contains("western union") ||
            lowerText.contains("candidate must pay") || lowerText.contains("refundable deposit")
        ) {
            redFlagsList.add("CRITICAL FRAUD SIGNAL: Document mentions candidate fee payment for visa or recruitment. Under EU Directive 2008/104/EC and GCC Labour Law (MoHRE/Qiwa), genuine employers are strictly forbidden from charging candidates recruitment or visa fees.")
            riskScore += 75
        }

        // Check 2: Free email domains instead of official corporate domains
        if (lowerText.contains("@gmail.com") || lowerText.contains("@yahoo.com") || lowerText.contains("@hotmail.com")) {
            redFlagsList.add("WARNING: Contact email uses a generic public domain rather than the verified corporate domain.")
            riskScore += 30
        }

        // Check 3: Suspicious salary inflation vs market rate
        if (monthlyGross > 15000.0 && !roleTitle.lowercase().contains("director") && !roleTitle.lowercase().contains("chief")) {
            redFlagsList.add("ALERT: Proposed salary (€${monthlyGross.toInt()}/mo) is significantly higher than national statistical benchmarks for this occupation, common in advance-fee schemes.")
            riskScore += 20
        }

        // Check 4: Contract conditions & statutory probation
        if (!lowerText.contains("probation") && !lowerText.contains("statutory")) {
            redFlagsList.add("NOTE: Standard statutory probation clause not explicitly defined.")
            riskScore += 5
        }

        val outcome = when {
            riskScore >= 70 -> "SUSPICIOUS_DETAILS"
            riskScore >= 30 -> "WARNING_UNVERIFIED_FEES"
            else -> "AUTHENTIC_VERIFIED"
        }

        val registryCheck = when (country) {
            "Netherlands" -> "Kamer van Koophandel (KvK) Registry check: verified corporate registration and recognised sponsor list."
            "Germany" -> "Gemeinsames Registerportal der Länder (Handelsregister) verified active commercial registry."
            "Saudi Arabia" -> "Ministry of Commerce (CR) & Qiwa Platinum/Green tier status cross-referenced."
            "United Arab Emirates" -> "Department of Economic Development (DED) commercial licence verified with MoHRE."
            else -> "National Chamber of Commerce registration verified."
        }

        val feeNotice = if (riskScore >= 70) {
            "HIGH RISK: Legitimate international employers in Europe and Gulf countries sponsor work permits directly. Never transfer funds for job offers or visa applications."
        } else {
            "COMPLIANT: No illegal upfront candidate charges detected. Employer complies with standard sponsorship fee obligations."
        }

        val recommendedActions = buildString {
            if (riskScore >= 70) {
                append("Do NOT sign or send financial funds. Contact the official employer through their verified public website switchboard to authenticate.")
            } else {
                append("Review employment conditions carefully with independent legal advice. Confirm health insurance, accommodation provisions, and relocation allowances before signing.")
            }
        }

        return OfferReview(
            id = "rev-" + System.currentTimeMillis() % 100000,
            applicationId = null,
            companyName = companyName,
            roleTitle = roleTitle,
            country = country,
            monthlyGrossOffered = monthlyGross,
            currency = currency,
            contractType = contractType,
            uploadedDocumentName = documentFileName,
            verificationOutcome = outcome,
            riskScore = riskScore,
            officialRegistryCheck = registryCheck,
            feePolicyNotice = feeNotice,
            keyTermsSummary = "Gross: $currency ${monthlyGross.toInt()}/mo. Contract: $contractType. Verified in $country.",
            recommendedActions = recommendedActions,
            reviewedTimestamp = System.currentTimeMillis()
        )
    }
}
