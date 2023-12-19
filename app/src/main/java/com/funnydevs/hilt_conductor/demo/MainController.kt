package com.funnydevs.hilt_conductor.demo

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.funnydevs.hilt_conductor.annotations.ConductorEntryPoint

@ConductorEntryPoint
class MainController(args: Bundle?) : BaseController(args) {

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.controller_main, container, false)
        view.findViewById<TextView>(R.id.tv_test).text = "$text $secondText"
        return view
    }


}