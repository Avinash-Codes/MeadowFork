package com.avinash.meadowfork

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform