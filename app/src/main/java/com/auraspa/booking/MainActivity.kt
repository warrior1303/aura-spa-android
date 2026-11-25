package com.auraspa.booking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ServicioSpa(
    val id: Int,
    val nombre: String,
    val duracionMin: Int,
    val precio: String,
    val descripcion: String
)

data class CitaAgendada(
    val servicio: ServicioSpa,
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
            val spaColors = lightColorScheme(
                primary = Color(0xFF5A7D7C),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFDCE8E7),
                surface = Color(0xFFFAF8F5),
                background = Color(0xFFFAF8F5)
            )
            MaterialTheme(colorScheme = spaColors) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SpaBookingScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaBookingScreen() {
    val servicios = remember {
        listOf(
            ServicioSpa(1, "Masaje Relajante", 60, "$35", "Masaje corporal completo con aceites esenciales."),
            ServicioSpa(2, "Limpieza Facial Profunda", 45, "$25", "Exfoliación, vapor de ozono e hidratación."),
            ServicioSpa(3, "Manicure & Pedicure Spa", 60, "$20", "Cuidado integral de manos y pies con parafina."),
            ServicioSpa(4, "Drenaje Linfático", 50, "$30", "Técnica suave para reducir retención de líquidos.")
        )
    }

    val horariosBase = listOf("09:00 AM", "10:30 AM", "12:00 PM", "02:00 PM", "03:30 PM", "05:00 PM")
    val proximosDias = remember { (0..13).map { normalizarDia(it) } }

    var pasoActual by remember { mutableIntStateOf(1) }
    var servicioSeleccionado by remember { mutableStateOf<ServicioSpa?>(null) }
    var fechaSeleccionada by remember { mutableStateOf(proximosDias.first()) }
    var horaSeleccionada by remember { mutableStateOf<String?>(null) }
    var nombreCliente by remember { mutableStateOf("") }
    var telefonoCliente by remember { mutableStateOf("") }

    val citasConfirmadas = remember { mutableStateListOf<CitaAgendada>() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Spa,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Aura Spa & Bienestar", fontWeight = FontWeight.Bold)
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
            if (pasoActual < 4) {
                BarraProgresoDidactica(pasoActual)
                Spacer(Modifier.height(16.dp))
            }

            when (pasoActual) {
                1 -> {
                    Text("1. ¿Qué tratamiento deseas hoy?", fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                                            Icon(
                                                Icons.Default.Schedule,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                " ${servicio.duracionMin} min",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = servicio.precio,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    val claveDiaActual = formatearClaveFecha(fechaSeleccionada)
                    Text("2. Elige el día y la hora", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Servicio: ${servicioSeleccionado?.nombre ?: ""}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))

                    Text("Selecciona un día:", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(proximosDias) { fecha ->
                            val esSeleccionado = formatearClaveFecha(fecha) == claveDiaActual
                            val diaSemana = SimpleDateFormat("EEE", Locale("es", "ES"))
                                .format(fecha)
                                .replace(".", "")
                                .uppercase()
                            val diaNumero = SimpleDateFormat("dd", Locale.getDefault()).format(fecha)

                            Card(
                                modifier = Modifier
                                    .width(64.dp)
                                    .clickable {
                                        fechaSeleccionada = fecha
                                        horaSeleccionada = null
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (esSeleccionado) MaterialTheme.colorScheme.primary else Color.White
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (esSeleccionado) MaterialTheme.colorScheme.primary else Color.LightGray
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(vertical = 12.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        diaSemana,
                                        fontSize = 12.sp,
                                        color = if (esSeleccionado) Color.White else Color.Gray
                                    )
                                    Text(
                                        diaNumero,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (esSeleccionado) Color.White else Color.Black
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Text("Horas disponibles:", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))

                    val horasOcupadasHoy = citasConfirmadas
                        .filter { it.claveFecha == claveDiaActual }
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
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (seleccionada) MaterialTheme.colorScheme.primary else Color.White,
                                    contentColor = if (seleccionada) Color.White else Color.Black
                                ),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(if (ocupada) "$hora (Ocupado)" else hora, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { pasoActual = 1 }) {
                            Text("← Cambiar servicio")
                        }
                        Button(
                            onClick = { pasoActual = 3 },
                            enabled = horaSeleccionada != null
                        ) {
                            Text("Continuar →")
                        }
                    }
                }

                3 -> {
                    Text("3. Confirma tu reserva", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Resumen de tu cita:", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "• Tratamiento: ${servicioSeleccionado?.nombre ?: ""} (${servicioSeleccionado?.precio ?: ""})"
                            )
                            Text("• Fecha: ${formatearFechaVisible(fechaSeleccionada)}")
                            Text("• Hora: ${horaSeleccionada ?: ""}")
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = nombreCliente,
                        onValueChange = { nombreCliente = it },
                        label = { Text("Tu Nombre Completo") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { pasoActual = 2 }) {
                            Text("← Cambiar hora")
                        }
                        Button(
                            onClick = {
                                val serv = servicioSeleccionado
                                val hora = horaSeleccionada
                                if (serv != null && hora != null) {
                                    citasConfirmadas.add(
                                        CitaAgendada(
                                            servicio = serv,
                                            claveFecha = formatearClaveFecha(fechaSeleccionada),
                                            hora = hora,
                                            clienteNombre = nombreCliente.trim(),
                                            clienteTelefono = telefonoCliente.trim()
                                        )
                                    )
                                    pasoActual = 4
                                }
                            },
                            enabled = nombreCliente.isNotBlank() && telefonoCliente.isNotBlank()
                        ) {
                            Text("Confirmar Cita")
                        }
                    }
                }

                4 -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("¡Cita Agendada con Éxito!", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Te esperamos el ${formatearFechaVisible(fechaSeleccionada)} a las ${horaSeleccionada ?: ""} para tu ${servicioSeleccionado?.nombre ?: ""}.",
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

@Composable
fun BarraProgresoDidactica(pasoActual: Int) {
    val pasos = listOf("1. Servicio", "2. Fecha/Hora", "3. Confirmar")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            color = if (activo) MaterialTheme.colorScheme.primary else Color.LightGray,
                            shape = RoundedCornerShape(3.dp)
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = titulo,
                    fontSize = 12.sp,
                    fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal,
                    color = if (activo) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }
    }
}
