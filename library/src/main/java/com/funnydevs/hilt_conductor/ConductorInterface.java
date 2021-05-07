package com.funnydevs.hilt_conductor;


import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@EntryPoint
@InstallIn(SingletonComponent.class)
public interface ConductorInterface {

    public ConductorComponentLifecycleHandler Conductor_LifeCycleHandler();
}
