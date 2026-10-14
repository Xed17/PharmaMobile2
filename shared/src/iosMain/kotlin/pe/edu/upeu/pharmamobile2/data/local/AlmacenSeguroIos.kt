package pe.edu.upeu.pharmamobile2.data.local

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSDictionary
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

class AlmacenSeguroIos : AlmacenSeguro {

    private val service = "pe.edu.upeu.pharmamobile2"
    private val account = "jwt_token"

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun guardarToken(token: String) {
        eliminarToken()
        val data = (token as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return

        val query: NSDictionary = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service,
            kSecAttrAccount to account,
            kSecValueData to data
        ) as NSDictionary

        val cfQuery = CFBridgingRetain(query) as CFDictionaryRef
        try {
            SecItemAdd(cfQuery, null)
        } finally {
            CFBridgingRelease(cfQuery)
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun obtenerToken(): String? = memScoped {
        val query: NSDictionary = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service,
            kSecAttrAccount to account,
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitOne
        ) as NSDictionary

        val cfQuery = CFBridgingRetain(query) as CFDictionaryRef
        val result = alloc<CFTypeRefVar>()

        val status = try {
            SecItemCopyMatching(cfQuery, result.ptr)
        } finally {
            CFBridgingRelease(cfQuery)
        }

        if (status == errSecSuccess && result.value != null) {
            val nsData = CFBridgingRelease(result.value) as? NSData
            nsData?.let {
                NSString.create(data = it, encoding = NSUTF8StringEncoding)?.toString()
            }
        } else {
            null
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun eliminarToken() {
        val query: NSDictionary = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service,
            kSecAttrAccount to account
        ) as NSDictionary

        val cfQuery = CFBridgingRetain(query) as CFDictionaryRef
        try {
            SecItemDelete(cfQuery)
        } finally {
            CFBridgingRelease(cfQuery)
        }
    }
}
