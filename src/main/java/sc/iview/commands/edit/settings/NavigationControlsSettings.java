/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2021 SciView developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package sc.iview.commands.edit.settings;

import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
import org.scijava.log.LogService;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.widget.NumberWidget;
import sc.iview.SciView;

import static sc.iview.commands.MenuWeights.*;

/**
 * A command for interactively editing step sizes and mouse sensitivity of all navigation controls.
 * @author Vladimir Ulman
 */
@Plugin(type = Command.class, initializer = "setupBoundsFromSciView", menuRoot = "SciView",
        menu = { @Menu(label = "Edit", weight = EDIT),
                 @Menu(label = "Settings", weight = EDIT_SETTINGS),
                 @Menu(label = "Controls", weight = EDIT_SETTINGS_CONTROLS) },
        label = "Input Controls Step Sizes and Mouse Sensitivities")
public class NavigationControlsSettings extends InteractiveCommand
{
    @Parameter
    private LogService logService;

    @Parameter
    private SciView sciView;

    @Parameter(label = "Small step size:", style = NumberWidget.SCROLL_BAR_STYLE, callback = "processSlowSpeed",
        description = "How much of a world coordinate the camera moves with W,A,S,D keys or mouse right click&drag.")
    private Float fpsSlowSpeed;

    @Parameter(label = "Large step size:", style = NumberWidget.SCROLL_BAR_STYLE, callback = "processFastSpeed",
            description = "How much of a world coordinate the camera moves with shift+ W,A,S,D keys or shift+ mouse right click&drag.")
    private Float fpsFastSpeed;

    @Parameter(label = "Very large step size:", style = NumberWidget.SCROLL_BAR_STYLE, callback = "processVeryFastSpeed",
            description = "How much of a world coordinate the camera moves with ctrl+shift+ W,A,S,D keys.")
    private Float fpsVeryFastSpeed;

    @Parameter(label = "Adjust all step sizes together:",
            description = "When locked (checked), all step sizes above are updated simultaneously.")
    private boolean adjustStepsLock = true;

    @Parameter(label = "Mouse move sensitivity:", style = NumberWidget.SCROLL_BAR_STYLE, callback = "processMouseMove",
            description = "Influences proportionally how much of a mouse move is required for an action in SciView.")
    private Float mouseMoveSensitivity;

    @Parameter(label = "Mouse scroll sensitivity:", style = NumberWidget.SCROLL_BAR_STYLE, callback = "processMouseScroll",
            description = "Influences proportionally how much of a mouse wheel scrolling is required for an action in SciView.")
    private Float mouseScrollSensitivity;


    //these follow SciView.setFPSSpeed()
    private final float baseSpeedIncr = 0.01f;
    private final float fastToSlowRatio = 20.0f;
    private final float veryFastToSlowRatio = 500.0f;
    private final float mouseMoveIncr = 0.02f;
    private final float mouseScrollIncr = 0.3f;

    //initiates GUI, all the spinners (scroll bars)
    private void setupBoundsFromSciView()
    {
        MutableModuleItem<Float> menuItem = getInfo().getMutableInput("fpsSlowSpeed", Float.class);
        if (menuItem == null) logService.error("Should never get here: Cannot find fpsSlowSpeed param.");
        //
        menuItem.setMinimumValue(SciView.FPSSPEED_MINBOUND_SLOW);
        menuItem.setMaximumValue(SciView.FPSSPEED_MAXBOUND_SLOW);
        menuItem.setStepSize(baseSpeedIncr);

        menuItem = getInfo().getMutableInput("fpsFastSpeed", Float.class);
        if (menuItem == null) logService.error("Should never get here: Cannot find fpsFastSpeed param.");
        //
        menuItem.setMinimumValue(SciView.FPSSPEED_MINBOUND_FAST);
        menuItem.setMaximumValue(SciView.FPSSPEED_MAXBOUND_FAST);
        menuItem.setStepSize(fastToSlowRatio * baseSpeedIncr);

        menuItem = getInfo().getMutableInput("fpsVeryFastSpeed", Float.class);
        if (menuItem == null) logService.error("Should never get here: Cannot find fpsVeryFastSpeed param.");
        //
        menuItem.setMinimumValue(SciView.FPSSPEED_MINBOUND_VERYFAST);
        menuItem.setMaximumValue(SciView.FPSSPEED_MAXBOUND_VERYFAST);
        menuItem.setStepSize(veryFastToSlowRatio * baseSpeedIncr);

        menuItem = getInfo().getMutableInput("mouseMoveSensitivity", Float.class);
        if (menuItem == null) logService.error("Should never get here: Cannot find mouseMoveSensitivity param.");
        //
        menuItem.setMinimumValue(SciView.MOUSESPEED_MINBOUND);
        menuItem.setMaximumValue(SciView.MOUSESPEED_MAXBOUND);
        menuItem.setStepSize(mouseMoveIncr);

        menuItem = getInfo().getMutableInput("mouseScrollSensitivity", Float.class);
        if (menuItem == null) logService.error("Should never get here: Cannot find mouseScrollSensitivity param.");
        //
        menuItem.setMinimumValue(SciView.MOUSESCROLL_MINBOUND);
        menuItem.setMaximumValue(SciView.MOUSESCROLL_MAXBOUND);
        menuItem.setStepSize(mouseScrollIncr);

        //backup the current state of SciView before we eventually override it
        //so that there is something to return to with the "first toggle"
        orig_fpsSlowSpeed = sciView.getControls().getParameters().getFpsSpeedSlow();
        orig_fpsFastSpeed = sciView.getControls().getParameters().getFpsSpeedFast();
        orig_fpsVeryFastSpeed = sciView.getControls().getParameters().getFpsSpeedVeryFast();
        orig_mouseMoveSensitivity = sciView.getControls().getParameters().getMouseSpeedMult();
        orig_mouseScrollSensitivity = sciView.getControls().getParameters().getMouseScrollMult();

        //try to retrieve stored dialog state and push it to SciView
        //so that the SciView and dialog states match
        final PrefService ps = getContext().getService(PrefService.class);
        if (ps == null) return;
        sciView.setFPSSpeedSlow(     ps.getFloat( NavigationControlsSettings.class, "fpsSlowSpeed", orig_fpsSlowSpeed) );
        sciView.setFPSSpeedFast(     ps.getFloat( NavigationControlsSettings.class, "fpsFastSpeed", orig_fpsFastSpeed) );
        sciView.setFPSSpeedVeryFast( ps.getFloat( NavigationControlsSettings.class, "fpsVeryFastSpeed", orig_fpsVeryFastSpeed) );
        sciView.setMouseSpeed(       ps.getFloat( NavigationControlsSettings.class, "mouseMoveSensitivity", orig_mouseMoveSensitivity) );
        sciView.setMouseScrollSpeed( ps.getFloat( NavigationControlsSettings.class, "mouseScrollSensitivity", orig_mouseScrollSensitivity) );
    }

    //updates GUI with fresh values
    private void updateDialogSpeedsAndMouseParams()
    {
        fpsSlowSpeed = sciView.getControls().getParameters().getFpsSpeedSlow();
        fpsFastSpeed = sciView.getControls().getParameters().getFpsSpeedFast();
        fpsVeryFastSpeed = sciView.getControls().getParameters().getFpsSpeedVeryFast();
        mouseMoveSensitivity = sciView.getControls().getParameters().getMouseSpeedMult();
        mouseScrollSensitivity = sciView.getControls().getParameters().getMouseScrollMult();
    }


    private void processSlowSpeed()
    {
        if (!adjustStepsLock)
            sciView.setFPSSpeedSlow( fpsSlowSpeed );
        else
            sciView.setFPSSpeed( fpsSlowSpeed );
        updateDialogSpeedsAndMouseParams();
    }
    private void processFastSpeed()
    {
        if (!adjustStepsLock)
            sciView.setFPSSpeedFast( fpsFastSpeed );
        else
            sciView.setFPSSpeed( fpsFastSpeed / fastToSlowRatio );
        updateDialogSpeedsAndMouseParams();
    }
    private void processVeryFastSpeed()
    {
        if (!adjustStepsLock)
            sciView.setFPSSpeedVeryFast( fpsVeryFastSpeed );
        else
            sciView.setFPSSpeed( fpsVeryFastSpeed / veryFastToSlowRatio );
        updateDialogSpeedsAndMouseParams();
    }

    private void processMouseMove()
    {
        sciView.setMouseSpeed( mouseMoveSensitivity );
        //updateDialogSpeedsAndMouseParams();
    }
    private void processMouseScroll()
    {
        sciView.setMouseScrollSpeed( mouseScrollSensitivity );
        //updateDialogSpeedsAndMouseParams();
    }


    @Parameter(label = "Click to re-read current state:", callback = "refreshDialog",
            description = "Changing its state triggers the dialog update -- useful when, e.g., step size is changed which keyboard shortcuts.")
    private boolean refreshToggle = false;
    private boolean firstHitOfRefreshToggle = true; //the "first toggle" flag

    private float orig_fpsSlowSpeed, orig_fpsFastSpeed, orig_fpsVeryFastSpeed;
    private float orig_mouseMoveSensitivity, orig_mouseScrollSensitivity;

    private void refreshDialog()
    {
        //only the "first toggle" will restore values in SciView
        //as they were at the time this dialog started,
        if (firstHitOfRefreshToggle)
        {
            sciView.setFPSSpeedSlow( orig_fpsSlowSpeed );
            sciView.setFPSSpeedFast( orig_fpsFastSpeed );
            sciView.setFPSSpeedVeryFast( orig_fpsVeryFastSpeed );
            sciView.setMouseSpeed( orig_mouseMoveSensitivity );
            sciView.setMouseScrollSpeed( orig_mouseScrollSensitivity );
            firstHitOfRefreshToggle = false;
        }

        //in any case, update the dialog to the current state of SciView
        updateDialogSpeedsAndMouseParams();
    }
}
