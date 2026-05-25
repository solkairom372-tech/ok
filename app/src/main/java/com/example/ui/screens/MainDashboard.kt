package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.alarm.AlarmScheduler
import com.example.alarm.AlarmViewModel
import com.example.data.Alarm
import com.example.data.SleepSession
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(viewModel: AlarmViewModel) {
    val alarms by viewModel.alarms.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val isAlarmActive by viewModel.isAlarmActive.collectAsState()
    val isSpeechLoading by viewModel.isSpeechLoading.collectAsState()
    val spokenText by viewModel.spokenText.collectAsState()
    val isSleepModeActive by viewModel.isSleepModeActive.collectAsState()
    val lastSessionSaved by viewModel.lastSessionSaved.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var currentHour by remember { mutableStateOf("") }
    var currentMinute by remember { mutableStateOf("") }
    var customTextPrompt by remember { mutableStateOf("") }
    var soundAlarm by remember { mutableStateOf("Bosque Sereno") }

    // Screen strobe background flashing transition when alarm is active (Luz Titilante Oscuro-Claro)
    val infiniteTransition = rememberInfiniteTransition(label = "strobe")
    val flashingBgColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF0D0D10),
        targetValue = Color(0xFFFFFFFF),
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "strobe_animation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
    ) {
        // --- RINGING FULL SCREEN OVERLAY ("Luz Titilante" strobe theme changer) ---
        if (isAlarmActive) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(flashingBgColor)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Determine text coloring based on flashing color background brightness to maintain accessibility readability
                val textColor = if (flashingBgColor.red > 0.5f) Color.Black else Color.White
                val accentCardColor = if (flashingBgColor.red > 0.5f) Color(0xFFE2E2E6) else Color(0xFF161618)
                val accentCardText = if (flashingBgColor.red > 0.5f) Color.Black else Color.White

                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Alarma Alerta Flashing",
                    tint = if (flashingBgColor.red > 0.5f) Color(0xFFFFB300) else Color(0xFFFFD54F),
                    modifier = Modifier
                        .size(100.dp)
                        .padding(bottom = 16.dp)
                )

                Text(
                    text = "¡ALERTA DE DESPERTADOR!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "La IA está sintetizando tu reporte matutino...",
                    fontSize = 16.sp,
                    color = textColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Speech bubble text loading state / generated content
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = accentCardColor)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isSpeechLoading) {
                            CircularProgressIndicator(
                                color = if (flashingBgColor.red > 0.5f) Color.DarkGray else Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Conectando con Gemini AI...\nGenerando clima, noticias y mensaje personalizado.",
                                color = accentCardText.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Asistente de Voz",
                                    tint = if (flashingBgColor.red > 0.5f) Color.Black else Color.Yellow,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "REPORTE MATUTINO IA (TTS):",
                                    color = accentCardText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = spokenText,
                                color = accentCardText,
                                fontSize = 15.sp,
                                lineHeight = 21.sp,
                                textAlign = TextAlign.Left
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Premium Smooth dismiss wake button
                Button(
                    onClick = { viewModel.dismissAlarm() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (flashingBgColor.red > 0.5f) Color.Black else Color.White,
                        contentColor = if (flashingBgColor.red > 0.5f) Color.White else Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(64.dp)
                        .border(
                            width = 2.dp,
                            color = if (flashingBgColor.red > 0.5f) Color.DarkGray else Color.LightGray,
                            shape = RoundedCornerShape(32.dp)
                        ),
                    shape = RoundedCornerShape(32.dp)
                ) {
                    Text(
                        "DESACTIVAR DESPERTADOR ☕",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
        // --- MANUAL SLEEP SCREENSAVER (Modo Sueño) ---
        else if (isSleepModeActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SlateBackground)
                    .clickable { viewModel.stopSleepMode() },
                contentAlignment = Alignment.Center
            ) {
                // Breathing glow animation in dark amoled screensaver
                val transition = rememberInfiniteTransition(label = "sleep_glow")
                val glowAlpha by transition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 0.7f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "glow_alpha"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Modo Sueño Activo",
                        tint = Color(0xFFFFCC00).copy(alpha = glowAlpha),
                        modifier = Modifier
                            .size(120.dp)
                            .padding(bottom = 24.dp)
                    )

                    var currentTimeText by remember { mutableStateOf("") }
                    LaunchedEffect(Unit) {
                        while (true) {
                            currentTimeText = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                            delay(1000)
                        }
                    }

                    Text(
                        text = currentTimeText,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Buenas noches... durmiendo 🌙",
                        color = Color.LightGray.copy(alpha = glowAlpha),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "El celular registra pantalla apagada hasta sonar.\nPresiona cualquier parte para salir.",
                        color = Color.DarkGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }
        // --- ACTIVE APPLICATION DASHBOARD ---
        else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp, bottom = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // HEADER SECTION
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Lumina Sleep",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "AI-ASSISTED WAKE",
                            fontSize = 10.sp,
                            color = MutedSlate,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }

                    // Go to sleep circular button (Modo Sueño)
                    IconButton(
                        onClick = { viewModel.startSleepMode() },
                        modifier = Modifier
                            .size(42.dp)
                            .background(DarkSlateAccent, CircleShape)
                            .border(1.dp, LightSlateAccent, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Activar Modo Sueño",
                            tint = Color(0xFFFFCC00),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Professional Polish Status Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dateFormatted = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES")).format(Date())
                    Text(
                        text = dateFormatted.replaceFirstChar { it.titlecase() },
                        fontSize = 12.sp,
                        color = MutedSlate,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF10B981), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI VOICE READY",
                            fontSize = 10.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // INSTANT TEST WAKE-UP CARD (Probar Despertador Simulación)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column {
                        // High-tech accent line at top
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFF6366F1))
                                    )
                                )
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    "Simulación Express ⚡",
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Prueba la alarma con la voz de IA del clima y noticias al instante.",
                                    color = MutedSlate,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.startRinging(-99, "Simulación rápida del despertador IA.", "Amanecer Zen")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, "Test", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Probar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ALARMS LIST COMPONENT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Mis Alarmas",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSlateAccent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, LightSlateAccent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Nueva Alarma",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Añadir", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (alarms.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(100.dp)
                            .background(SlateCard, RoundedCornerShape(24.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Sin alarmas guardadas.\nAñade una con el botón de arriba.",
                            color = MutedSlate,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    alarms.forEach { alarm ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val formattedTime = String.format("%02d:%02d", alarm.hour, alarm.minute)
                                    Text(
                                        text = formattedTime,
                                        color = Color.White,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = (-1).sp
                                    )
                                    val customLabel = if (alarm.customMessage.isNotEmpty()) {
                                        "💬 \"${alarm.customMessage}\""
                                    } else {
                                        "🤖 Voz IA Clima & Noticias"
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "🎵 ${alarm.soundName}",
                                            color = Color(0xFF10B981),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "•",
                                            color = MutedSlate.copy(alpha = 0.5f),
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = customLabel,
                                            color = MutedSlate,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = alarm.isEnabled,
                                        onCheckedChange = { viewModel.toggleAlarm(alarm, it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF10B981),
                                            uncheckedThumbColor = MutedSlate,
                                            uncheckedTrackColor = DarkSlateAccent,
                                            uncheckedBorderColor = Color.Transparent
                                        )
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(
                                        onClick = { viewModel.deleteAlarm(alarm) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar Alarma",
                                            tint = WarningCoral.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // SLEEP STATS SECTION (Calidad de sueño, semana y mes, gráficos)
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        "Análisis de Descanso",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. STATS KEY VALUE METRICS GRID
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Average sleep duration box
                        val avgHrsValue = remember(sessions) {
                            if (sessions.isEmpty()) 0.0
                            else {
                                val total = sessions.map { it.sleepDurationMillis }.sum()
                                total.toDouble() / (sessions.size * 1000 * 60 * 60)
                            }
                        }
                        val avgDuration = remember(avgHrsValue) {
                            String.format("%.1fh", avgHrsValue)
                        }
                        val durationFraction = remember(avgHrsValue) {
                            (avgHrsValue / 8.0).coerceIn(0.1, 1.0).toFloat()
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp)),
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "DURACIÓN MEDIA",
                                    color = MutedSlate,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = avgDuration,
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Capsule Progress indicator
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(DarkSlateAccent)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(durationFraction)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFF3B82F6))
                                    )
                                }
                            }
                        }

                        // Average alert turnoff latency (tiempo que tardaste en apagarlo)
                        val avgSecsValue = remember(sessions) {
                            if (sessions.isEmpty()) 0.0
                            else {
                                val total = sessions.map { it.dismissLatencyMillis }.sum()
                                total.toDouble() / (sessions.size * 1000)
                            }
                        }
                        val avgLatency = remember(avgSecsValue) {
                            if (avgSecsValue >= 60) {
                                val mins = (avgSecsValue / 60).toInt()
                                val secs = (avgSecsValue % 60).toInt()
                                "${mins}m ${secs}s"
                            } else {
                                String.format("%.0fs", avgSecsValue)
                            }
                        }
                        val latencyFraction = remember(avgSecsValue) {
                            (1.0 - (avgSecsValue / 120.0)).coerceIn(0.1, 1.0).toFloat()
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp)),
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "TIEMPO EN APAGAR",
                                    color = MutedSlate,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = avgLatency,
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Capsule Progress indicator
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(DarkSlateAccent)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(latencyFraction)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFF10B981))
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. WEEKLY GRAPH CANVAS CHART
                    WeeklySleepQualityChart(sessions = sessions)

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. MONTHLY ANALYSIS CARD
                    MonthlyOverallArcScore(sessions = sessions, onClearHistory = { viewModel.clearSessions() })
                }
            }
        }

        // --- ADD ALARM MODAL DIALOG ---
        if (showAddDialog) {
            Dialog(onDismissRequest = { showAddDialog = false }) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161618)),
                    border = BorderStroke(1.dp, Color(0xFF333336)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Nueva Alarma IA",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextField(
                                value = currentHour,
                                onValueChange = { if (it.length <= 2) currentHour = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                placeholder = { Text("08", color = Color.Gray) },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color.Black,
                                    unfocusedContainerColor = Color.Black,
                                    focusedIndicatorColor = Color(0xFF00FF87),
                                    unfocusedIndicatorColor = Color.DarkGray
                                ),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 26.sp),
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )

                            Text(
                                ":",
                                color = Color.White,
                                fontSize = 32.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            TextField(
                                value = currentMinute,
                                onValueChange = { if (it.length <= 2) currentMinute = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                placeholder = { Text("30", color = Color.Gray) },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color.Black,
                                    unfocusedContainerColor = Color.Black,
                                    focusedIndicatorColor = Color(0xFF00FF87),
                                    unfocusedIndicatorColor = Color.DarkGray
                                ),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 26.sp),
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom wakeup message details
                        TextField(
                            value = customTextPrompt,
                            onValueChange = { customTextPrompt = it },
                            placeholder = { Text("Ej: ¡Ánimo! Hoy tienes tu entrevista de trabajo, ¡tú puedes!", color = Color.Gray, fontSize = 12.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Black,
                                unfocusedContainerColor = Color.Black,
                                focusedIndicatorColor = Color(0xFF00FF87),
                                unfocusedIndicatorColor = Color.DarkGray
                            ),
                            maxLines = 3,
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(82.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            label = { Text("Mensaje personalizado matutino", color = Color.Gray, fontSize = 10.sp) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Selection of premium synthesized soundtracks
                        var soundDropdownExpanded by remember { mutableStateOf(false) }
                        val soundOptions = listOf("Bosque Sereno", "Amanecer Zen", "Pulso Clásico", "Tono del Sistema")

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Tono de Alarma premium",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black)
                                    .border(1.dp, Color.DarkGray, RoundedCornerShape(12.dp))
                                    .clickable { soundDropdownExpanded = true }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        soundAlarm,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Desplegar",
                                        tint = Color.White
                                    )
                                }

                                DropdownMenu(
                                    expanded = soundDropdownExpanded,
                                    onDismissRequest = { soundDropdownExpanded = false },
                                    modifier = Modifier
                                        .background(Color(0xFF161618))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                ) {
                                    soundOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    option,
                                                    color = if (option == soundAlarm) Color(0xFF10B981) else Color.White,
                                                    fontWeight = if (option == soundAlarm) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                soundAlarm = option
                                                soundDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showAddDialog = false },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFF333336)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancelar")
                            }

                            Button(
                                onClick = {
                                    val hr = currentHour.toIntOrNull() ?: 0
                                    val mn = currentMinute.toIntOrNull() ?: 0
                                    if (hr in 0..23 && mn in 0..59) {
                                        viewModel.addAlarm(hr, mn, customTextPrompt, soundAlarm)
                                        showAddDialog = false
                                        currentHour = ""
                                        currentMinute = ""
                                        customTextPrompt = ""
                                        soundAlarm = "Bosque Sereno"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00FF87),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Guardar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- COMPLETED SLEEP LOG EVENT POPUP CARD ---
        lastSessionSaved?.let { session ->
            Dialog(onDismissRequest = { viewModel.lastSessionSaved.value = null }) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161618)),
                    border = BorderStroke(1.dp, Color(0xFF333336)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Yoga Sleep Summary",
                            tint = Color(0xFF00FF87),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "¡Buenos Días!",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "Análisis de recuperación de anoche",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Central sleep score arc ring
                        Box(
                            modifier = Modifier.size(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    color = Color(0xFF262629),
                                    startAngle = -220f,
                                    sweepAngle = 260f,
                                    useCenter = false,
                                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        listOf(Color(0xFFFF5252), Color(0xFFFFCC00), Color(0xFF00FF87))
                                    ),
                                    startAngle = -220f,
                                    sweepAngle = (session.qualityScore / 100f) * 260f,
                                    useCenter = false,
                                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${session.qualityScore}%",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text("Puntaje", color = Color.Gray, fontSize = 9.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Score metrics text description
                        val explanation = when {
                            session.qualityScore >= 85 -> "¡Óptimo! Descansaste de manera excelente y te levantaste súper rápido al sonar. 🌟"
                            session.qualityScore >= 70 -> "¡Buen descanso! Mantuviste un balance saludable para tu rutina matutina. 👍"
                            session.qualityScore >= 50 -> "Suficiente, pero tardaste un poco en apagar la alarma. ¡Busca levantarte de inmediato! ⚠️"
                            else -> "Sueño ligero o retraso severo en apagar el despertador. ¡Procura acostarte más temprano! 😴"
                        }

                        Text(
                            text = explanation,
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Divider(color = Color(0xFF262629), thickness = 1.dp)

                        Spacer(modifier = Modifier.height(14.dp))

                        val durationHrs = session.sleepDurationMillis.toDouble() / (1000 * 60 * 60)
                        val elapsedLatencyMins = session.dismissLatencyMillis.toDouble() / (1000 * 60)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Duración", color = Color.Gray, fontSize = 11.sp)
                                Text(String.format("%.1fh", durationHrs), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Tiempo en Apagar", color = Color.Gray, fontSize = 11.sp)
                                Text(
                                    if (elapsedLatencyMins >= 1) String.format("%.1fm", elapsedLatencyMins) else "${session.dismissLatencyMillis / 1000}s",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.lastSessionSaved.value = null },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00FF87),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Aceptar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// WEEKLY STYLISH SLEEP QUALITY BAR GRAPH DRAWING
@Composable
fun WeeklySleepQualityChart(sessions: List<SleepSession>) {
    val data = remember(sessions) {
        sessions.take(7).reversed()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Consistencia Semanal (Últimos 7 días)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Calidad %",
                    fontSize = 11.sp,
                    color = MutedSlate
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No hay noches registradas todavía.\nTus reportes se graficarán aquí diariamente.",
                        color = MutedSlate,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    
                    val marginHorizontal = 10.dp.toPx()
                    val graphHeight = height - 24.dp.toPx()
                    val totalBars = data.size
                    
                    val barSpacing = 22.dp.toPx()
                    val barWidth = ((width - (marginHorizontal * 2) - (barSpacing * (totalBars - 1))) / totalBars).coerceAtLeast(14.dp.toPx())

                    data.forEachIndexed { index, session ->
                        val score = session.qualityScore
                        val barHeight = graphHeight * (score / 100f)
                        val x = marginHorizontal + index * (barWidth + barSpacing)
                        val y = graphHeight - barHeight

                        // Dynamic colors for recovery levels
                        val gradientBrush = Brush.verticalGradient(
                            colors = when {
                                score >= 85 -> listOf(Color(0xFF10B981), Color(0xFF047857))
                                score >= 70 -> listOf(Color(0xFFFFCC00), Color(0xFFFFB300))
                                else -> listOf(Color(0xFFEF4444), Color(0xFFB91C1C))
                            }
                        )

                        // Draw background track for bar
                        drawRoundRect(
                            color = DarkSlateAccent,
                            topLeft = Offset(x, 0f),
                            size = androidx.compose.ui.geometry.Size(barWidth, graphHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )

                        // Draw fill progress
                        drawRoundRect(
                            brush = gradientBrush,
                            topLeft = Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Axis descriptors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    data.forEach { session ->
                        val parts = session.dateLabel.split(" ")
                        val shortDayName = parts.getOrNull(0) ?: ""
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(36.dp)
                        ) {
                            Text(
                                text = shortDayName.replaceFirstChar { it.titlecase() }.take(3),
                                fontSize = 10.sp,
                                color = MutedSlate,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${session.qualityScore}%",
                                fontSize = 9.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// MONTHS OVERALL RECOVERY SCORE RING & CONFIG
@Composable
fun MonthlyOverallArcScore(sessions: List<SleepSession>, onClearHistory: () -> Unit) {
    val overallMonthScore = remember(sessions) {
        if (sessions.isEmpty()) 0
        else (sessions.map { it.qualityScore }.average()).toInt()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Análisis Mensual Estimado",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Glow arc
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = DarkSlateAccent,
                            startAngle = -220f,
                            sweepAngle = 260f,
                            useCenter = false,
                            style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF10B981), Color(0xFF3B82F6))
                            ),
                            startAngle = -220f,
                            sweepAngle = (overallMonthScore / 100f) * 260f,
                            useCenter = false,
                            style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$overallMonthScore%",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text("Mensual", color = MutedSlate, fontSize = 9.sp)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val count = sessions.size
                    Text(
                        "$count noches evaluadas",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val desc = if (count == 0) {
                        "Duerme con tu celular bloqueado al lado tuyo para iniciar mediciones continuas de salud."
                    } else if (overallMonthScore >= 80) {
                        "¡Nivel de Alerta Matutina Óptimo! Eres extremadamente consistente levantándote rápido."
                    } else if (overallMonthScore >= 65) {
                        "Buen rendimiento general mensual. Procura acostarte media hora antes para ganar duración."
                    } else {
                        "Estás teniendo retrasos severos en apagar la alarma del despertador. ¡Evita el botón snooze!"
                    }
                    Text(
                        text = desc,
                        color = MutedSlate,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            if (sessions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Limpiar Historial",
                        color = WarningCoral.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onClearHistory() }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}
