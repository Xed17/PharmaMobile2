package pe.edu.upeu.pharmamobile2.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    error: String? = null,
    mostrarError: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true
) {
    val mostrarTextoError = mostrarError && !error.isNullOrBlank()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        isError = mostrarTextoError,
        supportingText = if (mostrarTextoError) {
            {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            null
        },
        keyboardOptions = keyboardOptions,
        singleLine = singleLine
    )
}
