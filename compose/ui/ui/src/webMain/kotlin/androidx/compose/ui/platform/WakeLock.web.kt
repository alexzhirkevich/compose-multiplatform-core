/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.platform

internal interface WakeLock {

    /**
     * If the screen will be awake **while the application is visible**
     * */
    val isAcquired : Boolean

    /**
     * Request the screen wake lock.
     *
     * If the app is not visible at the moment, actual lock will be acquired when it became
     * visible again and re-acquired on every future visibility change until released.
     * */
    suspend fun request()

    suspend fun release()
}

internal expect fun wakeLock() : WakeLock