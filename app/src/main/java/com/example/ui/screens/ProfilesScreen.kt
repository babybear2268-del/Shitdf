package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CardProfileEntity
import com.example.data.PaymentCardEntity
import com.example.ui.theme.*

@Composable
fun ProfilesScreen(
    profiles: List<CardProfileEntity>,
    cards: List<PaymentCardEntity>,
    isCheckingUnactivated: Boolean = false,
    onAddProfileClick: () -> Unit,
    onEditProfileClick: (CardProfileEntity) -> Unit,
    onDeleteProfileClick: (String) -> Unit,
    onRunPipelineForProfile: (String) -> Unit,
    onAddCardToProfileClick: (CardProfileEntity) -> Unit,
    onSelectPrimaryCard: (profileId: String, cardId: String) -> Unit,
    onDeleteCardClick: (String) -> Unit,
    onJsonManifestClick: () -> Unit,
    onTriggerCheckUnactivated: () -> Unit = {},
    onScanCardPhotoClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CARD PROFILES & IDENTITY",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryCyan
                    )
                )
                val unactivatedCount = cards.count { !it.isActivated && it.balance <= 0.0 }
                Text(
                    text = "${profiles.size} Profiles | $unactivatedCount Pending 5m Auto-Check",
                    style = MaterialTheme.typography.labelSmall.copy(color = if (unactivatedCount > 0) AccentAmber else TextSecondary)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onScanCardPhotoClick,
                    modifier = Modifier.testTag("profiles_scan_photo_button")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SCAN PHOTO", style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary))
                }

                OutlinedButton(
                    onClick = onTriggerCheckUnactivated,
                    enabled = !isCheckingUnactivated,
                    modifier = Modifier.testTag("profiles_check_unactivated_button")
                ) {
                    if (isCheckingUnactivated) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = AccentAmber, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CHECKING...", style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary))
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("5M QUEUE", style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary))
                    }
                }

                Button(
                    onClick = onAddProfileClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                    modifier = Modifier.testTag("profiles_add_new_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = OnPrimaryDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ADD PROFILE", color = OnPrimaryDark, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (profiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(HighDensitySurface, RoundedCornerShape(8.dp))
                    .border(1.dp, HighDensityBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No Card Profiles Configured",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted, fontFamily = FontFamily.Monospace)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onAddProfileClick, colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)) {
                        Text("CREATE FIRST PROFILE", color = OnPrimaryDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(profiles, key = { it.id }) { profile ->
                    val profileCards = cards.filter { it.profileId == profile.id }
                    ProfileCardItem(
                        profile = profile,
                        associatedCards = profileCards,
                        onEditClick = { onEditProfileClick(profile) },
                        onDeleteClick = { onDeleteProfileClick(profile.id) },
                        onRunPipeline = { onRunPipelineForProfile(profile.id) },
                        onAddCard = { onAddCardToProfileClick(profile) },
                        onSelectPrimaryCard = { cardId -> onSelectPrimaryCard(profile.id, cardId) },
                        onDeleteCard = onDeleteCardClick
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileCardItem(
    profile: CardProfileEntity,
    associatedCards: List<PaymentCardEntity>,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRunPipeline: () -> Unit,
    onAddCard: () -> Unit,
    onSelectPrimaryCard: (String) -> Unit,
    onDeleteCard: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, HighDensityBorder, RoundedCornerShape(10.dp))
            .testTag("profile_card_item_${profile.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${profile.holderFirstName} ${profile.holderLastName}".uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "ID: ${profile.id} | ${profile.billingCity}, ${profile.billingState} (${profile.billingCountry})",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onRunPipeline, modifier = Modifier.testTag("profile_run_pipeline_${profile.id}")) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Run Worker", tint = SecondaryEmerald)
                    }
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = PrimaryCyan)
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Profile", tint = ErrorRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Portal & Identity Details
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HighDensitySurfaceVariant, RoundedCornerShape(6.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("TARGET PORTAL: ${profile.targetPortalUrl}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = PrimaryCyan, fontSize = 11.sp))
                    Text("EMAIL: ${profile.holderEmail} | PHONE: ${profile.holderPhone}", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                    Text("BILLING: ${profile.billingStreet} ${profile.billingUnit}, ${profile.billingZip}", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Associated Cards Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ASSOCIATED PAYMENT CARDS (${associatedCards.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMuted)
                )

                TextButton(onClick = onAddCard) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("ADD CARD", style = MaterialTheme.typography.labelSmall.copy(color = PrimaryCyan))
                }
            }

            if (associatedCards.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    associatedCards.forEach { card ->
                        val isCardActive = card.isActivated || card.balance > 0.0
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (card.isPrimary) HighDensityBackground else HighDensitySurfaceVariant, RoundedCornerShape(6.dp))
                                .border(1.dp, if (isCardActive) SecondaryEmerald else AccentAmber.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (card.isPrimary) {
                                        Icon(Icons.Default.Star, contentDescription = "Primary", tint = AccentAmber, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = "${card.cardBrand} **** **** **** ${card.cardNumber.takeLast(4)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    )
                                }

                                Surface(
                                    color = if (isCardActive) SecondaryEmerald.copy(alpha = 0.2f) else AccentAmber.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isCardActive) SecondaryEmerald else AccentAmber)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isCardActive) Icons.Default.CheckCircle else Icons.Default.Sync,
                                            contentDescription = null,
                                            tint = if (isCardActive) SecondaryEmerald else AccentAmber,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isCardActive) "ACTIVATED ($${card.balance})" else "PENDING ACTIVATION (5m Auto-Check)",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCardActive) SecondaryEmerald else AccentAmber,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val lastCheckedStr = if (card.lastCheckedAt > 0L) {
                                    val minutesAgo = ((System.currentTimeMillis() - card.lastCheckedAt) / (1000 * 60)).coerceAtLeast(0)
                                    if (minutesAgo < 1) "Last Checked: Just now" else "Last Checked: ${minutesAgo}m ago"
                                } else {
                                    "Last Checked: Never (Queued)"
                                }

                                Text(
                                    text = "EXP: ${card.cardExpMonth}/${card.cardExpYear} | CVC: ${card.cardCvc} | $lastCheckedStr",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!card.isPrimary) {
                                        TextButton(onClick = { onSelectPrimaryCard(card.id) }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                                            Text("SET PRIMARY", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp))
                                        }
                                    }
                                    IconButton(onClick = { onDeleteCard(card.id) }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete Card", tint = TextMuted, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
