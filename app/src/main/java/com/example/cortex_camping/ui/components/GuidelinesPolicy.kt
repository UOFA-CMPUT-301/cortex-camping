package com.example.cortex_camping.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.cortex_camping.ui.theme.CortexPalette
import com.example.cortex_camping.ui.theme.CortexTheme

/** Scrollable popup showing the app policies & guidelines. */
@Composable
fun GuidelinesPolicy(
    onDismiss: () -> Unit,
) {
    val colors = CortexTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceDefault,
        title = {
            Text(
                text = "Cortex Camping – App Policies & Guidelines",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = GUIDELINES_INTRO,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                Text(
                    text = GUIDELINES_AGREEMENT_NOTE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )

                GUIDELINE_SECTIONS.forEach { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        section.rules.forEach { rule ->
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.textPrimary)) {
                                        append("${rule.number} ${rule.label}: ")
                                    }
                                    append(rule.body)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CortexPalette.BrandPrimary)
            }
        },
    )
}

private data class GuidelineRule(val number: String, val label: String, val body: String)
private data class GuidelineSection(val title: String, val rules: List<GuidelineRule>)

private fun rule(number: String, label: String, body: String) = GuidelineRule(number, label, body)

private const val GUIDELINES_INTRO =
    "Welcome to Cortex Camping! Our goal is to provide a fair, safe, and welcoming platform where users and campsite owners can connect, participate in campsite lotteries, and share their experiences."

private const val GUIDELINES_AGREEMENT_NOTE =
    "By using this application, you agree to follow the policies and guidelines outlined below."

private val GUIDELINE_SECTIONS = listOf(
    GuidelineSection(
        "1. Authentic Campsite Listings",
        listOf(
            rule("1.1", "No Fake Campsites", "All listed campsites must be real, legitimate locations that are available for camping."),
            rule("1.2", "Accurate Information", "Campsite owners must provide truthful descriptions, locations, amenities, availability, pricing, and photographs."),
            rule("1.3", "No Misrepresentation", "Misleading photos, false advertisements, or inaccurate campsite details are prohibited."),
            rule("1.4", "Ownership Verification", "Users must not list campsites they do not own or have permission to manage."),
            rule("1.5", "Updated Listings", "Owners are responsible for keeping their campsite information and availability accurate and up to date."),
        ),
    ),
    GuidelineSection(
        "2. Respectful Communication",
        listOf(
            rule("2.1", "Be Respectful", "All users must treat one another with courtesy and respect in messages and other interactions."),
            rule("2.2", "No Harassment or Bullying", "Threatening, intimidating, bullying, or targeting other users is prohibited."),
            rule("2.3", "No Hate Speech", "Discrimination or hateful comments based on personal characteristics are not allowed."),
            rule("2.4", "No Inappropriate Content", "Sexually explicit, offensive, violent, or otherwise inappropriate messages are prohibited."),
            rule("2.5", "No Unwanted Contact", "Users must respect other people's boundaries and must not repeatedly message someone who does not wish to communicate."),
        ),
    ),
    GuidelineSection(
        "3. Reviews and Ratings",
        listOf(
            rule("3.1", "Honest Reviews", "Reviews must reflect genuine experiences with a campsite or reservation."),
            rule("3.2", "No Fake Reviews", "Fabricating reviews to manipulate campsite ratings is prohibited."),
            rule("3.3", "No Review Manipulation", "Offering rewards for positive reviews or pressuring users to change their ratings is not allowed."),
            rule("3.4", "No Personal Attacks", "Reviews should focus on the campsite and the user's experience rather than insulting or attacking individuals."),
            rule("3.5", "No Private Information", "Sharing another person's personal information in public reviews without their consent is prohibited."),
        ),
    ),
    GuidelineSection(
        "4. No Spam or Misuse",
        listOf(
            rule("4.1", "No Spam Messages", "Repeated, irrelevant, or unsolicited messages are prohibited."),
            rule("4.2", "No Unauthorized Advertising", "The messaging system and reviews must not be used to promote unrelated businesses, products, or services."),
            rule("4.3", "No Scamming", "Fraud, phishing, impersonation, and attempts to deceive other users are strictly prohibited."),
            rule("4.4", "No Platform Abuse", "Exploiting bugs, manipulating lottery results, or interfering with the application's normal operation is prohibited."),
            rule("4.5", "No Duplicate Accounts", "Creating multiple accounts to gain an unfair advantage in campsite lotteries is not allowed."),
        ),
    ),
    GuidelineSection(
        "5. Age-Appropriate Content",
        listOf(
            rule("5.1", "No Adult Content", "Sexually explicit, pornographic, or 18+ content is strictly prohibited."),
            rule("5.2", "No Sexual Harassment", "Unwanted sexual comments, advances, or inappropriate requests are not allowed."),
            rule("5.3", "No Inappropriate Images", "Uploading sexually suggestive, explicit, or otherwise unsuitable images is prohibited."),
            rule("5.4", "Appropriate Language", "All messages, reviews, usernames, and uploaded content must remain suitable for a general audience."),
        ),
    ),
    GuidelineSection(
        "6. Fair Lottery Participation",
        listOf(
            rule("6.1", "One Entry Per User", "Users may not enter the same lottery multiple times using different accounts or devices."),
            rule("6.2", "No Lottery Manipulation", "Attempts to manipulate lottery outcomes, exploit system vulnerabilities, or interfere with the selection process are prohibited."),
            rule("6.3", "No Selling Entries", "Lottery entries and winning opportunities cannot be sold, transferred, or exchanged for money."),
            rule("6.4", "Fair Participation", "All users must follow the established lottery rules and respect the outcome of the selection process."),
        ),
    ),
    GuidelineSection(
        "7. Account Responsibility and Security",
        listOf(
            rule("7.1", "No Impersonation", "Users must not pretend to be another user, campsite owner, or organization."),
            rule("7.2", "Account Sharing", "Accounts should only be accessed and used by their registered owners."),
            rule("7.3", "Account Security", "Users are responsible for maintaining the security of their accounts and must not share their login credentials."),
            rule("7.4", "Report Suspicious Activity", "Users should notify the platform if they encounter suspicious accounts, listings, messages, or activities."),
        ),
    ),
    GuidelineSection(
        "8. Prohibited Content and Activities",
        listOf(
            rule("8.1", "Illegal Activities", "Content promoting or encouraging illegal behaviour is strictly prohibited."),
            rule("8.2", "Threats and Violence", "Threats, violent content, or encouragement of harm are not allowed."),
            rule("8.3", "Hate Speech and Discrimination", "Content promoting hatred, discrimination, or harassment is prohibited."),
            rule("8.4", "Adult-Oriented Content", "Sexually explicit, pornographic, or otherwise adult-oriented content is strictly prohibited."),
            rule("8.5", "Fraudulent Activities", "Fake listings, scams, misleading information, and fraudulent behaviour are not allowed."),
            rule("8.6", "Privacy Violations", "Sharing another person's private or personal information without consent is prohibited."),
            rule("8.7", "Copyright Infringement", "Uploading or distributing copyrighted images or content without permission is not allowed."),
            rule("8.8", "Malicious Content", "Malicious links, phishing attempts, harmful files, or content intended to compromise user security are prohibited."),
            rule("8.9", "Dangerous Activities", "Content promoting dangerous, irresponsible, or harmful activities is not allowed."),
        ),
    ),
    GuidelineSection(
        "9. Reporting and Moderation",
        listOf(
            rule("9.1", "Reporting Violations", "Users are encouraged to report inappropriate listings, messages, reviews, or accounts."),
            rule("9.2", "Honest Reporting", "Reports must be submitted honestly and should include relevant information whenever possible."),
            rule("9.3", "Content Review", "Reported content may be reviewed by platform administrators to determine whether a violation has occurred."),
            rule("9.4", "Content Removal", "Administrators reserve the right to remove content that violates these guidelines."),
            rule("9.5", "Account Restrictions", "Users who repeatedly violate policies may receive warnings, temporary suspensions, or permanent account bans."),
            rule("9.6", "Serious Violations", "Serious violations may result in immediate account suspension or permanent removal without prior warning."),
        ),
    ),
    GuidelineSection(
        "10. Consequences of Violations",
        listOf(
            rule("10.1", "Minor Inappropriate Behaviour", "Users may receive a warning for minor violations of the guidelines."),
            rule("10.2", "Spam or Repeated Unwanted Messages", "Users may receive a temporary suspension."),
            rule("10.3", "Fake Reviews or Campsite Listings", "Misleading content may be removed, and the responsible account may be suspended."),
            rule("10.4", "Harassment or Hate Speech", "Users may receive a suspension or permanent account ban depending on the severity of the violation."),
            rule("10.5", "Lottery Manipulation", "Users may be disqualified from lotteries or have their accounts suspended."),
            rule("10.6", "Scams or Fraudulent Activity", "Serious fraudulent behaviour may result in immediate account termination."),
            rule("10.7", "Explicit Adult Content", "Inappropriate content will be removed, and the responsible account may receive a suspension or permanent ban."),
            rule("10.8", "Administrative Discretion", "Administrators reserve the right to determine appropriate consequences based on the severity, frequency, and nature of each violation."),
        ),
    ),
    GuidelineSection(
        "11. Agreement to Policies",
        listOf(
            rule("11.1", "Acceptance of Guidelines", "By creating an account and using Cortex Camping, users acknowledge that they have read, understood, and agreed to follow these policies and guidelines."),
            rule("11.2", "Continued Compliance", "Users are expected to follow these guidelines throughout their use of the application."),
            rule("11.3", "Policy Enforcement", "Failure to comply with these policies may result in content removal, account restrictions, suspension, or permanent account termination."),
        ),
    ),
)