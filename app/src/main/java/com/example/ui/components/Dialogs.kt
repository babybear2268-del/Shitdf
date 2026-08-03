package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CanadianIdentityGenerator
import com.example.data.CardProfileEntity
import com.example.data.PaymentCardEntity
import com.example.data.WorkerJobEntity
import com.example.ui.theme.*
import java.util.UUID

@Composable
fun AddEditProfileDialog(
    existingProfile: CardProfileEntity?,
    onSave: (CardProfileEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var targetUrl by remember { mutableStateOf(existingProfile?.targetPortalUrl ?: "") }
    var selector by remember { mutableStateOf(existingProfile?.balanceSelector ?: "") }
    var firstName by remember { mutableStateOf(existingProfile?.holderFirstName ?: "") }
    var lastName by remember { mutableStateOf(existingProfile?.holderLastName ?: "") }
    var email by remember { mutableStateOf(existingProfile?.holderEmail ?: "") }
    var phone by remember { mutableStateOf(existingProfile?.holderPhone ?: "") }
    var street by remember { mutableStateOf(existingProfile?.billingStreet ?: "") }
    var unit by remember { mutableStateOf(existingProfile?.billingUnit ?: "") }
    var city by remember { mutableStateOf(existingProfile?.billingCity ?: "") }
    var state by remember { mutableStateOf(existingProfile?.billingState ?: "") }
    var zip by remember { mutableStateOf(existingProfile?.billingZip ?: "") }
    var country by remember { mutableStateOf(existingProfile?.billingCountry ?: "Canada") }
    var cardNumber by remember { mutableStateOf(existingProfile?.cardNumber ?: "") }
    var expMonth by remember { mutableStateOf(existingProfile?.cardExpMonth ?: "") }
    var expYear by remember { mutableStateOf(existingProfile?.cardExpYear ?: "") }
    var cvc by remember { mutableStateOf(existingProfile?.cardCvc ?: "") }

    val detectedBrand = remember(cardNumber) { com.example.util.CardValidator.detectBrand(cardNumber) }
    val isLuhnValid = remember(cardNumber) { com.example.util.CardValidator.isValidLuhn(cardNumber) }
    val isExpiryValid = remember(expMonth, expYear) { com.example.util.CardValidator.isValidExpiry(expMonth, expYear) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .border(1.dp, HighDensityBorder, RoundedCornerShape(12.dp)),
            color = HighDensitySurface,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (existingProfile == null) "NEW CARD PROFILE" else "EDIT PROFILE (${existingProfile.id})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryCyan
                        )
                    )
                    Button(
                        onClick = {
                            val generated = CanadianIdentityGenerator.generate(targetUrl)
                            targetUrl = generated.targetPortalUrl
                            selector = generated.balanceSelector
                            firstName = generated.holderFirstName
                            lastName = generated.holderLastName
                            email = generated.holderEmail
                            phone = generated.holderPhone
                            street = generated.billingStreet
                            unit = generated.billingUnit
                            city = generated.billingCity
                            state = generated.billingState
                            zip = generated.billingZip
                            country = generated.billingCountry
                            cardNumber = generated.cardNumber
                            expMonth = generated.cardExpMonth
                            expYear = generated.cardExpYear
                            cvc = generated.cardCvc
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HighDensitySurfaceVariant),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("dialog_generate_ca_identity")
                    ) {
                        Icon(Icons.Default.Casino, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AUTO-GEN CANADIAN", style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = targetUrl,
                        onValueChange = { targetUrl = it },
                        label = { Text("Target Portal URL", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth().testTag("profile_input_url")
                    )

                    Text(
                        text = "PRESET CHECK BALANCE PORTALS:",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMuted, fontSize = 9.sp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf(
                            Triple("Joker Card", "https://cardholder.jokercard.ca/", ".balance-amount, #cardBalance, .card-details"),
                            Triple("MyPrepaidCenter", "https://www.myprepaidcenter.com/login/card", ".card-balance, #balance"),
                            Triple("Vanilla Gift", "https://www.vanillagift.com/balance", ".balance-amount, .card-balance"),
                            Triple("PrepaidCardStatus", "https://www.prepaidcardstatus.com", "#balanceVal, .account-total"),
                            Triple("PerfectGift", "https://www.perfectgift.com/check-balance", ".balance-value, .card-details"),
                            Triple("Cardholderplace", "https://www.cardholderplace.com", "#cardBalance, .balance"),
                            Triple("PrepaidDigital", "https://www.prepaiddigitalsolutions.com", ".amount, .card-balance"),
                            Triple("GiftCardMall", "https://www.giftcardmall.com/check-balance", ".balance-info, #balance")
                        )

                        presets.forEach { (name, url, sel) ->
                            val isSelected = targetUrl == url
                            Surface(
                                onClick = {
                                    targetUrl = url
                                    if (selector.isBlank() || presets.any { it.third == selector }) {
                                        selector = sel
                                    }
                                },
                                color = if (isSelected) PrimaryCyan.copy(alpha = 0.2f) else HighDensitySurfaceVariant,
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) PrimaryCyan else HighDensityBorder)
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) PrimaryCyan else TextSecondary,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = selector,
                        onValueChange = { selector = it },
                        label = { Text("Balance DOM Selector", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth().testTag("profile_input_selector")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text("First Name", color = TextSecondary) },
                            modifier = Modifier.weight(1f).testTag("profile_input_fn")
                        )
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text("Last Name", color = TextSecondary) },
                            modifier = Modifier.weight(1f).testTag("profile_input_ln")
                        )
                    }
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth().testTag("profile_input_email")
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth().testTag("profile_input_phone")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = street,
                            onValueChange = { street = it },
                            label = { Text("Street Address", color = TextSecondary) },
                            modifier = Modifier.weight(2f)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit/Apt", color = TextSecondary) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City", color = TextSecondary) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state,
                            onValueChange = { state = it },
                            label = { Text("Province/State", color = TextSecondary) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = zip,
                            onValueChange = { zip = it },
                            label = { Text("ZIP / Postal Code", color = TextSecondary) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = HighDensityBorder, modifier = Modifier.padding(vertical = 4.dp))
                    Text("CARD DETAILS", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = SecondaryEmerald))

                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { cardNumber = it },
                        label = { Text("Card Number", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth().testTag("profile_input_card")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = expMonth,
                            onValueChange = { expMonth = it },
                            label = { Text("Exp Month", color = TextSecondary) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = expYear,
                            onValueChange = { expYear = it },
                            label = { Text("Exp Year", color = TextSecondary) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cvc,
                            onValueChange = { cvc = it },
                            label = { Text("CVC", color = TextSecondary) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val prof = CardProfileEntity(
                                id = existingProfile?.id ?: "PRF-CA-${UUID.randomUUID().toString().take(4).uppercase()}",
                                targetPortalUrl = targetUrl,
                                balanceSelector = selector,
                                holderFirstName = firstName,
                                holderLastName = lastName,
                                holderEmail = email,
                                holderPhone = phone,
                                billingStreet = street,
                                billingUnit = unit,
                                billingCity = city,
                                billingState = state,
                                billingZip = zip,
                                billingCountry = country,
                                cardNumber = cardNumber,
                                cardExpMonth = expMonth,
                                cardExpYear = expYear,
                                cardCvc = cvc
                            )
                            onSave(prof)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                        modifier = Modifier.testTag("profile_save_button")
                    ) {
                        Text("SAVE PROFILE", color = OnPrimaryDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddCardToProfileDialog(
    profile: CardProfileEntity,
    onSaveCard: (PaymentCardEntity) -> Unit,
    onDismiss: () -> Unit,
    onScanPhotoClick: (() -> Unit)? = null
) {
    var num by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var cvc by remember { mutableStateOf("") }
    var holder by remember { mutableStateOf("${profile.holderFirstName} ${profile.holderLastName}".trim().uppercase()) }
    var brand by remember { mutableStateOf("Visa") }
    var serial by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = HighDensitySurface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.border(1.dp, HighDensityBorder, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ASSOCIATE NEW CARD TO PROFILE",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace, color = PrimaryCyan, fontWeight = FontWeight.Bold)
                    )
                    if (onScanPhotoClick != null) {
                        IconButton(onClick = onScanPhotoClick, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Scan Photo", tint = PrimaryCyan)
                        }
                    }
                }

                if (onScanPhotoClick != null) {
                    OutlinedButton(
                        onClick = onScanPhotoClick,
                        modifier = Modifier.fillMaxWidth().testTag("add_card_dialog_scan_photo_button")
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AUTO-FILL BY PHOTO SCAN (GEMINI AI)", style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                    }
                }

                OutlinedTextField(value = num, onValueChange = { num = it }, label = { Text("Card Number") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = month, onValueChange = { month = it }, label = { Text("Month") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("Year") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = cvc, onValueChange = { cvc = it }, label = { Text("CVC") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = holder, onValueChange = { holder = it }, label = { Text("Cardholder Name") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = serial, onValueChange = { serial = it }, label = { Text("Serial Ref") }, modifier = Modifier.weight(1f))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("CANCEL", color = TextMuted) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val card = PaymentCardEntity(
                                profileId = profile.id,
                                cardNumber = num,
                                cardExpMonth = month,
                                cardExpYear = year,
                                cardCvc = cvc,
                                cardholderName = holder,
                                cardBrand = brand,
                                cardSerialRef = serial,
                                balance = 0.0,
                                isPrimary = false
                            )
                            onSaveCard(card)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryEmerald)
                    ) {
                        Text("ADD CARD", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun JobDetailsDialog(
    job: WorkerJobEntity,
    onRetryJob: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.80f)
                .border(1.dp, HighDensityBorder, RoundedCornerShape(12.dp)),
            color = HighDensitySurface,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WORKER TELEMETRY: ${job.jobId}",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, color = PrimaryCyan, fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                PlaywrightLiveView(
                    job = job,
                    onRetryJob = onRetryJob,
                    onClose = onDismiss,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onRetryJob,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = OnPrimaryDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RE-EXECUTE WORKER", color = OnPrimaryDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun JsonManifestDialog(
    jsonText: String,
    onImportJson: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textState by remember { mutableStateOf(jsonText) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .border(1.dp, HighDensityBorder, RoundedCornerShape(12.dp)),
            color = HighDensitySurface,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "JSON MANIFEST IMPORT / EXPORT",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace, color = PrimaryCyan, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("json_manifest_textarea"),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = SecondaryEmerald, fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("CLOSE", color = TextMuted) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onImportJson(textState)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryEmerald)
                    ) {
                        Text("IMPORT MANIFEST", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
