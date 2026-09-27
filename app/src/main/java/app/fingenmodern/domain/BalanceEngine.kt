package app.fingenmodern.domain

class BalanceEngine {
    fun summarize(accounts: List<Account>, debts: List<DebtPosition> = emptyList(), currency: String = "RUB"): BalanceSummary {
        val relevant = accounts.filter { it.balance.currency == currency && it.includeInNetWorth && !it.archived }
        val positiveBalances = relevant.sumOf { it.balance.minor.coerceAtLeast(0) }
        val otherAccountLiabilities = relevant
            .filter { it.type !in setOf(AccountType.CreditCard, AccountType.Loan) }
            .sumOf { (-it.balance.minor).coerceAtLeast(0) }
        val accountBankDebt = relevant
            .filter { it.type in setOf(AccountType.CreditCard, AccountType.Loan) }
            .sumOf { (-it.balance.minor).coerceAtLeast(0) }
        val receivable = debts.filter { it.remaining.currency == currency && it.kind == DebtKind.TheyOweMe && it.status != DebtStatus.Closed }
            .sumOf { it.remaining.minor.coerceAtLeast(0) }
        val personalPayable = debts.filter { it.remaining.currency == currency && it.kind == DebtKind.IOwePerson && it.status != DebtStatus.Closed }
            .sumOf { it.remaining.minor.coerceAtLeast(0) }
        val explicitBankDebt = debts.filter {
            it.remaining.currency == currency && it.kind in setOf(DebtKind.BankLoan, DebtKind.CreditCard) && it.status != DebtStatus.Closed
        }.sumOf { it.remaining.minor.coerceAtLeast(0) }
        val bankDebt = if (explicitBankDebt > 0) explicitBankDebt else accountBankDebt
        val assets = Math.addExact(positiveBalances, receivable)
        val liabilities = Math.addExact(otherAccountLiabilities + personalPayable, bankDebt)
        val availableCash = relevant.filter { it.type in setOf(AccountType.Cash, AccountType.DebitCard, AccountType.Savings) }
            .sumOf { it.balance.minor.coerceAtLeast(0) }
        return BalanceSummary(
            Money(assets, currency), Money(liabilities, currency),
            Money(Math.subtractExact(assets, liabilities), currency),
            Money(availableCash, currency), Money(receivable, currency),
            Money(personalPayable, currency), Money(bankDebt, currency)
        )
    }
}
