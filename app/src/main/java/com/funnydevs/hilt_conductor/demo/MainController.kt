package com.funnydevs.hilt_conductor.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bluelinelabs.conductor.Controller
import com.funnydevs.hilt_conductor.ConductorInterface
import com.funnydevs.hilt_conductor.annotations.ConductorEntryPoint
import com.funnydevs.hilt_conductor.annotations.ControllerScoped
import dagger.hilt.EntryPoints
import dagger.hilt.android.EntryPointAccessors
import javax.inject.Inject
import javax.inject.Named

@ConductorEntryPoint
class MainController(args: Bundle?) : BaseController(args) {



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        System.out.println()
        val view = inflater.inflate(R.layout.controller_main, container, false)
        view.findViewById<TextView>(R.id.tv_test).text = testo
        return view
    }
}