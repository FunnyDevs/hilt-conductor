package com.funnydevs.hilt_conductor;

import android.app.Activity;

import com.funnydevs.hilt_conductor.annotations.ControllerScoped;

import dagger.BindsInstance;
import dagger.hilt.DefineComponent;
import dagger.hilt.android.components.ActivityComponent;
import dagger.hilt.android.components.ActivityRetainedComponent;
import dagger.hilt.android.scopes.ActivityRetainedScoped;
import dagger.hilt.components.SingletonComponent;

@ControllerScoped
@DefineComponent(parent = ActivityRetainedComponent.class)
public interface ControllerComponent {

    @DefineComponent.Builder
    public interface Factory {
        public Factory activity(@BindsInstance Activity activity);
        public ControllerComponent create();
    }
}
