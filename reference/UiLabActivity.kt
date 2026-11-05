package dev.alita.androidagent.zcode.runtime

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

/**
 * Phase 8 lab fixture (runbook §7.1/W7): a small, stable, known tree the
 * acceptance instrument drives when a vendor Settings screen proves flaky,
 * and the visible half of the self-test demo. Its effects (a counter, a
 * switch, echo text) only mutate in-process state — nothing to clean up.
 */
class UiLabActivity : Activity() {
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fixture-only privacy/critical checks; no system state is mutated.
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        if (intent.getBooleanExtra("secure", false)) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        if (intent.getBooleanExtra("black", false)) {
            window.decorView.setBackgroundColor(android.graphics.Color.BLACK)
            setContentView(android.view.View(this).apply { setBackgroundColor(android.graphics.Color.BLACK) })
            return
        }
        val pad = (resources.displayMetrics.density * 16).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        var count = 0
        val counter = TextView(this).apply { text = "count=0"; textSize = 18f }
        val btn = Button(this).apply { text = "Increment"; contentDescription = "lab increment button" }
        btn.setOnClickListener { count++; counter.text = "count=$count" }
        btn.setOnLongClickListener { counter.text = "long_press=1"; true }
        val sw = Switch(this).apply { text = "Lab switch"; contentDescription = "lab switch" }
        val edit = EditText(this).apply { hint = "type here" }
        val echo = TextView(this).apply { text = "echo:" }
        edit.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { echo.text = "echo:${s.toString().take(40)}" }
        })
        root.addView(TextView(this).apply { text = "UiLab fixture"; textSize = 22f })
        root.addView(counter); root.addView(btn); root.addView(sw); root.addView(edit); root.addView(echo)
        if (intent.getBooleanExtra("criticalFixture", false)) root.addView(Button(this).apply {
            text = "Factory Reset (lab only)"
            setOnClickListener { counter.text = "critical_clicked=1" }
        })
        setContentView(root)
    }
}

