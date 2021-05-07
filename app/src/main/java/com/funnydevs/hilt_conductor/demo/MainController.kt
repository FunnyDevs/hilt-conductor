package com.funnydevs.hilt_conductor.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bluelinelabs.conductor.Controller
import com.funnydevs.hilt_conductor.annotations.ConductorEntryPoint
import com.funnydevs.hilt_conductor.annotations.ControllerScoped
import javax.inject.Inject

@ConductorEntryPoint
class MainController(args: Bundle?) : Controller(args) {

    @Inject
    lateinit var testo: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.controller_main, container, false)
        view.findViewById<TextView>(R.id.tv_test).text = testo
        return view
    }
}