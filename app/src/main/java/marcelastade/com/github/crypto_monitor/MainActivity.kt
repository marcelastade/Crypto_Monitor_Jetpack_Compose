package marcelastade.com.github.crypto_monitor

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import marcelastade.com.github.crypto_monitor.service.MercadoBitcoinServiceFactory
import marcelastade.com.github.crypto_monitor.ui.theme.CryptomonitorTheme
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CryptomonitorTheme {
                CryptoMonitorApp()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CryptoMonitorApp() {
        var valueText by remember { mutableStateOf("R$ 0,00") }
        var dateText by remember { mutableStateOf("dd/mm/yyyy hh:mm:ss") }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Monitor de Crypto Moedas - BITCOIN",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color(0xFF0D6EFD)
                            ))

            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                QuoteInformation(
                    valueText = valueText,
                    dateText = dateText,
                    onRefresh = {
                        makeRestCall(
                            onSuccess = { formattedValue, formattedDate ->
                                valueText = formattedValue
                                dateText = formattedDate
                            },
                            onError = { message ->
                                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                )
            }
        }
    }

    @Composable
    fun QuoteInformation(
        valueText: String,
        dateText: String,
        onRefresh: () -> Unit
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = "Cotação - BITCOIN", fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = valueText, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = dateText)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRefresh,
                modifier = Modifier
                    .width(125.dp)
                    .padding(top = 5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF198754),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "ATUALIZAR")
            }

        }
    }

    private fun makeRestCall(
        onSuccess: (String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val service = MercadoBitcoinServiceFactory().create()
                val response = service.getTicker()

                if (response.isSuccessful) {
                    val tickerResponse = response.body()

                    val lastValue = tickerResponse?.ticker?.last?.toDoubleOrNull()
                    val date = tickerResponse?.ticker?.date?.let { Date(it * 1000L) }

                    withContext(Dispatchers.Main) {
                        if (lastValue != null && date != null) {
                            val numberFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
                            val formattedValue = numberFormat.format(lastValue)

                            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR"))
                            sdf.timeZone = TimeZone.getTimeZone("America/Sao_Paulo")
                            val formattedDate = sdf.format(date)

                            onSuccess(formattedValue, formattedDate)
                        } else {
                            onError("Dados inválidos recebidos.")
                        }
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "Bad Request"
                        401 -> "Unauthorized"
                        403 -> "Forbidden"
                        404 -> "Not Found"
                        else -> "Erro desconhecido (${response.code()})"
                    }
                    withContext(Dispatchers.Main) { onError(errorMessage) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Falha: ${e.message}")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CryptoMonitorPreview() {
    CryptomonitorTheme {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Cotação Atual", fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "R$ 200.000,00", fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "30/10/2025 16:30:12")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {}) { Text(text = "Atualizar") }
        }
    }
}
