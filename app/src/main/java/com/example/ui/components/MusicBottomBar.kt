package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MediumGray
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite

/**
 * 4-Tab Bottom Navigation matching designv2-example.png:
 * 0: Home / Discover
 * 1: Search
 * 2: Library
 * 3: Profile / Settings
 */
@Composable
fun MusicBottomBar(
    currentTab: Int,
    queueCount: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("music_bottom_bar"),
        containerColor = PureWhite,
        tonalElevation = 0.dp
    ) {
        // Tab 0: Home
        NavigationBarItem(
            selected = currentTab == 0,
            onClick = { onTabSelected(0) },
            icon = {
                Icon(
                    imageVector = if (currentTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Home"
                )
            },
            label = {
                Text(
                    text = "Home",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Medium
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ObsidianBlack,
                selectedTextColor = ObsidianBlack,
                indicatorColor = Color.Transparent,
                unselectedIconColor = MediumGray,
                unselectedTextColor = MediumGray
            ),
            modifier = Modifier.testTag("nav_home")
        )

        // Tab 1: Search
        NavigationBarItem(
            selected = currentTab == 1,
            onClick = { onTabSelected(1) },
            icon = {
                Icon(
                    imageVector = if (currentTab == 1) Icons.Filled.Search else Icons.Outlined.Search,
                    contentDescription = "Search"
                )
            },
            label = {
                Text(
                    text = "Search",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Medium
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ObsidianBlack,
                selectedTextColor = ObsidianBlack,
                indicatorColor = Color.Transparent,
                unselectedIconColor = MediumGray,
                unselectedTextColor = MediumGray
            ),
            modifier = Modifier.testTag("nav_search")
        )

        // Tab 2: Library
        NavigationBarItem(
            selected = currentTab == 2,
            onClick = { onTabSelected(2) },
            icon = {
                Icon(
                    imageVector = if (currentTab == 2) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                    contentDescription = "Library"
                )
            },
            label = {
                Text(
                    text = "Library",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Medium
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ObsidianBlack,
                selectedTextColor = ObsidianBlack,
                indicatorColor = Color.Transparent,
                unselectedIconColor = MediumGray,
                unselectedTextColor = MediumGray
            ),
            modifier = Modifier.testTag("nav_library")
        )

        // Tab 3: Profile
        NavigationBarItem(
            selected = currentTab == 3,
            onClick = { onTabSelected(3) },
            icon = {
                Icon(
                    imageVector = if (currentTab == 3) Icons.Filled.Person else Icons.Outlined.Person,
                    contentDescription = "Profile"
                )
            },
            label = {
                Text(
                    text = "Profile",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Medium
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ObsidianBlack,
                selectedTextColor = ObsidianBlack,
                indicatorColor = Color.Transparent,
                unselectedIconColor = MediumGray,
                unselectedTextColor = MediumGray
            ),
            modifier = Modifier.testTag("nav_profile")
        )
    }
}
