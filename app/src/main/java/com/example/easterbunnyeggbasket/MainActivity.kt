package com.example.easterbunnyeggbasket

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    // --- игра: сбор яиц ---
    private lateinit var timerText: TextView
    private lateinit var countText: TextView
    private lateinit var topScoresText: TextView
    private lateinit var tapButton: ImageButton
    private lateinit var resetButton: Button
    private lateinit var resetHighScoresButton: Button

    private var tapCount = 0          // количество собранных яиц
    private var isRunning = false
    private lateinit var timer: CountDownTimer
    private val topScores = mutableListOf<Int>()

    private lateinit var tapSound: MediaPlayer
    private lateinit var gameOverSound: MediaPlayer

    private val PREFS_NAME = "HighScores"
    private val SCORES_KEY = "TopScores"

    private var screenWidth = 0
    private var screenHeight = 0

    private var originalX = 0f
    private var originalY = 0f

    // разные яйца
    private val eggDrawables = listOf(
        R.drawable.egg_button,   // старое яйцо
//        R.drawable.egg_blue,
        R.drawable.egg_pink,
        R.drawable.egg_green,
//        R.drawable.egg_golden    // особое, если захочешь
    )

    private var lastEggIndex = -1


    // --- блок пасхального зайчика / настроения ---
    private lateinit var imgBunny: ImageView
    private lateinit var imgBasket: ImageView
    private lateinit var greetingTextView: TextView
    private lateinit var anotherTextView: TextView
    private lateinit var oneMoreTextView: TextView
    private lateinit var changeButton: Button
    private lateinit var changeBackButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // системные отступы
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // размер экрана
        val mainLayout = findViewById<View>(R.id.main)
        mainLayout.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    screenWidth = mainLayout.width
                    screenHeight = mainLayout.height
                    mainLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        )

        // --- инициализация виджетов игры ---
        timerText = findViewById(R.id.timerText)
        countText = findViewById(R.id.countText)
        topScoresText = findViewById(R.id.topScoresText)
        tapButton = findViewById(R.id.tapButton)
        resetButton = findViewById(R.id.resetButton)
        resetHighScoresButton = findViewById(R.id.resetHighScoresButton)

        tapButton.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    originalX = tapButton.x
                    originalY = tapButton.y
                    tapButton.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        )

        tapSound = MediaPlayer.create(this, R.raw.tap_sound)
        gameOverSound = MediaPlayer.create(this, R.raw.game_over)

        loadTopScores()

        val totalTime = 20 * 1000L

        timer = object : CountDownTimer(totalTime, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                timerText.text = getString(R.string.time_left, secondsLeft)
            }

            override fun onFinish() {
                timerText.text = getString(R.string.times_up)
                tapButton.isEnabled = false
                isRunning = false
                gameOverSound.start()
                updateTopScores()
                updateMoodAndColors()

                // показываем корзину и анимацию зайчика
                showBasketAndBunnyWithAnimation()

                if (tapCount >= 30) {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.golden_egg_unlocked),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        tapButton.setOnClickListener {
            if (!isRunning) {
                isRunning = true
                timer.start()
            }

            tapCount++
            tapSound.start()
            countText.text = getString(R.string.eggs_collected, tapCount)

            // новое случайное яйцо
            setRandomEggImage()

            // и убегает в новое место
            moveButtonRandomly()
        }


        resetButton.setOnClickListener {
            if (isRunning) {
                timer.cancel()
            }
            tapCount = 0
            countText.text = getString(R.string.eggs_collected, tapCount)
            timerText.text = getString(R.string.time_left_20)
            tapButton.isEnabled = true
            isRunning = false

            tapButton.x = originalX
            tapButton.y = originalY

            lastEggIndex = -1
            tapButton.setImageResource(R.drawable.egg_button)

            resetMoodTexts()
            setEndScreenVisible(false)
        }


        resetHighScoresButton.setOnClickListener {
            clearHighScores()
            topScoresText.text = getString(R.string.top_5_scores)
            Toast.makeText(
                this,
                getString(R.string.high_scores_cleared),
                Toast.LENGTH_SHORT
            ).show()
        }

        // --- инициализация пасхального блока ---
        imgBunny = findViewById(R.id.imgBunny)
        imgBasket = findViewById(R.id.imgBasket)
        greetingTextView = findViewById(R.id.txtGreeting)
        anotherTextView = findViewById(R.id.txtAnother)
        oneMoreTextView = findViewById(R.id.txtOneMore)
        changeButton = findViewById(R.id.btnChange)
        changeBackButton = findViewById(R.id.btnChangeBack)

        changeButton.setOnClickListener {
            changeTextViewsManual()
        }

        changeBackButton.setOnClickListener {
            resetMoodTexts()
        }

        // стартовое состояние
        displayTopScores()
        resetMoodTexts()
        countText.text = getString(R.string.eggs_collected, tapCount)
        setEndScreenVisible(false)   // в начале всё спрятано
    }

    // --- анимация: корзинка + зайчик ---

    // --- анимация: корзинка + зайчик ---
    private fun showBasketAndBunnyWithAnimation() {
        // сначала показываем корзину с мягким зумом
        imgBasket.apply {
            visibility = View.VISIBLE
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f

            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .withEndAction {
                    // теперь “вылезает” зайчик из-за корзины
                    imgBunny.visibility = View.VISIBLE
                    imgBunny.alpha = 0f

                    // не трогаем y, только лёгкий подъём через translationY
                    imgBunny.translationY = 80f   // старт ниже
                    imgBunny.animate()
                        .alpha(1f)
                        .translationY(0f)        // вернуться в нормальную позицию
                        .setDuration(600)
                        .withEndAction {
                            showBunnyTextsAndButtons()
                        }
                        .start()
                }
                .start()
        }

    }

    private fun showBunnyTextsAndButtons() {
        val views = listOf(
            greetingTextView,
            anotherTextView,
            oneMoreTextView,
            changeButton,
            changeBackButton
        )

        views.forEach { v ->
            v.visibility = View.VISIBLE
            v.alpha = 0f
            v.animate()
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(150)
                .start()
        }
    }

    // --- экран концовки (для сброса) ---

    private fun setEndScreenVisible(isVisible: Boolean) {
        val visibility = if (isVisible) View.VISIBLE else View.GONE

        val views = listOf(
            imgBunny,
            imgBasket,
            greetingTextView,
            anotherTextView,
            oneMoreTextView,
            changeButton,
            changeBackButton
        )

        views.forEach { v ->
            v.clearAnimation()
            v.visibility = visibility
            v.alpha = 1f
            v.translationY = 0f
        }
    }

    // --- рекорды ---

    private fun updateTopScores() {
        topScores.add(tapCount)
        topScores.sortDescending()

        if (topScores.size > 5) {
            topScores.removeAt(topScores.lastIndex)
        }

        saveTopScores()
        displayTopScores()
    }

    private fun displayTopScores() {
        val scoreText = StringBuilder(getString(R.string.top_5_scores) + "\n")
        topScores.forEachIndexed { index, score ->
            scoreText.append("${index + 1}: $score\n")
        }
        topScoresText.text = scoreText.toString()
    }

    private fun saveTopScores() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString(SCORES_KEY, topScores.joinToString(","))
        editor.apply()
    }

    private fun loadTopScores() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedScores = sharedPref.getString(SCORES_KEY, "")

        if (!savedScores.isNullOrEmpty()) {
            topScores.clear()
            topScores.addAll(savedScores.split(",").map { it.toInt() })
        }
    }

    private fun clearHighScores() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.remove(SCORES_KEY)
        editor.apply()

        topScores.clear()
    }

    // --- движение яйца по экрану ---
    // Случайно выбираем яйцо, стараясь не повторять предыдущее
    private fun setRandomEggImage() {
        if (eggDrawables.isEmpty()) return

        var index = Random.nextInt(eggDrawables.size)
        if (eggDrawables.size > 1) {
            // чтобы не было два одинаковых подряд
            while (index == lastEggIndex) {
                index = Random.nextInt(eggDrawables.size)
            }
        }

        lastEggIndex = index
        val drawableId = eggDrawables[index]
        tapButton.setImageResource(drawableId)

        // пример: если выпало золотое яйцо — можно сделать бонус

    }

    private fun moveButtonRandomly() {
        if (screenWidth == 0 || screenHeight == 0) return

        val buttonWidth = tapButton.width
        val buttonHeight = tapButton.height

        val maxX = (screenWidth - buttonWidth).coerceAtLeast(0)
        val maxY = (screenHeight - buttonHeight).coerceAtLeast(0)

        val randomX = Random.nextInt(0, maxX)
        val randomY = Random.nextInt(150, maxY)

        tapButton.x = randomX.toFloat()
        tapButton.y = randomY.toFloat()
    }

    // --- пасхальное настроение в зависимости от результата ---

    private fun updateMoodAndColors() {
        val moodTitle: String
        val line2: String
        val line3: String

        val colorMain: Int
        val colorSecondary: Int

        when {
            tapCount <= 15 -> {
                moodTitle = getString(R.string.mood_calm_title)
                line2 = getString(R.string.mood_calm_line2)
                line3 = getString(R.string.mood_calm_line3)

                colorMain = ContextCompat.getColor(this, R.color.sandy_brown)
                colorSecondary = ContextCompat.getColor(this, R.color.blue)
            }
            tapCount <= 35 -> {
                moodTitle = getString(R.string.mood_balanced_title)
                line2 = getString(R.string.mood_balanced_line2)
                line3 = getString(R.string.mood_balanced_line3)

                colorMain = ContextCompat.getColor(this, R.color.orange)
                colorSecondary = ContextCompat.getColor(this, R.color.yellow)
            }
            else -> {
                moodTitle = getString(R.string.mood_super_title)
                line2 = getString(R.string.mood_super_line2)
                line3 = getString(R.string.mood_super_line3)

                colorMain = ContextCompat.getColor(this, R.color.green)
                colorSecondary = ContextCompat.getColor(this, R.color.blue)
            }
        }

        greetingTextView.text = moodTitle
        anotherTextView.text = line2
        oneMoreTextView.text = line3

        greetingTextView.setTextColor(colorMain)
        anotherTextView.setTextColor(colorSecondary)
        oneMoreTextView.setTextColor(colorSecondary)
    }

    // --- ручная смена темы ---

    private fun changeTextViewsManual() {
        val purple = ContextCompat.getColor(this, R.color.purple_500)
        val lightYellow = ContextCompat.getColor(this, R.color.yellow)

        greetingTextView.setTextColor(purple)
        anotherTextView.setTextColor(lightYellow)
        oneMoreTextView.setTextColor(lightYellow)
    }

    private fun resetMoodTexts() {
        greetingTextView.text = getString(R.string.bunny_greeting)
        anotherTextView.text = getString(R.string.bunny_another)
        oneMoreTextView.text = getString(R.string.bunny_one_more)

        greetingTextView.setTextColor(ContextCompat.getColor(this, R.color.sandy_brown))
        anotherTextView.setTextColor(ContextCompat.getColor(this, R.color.blue))
        oneMoreTextView.setTextColor(ContextCompat.getColor(this, R.color.blue))
    }

    override fun onDestroy() {
        super.onDestroy()
        tapSound.release()
        gameOverSound.release()
    }
}
