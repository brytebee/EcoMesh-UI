package com.brytebee.ecomesh.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brytebee.ecomesh.core.discovery.*
import com.brytebee.ecomesh.core.transport.*
import com.brytebee.ecomesh.core.thermal.ThermalLevel
import com.brytebee.ecomesh.core.MeshCore
import com.brytebee.ecomesh.core.messaging.*
import com.brytebee.ecomesh.core.db.getDatabaseDriverFactory
import kotlinx.coroutines.launch
import com.brytebee.ecomesh.core.messaging.TransferProgress


/**
 * Root composable for EcoMesh — shared across Android, iOS, Desktop, and Web.
 * This is the entry point for the Compose Multiplatform UI layer.
 */
@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val meshCore = remember { 
        MeshCore(
            driverFactory = getDatabaseDriverFactory(),
            // Only use real platform discovery — MockDiscoveryService creates fake peers
            // with no host/port, causing silent failures and infinite loading spinners.
            discoveryServices = getPlatformDiscoveryServices(),
            nodeId = "mesh-node-${(1000..9999).random()}",
            displayName = "${getPlatformName()} Node" 
        )
    }
    
    val peers by meshCore.discoveryManager.peers.collectAsState()
    val thermalLevel by meshCore.discoveryManager.thermalService.thermalLevel.collectAsState()
    
    // Core Session & Messaging state
    val activeSessions by meshCore.sessionManager.activeSessions.collectAsState()
    val allMessages by meshCore.messagingManager.allMessages.collectAsState(initial = emptyList())
    val activeTransfers by meshCore.fileTransferService.activeTransfers.collectAsState()
    
    var connectingPeerId by remember { mutableStateOf<String?>(null) }
    var activeChatPeerId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeSessions) {
        // Clear connecting spinner if the session was established or failed
        if (connectingPeerId != null && activeSessions.containsKey(connectingPeerId)) {
            connectingPeerId = null
        }
    }

    LaunchedEffect(Unit) {
        meshCore.start()
    }

    EcoMeshTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF070B14)
        ) {
            if (activeChatPeerId != null) {
                // Show Chat Screen (to be implemented)
                ChatScreen(
                    peerId = activeChatPeerId!!,
                    messages = allMessages.filter { it.senderId == activeChatPeerId || it.senderId == meshCore.sessionManager.localNodeId },
                    activeTransfers = activeTransfers,
                    onBack = { activeChatPeerId = null },
                    onSendMessage = { text ->
                        scope.launch {
                            meshCore.messagingManager.sendMessage(activeChatPeerId!!, text)
                        }
                    },
                    onSendFileRaw = { bytes, fileName ->
                        scope.launch {
                            meshCore.messagingManager.sendMessage(activeChatPeerId!!, "📁 Shared file: $fileName")
                            meshCore.fileTransferService.sendFileRaw(
                                targetNodeId = activeChatPeerId!!,
                                fileId = "file-${(100..999).random()}",
                                fileName = fileName,
                                fileData = bytes
                            )
                        }
                    }
                )
            } else {
                EcoMeshHomeScreen(
                    meshCore = meshCore,
                    scope = scope,
                    peers = peers,
                    activeSessions = activeSessions,
                    connectingPeerId = connectingPeerId,
                    thermalLevel = thermalLevel,
                    onConnect = { peer ->
                        scope.launch {
                            connectingPeerId = peer.id
                            val success = meshCore.connectToPeer(peer)
                            if (!success) {
                                connectingPeerId = null
                            }
                        }
                    },
                    onOpenChat = { peerNodeId ->
                        activeChatPeerId = peerNodeId
                    },
                    onSendFile = { peerNodeId ->
                        activeChatPeerId = peerNodeId 
                        // Note: To use rememberFilePicker within a non-composable callback securely, 
                        // we must hoist it. We'll add this inside ChatScreen later.
                    }
                )
            }
        }
    }
}

@Composable
fun MeshPulseAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
                .background(Color(0xFF4FC3F7), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color(0xFF4FC3F7), CircleShape)
        )
    }
}

@Composable
fun EcoMeshHomeScreen(
    meshCore: MeshCore,
    scope: kotlinx.coroutines.CoroutineScope,
    peers: List<Peer>,
    activeSessions: Map<String, PeerSession>,
    connectingPeerId: String?,
    thermalLevel: ThermalLevel,
    onConnect: (Peer) -> Unit,
    onOpenChat: (String) -> Unit,
    onSendFile: (String) -> Unit
) {
    val transfers by meshCore.fileTransferService.activeTransfers.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1A237E), Color(0xFF070B14)),
                    radius = 2000f
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mesh Map Visualisation
            if (thermalLevel < ThermalLevel.LEVEL_2_ECO) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    MeshMapCanvas(peers = peers, activeSessions = activeSessions)
                }
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "🌐",
                        fontSize = 40.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            
            Text(
                text = "EcoMesh",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Text(
                text = "Autonomous · Distributed · Resilient",
                color = Color(0xFFB0BEC5),
                fontSize = 12.sp,
                fontWeight = FontWeight.Light
            )
            
            Spacer(Modifier.height(40.dp))

            // Thermal Alert (Glassy)
            if (thermalLevel >= ThermalLevel.LEVEL_2_ECO) {
                Card(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0x33FF5252)),
                    border = BorderStroke(1.dp, Color(0x66FF5252))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥", fontSize = 20.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Thermal Safety Active. Features limited.",
                            color = Color(0xFFFFCDD2),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            SessionStatusBar(sessionCount = activeSessions.size)

            Spacer(Modifier.height(16.dp))

            // Gossip Broadcast Button (Available if at least 1 session is active)
            if (activeSessions.isNotEmpty()) {
                Button(
                    onClick = {
                        scope.launch {
                            meshCore.gossipManager.publishGossip("ALERT", "🚨 Emergency Mesh Alert!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x22F44336)),
                    border = BorderStroke(1.dp, Color(0x44F44336)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("📢 SHARE ALERT (GOSSIP)", color = Color(0xFFEF5350), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))
            
            // Active Transfers Section
            if (transfers.isNotEmpty()) {
                Text(
                    text = "ACTIVE TRANSFERS",
                    color = Color(0xFF4FC3F7),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = transfers.values.toList()) { transfer ->
                        TransferItem(transfer)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // Active Sessions Section
            if (activeSessions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "ACTIVE SESSIONS",
                            color = Color(0xFF4CAF50),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                    items(items = activeSessions.values.toList()) { session ->
                        SessionItem(
                            session = session,
                            onSendFile = { onSendFile(session.nodeId) },
                            onOpenChat = { onOpenChat(session.nodeId) }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "NEARBY NODES",
                        color = Color(0xFF90CAF9),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                items(peers) { peer ->
                    PeerItem(
                        peer = peer,
                        isConnecting = connectingPeerId == peer.id,
                        onConnect = { onConnect(peer) }
                    )
                }
            }
        }
    }
}

@Composable
fun SessionStatusBar(sessionCount: Int) {
    val (text, color) = if (sessionCount > 0) {
        "$sessionCount peer${if (sessionCount > 1) "s" else ""} connected" to Color(0xFF4CAF50)
    } else {
        "No active sessions" to Color(0xFF78909C)
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SessionItem(
    session: PeerSession,
    onSendFile: () -> Unit,
    onOpenChat: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x224CAF50)),
        border = BorderStroke(width = 1.dp, color = Color(0x444CAF50))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF4CAF50)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🛡️", fontSize = 20.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = session.displayName, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text(text = session.nodeId.take(12), color = Color(0xFF90CAF9), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            IconButton(onClick = onOpenChat) { Text("💬", fontSize = 18.sp) }
            IconButton(onClick = onSendFile) { Text("📁", fontSize = 18.sp) }
        }
    }
}

@Composable
fun PeerItem(
    peer: Peer,
    isConnecting: Boolean,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)),
        border = BorderStroke(width = 1.dp, color = Color(0x22FFFFFF))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF1D3557)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (peer.type == PeerType.MOBILE) "📱" else "💻", fontSize = 20.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = peer.name, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = peer.id.take(12), color = Color(0xFF90CAF9), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(8.dp))
                    Text(text = "📶 Good", color = Color(0xFFAED581), fontSize = 10.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(text = "🔋 85%", color = Color(0xFFAED581), fontSize = 10.sp)
                }
                Text(text = "Online just now", color = Color(0x88FFFFFF), fontSize = 10.sp)
            }
            
            if (isConnecting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFFFFB300), strokeWidth = 2.dp)
            } else {
                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FC3F7), contentColor = Color(0xFF070B14)),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("CONNECT", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun TransferItem(transfer: TransferProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)),
        border = BorderStroke(1.dp, Color(0x22FFFFFF))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📄", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(transfer.fileName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(transfer.status, color = if (transfer.status == "COMPLETED") Color(0xFF4CAF50) else Color(0xFFB0BEC5), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = transfer.progress,
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = Color(0xFF4FC3F7),
                trackColor = Color(0x33FFFFFF)
            )
        }
    }
}

@Composable
fun ChatScreen(
    peerId: String,
    messages: List<com.brytebee.ecomesh.core.messaging.ChatMessageModel>,
    activeTransfers: Map<String, com.brytebee.ecomesh.core.messaging.TransferProgress>,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendFileRaw: (ByteArray, String) -> Unit
) {
    var textState by remember { mutableStateOf("") }
    
    val filePickerLauncher = rememberFilePicker { bytes, name ->
        if (bytes != null && name != null) {
            onSendFileRaw(bytes, name)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1A237E), Color(0xFF070B14)),
                    radius = 2000f
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            // Glassmorphism Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x331D3557)),
                border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp).background(Color(0x22FFFFFF), CircleShape)
                    ) {
                        Text("🔙", fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SECURE CHAT",
                            color = Color(0xFF4FC3F7),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF4CAF50), CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = peerId.take(12),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // Messages
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                reverseLayout = true,
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages.reversed(), key = { it.id }) { msg ->
                    val isMe = msg.senderId != peerId // Simplified checking
                    
                    val bubbleShape = if (isMe) {
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
                    } else {
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(min = 60.dp, max = 280.dp)
                                .shadow(if (isMe) 8.dp else 4.dp, bubbleShape)
                                .background(
                                    brush = if (isMe) {
                                        Brush.linearGradient(listOf(Color(0xFF1976D2), Color(0xFF0D47A1)))
                                    } else {
                                        Brush.linearGradient(listOf(Color(0xFF263238), Color(0xFF1C313A)))
                                    },
                                    shape = bubbleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isMe) Color.Transparent else Color(0x33FFFFFF),
                                    shape = bubbleShape
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Column {
                                Text(
                                    text = msg.content,
                                    color = if (isMe) Color.White else Color(0xFFE3F2FD),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 20.sp
                                )
                                
                                val filePrefix = "📁 Shared file: "
                                if (msg.content.startsWith(filePrefix)) {
                                    val fileName = msg.content.removePrefix(filePrefix)
                                    val transfer = activeTransfers.values.find { it.fileName == fileName }
                                    if (transfer != null && transfer.status != "COMPLETED") {
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0x22000000)).padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            LinearProgressIndicator(
                                                progress = transfer.progress,
                                                modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                                color = if (isMe) Color.White else Color(0xFF4FC3F7),
                                                trackColor = Color(0x44FFFFFF)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "${(transfer.progress * 100).toInt()}%",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else if (transfer?.status == "COMPLETED" || transfer == null) {
                                        // Allow opening the file if it's completed or already existed 
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0x22000000))
                                                .clickable { openFile(fileName) }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("👁 Open File", color = Color(0xFF64FFDA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                
                                Spacer(Modifier.height(4.dp))
                                Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = formatTimestamp(msg.timestamp),
                                        color = if (isMe) Color(0xAAFFFFFF) else Color(0x88E3F2FD),
                                        fontSize = 10.sp
                                    )
                                    if (isMe) {
                                        Spacer(Modifier.width(4.dp))
                                        val tickText = when (msg.status) {
                                            "PENDING", "SENDING" -> "⌚"
                                            "SENT" -> "✓"
                                            "DELIVERED" -> "✓✓"
                                            "FAILED" -> "✗"
                                            else -> "✓"
                                        }
                                        val tickColor = if (msg.status == "DELIVERED") Color(0xFF64FFDA) else Color(0xBBFFFFFF)
                                        Text(
                                            text = tickText,
                                            color = tickColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Input Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.Bottom
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
                    border = BorderStroke(1.dp, Color(0x22FFFFFF))
                ) {
                    TextField(
                        value = textState,
                        onValueChange = { textState = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp, max = 120.dp),
                        placeholder = { Text("Type a message...", color = Color(0x99FFFFFF)) },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White,
                            cursorColor = Color(0xFF4FC3F7),
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        )
                    )
                }
                
                // File Attachment Button
                Button(
                    onClick = { filePickerLauncher() },
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                    border = BorderStroke(1.dp, Color(0x22FFFFFF))
                ) {
                    Text("📎", fontSize = 24.sp)
                }
                
                Spacer(Modifier.width(8.dp))

                val isInputActive = textState.isNotBlank()
                Button(
                    onClick = {
                        if (isInputActive) {
                            onSendMessage(textState)
                            textState = ""
                        }
                    },
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isInputActive) Color(0xFF4FC3F7) else Color(0x33FFFFFF)
                    ),
                    border = if (isInputActive) null else BorderStroke(1.dp, Color(0x22FFFFFF))
                ) {
                    Text(
                        text = "🚀",
                        fontSize = 24.sp,
                        color = if (isInputActive) Color.White else Color(0x88FFFFFF)
                    )
                }
            }
        }
    }
}

fun formatTimestamp(epochMillis: Long): String {
    val instant = Instant.fromEpochMilliseconds(epochMillis)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = localDateTime.hour.toString().padStart(2, '0')
    val minute = localDateTime.minute.toString().padStart(2, '0')
    return "$hour:$minute"
}

@Composable
fun MeshMapCanvas(peers: List<com.brytebee.ecomesh.core.discovery.Peer>, activeSessions: Map<String, com.brytebee.ecomesh.core.messaging.PeerSession>) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
        val radiusLimit = size.minDimension / 2.5f
        
        // Draw Radar rings
        drawCircle(color = Color(0x11FFFFFF), radius = radiusLimit, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
        drawCircle(color = Color(0x22FFFFFF), radius = radiusLimit * 0.6f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
        
        // Central Node
        drawCircle(color = Color(0xFF4FC3F7), radius = 12f, center = center)
        
        // Draw Peers
        val angleStep = if (peers.isNotEmpty()) (2 * kotlin.math.PI / peers.size).toFloat() else 0f
        peers.forEachIndexed { index, peer ->
            val angle = index * angleStep
            val distance = radiusLimit * 0.8f // Simplified fixed distance
            val x = center.x + kotlin.math.cos(angle) * distance
            val y = center.y + kotlin.math.sin(angle) * distance
            val peerOffset = androidx.compose.ui.geometry.Offset(x, y)
            
            val isConnected = activeSessions.values.any { it.nodeId == peer.id }
            
            if (isConnected) {
                // Draw connecting line
                drawLine(
                    color = Color(0xFF4CAF50),
                    start = center,
                    end = peerOffset,
                    strokeWidth = 2f
                )
                // Draw connected peer
                drawCircle(color = Color(0xFF4CAF50), radius = 10f, center = peerOffset)
            } else {
                // Draw disconnected peer
                drawCircle(color = Color(0x88FFFFFF), radius = 8f, center = peerOffset)
            }
        }
    }
}
