package com.example.cortex_camping.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.cortex_camping.BuildConfig
import com.example.cortex_camping.model.AppNotification
import com.example.cortex_camping.model.NotificationTarget
import com.example.cortex_camping.ui.components.CortexBottomBar
import com.example.cortex_camping.ui.components.CortexButton
import com.example.cortex_camping.ui.components.CortexButtonStyle
import com.example.cortex_camping.ui.components.InfoDialog
import com.example.cortex_camping.ui.components.LocalRoleSwitcher
import com.example.cortex_camping.ui.components.OwnerBottomBar
import com.example.cortex_camping.ui.components.OwnerTab
import com.example.cortex_camping.ui.components.TopLevelTab
import com.example.cortex_camping.ui.screens.BrowseScreen
import com.example.cortex_camping.ui.screens.CampsiteDetailsScreen
import com.example.cortex_camping.ui.screens.ChatScreen
import com.example.cortex_camping.ui.screens.ChooseDatesDialog
import com.example.cortex_camping.ui.screens.EditProfileScreen
import com.example.cortex_camping.ui.screens.LotteriesScreen
import com.example.cortex_camping.ui.screens.LotteryActions
import com.example.cortex_camping.ui.screens.NotificationsScreen
import com.example.cortex_camping.ui.screens.OwnerChatScreen
import com.example.cortex_camping.ui.screens.ProfileScreen
import com.example.cortex_camping.ui.screens.RegisterScreen
import com.example.cortex_camping.ui.screens.ReservationsScreen
import com.example.cortex_camping.ui.screens.admin.AdminCampsitesScreen
import com.example.cortex_camping.ui.screens.admin.AdminDashboardScreen
import com.example.cortex_camping.ui.screens.admin.AdminImagesScreen
import com.example.cortex_camping.ui.screens.admin.AdminLogsScreen
import com.example.cortex_camping.ui.screens.admin.AdminUsersScreen
import com.example.cortex_camping.ui.screens.owner.CampsiteFormScreen
import com.example.cortex_camping.ui.screens.owner.LotteryFormScreen
import com.example.cortex_camping.ui.screens.owner.OwnerCampsitesScreen
import com.example.cortex_camping.ui.screens.owner.OwnerLotteriesScreen
import com.example.cortex_camping.ui.screens.owner.OwnerNotificationsScreen
import com.example.cortex_camping.ui.screens.owner.OwnerReservationsScreen
import com.example.cortex_camping.ui.theme.CortexTheme
import com.example.cortex_camping.viewmodel.CortexViewModel
import com.example.cortex_camping.viewmodel.Role
import kotlinx.coroutines.launch

object Routes {
    const val REGISTER = "register"
    const val BROWSE = "browse"
    const val RESERVATIONS = "reservations?choose={choose}"
    const val LOTTERIES = "lotteries"
    const val CAMPSITE = "campsite/{id}?tab={tab}"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "profile/edit"
    const val NOTIFICATIONS = "notifications"
    const val CHAT = "chat/{campsiteId}"

    const val OWNER_CAMPSITES = "owner/campsites"
    const val OWNER_CAMPSITE_FORM = "owner/campsite?id={id}"
    const val OWNER_RESERVATIONS = "owner/reservations"
    const val OWNER_LOTTERIES = "owner/lotteries"
    const val OWNER_LOTTERY_FORM = "owner/lottery?id={id}"
    const val OWNER_NOTIFICATIONS = "owner/notifications"
    const val OWNER_CHAT = "owner/chat/{campsiteId}/{entrantId}"

    const val ADMIN = "admin"
    const val ADMIN_CAMPSITES = "admin/campsites"
    const val ADMIN_USERS = "admin/users"
    const val ADMIN_IMAGES = "admin/images"
    const val ADMIN_LOGS = "admin/logs"

    fun reservations(chooseLotteryId: String? = null) = "reservations" + (chooseLotteryId?.let { "?choose=$it" } ?: "")
    fun campsite(id: String, lotteryTab: Boolean = false) = "campsite/$id?tab=${if (lotteryTab) 1 else 0}"
    fun chat(campsiteId: String) = "chat/$campsiteId"
    fun ownerCampsiteForm(id: String? = null) = "owner/campsite" + (id?.let { "?id=$it" } ?: "")
    fun ownerLotteryForm(id: String? = null) = "owner/lottery" + (id?.let { "?id=$it" } ?: "")
    fun ownerChat(campsiteId: String, entrantId: String) = "owner/chat/$campsiteId/$entrantId"
}

private fun homeFor(role: Role, registered: Boolean) = when (role) {
    Role.User -> if (registered) Routes.BROWSE else Routes.REGISTER
    Role.Owner -> Routes.OWNER_CAMPSITES
    Role.Admin -> Routes.ADMIN
}

private fun NavHostController.enterRole(role: Role, registered: Boolean) {
    navigate(homeFor(role, registered)) { popUpTo(0) { inclusive = true } }
}

private fun tabFor(route: String?): TopLevelTab? = when {
    route == null -> null
    route.startsWith("browse") -> TopLevelTab.Browse
    route.startsWith("reservations") -> TopLevelTab.Reservations
    route.startsWith("lotteries") -> TopLevelTab.Lotteries
    else -> null
}

private fun NavHostController.switchTab(tab: TopLevelTab) {
    val route = when (tab) {
        TopLevelTab.Browse -> Routes.BROWSE
        TopLevelTab.Reservations -> Routes.reservations()
        TopLevelTab.Lotteries -> Routes.LOTTERIES
    }
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun ownerTabFor(route: String?): OwnerTab? = when (route) {
    Routes.OWNER_CAMPSITES -> OwnerTab.Campsites
    Routes.OWNER_RESERVATIONS -> OwnerTab.Reservations
    Routes.OWNER_LOTTERIES -> OwnerTab.Lotteries
    else -> null
}

private fun NavHostController.switchOwnerTab(tab: OwnerTab) {
    val route = when (tab) {
        OwnerTab.Campsites -> Routes.OWNER_CAMPSITES
        OwnerTab.Reservations -> Routes.OWNER_RESERVATIONS
        OwnerTab.Lotteries -> Routes.OWNER_LOTTERIES
    }
    navigate(route) {
        popUpTo(Routes.OWNER_CAMPSITES) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun Context.openExternal(uri: String): Boolean =
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri))) }.isSuccess

@Composable
fun CortexApp(vm: CortexViewModel = viewModel()) {
    if (!vm.isLoaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    // Decided once, when the profiles first load. Later role changes navigate explicitly.
    val startDestination = remember {
        vm.role = vm.startingRole()
        homeFor(vm.role, vm.isRegistered)
    }
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showUserBar = tabFor(route) != null || route in setOf(Routes.CAMPSITE, Routes.PROFILE, Routes.EDIT_PROFILE, Routes.NOTIFICATIONS)
    val showOwnerBar = route in setOf(Routes.OWNER_CAMPSITES, Routes.OWNER_RESERVATIONS, Routes.OWNER_LOTTERIES, Routes.OWNER_NOTIFICATIONS)
    var showRolePicker by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    fun toast(msg: String) { scope.launch { snackbar.showSnackbar(msg) } }
    LaunchedEffect(vm.adminError) {
        vm.adminError?.let { snackbar.showSnackbar(it) }
    }
    fun shareCsv(list: List<AppNotification>) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Cortex Camping notifications")
            putExtra(Intent.EXTRA_TEXT, vm.notificationsCsv(list))
        }
        context.startActivity(Intent.createChooser(send, "Export notifications"))
    }

    Scaffold(
        containerColor = CortexTheme.colors.surfaceBase,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            when {
                showUserBar && vm.role != Role.Admin -> CortexBottomBar(selected = tabFor(route), onSelect = { nav.switchTab(it) })
                showOwnerBar -> OwnerBottomBar(selected = ownerTabFor(route), onSelect = { nav.switchOwnerTab(it) })
            }
        },
    ) { inner ->
      CompositionLocalProvider(LocalRoleSwitcher provides if (BuildConfig.DEBUG) ({ showRolePicker = true }) else null) {
        NavHost(
            navController = nav,
            startDestination = startDestination,
            modifier = Modifier.padding(bottom = inner.calculateBottomPadding()),
        ) {
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegister = { name, email, phone ->
                        vm.register(name, email, phone)
                        nav.navigate(Routes.BROWSE) { popUpTo(Routes.REGISTER) { inclusive = true } }
                    },
                    onPolicy = { /* TODO: open the policy & guidelines page */ },
                    onEditAvatar = { /* TODO: pick and upload a profile picture */ },
                )
            }
            composable(Routes.BROWSE) {
                BrowseScreen(
                    campsites = vm.campsites,
                    unreadNotifications = vm.unreadCount,
                    onCampsiteClick = { nav.navigate(Routes.campsite(it.campsiteId)) },
                    onProfileClick = { nav.navigate(Routes.PROFILE) },
                    onNotificationsClick = { nav.navigate(Routes.NOTIFICATIONS) },
                )
            }
            composable(
                Routes.RESERVATIONS,
                arguments = listOf(navArgument("choose") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { entry ->
                var chooseFor by rememberSaveable { mutableStateOf(entry.arguments?.getString("choose")) }
                ReservationsScreen(
                    upcoming = vm.upcomingReservations,
                    past = vm.pastReservations,
                    offers = vm.bookingOffers,
                    offerCampsite = vm::offerCampsite,
                    campsite = vm::campsite,
                    unreadNotifications = vm.unreadCount,
                    onProfileClick = { nav.navigate(Routes.PROFILE) },
                    onNotificationsClick = { nav.navigate(Routes.NOTIFICATIONS) },
                    onMessageOwner = { nav.navigate(Routes.chat(it.campsiteId)) },
                    onCancel = { vm.cancelReservation(it); toast("Reservation cancelled") },
                    onChooseDates = { chooseFor = it.id },
                    onDecline = { vm.declineOffer(it); toast("Offer declined") },
                    onRate = { r, stars -> vm.rate(r, stars) },
                )
                chooseFor?.let { id ->
                    val lottery = vm.bookingOffers.firstOrNull { it.id == id }
                    val site = lottery?.let(vm::offerCampsite)
                    if (lottery == null || site == null) chooseFor = null
                    else ChooseDatesDialog(
                        lottery = lottery, campsite = site, taken = vm.takenDates(site),
                        onConfirm = { s, e -> vm.chooseDates(lottery, s, e); chooseFor = null; toast("Dates confirmed") },
                        onDismiss = { chooseFor = null },
                    )
                }
            }
            composable(Routes.LOTTERIES) {
                LotteriesScreen(
                    lotteries = vm.myLotteries,
                    userId = vm.deviceId,
                    campsite = vm::campsite,
                    unreadNotifications = vm.unreadCount,
                    onProfileClick = { nav.navigate(Routes.PROFILE) },
                    onNotificationsClick = { nav.navigate(Routes.NOTIFICATIONS) },
                    onLeave = { vm.leaveLottery(it); toast("You left the lottery") },
                )
            }
            composable(
                Routes.CAMPSITE,
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("tab") { type = NavType.IntType; defaultValue = 0 },
                ),
                deepLinks = listOf(navDeepLink { uriPattern = "cortexcamping://campsite/{id}" }),
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                val site = vm.campsite(id)
                if (site == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    return@composable
                }
                CampsiteDetailsScreen(
                    campsite = site,
                    hostName = vm.ownerName(site.ownerId),
                    lotteries = vm.lotteriesFor(id),
                    userId = vm.deviceId,
                    actions = LotteryActions(
                        enter = { vm.enterLottery(it, id); toast("Entered ${it.name}") },
                        leave = { vm.leaveLottery(it) },
                        joinWaitlist = { vm.joinWaitlist(it, id); toast("Joined the waitlist") },
                        leaveWaitlist = { vm.leaveWaitlist(it); toast("Left the waitlist") },
                    ),
                    onBack = { nav.popBackStack() },
                    onOpenMap = {
                        if (!context.openExternal("geo:0,0?q=" + Uri.encode(site.location.ifBlank { site.name }))) toast("No map app found")
                    },
                    onOpenRules = {
                        if (site.rulesUrl.isBlank()) toast("The owner hasn't added rules yet")
                        else if (!context.openExternal(site.rulesUrl)) toast("Couldn't open ${site.rulesUrl}")
                    },
                    onMessageHost = { nav.navigate(Routes.chat(id)) },
                    onReviews = { /* TODO: show the campsite's reviews */ },
                    onLotteryDetails = { /* TODO: show the lottery's full details */ },
                    initialTab = entry.arguments?.getInt("tab") ?: 0,
                )
            }
            composable(Routes.PROFILE) {
                val entrant = vm.entrant ?: return@composable
                ProfileScreen(
                    entrant = entrant,
                    onPrefsChange = vm::updatePrefs,
                    onBack = { nav.popBackStack() },
                    onEditProfile = { nav.navigate(Routes.EDIT_PROFILE) },
                    onDeleteProfile = {
                        vm.deleteProfile()
                        nav.navigate(Routes.REGISTER) { popUpTo(0) { inclusive = true } }
                    },
                    onPolicy = { /* TODO: open the policy & guidelines page */ },
                    onHelp = { /* TODO: open the help docs */ },
                )
            }
            composable(Routes.EDIT_PROFILE) {
                val entrant = vm.entrant ?: return@composable
                EditProfileScreen(
                    entrant = entrant,
                    onSave = { name, email, phone -> vm.updateProfile(name, email, phone); nav.popBackStack(); toast("Profile updated") },
                    onCancel = { nav.popBackStack() },
                    onEditAvatar = { /* TODO: pick and upload a profile picture */ },
                )
            }
            composable(Routes.NOTIFICATIONS) {
                val notifications = vm.myNotifications
                NotificationsScreen(
                    notifications = notifications,
                    onBack = { nav.popBackStack() },
                    onMarkAllRead = { vm.markRead(notifications) },
                    onDownloadCsv = { shareCsv(notifications) },
                    onNotificationClick = { n ->
                        vm.markRead(listOf(n))
                        when (n.target) {
                            NotificationTarget.CHOOSE_DATES -> {
                                val offer = vm.bookingOffers.firstOrNull { it.entryFor(vm.deviceId)?.campsiteId == n.campsiteId }
                                nav.navigate(Routes.reservations(offer?.id))
                            }
                            NotificationTarget.CHAT -> nav.navigate(Routes.chat(n.campsiteId))
                            NotificationTarget.CAMPSITE_LOTTERY -> nav.navigate(Routes.campsite(n.campsiteId, lotteryTab = true))
                            NotificationTarget.MY_RESERVATIONS -> nav.switchTab(TopLevelTab.Reservations)
                            NotificationTarget.MY_LOTTERIES -> nav.switchTab(TopLevelTab.Lotteries)
                            NotificationTarget.BROWSE -> nav.switchTab(TopLevelTab.Browse)
                            else -> Unit
                        }
                    },
                )
            }
            // TODO: to be moved from campsite page to reservations page
            composable(Routes.CHAT, arguments = listOf(navArgument("campsiteId") { type = NavType.StringType })) { entry ->
                val id = entry.arguments?.getString("campsiteId").orEmpty()
                val site = vm.campsite(id) ?: return@composable
                ChatScreen(
                    campsite = site,
                    reservation = vm.upcomingReservations.firstOrNull { it.campsiteId == id },
                    messages = vm.conversation(id, vm.deviceId),
                    userId = vm.deviceId,
                    onSend = { vm.sendMessage(id, vm.deviceId, it) },
                    onBack = { nav.popBackStack() },
                )
            }

            composable(Routes.OWNER_CAMPSITES) {
                OwnerCampsitesScreen(
                    campsites = vm.myCampsites,
                    hasOngoingLottery = vm::hasOngoingLottery,
                    bookedDates = vm::bookedDates,
                    unread = vm.ownerUnreadCount,
                    onNotifications = { nav.navigate(Routes.OWNER_NOTIFICATIONS) },
                    onAdd = { nav.navigate(Routes.ownerCampsiteForm()) },
                    onEdit = { nav.navigate(Routes.ownerCampsiteForm(it.campsiteId)) },
                )
            }
            composable(Routes.OWNER_CAMPSITE_FORM, arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null })) { e ->
                val id = e.arguments?.getString("id")
                var saving by remember { mutableStateOf(false) }
                CampsiteFormScreen(
                    existing = id?.let(vm::campsite),
                    saving = saving,
                    onSave = { campsite ->
                        saving = true
                        vm.saveCampsite(
                            campsite,
                            onSaved = {
                                toast(if (id == null) "Campsite added" else "Changes saved")
                                // by route: the user may have already backed out while it saved
                                nav.popBackStack(Routes.OWNER_CAMPSITE_FORM, inclusive = true)
                            },
                            onError = {
                                saving = false
                                toast("Couldn't use that image. Nothing was saved, please pick a different one.")
                            },
                        )
                    },
                    onCancel = { nav.popBackStack() },
                )
            }
            composable(Routes.OWNER_RESERVATIONS) {
                OwnerReservationsScreen(
                    reservations = vm.ownerReservations,
                    campsite = vm::campsite,
                    unread = vm.ownerUnreadCount,
                    onNotifications = { nav.navigate(Routes.OWNER_NOTIFICATIONS) },
                    onMessage = { nav.navigate(Routes.ownerChat(it.reservation.campsiteId, it.reservation.userId)) },
                    onSetStatus = { item, status -> vm.setReservationStatus(item, status); toast("${item.guestName} notified") },
                )
            }
            composable(Routes.OWNER_LOTTERIES) {
                OwnerLotteriesScreen(
                    lotteries = vm.ownerLotteries,
                    campsite = vm::campsite,
                    entrantName = vm::entrantName,
                    unread = vm.ownerUnreadCount,
                    onNotifications = { nav.navigate(Routes.OWNER_NOTIFICATIONS) },
                    onCreate = { nav.navigate(Routes.ownerLotteryForm()) },
                    onUpdate = { nav.navigate(Routes.ownerLotteryForm(it.id)) },
                    onNotifyWaitlist = { vm.notifyWaitlist(it); toast("Notified ${it.waitlist.size} waitlisted users") },
                    onRunDraw = { vm.runDraw(it); toast("Draw complete") },
                )
            }
            composable(Routes.OWNER_LOTTERY_FORM, arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null })) { e ->
                val id = e.arguments?.getString("id")
                LotteryFormScreen(
                    existing = id?.let { lid -> vm.ownerLotteries.firstOrNull { it.id == lid } },
                    campsites = vm.myCampsites,
                    onSave = { vm.saveLottery(it); nav.popBackStack(); toast(if (id == null) "Lottery created" else "Lottery updated") },
                    onCancel = { nav.popBackStack() },
                )
            }
            composable(Routes.OWNER_NOTIFICATIONS) {
                val received = vm.ownerReceived
                OwnerNotificationsScreen(
                    received = received, sent = vm.ownerSent,
                    onBack = { nav.popBackStack() },
                    onMarkAllRead = { vm.markRead(received) },
                    onDownloadCsv = { shareCsv(vm.ownerSent) },
                    onClick = { n ->
                        vm.markRead(listOf(n))
                        when (n.target) {
                            NotificationTarget.CHAT ->
                                if (n.senderId.isNotBlank()) nav.navigate(Routes.ownerChat(n.campsiteId, n.senderId))
                            NotificationTarget.OWNER_RESERVATIONS -> nav.switchOwnerTab(OwnerTab.Reservations)
                            NotificationTarget.OWNER_LOTTERIES -> nav.switchOwnerTab(OwnerTab.Lotteries)
                            else -> nav.switchOwnerTab(OwnerTab.Campsites)
                        }
                    },
                )
            }
            composable(
                Routes.OWNER_CHAT,
                arguments = listOf(navArgument("campsiteId") { type = NavType.StringType }, navArgument("entrantId") { type = NavType.StringType }),
            ) { e ->
                val campsiteId = e.arguments?.getString("campsiteId").orEmpty()
                val entrantId = e.arguments?.getString("entrantId").orEmpty()
                val site = vm.campsite(campsiteId) ?: return@composable
                OwnerChatScreen(
                    guestName = vm.entrantName(entrantId),
                    campsite = site,
                    reservation = vm.ownerReservations.map { it.reservation }
                        .filter { it.campsiteId == campsiteId && it.userId == entrantId }.maxByOrNull { it.checkIn },
                    messages = vm.conversation(campsiteId, entrantId),
                    userId = vm.deviceId,
                    onSend = { vm.sendMessage(campsiteId, entrantId, it) },
                    onBack = { nav.popBackStack() },
                )
            }

            composable(Routes.ADMIN) {
                AdminDashboardScreen(
                    onCampsites = { nav.navigate(Routes.ADMIN_CAMPSITES) },
                    onUsers = { nav.navigate(Routes.ADMIN_USERS) },
                    onImages = { nav.navigate(Routes.ADMIN_IMAGES) },
                    onLogs = { nav.navigate(Routes.ADMIN_LOGS) },
                    onSpoofUser = { vm.role = Role.User; nav.navigate(homeFor(Role.User, vm.isRegistered)); toast("Viewing as camper") },
                    onSpoofOwner = { vm.role = Role.Owner; vm.ensureOwner(); nav.navigate(Routes.OWNER_CAMPSITES); toast("Viewing as owner") },
                )
            }
            composable(Routes.ADMIN_CAMPSITES) {
                AdminCampsitesScreen(
                    campsites = vm.campsites,
                    ownerName = vm::ownerName,
                    onBack = { nav.popBackStack() },
                    onView = { nav.navigate(Routes.campsite(it.campsiteId)) },
                    onDelete = { campsite -> vm.deleteCampsite(campsite) { error -> toast(error ?: "${campsite.name} deleted") } },
                )
            }
            composable(Routes.ADMIN_USERS) {
                AdminUsersScreen(vm.managedUsers, onBack = { nav.popBackStack() }, onDelete = { user -> vm.deleteUser(user) { error -> toast(error ?: "${user.name} deleted") } })
            }
            composable(Routes.ADMIN_IMAGES) {
                AdminImagesScreen(vm.campsitesWithImages, vm::ownerName, onBack = { nav.popBackStack() }, onDelete = { campsite -> vm.deleteImage(campsite) { error -> toast(error ?: "Image deleted") } })
            }
            composable(Routes.ADMIN_LOGS) { AdminLogsScreen(vm.communicationLogs, onBack = { nav.popBackStack() }) }
        }
      }
    }
    if (showRolePicker) {
        RolePickerDialog(current = vm.role, onPick = { role ->
            showRolePicker = false
            vm.role = role
            if (role == Role.Owner) vm.ensureOwner()
            nav.enterRole(role, vm.isRegistered)
        }, onDismiss = { showRolePicker = false })
    }
}

@Composable
private fun RolePickerDialog(current: Role, onPick: (Role) -> Unit, onDismiss: () -> Unit) {
    InfoDialog("Switch role", onDismiss) {
        Role.entries.forEach { role ->
            CortexButton(
                role.label + if (role == current) " (current)" else "", { onPick(role) },
                Modifier.fillMaxWidth(),
                style = if (role == current) CortexButtonStyle.Filled else CortexButtonStyle.OutlinedBrand,
            )
        }
    }
}
