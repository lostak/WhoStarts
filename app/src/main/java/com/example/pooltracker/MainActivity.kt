package com.example.pooltracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PoolTrackerTheme {
                PoolTrackerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoolTrackerApp(viewModel: PoolViewModel = viewModel()) {
    var showAddDialog by remember { mutableStateOf(false) }
    var historyTarget by remember { mutableStateOf<Matchup?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EightBallMark(size = 28.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Pool Tracker", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FeltGreenDark,
                    titleContentColor = CueCream
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Brass,
                contentColor = Color(0xFF241A00)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add matchup")
            }
        }
    ) { padding ->
        if (viewModel.matchups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EightBallMark(size = 56.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No matchups yet",
                        color = OnSurfaceMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Tap + to rack one up", color = OnSurfaceMuted, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(viewModel.matchups, key = { it.id }) { matchup ->
                    MatchupRow(
                        matchup = matchup,
                        onToggle = { team -> viewModel.recordResult(matchup, team) },
                        onHistoryClick = { historyTarget = matchup },
                        onDelete = { viewModel.removeMatchup(matchup) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddMatchupDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { teamA, teamB ->
                viewModel.addMatchup(teamA, teamB)
                showAddDialog = false
            }
        )
    }

    // Keep the dialog synced to the live matchup object as new results come in.
    val liveHistoryTarget = historyTarget?.let { target ->
        viewModel.matchups.find { it.id == target.id }
    }
    liveHistoryTarget?.let { matchup ->
        HistoryDialog(matchup = matchup, onDismiss = { historyTarget = null })
    }
}

/** A small stylized eight-ball mark used as the in-app logo. */
@Composable
fun EightBallMark(size: androidx.compose.ui.unit.Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(color = Color(0xFF1A1A1A), radius = r, center = center)
        drawCircle(
            color = Color(0x33FFFFFF),
            radius = r * 0.28f,
            center = Offset(center.x - r * 0.32f, center.y - r * 0.35f)
        )
        drawCircle(color = CueCream, radius = r * 0.46f, center = center)
        drawCircle(
            color = Color(0xFF1A1A1A),
            radius = r * 0.18f,
            center = Offset(center.x, center.y - r * 0.18f)
        )
        drawCircle(
            color = Color(0xFF1A1A1A),
            radius = r * 0.22f,
            center = Offset(center.x, center.y + r * 0.2f)
        )
    }
}

@Composable
fun MatchupRow(
    matchup: Matchup,
    onToggle: (Int) -> Unit,
    onHistoryClick: () -> Unit,
    onDelete: () -> Unit
) {
    val lastWinner = matchup.lastWinner
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(FeltGreenDark, SurfaceVariant, SurfaceVariant))
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${matchup.teamAName()}  vs  ${matchup.teamBName()}",
                        fontWeight = FontWeight.Bold,
                        color = CueCream,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete matchup",
                            tint = OnSurfaceMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = matchup.teamAName(),
                        color = if (lastWinner == 1) Brass else OnSurfaceMuted,
                        fontWeight = if (lastWinner == 1) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )

                    // Switch: unchecked = Team A won last, checked = Team B won last
                    Switch(
                        checked = lastWinner == 2,
                        onCheckedChange = { checkedForB ->
                            onToggle(if (checkedForB) 2 else 1)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Brass,
                            checkedTrackColor = FeltGreenLight,
                            uncheckedThumbColor = CueCream,
                            uncheckedTrackColor = FeltGreenDark,
                            uncheckedBorderColor = FeltGreenLight
                        )
                    )

                    Text(
                        text = matchup.teamBName(),
                        color = if (lastWinner == 2) Brass else OnSurfaceMuted,
                        fontWeight = if (lastWinner == 2) FontWeight.Bold else FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val label = when (lastWinner) {
                        1 -> "Last winner: ${matchup.teamAName()}"
                        2 -> "Last winner: ${matchup.teamBName()}"
                        else -> "No games recorded yet"
                    }
                    Text(text = label, style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)

                    TextButton(onClick = onHistoryClick) {
                        Text("History (${matchup.history.size})", color = ChalkBlue)
                    }
                }
            }
        }
    }
}

@Composable
fun AddMatchupDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<String>, List<String>) -> Unit
) {
    var teamAText by remember { mutableStateOf("") }
    var teamBText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceVariant,
        titleContentColor = CueCream,
        textContentColor = CueCream,
        title = { Text("New matchup") },
        text = {
            Column {
                Text("Enter players for each team, separated by commas.", color = OnSurfaceMuted)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = teamAText,
                    onValueChange = { teamAText = it },
                    label = { Text("Team A (e.g. Alice, Carl)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Brass,
                        cursorColor = Brass
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("vs", color = OnSurfaceMuted, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = teamBText,
                    onValueChange = { teamBText = it },
                    label = { Text("Team B (e.g. Bob, Dana)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Brass,
                        cursorColor = Brass
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val teamA = teamAText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val teamB = teamBText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (teamA.isNotEmpty() && teamB.isNotEmpty()) {
                        onConfirm(teamA, teamB)
                    }
                }
            ) { Text("Add", color = Brass, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = OnSurfaceMuted) }
        }
    )
}

@Composable
fun HistoryDialog(matchup: Matchup, onDismiss: () -> Unit) {
    val formatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val winsA = matchup.winsFor(1)
    val winsB = matchup.winsFor(2)
    val total = max(winsA + winsB, 1)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceVariant,
        titleContentColor = CueCream,
        textContentColor = CueCream,
        title = { Text("${matchup.teamAName()} vs ${matchup.teamBName()}") },
        text = {
            Column {
                if (matchup.history.isEmpty()) {
                    Text("No games recorded yet.", color = OnSurfaceMuted)
                } else {
                    Text(
                        "${matchup.teamAName()}  $winsA — $winsB  ${matchup.teamBName()}",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Win-ratio bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(winsA.toFloat().coerceAtLeast(0.001f))
                                .fillMaxHeight()
                                .background(FeltGreenLight)
                        )
                        Box(
                            modifier = Modifier
                                .weight(winsB.toFloat().coerceAtLeast(0.001f))
                                .fillMaxHeight()
                                .background(Brass)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "${((winsA.toFloat() / total) * 100).toInt()}%",
                            color = FeltGreenLight,
                            fontSize = 12.sp
                        )
                        Text(
                            "${((winsB.toFloat() / total) * 100).toInt()}%",
                            color = Brass,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Recent form", style = MaterialTheme.typography.labelMedium, color = OnSurfaceMuted)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Form strip: last up to 10 results, oldest -> newest
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        matchup.history.takeLast(10).forEach { result ->
                            val isA = result.winnerTeam == 1
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(if (isA) FeltGreenLight else Brass),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isA) "A" else "B",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isA) CueCream else Color(0xFF241A00)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Full history", style = MaterialTheme.typography.labelMedium, color = OnSurfaceMuted)
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                        items(matchup.history.reversed()) { result ->
                            val winnerName = if (result.winnerTeam == 1) matchup.teamAName() else matchup.teamBName()
                            val dotColor = if (result.winnerTeam == 1) FeltGreenLight else Brass
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "${formatter.format(Date(result.timestamp))} — $winnerName won",
                                    fontSize = 13.sp,
                                    color = CueCream
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Brass, fontWeight = FontWeight.Bold) }
        }
    )
}
