package com.example.pooltracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
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
                        knownPlayers = viewModel.knownPlayers,
                        onToggle = { team -> viewModel.recordResult(matchup, team) },
                        onHistoryClick = { historyTarget = matchup },
                        onDelete = { viewModel.removeMatchup(matchup) },
                        onUpdateTeams = { teamA, teamB, solidsTeam ->
                            viewModel.updateTeams(matchup.id, teamA, teamB, solidsTeam)
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
    knownPlayers: List<String>,
    onToggle: (Int) -> Unit,
    onHistoryClick: () -> Unit,
    onDelete: () -> Unit,
    onUpdateTeams: (Team, Team, Int?) -> Unit
) {
    val lastWinner = matchup.lastWinner
    var showCoinFlip by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val colorA = matchup.teamA.color(FeltGreenLight)
    val colorB = matchup.teamB.color(Brass)

    var burstSide by remember { mutableStateOf(1) }
    var burstTrigger by remember { mutableStateOf(0) }
    var burstOrigin by remember { mutableStateOf<Offset?>(null) }
    var cardCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

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
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Matchup settings",
                                tint = OnSurfaceMuted
                            )
                        }
                    }

                    if (matchup.solidsTeam != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        BallAssignmentRow(
                            teamAName = matchup.teamAName(),
                            teamBName = matchup.teamBName(),
                            solidsTeam = matchup.solidsTeam
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    WinnerTile(
                        teamAName = matchup.teamAName(),
                        teamBName = matchup.teamBName(),
                        colorA = colorA,
                        colorB = colorB,
                        lastWinner = lastWinner,
                        onSelectWinner = onToggle,
                        onBurst = { side, tapOffset, tileCoords ->
                            burstSide = side
                            // Translate the tap from the tile's coordinate space into
                            // the card's, so the explosion originates exactly where
                            // the finger landed even though it renders card-wide.
                            burstOrigin = if (tileCoords != null && cardCoords != null) {
                                cardCoords!!.localPositionOf(tileCoords, tapOffset)
                            } else {
                                null
                            }
                            burstTrigger++
                        }
                    )

                    if (lastWinner == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { showCoinFlip = true },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("🪙 Flip a coin to decide who breaks", color = ChalkBlue, fontWeight = FontWeight.SemiBold)
                        }
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

            // Explosion overlay spans the *entire* card, not just the win-tile strip,
            // so the burst has real room to breathe.
            BurstEffect(
                trigger = burstTrigger,
                color = if (burstSide == 2) colorB else colorA,
                originPx = burstOrigin,
                originXFraction = if (burstSide == 1) 0.25f else 0.75f,
                originYFraction = 0.5f,
                particleCount = 22,
                maxRadiusDp = 110.dp,
                modifier = Modifier.matchParentSize()
            )
        }
    }

    if (showCoinFlip) {
        CoinFlipDialog(
            teamAName = matchup.teamAName(),
            teamBName = matchup.teamBName(),
            colorA = colorA,
            colorB = colorB,
            onDismiss = { showCoinFlip = false }
        )
    }

    if (showSettings) {
        MatchupSettingsDialog(
            matchup = matchup,
            knownPlayers = knownPlayers,
            onSave = { teamA, teamB, solidsTeam ->
                onUpdateTeams(teamA, teamB, solidsTeam)
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
 * A large, tappable "who won" tile. Tapping the left half always records a win
 * for Team A, tapping the right half always records a win for Team B — so the
 * same team can win repeatedly without the control needing to "toggle" first.
 *
 * - Tapping the side that's already winning makes the puck pulse and burst in place.
 * - Tapping the other side flings the puck across (with a physical bounce-and-settle),
 *   exploding on arrival.
 * - A soft gradient always sweeps toward whichever side is currently ahead.
 */
@Composable
fun WinnerTile(
    teamAName: String,
    teamBName: String,
    colorA: Color,
    colorB: Color,
    lastWinner: Int?,
    onSelectWinner: (Int) -> Unit,
    onBurst: (side: Int, tapOffset: Offset, tileCoords: LayoutCoordinates?) -> Unit
) {
    val scope = rememberCoroutineScope()
    val posFraction = remember { Animatable(if (lastWinner == 2) 1f else 0f) }
    val pulseScale = remember { Animatable(1f) }
    var tileCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val indicatorAlpha by animateFloatAsState(
        targetValue = if (lastWinner == null) 0f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "indicatorAlpha"
    )

    fun handleTap(side: Int, tapOffset: Offset) {
        val isRepeat = lastWinner == side
        val targetFraction = if (side == 1) 0f else 1f
        scope.launch {
            if (isRepeat) {
                // Pulse and burst in place — same winner again.
                pulseScale.animateTo(1.18f, tween(130, easing = FastOutSlowInEasing))
                onBurst(side, tapOffset, tileCoords)
                pulseScale.animateTo(
                    1f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                )
            } else {
                // Fling to the new side; the spring's natural overshoot gives the bounce.
                launch {
                    delay(110)
                    onBurst(side, tapOffset, tileCoords)
                }
                posFraction.animateTo(
                    targetFraction,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioHighBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
        }
        onSelectWinner(side)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .onGloballyPositioned { tileCoords = it }
    ) {
        val halfWidth = maxWidth / 2
        val tint = lerp(colorA, colorB, posFraction.value)
        val textOnA = contrastingTextColor(colorA)
        val textOnB = contrastingTextColor(colorB)

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
                            posFraction.value.coerceIn(0.06f, 0.94f) to tint.copy(alpha = 0.6f),
                            1f to FeltGreenDark
                        )
                    )
            )

            // Solid puck marking exactly who is winning right now.
            Box(
                modifier = Modifier
                    .offset(x = halfWidth * posFraction.value)
                    .width(halfWidth)
                    .fillMaxHeight()
                    .alpha(indicatorAlpha)
                    .graphicsLayer {
                        scaleX = pulseScale.value
                        scaleY = pulseScale.value
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (posFraction.value > 0.5f) colorB else colorA)
            )

            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(lastWinner) {
                            detectTapGestures { offset -> handleTap(1, offset) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = teamAName,
                        fontWeight = if (lastWinner == 1) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = if (lastWinner == 1) textOnA else CueCream,
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
                        .pointerInput(lastWinner) {
                            detectTapGestures { offset ->
                                // Offset is local to this half; shift into full-tile space.
                                handleTap(2, Offset(offset.x + size.width.toFloat(), offset.y))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = teamBName,
                        fontWeight = if (lastWinner == 2) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = if (lastWinner == 2) textOnB else CueCream,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

/** Picks readable black or cream text depending on how light/dark [bg] is. */
fun contrastingTextColor(bg: Color): Color =
    if (bg.luminance() > 0.5f) Color(0xFF15231C) else CueCream

/**
 * A radial particle burst used to punctuate a win. Increment [trigger] to fire
 * it again (even from the same spot) since the value itself, not just its
 * identity, needs to change to relaunch the effect.
 *
 * [originPx] is in this composable's own pixel coordinate space; when null the
 * burst falls back to the fractional origin.
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
 * A pseudo-3D coin flip to decide who *breaks* (goes first). This is purely a
 * tiebreaker aid — it does NOT record a game result, so nothing is written to
 * the matchup's history. The result stays on screen until dismissed.
 */
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
            // Land on the winner's face, measured from the coin's current angle
            // so repeat flips always spin forward rather than snapping backward.
            val current = angle.value
            val base = current - (current % 360f)
            val finalAngle = base + spins * 360f + if (outcome == 1) 0f else 180f

            launch {
                lift.animateTo(-46f, tween(280, easing = FastOutSlowInEasing))
                lift.animateTo(
                    0f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                )
            }
            angle.animateTo(finalAngle, tween(1500, easing = FastOutSlowInEasing))

            resultTeam = outcome
            isFlipping = false
            burstTrigger++
        }
    }

    Dialog(onDismissRequest = { if (!isFlipping) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(280.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Who breaks?", color = CueCream, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Just a tiebreaker — this isn't recorded as a game.",
                    color = OnSurfaceMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier.size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
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

                Spacer(modifier = Modifier.height(20.dp))

                when {
                    isFlipping -> Text("Flipping…", color = OnSurfaceMuted)
                    resultTeam != null -> {
                        val winnerName = if (resultTeam == 1) teamAName else teamBName
                        val winnerColor = if (resultTeam == 1) colorA else colorB
                        Text(
                            "$winnerName breaks!",
                            color = winnerColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { startFlip() }) {
                                Text("Flip again", color = ChalkBlue)
                            }
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Brass,
                                    contentColor = Color(0xFF241A00)
                                )
                            ) {
                                Text("Done", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    else -> Button(
                        onClick = { startFlip() },
                        colors = ButtonDefaults.buttonColors(containerColor = Brass, contentColor = Color(0xFF241A00))
                    ) {
                        Text("Flip", fontWeight = FontWeight.Bold)
                    }
                }

                if (!isFlipping && resultTeam == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onDismiss) { Text("Cancel", color = OnSurfaceMuted) }
                }
            }
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

/** Small inline badges showing which team has solids and which has stripes. */
@Composable
fun BallAssignmentRow(teamAName: String, teamBName: String, solidsTeam: Int) {
    val solidsName = if (solidsTeam == 1) teamAName else teamBName
    val stripesName = if (solidsTeam == 1) teamBName else teamAName
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BallBadge(label = solidsName, striped = false)
        BallBadge(label = stripesName, striped = true)
    }
}

/** A tiny pool-ball glyph plus a team label. */
@Composable
fun BallBadge(label: String, striped: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(14.dp)) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)
            if (striped) {
                drawCircle(color = CueCream, radius = r, center = c)
                // Horizontal stripe band across the middle.
                drawRect(
                    color = Brass,
                    topLeft = Offset(0f, c.y - r * 0.48f),
                    size = androidx.compose.ui.geometry.Size(size.width, r * 0.96f)
                )
                drawCircle(color = CueCream.copy(alpha = 0f), radius = r, center = c)
            } else {
                drawCircle(color = Brass, radius = r, center = c)
                drawCircle(color = CueCream, radius = r * 0.4f, center = c)
            }
        }
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = if (striped) "$label · stripes" else "$label · solids",
            fontSize = 11.sp,
            color = OnSurfaceMuted
        )
    }
}

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

/**
 * Settings for an existing matchup: rename teams, add/remove players, pick each
 * team's color, assign solids/stripes, or delete the matchup entirely.
 */
@Composable
fun MatchupSettingsDialog(
    matchup: Matchup,
    knownPlayers: List<String>,
    onSave: (Team, Team, Int?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var nameA by remember { mutableStateOf(matchup.teamA.name ?: "") }
    var nameB by remember { mutableStateOf(matchup.teamB.name ?: "") }
    val playersA = remember { mutableStateListOf<String>().apply { addAll(matchup.teamA.players) } }
    val playersB = remember { mutableStateListOf<String>().apply { addAll(matchup.teamB.players) } }
    var colorA by remember { mutableStateOf(matchup.teamA.color(FeltGreenLight)) }
    var colorB by remember { mutableStateOf(matchup.teamB.color(Brass)) }
    var solidsTeam by remember { mutableStateOf(matchup.solidsTeam) }
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
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                TeamInputSection(
                    label = "Team A",
                    teamName = nameA,
                    onTeamNameChange = { nameA = it },
                    players = playersA,
                    knownPlayers = knownPlayers
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Team A color", color = OnSurfaceMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                ColorPickerRow(selected = colorA, onSelect = { colorA = it })

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = OnSurfaceMuted.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(18.dp))

                TeamInputSection(
                    label = "Team B",
                    teamName = nameB,
                    onTeamNameChange = { nameB = it },
                    players = playersB,
                    knownPlayers = knownPlayers
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Team B color", color = OnSurfaceMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                ColorPickerRow(selected = colorB, onSelect = { colorB = it })

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = OnSurfaceMuted.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(18.dp))

                Text("Solids / stripes", color = Brass, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                val nameAResolved = nameA.trim().ifBlank {
                    playersA.filter { it.isNotBlank() }.joinToString(" & ").ifBlank { "Team A" }
                }
                val nameBResolved = nameB.trim().ifBlank {
                    playersB.filter { it.isNotBlank() }.joinToString(" & ").ifBlank { "Team B" }
                }
                Column {
                    BallAssignmentOption(
                        text = "$nameAResolved has solids",
                        selected = solidsTeam == 1,
                        onClick = { solidsTeam = 1 }
                    )
                    BallAssignmentOption(
                        text = "$nameBResolved has solids",
                        selected = solidsTeam == 2,
                        onClick = { solidsTeam = 2 }
                    )
                    BallAssignmentOption(
                        text = "Not assigned",
                        selected = solidsTeam == null,
                        onClick = { solidsTeam = null }
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
                        TextButton(onClick = { confirmDelete = false }) {
                            Text("Keep", color = OnSurfaceMuted)
                        }
                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Delete", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val teamA = Team(
                        name = nameA.trim().ifBlank { null },
                        players = playersA.map { it.trim() }.filter { it.isNotEmpty() },
                        colorArgb = colorA.toArgb()
                    )
                    val teamB = Team(
                        name = nameB.trim().ifBlank { null },
                        players = playersB.map { it.trim() }.filter { it.isNotEmpty() },
                        colorArgb = colorB.toArgb()
                    )
                    val validA = teamA.name != null || teamA.players.isNotEmpty()
                    val validB = teamB.name != null || teamB.players.isNotEmpty()
                    if (validA && validB) {
                        onSave(teamA, teamB, solidsTeam)
                    }
                }
            ) { Text("Save", color = Brass, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = OnSurfaceMuted) }
        }
    )
}

/** A single radio-style row for choosing the solids/stripes assignment. */
@Composable
fun BallAssignmentOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Brass,
                unselectedColor = OnSurfaceMuted
            )
        )
        Text(text, color = CueCream, fontSize = 14.sp)
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
    var colorA by remember { mutableStateOf(FeltGreenLight) }
    var colorB by remember { mutableStateOf(Brass) }

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
                Spacer(modifier = Modifier.height(8.dp))
                Text("Team A color", color = OnSurfaceMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                ColorPickerRow(selected = colorA, onSelect = { colorA = it })

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
                Spacer(modifier = Modifier.height(8.dp))
                Text("Team B color", color = OnSurfaceMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                ColorPickerRow(selected = colorB, onSelect = { colorB = it })

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Solids/stripes can be set later from the matchup's settings gear.",
                    color = OnSurfaceMuted,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val teamA = Team(
                        name = teamAName.trim().ifBlank { null },
                        players = teamAPlayers.map { it.trim() }.filter { it.isNotEmpty() },
                        colorArgb = colorA.toArgb()
                    )
                    val teamB = Team(
                        name = teamBName.trim().ifBlank { null },
                        players = teamBPlayers.map { it.trim() }.filter { it.isNotEmpty() },
                        colorArgb = colorB.toArgb()
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
                color = if (isA) colorA else colorB,
                radius = 4.5.dp.toPx(),
                center = p
            )
        }
    }
}

@Composable
fun HistoryDialog(matchup: Matchup, onDismiss: () -> Unit) {
    val colorA = matchup.teamA.color(FeltGreenLight)
    val colorB = matchup.teamB.color(Brass)
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
                                .background(colorA)
                        )
                        Box(
                            modifier = Modifier
                                .weight(winsB.toFloat().coerceAtLeast(0.001f))
                                .fillMaxHeight()
                                .background(colorB)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "${((winsA.toFloat() / total) * 100).toInt()}%",
                            color = colorA,
                            fontSize = 12.sp
                        )
                        Text(
                            "${((winsB.toFloat() / total) * 100).toInt()}%",
                            color = colorB,
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
                                Text(matchup.teamAName(), color = colorA, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("  ·  ", color = OnSurfaceMuted, fontSize = 11.sp)
                                Text(matchup.teamBName(), color = colorB, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        MomentumLineChart(
                            history = matchup.history,
                            colorA = colorA,
                            colorB = colorB,
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
                                    .background(if (isA) colorA else colorB),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isA) "A" else "B",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = contrastingTextColor(if (isA) colorA else colorB)
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
                            val dotColor = if (result.winnerTeam == 1) colorA else colorB
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
