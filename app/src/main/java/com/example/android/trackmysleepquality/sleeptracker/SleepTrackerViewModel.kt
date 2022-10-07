/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.*
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.*

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {


        /**
         * so now we take parameter
         * 1- dao so we can access the dao fun
         * 2- context to access the res ( strings , colors , images )
         */


        private var tonight = MutableLiveData<SleepNight?>()



        private var nights = database.getAllNights()

        private val night = MediatorLiveData<SleepNight>()


        /**
         * Variable that tells the Fragment to navigate to a specific [SleepQualityFragment]
         *
         * This is private because we don't want to expose setting this value to the Fragment.
         */
        private val _navigateToSleepQuality = MutableLiveData<SleepNight>()


        /**
         * If this is non-null, immediately navigate to [SleepQualityFragment] and call [doneNavigating]
         */
        val navigateToSleepQualtiy:LiveData<SleepNight>  get() = _navigateToSleepQuality


        /**
         * Converted nights to Spanned for displaying.
         */
        val nightsString = Transformations.map(nights) { nights ->
                formatNights(nights, application.resources)
        }




        private var _showSnackbarEvent = MutableLiveData<Boolean>()

        val showSnackBarEvent: LiveData<Boolean>
                get() = _showSnackbarEvent



        init {
            initializeTonight()
        }

        private  fun initializeTonight() {
                viewModelScope.launch {
                        tonight.value = getTonightFromDatabase()
                }
        }


        /**
         *  Handling the case of the stopped app or forgotten recording,
         *  the start and end times will be the same.j
         *
         *  If the start time and end time are not the same, then we do not have an unfinished
         *  recording.
         */
        private suspend fun getTonightFromDatabase(): SleepNight? {
                        var night = database.getTonight()
                        if (night?.endTimeMilli != night?.startTimeMilli ){
                                night = null
                        }
                       return night
        }

        /**
         * Executes when the START button is clicked.
         */
        fun onStartTracking(){
                viewModelScope.launch {
                        // Create a new night, which captures the current time,
                        // and insert it into the database.
                        val newNight = SleepNight()
                        insert(newNight)
                        tonight.value = getTonightFromDatabase()

                }
        }



        /**
         * Executes when the STOP button is clicked.
         */
        fun onStopTracking(){
                viewModelScope.launch {
                        // In Kotlin, the return@label syntax is used for specifying which function among
                        // several nested ones this statement returns from.
                        // In this case, we are specifying to return from launch(),
                        // not the lambda.
                        val oldNight = tonight.value?:return@launch

                        // Update the night in the database to add the end time.
                        oldNight.endTimeMilli = System.currentTimeMillis()


                        update(oldNight)

                        // Set state to navigate to the SleepQualityFragment
                        _navigateToSleepQuality.value = oldNight
                }
        }

        private suspend fun update(oldNight: SleepNight) {
                        database.update(oldNight)
        }

        private suspend fun insert(newNight: SleepNight) {
                database.insert(newNight)
        }

        private suspend fun Clear() {
                database.clear()
        }

        fun onClear(){
                viewModelScope.launch {
                        Clear()
                        tonight.value = null
                        _showSnackbarEvent.value = true
                }
        }

        fun doneShowingSnackbar() {
                _showSnackbarEvent.value = false
        }

        /**
         * make it visible if it is not pressed
         */
        val startButtonVisible = Transformations.map(tonight) {
                null == it
        }


        /**
         * only show after the start pressed
         */
        val stopButtonVisible = Transformations.map(tonight) {
                null != it
        }

        /**
         * if there is a database data show the btn
         */
        val clearButtonVisible = Transformations.map(nights) {
                it?.isNotEmpty()
        }



        /**
         * Call this immediately after navigating to [SleepQualityFragment]
         *
         * It will clear the navigation request, so if the user rotates their phone it won't navigate
         * twice.
         */
        fun doneNavigation(){
                _navigateToSleepQuality.value = null
        }



}

