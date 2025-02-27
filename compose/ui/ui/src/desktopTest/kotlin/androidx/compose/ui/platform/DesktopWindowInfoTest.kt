/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.ui.scene.BaseComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.runApplicationTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopWindowInfoTest {

    @Test
    fun windowContainerSize() = runApplicationTest {
        launchTestApplication {
            val state = rememberWindowState(
                size = DpSize(123.dp, 321.dp)
            )
            Window(onCloseRequest = {}, state = state) {
                val containerSize = window.windowContext.windowInfo.containerSize
                assertEquals(123, containerSize.width)
                assertEquals(321, containerSize.height)
            }
        }
    }
}

private val ComposeScene.windowInfo: WindowInfo
    get() {
        this as BaseComposeScene
        return composeSceneContext.platformContext.windowInfo
    }
