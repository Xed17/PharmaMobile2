package pe.edu.upeu.pharmamobile2

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.upeu.pharmamobile2.domain.model.Sesion
import pe.edu.upeu.pharmamobile2.navigation.Screen
import pe.edu.upeu.pharmamobile2.navigation.tituloPantalla
import pe.edu.upeu.pharmamobile2.platform.InfoDispositivo
import pe.edu.upeu.pharmamobile2.presentation.acercade.AcercaDeScreen
import pe.edu.upeu.pharmamobile2.presentation.cliente.ClientesScreen
import pe.edu.upeu.pharmamobile2.presentation.detalle.DetalleProductoScreen
import pe.edu.upeu.pharmamobile2.presentation.detalle.DetalleProductoViewModel
import pe.edu.upeu.pharmamobile2.presentation.inicio.InicioScreen
import pe.edu.upeu.pharmamobile2.presentation.pedido.PedidosScreen
import pe.edu.upeu.pharmamobile2.presentation.producto.ProductoScreen
import pe.edu.upeu.pharmamobile2.presentation.producto.ProductoViewModel
import pe.edu.upeu.pharmamobile2.presentation.producto.toDomain
import pe.edu.upeu.pharmamobile2.presentation.sesion.EstadoSesion
import pe.edu.upeu.pharmamobile2.presentation.sesion.LoginScreen
import pe.edu.upeu.pharmamobile2.presentation.sesion.SesionViewModel
import pe.edu.upeu.pharmamobile2.theme.PharmaMobilTheme
import pharmamobile2.shared.generated.resources.Res
import pharmamobile2.shared.generated.resources.compose_multiplatform

private val ArrowBackIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ArrowBack",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(20f, 11f)
            horizontalLineTo(7.83f)
            lineToRelative(5.59f, -5.59f)
            lineTo(12f, 4f)
            lineToRelative(-8f, 8f)
            lineToRelative(8f, 8f)
            lineToRelative(1.41f, -1.41f)
            lineTo(7.83f, 13f)
            horizontalLineTo(20f)
            verticalLineToRelative(-2f)
            close()
        }
    }.build()
}

private val MenuIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Menu",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(3f, 18f)
            horizontalLineTo(21f)
            verticalLineTo(16f)
            horizontalLineTo(3f)
            verticalLineTo(18f)
            close()
            moveTo(3f, 13f)
            horizontalLineTo(21f)
            verticalLineTo(11f)
            horizontalLineTo(3f)
            verticalLineTo(13f)
            close()
            moveTo(3f, 6f)
            verticalLineTo(8f)
            horizontalLineTo(21f)
            verticalLineTo(6f)
            horizontalLineTo(3f)
            close()
        }
    }.build()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    var darkTheme by remember { mutableStateOf(false) }
    var pantallaActual by remember { mutableStateOf<Screen>(Screen.Inicio) }
    val snackbarHostState = remember { SnackbarHostState() }

    KoinContext {
        val sesionViewModel: SesionViewModel = koinViewModel()
        val sesionState by sesionViewModel.uiState.collectAsState()

        LaunchedEffect(Unit) {
            sesionViewModel.snackbarEventos.collect { mensaje ->
                snackbarHostState.showSnackbar(
                    message = mensaje,
                    withDismissAction = true
                )
            }
        }

        PharmaMobilTheme(darkTheme = darkTheme) {
            when (val estado = sesionState.estado) {
                is EstadoSesion.Comprobando -> {
                    PantallaCargandoSesion()
                }

                is EstadoSesion.SinSesion -> {
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { padding ->
                        LoginScreen(
                            viewModel = sesionViewModel,
                            aviso = estado.aviso,
                            modifier = Modifier.padding(padding)
                        )
                    }
                }

                is EstadoSesion.Autenticada -> {
                    val sesion = estado.sesion
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val width = maxWidth

                        when {
                            // Pantalla amplia (Desktop / Foldable abierto) >= 840dp -> PermanentNavigationDrawer
                            width >= 840.dp -> {
                                PermanentNavigationDrawer(
                                    drawerContent = {
                                        PermanentDrawerSheet(modifier = Modifier.width(280.dp)) {
                                            CabeceraDrawerSesion(sesion = sesion)
                                            HorizontalDivider()
                                            DrawerNavigationItems(
                                                pantallaActual = pantallaActual,
                                                onSelectScreen = { pantallaActual = it },
                                                onCerrarSesion = {
                                                    pantallaActual = Screen.Inicio
                                                    sesionViewModel.cerrarSesion()
                                                }
                                            )
                                        }
                                    }
                                ) {
                                    AppScaffold(
                                        pantallaActual = pantallaActual,
                                        darkTheme = darkTheme,
                                        onThemeChange = { darkTheme = it },
                                        onSelectScreen = { pantallaActual = it },
                                        onCerrarSesion = {
                                            pantallaActual = Screen.Inicio
                                            sesionViewModel.cerrarSesion()
                                        },
                                        snackbarHostState = snackbarHostState,
                                        navigationIcon = null
                                    )
                                }
                            }

                            // Pantalla mediana (Tablet) 600dp a 839dp -> NavigationRail
                            width >= 600.dp -> {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    NavigationRail(
                                        modifier = Modifier.fillMaxHeight(),
                                        header = {
                                            Text(
                                                text = "PM",
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.padding(vertical = 12.dp)
                                            )
                                        }
                                    ) {
                                        NavigationRailItem(
                                            selected = pantallaActual is Screen.Inicio,
                                            onClick = { pantallaActual = Screen.Inicio },
                                            icon = { Text("🏠") },
                                            label = { Text("Inicio") }
                                        )
                                        NavigationRailItem(
                                            selected = pantallaActual is Screen.Productos || pantallaActual is Screen.DetalleProducto,
                                            onClick = { pantallaActual = Screen.Productos },
                                            icon = { Text("💊") },
                                            label = { Text("Productos") }
                                        )
                                        NavigationRailItem(
                                            selected = pantallaActual is Screen.Clientes,
                                            onClick = { pantallaActual = Screen.Clientes },
                                            icon = { Text("👥") },
                                            label = { Text("Clientes") }
                                        )
                                        NavigationRailItem(
                                            selected = pantallaActual is Screen.Pedidos,
                                            onClick = { pantallaActual = Screen.Pedidos },
                                            icon = { Text("📋") },
                                            label = { Text("Pedidos") }
                                        )
                                        NavigationRailItem(
                                            selected = pantallaActual is Screen.AcercaDe,
                                            onClick = { pantallaActual = Screen.AcercaDe },
                                            icon = { Text("ℹ️") },
                                            label = { Text("Acerca de") }
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        NavigationRailItem(
                                            selected = false,
                                            onClick = {
                                                pantallaActual = Screen.Inicio
                                                sesionViewModel.cerrarSesion()
                                            },
                                            icon = { Text("🚪") },
                                            label = { Text("Salir") }
                                        )
                                    }

                                    Box(modifier = Modifier.weight(1f)) {
                                        AppScaffold(
                                            pantallaActual = pantallaActual,
                                            darkTheme = darkTheme,
                                            onThemeChange = { darkTheme = it },
                                            onSelectScreen = { pantallaActual = it },
                                            onCerrarSesion = {
                                                pantallaActual = Screen.Inicio
                                                sesionViewModel.cerrarSesion()
                                            },
                                            snackbarHostState = snackbarHostState,
                                            navigationIcon = null
                                        )
                                    }
                                }
                            }

                            // Teléfono (< 600dp) -> ModalNavigationDrawer estándar
                            else -> {
                                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                                val scope = rememberCoroutineScope()

                                ModalNavigationDrawer(
                                    drawerState = drawerState,
                                    drawerContent = {
                                        ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                                            CabeceraDrawerSesion(sesion = sesion)
                                            HorizontalDivider()
                                            DrawerNavigationItems(
                                                pantallaActual = pantallaActual,
                                                onSelectScreen = {
                                                    pantallaActual = it
                                                    scope.launch { drawerState.close() }
                                                },
                                                onCerrarSesion = {
                                                    scope.launch {
                                                        drawerState.close()
                                                        pantallaActual = Screen.Inicio
                                                        sesionViewModel.cerrarSesion()
                                                    }
                                                }
                                            )
                                        }
                                    }
                                ) {
                                    AppScaffold(
                                        pantallaActual = pantallaActual,
                                        darkTheme = darkTheme,
                                        onThemeChange = { darkTheme = it },
                                        onSelectScreen = { pantallaActual = it },
                                        onCerrarSesion = {
                                            pantallaActual = Screen.Inicio
                                            sesionViewModel.cerrarSesion()
                                        },
                                        snackbarHostState = snackbarHostState,
                                        navigationIcon = {
                                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                                Icon(
                                                    imageVector = MenuIcon,
                                                    contentDescription = "Abrir menú",
                                                    tint = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                is EstadoSesion.ErrorRecuperacion -> {
                    PantallaCargandoSesion(mensaje = estado.mensaje)
                }
            }
        }
    }
}

@Composable
private fun PantallaCargandoSesion(mensaje: String = "Comprobando sesión...") {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.compose_multiplatform),
                contentDescription = "Logo PharmaMobile",
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CabeceraDrawerSesion(sesion: Sesion) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sesion.username.take(2).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = sesion.nombreCompleto.ifBlank { sesion.username },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "@${sesion.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val rolesStr = if (sesion.roles.isNotEmpty()) {
            sesion.roles.joinToString(", ")
        } else {
            "Usuario"
        }

        Text(
            text = "Rol: $rolesStr",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DrawerNavigationItems(
    pantallaActual: Screen,
    onSelectScreen: (Screen) -> Unit,
    onCerrarSesion: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            NavigationDrawerItem(
                label = { Text("Inicio") },
                icon = { Text("🏠") },
                selected = pantallaActual is Screen.Inicio,
                onClick = { onSelectScreen(Screen.Inicio) }
            )
            NavigationDrawerItem(
                label = { Text("Productos") },
                icon = { Text("💊") },
                selected = pantallaActual is Screen.Productos || pantallaActual is Screen.DetalleProducto,
                onClick = { onSelectScreen(Screen.Productos) }
            )
            NavigationDrawerItem(
                label = { Text("Clientes") },
                icon = { Text("👥") },
                selected = pantallaActual is Screen.Clientes,
                onClick = { onSelectScreen(Screen.Clientes) }
            )
            NavigationDrawerItem(
                label = { Text("Pedidos") },
                icon = { Text("📋") },
                selected = pantallaActual is Screen.Pedidos,
                onClick = { onSelectScreen(Screen.Pedidos) }
            )
            NavigationDrawerItem(
                label = { Text("Acerca de") },
                icon = { Text("ℹ️") },
                selected = pantallaActual is Screen.AcercaDe,
                onClick = { onSelectScreen(Screen.AcercaDe) }
            )
        }

        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            NavigationDrawerItem(
                label = {
                    Text(
                        text = "Cerrar sesión",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                },
                icon = { Text("🚪") },
                selected = false,
                onClick = onCerrarSesion
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScaffold(
    pantallaActual: Screen,
    darkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onSelectScreen: (Screen) -> Unit,
    onCerrarSesion: () -> Unit,
    snackbarHostState: SnackbarHostState,
    navigationIcon: (@Composable () -> Unit)?
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(tituloPantalla(pantallaActual)) },
                navigationIcon = {
                    if (pantallaActual is Screen.DetalleProducto) {
                        IconButton(onClick = { onSelectScreen(Screen.Productos) }) {
                            Icon(
                                imageVector = ArrowBackIcon,
                                contentDescription = "Volver a productos",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        navigationIcon?.invoke()
                    }
                },
                actions = {
                    Switch(
                        checked = darkTheme,
                        onCheckedChange = onThemeChange,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    IconButton(onClick = onCerrarSesion) {
                        Text(
                            text = "🚪",
                            fontSize = 18.sp
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (pantallaActual) {
                is Screen.Inicio -> InicioScreen()
                is Screen.Productos -> {
                    val viewModel: ProductoViewModel = koinViewModel()
                    val detalleVm: DetalleProductoViewModel = koinViewModel()
                    ProductoScreen(
                        viewModel = viewModel,
                        onVerDetalle = { id -> onSelectScreen(Screen.DetalleProducto(id)) },
                        onCompartir = { prod -> detalleVm.compartir(prod.toDomain()) }
                    )
                }
                is Screen.DetalleProducto -> {
                    val detalleVm: DetalleProductoViewModel = koinViewModel()
                    DetalleProductoScreen(
                        productoId = pantallaActual.productoId,
                        viewModel = detalleVm,
                        onVolver = { onSelectScreen(Screen.Productos) }
                    )
                }
                is Screen.Clientes -> ClientesScreen()
                is Screen.Pedidos -> PedidosScreen()
                is Screen.AcercaDe -> {
                    val infoDispositivo: InfoDispositivo = koinInject()
                    AcercaDeScreen(infoDispositivo = infoDispositivo)
                }
            }
        }
    }
}