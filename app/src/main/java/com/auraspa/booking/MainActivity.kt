package com.auraspa.booking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Colores oficiales de Mérida Beauty
val MeridaPink = Color(0xFFE94E89)
val MeridaPinkLight = Color(0xFFFCE4EC)
val MeridaBackground = Color(0xFFFFF5F8)
val MeridaGold = Color(0xFFD4AF37)

data class ServicioSpa(
    val id: Int,
    val nombre: String,
    val duracionMin: Int,
    val precio: String,
    val descripcion: String
)

data class EspecialistaSpa(
    val id: Int,
    val nombre: String,
    val especialidad: String
)

data class CitaAgendada(
    val servicio: ServicioSpa,
    val especialista: EspecialistaSpa,
    val claveFecha: String,
    val hora: String,
    val clienteNombre: String,
    val clienteTelefono: String
)

private fun normalizarDia(offsetDias: Int): Date {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, offsetDias)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.time
}

private fun formatearClaveFecha(fecha: Date): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(fecha)
}

private fun formatearFechaVisible(fecha: Date): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fecha)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val meridaColors = lightColorScheme(
                primary = MeridaPink,
                onPrimary = Color.White,
                primaryContainer = MeridaPinkLight,
                secondary = MeridaGold,
                surface = MeridaBackground,
                background = MeridaBackground
            )
            MaterialTheme(colorScheme = meridaColors) {
                Surface(modifier = Modifier.fillMaxSize(), color = MeridaBackground) {
                    SpaBookingApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaBookingApp() {
    // Listas editables en tiempo real desde el Panel Admin
    val servicios = remember {
        mutableStateListOf(
            ServicioSpa(1, "Masaje Relajante", 60, "$35", "Masaje corporal completo con aceites esenciales."),
            ServicioSpa(2, "Limpieza Facial Profunda", 45, "$25", "Exfoliación, vapor de ozono e hidratación."),
            ServicioSpa(3, "Manicure & Pedicure Spa", 60, "$20", "Cuidado integral de manos y pies con parafina."),
            ServicioSpa(4, "Micropigmentación", 90, "$80", "Diseño y perfilado semipermanente.")
        )
    }

    val especialistas = remember {
        mutableStateListOf(
            EspecialistaSpa(1, "Valentina", "Especialista en Faciales y Micropigmentación"),
            EspecialistaSpa(2, "Camila", "Terapeuta Corporal y Masajes"),
            EspecialistaSpa(3, "Sofía", "Manicurista y Estética Integral")
        )
    }

    val horariosBase = listOf("09:00 AM", "10:30 AM", "12:00 PM", "02:00 PM", "03:30 PM", "05:00 PM")
    val proximosDias = remember { (0..13).map { normalizarDia(it) } }
    val citasConfirmadas = remember { mutableStateListOf<CitaAgendada>() }

    // Navegación
    var pasoActual by remember { mutableIntStateOf(0) } // 0 = Bienvenida, 1..5 = Flujo Reserva
    var enModoAdmin by remember { mutableStateOf(false) }
    var mostrarDialogoPin by remember { mutableStateOf(false) }

    // Estado de la reserva actual
    var servicioSeleccionado by remember { mutableStateOf<ServicioSpa?>(null) }
    var especialistaSeleccionado by remember { mutableStateOf<EspecialistaSpa?>(null) }
    var fechaSeleccionada by remember { mutableStateOf(proximosDias.first()) }
    var horaSeleccionada by remember { mutableStateOf<String?>(null) }
    var nombreCliente by remember { mutableStateOf("") }
    var telefonoCliente by remember { mutableStateOf("") }

    // Diálogo de PIN para entrar a Administración
    if (mostrarDialogoPin) {
        DialogoPinAdmin(
            onDismiss = { mostrarDialogoPin = false },
            onPinCorrecto = {
                mostrarDialogoPin = false
                enModoAdmin = true
            }
        )
    }

    if (enModoAdmin) {
        PanelAdministracionScreen(
            servicios = servicios,
            especialistas = especialistas,
            onVolver = { enModoAdmin = false }
        )
        return
    }

    if (pasoActual == 0) {
        PantallaBienvenidaAvatar(onComenzar = { pasoActual = 1 })
        return
    }

    Scaffold(
        containerColor = MeridaBackground,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_merida),
                            contentDescription = "Logo Mérida Beauty",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MeridaGold, CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Mérida Beauty", fontWeight = FontWeight.Bold, color = MeridaPink)
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarDialogoPin = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Administrar Spa",
                            tint = MeridaGold
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            if (pasoActual in 1..4) {
                BarraProgresoDidactica(pasoActual)
                Spacer(Modifier.height(16.dp))
            }

            when (pasoActual) {
                // PASO 1: SERVICIO
                1 -> {
                    Text("1. ¿Qué tratamiento deseas hoy?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                    Text("Toca una tarjeta para continuar", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(servicios) { servicio ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        servicioSeleccionado = servicio
                                        pasoActual = 2
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, MeridaPinkLight),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(servicio.nombre, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text(servicio.descripcion, fontSize = 13.sp, color = Color.DarkGray)
                                        Spacer(Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = MeridaGold)
                                            Text(" ${servicio.duracionMin} min", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                    Surface(
                                        color = MeridaPinkLight,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MeridaGold)
                                    ) {
                                        Text(
                                            text = servicio.precio,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            fontWeight = FontWeight.Bold,
                                            color = MeridaPink
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // PASO 2: ELEGIR ESPECIALISTA
                2 -> {
                    Text("2. Elige a tu especialista", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Servicio: ${servicioSeleccionado?.nombre ?: ""}", fontSize = 14.sp, color = MeridaPink, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(especialistas) { esp ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        especialistaSeleccionado = esp
                                        horaSeleccionada = null
                                        pasoActual = 3
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, MeridaPinkLight),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MeridaPinkLight,
                                        border = BorderStroke(1.5.dp, MeridaGold),
                                        modifier = Modifier.size(50.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Face, contentDescription = null, tint = MeridaPink, modifier = Modifier.size(28.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(esp.nombre, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                        Text(esp.especialidad, fontSize = 13.sp, color = Color.DarkGray)
                                    }
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MeridaGold)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        TextButton(onClick = { pasoActual = 1 }) {
                            Text("← Cambiar servicio")
                        }
                    }
                }

                // PASO 3: FECHA Y HORA
                3 -> {
                    val claveDiaActual = formatearClaveFecha(fechaSeleccionada)
                    Text("3. Elige el día y la hora", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${servicioSeleccionado?.nombre ?: ""} con ${especialistaSeleccionado?.nombre ?: ""}",
                        fontSize = 14.sp,
                        color = MeridaPink,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(16.dp))

                    Text("Selecciona un día:", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(proximosDias) { fecha ->
                            val esSeleccionado = formatearClaveFecha(fecha) == claveDiaActual
                            val diaSemana = SimpleDateFormat("EEE", Locale("es", "ES")).format(fecha).replace(".", "").uppercase()
                            val diaNumero = SimpleDateFormat("dd", Locale.getDefault()).format(fecha)

                            Card(
                                modifier = Modifier
                                    .width(64.dp)
                                    .clickable {
                                        fechaSeleccionada = fecha
                                        horaSeleccionada = null
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (esSeleccionado) MeridaPink else Color.White
                                ),
                                border = BorderStroke(1.dp, if (esSeleccionado) MeridaGold else MeridaPinkLight)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(diaSemana, fontSize = 12.sp, color = if (esSeleccionado) Color.White else Color.Gray)
                                    Text(diaNumero, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (esSeleccionado) Color.White else Color.Black)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Text("Horas disponibles:", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))

                    // Bloqueo inteligente: solo bloquea si ese mismo especialista ya tiene cita ese día y hora
                    val horasOcupadasHoy = citasConfirmadas
                        .filter { it.claveFecha == claveDiaActual && it.especialista.id == especialistaSeleccionado?.id }
                        .map { it.hora }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(horariosBase) { hora ->
                            val ocupada = horasOcupadasHoy.contains(hora)
                            val seleccionada = hora == horaSeleccionada

                            OutlinedButton(
                                onClick = { horaSeleccionada = hora },
                                enabled = !ocupada,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (seleccionada) MeridaGold else MeridaPinkLight),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (seleccionada) MeridaPink else Color.White,
                                    contentColor = if (seleccionada) Color.White else Color.Black
                                ),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(if (ocupada) "$hora (Ocupado)" else hora, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { pasoActual = 2 }) {
                            Text("← Especialista")
                        }
                        Button(onClick = { pasoActual = 4 }, enabled = horaSeleccionada != null) {
                            Text("Continuar →")
                        }
                    }
                }

                // PASO 4: DATOS Y CONFIRMACIÓN
                4 -> {
                    Text("4. Confirma tu reserva", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.5.dp, MeridaGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Resumen en Mérida Beauty:", fontWeight = FontWeight.Bold, color = MeridaPink)
                            Spacer(Modifier.height(6.dp))
                            Text("• Tratamiento: ${servicioSeleccionado?.nombre ?: ""} (${servicioSeleccionado?.precio ?: ""})")
                            Text("• Especialista: ${especialistaSeleccionado?.nombre ?: ""}")
                            Text("• Fecha: ${formatearFechaVisible(fechaSeleccionada)}")
                            Text("• Hora: ${horaSeleccionada ?: ""}")
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = nombreCliente,
                        onValueChange = { nombreCliente = it },
                        label = { Text("Tu Nombre Completo") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MeridaPink) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = telefonoCliente,
                        onValueChange = { telefonoCliente = it },
                        label = { Text("WhatsApp / Teléfono") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.weight(1f))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { pasoActual = 3 }) {
                            Text("← Cambiar hora")
                        }
                        Button(
                            onClick = {
                                val serv = servicioSeleccionado
                                val esp = especialistaSeleccionado
                                val hora = horaSeleccionada
                                if (serv != null && esp != null && hora != null) {
                                    citasConfirmadas.add(
                                        CitaAgendada(
                                            servicio = serv,
                                            especialista = esp,
                                            claveFecha = formatearClaveFecha(fechaSeleccionada),
                                            hora = hora,
                                            clienteNombre = nombreCliente.trim(),
                                            clienteTelefono = telefonoCliente.trim()
                                        )
                                    )
                                    pasoActual = 5
                                }
                            },
                            enabled = nombreCliente.isNotBlank() && telefonoCliente.isNotBlank()
                        ) {
                            Text("Confirmar Cita")
                        }
                    }
                }

                // PASO 5: COMPROBANTE FINAL
                5 -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MeridaPink, modifier = Modifier.size(80.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("¡Cita Agendada con Éxito!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MeridaPink)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Te esperamos en Mérida Beauty el ${formatearFechaVisible(fechaSeleccionada)} a las ${horaSeleccionada ?: ""} con ${especialistaSeleccionado?.nombre ?: ""} para tu ${servicioSeleccionado?.nombre ?: ""}.",
                            textAlign = TextAlign.Center,
                            color = Color.DarkGray
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = {
                            horaSeleccionada = null
                            nombreCliente = ""
                            telefonoCliente = ""
                            pasoActual = 1
                        }) {
                            Icon(Icons.Default.EventAvailable, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Agendar otra cita")
                        }
                    }
                }
            }
        }
    }
}

// --- PANTALLA DE ADMINISTRACIÓN (SERVICIOS Y ESPECIALISTAS) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelAdministracionScreen(
    servicios: MutableList<ServicioSpa>,
    especialistas: MutableList<EspecialistaSpa>,
    onVolver: () -> Unit
) {
    var pestanaActiva by remember { mutableIntStateOf(0) } // 0 = Servicios, 1 = Especialistas

    // Estados para modales de Servicio
    var mostrarDialogoServicio by remember { mutableStateOf(false) }
    var servicioEnEdicion by remember { mutableStateOf<ServicioSpa?>(null) }

    // Estados para modales de Especialista
    var mostrarDialogoEspecialista by remember { mutableStateOf(false) }
    var especialistaEnEdicion by remember { mutableStateOf<EspecialistaSpa?>(null) }

    Scaffold(
        containerColor = MeridaBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                title = { Text("Administrar Mérida Beauty", fontWeight = FontWeight.Bold, color = MeridaPink) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MeridaPink)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (pestanaActiva == 0) {
                        servicioEnEdicion = null
                        mostrarDialogoServicio = true
                    } else {
                        especialistaEnEdicion = null
                        mostrarDialogoEspecialista = true
                    }
                },
                containerColor = MeridaPink,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = pestanaActiva,
                containerColor = Color.White,
                contentColor = MeridaPink
            ) {
                Tab(
                    selected = pestanaActiva == 0,
                    onClick = { pestanaActiva = 0 },
                    text = { Text("Servicios (${servicios.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = pestanaActiva == 1,
                    onClick = { pestanaActiva = 1 },
                    text = { Text("Especialistas (${especialistas.size})", fontWeight = FontWeight.Bold) }
                )
            }

            if (pestanaActiva == 0) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(servicios) { servicio ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, MeridaPinkLight)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${servicio.nombre} — ${servicio.precio}", fontWeight = FontWeight.Bold)
                                    Text("${servicio.duracionMin} min • ${servicio.descripcion}", fontSize = 12.sp, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    servicioEnEdicion = servicio
                                    mostrarDialogoServicio = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MeridaGold)
                                }
                                IconButton(onClick = { servicios.remove(servicio) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(especialistas) { esp ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, MeridaPinkLight)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(esp.nombre, fontWeight = FontWeight.Bold)
                                    Text(esp.especialidad, fontSize = 12.sp, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    especialistaEnEdicion = esp
                                    mostrarDialogoEspecialista = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MeridaGold)
                                }
                                IconButton(onClick = { especialistas.remove(esp) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal para agregar/editar Servicio
    if (mostrarDialogoServicio) {
        var nombre by remember { mutableStateOf(servicioEnEdicion?.nombre ?: "") }
        var precio by remember { mutableStateOf(servicioEnEdicion?.precio ?: "$") }
        var duracion by remember { mutableStateOf(servicioEnEdicion?.duracionMin?.toString() ?: "45") }
        var desc by remember { mutableStateOf(servicioEnEdicion?.descripcion ?: "") }

        AlertDialog(
            onDismissRequest = { mostrarDialogoServicio = false },
            title = { Text(if (servicioEnEdicion == null) "Nuevo Servicio" else "Editar Servicio", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre del servicio") }, singleLine = true)
                    OutlinedTextField(value = precio, onValueChange = { precio = it }, label = { Text("Precio (ej. $25)") }, singleLine = true)
                    OutlinedTextField(value = duracion, onValueChange = { duracion = it.filter { c -> c.isDigit() } }, label = { Text("Duración (minutos)") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descripción breve") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val min = duracion.toIntOrNull() ?: 45
                        val editando = servicioEnEdicion
                        if (editando == null) {
                            val nuevoId = (servicios.maxOfOrNull { it.id } ?: 0) + 1
                            servicios.add(ServicioSpa(nuevoId, nombre.trim(), min, precio.trim(), desc.trim()))
                        } else {
                            val idx = servicios.indexOfFirst { it.id == editando.id }
                            if (idx != -1) {
                                servicios[idx] = editando.copy(nombre = nombre.trim(), duracionMin = min, precio = precio.trim(), descripcion = desc.trim())
                            }
                        }
                        mostrarDialogoServicio = false
                    },
                    enabled = nombre.isNotBlank() && precio.isNotBlank()
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoServicio = false }) { Text("Cancelar") }
            }
        )
    }

    // Modal para agregar/editar Especialista
    if (mostrarDialogoEspecialista) {
        var nombre by remember { mutableStateOf(especialistaEnEdicion?.nombre ?: "") }
        var especialidad by remember { mutableStateOf(especialistaEnEdicion?.especialidad ?: "") }

        AlertDialog(
            onDismissRequest = { mostrarDialogoEspecialista = false },
            title = { Text(if (especialistaEnEdicion == null) "Nuevo Especialista" else "Editar Especialista", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, singleLine = true)
                    OutlinedTextField(value = especialidad, onValueChange = { especialidad = it }, label = { Text("Especialidad / Rol") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val editando = especialistaEnEdicion
                        if (editando == null) {
                            val nuevoId = (especialistas.maxOfOrNull { it.id } ?: 0) + 1
                            especialistas.add(EspecialistaSpa(nuevoId, nombre.trim(), especialidad.trim()))
                        } else {
                            val idx = especialistas.indexOfFirst { it.id == editando.id }
                            if (idx != -1) {
                                especialistas[idx] = editando.copy(nombre = nombre.trim(), especialidad = especialidad.trim())
                            }
                        }
                        mostrarDialogoEspecialista = false
                    },
                    enabled = nombre.isNotBlank()
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEspecialista = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun DialogoPinAdmin(onDismiss: () -> Unit, onPinCorrecto: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MeridaPink) },
        title = { Text("Acceso Administrador") },
        text = {
            Column {
                Text("Ingresa el PIN de Mérida Beauty (por defecto: 1234)", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it
                        error = false
                    },
                    label = { Text("PIN") },
                    isError = error,
                    singleLine = true
                )
                if (error) {
                    Text("PIN incorrecto", color = Color.Red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (pin == "1234") onPinCorrecto() else error = true
            }) { Text("Entrar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun PantallaBienvenidaAvatar(onComenzar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeridaBackground)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(2.5.dp, MeridaGold),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.avatar_merida),
                contentDescription = "Bienvenida Mérida Beauty",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(20.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MeridaPinkLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("¡Bienvenida a Mérida Beauty!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MeridaPink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Estoy aquí para ayudarte a reservar tu momento de relajación y belleza en pocos pasos.",
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onComenzar,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Agendar mi cita", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun BarraProgresoDidactica(pasoActual: Int) {
    val pasos = listOf("1. Servicio", "2. Equipo", "3. Horario", "4. Listo")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        pasos.forEachIndexed { index, titulo ->
            val numeroPaso = index + 1
            val activo = numeroPaso <= pasoActual
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(
                            color = if (activo) MeridaPink else MeridaPinkLight,
                            shape = RoundedCornerShape(3.dp)
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = titulo,
                    fontSize = 11.sp,
                    fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal,
                    color = if (activo) MeridaPink else Color.Gray
                )
            }
        }
    }
}
