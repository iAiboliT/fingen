package app.fingenmodern
import android.app.Application
import app.fingenmodern.data.DatabaseSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltAndroidApp
class FingenModernApp:Application(){
    @Inject lateinit var seeder:DatabaseSeeder
    override fun onCreate(){super.onCreate();CoroutineScope(SupervisorJob()+Dispatchers.IO).launch{seeder.seedIfNeeded()}}
}
