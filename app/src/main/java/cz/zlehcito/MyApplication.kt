package cz.zlehcito

import android.app.Application
import cz.zlehcito.di.appModule // Assuming appModule will be in a 'di' package
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.DEBUG) // Use Level.INFO or Level.NONE for release builds
            androidContext(this@MyApplication)
            modules(appModule) // We'll define appModule in the next step
        }
    }
}
