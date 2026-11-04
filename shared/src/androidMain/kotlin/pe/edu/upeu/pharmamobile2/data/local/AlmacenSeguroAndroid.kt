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
        EncryptedSharedPreferences.create(
            context, PREFS_FILENAME, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override suspend fun guardarPerfil(perfil: pe.edu.upeu.pharmamobile2.domain.model.PerfilLocal) {
        check(sharedPreferences.edit().putString("perfil_validado", kotlinx.serialization.json.Json.encodeToString(pe.edu.upeu.pharmamobile2.domain.model.PerfilLocal.serializer(), perfil)).commit())
    }
    override suspend fun obtenerPerfil(): pe.edu.upeu.pharmamobile2.domain.model.PerfilLocal? =
        sharedPreferences.getString("perfil_validado", null)?.let { kotlinx.serialization.json.Json.decodeFromString(pe.edu.upeu.pharmamobile2.domain.model.PerfilLocal.serializer(), it) }
    override suspend fun borrarContinuidad() {
        check(sharedPreferences.edit().remove("perfil_validado").commit())
    }

    override suspend fun guardarToken(token: String) {
        check(sharedPreferences.edit().putString(KEY_TOKEN, token).commit())
    }

    override suspend fun obtenerToken(): String? {
        return runCatching {
            sharedPreferences.getString(KEY_TOKEN, null)
        }.getOrNull()
    }

    override suspend fun eliminarToken() {
        check(sharedPreferences.edit().remove(KEY_TOKEN).commit())
    }

    companion object {
        private const val PREFS_FILENAME = "pharmamobil_secure_prefs"
        private const val KEY_TOKEN = "jwt_token"
    }
}
