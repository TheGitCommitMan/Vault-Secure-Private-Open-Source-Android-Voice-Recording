package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.database.JournalEntry
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    JournalAppScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalAppScreen(
    modifier: Modifier = Modifier,
    viewModel: JournalViewModel = viewModel()
) {
    val context = LocalContext.current
    val entries by viewModel.allEntries.collectAsState()
    val decryptedEntries by viewModel.decryptedEntries.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()

    var selectedCategory by remember { mutableStateOf("Personal") }
    var recordingDurationSec by remember { mutableStateOf(0) }

    // Launcher for Audio capturing permission request
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording()
        }
    }

    // Sound recording timer tracking
    LaunchedEffect(recordingState) {
        if (recordingState is RecordingState.Listening) {
            recordingDurationSec = 0
            while (true) {
                delay(1000)
                recordingDurationSec++
            }
        }
    }

    // Dynamic scale oscillation for recorder animation
    val infiniteTransition = rememberInfiniteTransition(label = "RadarAnimation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgLight)
    ) {
        // --- 1. Top Decorative Navigation / Stats Header ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo & On-Device Info Badge with M3 rounded icon-like details
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BadgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Logo",
                            tint = BadgeText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Vaulted",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "ON-DEVICE AES-256",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMedium,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Security Mode Indicator badge/button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(BadgeBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "AES-256 Secured",
                        tint = BadgeText,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SECURE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BadgeText,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-status row confirming no internet usage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NO NETWORK PERMISSION REQUIREMENT",
                    fontSize = 10.sp,
                    color = TextMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(RedAlertDot, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ZERO DATA SENT",
                        fontSize = 10.sp,
                        color = RedAlertDot,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            HorizontalDivider(
                color = BorderLightSecondary,
                thickness = 1.dp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        // --- 2. Scrollable Journal Entries List or Lighter Empty State ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (entries.isEmpty()) {
                // Clean Minimalist Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(BadgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Outline",
                            tint = BadgeText,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Your voice remains secret.",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextDark,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No audio or text ever leaves this phone. Recording is done strictly offline. Tap entries to decrypt them instantly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMedium,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        val isDecrypted = decryptedEntries.containsKey(entry.id)
                        val content = decryptedEntries[entry.id] ?: ""

                        JournalEntryCard(
                            entry = entry,
                            isDecrypted = isDecrypted,
                            decryptedContent = content,
                            onToggleDecrypt = { viewModel.toggleDecryption(entry) },
                            onDelete = { viewModel.deleteEntry(entry) }
                        )
                    }
                }
            }
        }

        // --- 3. Dynamic Interactive Voice Controller Sheet ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
            color = CardBgLight,
            border = BorderStroke(1.dp, BorderLightSecondary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive state visualizers (Cascading State View)
                when (val state = recordingState) {
                    is RecordingState.Idle -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(RedAlertDot, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ZERO INTERNET CONNECTION REQUIRED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMedium,
                                letterSpacing = 0.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Concentric Outer pulsing circle
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(96.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(PulseColor)
                            )
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryPurple)
                                    .clickable {
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasPermission) {
                                            viewModel.startRecording()
                                        } else {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                    .testTag("record_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Start Voice Note",
                                    tint = CardBgLight,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tap to Record Voice Note",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }

                    is RecordingState.Listening -> {
                        // cassette-tape duration indicator
                        Text(
                            text = "REC  ${formatDuration(recordingDurationSec)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedAlertDot,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Pulser
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(96.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(PulseColor)
                            )
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(RedAlertDot)
                                    .clickable { viewModel.stopRecording() }
                                    .testTag("stop_record_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(CardBgLight, RoundedCornerShape(2.dp))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Category Selector chip row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Tag:",
                                color = TextMedium,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            listOf("Personal", "Ideas", "Secrets", "Daily").forEach { cat ->
                                val selected = selectedCategory == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) BadgeBg else CardBgLight)
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "#$cat",
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) BadgeText else TextMedium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Live Speech Output Text Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp, max = 120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardAccentBg)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (liveTranscript.isBlank()) "Listening... speak clearly..." else liveTranscript,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (liveTranscript.isBlank()) TextMedium.copy(alpha = 0.6f) else TextDark,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Control Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = { viewModel.cancelRecording() },
                                colors = ButtonDefaults.buttonColors(containerColor = CardAccentBg)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = RedAlertDot, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Discard", color = TextDark)
                            }

                            Button(
                                onClick = { viewModel.saveEntry(selectedCategory, recordingDurationSec) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                enabled = liveTranscript.isNotBlank()
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Save", tint = CardBgLight, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Encrypted", color = CardBgLight, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    is RecordingState.Deserializing -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PrimaryPurple, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "ENCRYPTING WITH AES-256...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryPurple,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    is RecordingState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(RedAlertDot.copy(alpha = 0.15f))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ENGINE WARNING",
                                color = RedAlertDot,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = state.message,
                                color = TextDark,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.clearError() },
                                colors = ButtonDefaults.buttonColors(containerColor = RedAlertDot)
                            ) {
                                Text("Dismiss", color = CardBgLight, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun JournalEntryCard(
    entry: JournalEntry,
    isDecrypted: Boolean,
    decryptedContent: String,
    onToggleDecrypt: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val df = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    val dateString = df.format(Date(entry.timestamp))

    // Animated Hex Scramble simulation
    val hexBlobPlaceholder = remember(entry.encryptedText) {
        entry.encryptedText.take(16).joinToString(" ") { String.format("%02X", it) }.plus("...")
    }

    val isAlternate = entry.id % 2 == 1
    val cardBg = if (isAlternate) CardAccentBg else CardBgLight
    val cardBorder = if (isAlternate) BorderLight else BorderLightSecondary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggleDecrypt() }
            .testTag("entry_card_${entry.id}"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, if (isDecrypted) PrimaryPurple.copy(alpha = 0.4f) else cardBorder),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Date, Length, category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(BadgeBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "#${entry.category}",
                            color = BadgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${entry.durationSec}s",
                        color = TextMedium,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = dateString,
                    color = TextMedium,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body: Encrypted cipher stream vs Decrypted real text
            AnimatedContent(
                targetState = isDecrypted,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.95f, animationSpec = tween(220, delayMillis = 90)))
                        .togetherWith(fadeOut(animationSpec = tween(90)))
                },
                label = "DecryptAnimation"
            ) { opened ->
                if (opened) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Decrypted",
                                tint = PrimaryPurple,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AES-256 AES-GCM TRANSLATED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryPurple,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\"$decryptedContent\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextDark,
                            lineHeight = 22.sp,
                            fontStyle = if (isAlternate) FontStyle.Italic else FontStyle.Normal,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isAlternate) CardBgLight else CardAccentBg)
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Encrypted",
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SECURE ENCRYPTED BLOB",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryPurple,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                "TAP TO DECRYPT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMedium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = hexBlobPlaceholder,
                            fontSize = 12.sp,
                            color = TextMedium,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer / Trash action button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { onDelete() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete entry",
                        tint = TextMedium,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
