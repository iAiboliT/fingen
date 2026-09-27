package app.fingenmodern.domain

import java.time.temporal.ChronoUnit
import java.util.Locale

data class ReceiptTransactionMatch(val transactionId:Long,val score:Int,val reasons:List<String>)

object ReceiptTransactionMatcher {
    fun rank(receipt:Receipt,candidates:List<Transaction>):List<ReceiptTransactionMatch>{
        val receiptDate=receipt.dateTime?.toLocalDate() ?: return emptyList()
        return candidates.mapNotNull { transaction ->
            if(transaction.type!=TransactionType.Expense || transaction.amount.currency!=receipt.total.currency || transaction.amount.minor!=receipt.total.minor) return@mapNotNull null
            val days=kotlin.math.abs(ChronoUnit.DAYS.between(receiptDate,transaction.date))
            if(days>2) return@mapNotNull null
            var score=70
            val reasons=mutableListOf("совпадает сумма")
            when(days){0L->{score+=25;reasons+="та же дата"};1L->{score+=15;reasons+="соседняя дата"};else->{score+=5;reasons+="рядом по дате"}}
            val merchant=receipt.merchantName?.trim()?.lowercase(Locale.ROOT)
            val note=transaction.note.orEmpty().lowercase(Locale.ROOT)
            if(!merchant.isNullOrBlank()&&merchant.length>=4){
                val tokens=merchant.split(Regex("""[^\p{L}\p{N}]+""")).filter{it.length>=4}
                if(tokens.any{note.contains(it)}){score+=5;reasons+="совпадает продавец"}
            }
            ReceiptTransactionMatch(transaction.id,score,reasons)
        }.sortedByDescending{it.score}
    }
}
