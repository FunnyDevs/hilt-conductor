package com.funnydevs.hilt_conductor;


import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ActivityComponent;
import dagger.hilt.android.components.ActivityRetainedComponent;
import dagger.hilt.components.SingletonComponent;

@EntryPoint
@InstallIn(ActivityComponent.class)
public interface ConductorInterface {

    public ConductorComponentLifecycleHandler Conductor_LifeCycleHandler();
}
