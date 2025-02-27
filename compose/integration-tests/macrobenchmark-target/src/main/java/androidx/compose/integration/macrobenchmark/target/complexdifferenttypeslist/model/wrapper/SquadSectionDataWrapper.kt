/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.model.wrapper

import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.model.ui.SectionHeaderUiModel
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.model.ui.SquadPlayerUiModel

/**
 * Wraps [SectionHeaderUiModel] and list of [SquadPlayerUiModel]. Represents list of players by
 * their positions with section header.
 */
data class SquadSectionDataWrapper(
    val sectionHeader: SectionHeaderUiModel,
    val playersList: List<SquadPlayerUiModel>
)
