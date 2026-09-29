package com.example.pooltracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

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

/** Largest number of teams a single matchup may hold. */
const val MAX_TEAMS = 8

/** Preset palette offered when picking a team color. */
val TeamColorChoices = listOf(
    Color(0xFF1C5A3D), // felt green
    Color(0xFFC9A24B), // brass
    Color(0xFF4E7C8C), // chalk blue
    Color(0xFFB3452F), // clay red
    Color(0xFF6C4E9C), // purple
    Color(0xFFCF7A2E), // orange
    Color(0xFF2F7FB3), // bright blue
    Color(0xFFAE3B62), // magenta
    Color(0xFF3F8F55), // leaf green
    Color(0xFF8C8C8C)  // grey
)

/** Fallback color for the team at [index] (0-based) when none was chosen. */
fun defaultTeamColor(index: Int): Color = TeamColorChoices[index % TeamColorChoices.size]

/** Resolved colors for every team in a matchup, in order. */
fun Matchup.teamColors(): List<Color> =
    teams.mapIndexed { i, t -> t.color(defaultTeamColor(i)) }

/** Picks readable dark or cream text depending on how light [bg] is. */
fun contrastingTextColor(bg: Color): Color =
    if (bg.luminance() > 0.5f) Color(0xFF15231C) else CueCream

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
                    Text("No matchups yet", color = OnSurfaceMuted, fontWeight = FontWeight.SemiBold)
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
                        knownPlayers = viewModel.knownPlayers,
                        onToggle = { team -> viewModel.recordResult(matchup, team) },
                        onHistoryClick = { historyTarget = matchup },
                        onDelete = { viewModel.removeMatchup(matchup) },
                        onUpdateTeams = { teams, solidsTeam ->
                            viewModel.updateTeams(matchup.id, teams, solidsTeam)
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddMatchupDialog(
            knownPlayers = viewModel.knownPlayers,
            onDismiss = { showAddDialog = false },
            onConfirm = { teams ->
                viewModel.addMatchup(teams)
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
fun EightBallMark(size: Dp) {
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
        drawCircle(color = Color(0xFF1A1A1A), radius = r * 0.18f, center = Offset(center.x, center.y - r * 0.18f))
        drawCircle(color = Color(0xFF1A1A1A), radius = r * 0.22f, center = Offset(center.x, center.y + r * 0.2f))
    }
}

/**
 * Describes a confirmed win handed to [WinnerTile] so it knows to run its
 * celebration. [seq] increments on each confirmation so repeat wins by the
 * same team still retrigger the animation.
 */
data class ConfirmRequest(
    val side: Int = 1,
    val tapOffset: Offset = Offset.Zero,
    val tileCoords: LayoutCoordinates? = null,
    val seq: Int = 0
)

@Composable
fun MatchupRow(
    matchup: Matchup,
    knownPlayers: List<String>,
    onToggle: (Int) -> Unit,
    onHistoryClick: () -> Unit,
    onDelete: () -> Unit,
    onUpdateTeams: (List<Team>, Int?) -> Unit
) {
    val lastWinner = matchup.lastWinner
    val colors = matchup.teamColors()
    var showTiebreaker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    var burstSide by remember { mutableStateOf(1) }
    var burstTrigger by remember { mutableStateOf(0) }
    var burstOrigin by remember { mutableStateOf<Offset?>(null) }
    var cardCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    // Staged (tapped but not yet confirmed) winner, plus where the tap landed so
    // the celebration can still originate from the finger once confirmed.
    var pendingWinner by remember { mutableStateOf<Int?>(null) }
    var pendingTapOffset by remember { mutableStateOf(Offset.Zero) }
    var pendingTileCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var confirmTrigger by remember { mutableStateOf(ConfirmRequest()) }

    LaunchedEffect(matchup.history.size) { pendingWinner = null }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { cardCoords = it }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(FeltGreenDark, SurfaceVariant, SurfaceVariant)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = matchup.teams.joinToString("  vs  ") { it.displayName() },
                                fontWeight = FontWeight.Bold,
                                color = CueCream
                            )
                            if (!matchup.isHeadToHead) {
                                Text(
                                    text = "${matchup.teamCount}-way",
                                    fontSize = 11.sp,
                                    color = OnSurfaceMuted
                                )
                            }
                        }
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Matchup settings", tint = OnSurfaceMuted)
                        }
                    }

                    // Solids/stripes only makes sense head-to-head.
                    if (matchup.isHeadToHead) {
                        Spacer(modifier = Modifier.height(8.dp))
                        BallAssignmentRow(
                            teamAName = matchup.teamName(1),
                            teamBName = matchup.teamName(2),
                            solidsTeam = matchup.solidsTeam,
                            colorA = colors[0],
                            colorB = colors[1],
                            onAssign = { newSolids -> onUpdateTeams(matchup.teams, newSolids) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    WinnerTile(
                        teamNames = matchup.teams.map { it.displayName() },
                        colors = colors,
                        lastWinner = lastWinner,
                        pendingWinner = pendingWinner,
                        confirmRequest = confirmTrigger,
                        onStageWinner = { side, tapOffset, tileCoords ->
                            pendingWinner = if (pendingWinner == side) null else side
                            pendingTapOffset = tapOffset
                            pendingTileCoords = tileCoords
                        },
                        onConfirmedBurst = { side, tapOffset, tileCoords ->
                            burstSide = side
                            // Translate the tap from the tile's coordinate space into
                            // the card's, so the explosion originates exactly where
                            // the finger landed even though it renders card-wide.
                            burstOrigin = if (tileCoords != null && cardCoords != null) {
                                cardCoords!!.localPositionOf(tileCoords, tapOffset)
                            } else null
                            burstTrigger++
                        }
                    )

                    // Confirmation step: a deliberate second action guards against
                    // an accidental tap being saved as a game result.
                    AnimatedVisibility(visible = pendingWinner != null) {
                        val stagedSide = pendingWinner ?: 1
                        val stagedName = matchup.teamName(stagedSide)
                        val stagedColor = colors.getOrElse(stagedSide - 1) { Brass }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { pendingWinner = null },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel", color = OnSurfaceMuted)
                                }
                                Button(
                                    onClick = {
                                        val side = stagedSide
                                        val off = pendingTapOffset
                                        val coords = pendingTileCoords
                                        pendingWinner = null
                                        confirmTrigger = ConfirmRequest(side, off, coords, confirmTrigger.seq + 1)
                                        onToggle(side)
                                    },
                                    modifier = Modifier.weight(2f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = stagedColor,
                                        contentColor = contrastingTextColor(stagedColor)
                                    )
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("$stagedName won", fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                    }

                    if (lastWinner == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { showTiebreaker = true },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                if (matchup.isHeadToHead) "🪙 Flip a coin to decide who breaks"
                                else "🎯 Spin to decide who breaks",
                                color = ChalkBlue,
                                fontWeight = FontWeight.SemiBold
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
                            text = if (lastWinner != null) "Last winner: ${matchup.teamName(lastWinner)}"
                            else "No games recorded yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceMuted
                        )
                        TextButton(onClick = onHistoryClick) {
                            Text("History (${matchup.history.size})", color = ChalkBlue)
                        }
                    }
                }
            }

            // Explosion overlay spans the entire card so the burst has room to breathe.
            BurstEffect(
                trigger = burstTrigger,
                color = colors.getOrElse(burstSide - 1) { Brass },
                originPx = burstOrigin,
                originXFraction = (burstSide - 0.5f) / matchup.teamCount,
                originYFraction = 0.5f,
                particleCount = 22,
                maxRadiusDp = 110.dp,
                modifier = Modifier.matchParentSize()
            )
        }
    }

    if (showTiebreaker) {
        TiebreakerDialog(
            teamNames = matchup.teams.map { it.displayName() },
            colors = colors,
            onDismiss = { showTiebreaker = false }
        )
    }

    if (showSettings) {
        MatchupSettingsDialog(
            matchup = matchup,
            knownPlayers = knownPlayers,
            onSave = { teams, solidsTeam ->
                onUpdateTeams(teams, solidsTeam)
                showSettings = false
            },
            onDelete = {
                onDelete()
                showSettings = false
            },
            onDismiss = { showSettings = false }
        )
    }
}

/**
 * A segmented "who won" control with one tappable segment per team. Tapping a
 * segment *stages* that team (outlined in their color) but records nothing —
 * the caller shows a confirm button, and only on confirmation does the win
 * animation play and the result save. This guards against accidental taps.
 */
@Composable
fun WinnerTile(
    teamNames: List<String>,
    colors: List<Color>,
    lastWinner: Int?,
    pendingWinner: Int?,
    confirmRequest: ConfirmRequest,
    onStageWinner: (side: Int, tapOffset: Offset, tileCoords: LayoutCoordinates?) -> Unit,
    onConfirmedBurst: (side: Int, tapOffset: Offset, tileCoords: LayoutCoordinates?) -> Unit
) {
    val count = teamNames.size.coerceAtLeast(2)
    // Position is measured in segment indices (0-based) so the puck can slide
    // between any pair of teams, not just two.
    val posIndex = remember { Animatable(((lastWinner ?: 1) - 1).toFloat()) }
    val pulseScale = remember { Animatable(1f) }
    var tileCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val indicatorAlpha by animateFloatAsState(
        targetValue = if (lastWinner == null) 0f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "indicatorAlpha"
    )

    LaunchedEffect(confirmRequest.seq) {
        if (confirmRequest.seq == 0) return@LaunchedEffect
        val target = (confirmRequest.side - 1).toFloat()
        val alreadyThere = abs(posIndex.value - target) < 0.01f
        if (alreadyThere) {
            pulseScale.animateTo(1.18f, tween(130, easing = FastOutSlowInEasing))
            onConfirmedBurst(confirmRequest.side, confirmRequest.tapOffset, confirmRequest.tileCoords ?: tileCoords)
            pulseScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        } else {
            launch {
                delay(110)
                onConfirmedBurst(confirmRequest.side, confirmRequest.tapOffset, confirmRequest.tileCoords ?: tileCoords)
            }
            posIndex.animateTo(
                target,
                animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow)
            )
        }
    }

    val tileHeight = if (count <= 3) 72.dp else 88.dp
    val nameSize = when {
        count <= 2 -> 17.sp
        count == 3 -> 15.sp
        count == 4 -> 13.sp
        else -> 11.sp
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(tileHeight)
            .onGloballyPositioned { tileCoords = it }
    ) {
        val segWidth = maxWidth / count
        // Blend toward whichever team the puck is currently nearest.
        val lowIdx = posIndex.value.toInt().coerceIn(0, count - 1)
        val highIdx = (lowIdx + 1).coerceAtMost(count - 1)
        val tint = lerp(
            colors.getOrElse(lowIdx) { Brass },
            colors.getOrElse(highIdx) { Brass },
            posIndex.value - lowIdx
        )
        val gradientCenter = ((posIndex.value + 0.5f) / count).coerceIn(0.06f, 0.94f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(FeltGreenDark)
        ) {
            // Gradient fade that always sweeps toward the current leader.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(indicatorAlpha)
                    .background(
                        Brush.horizontalGradient(
                            0f to FeltGreenDark,
                            gradientCenter to tint.copy(alpha = 0.6f),
                            1f to FeltGreenDark
                        )
                    )
            )

            // Solid puck marking exactly who is winning right now.
            Box(
                modifier = Modifier
                    .offset(x = segWidth * posIndex.value)
                    .width(segWidth)
                    .fillMaxHeight()
                    .alpha(indicatorAlpha)
                    .graphicsLayer {
                        scaleX = pulseScale.value
                        scaleY = pulseScale.value
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.getOrElse(posIndex.value.roundToInt().coerceIn(0, count - 1)) { Brass })
            )

            Row(modifier = Modifier.fillMaxSize()) {
                teamNames.forEachIndexed { i, name ->
                    if (i > 0) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight(0.5f)
                                .align(Alignment.CenterVertically)
                                .background(OnSurfaceMuted.copy(alpha = 0.25f))
                        )
                    }
                    TeamSegment(
                        name = name,
                        isWinner = lastWinner == i + 1,
                        isPending = pendingWinner == i + 1,
                        winnerTextColor = contrastingTextColor(colors.getOrElse(i) { Brass }),
                        pendingColor = colors.getOrElse(i) { Brass },
                        nameSize = nameSize,
                        modifier = Modifier.weight(1f),
                        onTap = { offset, segWidthPx ->
                            // Shift the segment-local tap into full-tile space.
                            onStageWinner(i + 1, Offset(offset.x + i * segWidthPx, offset.y), tileCoords)
                        }
                    )
                }
            }
        }
    }
}

/** One tappable segment of the winner tile. */
@Composable
fun TeamSegment(
    name: String,
    isWinner: Boolean,
    isPending: Boolean,
    winnerTextColor: Color,
    pendingColor: Color,
    nameSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier,
    onTap: (Offset, Float) -> Unit
) {
    val pendingBorderAlpha by animateFloatAsState(
        targetValue = if (isPending) 1f else 0f,
        animationSpec = tween(180),
        label = "pendingBorder"
    )
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(4.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 2.dp,
                color = pendingColor.copy(alpha = pendingBorderAlpha),
                shape = RoundedCornerShape(16.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures { offset -> onTap(offset, size.width.toFloat()) }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            fontWeight = if (isWinner || isPending) FontWeight.ExtraBold else FontWeight.SemiBold,
            fontSize = nameSize,
            color = if (isWinner) winnerTextColor else CueCream,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

/**
 * A radial particle burst used to punctuate a win. Increment [trigger] to fire
 * it again (even from the same spot) since the value itself, not just its
 * identity, needs to change to relaunch the effect.
 */
@Composable
fun BurstEffect(
    trigger: Int,
    color: Color,
    modifier: Modifier = Modifier,
    originPx: Offset? = null,
    originXFraction: Float = 0.5f,
    originYFraction: Float = 0.5f,
    particleCount: Int = 14,
    maxRadiusDp: Dp = 46.dp
) {
    val progress = remember { Animatable(1f) }
    val angles = remember(trigger) { List(particleCount) { Random.nextDouble(0.0, 2 * PI) } }
    val distances = remember(trigger) { List(particleCount) { 0.55f + Random.nextFloat() * 0.45f } }

    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(600, easing = LinearOutSlowInEasing))
    }

    if (progress.value < 1f) {
        Canvas(modifier = modifier) {
            val cx = originPx?.x ?: (size.width * originXFraction)
            val cy = originPx?.y ?: (size.height * originYFraction)
            val maxR = maxRadiusDp.toPx()
            angles.forEachIndexed { i, ang ->
                val dist = distances[i] * maxR * progress.value
                val x = cx + cos(ang).toFloat() * dist
                val y = cy + sin(ang).toFloat() * dist
                val particleAlpha = 1f - progress.value
                val r = 4.dp.toPx() * (1f - progress.value * 0.4f)
                drawCircle(color = color.copy(alpha = particleAlpha), radius = r, center = Offset(x, y))
            }
        }
    }
}

/**
 * Inline, tappable solids/stripes assignment shown directly on the matchup tile.
 * Tap a team to give them solids (the other side automatically gets stripes);
 * tap the team that already has solids to clear the assignment entirely.
 */
@Composable
fun BallAssignmentRow(
    teamAName: String,
    teamBName: String,
    solidsTeam: Int?,
    colorA: Color,
    colorB: Color,
    onAssign: (Int?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BallChip(
            striped = solidsTeam == 2,
            assigned = solidsTeam != null,
            accent = colorA,
            modifier = Modifier.weight(1f),
            onClick = { onAssign(if (solidsTeam == 1) null else 1) }
        )
        BallChip(
            striped = solidsTeam == 1,
            assigned = solidsTeam != null,
            accent = colorB,
            modifier = Modifier.weight(1f),
            onClick = { onAssign(if (solidsTeam == 2) null else 2) }
        )
    }
}

/** A tappable chip showing a team's ball group (or an invitation to set one). */
@Composable
fun BallChip(
    striped: Boolean,
    assigned: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (assigned) accent.copy(alpha = 0.16f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (assigned) accent.copy(alpha = 0.55f) else OnSurfaceMuted.copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BallGlyph(striped = striped, dimmed = !assigned, accent = accent)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (assigned) (if (striped) "stripes" else "solids") else "set balls",
            fontSize = 11.sp,
            fontWeight = if (assigned) FontWeight.SemiBold else FontWeight.Normal,
            color = if (assigned) CueCream else OnSurfaceMuted,
            maxLines = 1
        )
    }
}

/** A tiny pool-ball glyph: solid, or with a stripe band across the middle. */
@Composable
fun BallGlyph(striped: Boolean, dimmed: Boolean, accent: Color) {
    val ballColor = if (dimmed) OnSurfaceMuted.copy(alpha = 0.45f) else accent
    Canvas(modifier = Modifier.size(14.dp)) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        if (striped) {
            drawCircle(color = CueCream, radius = r, center = c)
            clipPath(Path().apply { addOval(Rect(center = c, radius = r)) }) {
                drawRect(color = ballColor, topLeft = Offset(0f, c.y - r * 0.5f), size = Size(size.width, r))
            }
        } else {
            drawCircle(color = ballColor, radius = r, center = c)
            drawCircle(color = CueCream, radius = r * 0.38f, center = c)
        }
    }
}

/**
 * Picks who breaks. Two teams get the coin flip; three or more get a spinning
 * wheel. Either way this is only a tiebreaker aid — nothing is recorded to the
 * matchup's history, and the result stays up until dismissed.
 */
@Composable
fun TiebreakerDialog(
    teamNames: List<String>,
    colors: List<Color>,
    onDismiss: () -> Unit
) {
    if (teamNames.size == 2) {
        CoinFlipDialog(
            teamAName = teamNames[0],
            teamBName = teamNames[1],
            colorA = colors.getOrElse(0) { FeltGreenLight },
            colorB = colors.getOrElse(1) { Brass },
            onDismiss = onDismiss
        )
    } else {
        WheelSpinDialog(teamNames = teamNames, colors = colors, onDismiss = onDismiss)
    }
}

/** A pseudo-3D coin flip for head-to-head matchups. */
@Composable
fun CoinFlipDialog(
    teamAName: String,
    teamBName: String,
    colorA: Color,
    colorB: Color,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val angle = remember { Animatable(0f) }
    val lift = remember { Animatable(0f) }
    var isFlipping by remember { mutableStateOf(false) }
    var resultTeam by remember { mutableStateOf<Int?>(null) }
    var burstTrigger by remember { mutableStateOf(0) }

    fun startFlip() {
        if (isFlipping) return
        isFlipping = true
        resultTeam = null
        scope.launch {
            val outcome = if (Random.nextBoolean()) 1 else 2
            val spins = Random.nextInt(6, 10)
            val current = angle.value
            val base = current - (current % 360f)
            val finalAngle = base + spins * 360f + if (outcome == 1) 0f else 180f

            launch {
                lift.animateTo(-46f, tween(280, easing = FastOutSlowInEasing))
                lift.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            }
            angle.animateTo(finalAngle, tween(1500, easing = FastOutSlowInEasing))

            resultTeam = outcome
            isFlipping = false
            burstTrigger++
        }
    }

    TiebreakerShell(
        title = "Who breaks?",
        isBusy = isFlipping,
        busyLabel = "Flipping…",
        resultLabel = resultTeam?.let { "${if (it == 1) teamAName else teamBName} breaks!" },
        resultColor = if (resultTeam == 2) colorB else colorA,
        actionLabel = "Flip",
        againLabel = "Flip again",
        onAction = { startFlip() },
        onDismiss = onDismiss
    ) {
        Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .offset(y = lift.value.dp)
                    .graphicsLayer {
                        rotationY = angle.value
                        cameraDistance = 16f * density
                    }
            ) {
                val normalized = ((angle.value % 360f) + 360f) % 360f
                val showFront = normalized < 90f || normalized > 270f
                if (showFront) {
                    CoinFace(label = teamAName, color = colorA)
                } else {
                    Box(modifier = Modifier.graphicsLayer { rotationY = 180f }) {
                        CoinFace(label = teamBName, color = colorB)
                    }
                }
            }
            BurstEffect(
                trigger = burstTrigger,
                color = if (resultTeam == 2) colorB else colorA,
                modifier = Modifier.matchParentSize(),
                particleCount = 18,
                maxRadiusDp = 90.dp
            )
        }
    }
}

/** One face of the coin: a metallic-looking circle with a team's name centered on it. */
@Composable
fun CoinFace(label: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(BrassLight, color)))
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color(0xFF1A1200),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            maxLines = 3
        )
    }
}

/** A spinning wheel tiebreaker for matchups with three or more teams. */
@Composable
fun WheelSpinDialog(
    teamNames: List<String>,
    colors: List<Color>,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val angle = remember { Animatable(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    var resultIndex by remember { mutableStateOf<Int?>(null) }
    var burstTrigger by remember { mutableStateOf(0) }
    val textMeasurer = rememberTextMeasurer()
    val sweep = 360f / teamNames.size

    fun startSpin() {
        if (isSpinning) return
        isSpinning = true
        resultIndex = null
        scope.launch {
            val outcome = Random.nextInt(teamNames.size)
            val spins = Random.nextInt(5, 9)
            // Sector i is centered at (-90 + (i+0.5)*sweep) before rotation; to land
            // it under the pointer at the top we rotate by the negative of that.
            val landing = -((outcome + 0.5f) * sweep)
            val current = angle.value
            val base = current - (current % 360f)
            angle.animateTo(base + spins * 360f + landing, tween(2600, easing = FastOutSlowInEasing))
            resultIndex = outcome
            isSpinning = false
            burstTrigger++
        }
    }

    TiebreakerShell(
        title = "Who breaks?",
        isBusy = isSpinning,
        busyLabel = "Spinning…",
        resultLabel = resultIndex?.let { "${teamNames[it]} breaks!" },
        resultColor = resultIndex?.let { colors.getOrElse(it) { Brass } } ?: Brass,
        actionLabel = "Spin",
        againLabel = "Spin again",
        onAction = { startSpin() },
        onDismiss = onDismiss
    ) {
        Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                rotate(degrees = angle.value, pivot = center) {
                    teamNames.indices.forEach { i ->
                        drawArc(
                            color = colors.getOrElse(i) { Brass },
                            startAngle = -90f + i * sweep,
                            sweepAngle = sweep,
                            useCenter = true,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2)
                        )
                        drawArc(
                            color = FeltGreenDark.copy(alpha = 0.55f),
                            startAngle = -90f + i * sweep,
                            sweepAngle = sweep,
                            useCenter = true,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // Labels are drawn outside the rotation so they stay upright,
                // positioned using the wheel's current angle.
                teamNames.forEachIndexed { i, name ->
                    val deg = -90f + (i + 0.5f) * sweep + angle.value
                    val rad = deg * PI.toFloat() / 180f
                    val lr = radius * 0.6f
                    val laid = textMeasurer.measure(
                        text = name,
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = contrastingTextColor(colors.getOrElse(i) { Brass })
                        )
                    )
                    drawText(
                        textLayoutResult = laid,
                        topLeft = Offset(
                            center.x + cos(rad) * lr - laid.size.width / 2f,
                            center.y + sin(rad) * lr - laid.size.height / 2f
                        )
                    )
                }

                // Hub and pointer.
                drawCircle(color = SurfaceVariant, radius = radius * 0.16f, center = center)
                drawCircle(color = CueCream, radius = radius * 0.16f, center = center, style = Stroke(width = 2.dp.toPx()))
                val tip = Offset(center.x, center.y - radius - 2.dp.toPx())
                drawPath(
                    path = Path().apply {
                        moveTo(tip.x, tip.y + 16.dp.toPx())
                        lineTo(tip.x - 9.dp.toPx(), tip.y - 4.dp.toPx())
                        lineTo(tip.x + 9.dp.toPx(), tip.y - 4.dp.toPx())
                        close()
                    },
                    color = CueCream
                )
            }
            BurstEffect(
                trigger = burstTrigger,
                color = resultIndex?.let { colors.getOrElse(it) { Brass } } ?: Brass,
                modifier = Modifier.matchParentSize(),
                particleCount = 20,
                maxRadiusDp = 110.dp
            )
        }
    }
}

/** Shared chrome for both tiebreaker dialogs. */
@Composable
fun TiebreakerShell(
    title: String,
    isBusy: Boolean,
    busyLabel: String,
    resultLabel: String?,
    resultColor: Color,
    actionLabel: String,
    againLabel: String,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = { if (!isBusy) onDismiss() }) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SurfaceVariant)) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(300.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, color = CueCream, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Just a tiebreaker — this isn't recorded as a game.",
                    color = OnSurfaceMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))

                content()

                Spacer(modifier = Modifier.height(18.dp))

                when {
                    isBusy -> Text(busyLabel, color = OnSurfaceMuted)
                    resultLabel != null -> {
                        Text(
                            resultLabel,
                            color = resultColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onAction) { Text(againLabel, color = ChalkBlue) }
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = Brass, contentColor = Color(0xFF241A00))
                            ) {
                                Text("Done", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    else -> {
                        Button(
                            onClick = onAction,
                            colors = ButtonDefaults.buttonColors(containerColor = Brass, contentColor = Color(0xFF241A00))
                        ) {
                            Text(actionLabel, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onDismiss) { Text("Cancel", color = OnSurfaceMuted) }
                    }
                }
            }
        }
    }
}

/** A row of tappable color swatches. */
@Composable
fun ColorPickerRow(selected: Color, onSelect: (Color) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TeamColorChoices.forEach { choice ->
            val isSelected = choice.toArgb() == selected.toArgb()
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(choice)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) CueCream else Color.Black.copy(alpha = 0.35f),
                        shape = CircleShape
                    )
                    .clickable { onSelect(choice) }
            )
        }
    }
}

/** Editable working copy of a team while a dialog is open. */
class TeamDraft(
    initialName: String = "",
    initialPlayers: List<String> = emptyList(),
    initialColor: Color
) {
    var name by mutableStateOf(initialName)
    var color by mutableStateOf(initialColor)
    val players = mutableStateListOf<String>().apply { addAll(initialPlayers) }

    fun toTeam(): Team = Team(
        name = name.trim().ifBlank { null },
        players = players.map { it.trim() }.filter { it.isNotEmpty() },
        colorArgb = color.toArgb()
    )

    val isValid: Boolean
        get() = name.isNotBlank() || players.any { it.isNotBlank() }

    fun resolvedName(fallback: String): String = when {
        name.isNotBlank() -> name.trim()
        players.any { it.isNotBlank() } -> players.filter { it.isNotBlank() }.joinToString(" & ")
        else -> fallback
    }
}

/**
 * Editor for the full list of teams in a matchup: add or remove teams, rename
 * them, manage their players, and pick their colors.
 */
@Composable
fun TeamDraftEditor(drafts: SnapshotStateList<TeamDraft>, knownPlayers: List<String>) {
    Column {
        drafts.forEachIndexed { index, draft ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("vs", color = OnSurfaceMuted, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(draft.color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Team ${index + 1}", color = Brass, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                if (drafts.size > 2) {
                    TextButton(onClick = { drafts.removeAt(index) }) {
                        Text("Remove", color = OnSurfaceMuted, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            TeamInputSection(
                teamName = draft.name,
                onTeamNameChange = { draft.name = it },
                players = draft.players,
                knownPlayers = knownPlayers
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Color", color = OnSurfaceMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            ColorPickerRow(selected = draft.color, onSelect = { draft.color = it })
        }

        if (drafts.size < MAX_TEAMS) {
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = {
                    drafts.add(TeamDraft(initialColor = defaultTeamColor(drafts.size)))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = ChalkBlue)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add team (${drafts.size} of $MAX_TEAMS)", color = ChalkBlue)
            }
        }
    }
}

@Composable
fun AddMatchupDialog(
    knownPlayers: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<Team>) -> Unit
) {
    val drafts = remember {
        mutableStateListOf(
            TeamDraft(initialColor = defaultTeamColor(0)),
            TeamDraft(initialColor = defaultTeamColor(1))
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceVariant,
        titleContentColor = CueCream,
        textContentColor = CueCream,
        title = { Text("New matchup") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                TeamDraftEditor(drafts = drafts, knownPlayers = knownPlayers)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Add a third or fourth team for free-for-all games. Solids/stripes is available on two-team matchups.",
                    color = OnSurfaceMuted,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (drafts.size >= 2 && drafts.all { it.isValid }) {
                        onConfirm(drafts.map { it.toTeam() })
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
 * Settings for an existing matchup: add/remove teams, rename them, manage
 * players, pick colors, or delete the matchup entirely.
 */
@Composable
fun MatchupSettingsDialog(
    matchup: Matchup,
    knownPlayers: List<String>,
    onSave: (List<Team>, Int?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val drafts = remember {
        mutableStateListOf<TeamDraft>().apply {
            matchup.teams.forEachIndexed { i, t ->
                add(TeamDraft(initialName = t.name ?: "", initialPlayers = t.players, initialColor = t.color(defaultTeamColor(i))))
            }
        }
    }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceVariant,
        titleContentColor = CueCream,
        textContentColor = CueCream,
        title = { Text("Matchup settings") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                TeamDraftEditor(drafts = drafts, knownPlayers = knownPlayers)

                if (matchup.history.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Removing a team also clears that team's recorded results.",
                        color = OnSurfaceMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = OnSurfaceMuted.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))

                if (!confirmDelete) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete matchup", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Text(
                        "Delete this matchup and all ${matchup.history.size} recorded games?",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { confirmDelete = false }) { Text("Keep", color = OnSurfaceMuted) }
                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            )
                        ) { Text("Delete", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (drafts.size >= 2 && drafts.all { it.isValid }) {
                        // Keep the ball assignment only if this is still head-to-head.
                        val solids = if (drafts.size == 2) matchup.solidsTeam else null
                        onSave(drafts.map { it.toTeam() }, solids)
                    }
                }
            ) { Text("Save", color = Brass, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = OnSurfaceMuted) }
        }
    )
}

/** Optional team name plus zero or more player name fields. */
@Composable
fun TeamInputSection(
    teamName: String,
    onTeamNameChange: (String) -> Unit,
    players: SnapshotStateList<String>,
    knownPlayers: List<String>
) {
    Column {
        OutlinedTextField(
            value = teamName,
            onValueChange = onTeamNameChange,
            label = { Text("Team name (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Brass, cursorColor = Brass)
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
            Icon(Icons.Default.Add, contentDescription = null, tint = ChalkBlue, modifier = Modifier.size(18.dp))
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
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Brass, cursorColor = Brass)
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove player", tint = OnSurfaceMuted)
            }
        }

        val suggestions = remember(value, knownPlayers, alreadyChosen) {
            if (value.isBlank()) emptyList()
            else knownPlayers.filter { candidate ->
                candidate.contains(value, ignoreCase = true) &&
                    !candidate.equals(value, ignoreCase = true) &&
                    alreadyChosen.none { it.equals(candidate, ignoreCase = true) }
            }.take(5)
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
 * Cumulative wins per team over the course of the matchup — one line per team,
 * in that team's color. Works for any number of teams.
 */
@Composable
fun CumulativeWinsChart(
    history: List<GameResult>,
    teamCount: Int,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val series = remember(history, teamCount) {
        val running = IntArray(teamCount)
        val out = List(teamCount) { mutableListOf(0) }
        history.forEach { r ->
            if (r.winnerTeam in 1..teamCount) running[r.winnerTeam - 1]++
            for (t in 0 until teamCount) out[t].add(running[t])
        }
        out.map { it.toList() }
    }
    val maxWins = max(series.maxOfOrNull { s -> s.maxOrNull() ?: 0 } ?: 1, 1)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val pad = 8f
        val usableH = h - pad * 2
        val pointCount = series.firstOrNull()?.size ?: 0
        if (pointCount < 2) return@Canvas
        val stepX = w / (pointCount - 1)

        drawLine(
            color = OnSurfaceMuted.copy(alpha = 0.25f),
            start = Offset(0f, h - pad),
            end = Offset(w, h - pad),
            strokeWidth = 1.dp.toPx()
        )

        series.forEachIndexed { teamIdx, values ->
            val path = Path()
            values.forEachIndexed { i, v ->
                val x = i * stepX
                val y = h - pad - (v.toFloat() / maxWins) * usableH
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = colors.getOrElse(teamIdx) { Brass },
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            val lastX = (values.size - 1) * stepX
            val lastY = h - pad - (values.last().toFloat() / maxWins) * usableH
            drawCircle(
                color = colors.getOrElse(teamIdx) { Brass },
                radius = 4.dp.toPx(),
                center = Offset(lastX, lastY)
            )
        }
    }
}

/**
 * Head-to-head momentum: the running win differential (team 1 minus team 2).
 * Rises when the first team is on top, dips when the second is.
 */
@Composable
fun MomentumLineChart(
    history: List<GameResult>,
    colorA: Color,
    colorB: Color,
    modifier: Modifier = Modifier
) {
    val diffs = remember(history) {
        var running = 0
        history.map { r -> running += if (r.winnerTeam == 1) 1 else -1; running }
    }
    val maxAbs = (diffs.maxOfOrNull { abs(it) } ?: 1).coerceAtLeast(1)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val topPad = 10f
        val usableH = h - topPad * 2
        val midY = h / 2f
        val stepX = if (diffs.size > 1) w / (diffs.size - 1) else w

        drawLine(
            color = OnSurfaceMuted.copy(alpha = 0.25f),
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 1.dp.toPx()
        )

        val points = diffs.mapIndexed { i, d ->
            Offset(i * stepX, midY - (d.toFloat() / maxAbs) * (usableH / 2f))
        }

        val path = Path().apply {
            points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
        }
        drawPath(
            path = path,
            color = Brass,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        points.forEachIndexed { i, p ->
            drawCircle(
                color = if (history[i].winnerTeam == 1) colorA else colorB,
                radius = 4.5.dp.toPx(),
                center = p
            )
        }
    }
}

@Composable
fun HistoryDialog(matchup: Matchup, onDismiss: () -> Unit) {
    val formatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val colors = matchup.teamColors()
    val wins = (1..matchup.teamCount).map { matchup.winsFor(it) }
    val total = max(wins.sum(), 1)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceVariant,
        titleContentColor = CueCream,
        textContentColor = CueCream,
        title = { Text(matchup.teams.joinToString(" vs ") { it.displayName() }, fontSize = 17.sp) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (matchup.history.isEmpty()) {
                    Text("No games recorded yet.", color = OnSurfaceMuted)
                } else {
                    // Per-team score summary.
                    matchup.teams.forEachIndexed { i, team ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(colors[i])
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(team.displayName(), color = CueCream, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text(
                                "${wins[i]}  ·  ${((wins[i].toFloat() / total) * 100).toInt()}%",
                                color = colors[i],
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Weighted win-share bar.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                    ) {
                        wins.forEachIndexed { i, w ->
                            Box(
                                modifier = Modifier
                                    .weight(w.toFloat().coerceAtLeast(0.001f))
                                    .fillMaxHeight()
                                    .background(colors[i])
                            )
                        }
                    }

                    if (matchup.history.size >= 2) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            if (matchup.isHeadToHead) "Momentum" else "Cumulative wins",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (matchup.isHeadToHead) {
                            MomentumLineChart(
                                history = matchup.history,
                                colorA = colors[0],
                                colorB = colors[1],
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                            )
                        } else {
                            CumulativeWinsChart(
                                history = matchup.history,
                                teamCount = matchup.teamCount,
                                colors = colors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Recent form", style = MaterialTheme.typography.labelMedium, color = OnSurfaceMuted)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        matchup.history.takeLast(12).forEach { result ->
                            val c = colors.getOrElse(result.winnerTeam - 1) { Brass }
                            val initial = matchup.teamName(result.winnerTeam).trim().firstOrNull()?.uppercase() ?: "?"
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(c),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initial,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = contrastingTextColor(c)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Full history", style = MaterialTheme.typography.labelMedium, color = OnSurfaceMuted)
                    Spacer(modifier = Modifier.height(4.dp))

                    matchup.history.reversed().forEach { result ->
                        val c = colors.getOrElse(result.winnerTeam - 1) { Brass }
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
                                    .background(c)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "${formatter.format(Date(result.timestamp))} — ${matchup.teamName(result.winnerTeam)} won",
                                fontSize = 13.sp,
                                color = CueCream
                            )
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
