package app.fingenmodern.di
import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.fingenmodern.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Module
@InstallIn(SingletonComponent::class)
object AppModule{
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context:Context):AppDatabase=
        Room.databaseBuilder(context,AppDatabase::class.java,"fingen_next.db")
            .setDriver(BundledSQLiteDriver())
            .addMigrations(DatabaseMigrations.MIGRATION_1_2,DatabaseMigrations.MIGRATION_2_3)
            .fallbackToDestructiveMigrationOnDowngrade().build()
    @Provides fun provideDao(db:AppDatabase)=db.financeDao()
    @Provides fun provideRepository(repo:RoomFinanceRepository):FinanceRepository=repo
}
