package com.funnydevs.hilt_conductor;

import android.app.Activity;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.components.ActivityComponent;
import dagger.hilt.android.scopes.ActivityRetainedScoped;
import dagger.hilt.android.scopes.ActivityScoped;
import dagger.hilt.internal.GeneratedComponentManager;

@ActivityScoped
public class ConductorComponentLifecycleHandler implements GeneratedComponentManager<ControllerComponent> {

    private ControllerComponent.Factory factory;
    private ControllerComponent controllerComponent;

    @Inject
    public ConductorComponentLifecycleHandler(ControllerComponent.Factory factory) {
        this.factory = factory;
    }

    public void inject(Activity activity) {
        controllerComponent = factory
                .activity(activity)
                .create();
    }

    @Override
    public ControllerComponent generatedComponent() {
        return controllerComponent;
    }

    public void destroy(){
        controllerComponent = null;
    }

}
