// Temporarily disabled due to compilation errors
/*
package com.spacetec.vehicle.di

import android.content.Context
import com.spacetec.vehicle.library.VehicleLibrary
import com.spacetec.vehicle.library.VehicleLookupService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing vehicle-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object VehicleModule {

    @Provides
    @Singleton
    fun provideVehicleLibrary(
        @ApplicationContext context: Context
    ): VehicleLibrary {
        return VehicleLibrary(context)
    }

    @Provides
    @Singleton
    fun provideVehicleLookupService(
        @ApplicationContext context: Context,
        vehicleLibrary: VehicleLibrary
    ): VehicleLookupService {
        return VehicleLookupService(context, vehicleLibrary)
    }
}
*/
