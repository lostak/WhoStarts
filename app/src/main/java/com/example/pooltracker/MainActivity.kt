package com.example.pooltracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
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
                        Text("WhoStarts", fontWeight = FontWeight.Bold)
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
            knownPlayers = viewModel.knownPlayers,
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

                Spacer(modifier = Modifier.height(12.dp))

                WinnerTile(
                    teamAName = matchup.teamAName(),
                    teamBName = matchup.teamBName(),
                    lastWinner = lastWinner,
                    onSelectWinner = onToggle
                )

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

/**
 * A large, tappable "who won" tile. Tapping the left half always records a win
 * for Team A, tapping the right half always records a win for Team B — so the
 * same team can win repeatedly without the control needing to "toggle" first.
 * A colored indicator animates to whichever side most recently won.
 */
@Composable
fun WinnerTile(
    teamAName: String,
    teamBName: String,
    lastWinner: Int?,
    onSelectWinner: (Int) -> Unit
) {
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (lastWinner == null) 0f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "indicatorAlpha"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val halfWidth = maxWidth / 2
        val indicatorOffset by animateDpAsState(
            targetValue = if (lastWinner == 2) halfWidth else 0.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "indicatorOffset"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(FeltGreenDark)
        ) {
            // Sliding highlight showing the current leader.
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(halfWidth)
                    .fillMaxHeight()
                    .alpha(indicatorAlpha)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            if (lastWinner == 2) listOf(Brass, BrassLight) else listOf(FeltGreenLight, FeltGreen)
                        )
                    )
            )

            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelectWinner(1) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = teamAName,
                        fontWeight = if (lastWinner == 1) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = if (lastWinner == 1) Color(0xFF07160F) else CueCream,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight(0.5f)
                        .align(Alignment.CenterVertically)
                        .background(OnSurfaceMuted.copy(alpha = 0.25f))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelectWinner(2) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = teamBName,
                        fontWeight = if (lastWinner == 2) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = if (lastWinner == 2) Color(0xFF241A00) else CueCream,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddMatchupDialog(
    knownPlayers: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (Team, Team) -> Unit
) {
    var teamAName by remember { mutableStateOf("") }
    var teamBName by remember { mutableStateOf("") }
    val teamAPlayers = remember { mutableStateListOf<String>() }
    val teamBPlayers = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceVariant,
        titleContentColor = CueCream,
        textContentColor = CueCream,
        title = { Text("New matchup") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                TeamInputSection(
                    label = "Team A",
                    teamName = teamAName,
                    onTeamNameChange = { teamAName = it },
                    players = teamAPlayers,
                    knownPlayers = knownPlayers
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text("vs", color = OnSurfaceMuted, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(14.dp))

                TeamInputSection(
                    label = "Team B",
                    teamName = teamBName,
                    onTeamNameChange = { teamBName = it },
                    players = teamBPlayers,
                    knownPlayers = knownPlayers
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val teamA = Team(
                        name = teamAName.trim().ifBlank { null },
                        players = teamAPlayers.map { it.trim() }.filter { it.isNotEmpty() }
                    )
                    val teamB = Team(
                        name = teamBName.trim().ifBlank { null },
                        players = teamBPlayers.map { it.trim() }.filter { it.isNotEmpty() }
                    )
                    val validA = teamA.name != null || teamA.players.isNotEmpty()
                    val validB = teamB.name != null || teamB.players.isNotEmpty()
                    if (validA && validB) {
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

/**
 * Input block for one side of a matchup: an optional team name, plus zero or
 * more player name fields that can be added one at a time.
 */
@Composable
fun TeamInputSection(
    label: String,
    teamName: String,
    onTeamNameChange: (String) -> Unit,
    players: SnapshotStateList<String>,
    knownPlayers: List<String>
) {
    Column {
        Text(label, color = Brass, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = teamName,
            onValueChange = onTeamNameChange,
            label = { Text("Team name (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Brass,
                cursorColor = Brass
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        players.forEachIndexed { index, playerName ->
            PlayerInputRow(
                value = playerName,
                onValueChange = { players[index] = it },
                onRemove = { players.removeAt(index) },
                knownPlayers = knownPlayers,
                alreadyChosen = players.filterIndexed { i, _ -> i != index }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        TextButton(onClick = { players.add("") }) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = ChalkBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add player", color = ChalkBlue)
        }
    }
}

/** A single player name field with a remove button and inline autocomplete chips. */
@Composable
fun PlayerInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit,
    knownPlayers: List<String>,
    alreadyChosen: List<String>
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("Player name") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Brass,
                    cursorColor = Brass
                )
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove player", tint = OnSurfaceMuted)
            }
        }

        val suggestions = remember(value, knownPlayers, alreadyChosen) {
            if (value.isBlank()) {
                emptyList()
            } else {
                knownPlayers.filter { candidate ->
                    candidate.contains(value, ignoreCase = true) &&
                        !candidate.equals(value, ignoreCase = true) &&
                        alreadyChosen.none { it.equals(candidate, ignoreCase = true) }
                }.take(5)
            }
        }

        if (suggestions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 4.dp, start = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                suggestions.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { onValueChange(suggestion) },
                        label = { Text(suggestion, fontSize = 12.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = FeltGreenDark,
                            labelColor = CueCream
                        ),
                        border = null
                    )
                }
            }
        }
    }
}


/**
 * Line chart of running win-differential (Team A wins minus Team B wins) over
 * the course of the matchup's history. Rises when A is on top, dips when B is.
 */
@Composable
fun MomentumLineChart(history: List<GameResult>, modifier: Modifier = Modifier) {
    val diffs = remember(history) {
        var running = 0
        history.map { r -> running += if (r.winnerTeam == 1) 1 else -1; running }
    }
    val maxAbs = (diffs.maxOfOrNull { abs(it) } ?: 1).coerceAtLeast(1)
    val lineColor = Brass
    val zeroLineColor = OnSurfaceMuted

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val topPad = 10f
        val usableH = h - topPad * 2
        val midY = h / 2f
        val stepX = if (diffs.size > 1) w / (diffs.size - 1) else w

        // Zero line (parity between teams)
        drawLine(
            color = zeroLineColor.copy(alpha = 0.25f),
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 1.dp.toPx()
        )

        val points = diffs.mapIndexed { i, d ->
            Offset(i * stepX, midY - (d.toFloat() / maxAbs) * (usableH / 2f))
        }

        val path = Path().apply {
            points.forEachIndexed { i, p ->
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        points.forEachIndexed { i, p ->
            val isA = history[i].winnerTeam == 1
            drawCircle(
                color = if (isA) FeltGreenLight else Brass,
                radius = 4.5.dp.toPx(),
                center = p
            )
        }
    }
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

                    if (matchup.history.size >= 2) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Momentum", style = MaterialTheme.typography.labelMedium, color = OnSurfaceMuted)
                            Row {
                                Text(matchup.teamAName(), color = FeltGreenLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("  ·  ", color = OnSurfaceMuted, fontSize = 11.sp)
                                Text(matchup.teamBName(), color = Brass, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        MomentumLineChart(
                            history = matchup.history,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
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
