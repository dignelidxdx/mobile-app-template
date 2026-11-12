package com.example.lugarfavorito.ui

class LatestRequestGate {
    private var generation = 0L

    fun next(): Long = ++generation

    fun invalidate() {
        generation++
    }

    fun isCurrent(request: Long): Boolean = request == generation
}
