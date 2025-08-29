package com.spacetec.di

import android.content.Context
import com.spacetec.obd.ObdManager
import com.spacetec.obd.RealObdManager
import com.spacetec.vehicle.VehicleDataRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideObdManager(@ApplicationContext context: Context): ObdManager {
        return RealObdManager(context) as ObdManager
    }
    
    @Provides
    @Singleton
    fun provideVehicleDataRepository(obdManager: ObdManager): VehicleDataRepository {
        return VehicleDataRepository(obdManager)
    }
}
