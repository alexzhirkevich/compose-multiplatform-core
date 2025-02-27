/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.foundation.selection

import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.TapIndicationDelay
import androidx.compose.foundation.TestIndication
import androidx.compose.foundation.TestIndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.setFocusableContent
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertModifierIsPure
import androidx.compose.testutils.first
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.InputMode.Companion.Touch
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class ToggleableTest {

    @get:Rule val rule = createComposeRule()

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    // TODO(b/267253920): Add a compose test API to set/reset InputMode.
    @After
    fun resetTouchMode() =
        with(InstrumentationRegistry.getInstrumentation()) {
            if (SDK_INT < 33) setInTouchMode(true) else resetInTouchMode()
        }

    @Test
    fun toggleableTest_defaultSemantics() {
        rule.setContent {
            Column {
                Box(
                    Modifier.triStateToggleable(state = ToggleableState.On, onClick = {})
                        .testTag("checkedToggleable"),
                    content = { BasicText("ToggleableText") }
                )
                Box(
                    Modifier.triStateToggleable(state = ToggleableState.Off, onClick = {})
                        .testTag("unCheckedToggleable"),
                    content = { BasicText("ToggleableText") }
                )
                Box(
                    Modifier.triStateToggleable(state = ToggleableState.Indeterminate, onClick = {})
                        .testTag("indeterminateToggleable"),
                    content = { BasicText("ToggleableText") }
                )
            }
        }

        fun hasIndeterminateState(): SemanticsMatcher =
            SemanticsMatcher.expectValue(
                SemanticsProperties.ToggleableState,
                ToggleableState.Indeterminate
            )

        fun roleNotSet(): SemanticsMatcher =
            SemanticsMatcher.keyNotDefined(SemanticsProperties.Role)

        rule
            .onNodeWithTag("checkedToggleable")
            .assert(roleNotSet())
            .assertIsEnabled()
            .assertIsOn()
            .assertHasClickAction()
        rule
            .onNodeWithTag("unCheckedToggleable")
            .assert(roleNotSet())
            .assertIsEnabled()
            .assertIsOff()
            .assertHasClickAction()
        rule
            .onNodeWithTag("indeterminateToggleable")
            .assert(roleNotSet())
            .assertIsEnabled()
            .assert(hasIndeterminateState())
            .assertHasClickAction()
    }

    @Test
    fun toggleableTest_booleanOverload_defaultSemantics() {
        rule.setContent {
            Column {
                Box(
                    Modifier.toggleable(value = true, onValueChange = {})
                        .testTag("checkedToggleable"),
                    content = { BasicText("ToggleableText") }
                )
                Box(
                    Modifier.toggleable(value = false, onValueChange = {})
                        .testTag("unCheckedToggleable"),
                    content = { BasicText("ToggleableText") }
                )
            }
        }

        rule
            .onNodeWithTag("checkedToggleable")
            .assertIsEnabled()
            .assertIsOn()
            .assertHasClickAction()
        rule
            .onNodeWithTag("unCheckedToggleable")
            .assertIsEnabled()
            .assertIsOff()
            .assertHasClickAction()
    }

    @Test
    fun toggleableTest_disabledSemantics() {
        rule.setContent {
            Box {
                Box(
                    Modifier.triStateToggleable(
                        state = ToggleableState.On,
                        onClick = {},
                        enabled = false
                    ),
                    content = { BasicText("ToggleableText") }
                )
            }
        }

        rule.onNode(isToggleable()).assertIsNotEnabled().assertHasClickAction()
    }

    @Test
    fun toggleableTest_toggle() {
        var checked = true
        val onCheckedChange: (Boolean) -> Unit = { checked = it }

        rule.setContent {
            Box {
                Box(
                    Modifier.toggleable(value = checked, onValueChange = onCheckedChange),
                    content = { BasicText("ToggleableText") }
                )
            }
        }

        rule.onNode(isToggleable()).performClick()

        rule.runOnIdle { assertThat(checked).isEqualTo(false) }
    }

    @Test
    fun toggleableTest_toggle_consumedWhenDisabled() {
        val enabled = mutableStateOf(false)
        var checked = true
        val onCheckedChange: (Boolean) -> Unit = { checked = it }
        var outerChecked = true
        val outerOnCheckedChange: (Boolean) -> Unit = { outerChecked = it }

        rule.setContent {
            Box(Modifier.toggleable(value = outerChecked, onValueChange = outerOnCheckedChange)) {
                BasicText(
                    "ToggleableText",
                    modifier =
                        Modifier.testTag("myToggleable")
                            .toggleable(
                                value = checked,
                                onValueChange = onCheckedChange,
                                enabled = enabled.value
                            )
                )
            }
        }

        rule.onNodeWithTag("myToggleable").performClick()

        rule.runOnIdle {
            assertThat(checked).isTrue()
            assertThat(outerChecked).isTrue()
            enabled.value = true
        }

        rule.onNodeWithTag("myToggleable").performClick()

        rule.runOnIdle {
            assertThat(checked).isFalse()
            assertThat(outerChecked).isTrue()
        }
    }

    @Test
    fun toggleableTest_interactionSource_noScrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                Box(
                    Modifier.toggleable(
                        value = true,
                        interactionSource = interactionSource,
                        indication = null,
                        onValueChange = {}
                    )
                ) {
                    BasicText("ToggleableText")
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithText("ToggleableText").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithText("ToggleableText").performTouchInput { up() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun toggleableTest_interactionSource_resetWhenDisposed_noScrollableContainer() {
        val interactionSource = MutableInteractionSource()
        var emitToggleableText by mutableStateOf(true)

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                if (emitToggleableText) {
                    Box(
                        Modifier.toggleable(
                            value = true,
                            interactionSource = interactionSource,
                            indication = null,
                            onValueChange = {}
                        )
                    ) {
                        BasicText("ToggleableText")
                    }
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithText("ToggleableText").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        // Dispose toggleable
        rule.runOnIdle { emitToggleableText = false }

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun toggleableTest_interactionSource_scrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                Box(
                    Modifier.toggleable(
                        value = true,
                        interactionSource = interactionSource,
                        indication = null,
                        onValueChange = {}
                    )
                ) {
                    BasicText("ToggleableText")
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithText("ToggleableText").performTouchInput { down(center) }

        // Advance past the tap timeout
        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithText("ToggleableText").performTouchInput { up() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun toggleableTest_interactionSource_resetWhenDisposed_scrollableContainer() {
        val interactionSource = MutableInteractionSource()
        var emitToggleableText by mutableStateOf(true)

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                if (emitToggleableText) {
                    Box(
                        Modifier.toggleable(
                            value = true,
                            interactionSource = interactionSource,
                            indication = null,
                            onValueChange = {}
                        )
                    ) {
                        BasicText("ToggleableText")
                    }
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithText("ToggleableText").performTouchInput { down(center) }

        // Advance past the tap timeout
        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        // Dispose toggleable
        rule.runOnIdle { emitToggleableText = false }

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun toggleableTest_interactionSource_hover() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                Box(
                    Modifier.toggleable(
                        value = true,
                        interactionSource = interactionSource,
                        indication = null,
                        onValueChange = {}
                    )
                ) {
                    BasicText("ToggleableText")
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithText("ToggleableText").performMouseInput { enter(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
        }

        rule.onNodeWithText("ToggleableText").performMouseInput { exit(Offset(-1f, -1f)) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
            assertThat(interactions[1]).isInstanceOf(HoverInteraction.Exit::class.java)
            assertThat((interactions[1] as HoverInteraction.Exit).enter).isEqualTo(interactions[0])
        }
    }

    @Test
    fun toggleableTest_interactionSource_focus_inTouchMode() {
        val interactionSource = MutableInteractionSource()
        lateinit var scope: CoroutineScope
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager

        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box {
                Box(
                    Modifier.focusRequester(focusRequester)
                        .toggleable(
                            value = true,
                            interactionSource = interactionSource,
                            indication = null,
                            onValueChange = {}
                        )
                ) {
                    BasicText("ToggleableText")
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.runOnIdle {
            inputModeManager.requestInputMode(Touch)
            focusRequester.requestFocus()
        }

        // Touch mode by default, so we shouldn't be focused
        rule.runOnIdle { assertThat(interactions).isEmpty() }
    }

    @Test
    fun toggleableTest_interactionSource_focus_inKeyboardMode() {
        val interactionSource = MutableInteractionSource()
        lateinit var scope: CoroutineScope
        val focusRequester = FocusRequester()
        lateinit var focusManager: FocusManager
        lateinit var inputModeManager: InputModeManager

        rule.setFocusableContent {
            scope = rememberCoroutineScope()
            focusManager = LocalFocusManager.current
            inputModeManager = LocalInputModeManager.current
            Box {
                Box(
                    Modifier.focusRequester(focusRequester)
                        .toggleable(
                            value = true,
                            interactionSource = interactionSource,
                            indication = null,
                            onValueChange = {}
                        )
                ) {
                    BasicText("ToggleableText")
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        // Keyboard mode, so we should now be focused and see an interaction
        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }

        rule.runOnIdle { focusManager.clearFocus() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(FocusInteraction.Unfocus::class.java)
            assertThat((interactions[1] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
        }
    }

    // TODO: b/202871171 - add test for changing between keyboard mode and touch mode, making sure
    // it resets existing focus

    @Test
    fun toggleableText_testInspectorValue_noIndication() {
        rule.setContent {
            val modifier = Modifier.toggleable(value = true, onValueChange = {}) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("toggleable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "value",
                    "enabled",
                    "role",
                    "onValueChange",
                )
        }
    }

    @Test
    fun toggleableTest_testInspectorValue_fullParams() {
        rule.setContent {
            val modifier =
                Modifier.toggleable(
                        value = true,
                        onValueChange = {},
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    )
                    .first() as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("toggleable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "value",
                    "indicationNodeFactory",
                    "interactionSource",
                    "enabled",
                    "role",
                    "onValueChange"
                )
        }
    }

    @Test
    fun toggleableTest_testInspectorValueTriState_noIndication() {
        rule.setContent {
            val modifier =
                Modifier.triStateToggleable(state = ToggleableState.On, onClick = {})
                    as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("triStateToggleable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "state",
                    "enabled",
                    "role",
                    "onClick",
                )
        }
    }

    @Test
    fun toggleableTest_testInspectorValueTriState_fullParams() {
        rule.setContent {
            val modifier =
                Modifier.triStateToggleable(
                        state = ToggleableState.On,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .first() as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("triStateToggleable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "state",
                    "indicationNodeFactory",
                    "interactionSource",
                    "enabled",
                    "role",
                    "onClick"
                )
        }
    }

    @Test
    fun toggleable_minTouchTarget_clickOutsideLayoutBounds() {
        var toggled by mutableStateOf(false)
        val interactionSource = MutableInteractionSource()
        testToggleableMinTouchTarget {
            Modifier.toggleable(
                value = toggled,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = { toggled = true }
            )
        }
    }

    @Test
    fun triStateToggleable_minTouchTarget_clickOutsideLayoutBounds() {
        var toggleableState by mutableStateOf(ToggleableState.Off)
        val interactionSource = MutableInteractionSource()
        testToggleableMinTouchTarget {
            Modifier.triStateToggleable(
                state = toggleableState,
                interactionSource = interactionSource,
                indication = null,
                onClick = { toggleableState = ToggleableState.On }
            )
        }
    }

    @Test
    fun triStateToggleable_noInteractionSource_minTouchTarget_clickOutsideLayoutBounds() {
        var toggleableState by mutableStateOf(ToggleableState.Off)
        testToggleableMinTouchTarget {
            Modifier.triStateToggleable(
                state = toggleableState,
                onClick = { toggleableState = ToggleableState.On }
            )
        }
    }

    private fun testToggleableMinTouchTarget(modifier: () -> Modifier): Unit =
        with(rule.density) {
            val tag = "toggleable"
            rule.setContent {
                Box(Modifier.fillMaxSize()) {
                    Box(modifier().requiredSize(2.dp).testTag(tag)) { BasicText("ToggleableText") }
                }
            }

            rule
                .onNodeWithTag(tag)
                .assertIsOff()
                .assertWidthIsEqualTo(2.dp)
                .assertHeightIsEqualTo(2.dp)
                .assertTouchWidthIsEqualTo(48.dp)
                .assertTouchHeightIsEqualTo(48.dp)
                .performTouchInput { click(position = Offset(-1f, -1f)) }
                .assertIsOn()
        }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun toggleableTest_clickWithEnterKey() {
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        var toggled by mutableStateOf(false)
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BasicText(
                "ToggleableText",
                modifier =
                    Modifier.testTag("toggleable").focusRequester(focusRequester).toggleable(
                        value = toggled
                    ) {
                        toggled = it
                    }
            )
        }

        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val toggleableNode = rule.onNodeWithTag("toggleable")
        rule.runOnIdle { assertThat(toggled).isFalse() }

        toggleableNode.performKeyInput { keyDown(Key.Enter) }
        rule.runOnIdle { assertThat(toggled).isFalse() }

        toggleableNode.performKeyInput { keyUp(Key.Enter) }
        rule.runOnIdle { assertThat(toggled).isTrue() }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun toggleableTest_clickWithSpaceKey() {
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        var toggled by mutableStateOf(false)
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BasicText(
                "ToggleableText",
                modifier =
                    Modifier.testTag("toggleable").focusRequester(focusRequester).toggleable(
                        value = toggled
                    ) {
                        toggled = it
                    }
            )
        }

        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val toggleableNode = rule.onNodeWithTag("toggleable")
        rule.runOnIdle { assertThat(toggled).isFalse() }

        toggleableNode.performKeyInput { keyDown(Key.Spacebar) }
        rule.runOnIdle { assertThat(toggled).isFalse() }

        toggleableNode.performKeyInput { keyUp(Key.Spacebar) }
        rule.runOnIdle { assertThat(toggled).isTrue() }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun toggleableTest_clickWithNumPadEnterKey() {
        val focusRequester = FocusRequester()
        var toggled by mutableStateOf(false)
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BasicText(
                "ToggleableText",
                modifier =
                    Modifier.testTag("toggleable").focusRequester(focusRequester).toggleable(
                        value = toggled
                    ) {
                        toggled = it
                    }
            )
        }

        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val toggleableNode = rule.onNodeWithTag("toggleable")
        rule.runOnIdle { assertThat(toggled).isFalse() }

        toggleableNode.performKeyInput { keyDown(Key.NumPadEnter) }
        rule.runOnIdle { assertThat(toggled).isFalse() }

        toggleableNode.performKeyInput { keyUp(Key.NumPadEnter) }
        rule.runOnIdle { assertThat(toggled).isTrue() }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun toggleableTest_clickWithDpadCenter() {
        val focusRequester = FocusRequester()
        var toggled by mutableStateOf(false)
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BasicText(
                "ToggleableText",
                modifier =
                    Modifier.testTag("toggleable").focusRequester(focusRequester).toggleable(
                        value = toggled
                    ) {
                        toggled = it
                    }
            )
        }

        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val toggleableNode = rule.onNodeWithTag("toggleable")
        rule.runOnIdle { assertThat(toggled).isFalse() }

        toggleableNode.performKeyInput { keyDown(Key.DirectionCenter) }
        rule.runOnIdle { assertThat(toggled).isFalse() }

        toggleableNode.performKeyInput { keyUp(Key.DirectionCenter) }
        rule.runOnIdle { assertThat(toggled).isTrue() }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun toggleableTest_clickWithEnterKey_triStateToggleable() {
        val focusRequester = FocusRequester()
        var toggled by mutableStateOf(false)
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BasicText(
                "ToggleableText",
                modifier =
                    Modifier.testTag("toggleable")
                        .focusRequester(focusRequester)
                        .triStateToggleable(ToggleableState(toggled)) { toggled = !toggled }
            )
        }

        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val toggleableNode = rule.onNodeWithTag("toggleable")
        rule.runOnIdle { assertThat(toggled).isFalse() }

        toggleableNode.performKeyInput { keyDown(Key.Enter) }
        rule.runOnIdle { assertThat(toggled).isFalse() }

        toggleableNode.performKeyInput { keyUp(Key.Enter) }
        rule.runOnIdle { assertThat(toggled).isTrue() }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun toggleableTest_enterKey_emitsIndication() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            scope = rememberCoroutineScope()
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ToggleableText",
                    modifier =
                        Modifier.testTag("toggleable").focusRequester(focusRequester).toggleable(
                            value = false,
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }

        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val interactions = mutableListOf<Interaction>()
        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.onNodeWithTag("toggleable").performKeyInput { keyDown(Key.Enter) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("toggleable").performKeyInput { keyUp(Key.Enter) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions.last()).isInstanceOf(PressInteraction.Release::class.java)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun toggleableTest_numPadEnterKey_emitsIndication() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager

        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ToggleableText",
                    modifier =
                        Modifier.testTag("toggleable").focusRequester(focusRequester).toggleable(
                            value = false,
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }

        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val interactions = mutableListOf<Interaction>()
        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.onNodeWithTag("toggleable").performKeyInput { keyDown(Key.NumPadEnter) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("toggleable").performKeyInput { keyUp(Key.NumPadEnter) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions.last()).isInstanceOf(PressInteraction.Release::class.java)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun toggleableTest_dpadCenter_emitsIndication() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ToggleableText",
                    modifier =
                        Modifier.testTag("toggleable").focusRequester(focusRequester).toggleable(
                            value = false,
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }

        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }
        rule.waitForIdle()

        val interactions = mutableListOf<Interaction>()
        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.onNodeWithTag("toggleable").performKeyInput { keyDown(Key.DirectionCenter) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("toggleable").performKeyInput { keyUp(Key.DirectionCenter) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions.last()).isInstanceOf(PressInteraction.Release::class.java)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun toggleableTest_otherKey_doesNotEmitIndication() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ToggleableText",
                    modifier =
                        Modifier.testTag("toggleable").focusRequester(focusRequester).toggleable(
                            value = false,
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }

        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val interactions = mutableListOf<Interaction>()
        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.onNodeWithTag("toggleable").performKeyInput { pressKey(Key.Backspace) }
        rule.runOnIdle { assertThat(interactions).isEmpty() }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun toggleableTest_doubleEnterKey_emitsFurtherInteractions() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ToggleableText",
                    modifier =
                        Modifier.testTag("toggleable").focusRequester(focusRequester).toggleable(
                            value = false,
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }

        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val interactions = mutableListOf<Interaction>()
        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        val toggleableNode = rule.onNodeWithTag("toggleable")

        toggleableNode.performKeyInput { pressKey(Key.Enter) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
        }

        toggleableNode.performKeyInput { keyDown(Key.Enter) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(3)
            assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat(interactions[2]).isInstanceOf(PressInteraction.Press::class.java)
        }

        toggleableNode.performKeyInput { keyUp(Key.Enter) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(4)
            assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat(interactions[2]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[3]).isInstanceOf(PressInteraction.Release::class.java)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun toggleableTest_repeatKeyEvents_doNotEmitFurtherInteractions() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager
        var repeatCounter = 0
        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ToggleableText",
                    modifier =
                        Modifier.testTag("toggleable")
                            .focusRequester(focusRequester)
                            .onKeyEvent {
                                if (it.nativeKeyEvent.repeatCount != 0) repeatCounter++
                                false
                            }
                            .toggleable(
                                value = false,
                                interactionSource = interactionSource,
                                indication = null,
                            ) {}
                )
            }
        }

        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val interactions = mutableListOf<Interaction>()
        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.onNodeWithTag("toggleable").performKeyInput {
            keyDown(Key.Enter)

            advanceEventTime(500) // First repeat
            advanceEventTime(50) // Second repeat
        }

        rule.runOnIdle {
            // Ensure that expected number of repeats occurred and did not cause press interactions.
            assertThat(repeatCounter).isEqualTo(2)
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("toggleable").performKeyInput { keyUp(Key.Enter) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions.last()).isInstanceOf(PressInteraction.Release::class.java)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun toggleableTest_interruptedClick_emitsCancelIndication() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        val enabled = mutableStateOf(true)
        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager

        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ToggleableText",
                    modifier =
                        Modifier.testTag("toggleable").focusRequester(focusRequester).toggleable(
                            value = false,
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = enabled.value
                        ) {}
                )
            }
        }

        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val interactions = mutableListOf<Interaction>()
        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        val toggleableNode = rule.onNodeWithTag("toggleable")

        toggleableNode.performKeyInput { keyDown(Key.Enter) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        enabled.value = false

        toggleableNode.assertIsNotEnabled()

        rule.runOnIdle {
            // Filter out focus interactions.
            val pressInteractions = interactions.filterIsInstance<PressInteraction>()
            assertThat(pressInteractions).hasSize(2)
            assertThat(pressInteractions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(pressInteractions.last()).isInstanceOf(PressInteraction.Cancel::class.java)
        }

        // Key releases should not result in interactions.
        toggleableNode.performKeyInput { keyUp(Key.Enter) }

        // Make sure nothing has changed.
        rule.runOnIdle {
            val pressInteractions = interactions.filterIsInstance<PressInteraction>()
            assertThat(pressInteractions).hasSize(2)
            assertThat(pressInteractions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(pressInteractions.last()).isInstanceOf(PressInteraction.Cancel::class.java)
        }
    }

    @Test
    fun toggleableTest_noInteractionSource_lazilyCreated_pointerInput() {
        var created = false
        lateinit var interactionSource: InteractionSource
        val interactions = mutableListOf<Interaction>()
        val indication = TestIndicationNodeFactory { source, coroutineScope ->
            interactionSource = source
            created = true
            coroutineScope.launch {
                interactionSource.interactions.collect { interaction ->
                    interactions.add(interaction)
                }
            }
        }

        rule.setContent {
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "Toggleable",
                    modifier =
                        Modifier.testTag("toggleable").toggleable(
                            value = false,
                            interactionSource = null,
                            indication = indication
                        ) {}
                )
            }
        }

        rule.runOnIdle { assertThat(created).isFalse() }

        // The touch event should cause the indication node to be created
        rule.onNodeWithTag("toggleable").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(created).isTrue()
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    @Test
    fun triStateToggleable_noInteractionSource_lazilyCreated_pointerInput() {
        var created = false
        val state = ToggleableState(value = false)
        lateinit var interactionSource: InteractionSource
        val interactions = mutableListOf<Interaction>()
        val indication = TestIndicationNodeFactory { source, coroutineScope ->
            interactionSource = source
            created = true
            coroutineScope.launch {
                interactionSource.interactions.collect { interaction ->
                    interactions.add(interaction)
                }
            }
        }

        rule.setContent {
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "Toggleable",
                    modifier =
                        Modifier.testTag("toggleable").triStateToggleable(
                            state = state,
                            interactionSource = null,
                            indication = indication
                        ) {}
                )
            }
        }

        rule.runOnIdle { assertThat(created).isFalse() }

        // The touch event should cause the indication node to be created
        rule.onNodeWithTag("toggleable").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(created).isTrue()
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    @Test
    fun toggleable_composedOverload_nonEquality() {
        val onValueChange: (Boolean) -> Unit = {}
        val modifier1 = Modifier.toggleable(value = true, onValueChange = onValueChange)
        val modifier2 = Modifier.toggleable(value = true, onValueChange = onValueChange)

        // The composed overload can never compare equal
        assertThat(modifier1).isNotEqualTo(modifier2)
    }

    @Test
    fun toggleable_nullInteractionSourceNullIndication_equality() {
        val onValueChange: (Boolean) -> Unit = {}
        assertModifierIsPure { toggleInput ->
            Modifier.toggleable(
                value = toggleInput,
                interactionSource = null,
                indication = null,
                onValueChange = onValueChange
            )
        }
    }

    @Test
    fun toggleable_nonNullInteractionSourceNullIndication_equality() {
        val onValueChange: (Boolean) -> Unit = {}
        val interactionSource = MutableInteractionSource()
        assertModifierIsPure { toggleInput ->
            Modifier.toggleable(
                value = toggleInput,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = onValueChange
            )
        }
    }

    @Test
    fun toggleable_nullInteractionSourceNonNullIndicationNodeFactory_equality() {
        val onValueChange: (Boolean) -> Unit = {}
        val indication = TestIndicationNodeFactory({}, { _, _ -> })
        assertModifierIsPure { toggleInput ->
            Modifier.toggleable(
                value = toggleInput,
                interactionSource = null,
                indication = indication,
                onValueChange = onValueChange
            )
        }
    }

    @Test
    fun toggleable_nullInteractionSourceNonNullIndication_nonEquality() {
        val onValueChange: (Boolean) -> Unit = {}
        val indication = TestIndication {}
        val modifier1 =
            Modifier.toggleable(
                value = true,
                interactionSource = null,
                indication = indication,
                onValueChange = onValueChange
            )
        val modifier2 =
            Modifier.toggleable(
                value = true,
                interactionSource = null,
                indication = indication,
                onValueChange = onValueChange
            )

        // Indication requires composed, so cannot compare equal
        assertThat(modifier1).isNotEqualTo(modifier2)
    }

    @Test
    fun toggleable_nonNullInteractionSourceNonNullIndicationNodeFactory_equality() {
        val onValueChange: (Boolean) -> Unit = {}
        val interactionSource = MutableInteractionSource()
        val indication = TestIndicationNodeFactory({}, { _, _ -> })
        assertModifierIsPure { toggleInput ->
            Modifier.toggleable(
                value = toggleInput,
                interactionSource = interactionSource,
                indication = indication,
                onValueChange = onValueChange
            )
        }
    }

    @Test
    fun toggleable_nonNullInteractionSourceNonNullIndication_nonEquality() {
        val onValueChange: (Boolean) -> Unit = {}
        val interactionSource = MutableInteractionSource()
        val indication = TestIndication {}
        val modifier1 =
            Modifier.toggleable(
                value = true,
                interactionSource = interactionSource,
                indication = indication,
                onValueChange = onValueChange
            )
        val modifier2 =
            Modifier.toggleable(
                value = true,
                interactionSource = interactionSource,
                indication = indication,
                onValueChange = onValueChange
            )

        // Indication requires composed, so cannot compare equal
        assertThat(modifier1).isNotEqualTo(modifier2)
    }

    @Test
    fun triStateToggleable_composedOverload_nonEquality() {
        val onClick = {}
        val modifier1 = Modifier.triStateToggleable(state = ToggleableState.On, onClick = onClick)
        val modifier2 = Modifier.triStateToggleable(state = ToggleableState.On, onClick = onClick)

        // The composed overload can never compare equal
        assertThat(modifier1).isNotEqualTo(modifier2)
    }

    @Test
    fun triStateToggleable_nullInteractionSourceNullIndication_equality() {
        val onClick = {}
        assertModifierIsPure { toggleInput ->
            Modifier.triStateToggleable(
                state = ToggleableState(toggleInput),
                interactionSource = null,
                indication = null,
                onClick = onClick
            )
        }
    }

    @Test
    fun triStateToggleable_nonNullInteractionSourceNullIndication_equality() {
        val onClick = {}
        val interactionSource = MutableInteractionSource()
        assertModifierIsPure { toggleInput ->
            Modifier.triStateToggleable(
                state = ToggleableState(toggleInput),
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
        }
    }

    @Test
    fun triStateToggleable_nullInteractionSourceNonNullIndicationNodeFactory_equality() {
        val onClick = {}
        val indication = TestIndicationNodeFactory({}, { _, _ -> })
        assertModifierIsPure { toggleInput ->
            Modifier.triStateToggleable(
                state = ToggleableState(toggleInput),
                interactionSource = null,
                indication = indication,
                onClick = onClick
            )
        }
    }

    @Test
    fun triStateToggleable_nullInteractionSourceNonNullIndication_nonEquality() {
        val onClick = {}
        val indication = TestIndication {}
        val modifier1 =
            Modifier.triStateToggleable(
                state = ToggleableState.On,
                interactionSource = null,
                indication = indication,
                onClick = onClick
            )
        val modifier2 =
            Modifier.triStateToggleable(
                state = ToggleableState.On,
                interactionSource = null,
                indication = indication,
                onClick = onClick
            )

        // Indication requires composed, so cannot compare equal
        assertThat(modifier1).isNotEqualTo(modifier2)
    }

    @Test
    fun triStateToggleable_nonNullInteractionSourceNonNullIndicationNodeFactory_equality() {
        val onClick = {}
        val interactionSource = MutableInteractionSource()
        val indication = TestIndicationNodeFactory({}, { _, _ -> })
        assertModifierIsPure { toggleInput ->
            Modifier.triStateToggleable(
                state = ToggleableState(toggleInput),
                interactionSource = interactionSource,
                indication = indication,
                onClick = onClick
            )
        }
    }

    @Test
    fun triStateToggleable_nonNullInteractionSourceNonNullIndication_nonEquality() {
        val onClick = {}
        val interactionSource = MutableInteractionSource()
        val indication = TestIndication {}
        val modifier1 =
            Modifier.triStateToggleable(
                state = ToggleableState.On,
                interactionSource = interactionSource,
                indication = indication,
                onClick = onClick
            )
        val modifier2 =
            Modifier.triStateToggleable(
                state = ToggleableState.On,
                interactionSource = interactionSource,
                indication = indication,
                onClick = onClick
            )

        // Indication requires composed, so cannot compare equal
        assertThat(modifier1).isNotEqualTo(modifier2)
    }
}
