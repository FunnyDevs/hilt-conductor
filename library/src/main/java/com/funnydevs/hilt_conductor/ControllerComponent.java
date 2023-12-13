package com.funnydevs.hilt_conductor;

import android.app.Activity;

import com.bluelinelabs.conductor.Controller;
import com.funnydevs.hilt_conductor.annotations.ControllerScoped;

import dagger.BindsInstance;
import dagger.hilt.DefineComponent;
import dagger.hilt.android.components.ActivityRetainedComponent;

@ControllerScoped
@DefineComponent(parent = ActivityRetainedComponent.class)
public interface ControllerComponent {

    @DefineComponent.Builder
    public interface Factory {
        Factory activity(@BindsInstance Activity activity);
        Factory controller(@BindsInstance Controller controller);
        public ControllerComponent create();
    }
}
