package pe.edu.upeu.pharmamobile2

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform