package app.fingenmodern.ui.components
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.fingenmodern.domain.Money
@Composable fun MetricCard(title:String,amount:Money,subtitle:String,modifier:Modifier=Modifier){
    ElevatedCard(modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text(title,style=MaterialTheme.typography.titleMedium);Text(amount.format(),style=MaterialTheme.typography.headlineSmall);Text(subtitle,style=MaterialTheme.typography.bodySmall)}}
}
@Composable fun TextCard(title:String,body:String,modifier:Modifier=Modifier){
    ElevatedCard(modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text(title,style=MaterialTheme.typography.titleMedium);Text(body,style=MaterialTheme.typography.bodyMedium)}}
}
