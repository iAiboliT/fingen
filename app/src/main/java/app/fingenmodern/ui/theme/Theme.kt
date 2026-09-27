package app.fingenmodern.ui.theme
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
@Composable
fun FingenTheme(darkTheme:Boolean=isSystemInDarkTheme(),dynamicColor:Boolean=true,content:@Composable()->Unit){
    val context=LocalContext.current
    val colors=when{
        dynamicColor&&Build.VERSION.SDK_INT>=Build.VERSION_CODES.S&&darkTheme->dynamicDarkColorScheme(context)
        dynamicColor&&Build.VERSION.SDK_INT>=Build.VERSION_CODES.S->dynamicLightColorScheme(context)
        darkTheme->darkColorScheme()
        else->lightColorScheme()
    }
    MaterialTheme(colorScheme=colors,content=content)
}
