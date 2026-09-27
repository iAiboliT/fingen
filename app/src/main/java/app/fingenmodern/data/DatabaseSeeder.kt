package app.fingenmodern.data
import app.fingenmodern.domain.RussianCreditCardTemplateSamples
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class DatabaseSeeder @Inject constructor(private val dao:FinanceDao){
    suspend fun seedIfNeeded(){
        if(dao.countAccounts()==0){
            val date=LocalDate.now().toString()
            dao.insertAccount(AccountEntity(name="Наличные",type="Cash",openingBalanceMinor=0,openingDate=date))
            dao.insertAccount(AccountEntity(name="Основная карта",type="DebitCard",openingBalanceMinor=0,openingDate=date))
            dao.insertAccount(AccountEntity(name="Сбережения",type="Savings",openingBalanceMinor=0,openingDate=date))
            dao.insertAccount(AccountEntity(name="Кредитная карта",type="CreditCard",openingBalanceMinor=0,openingDate=date))
        }
        if(dao.countCreditCardTemplates()==0) RussianCreditCardTemplateSamples.editableSamples.forEach{t->
            dao.insertCreditCardTemplate(CreditCardTemplateEntity(t.id,t.version,t.bankName,t.productName,t.annualPurchaseRatePercent.toPlainString(),t.annualCashRatePercent?.toPlainString(),t.gracePeriodModel.name,t.graceLengthDays,t.statementDayOfMonth,t.paymentWindowDays,t.minimumPaymentPercent.toPlainString(),t.minimumPaymentFloor.minor,t.minimumPaymentFloor.currency,t.interestStartRule.name,t.cashOperationsBreakGrace,t.notes))
        }
    }
}
