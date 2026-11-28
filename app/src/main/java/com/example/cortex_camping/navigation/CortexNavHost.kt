package com.example.cortex_camping.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
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
import com.example.cortex_camping.model.Campsite
import com.example.cortex_camping.model.Lottery
import com.example.cortex_camping.model.AppNotification
import com.example.cortex_camping.model.NotificationTarget
import com.example.cortex_camping.ui.components.CortexBottomBar
import com.example.cortex_camping.ui.components.CortexButton
import com.example.cortex_camping.ui.components.CortexButtonSize
import com.example.cortex_camping.ui.components.CortexButtonStyle
import com.example.cortex_camping.ui.components.InfoDialog
import com.example.cortex_camping.ui.components.LocalRoleSwitcher
import com.example.cortex_camping.ui.components.OwnerBottomBar
import com.example.cortex_camping.ui.components.OwnerTab
import com.example.cortex_camping.ui.components.TopLevelTab
import com.example.cortex_camping.ui.screens.WelcomeScreen
import com.example.cortex_camping.ui.screens.LoginRoleScreen
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
import com.example.cortex_camping.ui.screens.owner.OwnerCampsiteDetailsScreen
import com.example.cortex_camping.ui.screens.owner.OwnerCampsitesScreen
import com.example.cortex_camping.ui.screens.owner.OwnerLotteriesScreen
import com.example.cortex_camping.ui.screens.owner.SharedLotteryActions
import com.example.cortex_camping.ui.screens.owner.OwnerNotificationsScreen
import com.example.cortex_camping.ui.screens.owner.OwnerReservationsScreen
import com.example.cortex_camping.ui.theme.CortexTheme
import com.example.cortex_camping.viewmodel.CortexViewModel
import com.example.cortex_camping.viewmodel.Role
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

object Routes {
    const val WELCOME = "welcome"
    const val LOGIN_ROLE = "login_role"
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
    const val OWNER_CAMPSITE_DETAILS = "owner/campsite/details/{id}"
    const val OWNER_RESERVATIONS = "owner/reservations"
    const val OWNER_LOTTERIES = "owner/lotteries"
    const val OWNER_LOTTERY_FORM = "owner/lottery?id={id}"
    const val OWNER_NOTIFICATIONS = "owner/notifications"
    const val OWNER_CHAT = "owner/chat/{campsiteId}/{entrantId}"

    const val ADMIN = "admin"
    const val ADMIN_CAMPSITES = "admin/campsites"
    const val ADMIN_USERS = "admin/users"
    const val ADMIN_IMAGES = "admin/images"
    const val ADMIN_REVIEWS = "admin/reviews"
    const val ADMIN_LOGS = "admin/logs"

    fun reservations(chooseLotteryId: String? = null) = "reservations" + (chooseLotteryId?.let { "?choose=$it" } ?: "")
    fun campsite(id: String, lotteryTab: Boolean = false) = "campsite/$id?tab=${if (lotteryTab) 1 else 0}"
    fun chat(campsiteId: String) = "chat/$campsiteId"
    fun ownerCampsiteForm(id: String? = null) = "owner/campsite" + (id?.let { "?id=$it" } ?: "")
    fun ownerCampsiteDetails(id: String) = "owner/campsite/details/$id"
    fun ownerLotteryForm(id: String? = null) = "owner/lottery" + (id?.let { "?id=$it" } ?: "")
    fun ownerChat(campsiteId: String, entrantId: String) = "owner/chat/$campsiteId/$entrantId"
}

private fun homeFor(role: Role, registered: Boolean) = when (role) {
    Role.User -> if (registered) Routes.BROWSE else Routes.WELCOME
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
fun CortexApp(
    vm: CortexViewModel = viewModel(),
    tappedNotification: AppNotification? = null, // the push notification that opened the app
    onTappedNotificationShown: () -> Unit = {},
) {
    if (!vm.isLoaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val startDestination = Routes.WELCOME // app defaults to the Welcome page on app launch

    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showUserBar = tabFor(route) != null || route in setOf(Routes.CAMPSITE, Routes.PROFILE, Routes.EDIT_PROFILE, Routes.NOTIFICATIONS)
    val showOwnerBar = route in setOf(Routes.OWNER_CAMPSITES, Routes.OWNER_RESERVATIONS, Routes.OWNER_LOTTERIES, Routes.OWNER_NOTIFICATIONS)
    var showRolePicker by remember { mutableStateOf(false) }
    // true while an owner with no camper profile (e.g. they deleted it) is registering one from role select
    var registeringCamper by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    fun toast(msg: String) { scope.launch { snackbar.showSnackbar(msg) } }
    fun enterAs(role: Role) {
        if (role == Role.User && !vm.isRegistered && !vm.hasAdminProfile) {
            // no camper profile to use yet, so make one first
            registeringCamper = true
            nav.navigate(Routes.REGISTER)
            return
        }
        vm.role = role
        when (role) {
            Role.Owner -> vm.ensureOwner()
            Role.User -> vm.ensureEntrant()
            Role.Admin -> Unit
        }
        nav.enterRole(role, vm.isRegistered || vm.hasAdminProfile)
    }
    fun openMap(site: Campsite) {
        if (!context.openExternal("geo:0,0?q=" + Uri.encode(site.location.ifBlank { site.name }))) toast("No map app found")
    }
    fun openRules(site: Campsite) {
        if (site.rulesUrl.isBlank()) toast("The owner hasn't added rules yet")
        else if (!context.openExternal(site.rulesUrl)) toast("Couldn't open ${site.rulesUrl}")
    }
    LaunchedEffect(vm.adminError) {
        vm.adminError?.let { snackbar.showSnackbar(it) }
    }
    // the CSV waiting to be written once the user picks where to save it
    var pendingCsv by rememberSaveable { mutableStateOf("") }
    val saveCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(("\uFEFF" + pendingCsv).toByteArray()) }
                toast("Notifications saved")
            } catch (e: Exception) {
                toast("Couldn't save the CSV file")
            }
        }
    }
    // asks where to save the notifications, then writes them there as a CSV file
    fun downloadCsv(list: List<AppNotification>) {
        pendingCsv = vm.notificationsCsv(list)
        saveCsv.launch("cortex-notifications.csv")
    }

    // goes where a notification's link points, from a feed or from a push
    fun openNotification(n: AppNotification, asOwner: Boolean = n.forOwner) {
        vm.markRead(listOf(n))
        if (asOwner) {
            when (n.target) {
                NotificationTarget.CHAT ->
                    if (n.senderId.isNotBlank()) nav.navigate(Routes.ownerChat(n.campsiteId, n.senderId))
                NotificationTarget.OWNER_RESERVATIONS -> nav.switchOwnerTab(OwnerTab.Reservations)
                NotificationTarget.OWNER_LOTTERIES -> nav.switchOwnerTab(OwnerTab.Lotteries)
                else -> nav.switchOwnerTab(OwnerTab.Campsites)
            }
            return
        }
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
    }
    // a tapped push opens the feed it belongs to, then its link
    LaunchedEffect(tappedNotification) {
        val n = tappedNotification ?: return@LaunchedEffect
        val role = if (n.forOwner) Role.Owner else Role.User
        if (vm.role != role) enterAs(role)
        if (role == Role.Owner || vm.isRegistered || vm.hasAdminProfile) {
            nav.navigate(if (n.forOwner) Routes.OWNER_NOTIFICATIONS else Routes.NOTIFICATIONS)
            openNotification(n)
        }
        onTappedNotificationShown()
    }

    Scaffold(
        containerColor = CortexTheme.colors.surfaceBase,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            val userBar = showUserBar && vm.role != Role.Admin
            Column {
                if (vm.hasAdminProfile && vm.role != Role.Admin && (userBar || showOwnerBar)) {
                    AdminModeBar(role = vm.role, onBackToAdmin = { enterAs(Role.Admin) })
                }
                when {
                    userBar -> CortexBottomBar(selected = tabFor(route), onSelect = { nav.switchTab(it) })
                    showOwnerBar -> OwnerBottomBar(selected = ownerTabFor(route), onSelect = { nav.switchOwnerTab(it) })
                }
            }
        },
    ) { inner ->
      CompositionLocalProvider(LocalRoleSwitcher provides if (BuildConfig.DEBUG || vm.hasAdminProfile) ({ showRolePicker = true }) else null) {
        NavHost(
            navController = nav,
            startDestination = startDestination,
            modifier = Modifier.padding(bottom = inner.calculateBottomPadding()),
        ) {
            composable(Routes.WELCOME) {
                WelcomeScreen(
                    hasAccount = vm.canLogIn,
                    onRegisterClick = { nav.navigate(Routes.REGISTER) },
                    onLoginClick = {
                        nav.navigate(Routes.LOGIN_ROLE) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegister = { name, email, phone, image ->
                        vm.register(name, email, phone, image, onImageError = { toast(it) })
                        if (registeringCamper) {
                            // they already picked Camper on role select, so go straight in
                            registeringCamper = false
                            vm.role = Role.User
                            nav.enterRole(Role.User, registered = true)
                        } else {
                            // new accounts pick their role, just like logging in
                            nav.navigate(Routes.LOGIN_ROLE) { popUpTo(Routes.WELCOME) { inclusive = true } }
                        }
                    },
                )
            }
            composable(Routes.LOGIN_ROLE) {
                LoginRoleScreen(
                    roles = vm.availableRoles,
                    onSelectRole = { selectedRole -> enterAs(selectedRole) },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.BROWSE) {
                BrowseScreen(
                    currentRole = vm.role,
                    onRoleSwitch = { enterAs(it) },
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
                var campsiteFor by rememberSaveable { mutableStateOf<String?>(null) } // set once a winner picks a campsite
                ReservationsScreen(
                    currentRole = vm.role,
                    onRoleSwitch = { enterAs(it) },
                    upcoming = vm.upcomingReservations,
                    past = vm.pastReservations,
                    offers = vm.bookingOffers,
                    offerCampsite = vm::offerCampsite,
                    campsite = vm::campsite,
                    unreadNotifications = vm.unreadCount,
                    onProfileClick = { nav.navigate(Routes.PROFILE) },
                    onNotificationsClick = { nav.navigate(Routes.NOTIFICATIONS) },
                    onMessageOwner = { nav.navigate(Routes.chat(it.campsiteId)) },
                    onCancel = { vm.cancelReservation(it) { error -> toast(error ?: "Reservation cancelled") } },
                    onChooseDates = { chooseFor = it.id },
                    onDecline = { vm.declineOffer(it) { error -> toast(error ?: "Offer declined") } },
                    onRate = { r, stars -> vm.rate(r, stars) { error -> toast(error ?: "Thanks for rating your stay") } },
                    )
                chooseFor?.let { id ->
                    val lottery = vm.bookingOffers.firstOrNull { it.id == id }
                    // a multi-campsite win asks which campsite first; a single-campsite lottery skips that step
                    val siteId = lottery?.let { campsiteFor ?: it.campsiteIds.singleOrNull() }
                    val site = siteId?.let(vm::campsite)
                    when {
                        lottery == null -> { chooseFor = null; campsiteFor = null }
                        siteId == null -> CampsitePickerDialog(
                            lottery = lottery,
                            campsite = vm::campsite,
                            openStays = { vm.openCheckIns(lottery, it).size },
                            onPick = { campsiteFor = it.campsiteId },
                            onDismiss = { chooseFor = null },
                        )
                        site == null -> { chooseFor = null; campsiteFor = null }
                        else -> ChooseDatesDialog(
                            lottery = lottery, campsite = site, taken = vm.takenDates(site),
                            onConfirm = { s, e ->
                                vm.chooseDates(lottery, site.campsiteId, s, e) { error -> toast(error ?: "Dates confirmed") }
                                chooseFor = null; campsiteFor = null
                            },
                            onDismiss = { chooseFor = null; campsiteFor = null },
                        )
                    }
                }
            }
            composable(Routes.LOTTERIES) {
                LotteriesScreen(
                    currentRole = vm.role,
                    onRoleSwitch = { enterAs(it) },
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
                    campsiteName = { vm.campsite(it)?.name },
                    userId = vm.deviceId,
                    actions = LotteryActions(
                        enter = { l -> vm.enterLottery(l) { error -> toast(error ?: "Entered ${l.name}") } },
                        leave = { vm.leaveLottery(it) },
                        joinWaitlist = { vm.joinWaitlist(it) { error -> toast(error ?: "Joined the waitlist") } },
                        leaveWaitlist = { vm.leaveWaitlist(it); toast("Left the waitlist") },
                    ),
                    onBack = { nav.popBackStack() },
                    onOpenMap = { openMap(site) },
                    onOpenRules = { openRules(site) },
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
                        nav.navigate(Routes.WELCOME) { popUpTo(0) { inclusive = true } } // user is taken back to the welcome page after choosing to delete profile
                    },
                    onHelp = { /* TODO: open the help docs */ },
                )
            }
            composable(Routes.EDIT_PROFILE) {
                val entrant = vm.entrant ?: return@composable
                var saving by remember { mutableStateOf(false) }
                EditProfileScreen(
                    entrant = entrant,
                    saving = saving,
                    onSave = { name, email, phone, image ->
                        saving = true
                        vm.updateProfile(
                            name, email, phone, image,
                            onSaved = {
                                toast("Profile updated")
                                nav.popBackStack(Routes.EDIT_PROFILE, inclusive = true)
                            },
                            onError = { message ->
                                saving = false
                                toast(message)
                            },
                        )
                    },
                    onCancel = { nav.popBackStack() },
                )
            }
            composable(Routes.NOTIFICATIONS) {
                val notifications = vm.myNotifications
                NotificationsScreen(
                    notifications = notifications,
                    onBack = { nav.popBackStack() },
                    onMarkAllRead = { vm.markRead(notifications) },
                    onDownloadCsv = { downloadCsv(notifications) },
                    onNotificationClick = { openNotification(it) },
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
                    currentRole = vm.role,
                    onRoleSwitch = { enterAs(it) },
                    onProfileClick = { nav.navigate(Routes.PROFILE) },
                    campsites = vm.myCampsites,
                    hasOngoingLottery = vm::hasOngoingLottery,
                    bookedDates = vm::bookedDates,
                    unread = vm.ownerUnreadCount,
                    onNotifications = { nav.navigate(Routes.OWNER_NOTIFICATIONS) },
                    onAdd = { nav.navigate(Routes.ownerCampsiteForm()) },
                    onView = { nav.navigate(Routes.ownerCampsiteDetails(it.campsiteId)) },
                    onEdit = { nav.navigate(Routes.ownerCampsiteForm(it.campsiteId)) },
                )
            }
            composable(Routes.OWNER_CAMPSITE_DETAILS, arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                val id = e.arguments?.getString("id").orEmpty()
                val site = vm.campsite(id) ?: return@composable
                OwnerCampsiteDetailsScreen(
                    campsite = site,
                    bookedDates = vm.bookedDates(site),
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.ownerCampsiteForm(id)) },
                    onOpenMap = { openMap(site) },
                    onOpenRules = { openRules(site) },
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
                            onError = { message ->
                                saving = false
                                toast(message)
                            },
                        )
                    },
                    onCancel = { nav.popBackStack() },
                )
            }
            composable(Routes.OWNER_RESERVATIONS) {
                OwnerReservationsScreen(
                    currentRole = vm.role,
                    onRoleSwitch = { enterAs(it) },
                    onProfileClick = { nav.navigate(Routes.PROFILE) },
                    reservations = vm.ownerReservations,
                    campsite = vm::campsite,
                    unread = vm.ownerUnreadCount,
                    onNotifications = { nav.navigate(Routes.OWNER_NOTIFICATIONS) },
                    onMessage = { nav.navigate(Routes.ownerChat(it.reservation.campsiteId, it.reservation.userId)) },
                    onReject = { item, reason ->
                        vm.rejectReservation(item, reason) { error -> toast(error ?: "Reservation rejected. ${item.guestName} was notified.") }
                    },
                )
            }
            composable(Routes.OWNER_LOTTERIES) {
                OwnerLotteriesScreen(
                    currentRole = vm.role,
                    onRoleSwitch = { enterAs(it) },
                    onProfileClick = { nav.navigate(Routes.PROFILE) },
                    lotteries = vm.ownerLotteries,
                    invites = vm.lotteryInvites,
                    userId = vm.deviceId,
                    myCampsites = vm.myCampsites,
                    campsite = vm::campsite,
                    entrantName = vm::entrantName,
                    ownerName = vm::lotteryOwnerName,
                    unread = vm.ownerUnreadCount,
                    onNotifications = { nav.navigate(Routes.OWNER_NOTIFICATIONS) },
                    onCreate = { nav.navigate(Routes.ownerLotteryForm()) },
                    onUpdate = { nav.navigate(Routes.ownerLotteryForm(it.id)) },
                    onNotifyWaitlist = { vm.notifyWaitlist(it); toast("Notified ${it.waitlist.size} waitlisted users") },
                    onRunDraw = { vm.runDraw(it) { error -> toast(error ?: "Drawing now. Results will show in a few seconds.") } },
                    onCancel = { l -> vm.cancelLottery(l) { error -> toast(error ?: "${l.name} was cancelled") } },
                    shared = SharedLotteryActions(
                        invite = { l, email -> vm.inviteOwner(l, email) { error -> toast(error ?: "Invite sent") } },
                        respond = { l, accept ->
                            vm.respondToInvite(l, accept) { error -> toast(error ?: if (accept) "Invite accepted. Add your campsites next." else "Invite declined") }
                        },
                        setMyCampsites = { l, ids -> vm.setMyLotteryCampsites(l, ids) { error -> toast(error ?: "Campsites updated") } },
                        removeOwner = { l, ownerId ->
                            vm.removeLotteryOwner(l, ownerId) { error -> toast(error ?: if (ownerId == vm.deviceId) "You left the lottery" else "Owner removed") }
                        },
                    ),
                )
            }
            composable(Routes.OWNER_LOTTERY_FORM, arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null })) { e ->
                val id = e.arguments?.getString("id")
                var saving by remember { mutableStateOf(false) }
                LotteryFormScreen(
                    existing = id?.let { lid -> vm.ownerLotteries.firstOrNull { it.id == lid && it.isCreator(vm.deviceId) } },
                    campsites = vm.myCampsites,
                    saving = saving,
                    onSave = { lottery ->
                        if (id == null) {
                            // new lotteries sync in the background, the server might not answer until we're back online
                            vm.saveLottery(lottery) { error -> error?.let(::toast) }
                            toast("Lottery created")
                            nav.popBackStack()
                        } else {
                            saving = true
                            vm.saveLottery(lottery) { error ->
                                saving = false
                                if (error != null) {
                                    toast(error)
                                } else {
                                    toast("Lottery updated")
                                    // by route: the user may have already backed out while it saved
                                    nav.popBackStack(Routes.OWNER_LOTTERY_FORM, inclusive = true)
                                }
                            }
                        }
                    },
                    onCancel = { nav.popBackStack() },
                )
            }
            composable(Routes.OWNER_NOTIFICATIONS) {
                val received = vm.ownerReceived
                OwnerNotificationsScreen(
                    received = received, sent = vm.ownerSent,
                    onBack = { nav.popBackStack() },
                    onMarkAllRead = { vm.markRead(received) },
                    onDownloadCsv = { downloadCsv(vm.ownerSent) },
                    // the sent tab lists entrants' notifications, which still open owner screens
                    onClick = { openNotification(it, asOwner = true) },
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
                    onReviews = { nav.navigate(Routes.ADMIN_REVIEWS) },
                    onSpoofUser = { enterAs(Role.User); toast("Using your admin profile as a camper") },
                    onSpoofOwner = { enterAs(Role.Owner); toast("Using your admin profile as a campsite owner") },
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
            composable(Routes.ADMIN_REVIEWS) {
                com.example.cortex_camping.ui.screens.admin.AdminReviewsScreen(
                    reservations = vm.ratedReservations,
                    campsite = vm::campsite,
                    entrantName = vm::entrantName,
                    onBack = { nav.popBackStack() },
                    onDelete = { reservation ->
                        vm.deleteReview(reservation) { error -> toast(error ?: "Rating removed") }
                    },
                )
            }
            composable(Routes.ADMIN_LOGS) { AdminLogsScreen(vm.communicationLogs, onBack = { nav.popBackStack() }) }
        }
      }
    }
    if (showRolePicker) {
        RolePickerDialog(
            current = vm.role,
            roles = if (BuildConfig.DEBUG) Role.entries else vm.availableRoles,
            onPick = { role ->
                showRolePicker = false
                enterAs(role)
            },
            onDismiss = { showRolePicker = false },
        )
    }
}

// shown to winners of a lottery that covers several campsites: pick the one you want, the rest are released
@Composable
private fun CampsitePickerDialog(
    lottery: Lottery,
    campsite: (String) -> Campsite?,
    openStays: (Campsite) -> Int,
    onPick: (Campsite) -> Unit,
    onDismiss: () -> Unit,
) {
    InfoDialog("Choose a campsite", onDismiss) {
        Text("Pick the campsite you want. Choosing one releases your spot at the others.")
        lottery.campsiteIds.mapNotNull(campsite).forEach { site ->
            val open = openStays(site)
            if (open > 0) {
                CortexButton(
                    "${site.name} ($open start dates open)", { onPick(site) },
                    Modifier.fillMaxWidth(),
                    style = CortexButtonStyle.OutlinedBrand,
                )
            } else {
                Text("${site.name}: no dates left")
            }
        }
    }
}

@Composable
private fun RolePickerDialog(current: Role, roles: List<Role>, onPick: (Role) -> Unit, onDismiss: () -> Unit) {
    InfoDialog("Switch role", onDismiss) {
        roles.forEach { role ->
            CortexButton(
                role.label + if (role == current) " (current)" else "", { onPick(role) },
                Modifier.fillMaxWidth(),
                style = if (role == current) CortexButtonStyle.Filled else CortexButtonStyle.OutlinedBrand,
            )
        }
    }
}

@Composable
private fun AdminModeBar(role: Role, onBackToAdmin: () -> Unit) {
    val colors = CortexTheme.colors
    Row(
        Modifier.fillMaxWidth().background(colors.surfaceDefault).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Admin profile · ${role.label}",
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        CortexButton("Admin dashboard", onBackToAdmin, style = CortexButtonStyle.Text, size = CortexButtonSize.XSmall)
    }
}
