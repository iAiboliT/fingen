package app.fingenmodern.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

data class Money(val minor: Long, val currency: String = "RUB") : Comparable<Money> {
    init { require(currency.matches(Regex("^[A-Z]{3}$"))) { "Currency must be ISO-4217 code: " + currency } }
    operator fun plus(other: Money): Money { requireSameCurrency(other); return Money(Math.addExact(minor, other.minor), currency) }
    operator fun minus(other: Money): Money { requireSameCurrency(other); return Money(Math.subtractExact(minor, other.minor), currency) }
    operator fun unaryMinus(): Money = Money(Math.negateExact(minor), currency)
    override fun compareTo(other: Money): Int { requireSameCurrency(other); return minor.compareTo(other.minor) }
    fun abs(): Money = if (minor >= 0) this else -this
    fun isZero() = minor == 0L
    fun format(locale: Locale = Locale.Builder().setLanguage("ru").setRegion("RU").build()): String {
        val digits = Currency.getInstance(currency).defaultFractionDigits.coerceAtLeast(0)
        val value = BigDecimal.valueOf(minor).divide(BigDecimal.TEN.pow(digits))
        return NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(this@Money.currency)
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }.format(value)
    }
    private fun requireSameCurrency(other: Money) =
        require(currency == other.currency) { "Currency mismatch: " + currency + " != " + other.currency }
    companion object {
        fun rub(roubles: Long, kopecks: Int = 0): Money {
            require(kopecks in 0..99)
            return Money(Math.addExact(Math.multiplyExact(roubles, 100L), kopecks.toLong()), "RUB")
        }
        fun zero(currency: String = "RUB") = Money(0, currency)
    }
}
fun Iterable<Money>.sumMoney(currency: String = "RUB"): Money =
    fold(Money.zero(currency)) { acc, item -> acc + item }
internal fun percentOf(amountMinor: Long, percent: BigDecimal): Long =
    BigDecimal.valueOf(amountMinor).multiply(percent)
        .divide(BigDecimal("100"), 0, RoundingMode.HALF_UP).longValueExact()
