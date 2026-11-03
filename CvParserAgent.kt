package com.example.domain

import com.example.data.model.CandidateProfile

data class ExtractedCvData(
    val fullName: String,
    val email: String,
    val phone: String,
    val citizenship: String,
    val currentResidenceCountry: String,
    val educationLevel: String,
    val fieldOfStudy: String,
    val yearsExperience: Int,
    val currentJobTitle: String,
    val responsibilities: String,
    val primaryIndustry: String,
    val machineEquipment: String,
    val licences: String,
    val licenceIssuingCountry: String,
    val languages: String,
    val expectedSalaryMonthly: Double,
    val rawSummary: String
)

object CvParserAgent {

    val sampleCvs = listOf(
        SampleCv(
            label = "Heavy Machinery & Automation Specialist",
            role = "Senior Automation & Heavy Equipment Specialist",
            preview = "6 years exp • CAT 320, Haas CNC, Siemens PLC • EU Code 95, TÜV Welder • English C1, German B1",
            data = ExtractedCvData(
                fullName = "Alex Mercer",
                email = "alex.mercer@jobnexa.ai",
                phone = "+44 7700 900123",
                citizenship = "India",
                currentResidenceCountry = "United Kingdom",
                educationLevel = "Bachelor of Science",
                fieldOfStudy = "Mechanical & Automation Engineering",
                yearsExperience = 6,
                currentJobTitle = "Senior Automation & Heavy Equipment Specialist",
                responsibilities = "Supervising 5-axis CNC machining, Siemens PLC programming, and operating hydraulic excavators. Ensuring ISO 9606 safety compliance and site maintenance.",
                primaryIndustry = "Heavy Engineering & Manufacturing",
                machineEquipment = "CAT 320 Excavator, Siemens S7-1500 PLC, Haas 5-Axis CNC, Komatsu WA380",
                licences = "EU Code 95 Professional Driver, TÜV Welder ISO 9606-1, OSHA 30 Industrial Safety",
                licenceIssuingCountry = "Germany & United Kingdom",
                languages = "English (C1 - Fluent), German (B1 - Intermediate), Arabic (A2 - Elementary)",
                expectedSalaryMonthly = 4800.0,
                rawSummary = "Extracted from verified technical portfolio: 6 years experience across EU/UK industrial sites, accredited safety credentials, high CNC precision."
            )
        ),
        SampleCv(
            label = "Precision Mechatronics & Robotics Lead",
            role = "Industrial Robotics & Mechatronics Technician",
            preview = "5 years exp • KUKA Robots, Siemens TIA, Fanuc • Cleanroom Class 4, CE Safety • English C1",
            data = ExtractedCvData(
                fullName = "Elena Rostova",
                email = "elena.rostova@jobnexa.ai",
                phone = "+48 22 555 0192",
                citizenship = "Ukraine",
                currentResidenceCountry = "Poland",
                educationLevel = "Master of Science",
                fieldOfStudy = "Mechatronics & Embedded Robotics",
                yearsExperience = 5,
                currentJobTitle = "Industrial Robotics & Mechatronics Technician",
                responsibilities = "Programming and calibrating KUKA 6-axis industrial robots, PLC motion control loops, and optical quality verification systems in high-tech cleanroom environments.",
                primaryIndustry = "Semiconductor & Advanced Robotics",
                machineEquipment = "KUKA KR CYBERTECH, Siemens TIA Portal, Fanuc Robodrill, Cognex Vision",
                licences = "CE Functional Safety Engineer, ISO Cleanroom Class 4 Operator, IPC-A-610 Soldering",
                licenceIssuingCountry = "Poland & Germany",
                languages = "English (C1 - Fluent), Ukrainian (Native), Polish (C1 - Fluent), German (A2)",
                expectedSalaryMonthly = 5200.0,
                rawSummary = "Extracted from robotics engineering CV: European cleanroom certifications, robot kinematics calibration, strong English/Polish proficiency."
            )
        ),
        SampleCv(
            label = "Civil Infrastructure & Earthmoving Supervisor",
            role = "Earthmoving Fleet & Heavy Plant Superintendent",
            preview = "8 years exp • Liebherr, Caterpillar 349, Komatsu D375 • NEBOSH, CPCS Black Card • English B2",
            data = ExtractedCvData(
                fullName = "Tariq Mansoor",
                email = "tariq.mansoor@jobnexa.ai",
                phone = "+971 50 123 4567",
                citizenship = "Pakistan",
                currentResidenceCountry = "United Arab Emirates",
                educationLevel = "Diploma in Civil Engineering",
                fieldOfStudy = "Civil Infrastructure & Heavy Earthworks",
                yearsExperience = 8,
                currentJobTitle = "Earthmoving Fleet & Heavy Plant Superintendent",
                responsibilities = "Directing mass earthworks, fleet mobilization of 40+ excavators and articulated dump trucks, geotechnical compliance, and HSE zero-harm enforcement.",
                primaryIndustry = "Civil Mega-Infrastructure & Mining",
                machineEquipment = "Liebherr R 9800 Excavator, CAT 349, Komatsu D375A Dozer, Volvo A40G Hauler",
                licences = "NEBOSH IGC Safety Certification, CPCS Plant Manager, UAE Heavy Vehicle Licence #4",
                licenceIssuingCountry = "United Arab Emirates & UK",
                languages = "English (B2 - Professional), Urdu (Native), Arabic (B1 - Conversational)",
                expectedSalaryMonthly = 6000.0,
                rawSummary = "Extracted from mega-project supervisor dossier: 8 years leading earthmoving machinery in GCC extreme climates, NEBOSH verified."
            )
        )
    )

    fun parseCvText(text: String, existingProfile: CandidateProfile): ExtractedCvData {
        val lower = text.lowercase()
        val years = Regex("(\\d+)\\+?\\s*years?").find(lower)?.groupValues?.get(1)?.toIntOrNull() ?: existingProfile.yearsExperience

        val foundMachines = mutableListOf<String>()
        if (lower.contains("cnc")) foundMachines.add("5-Axis CNC")
        if (lower.contains("siemens") || lower.contains("plc")) foundMachines.add("Siemens S7 PLC")
        if (lower.contains("cat") || lower.contains("excavator")) foundMachines.add("CAT Excavator")
        if (lower.contains("kuka") || lower.contains("robot")) foundMachines.add("KUKA Robotics")
        if (foundMachines.isEmpty()) foundMachines.add(existingProfile.machineEquipment)

        val foundLicences = mutableListOf<String>()
        if (lower.contains("code 95")) foundLicences.add("EU Code 95")
        if (lower.contains("tüv") || lower.contains("welder")) foundLicences.add("TÜV Welder ISO 9606")
        if (lower.contains("osha") || lower.contains("nebosh")) foundLicences.add("OSHA/NEBOSH Safety")
        if (foundLicences.isEmpty()) foundLicences.add(existingProfile.licences)

        val foundLangs = mutableListOf<String>()
        if (lower.contains("english")) foundLangs.add("English (C1)")
        if (lower.contains("german") || lower.contains("deutsch")) foundLangs.add("German (B1)")
        if (lower.contains("arabic")) foundLangs.add("Arabic (A2)")
        if (foundLangs.isEmpty()) foundLangs.add(existingProfile.languages)

        return ExtractedCvData(
            fullName = existingProfile.fullName,
            email = existingProfile.email,
            phone = existingProfile.phone,
            citizenship = existingProfile.citizenship,
            currentResidenceCountry = existingProfile.currentResidenceCountry,
            educationLevel = existingProfile.educationLevel,
            fieldOfStudy = existingProfile.fieldOfStudy,
            yearsExperience = years,
            currentJobTitle = existingProfile.currentJobTitle,
            responsibilities = if (text.isNotBlank()) text.take(300) else existingProfile.responsibilities,
            primaryIndustry = existingProfile.primaryIndustry,
            machineEquipment = foundMachines.joinToString(", "),
            licences = foundLicences.joinToString(", "),
            licenceIssuingCountry = existingProfile.licenceIssuingCountry,
            languages = foundLangs.joinToString(", "),
            expectedSalaryMonthly = existingProfile.expectedSalaryMonthly,
            rawSummary = "Parsed from candidate CV text upload. Factual items extracted and ready for user review."
        )
    }
}

data class SampleCv(
    val label: String,
    val role: String,
    val preview: String,
    val data: ExtractedCvData
)
