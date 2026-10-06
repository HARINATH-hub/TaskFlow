package com.example.taskflow.model

/**
 * Represents an international country with dialing code and flag for phone authentication.
 */
data class Country(
    val isoCode: String,
    val name: String,
    val dialCode: String,
    val flagEmoji: String
) {
    val searchTerms: String
        get() = "$name $dialCode $isoCode".lowercase()

    companion object {
        fun isoToEmoji(iso: String): String {
            if (iso.length != 2) return "🌐"
            val first = Character.codePointAt(iso.uppercase(), 0) - 0x41 + 0x1F1E6
            val second = Character.codePointAt(iso.uppercase(), 1) - 0x41 + 0x1F1E6
            return String(Character.toChars(first)) + String(Character.toChars(second))
        }

        private fun create(iso: String, name: String, dial: String): Country {
            return Country(
                isoCode = iso,
                name = name,
                dialCode = dial,
                flagEmoji = isoToEmoji(iso)
            )
        }

        /**
         * Comprehensive list of countries with international dialing codes.
         * India (+91) is explicitly prioritized at index 0.
         */
        val ALL: List<Country> = listOf(
            create("IN", "India", "+91"),
            create("US", "United States", "+1"),
            create("GB", "United Kingdom", "+44"),
            create("CA", "Canada", "+1"),
            create("AU", "Australia", "+61"),
            create("DE", "Germany", "+49"),
            create("FR", "France", "+33"),
            create("JP", "Japan", "+81"),
            create("AE", "United Arab Emirates", "+971"),
            create("SA", "Saudi Arabia", "+966"),
            create("SG", "Singapore", "+65"),
            create("IT", "Italy", "+39"),
            create("ES", "Spain", "+34"),
            create("BR", "Brazil", "+55"),
            create("MX", "Mexico", "+52"),
            create("ZA", "South Africa", "+27"),
            create("NG", "Nigeria", "+234"),
            create("MY", "Malaysia", "+60"),
            create("ID", "Indonesia", "+62"),
            create("PH", "Philippines", "+63"),
            create("VN", "Vietnam", "+84"),
            create("TH", "Thailand", "+66"),
            create("KR", "South Korea", "+82"),
            create("CN", "China", "+86"),
            create("PK", "Pakistan", "+92"),
            create("BD", "Bangladesh", "+880"),
            create("LK", "Sri Lanka", "+94"),
            create("NP", "Nepal", "+977"),
            create("NZ", "New Zealand", "+64"),
            create("IE", "Ireland", "+353"),
            create("NL", "Netherlands", "+31"),
            create("CH", "Switzerland", "+41"),
            create("SE", "Sweden", "+46"),
            create("NO", "Norway", "+47"),
            create("DK", "Denmark", "+45"),
            create("FI", "Finland", "+358"),
            create("PL", "Poland", "+48"),
            create("PT", "Portugal", "+351"),
            create("GR", "Greece", "+30"),
            create("TR", "Turkey", "+90"),
            create("EG", "Egypt", "+20"),
            create("KE", "Kenya", "+254"),
            create("GH", "Ghana", "+233"),
            create("AR", "Argentina", "+54"),
            create("CO", "Colombia", "+57"),
            create("CL", "Chile", "+56"),
            create("PE", "Peru", "+51"),
            create("RU", "Russia", "+7"),
            create("UA", "Ukraine", "+380"),
            create("IL", "Israel", "+972"),
            create("QA", "Qatar", "+974"),
            create("KW", "Kuwait", "+965"),
            create("OM", "Oman", "+968"),
            create("BH", "Bahrain", "+973"),
            create("HK", "Hong Kong", "+852"),
            create("TW", "Taiwan", "+886"),
            create("AT", "Austria", "+43"),
            create("BE", "Belgium", "+32"),
            create("CZ", "Czech Republic", "+420"),
            create("HU", "Hungary", "+36"),
            create("RO", "Romania", "+40"),
            create("BG", "Bulgaria", "+359"),
            create("HR", "Croatia", "+385"),
            create("SK", "Slovakia", "+421"),
            create("SI", "Slovenia", "+386"),
            create("EE", "Estonia", "+372"),
            create("LV", "Latvia", "+371"),
            create("LT", "Lithuania", "+370"),
            create("IS", "Iceland", "+354"),
            create("LU", "Luxembourg", "+352"),
            create("MA", "Morocco", "+212"),
            create("DZ", "Algeria", "+213"),
            create("TN", "Tunisia", "+216"),
            create("ET", "Ethiopia", "+251"),
            create("TZ", "Tanzania", "+255"),
            create("UG", "Uganda", "+256"),
            create("ZW", "Zimbabwe", "+263"),
            create("JO", "Jordan", "+962"),
            create("LB", "Lebanon", "+961"),
            create("IQ", "Iraq", "+964"),
            create("KZ", "Kazakhstan", "+7"),
            create("UZ", "Uzbekistan", "+998"),
            create("AZ", "Azerbaijan", "+994"),
            create("GE", "Georgia", "+995"),
            create("AM", "Armenia", "+374"),
            create("CR", "Costa Rica", "+506"),
            create("PA", "Panama", "+507"),
            create("UY", "Uruguay", "+598"),
            create("EC", "Ecuador", "+593"),
            create("VE", "Venezuela", "+58"),
            create("BO", "Bolivia", "+591"),
            create("PY", "Paraguay", "+595"),
            create("JM", "Jamaica", "+1876"),
            create("TT", "Trinidad and Tobago", "+1868"),
            create("BS", "Bahamas", "+1242"),
            create("BB", "Barbados", "+1246"),
            create("DO", "Dominican Republic", "+1809"),
            create("GT", "Guatemala", "+502"),
            create("HN", "Honduras", "+504"),
            create("SV", "El Salvador", "+503"),
            create("AF", "Afghanistan", "+93"),
            create("AL", "Albania", "+355"),
            create("AO", "Angola", "+244"),
            create("BW", "Botswana", "+267"),
            create("KH", "Cambodia", "+855"),
            create("CM", "Cameroon", "+237"),
            create("FJ", "Fiji", "+679"),
            create("MU", "Mauritius", "+230"),
            create("PG", "Papua New Guinea", "+675")
        )

        val DEFAULT: Country = ALL.first { it.isoCode == "IN" }

        /**
         * Safely extracts the local subscriber number from user input or clipboard paste.
         *
         * The visible phone input field MUST represent the LOCAL number only.
         * The country dial code is owned by the country picker.
         *
         * Examples for India (+91):
         * - User enters "8328627099" -> "8328627099"
         * - User pastes "+918328627099" -> "8328627099"
         * - User pastes "+91 8328627099" -> "8328627099"
         * - User pastes "918328627099" -> "8328627099"
         * - User pastes "08328627099" -> "8328627099"
         * - User pastes "1918328627099" -> "8328627099" (strips accidental US/191 prefix)
         *
         * Never prepends "191" or any digits not supplied by the user.
         */
        fun extractLocalNumber(country: Country, input: String): String {
            val trimmed = input.trim()
            if (trimmed.isEmpty()) return ""

            val dialDigits = country.dialCode.replace("+", "")

            // 1. If explicit international format (+...)
            if (trimmed.startsWith("+")) {
                val digits = trimmed.filter { it.isDigit() }
                if (digits.startsWith(dialDigits) && digits.length > dialDigits.length) {
                    val local = digits.substring(dialDigits.length)
                    return sanitizeLocalDigits(country, local)
                }
                return sanitizeLocalDigits(country, digits)
            }

            // 2. Local format or pasted raw digits
            val digits = trimmed.filter { it.isDigit() }

            if (country.isoCode == "IN") {
                // Safeguard against '191' prefix (accidental US '1' + India '91' concatenation)
                if (digits.startsWith("191") && digits.length == 13) {
                    return digits.substring(3)
                }
                // Country code 91 pasted without '+' (e.g. 918328627099 -> length 12)
                if (digits.startsWith("91") && digits.length == 12) {
                    return digits.substring(2)
                }
                // Trunk prefix '0' (e.g. 08328627099 -> length 11)
                if (digits.startsWith("0") && digits.length == 11) {
                    return digits.substring(1)
                }
                return digits
            }

            // Other countries:
            if (digits.startsWith(dialDigits) && digits.length > dialDigits.length + 5) {
                return digits.substring(dialDigits.length)
            }
            if (digits.startsWith("0") && digits.length > 7) {
                return digits.trimStart('0')
            }

            return sanitizeLocalDigits(country, digits)
        }

        private fun sanitizeLocalDigits(country: Country, digits: String): String {
            if (country.isoCode == "IN") {
                if (digits.startsWith("191") && digits.length == 13) {
                    return digits.substring(3)
                }
                if (digits.startsWith("91") && digits.length == 12) {
                    return digits.substring(2)
                }
                if (digits.startsWith("0") && digits.length == 11) {
                    return digits.substring(1)
                }
            }
            return digits
        }

        /**
         * Converts user input and selected country to normalized international E.164 format.
         *
         * Example:
         * Country = India (+91), nationalNumber = "8328627099" -> "+918328627099"
         * Country = India (+91), nationalNumber = "+918328627099" -> "+918328627099"
         * Country = India (+91), nationalNumber = "918328627099" -> "+918328627099"
         * Country = India (+91), nationalNumber = "1918328627099" -> "+918328627099"
         *
         * Under NO circumstances will output produce invalid numbers like "1918328627099".
         */
        fun normalizePhoneNumber(country: Country, nationalNumber: String): String {
            val localDigits = extractLocalNumber(country, nationalNumber)
            return "${country.dialCode}$localDigits"
        }

        /**
         * Validates phone number for the selected country.
         */
        fun validatePhoneNumber(country: Country, nationalNumber: String): String? {
            val cleanDigits = extractLocalNumber(country, nationalNumber)
            if (cleanDigits.isBlank()) return "Please enter your phone number."

            if (country.isoCode == "IN") {
                if (cleanDigits.length != 10) {
                    return "Please enter a valid 10-digit Indian mobile number."
                }
                val firstDigit = cleanDigits.first()
                if (firstDigit !in '6'..'9') {
                    return "Indian mobile numbers must begin with 6, 7, 8, or 9."
                }
                return null
            }

            return when {
                cleanDigits.length < 6 -> "Phone number is too short (at least 6 digits required)."
                cleanDigits.length > 15 -> "Phone number is too long (maximum 15 digits according to ITU-T E.164)."
                else -> null
            }
        }

        fun validatePhoneNumber(nationalNumber: String): String? {
            return validatePhoneNumber(DEFAULT, nationalNumber)
        }
    }
}
