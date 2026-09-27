package app.fingenmodern.domain
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FinancialEngineTest {
    @Test fun moneyIsExact() {
        assertEquals(1005L, Money.rub(10,5).minor)
        assertEquals(Money.rub(15), Money.rub(10)+Money.rub(5))
        assertEquals(Money.rub(-5), Money.rub(10)-Money.rub(15))
    }
    @Test fun creditLiabilityIsNotDoubleCounted() {
        val s=BalanceEngine().summarize(listOf(
            Account(1,"Карта",AccountType.DebitCard,Money.rub(100_000)),
            Account(2,"Кредитка",AccountType.CreditCard,Money.rub(-30_000))
        ))
        assertEquals(Money.rub(100_000),s.availableCash)
        assertEquals(Money.rub(30_000),s.bankDebt)
        assertEquals(Money.rub(70_000),s.netWorth)
    }
    @Test fun personalDebtIssueDoesNotChangeNetWorth() {
        val p=DebtEngine().preview(DebtAction.GiveMoney(1,"Иван",Money.rub(10_000),LocalDate.now()))
        assertEquals(Money.rub(-10_000),p.walletEffect)
        assertTrue(p.netWorthEffect.isZero())
    }
    @Test fun minimumPaymentNeverExceedsDebt() {
        val t=CreditCardRuleTemplate("test",bankName="Test",productName="Card",annualPurchaseRatePercent=BigDecimal("36"),
            gracePeriodModel=GracePeriodModel.Custom,graceLengthDays=50,paymentWindowDays=25,
            minimumPaymentPercent=BigDecimal("10"),minimumPaymentFloor=Money.rub(600),interestStartRule=InterestStartRule.FromGraceEndDate)
        val p=CreditCardInterestEngine().projectStatement(t,
            listOf(CreditCardOperation(date=LocalDate.of(2026,9,1),amount=Money.rub(1_000),type=CreditOperationType.Purchase)),
            LocalDate.of(2026,9,20),LocalDate.of(2026,9,25))
        assertEquals(Money.rub(600),p.minimumPayment)
        assertTrue(p.estimatedInterestIfPaidOn.isZero())
    }
}
