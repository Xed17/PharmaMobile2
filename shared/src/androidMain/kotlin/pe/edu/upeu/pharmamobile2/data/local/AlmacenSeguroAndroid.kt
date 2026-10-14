package pe.edu.upeu.pharmamobile2.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AlmacenSeguroAndroid(
    private val context: Context
) : AlmacenSeguro {

    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val sharedPreferences by lazy {
        runCatching {
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILENAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrElse {
            context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        }
    }

    override suspend fun guardarToken(token: String) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply()
    }

    override suspend fun obtenerToken(): String? {
        return runCatching {
            sharedPreferences.getString(KEY_TOKEN, null)
        }.getOrNull()
    }

    override suspend fun eliminarToken() {
        sharedPreferences.edit().remove(KEY_TOKEN).apply()
    }

    companion object {
        private const val PREFS_FILENAME = "pharmamobil_secure_prefs"
        private const val KEY_TOKEN = "jwt_token"
    }
}
