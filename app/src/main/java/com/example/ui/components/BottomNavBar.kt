package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.theme.DarkPill
import com.example.ui.theme.GreenPrimary

data class NavTabItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun HomeChefBottomNavBar(
    currentScreen: String,
    userRole: UserRole = UserRole.CUSTOMER,
    cartBadgeCount: Int = 0,
    favoriteBadgeCount: Int = 0,
    onNavigate: (String) -> Unit
) {
    val tabs = when (userRole) {
        UserRole.COOK -> listOf(
            NavTabItem("cook_dashboard", "Kitchen", Icons.Filled.Restaurant, Icons.Outlined.Restaurant),
            NavTabItem("orders", "Orders", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
            NavTabItem("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
        )
        UserRole.DELIVERY -> listOf(
            NavTabItem("delivery_dashboard", "Trips", Icons.Filled.TwoWheeler, Icons.Outlined.TwoWheeler),
            NavTabItem("orders", "Orders", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
            NavTabItem("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
        )
        UserRole.ADMIN -> listOf(
            NavTabItem("admin_dashboard", "Portal", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings),
            NavTabItem("orders", "Orders", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
            NavTabItem("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
        )
        UserRole.CUSTOMER -> listOf(
            NavTabItem("customer_home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
            NavTabItem("favorites", "Favorites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, favoriteBadgeCount),
            NavTabItem("chef_ai", "Chef AI", Icons.Filled.SmartToy, Icons.Outlined.SmartToy),
            NavTabItem("cart", "Cart", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart, cartBadgeCount),
            NavTabItem("orders", "Orders", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
            NavTabItem("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Color.White,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = currentScreen == tab.route

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                                if (isSelected) GreenPrimary.copy(alpha = 0.12f) else Color.Transparent
                            )
                        .clickable { onNavigate(tab.route) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                tint = if (isSelected) GreenPrimary else Color(0xFF737880),
                                modifier = Modifier.size(24.dp)
                            )
                            if (tab.badgeCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-4).dp)
                                        .background(Color(0xFFFF4B4B), CircleShape)
                                        .size(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tab.badgeCount.toString(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.title,
                                color = GreenPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
