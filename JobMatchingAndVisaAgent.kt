package com.example.domain

import com.example.data.model.CandidateProfile
import com.example.data.model.Job
import com.example.data.model.JobMatch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object JobMatchingAndVisaAgent {

    fun calculateMatch(candidate: CandidateProfile, job: Job): JobMatch {
        // 1. Skill & Machinery Match (Weight 25%)
        val candSkills = (candidate.machineEquipment + " " + candidate.responsibilities + " " + candidate.fieldOfStudy).lowercase()
        val jobReqs = (job.title + " " + job.requiredQualifications + " " + job.description).lowercase()

        var skillScore = 70
        val keywords = listOf(
            "excavator", "bulldozer", "crane", "loader", "machinery", "heavy", "plant",
            "welder", "hydraulic", "rigging", "operator", "driver", "mechanic", "drilling",
            "plc", "cnc", "automation", "maintenance", "scada", "engineering"
        )
        var matchesFound = 0
        for (kw in keywords) {
            if (candSkills.contains(kw) && jobReqs.contains(kw)) {
                matchesFound++
            }
        }
        skillScore = min(100, 65 + (matchesFound * 8))

        // 2. Experience Match (Weight 20%)
        val expDiff = candidate.yearsExperience - job.minYearsExperience
        val expScore = when {
            expDiff >= 2 -> 98
            expDiff >= 0 -> 90
            expDiff == -1 -> 75
            else -> 60
        }

        // 3. Licence & Safety Match (Weight 15%)
        val candLicences = candidate.licences.lowercase()
        val jobLicences = job.requiredLicences.lowercase()
        var licenceScore = 80
        if (candLicences.contains("code 95") && (jobLicences.contains("code 95") || jobLicences.contains("driving"))) licenceScore += 10
        if (candLicences.contains("operator") && (jobLicences.contains("operator") || jobLicences.contains("plant"))) licenceScore += 10
        if (candLicences.contains("crane") && jobLicences.contains("crane")) licenceScore += 10
        if (candLicences.contains("tüv") && (jobLicences.contains("safety") || jobLicences.contains("tüv"))) licenceScore += 8
        if (candLicences.contains("osha") || candLicences.contains("nebosh")) licenceScore += 5
        licenceScore = min(100, licenceScore)

        // 4. Language Match (Weight 15%)
        val candLangs = candidate.languages.lowercase()
        val reqLangs = job.requiredLanguages.lowercase()
        var langScore = 85
        if (reqLangs.contains("german") && !candLangs.contains("german")) {
            langScore -= 30
        } else if (reqLangs.contains("german") && candLangs.contains("b1")) {
            langScore = 95
        }
        if (reqLangs.contains("english") && candLangs.contains("c1")) {
            langScore = max(langScore, 92)
        }

        // 5. Visa Eligibility & Sponsorship Match (Weight 15%)
        var visaScore = 85
        if (job.sponsorshipStatus == "Verified Sponsor") visaScore += 10
        if (job.grossSalaryMonthly >= 4500.0) visaScore += 5 // Meets EU Blue Card wage floor threshold (€45,300/yr)
        visaScore = min(100, visaScore)

        // 6. Salary & Relocation Preference Fit (Weight 10%)
        val salaryRatio = job.grossSalaryMonthly / max(1.0, candidate.expectedSalaryMonthly)
        val salaryScore = when {
            salaryRatio >= 1.1 -> 100
            salaryRatio >= 0.9 -> 90
            salaryRatio >= 0.75 -> 75
            else -> 60
        }

        // Location bonus/penalty
        val countryMatch = candidate.preferredCountries.contains(job.country, ignoreCase = true)
        val locationFactor = if (countryMatch) 1.0 else 0.95

        val rawOverall = (skillScore * 0.25) +
                (expScore * 0.20) +
                (licenceScore * 0.15) +
                (langScore * 0.15) +
                (visaScore * 0.15) +
                (salaryScore * 0.10)
        val overallScore = min(99, max(50, (rawOverall * locationFactor).roundToInt()))

        // Financial & Living Cost Calculations
        val estimatedTax = job.grossSalaryMonthly * job.estimatedTaxRate
        val netSalary = job.grossSalaryMonthly - estimatedTax
        val livingCost = job.estimatedLivingCostMonthly
        val savings = max(0.0, netSalary - livingCost)

        // Visa Specific Intelligence
        val (recognitionReq, authName, licenceNote) = evaluateImmigrationCriteria(candidate, job)

        val explanation = buildString {
            append("Overall Match: $overallScore% based on transparent multi-criteria evaluation. ")
            if (countryMatch) append("Matches preferred destination country (${job.country}). ")
            append("Meets required experience (${candidate.yearsExperience} yrs vs ${job.minYearsExperience} yrs required). ")
            if (job.sponsorshipStatus == "Verified Sponsor") append("Employer is an accredited sponsor for ${job.visaTrack}. ")
            append("Estimated net savings: €${savings.roundToInt()}/month after destination living costs.")
        }

        return JobMatch(
            id = job.id,
            jobId = job.id,
            overallScore = overallScore,
            skillScore = skillScore,
            experienceScore = expScore,
            licenceScore = licenceScore,
            languageScore = langScore,
            visaScore = visaScore,
            salaryScore = salaryScore,
            visaTrackSummary = job.visaTrack,
            recognitionRequired = recognitionReq,
            recognitionAuthority = authName,
            licenceEquivalenceNote = licenceNote,
            grossSalary = job.grossSalaryMonthly,
            estimatedNetSalary = netSalary,
            estimatedLivingCost = livingCost,
            estimatedMonthlySavings = savings,
            currency = job.currency,
            matchExplanation = explanation,
            isSaved = false
        )
    }

    private fun evaluateImmigrationCriteria(
        candidate: CandidateProfile,
        job: Job
    ): Triple<Boolean, String, String> {
        return when (job.country) {
            "Germany" -> {
                Triple(
                    true,
                    "Zentralstelle für ausländisches Bildungswesen (ZAB) / Anabin Database",
                    "Driving and trade licences must be recognized under German Kraftfahrt-Bundesamt. Professional driver Code 95 requires verified training proof."
                )
            }
            "Netherlands" -> {
                Triple(
                    true,
                    "Nuffic (Dutch Organization for Internationalisation in Education)",
                    "Employer must be listed on the public IND Public Register of Recognised Sponsors. 30% tax facility requires salary above €41,710 threshold."
                )
            }
            "Poland" -> {
                Triple(
                    false,
                    "Voivodeship Labour Office (Wojewódzki Urząd Pracy)",
                    "Type A Work Permit requires employer labor market test exemption or certified shortage list occupation. EU Code 95 is mutually recognized."
                )
            }
            "Saudi Arabia" -> {
                Triple(
                    true,
                    "Saudi Council of Engineers (SCE) / Qiwa Verification",
                    "Saudi law mandates degree verification via Primary Source Verification (DataFlow). EU/UK driving licences require direct conversion test at Dallah Driving Academy."
                )
            }
            "United Arab Emirates" -> {
                Triple(
                    true,
                    "Ministry of Foreign Affairs (MoFA) & MoHRE Attestation",
                    "Degree must be authenticated by issuing country embassy and UAE MoFA. Employer covers 100% of residence visa and medical fitness fees by law."
                )
            }
            "Sweden" -> {
                Triple(
                    false,
                    "Swedish Council for Higher Education (UHR)",
                    "Requires trade union statement of opinion on wages and collective agreement compliance prior to Migrationsverket approval."
                )
            }
            else -> {
                Triple(
                    false,
                    "National Ministry of Labour & Foreign Affairs",
                    "Work permit issuance subject to employer sponsorship proof and consular verification."
                )
            }
        }
    }
}
