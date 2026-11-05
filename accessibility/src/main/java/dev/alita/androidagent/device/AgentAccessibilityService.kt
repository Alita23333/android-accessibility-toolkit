package dev.alita.androidagent.device

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.os.Bundle
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Phase 8 W1 (runbook §3.1, §7.1–§7.2): the accessibility half of UI
 * automation. THIS class owns the live-service latch, the event cache, the
 * bounded traversal (the raw node list) and the action primitives incl. the
 * gesture fallback. Every model-facing rule (normalization, budgets, role
 * mapping, rendering) lives in assets/runtime/ui-tree.cjs (amendment C6) —
 * the raw list produced here is handed to the bridge /ui/render hop by the
 * worker and never reaches the model.
 *
 * Process model (§7.2): same process as NodeService, separate Service class,
 * no android:process; traversal/actions run on one single-threaded executor;
 * events only set dirty flags and update the cache.
 */
class AgentAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ZcodeA11y"
        const val RAW_MAX_NODES = 4000          // §3.1 hard traversal bound
        const val VERIFY_WINDOW_MS = 1500L      // §3.4 action verification poll
        const val VERIFY_TICK_MS = 100L

        /** The live connected instance; null whenever the service is down. */
        @Volatile var instance: AgentAccessibilityService? = null
            private set

        @Volatile var boundSince: Long = 0
            private set

        /** Re-probed per request (§7.2: never cache "service is up"). */
        fun serviceUp(): Boolean {
            val svc = instance ?: return false
            // measured on device: AccessibilityServiceInfo.getId() does NOT
            // always equal ComponentName.flattenToString() — match on the
            // resolved service class name, which is unambiguous
            val cls = AgentAccessibilityService::class.java.name
            return svc.getSystemService(AccessibilityManager::class.java)
                ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                ?.any { info ->
                    info.id.endsWith(cls) ||
                        info.resolveInfo?.serviceInfo?.name == cls ||
                        info.resolveInfo?.serviceInfo?.packageName == svc.packageName
                } == true
        }

        /** Legacy diagnostics bridge. Production model calls use the Phase 8 carrier. */
        fun current(): AgentAccessibilityService? = instance

        /** Frame cache entry for one generation (§3.5: the last two stay resolvable). */
        class Frame(
            val generation: Int,
            val raw: JSONObject,                 // the raw snapshot JSON
            val fingerprint: String,             // comparison key of the emitted tree
            val windowId: Int,
            val renderedAt: Long,
        ) { val emittedToRaw = HashMap<Int, Int>() }
    }

    private val exec = Executors.newSingleThreadExecutor()
    val observationEpoch: String = java.util.UUID.randomUUID().toString()
    private val captureExec = Executors.newSingleThreadExecutor()
    @Volatile private var lastCaptureAt = 0L
    private val genCounter = AtomicInteger(0)
    private var currentFrame: Frame? = null
    private var prevFrame: Frame? = null

    // Event cache (§3.1 step 1 + §4.4): dirty flags invalidate the frame cache;
    // the counters feed ui_wait's events_seen audit trail.
    @Volatile private var contentDirty = false
    @Volatile private var windowDirty = false
    @Volatile private var lastScreenState: String? = null   // TYPE_WINDOW_STATE_CHANGED className
    @Volatile private var lastScreenStale = false
    @Volatile private var lastWindowId = -1
    private val eventCounts = HashMap<String, Int>()
    @Volatile private var lastEventTs: Long = 0

    // ---- lifecycle ------------------------------------------------------------

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            notificationTimeout = 100   // tuned (§7.2): never inherit an OEM default
        }
        instance = this
        boundSince = System.currentTimeMillis()
        Log.i(TAG, "service connected; ui.accessibility available")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        Log.w(TAG, "service unbound — ui.accessibility PENDING")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onInterrupt() { /* best effort; nothing to flush */ }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        lastEventTs = System.currentTimeMillis()
        val key = "type${event.eventType}"
        synchronized(eventCounts) { eventCounts[key] = (eventCounts[key] ?: 0) + 1 }
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                event.className?.let { cls ->
                    lastScreenState = cls.toString()
                    lastScreenStale = true
                }
                windowDirty = true
                contentDirty = true
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> { contentDirty = true }
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> { windowDirty = true; contentDirty = true }
            else -> {}
        }
    }

    fun eventCountsSnapshot(): Map<String, Int> = synchronized(eventCounts) { HashMap(eventCounts) }

    // ---- capture (§3.1): the bounded walk ------------------------------------

    /**
     * Captures the active window into a raw snapshot JSON (§3.1 step 2–4).
     * MUST run on the executor (single writer; §7.2 "no blocking on main").
     * The generation rule (§3.5/A13): identical trees reuse the generation;
     * changed trees mint gen+1 and keep the previous frame resolvable.
     */
    fun captureRaw(): CaptureResult {
        if (!serviceUp()) return CaptureResult(null, error = "ui_a11y_disabled")
        val windows = windows ?: emptyList()
        val active: AccessibilityWindowInfo? = windows.firstOrNull { it.isActive && it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
        val root: AccessibilityNodeInfo = active?.root ?: rootInActiveWindow
            ?: return CaptureResult(null, error = "ui_no_window")
        val winId = active?.id ?: root.windowId
        lastWindowId = winId

        val truncated = booleanArrayOf(false)
        val rootJson = buildTree(root, truncated)
        val rawCount = countTree(rootJson)

        // API 36 stub: AccessibilityWindowInfo.getPackageName() is gone — the
        // root node's package is the authority anyway
        val pkg: String = root.packageName?.toString() ?: ""
        val screen = lastScreenState ?: ""
        val bounds = Rect().also { active?.getBoundsInScreen(it) }
        val winJson = JSONObject()
            .put("id", winId)
            .put("type", "application")
            .put("active", true)
            .put("focused", active?.isFocused ?: true)
            .put("layer", active?.layer ?: 0)
            .put("bounds", JSONArray().put(bounds.left).put(bounds.top).put(bounds.right).put(bounds.bottom))
            .put("pkg", pkg)
        if (active?.title != null) winJson.put("title", active.title.toString())

        val fingerprint = treeFingerprint(rootJson) + "|w$winId|$pkg"

        // §3.5 generation rule: identical tree + same window => reuse
        val cur = currentFrame
        if (cur != null && cur.fingerprint == fingerprint) {
            val age = System.currentTimeMillis() - cur.renderedAt
            val rawReuse = JSONObject(cur.raw.toString())
                .put("generation", cur.generation)
                .put("renderedAt", java.time.Instant.ofEpochMilli(cur.renderedAt).toString())
                .put("ageMs", age)
            contentDirty = false
            return CaptureResult(rawReuse, frame = cur)
        }
        val gen = genCounter.incrementAndGet()   // starts at 1 (A19: g0 is the sentinel)
        val raw = JSONObject()
            .put("observationEpoch", observationEpoch)
            .put("generation", gen)
            .put("renderedAt", java.time.Instant.now().toString())
            .put("ageMs", 0)
            .put("app", pkg)
            .put("screen", screen)
            .put("screenStale", lastScreenStale)
            .put("window", winJson)
            .put("nodes", JSONArray().put(rootJson))
            .put("truncatedRaw", truncated[0])
            .put("rawCount", rawCount)
        val frame = Frame(gen, raw, fingerprint, winId, System.currentTimeMillis())
        prevFrame = currentFrame
        currentFrame = frame
        contentDirty = false
        lastScreenStale = false
        return CaptureResult(raw, frame = frame)
    }

    /** Preorder serialization of the node tree, bounded by RAW_MAX_NODES. */
    private fun buildTree(node: AccessibilityNodeInfo, truncated: BooleanArray): JSONObject {
        var count = 0
        fun ser(n: AccessibilityNodeInfo): JSONObject? {
            if (count >= RAW_MAX_NODES) { truncated[0] = true; return null }
            count++
            val j = nodeToJson(n)
            val kids = JSONArray()
            for (i in 0 until n.childCount) {
                val c = try { n.getChild(i) } catch (t: Throwable) { null } ?: continue
                ser(c)?.let { kids.put(it) }
            }
            j.put("children", kids)
            return j
        }
        return ser(node)!!
    }

    private fun countTree(root: JSONObject): Int {
        var c = 0
        fun walkN(j: JSONObject) {
            c++
            val kids = j.optJSONArray("children") ?: return
            for (i in 0 until kids.length()) walkN(kids.getJSONObject(i))
        }
        walkN(root)
        return c
    }

    private fun nodeToJson(n: AccessibilityNodeInfo): JSONObject {
        val b = Rect().also { n.getBoundsInScreen(it) }
        // org.json stores a raw int[] as an opaque object ("[I@…") — build a
        // real JSONArray (measured on device: every bounds rendered [0,0][0,0])
        val j = JSONObject()
            .put("cls", n.className?.toString() ?: JSONObject.NULL)
            .put("vid", n.viewIdResourceName ?: JSONObject.NULL)
            .put("t", n.text?.toString() ?: JSONObject.NULL)
            .put("cd", n.contentDescription?.toString() ?: JSONObject.NULL)
            .put("sd", n.stateDescription?.toString() ?: JSONObject.NULL)
            .put("pkg", n.packageName?.toString() ?: JSONObject.NULL)
            .put("vis", n.isVisibleToUser)
            .put("en", n.isEnabled)
            .put("cl", n.isClickable)
            .put("lc", n.isLongClickable)
            .put("chk", n.isChecked)
            .put("chkable", n.isCheckable)
            .put("sel", n.isSelected)
            .put("foc", n.isFocused)
            .put("editable", n.isEditable)
            .put("scroll", n.isScrollable)
            .put("pwd", n.isPassword)
            .put("head", n.isHeading)
            .put("coll", n.collectionInfo != null)
            .put("b", JSONArray().put(b.left).put(b.top).put(b.right).put(b.bottom))
        n.paneTitle?.let { j.put("pane", it.toString()) }
        return j
    }

    private fun treeFingerprint(node: JSONObject): String {
        val sb = StringBuilder()
        fun walkN(j: JSONObject) {
            sb.append(j.optString("cls")).append('|')
                .append(j.optString("vid")).append('|')
                .append(j.optString("t")).append('|')
                .append(j.optString("cd")).append('|')
            val b = j.optJSONArray("b")
            if (b != null) sb.append(b.join(",")).append('|')
            val kids = j.optJSONArray("children")
            if (kids != null) for (i in 0 until kids.length()) walkN(kids.getJSONObject(i))
        }
        walkN(node)
        return Integer.toHexString(sb.toString().hashCode().toString().hashCode())
    }

    class CaptureResult(
        val raw: JSONObject?,
        val frame: Frame? = null,
        val error: String? = null,
    )

    // ---- node addressing (§3.6) ----------------------------------------------

    /** The raw JSON node at the emitted DFS index (1-based) of a generation. */
    fun installNodeTargets(generation: Int, targets: JSONArray) {
        val frame = frameFor(generation) ?: return
        for (i in 0 until targets.length()) {
            val target = targets.getJSONObject(i)
            val id = target.optInt("id", 0)
            val rawIndex = target.optInt("rawIndex", 0)
            if (id > 0 && rawIndex > 0) frame.emittedToRaw[id] = rawIndex
        }
    }

    fun rawNodeAt(generation: Int, idx: Int): JSONObject? {
        val f = when (generation) {
            currentFrame?.generation -> currentFrame
            prevFrame?.generation -> prevFrame
            else -> null
        } ?: return null
        val rawIndex = f.emittedToRaw[idx] ?: return null
        val root = f.raw.optJSONArray("nodes")?.optJSONObject(0) ?: return null
        val counter = intArrayOf(0)
        fun walkN(j: JSONObject): JSONObject? {
            counter[0]++
            if (counter[0] == rawIndex) return j
            val kids = j.optJSONArray("children") ?: return null
            for (i in 0 until kids.length()) {
                walkN(kids.getJSONObject(i))?.let { return it }
            }
            return null
        }
        return walkN(root)
    }

    fun currentGeneration(): Int = currentFrame?.generation ?: 0
    fun frameFor(generation: Int): Frame? =
        if (currentFrame?.generation == generation) currentFrame else prevFrame

    /**
     * The §3.6 pipeline for a node-addressed action. Returns either the live
     * [AccessibilityNodeInfo] plus its raw JSON, or a ui_* error code.
     */
    fun resolveTarget(treeId: String?, nodeId: String, requireClickable: Boolean? = null,
                      requireEditable: Boolean? = null, requireScrollable: Boolean? = null): ResolveResult {
        val liveGen = currentGeneration()
        if (liveGen == 0) return ResolveResult(null, null, "ui_a11y_disabled")
        val gen = treeId?.removePrefix("g")?.toIntOrNull()
        if (treeId != null && gen != liveGen) {
            return ResolveResult(null, null, "ui_stale_node",
                hint = "treeId $treeId is not the live generation g$liveGen — re-run ui_dump")
        }
        val idx = nodeId.removePrefix("#").substringAfter(':').toIntOrNull()
            ?: return ResolveResult(null, null, "ui_invalid_input", hint = "nodeId must be #12 | 12 | g7:12")
        val raw = rawNodeAt(liveGen, idx) ?: return ResolveResult(null, null, "ui_node_gone")
        // find the live node by fingerprint (strong: vid+label+bounds; weak: cls+label)
        val live = findLiveNode(raw)
            ?: return ResolveResult(null, null, "ui_stale_node",
                hint = "node no longer resolves in the live tree — re-run ui_dump")
        if (!live.isEnabled) return ResolveResult(null, null, "ui_not_actionable",
            hint = "node disabled; nearest actionable ancestor not searched")
        if (requireClickable == true && !live.isClickable) {
            val anc = nearestActionableAncestor(live)
            return ResolveResult(null, null, "ui_not_actionable",
                hint = "not clickable${anc?.let { a -> " — nearest actionable ancestor #${ancestorIndex(a)}" } ?: ""}")
        }
        if (requireEditable == true && !live.isEditable)
            return ResolveResult(null, null, "ui_not_actionable", hint = "node is not editable")
        if (requireScrollable == true && !live.isScrollable)
            return ResolveResult(null, null, "ui_not_actionable", hint = "node is not scrollable")
        return ResolveResult(live, raw, null)
    }

    class ResolveResult(val node: AccessibilityNodeInfo?, val raw: JSONObject?, val error: String?, val hint: String? = null)

    private fun findLiveNode(raw: JSONObject): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val wantVid = raw.optString("vid")
        val wantLabel = raw.optString("t").ifEmpty { raw.optString("cd") }
        val wantCls = raw.optString("cls")
        val b = raw.optJSONArray("b")
        val wantRect = if (b != null && b.length() == 4) Rect(b.getInt(0), b.getInt(1), b.getInt(2), b.getInt(3)) else null
        var weak: AccessibilityNodeInfo? = null
        fun walkN(n: AccessibilityNodeInfo) {
            val nb = Rect().also { n.getBoundsInScreen(it) }
            val label = n.text?.toString() ?: n.contentDescription?.toString() ?: ""
            val intersects = wantRect != null && Rect(nb).intersect(wantRect)
            if (wantVid.isNotEmpty() && n.viewIdResourceName == wantVid && label == wantLabel && intersects) {
                throw Found(n)
            }
            if (weak == null && n.className?.toString() == wantCls && label == wantLabel && intersects) weak = n
            for (i in 0 until n.childCount) {
                val c = n.getChild(i) ?: continue
                walkN(c)
            }
        }
        try { walkN(root) } catch (e: Found) { return e.node }
        return weak
    }
    private class Found(val node: AccessibilityNodeInfo) : Exception()

    private fun nearestActionableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var p = node.parent
        var hops = 0
        while (p != null && hops < 10) {
            if (p.isClickable || p.isLongClickable) return p
            p = p.parent
            hops++
        }
        return null
    }
    private fun ancestorIndex(node: AccessibilityNodeInfo): Int {
        // best-effort: index by vid or label in the current frame (audit only)
        return node.viewIdResourceName?.hashCode()?.rem(10000)?.coerceAtLeast(1) ?: 0
    }

    // ---- action primitives (§4.1) + gesture fallback (§4.3) -------------------

    fun performNodeAction(node: AccessibilityNodeInfo, action: Int, args: android.os.Bundle? = null): Boolean =
        try { node.performAction(action, args) } catch (t: Throwable) { false }

    fun globalBack(): Boolean = try { performGlobalAction(GLOBAL_ACTION_BACK) } catch (t: Throwable) { false }

    /** Dispatches a tap/long-press gesture at the bounds centre (§4.3). */
    fun gestureAt(bounds: Rect, longPress: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val cx = (bounds.left + bounds.right) / 2
        val cy = (bounds.top + bounds.bottom) / 2
        val path = Path().apply { moveTo(cx.toFloat(), cy.toFloat()) }
        val duration = if (longPress) 500L else 100L
        val stroke = GestureDescription.StrokeDescription(path, 0L, duration)
        val gd = GestureDescription.Builder().addStroke(stroke).build()
        return try { dispatchGesture(gd, null, null) } catch (t: Throwable) { false }
    }

    /** Swipe inside the container's own bounds (never full-screen, §4.3). */
    fun swipeInBounds(bounds: Rect, forward: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val cx = (bounds.left + bounds.right) / 2
        val top = bounds.top + (bounds.height() * 0.25f).toInt()
        val bottom = bounds.top + (bounds.height() * 0.75f).toInt()
        val (y1, y2) = if (forward) bottom to top else top to bottom
        val path = Path().apply {
            moveTo(cx.toFloat(), y1.toFloat()); lineTo(cx.toFloat(), y2.toFloat())
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 350L)
        val gd = GestureDescription.Builder().addStroke(stroke).build()
        return try { dispatchGesture(gd, null, null) } catch (t: Throwable) { false }
    }

    /** The event counts since a baseline — ui_wait's events_seen. */
    fun eventsSince(baseline: Map<String, Int>): Map<String, Int> {
        val now = eventCountsSnapshot()
        val out = HashMap<String, Int>()
        for ((k, v) in now) {
            val d = v - (baseline[k] ?: 0)
            if (d > 0) out[k] = d
        }
        return out
    }

    /** Runs [block] on the single traversal/action executor (§7.2). */
    fun <T> onExec(block: () -> T): T {
        val f = java.util.concurrent.FutureTask(block)
        exec.execute(f)
        return try { f.get() } catch (error: InterruptedException) {
            f.cancel(true)
            throw error
        }
    }

    data class ScreenCapture(val bytes: ByteArray, val width: Int, val height: Int,
        val screenWidth: Int, val screenHeight: Int, val rotation: Int)

    fun displayGeometry(): Triple<Int, Int, Int> {
        val display = getSystemService(android.hardware.display.DisplayManager::class.java)
            .getDisplay(android.view.Display.DEFAULT_DISPLAY) ?: error("ui_screenshot_invalid_display")
        val metrics = android.util.DisplayMetrics()
        @Suppress("DEPRECATION") display.getRealMetrics(metrics)
        return Triple(metrics.widthPixels, metrics.heightPixels, display.rotation)
    }
    fun imeVisible(): Boolean = windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }

    /** Separate callback executor: never wait for a callback on the traversal executor. */
    fun captureScreen(maxLongEdge: Int, windowId: Int, windowBounds: Rect): ScreenCapture {
        check(!imeVisible()) { "ui_screenshot_ime_visible" }
        if (Build.VERSION.SDK_INT < 34) error("ui_screenshot_unsupported")
        check(serviceInfo.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_TAKE_SCREENSHOT != 0) {
            "ui_screenshot_unsupported"
        }
        @Suppress("DEPRECATION")
        check(magnificationController.scale <= 1f) { "ui_screenshot_magnified" }
        val now = android.os.SystemClock.uptimeMillis()
        check(now - lastCaptureAt >= 350) { "ui_screenshot_rate_limited" }
        lastCaptureAt = now
        val geometry = displayGeometry()
        check(windowBounds.left >= 0 && windowBounds.top >= 0 && windowBounds.right <= geometry.first &&
            windowBounds.bottom <= geometry.second && !windowBounds.isEmpty) { "ui_screenshot_geometry_changed" }
        val latch = CountDownLatch(1)
        val result = java.util.concurrent.atomic.AtomicReference<ScreenCapture?>()
        val failure = java.util.concurrent.atomic.AtomicReference("ui_screenshot_timeout")
        val expired = java.util.concurrent.atomic.AtomicBoolean(false)
        takeScreenshotOfWindow(windowId, captureExec, object : TakeScreenshotCallback {
            override fun onSuccess(screenshot: ScreenshotResult) {
                val buffer = screenshot.hardwareBuffer
                var bitmap: Bitmap? = null
                try {
                    if (expired.get()) return
                    val hardware = Bitmap.wrapHardwareBuffer(buffer, screenshot.colorSpace)
                        ?: error("ui_screenshot_decode_failed")
                    bitmap = try { hardware.copy(Bitmap.Config.ARGB_8888, false) } finally { hardware.recycle() }
                    val windowImage = requireNotNull(bitmap)
                    check(windowImage.width == windowBounds.width() && windowImage.height == windowBounds.height()) { "ui_screenshot_geometry_changed" }
                    // Pad excluded system bars/overlays; preserve display-pixel coordinate mapping.
                    if (windowImage.width != geometry.first || windowImage.height != geometry.second) {
                        bitmap = Bitmap.createBitmap(geometry.first, geometry.second, Bitmap.Config.ARGB_8888)
                        android.graphics.Canvas(bitmap!!).drawBitmap(windowImage, windowBounds.left.toFloat(), windowBounds.top.toFloat(), null)
                        windowImage.recycle()
                    }
                    val source = requireNotNull(bitmap)
                    var nonBlack = false
                    val pixels = IntArray(source.width)
                    for (y in 0 until source.height) {
                        source.getPixels(pixels, 0, source.width, 0, y, source.width, 1)
                        if (pixels.any { it and 0x00ffffff != 0 }) { nonBlack = true; break }
                    }
                    check(nonBlack) { "ui_screenshot_secure_window" }
                    var edge = maxLongEdge.coerceIn(320, 1568)
                    var accepted: ScreenCapture? = null
                    while (edge >= 320 && accepted == null && !expired.get()) {
                        val factor = minOf(1.0, edge.toDouble() / maxOf(source.width, source.height))
                        val scaled = Bitmap.createScaledBitmap(source, maxOf(1, (source.width * factor).toInt()),
                            maxOf(1, (source.height * factor).toInt()), true)
                        try {
                            for (quality in listOf(80, 65, 50, 35)) {
                                val bytes = ByteArrayOutputStream().use { out ->
                                    check(scaled.compress(Bitmap.CompressFormat.JPEG, quality, out))
                                    out.toByteArray()
                                }
                                if (bytes.size <= 153600) {
                                    accepted = ScreenCapture(bytes, scaled.width, scaled.height,
                                        source.width, source.height, geometry.third)
                                    break
                                }
                            }
                        } finally { if (scaled !== source) scaled.recycle() }
                        edge = edge * 3 / 4
                    }
                    check(accepted != null) { "ui_screenshot_too_large" }
                    if (!expired.get()) result.set(accepted)
                } catch (error: Throwable) { failure.set(error.message ?: "ui_screenshot_decode_failed") }
                finally { bitmap?.recycle(); buffer.close(); latch.countDown() }
            }
            override fun onFailure(errorCode: Int) {
                failure.set(when (errorCode) {
                    ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT -> "ui_screenshot_rate_limited"
                    ERROR_TAKE_SCREENSHOT_INVALID_DISPLAY -> "ui_screenshot_invalid_display"
                    ERROR_TAKE_SCREENSHOT_NO_ACCESSIBILITY_ACCESS -> "ui_a11y_disabled"
                    ERROR_TAKE_SCREENSHOT_SECURE_WINDOW -> "ui_screenshot_secure_window"
                    ERROR_TAKE_SCREENSHOT_INVALID_WINDOW -> "ui_screenshot_invalid_window"
                    else -> "ui_screenshot_internal"
                })
                latch.countDown()
            }
        })
        try { check(latch.await(3, TimeUnit.SECONDS)) { "ui_screenshot_timeout" } }
        finally { expired.set(true) }
        return result.get() ?: error(failure.get())
    }

    /** Compatibility methods retained only for the diagnostics-only Kotlin surface. */
    fun observe(): JSONObject = onExec {
        val cap = captureRaw()
        cap.error?.let { error(it) }
        val raw = requireNotNull(cap.raw)
        JSONObject()
            .put("snapshotId", "g${raw.optInt("generation")}")
            .put("package", raw.optString("app"))
            .put("windows", JSONArray().put(raw.optJSONObject("window")))
            .put("truncated", raw.optBoolean("truncatedRaw"))
            .put("raw", raw)
    }

    fun click(snapshotId: String, nodeId: String): Boolean = onExec {
        val target = resolveTarget(snapshotId, nodeId, requireClickable = true)
        target.node?.let { performNodeAction(it, AccessibilityNodeInfo.ACTION_CLICK) } ?: false
    }

    fun inputText(snapshotId: String, nodeId: String, text: String): Boolean = onExec {
        val target = resolveTarget(snapshotId, nodeId, requireEditable = true)
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
        target.node?.let { performNodeAction(it, AccessibilityNodeInfo.ACTION_SET_TEXT, args) } ?: false
    }

    fun gesture(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long): Boolean {
        val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs.coerceIn(1, 10_000)))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    fun screenshotPng(): ByteArray {
        val latch = CountDownLatch(1)
        var bytes = ByteArray(0)
        takeScreenshot(android.view.Display.DEFAULT_DISPLAY, mainExecutor,
            object : TakeScreenshotCallback {
                override fun onSuccess(result: ScreenshotResult) {
                    val buffer = result.hardwareBuffer
                    try {
                        val bitmap = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace ?: ColorSpace.get(ColorSpace.Named.SRGB))
                        if (bitmap != null) ByteArrayOutputStream().use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            bytes = out.toByteArray()
                        }
                    } finally {
                        buffer.close()
                        latch.countDown()
                    }
                }
                override fun onFailure(errorCode: Int) { latch.countDown() }
            })
        latch.await(5, TimeUnit.SECONDS)
        return bytes
    }
}

