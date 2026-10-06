package com.example.taskflow.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import com.example.taskflow.R
import com.example.taskflow.data.remote.AuthState
import com.example.taskflow.ui.components.AddEditTaskSheet
import com.example.taskflow.ui.components.EmptyState
import com.example.taskflow.ui.components.FilterBar
import com.example.taskflow.ui.components.ManageCategoriesSheet
import com.example.taskflow.ui.components.StatsCard
import com.example.taskflow.ui.components.TaskItem
import com.example.taskflow.ui.screens.AuthScreen
import com.example.taskflow.ui.screens.ProfileScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val addEditSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val manageCatsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    // Listen to transient snackbar messages
    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        val action = uiState.snackbarActionLabel
        if (msg != null) {
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = action,
                duration = if (action != null) SnackbarDuration.Short else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undo()
            }
            viewModel.clearSnackbar()
        }
    }

    // Authentication State Gate
    if (uiState.authState is AuthState.Loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "TaskFlow",
                    modifier = Modifier.size(96.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Connecting to TaskFlow Cloud...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    if (!uiState.isAuthenticated) {
        AuthScreen(
            uiState = uiState,
            onGoogleSignInSuccess = { idToken ->
                viewModel.signInWithGoogle(idToken)
            },
            onSendPhoneOtp = { phone, activity ->
                viewModel.sendPhoneOtp(phone, activity)
            },
            onVerifyPhoneOtp = { otp ->
                viewModel.verifyPhoneOtp(otp)
            },
            onResetPhoneAuth = {
                viewModel.resetPhoneAuthState()
            },
            onSetGoogleLoading = { loading ->
                viewModel.setGoogleAuthLoading(loading)
            }
        )
        return
    }

    // Profile Screen Navigation
    if (uiState.isProfileOpen) {
        ProfileScreen(
            uiState = uiState,
            onBack = { viewModel.closeProfile() },
            onUpdateName = { viewModel.setUserName(it) },
            onThemeChange = { viewModel.setThemeMode(it) },
            onOpenManageCategories = { viewModel.openManageCategories() },
            onSignOut = { viewModel.signOut() },
            onUpdatePhoto = { viewModel.updateProfilePhoto(it) }
        )

        // Manage Categories Modal Sheet (accessible from Profile)
        if (uiState.isManageCategoriesOpen) {
            ManageCategoriesSheet(
                sheetState = manageCatsSheetState,
                categories = uiState.categories,
                onDismiss = { viewModel.closeManageCategories() },
                onAddCategory = { name, color, icon ->
                    viewModel.addCustomCategory(name, color, icon)
                },
                onUpdateCategory = { cat, name, color, icon ->
                    viewModel.updateCustomCategory(cat, name, color, icon)
                },
                onDeleteCategory = { cat ->
                    viewModel.deleteCustomCategory(cat)
                },
                onCheckUsage = { name, callback ->
                    viewModel.checkCategoryUsage(name, callback)
                }
            )
        }
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TaskFlow",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    // Undo Button
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = uiState.canUndo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (uiState.canUndo) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    // Redo Button
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = uiState.canRedo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (uiState.canRedo) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    // Search Button
                    IconButton(onClick = { viewModel.toggleSearch(!uiState.isSearchOpen) }) {
                        Icon(
                            imageVector = if (uiState.isSearchOpen) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (uiState.isSearchOpen) "Close search" else "Search tasks",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Profile Avatar Entry Point
                    val topBarAvatarBitmap = remember(uiState.profilePhotoPath) {
                        uiState.profilePhotoPath?.let { path ->
                            try {
                                val file = java.io.File(path)
                                if (file.exists()) android.graphics.BitmapFactory.decodeFile(file.absolutePath) else null
                            } catch (_: Exception) { null }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { viewModel.openProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (topBarAvatarBitmap != null) {
                            Image(
                                bitmap = topBarAvatarBitmap.asImageBitmap(),
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = uiState.userName.firstOrNull()?.uppercase() ?: "P",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddTask() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add new task",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input Field (when toggled open)
            AnimatedVisibility(
                visible = uiState.isSearchOpen,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search by title or description...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Stats and Filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                StatsCard(
                    totalCount = uiState.totalCount,
                    completedCount = uiState.completedCount,
                    progress = uiState.completionProgress
                )

                Spacer(modifier = Modifier.height(14.dp))

                FilterBar(
                    currentFilter = uiState.currentFilter,
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onFilterChange = { viewModel.setFilter(it) },
                    onCategoryChange = { viewModel.setCategory(it) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Tasks counter header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.filteredTasks.size} ${if (uiState.filteredTasks.size == 1) "task" else "tasks"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Task List or Empty State
            if (uiState.filteredTasks.isEmpty()) {
                EmptyState(
                    isSearchingOrFiltered = uiState.searchQuery.isNotEmpty() ||
                            uiState.currentFilter != TaskFilter.ALL ||
                            uiState.selectedCategory != null,
                    onAddTaskClick = { viewModel.openAddTask() },
                    modifier = Modifier.weight(1f)
                )

                // Motivational quote on empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 84.dp, top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "“Small steps. Clear goals. Real progress.”",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = uiState.filteredTasks,
                        key = { it.id }
                    ) { task ->
                        TaskItem(
                            task = task,
                            categories = uiState.categories,
                            onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                            onEdit = { viewModel.openEditTask(task) },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }

                    // Motivational Quote item
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 18.dp, bottom = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "“Small steps. Clear goals. Real progress.”",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Spacer at bottom so FAB doesn't obscure the last task
                    item {
                        Spacer(modifier = Modifier.height(84.dp))
                    }
                }
            }
        }

        // Add/Edit Bottom Sheet
        if (uiState.isAddEditSheetOpen) {
            AddEditTaskSheet(
                sheetState = addEditSheetState,
                editingTask = uiState.editingTask,
                categories = uiState.categories,
                onDismiss = { viewModel.dismissAddEditSheet() },
                onSave = { title, description, category, priority, dueDate ->
                    viewModel.saveTask(title, description, category, priority, dueDate)
                }
            )
        }

        // Manage Categories Modal Sheet
        if (uiState.isManageCategoriesOpen) {
            ManageCategoriesSheet(
                sheetState = manageCatsSheetState,
                categories = uiState.categories,
                onDismiss = { viewModel.closeManageCategories() },
                onAddCategory = { name, color, icon ->
                    viewModel.addCustomCategory(name, color, icon)
                },
                onUpdateCategory = { cat, name, color, icon ->
                    viewModel.updateCustomCategory(cat, name, color, icon)
                },
                onDeleteCategory = { cat ->
                    viewModel.deleteCustomCategory(cat)
                },
                onCheckUsage = { name, callback ->
                    viewModel.checkCategoryUsage(name, callback)
                }
            )
        }
    }
}
