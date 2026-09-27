package app.fingenmodern.data

import androidx.room3.Entity
import androidx.room3.Index

@Entity(tableName="receipts", indices=[Index(value=["fiscalKey"], unique=true), Index("occurredAt")])
data class ReceiptEntity(
    @androidx.room3.PrimaryKey(autoGenerate=true) val id:Long=0,
    val fiscalKey:String, val rawQr:String, val merchantName:String?, val merchantInn:String?,
    val occurredAt:String?, val totalMinor:Long, val currency:String
)

@Entity(tableName="receipt_items", indices=[Index("receiptId"), Index(value=["receiptId","position"], unique=true)])
data class ReceiptItemEntity(
    @androidx.room3.PrimaryKey(autoGenerate=true) val id:Long=0,
    val receiptId:Long, val position:Int, val name:String, val quantity:String,
    val unitPriceMinor:Long, val totalMinor:Long, val currency:String, val suggestedCategory:String
)

@Entity(tableName="receipt_transactions", primaryKeys=["receiptId","transactionId"], indices=[Index("transactionId")])
data class ReceiptTransactionLinkEntity(val receiptId:Long, val transactionId:Long)
