package com.example.ViDroidCall_Studio.data.nlu

import java.util.Calendar

/**
 * Interface trừu tượng hóa thời gian hệ thống
 * Giúp dễ dàng inject thời gian cố định trong Unit Test mà không phụ thuộc system clock thực tế.
 */
interface TimeProvider {
    fun getCurrentTimeMillis(): Long
    fun getCurrentHour(): Int
    fun getCurrentMinute(): Int
    fun getCurrentSecond(): Int

    companion object {
        fun createDefault(): TimeProvider = DefaultTimeProvider()
        fun createFixed(hour: Int, minute: Int, second: Int = 0): TimeProvider = FixedTimeProvider(hour, minute, second)
    }
}

class DefaultTimeProvider : TimeProvider {
    override fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

    override fun getCurrentHour(): Int {
        return Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    }

    override fun getCurrentMinute(): Int {
        return Calendar.getInstance().get(Calendar.MINUTE)
    }

    override fun getCurrentSecond(): Int {
        return Calendar.getInstance().get(Calendar.SECOND)
    }
}

class FixedTimeProvider(
    private val hour: Int,
    private val minute: Int,
    private val second: Int = 0
) : TimeProvider {
    override fun getCurrentTimeMillis(): Long = System.currentTimeMillis()
    override fun getCurrentHour(): Int = hour
    override fun getCurrentMinute(): Int = minute
    override fun getCurrentSecond(): Int = second
}
