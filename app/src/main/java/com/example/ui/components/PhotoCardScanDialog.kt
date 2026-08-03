package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.CardProfileEntity
import com.example.data.PaymentCardEntity
import com.example.network.ExtractedCardData
import com.example.network.GeminiCardScanner
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PhotoCardScanDialog(
    profiles: List<CardProfileEntity>,
    initialProfileId: String? = null,
    onSaveExtractedCard: (PaymentCardEntity, String) -> Unit, // card, selectedProfileId
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var extractedData by remember { mutableStateOf<ExtractedCardData?>(null) }

    var selectedProfileId by remember(profiles, initialProfileId) {
        mutableStateOf(initialProfileId ?: profiles.firstOrNull()?.id ?: "")
    }

    // Editable extracted fields
    var cardNumber by remember { mutableStateOf("") }
    var expMonth by remember { mutableStateOf("") }
    var expYear by remember { mutableStateOf("") }
    var cvc by remember { mutableStateOf("") }
    var cardholderName by remember { mutableStateOf("") }
    var cardBrand by remember { mutableStateOf("Visa") }
    var serialRef by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    selectedBitmap = bmp
                    if (bmp != null) {
                        isAnalyzing = true
                        coroutineScope.launch {
                            val result = GeminiCardScanner.analyzeCardPhoto(bmp)
                            extractedData = result
                            cardNumber = result.cardNumber
                            expMonth = result.expMonth
                            expYear = result.expYear
                            cvc = result.cvc
                            cardholderName = result.cardholderName
                            cardBrand = result.cardBrand
                            serialRef = result.serialRef
                            isAnalyzing = false
                        }
                    }
                }
            } catch (e: Exception) {
                isAnalyzing = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = HighDensitySurface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, HighDensityBorder, RoundedCornerShape(12.dp))
                .testTag("photo_card_scan_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ADD CARD BY PHOTO SCAN",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = PrimaryCyan,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Text(
                    text = "Upload or take a photo of a Visa, Mastercard, or Prepaid Card. AI Gemini Vision extracts card details automatically.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                )

                // Image Preview / Scanner Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HighDensityBackground)
                        .border(1.dp, if (selectedBitmap != null) PrimaryCyan else HighDensityBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedBitmap != null) {
                        Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = "Card Photo Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )

                        if (isAnalyzing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = PrimaryCyan, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "GEMINI MULTIMODAL AI ANALYZING CARD...",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = PrimaryCyan, fontSize = 10.sp)
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(40.dp))
                            Text(
                                text = "NO CARD PHOTO SELECTED",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMuted)
                            )
                            Button(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("select_photo_button")
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = OnPrimaryDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SELECT FROM GALLERY / CAMERA", style = MaterialTheme.typography.labelSmall.copy(color = OnPrimaryDark, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }

                // Sample Preset Photos for Instant Demo
                Text(
                    text = "OR SCAN SAMPLE CARD DEMOS:",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMuted, fontSize = 9.sp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val sampleCards = listOf(
                        SampleCardPreset("Joker Visa $100 (Attached Photo)", "4611260031926267", "07", "33", "039", "GIFT RECIPIENT", "Visa (Joker)", "6039539105224365840"),
                        SampleCardPreset("Vanilla Visa $100", "4532829102938471", "09", "28", "832", "JOHN DOE", "Visa", "SN-93821"),
                        SampleCardPreset("MyPrepaid Mastercard", "5412738291028472", "11", "27", "492", "PREPAID HOLDER", "Mastercard", "MPC-8821"),
                        SampleCardPreset("PerfectGift Amex", "378282910293841", "04", "29", "1029", "VALUED CUSTOMER", "American Express", "AMX-3012")
                    )

                    sampleCards.forEach { preset ->
                        Surface(
                            onClick = {
                                isAnalyzing = true
                                val dummyBmp = Bitmap.createBitmap(400, 250, Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(dummyBmp)
                                canvas.drawColor(android.graphics.Color.DKGRAY)
                                selectedBitmap = dummyBmp

                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(600)
                                    cardNumber = preset.num
                                    expMonth = preset.m
                                    expYear = preset.y
                                    cvc = preset.c
                                    cardholderName = preset.holder
                                    cardBrand = preset.b
                                    serialRef = preset.s
                                    extractedData = ExtractedCardData(
                                        cardNumber = preset.num,
                                        expMonth = preset.m,
                                        expYear = preset.y,
                                        cvc = preset.c,
                                        cardholderName = preset.holder,
                                        cardBrand = preset.b,
                                        serialRef = preset.s,
                                        isSuccess = true
                                    )
                                    isAnalyzing = false
                                }
                            },
                            color = HighDensitySurfaceVariant,
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HighDensityBorder)
                        ) {
                            Text(
                                text = "📷 ${preset.label}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = PrimaryCyan, fontSize = 10.sp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (extractedData != null) {
                    Surface(
                        color = SecondaryEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryEmerald)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SecondaryEmerald, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI EXTRACTION SUCCESSFUL (CONFIDENCE: ${(extractedData!!.confidenceScore * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = SecondaryEmerald, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            )
                        }
                    }
                }

                // Extracted Form Fields
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { cardNumber = it },
                        label = { Text("Extracted Card Number", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth().testTag("scanned_card_number_input")
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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cardholderName,
                            onValueChange = { cardholderName = it },
                            label = { Text("Cardholder Name", color = TextSecondary) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cardBrand,
                            onValueChange = { cardBrand = it },
                            label = { Text("Brand", color = TextSecondary) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Profile Selector to assign card to
                    Text(
                        text = "ASSIGN CARD TO PROFILE:",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMuted, fontSize = 10.sp)
                    )

                    if (profiles.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            profiles.forEach { prof ->
                                val isSelected = selectedProfileId == prof.id
                                Surface(
                                    onClick = { selectedProfileId = prof.id },
                                    color = if (isSelected) PrimaryCyan.copy(alpha = 0.2f) else HighDensitySurfaceVariant,
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) PrimaryCyan else HighDensityBorder)
                                ) {
                                    Text(
                                        text = "${prof.id} (${prof.holderFirstName})",
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
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                            if (cardNumber.isNotBlank() && selectedProfileId.isNotBlank()) {
                                val card = PaymentCardEntity(
                                    profileId = selectedProfileId,
                                    cardNumber = cardNumber,
                                    cardExpMonth = expMonth,
                                    cardExpYear = expYear,
                                    cardCvc = cvc,
                                    cardholderName = cardholderName,
                                    cardBrand = cardBrand,
                                    cardSerialRef = serialRef,
                                    balance = 0.0,
                                    isPrimary = false
                                )
                                onSaveExtractedCard(card, selectedProfileId)
                            }
                        },
                        enabled = cardNumber.isNotBlank() && selectedProfileId.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                        modifier = Modifier.testTag("apply_scanned_card_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = OnPrimaryDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("APPLY & SAVE CARD", color = OnPrimaryDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private data class SampleCardPreset(
    val label: String,
    val num: String,
    val m: String,
    val y: String,
    val c: String,
    val holder: String,
    val b: String,
    val s: String
)
