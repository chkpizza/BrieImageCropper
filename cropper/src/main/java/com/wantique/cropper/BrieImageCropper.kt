package com.wantique.cropper

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class BrieImageCropper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    private var minimumCropperSize: Int = 0
    private var mViewWidth = 0
    private var mViewHeight = 0
    private var mExifRotation = 0
    private var mIsInitialized = false
    private var mLastXAxis = 0f
    private var mLastYAxis = 0f
    private var lastTouchArea = INVALID_AREA

    private lateinit var mImageRect: RectF
    private lateinit var mCropperRect: RectF
    private lateinit var imageUri: Uri

    private val mMatrix = Matrix()
    private val mPaintTranslucent = Paint()
    private val mPaintFrame = Paint()
    private val mPaintGuideLine = Paint()
    private val mPaintHandle = Paint()

    private val handleSize = 8 * getDeviceDensity(context)
    private val axisThreshold = 24

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BrieImageCropper)
        minimumCropperSize = typedArray.getDimensionPixelSize(R.styleable.BrieImageCropper_minimum_cropper_size, (100 * getDeviceDensity(context)).toInt())
        typedArray.recycle()

        scaleType = ScaleType.MATRIX
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        val viewHeight = MeasureSpec.getSize(heightMeasureSpec)

        this.mViewWidth = viewWidth
        this.mViewHeight = viewHeight

        setMeasuredDimension(viewWidth, viewHeight)
    }

    fun load(imageUri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            this@BrieImageCropper.imageUri = imageUri

            /*
            * Exif Rotation 90 CW 이미지를 ImageDecoder.decodeBitmap() 메서드를 호출하면 정상적인 각도로 이미지가 제대로 보입니다.
            * 동일한 이미지를 MediaStore.getBitmap() 메서드를 호출하면 Rotation 90 이 적용된 Image 가 로드됩니다.
            * Exif Rotation 90 CW 는 Image 상단을 기준으로 왼쪽으로 90도 회전합니다.
            * */
            val bitmap = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, imageUri))
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            }

            withContext(Dispatchers.Main) {
                setBitmapDrawable(BitmapDrawable(resources, bitmap))
            }
        }
    }

    private fun setBitmapDrawable(drawable: Drawable) {
        super.setImageDrawable(drawable)
        prepare()
    }

    private fun prepare() {
        if (drawable == null || drawable.intrinsicWidth == 0 || drawable.intrinsicHeight == 0) {
            return
        }

        mExifRotation = getExifOrientation(context, imageUri).also {
            if(it == -1) {
                return
            }
        }

        val scaleX = mViewWidth.toFloat() / drawable.intrinsicWidth.toFloat()
        val scaleY = mViewHeight.toFloat() / drawable.intrinsicHeight.toFloat()
        val scale = scaleX.coerceAtMost(scaleY)
        mMatrix.setScale(scale, scale)

        val translateX = ((mViewWidth.toFloat()) - scale * drawable.intrinsicWidth.toFloat()) / 2
        val translateY = ((mViewHeight.toFloat()) - scale * drawable.intrinsicHeight.toFloat()) / 2
        mMatrix.postTranslate(translateX, translateY)

        imageMatrix = mMatrix

        mImageRect = RectF().also {
            mMatrix.mapRect(it, RectF(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat()))
        }
        mCropperRect = RectF(mImageRect.left, mImageRect.top, mImageRect.right, mImageRect.bottom)

        mIsInitialized = true
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if(mIsInitialized) {
            drawable?.let {
                drawCropperElements(canvas)
            }
        }
    }

    private fun drawCropperElements(canvas: Canvas) {
        drawOverlay(canvas)
        drawCropper(canvas)
        drawCropperGuideLine(canvas)
    }


    private fun drawOverlay(canvas: Canvas) {
        mPaintTranslucent.isAntiAlias = true
        mPaintTranslucent.isFilterBitmap = true
        mPaintTranslucent.color = resources.getColor(R.color.translucent_black, null)
        mPaintTranslucent.style = Paint.Style.FILL

        val path = Path()
        val overlayRect = RectF(mImageRect.left, mImageRect.top, mImageRect.right, mImageRect.bottom)

        path.addRect(overlayRect, Path.Direction.CW)
        path.addRect(mCropperRect, Path.Direction.CCW)
        canvas.drawPath(path, mPaintTranslucent)
    }

    private fun drawCropper(canvas: Canvas) {
        mPaintFrame.isAntiAlias = true
        mPaintFrame.isFilterBitmap = true
        mPaintFrame.style = Paint.Style.STROKE
        mPaintFrame.color = resources.getColor(R.color.white, null)
        mPaintFrame.strokeWidth = 1 * getDeviceDensity(context)

        canvas.drawRect(mCropperRect, mPaintFrame)
    }

    private fun drawCropperGuideLine(canvas: Canvas) {
        mPaintGuideLine.isAntiAlias = true
        mPaintGuideLine.isFilterBitmap = true
        mPaintGuideLine.style = Paint.Style.STROKE
        mPaintGuideLine.color = resources.getColor(R.color.white, null)
        mPaintGuideLine.strokeWidth = 1 * getDeviceDensity(context)

        val guideX1 = mCropperRect.left + (mCropperRect.right - mCropperRect.left) / 3.0f
        val guideX2 = mCropperRect.right - (mCropperRect.right - mCropperRect.left) / 3.0f
        val guideY1 = mCropperRect.top + (mCropperRect.bottom - mCropperRect.top) / 3.0f
        val guideY2 = mCropperRect.bottom - (mCropperRect.bottom - mCropperRect.top) / 3.0f

        canvas.drawLine(guideX1, mCropperRect.top, guideX1, mCropperRect.bottom, mPaintGuideLine)
        canvas.drawLine(guideX2, mCropperRect.top, guideX2, mCropperRect.bottom, mPaintGuideLine)
        canvas.drawLine(mCropperRect.left, guideY1, mCropperRect.right, guideY1, mPaintGuideLine)
        canvas.drawLine(mCropperRect.left, guideY2, mCropperRect.right, guideY2, mPaintGuideLine)
    }

    private fun drawHandles(canvas: Canvas) {
        mPaintHandle.isAntiAlias = true
        mPaintHandle.isFilterBitmap = true
        mPaintHandle.style = Paint.Style.FILL
        mPaintHandle.color = resources.getColor(R.color.red, null)

        canvas.drawCircle(mCropperRect.left, mCropperRect.top, handleSize, mPaintHandle)
        canvas.drawCircle(mCropperRect.right, mCropperRect.top, handleSize, mPaintHandle)
        canvas.drawCircle(mCropperRect.left, mCropperRect.bottom, handleSize, mPaintHandle)
        canvas.drawCircle(mCropperRect.right, mCropperRect.bottom, handleSize, mPaintHandle)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(!mIsInitialized) {
            return false
        }

        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                handleDownEvent(event)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if(lastTouchArea != INVALID_AREA) {
                    parent.requestDisallowInterceptTouchEvent(true)
                    handleMoveEvent(event)
                } else {
                    parent.requestDisallowInterceptTouchEvent(false)
                }
                return true
            }

            /*
            MotionEvent.ACTION_UP -> {

            }

             */
        }

        return false
    }

    private fun handleDownEvent(event: MotionEvent) {
        mLastXAxis = event.x
        mLastYAxis = event.y
        retrieveTouchArea(event.x, event.y)
    }

    private fun retrieveTouchArea(x: Float, y: Float) {
        lastTouchArea = when {
            isValidLeftTopTouch(x, y) -> LEFT_TOP
            isValidRightTopTouch(x, y) -> RIGHT_TOP
            isValidLeftBottomTouch(x, y) -> LEFT_BOTTOM
            isValidRightBottomTouch(x, y) -> RIGHT_BOTTOM
            isValidInsideTouch(x, y) -> INSIDE
            else -> INVALID_AREA
        }
    }

    private fun isValidLeftTopTouch(x: Float, y: Float): Boolean {
        val xAxisMaxThreshold = mCropperRect.left + (axisThreshold * getDeviceDensity(context))
        val xAxisMinThreshold = mCropperRect.left - (axisThreshold * getDeviceDensity(context))
        val yAxisMaxThreshold = mCropperRect.top + (axisThreshold * getDeviceDensity(context))
        val yAxisMinThreshold = mCropperRect.top - (axisThreshold * getDeviceDensity(context))

        return x in xAxisMinThreshold..xAxisMaxThreshold &&
                y in yAxisMinThreshold .. yAxisMaxThreshold
    }

    private fun isValidRightTopTouch(x: Float, y: Float): Boolean {
        val xAxisMaxThreshold = mCropperRect.right + (axisThreshold * getDeviceDensity(context))
        val xAxisMinThreshold = mCropperRect.right - (axisThreshold * getDeviceDensity(context))
        val yAxisMaxThreshold = mCropperRect.top + (axisThreshold * getDeviceDensity(context))
        val yAxisMinThreshold = mCropperRect.top - (axisThreshold * getDeviceDensity(context))

        return x in xAxisMinThreshold..xAxisMaxThreshold &&
                y in yAxisMinThreshold .. yAxisMaxThreshold
    }

    private fun isValidLeftBottomTouch(x: Float, y: Float): Boolean {
        val xAxisMaxThreshold = mCropperRect.left + (axisThreshold * getDeviceDensity(context))
        val xAxisMinThreshold = mCropperRect.left - (axisThreshold * getDeviceDensity(context))
        val yAxisMaxThreshold = mCropperRect.bottom + (axisThreshold * getDeviceDensity(context))
        val yAxisMinThreshold = mCropperRect.bottom - (axisThreshold * getDeviceDensity(context))

        return x in xAxisMinThreshold..xAxisMaxThreshold &&
                y in yAxisMinThreshold .. yAxisMaxThreshold
    }

    private fun isValidRightBottomTouch(x: Float, y: Float): Boolean {
        val xAxisMaxThreshold = mCropperRect.right + (axisThreshold * getDeviceDensity(context))
        val xAxisMinThreshold = mCropperRect.right - (axisThreshold * getDeviceDensity(context))
        val yAxisMaxThreshold = mCropperRect.bottom + (axisThreshold * getDeviceDensity(context))
        val yAxisMinThreshold = mCropperRect.bottom - (axisThreshold * getDeviceDensity(context))

        return x in xAxisMinThreshold..xAxisMaxThreshold &&
                y in yAxisMinThreshold .. yAxisMaxThreshold
    }

    private fun isValidInsideTouch(x: Float, y: Float): Boolean {
        return mCropperRect.left <= x && mCropperRect.right >= x && mCropperRect.top <= y && mCropperRect.bottom >= y
    }

    private fun handleMoveEvent(event: MotionEvent) {
        val deltaX = event.x - mLastXAxis
        val deltaY = event.y - mLastYAxis

        when(lastTouchArea) {
            INSIDE -> translateCropper(deltaX, deltaY)
            LEFT_TOP -> transformCropperByLeftTop(deltaX, deltaY)
            RIGHT_TOP -> transformCropperByRightTop(deltaX, deltaY)
            LEFT_BOTTOM -> transformCropperByLeftBottom(deltaX, deltaY)
            RIGHT_BOTTOM -> transformCropperByRightBottom(deltaX, deltaY)
        }

        mLastXAxis = event.x
        mLastYAxis = event.y

        invalidate()
    }

    /*
    * 이전 MotionEvent X,Y 축과의 Delta 값을 Cropper Rectangle 의 left, right, top, bottom 에 더해서  Cropper Rectangle 을 전체적으로 이동시킵니다.
    * */
    private fun translateCropper(deltaX: Float, deltaY: Float) {
        mCropperRect.left += deltaX
        mCropperRect.right += deltaX
        mCropperRect.top += deltaY
        mCropperRect.bottom += deltaY
        coordinateCropperPosition()
    }

    private fun transformCropperByLeftTop(deltaX: Float, deltaY: Float) {
        mCropperRect.left += deltaX
        mCropperRect.top += deltaY
        if (mCropperRect.right - mCropperRect.left < minimumCropperSize) {
            val correctionX = (mCropperRect.right - mCropperRect.left) - minimumCropperSize
            mCropperRect.left += correctionX
        }
        if (mCropperRect.bottom - mCropperRect.top < minimumCropperSize) {
            val correctionY = (mCropperRect.bottom - mCropperRect.top) - minimumCropperSize
            mCropperRect.top += correctionY
        }
        coordinateCropperSize()
    }

    private fun transformCropperByRightTop(deltaX: Float, deltaY: Float) {
        mCropperRect.right += deltaX
        mCropperRect.top += deltaY
        if (mCropperRect.right - mCropperRect.left < minimumCropperSize) {
            val correctionX = minimumCropperSize - (mCropperRect.right - mCropperRect.left)
            mCropperRect.right -= -correctionX
        }
        if (mCropperRect.bottom - mCropperRect.top < minimumCropperSize) {
            val correctionY = (mCropperRect.bottom - mCropperRect.top) - minimumCropperSize
            mCropperRect.top += correctionY
        }
        coordinateCropperSize()
    }

    private fun transformCropperByLeftBottom(deltaX: Float, deltaY: Float) {
        mCropperRect.left += deltaX
        mCropperRect.bottom += deltaY
        if (mCropperRect.right - mCropperRect.left < minimumCropperSize) {
            val correctionX = (mCropperRect.right - mCropperRect.left) - minimumCropperSize
            mCropperRect.left += correctionX
        }
        if (mCropperRect.bottom - mCropperRect.top < minimumCropperSize) {
            val correctionY = (mCropperRect.bottom - mCropperRect.top) - minimumCropperSize
            mCropperRect.bottom -= correctionY
        }
        coordinateCropperSize()
    }

    private fun transformCropperByRightBottom(deltaX: Float, deltaY: Float) {
        mCropperRect.right += deltaX
        mCropperRect.bottom += deltaY
        if (mCropperRect.right - mCropperRect.left < minimumCropperSize) {
            val correctionX = (mCropperRect.right - mCropperRect.left) - minimumCropperSize
            mCropperRect.right -= correctionX
        }
        if (mCropperRect.bottom - mCropperRect.top < minimumCropperSize) {
            val correctionY = (mCropperRect.bottom - mCropperRect.top) - minimumCropperSize
            mCropperRect.bottom -= correctionY
        }
        coordinateCropperSize()
    }

    /*
    * Cropper Rectangle 의 각 좌표와 Image Rectangle 의 각 좌표의 차이를 구합니다.
    * 각 좌표의 차이가 0보다 작거나 클 경우 Cropper Rectangle 이 Image Rectangle 의 크기보다 커진 것으로, 각 좌표에 각 좌표 차이의 반대 부호값을 더해 Cropper Rectangle 이 Image Cropper 보다 커지지 못하게 합니다.
    * */
    private fun coordinateCropperSize() {
        val diffLeft = mCropperRect.left - mImageRect.left
        val diffRight = mCropperRect.right - mImageRect.right
        val diffTop = mCropperRect.top - mImageRect.top
        val diffBottom = mCropperRect.bottom - mImageRect.bottom

        if (diffLeft < 0) {
            mCropperRect.left += -diffLeft
        }
        if (diffRight > 0) {
            mCropperRect.right += -diffRight
        }
        if (diffTop < 0) {
            mCropperRect.top += -diffTop
        }
        if (diffBottom > 0) {
            mCropperRect.bottom += -diffBottom
        }
    }

    private fun coordinateCropperPosition() {
        val diffLeft = mCropperRect.left - mImageRect.left
        val diffRight = mCropperRect.right - mImageRect.right
        val diffTop = mCropperRect.top - mImageRect.top
        val diffBottom = mCropperRect.bottom - mImageRect.bottom

        if(diffLeft < 0) {
            mCropperRect.left += -diffLeft
            mCropperRect.right += -diffLeft
        }

        if(diffRight > 0) {
            mCropperRect.left += -diffRight
            mCropperRect.right += -diffRight
        }

        if(diffTop < 0) {
            mCropperRect.top += -diffTop
            mCropperRect.bottom += -diffTop
        }

        if(diffBottom > 0) {
            mCropperRect.top += -diffBottom
            mCropperRect.bottom += -diffBottom
        }
    }

    fun crop(): Bitmap? {
        drawable?.let {
            val drawableBitmap = (it as BitmapDrawable).bitmap
            val bitmapWidth = drawableBitmap.width
            val bitmapHeight = drawableBitmap.height

            val rollbackScale = bitmapWidth / mImageRect.width()
            val left = (bitmapWidth / (mImageRect.width() / mCropperRect.left) - mImageRect.left * rollbackScale).roundToInt()
            val right = (bitmapWidth / (mImageRect.width() / mCropperRect.right) - mImageRect.left * rollbackScale).roundToInt()
            val top = (bitmapHeight / (mImageRect.height() / mCropperRect.top) - mImageRect.top * rollbackScale).roundToInt()
            val bottom = (bitmapHeight / (mImageRect.height() / mCropperRect.bottom) - mImageRect.top * rollbackScale).roundToInt()
            val cropper = Rect(left, top, right, bottom)

            return Bitmap.createBitmap(drawableBitmap, cropper.left, cropper.top, cropper.width(), cropper.height(), null, true)
        }
        return null

        /*
        var bitmap: Bitmap? = null
        context.contentResolver.openInputStream(imageUri)?.let {
            val decoder = BitmapRegionDecoder.newInstance(it, false)
            if(decoder != null) {
                val bitmapWidth = decoder.width
                val bitmapHeight = decoder.height

                val rollbackScale = bitmapWidth / mImageRect.width()
                val left = (bitmapWidth / (mImageRect.width() / mFrameRect.left) - mImageRect.left * rollbackScale).roundToInt()
                val right = (bitmapWidth / (mImageRect.width() / mFrameRect.right) - mImageRect.left * rollbackScale).roundToInt()
                val top = (bitmapHeight / (mImageRect.height() / mFrameRect.top) - mImageRect.top * rollbackScale).roundToInt()
                val bottom = (bitmapHeight / (mImageRect.height() / mFrameRect.bottom) - mImageRect.top * rollbackScale).roundToInt()
                val cropper = Rect(left, top, right, bottom)

                bitmap = decoder.decodeRegion(cropper, BitmapFactory.Options())
            }

            it.close()
        }

        return bitmap

         */

    }

    fun save(bitmap: Bitmap): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        context.contentResolver.also { contentResolver ->
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let { saveUri ->
                val outputStream = contentResolver.openOutputStream(saveUri)
                if(outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    contentResolver.update(saveUri, contentValues, null, null)

                    outputStream.close()

                    return saveUri
                }
            }
        }

        return null
    }

    companion object {
        const val INSIDE = 0
        const val LEFT_TOP = 1
        const val RIGHT_TOP = 2
        const val LEFT_BOTTOM = 3
        const val RIGHT_BOTTOM = 4
        const val INVALID_AREA = -1
    }
}