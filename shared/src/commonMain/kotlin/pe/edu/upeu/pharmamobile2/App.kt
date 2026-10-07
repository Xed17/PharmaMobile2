package pe.edu.upeu.pharmamobile2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.KoinContext
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.upeu.pharmamobile2.navigation.Screen
import pe.edu.upeu.pharmamobile2.navigation.tituloPantalla
import pe.edu.upeu.pharmamobile2.presentation.cliente.ClientesScreen
import pe.edu.upeu.pharmamobile2.presentation.detalle.DetalleProductoScreen
import pe.edu.upeu.pharmamobile2.presentation.detalle.DetalleProductoViewModel
import pe.edu.upeu.pharmamobile2.presentation.inicio.InicioScreen
import pe.edu.upeu.pharmamobile2.presentation.pedido.PedidosScreen
import pe.edu.upeu.pharmamobile2.presentation.producto.ProductoScreen
import pe.edu.upeu.pharmamobile2.presentation.producto.ProductoViewModel
import pe.edu.upeu.pharmamobile2.presentation.producto.toDomain
import pe.edu.upeu.pharmamobile2.theme.PharmaMobilTheme

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

    KoinContext {
        PharmaMobilTheme(darkTheme = darkTheme) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val width = maxWidth

            when {
                // Pantalla amplia (Desktop / Foldable abierto) >= 840dp -> PermanentNavigationDrawer
                width >= 840.dp -> {
                    PermanentNavigationDrawer(
                        drawerContent = {
                            PermanentDrawerSheet(modifier = Modifier.width(260.dp)) {
                                Text(
                                    text = "PharmaMobil",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                HorizontalDivider()
                                DrawerNavigationItems(
                                    pantallaActual = pantallaActual,
                                    onSelectScreen = { pantallaActual = it }
                                )
                            }
                        }
                    ) {
                        AppScaffold(
                            pantallaActual = pantallaActual,
                            darkTheme = darkTheme,
                            onThemeChange = { darkTheme = it },
                            onSelectScreen = { pantallaActual = it },
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
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            AppScaffold(
                                pantallaActual = pantallaActual,
                                darkTheme = darkTheme,
                                onThemeChange = { darkTheme = it },
                                onSelectScreen = { pantallaActual = it },
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
                            ModalDrawerSheet {
                                Text(
                                    text = "PharmaMobil",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                HorizontalDivider()
                                DrawerNavigationItems(
                                    pantallaActual = pantallaActual,
                                    onSelectScreen = {
                                        pantallaActual = it
                                        scope.launch { drawerState.close() }
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
}
}

@Composable
private fun DrawerNavigationItems(
    pantallaActual: Screen,
    onSelectScreen: (Screen) -> Unit
) {
    NavigationDrawerItem(
        label = { Text("Inicio") },
        selected = pantallaActual is Screen.Inicio,
        onClick = { onSelectScreen(Screen.Inicio) }
    )
    NavigationDrawerItem(
        label = { Text("Productos") },
        selected = pantallaActual is Screen.Productos || pantallaActual is Screen.DetalleProducto,
        onClick = { onSelectScreen(Screen.Productos) }
    )
    NavigationDrawerItem(
        label = { Text("Clientes") },
        selected = pantallaActual is Screen.Clientes,
        onClick = { onSelectScreen(Screen.Clientes) }
    )
    NavigationDrawerItem(
        label = { Text("Pedidos") },
        selected = pantallaActual is Screen.Pedidos,
        onClick = { onSelectScreen(Screen.Pedidos) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScaffold(
    pantallaActual: Screen,
    darkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onSelectScreen: (Screen) -> Unit,
    navigationIcon: (@Composable () -> Unit)?
) {
    Scaffold(
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
                        modifier = Modifier.padding(end = 8.dp)
                    )
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
            }
        }
    }
}