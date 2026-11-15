package com.avinash.meadowfork.simulation

enum class DrawPurpose(val code: Int) {
    InitialGrassReady(1),
    InitialGrassTimer(2),
    FounderPlacement(3),
    Movement(4),
    Reproduction(5),
}

object KeyedRandom {
    private const val GAMMA = 0x9E3779B97F4A7C15uL

    private fun mix(input: ULong): ULong {
        var x = (input xor (input shr 30)) * 0xBF58476D1CE4E5B9uL
        x = (x xor (x shr 27)) * 0x94D049BB133111EBuL
        return x xor (x shr 31)
    }

    fun draw(seed: Long, day: Int, purpose: DrawPurpose, entity: List<Int>, bound: Int): Int {
        require(seed >= 0 && day >= 0 && bound > 0 && entity.all { it >= 0 })
        var state = mix(seed.toULong() + GAMMA)
        for (part in listOf(day, purpose.code, entity.size) + entity) {
            state = mix((state xor part.toULong()) + GAMMA)
        }
        return (state % bound.toULong()).toInt()
    }
}
