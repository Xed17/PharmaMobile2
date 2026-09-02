package pe.edu.upeu.pharmamobile2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
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
import pe.edu.upeu.pharmamobile2.navigation.Screen
import pe.edu.upeu.pharmamobile2.navigation.tituloPantalla
import pe.edu.upeu.pharmamobile2.presentation.cliente.ClientesScreen
import pe.edu.upeu.pharmamobile2.presentation.inicio.InicioScreen
import pe.edu.upeu.pharmamobile2.presentation.pedido.PedidosScreen
import pe.edu.upeu.pharmamobile2.presentation.producto.ProductoScreen
import pe.edu.upeu.pharmamobile2.theme.PharmaMobilTheme

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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    PharmaMobilTheme(darkTheme = darkTheme) {
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
                    NavigationDrawerItem(
                        label = { Text("Inicio") },
                        selected = pantallaActual is Screen.Inicio,
                        onClick = {
                            pantallaActual = Screen.Inicio
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("Productos") },
                        selected = pantallaActual is Screen.Productos,
                        onClick = {
                            pantallaActual = Screen.Productos
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("Clientes") },
                        selected = pantallaActual is Screen.Clientes,
                        onClick = {
                            pantallaActual = Screen.Clientes
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("Pedidos") },
                        selected = pantallaActual is Screen.Pedidos,
                        onClick = {
                            pantallaActual = Screen.Pedidos
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(tituloPantalla(pantallaActual)) },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch { drawerState.open() }
                            }) {
                                Icon(
                                    imageVector = MenuIcon,
                                    contentDescription = "Abrir menú",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        actions = {
                            Switch(
                                checked = darkTheme,
                                onCheckedChange = { darkTheme = it },
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
                        is Screen.Productos -> ProductoScreen()
                        is Screen.Clientes -> ClientesScreen()
                        is Screen.Pedidos -> PedidosScreen()
                    }
                }
            }
        }
    }
}