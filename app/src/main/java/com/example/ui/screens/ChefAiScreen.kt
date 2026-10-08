package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.example.data.model.Food
import com.example.ui.theme.*
import com.example.util.ImageUtils
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Data models ────────────────────────────────────────────────────────────

data class ChatMessage(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val suggestedFoods: List<Food> = emptyList(),
    val isTyping: Boolean = false,
    val timestamp: String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
)

// ─── Quick suggestion items ──────────────────────────────────────────────────

private data class QuickPrompt(
    val title: String,
    val description: String,
    val query: String,
    val emoji: String
)

private val clearHeroPrompts = listOf(
    QuickPrompt("Budget Under $10", "Affordable delicious dishes", "Best food under $10", "💰"),
    QuickPrompt("Healthy & Fresh", "Low calorie & nutritious meals", "Healthy meals", "🥗"),
    QuickPrompt("Fast Delivery", "Ready in under 15 minutes", "Quick & fast meals", "⚡"),
    QuickPrompt("Top Rated Food", "Most popular customer favorites", "Top rated popular dishes", "⭐")
)

private val quickSuggestions = listOf(
    "💰 Under $10",
    "🥗 Healthy",
    "⚡ Under 15m",
    "⭐ Top Rated",
    "🌶️ Spicy Food",
    "🍔 Fast Food",
    "🍛 Biryani",
    "🍰 Desserts",
    "💵 Under $5"
)

// ─── Main screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChefAiScreen(
    foods: List<Food>,
    onBackClick: () -> Unit,
    onFoodClick: (Food) -> Unit,
    onAddToCart: (Food) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var isLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // SnackBar state
    val snackbarHostState = remember { SnackbarHostState() }

    fun resetConversation() {
        messages.clear()
        messages.add(
            ChatMessage(
                id = "welcome",
                text = "👋 Hello! I'm **Chef AI**, your personal food advisor.\n\nTell me what you're craving, your budget, or dietary needs, and I'll find the best home-cooked meals for you!",
                isFromUser = false
            )
        )
    }

    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            delay(250)
            resetConversation()
        }
    }

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage(query: String) {
        if (query.isBlank() || isLoading) return
        keyboardController?.hide()

        val userMsg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            text = query.trim(),
            isFromUser = true
        )
        messages.add(userMsg)
        inputText = ""
        isLoading = true

        // Add typing indicator
        val typingId = "typing_${System.currentTimeMillis()}"
        messages.add(ChatMessage(id = typingId, text = "", isFromUser = false, isTyping = true))
    }

    // AI response simulation
    LaunchedEffect(isLoading) {
        if (!isLoading) return@LaunchedEffect
        delay(900)

        val lastUserMsg = messages.lastOrNull { it.isFromUser }?.text?.lowercase() ?: ""
        val matched = getAiResponse(lastUserMsg, foods)

        // Remove typing indicator
        val typingIndex = messages.indexOfFirst { it.isTyping }
        if (typingIndex >= 0) messages.removeAt(typingIndex)

        messages.add(matched)
        isLoading = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Clean, High-Contrast Header placed at upper limit
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF047857),
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // AI Icon Badge
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Chef AI",
                            tint = Color(0xFF047857),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // Title & Clear status
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Chef AI Assistant",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34D399))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Online · Ready to help",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Reset button
                    IconButton(
                        onClick = { resetConversation() },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Chat",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            // High-Contrast, Clear Input Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 10.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Quick suggestion pills
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(quickSuggestions) { suggestion ->
                            Surface(
                                onClick = { sendMessage(suggestion) },
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = suggestion,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                    // Input Field and Send Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    text = "Ask e.g. 'Best pasta under $12'...",
                                    color = Color(0xFF64748B),
                                    fontSize = 14.sp
                                )
                            },
                            shape = RoundedCornerShape(24.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color(0xFF0F172A),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF047857),
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                cursorColor = Color(0xFF047857)
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { sendMessage(inputText) })
                        )

                        Spacer(Modifier.width(10.dp))

                        val hasInput = inputText.isNotBlank()
                        Button(
                            onClick = { sendMessage(inputText) },
                            enabled = hasInput && !isLoading,
                            modifier = Modifier
                                .size(48.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF047857),
                                disabledContainerColor = Color(0xFFE2E8F0)
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = if (hasInput) Color.White else Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Clear Inspiration Cards when only welcome message exists
            if (messages.size <= 1) {
                item {
                    ClearInspirationSection(onPromptClick = { sendMessage(it) })
                    Spacer(Modifier.height(8.dp))
                }
            }

            items(messages, key = { it.id }) { msg ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
                ) {
                    if (msg.isTyping) {
                        ClearTypingBubble()
                    } else if (msg.isFromUser) {
                        ClearUserBubble(msg)
                    } else {
                        ClearAiBubble(
                            message = msg,
                            onFoodClick = onFoodClick,
                            onAddToCart = onAddToCart
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

// ─── Clear Inspiration Section ───────────────────────────────────────────────

@Composable
private fun ClearInspirationSection(onPromptClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💡", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Suggestions & Prompts",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A)
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tap any category below to get instant recommendations:",
                fontSize = 13.sp,
                color = Color(0xFF475569)
            )

            Spacer(Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                clearHeroPrompts.forEach { prompt ->
                    Surface(
                        onClick = { onPromptClick(prompt.query) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = prompt.emoji, fontSize = 22.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = prompt.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = prompt.description,
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Clear Chat Bubbles ──────────────────────────────────────────────────────

@Composable
private fun ClearUserBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Box(
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp, topEnd = 4.dp,
                            bottomStart = 18.dp, bottomEnd = 18.dp
                        )
                    )
                    .background(Color(0xFF047857))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = message.timestamp,
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}

@Composable
private fun ClearAiBubble(
    message: ChatMessage,
    onFoodClick: (Food) -> Unit,
    onAddToCart: (Food) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // AI Avatar
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF047857)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f, fill = false)) {
            // Text Card
            Surface(
                shape = RoundedCornerShape(
                    topStart = 4.dp, topEnd = 18.dp,
                    bottomStart = 18.dp, bottomEnd = 18.dp
                ),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    val parts = message.text.split("**")
                    Text(
                        text = buildAnnotatedString(parts),
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = message.timestamp,
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(start = 6.dp)
            )

            // Clear Food Cards
            if (message.suggestedFoods.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    message.suggestedFoods.forEach { food ->
                        ClearFoodCard(
                            food = food,
                            onFoodClick = { onFoodClick(food) },
                            onAddToCart = { onAddToCart(food) }
                        )
                    }
                }
            }
        }
    }
}

// ─── Clear Typing Bubble ─────────────────────────────────────────────────────

@Composable
private fun ClearTypingBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF047857)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Surface(
            shape = RoundedCornerShape(
                topStart = 4.dp, topEnd = 16.dp,
                bottomStart = 16.dp, bottomEnd = 16.dp
            ),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF047857).copy(alpha = dotAlpha))
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF047857).copy(alpha = ((dotAlpha + 0.3f) % 1f).coerceIn(0.3f, 1f)))
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF047857).copy(alpha = ((dotAlpha + 0.6f) % 1f).coerceIn(0.3f, 1f)))
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Chef AI is thinking…",
                    fontSize = 13.sp,
                    color = Color(0xFF334155),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─── Clear Food Recommendation Card ──────────────────────────────────────────

@Composable
private fun ClearFoodCard(
    food: Food,
    onFoodClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    val effectivePrice = food.discountPrice ?: food.price

    Card(
        onClick = onFoodClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Food Image
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = ImageUtils.resolveImageModel(food.imageUrl),
                    contentDescription = food.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Rating overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "⭐ ${food.rating}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = "Cook: ${food.cookName}",
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", effectivePrice)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color(0xFF047857)
                    )

                    if (food.discountPrice != null) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", food.price)}",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            style = androidx.compose.ui.text.TextStyle(
                                textDecoration = TextDecoration.LineThrough
                            )
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Prep time
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFECFDF5))
                            .border(0.5.dp, Color(0xFFA7F3D0), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "⏱ ${food.preparationTimeMins}m",
                            fontSize = 11.sp,
                            color = Color(0xFF047857),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Clear "+ Add" Button with text so action is unmistakable
            Button(
                onClick = onAddToCart,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Add",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ─── AI Logic Matching ────────────────────────────────────────────────────────

private fun getAiResponse(query: String, foods: List<Food>): ChatMessage {
    val availableFoods = foods.filter { it.isAvailable }

    val budgetRegex = Regex("""under\s*\$?(\d+(?:\.\d+)?)""")
    val budgetMatch = budgetRegex.find(query)
    val budget = budgetMatch?.groupValues?.get(1)?.toDoubleOrNull()

    val matchedByKeyword = when {
        query.contains("healthy") || query.contains("diet") || query.contains("salad") || query.contains("light") ->
            availableFoods.filter { it.category.equals("Healthy", true) || it.calories < 450 }
        query.contains("fast") || query.contains("quick") ->
            availableFoods.filter { it.preparationTimeMins <= 15 }
        query.contains("biryani") || query.contains("rice") ->
            availableFoods.filter { it.category.equals("Biryani", true) }
        query.contains("snack") || query.contains("fries") ->
            availableFoods.filter { it.category.equals("Snacks", true) }
        query.contains("bakery") || query.contains("dessert") || query.contains("sweet") || query.contains("cake") ->
            availableFoods.filter { it.category.equals("Bakery", true) }
        query.contains("spicy") || query.contains("taco") || query.contains("curry") ->
            availableFoods.filter { it.category.equals("Fast Food", true) || it.title.contains("Curry", true) }
        query.contains("top") || query.contains("best") || query.contains("popular") || query.contains("rated") ->
            availableFoods.sortedByDescending { it.rating }.take(4)
        else -> emptyList()
    }

    val baseList = if (matchedByKeyword.isNotEmpty()) matchedByKeyword else availableFoods
    val filtered = if (budget != null) {
        baseList.filter { (it.discountPrice ?: it.price) <= budget }
    } else {
        baseList
    }

    val results = filtered.sortedByDescending { it.rating }.take(4)

    val responseText = when {
        results.isEmpty() && budget != null ->
            "😔 I couldn't find available meals under **\$${String.format(Locale.US, "%.0f", budget)}** right now. Try a higher budget or ask for our top favorites!"
        results.isEmpty() ->
            "🤔 I couldn't find an exact match, but here are our **top-rated dishes** you might like!"
        budget != null && matchedByKeyword.isNotEmpty() ->
            "🎯 Here are the **best dishes under \$${String.format(Locale.US, "%.0f", budget)}** matching your taste! Hand-crafted by our home chefs 👨‍🍳"
        budget != null ->
            "💰 Great value! Here are delicious meals **under \$${String.format(Locale.US, "%.0f", budget)}** ready for you right now 😋"
        matchedByKeyword.isNotEmpty() ->
            "✨ Found great options for you! Here are our **top recommendations** based on your request 🍽️"
        else ->
            "⭐ Here are our **most-loved meals** today! You can ask by price, prep time, or cuisine 🍲"
    }

    val finalResults = if (results.isEmpty()) {
        availableFoods.sortedByDescending { it.rating }.take(3)
    } else results

    return ChatMessage(
        id = System.currentTimeMillis().toString(),
        text = responseText,
        isFromUser = false,
        suggestedFoods = finalResults
    )
}

// ─── Markdown Text Builder ───────────────────────────────────────────────────

private fun buildAnnotatedString(parts: List<String>): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857)
                    )
                ) { append(part) }
            } else {
                val italicParts = part.split("*")
                italicParts.forEachIndexed { i, p ->
                    if (i % 2 == 1) {
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                        ) { append(p) }
                    } else {
                        append(p)
                    }
                }
            }
        }
    }
}
