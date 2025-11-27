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

    // --- UI для игры ---
    private lateinit var timerText: TextView
    private lateinit var countText: TextView
    private lateinit var bonusEggText: TextView
    private lateinit var infoText: TextView

    private lateinit var tapButton: ImageButton
    private lateinit var resetButton: Button
    private lateinit var resetHighScoresButton: Button
    private lateinit var bottomButtons: View

    // --- Итоговая карточка ---
    private lateinit var resultCard: View
    private lateinit var imgBasket: ImageView
    private lateinit var totalEggsText: TextView
    private lateinit var rareEggsText: TextView
    private lateinit var topScoresTitle: TextView
    private lateinit var topScoresText: TextView

    // поддерживающие фразы
    private lateinit var greetingTextView: TextView
    private lateinit var anotherTextView: TextView
    private lateinit var oneMoreTextView: TextView

    // --- логика игры ---
    private var tapCount = 0              // всего яиц за игру
    private var bonusEggCount = 0         // редкие яйца за игру
    private var isRunning = false
    private lateinit var timer: CountDownTimer
    private val topScores = mutableListOf<Int>()

    // звуки
    private lateinit var tapSound: MediaPlayer
    private lateinit var gameOverSound: MediaPlayer

    private val PREFS_NAME = "HighScores"
    private val SCORES_KEY = "TopScores"

    // размеры экрана для движения яйца
    private var screenWidth = 0
    private var screenHeight = 0

    private var originalX = 0f
    private var originalY = 0f

    // --- разные яйца ---
    private val EGG_DEFAULT = R.drawable.egg_button
    private val EGG_PINK = R.drawable.egg_pink
    private val EGG_GREEN = R.drawable.egg_green
    private val EGG_RARE = R.drawable.egg_golden   // редкое золотое

    private val normalEggs = listOf(
        EGG_DEFAULT,
        EGG_PINK,
        EGG_GREEN
    )

    private var lastEggResId: Int = EGG_DEFAULT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // системные отступы: убираем верхний, оставляем боковые и нижний
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
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

        // --- init views ---

        timerText = findViewById(R.id.timerText)
        countText = findViewById(R.id.countText)
        bonusEggText = findViewById(R.id.bonusEggText)
        infoText = findViewById(R.id.textViewInfo)

        tapButton = findViewById(R.id.tapButton)
        resetButton = findViewById(R.id.resetButton)
        resetHighScoresButton = findViewById(R.id.resetHighScoresButton)
        bottomButtons = findViewById(R.id.bottomButtons)

        // итоговая карточка
        resultCard = findViewById(R.id.resultCard)
        imgBasket = findViewById(R.id.imgBasket)
        totalEggsText = findViewById(R.id.totalEggsText)
        rareEggsText = findViewById(R.id.rareEggsText)
        topScoresTitle = findViewById(R.id.topScoresTitle)
        topScoresText = findViewById(R.id.topScoresText)

        greetingTextView = findViewById(R.id.txtGreeting)
        anotherTextView = findViewById(R.id.txtAnother)
        oneMoreTextView = findViewById(R.id.txtOneMore)

        // запоминаем стартовую позицию яйца
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

        val totalTime = 10 * 1000L   // 10 секунд

        timer = object : CountDownTimer(totalTime, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                timerText.text = getString(R.string.time_left, secondsLeft)
            }

            override fun onFinish() {
                timerText.text = getString(R.string.times_up)
                tapButton.isEnabled = false
                tapButton.visibility = View.GONE     // убираем яйцо после игры
                isRunning = false
                gameOverSound.start()

                updateTopScores()
                updateResultTexts()                  // total / rare / support text

                // показываем кнопки и карточку с анимацией
                showGameUIAfterFinish()
            }
        }

        tapButton.setOnClickListener {
            if (!isRunning) {
                isRunning = true
                hideGameUI()     // прячем нижние кнопки и старую карточку
                timer.start()
            }

            tapCount++
            tapSound.start()
            countText.text = getString(R.string.eggs_collected, tapCount)

            // меняем картинку яйца (с шансом редкого)
            setRandomEggImage()

            // и двигаем в новое место
            moveButtonRandomly()
        }

        resetButton.setOnClickListener {
            if (isRunning) {
                timer.cancel()
            }
            tapCount = 0
            bonusEggCount = 0
            isRunning = false

            // сбрасываем тексты
            timerText.text = getString(R.string.time_left_20)
            countText.text = getString(R.string.eggs_collected, tapCount)
            bonusEggText.text = getString(R.string.bonus_eggs_0)
            infoText.text = getString(R.string.start_collect_eggs)

            // возвращаем яйцо
            tapButton.isEnabled = true
            tapButton.visibility = View.VISIBLE
            tapButton.setImageResource(EGG_DEFAULT)
            lastEggResId = EGG_DEFAULT
            tapButton.x = originalX
            tapButton.y = originalY

            // скрываем итоговую карточку
            resultCard.visibility = View.GONE

            // показываем нижние кнопки (без анимации можно, но пусть останутся)
            bottomButtons.visibility = View.VISIBLE

            resetMoodTexts()
        }

        resetHighScoresButton.setOnClickListener {
            clearHighScores()
            topScoresText.text = ""
            Toast.makeText(
                this,
                getString(R.string.high_scores_cleared),
                Toast.LENGTH_SHORT
            ).show()
        }

        // стартовое состояние
        displayTopScores()
        resetMoodTexts()
        countText.text = getString(R.string.eggs_collected, tapCount)
        bonusEggText.text = getString(R.string.bonus_eggs_0)
        resultCard.visibility = View.GONE
    }

    // --- случайное яйцо + редкое ---

    private fun setRandomEggImage() {
        // шанс 15% показать редкое золотое яйцо
        val isRare = Random.nextInt(100) < 15

        val resId = if (isRare) {
            EGG_RARE
        } else {
            // обычное яйцо, стараемся не повторять то же
            var next = normalEggs.random()
            if (normalEggs.size > 1) {
                while (next == lastEggResId) {
                    next = normalEggs.random()
                }
            }
            next
        }

        tapButton.setImageResource(resId)
        lastEggResId = resId

        if (isRare) {
            bonusEggCount++
            bonusEggText.text = getString(R.string.bonus_eggs, bonusEggCount)
            Toast.makeText(this, getString(R.string.golden_egg_unlocked), Toast.LENGTH_SHORT).show()
        }
    }

    // --- движение яйца по экрану ---

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

        tapButton.bringToFront()
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
        if (topScores.isEmpty()) {
            topScoresText.text = ""
            return
        }

        val scoreText = StringBuilder()
        topScores.forEachIndexed { index, score ->
            scoreText.append("${index + 1}. $score\n")
        }
        topScoresText.text = scoreText.toString().trimEnd()
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
            topScores.addAll(savedScores.split(",").mapNotNull {
                it.toIntOrNull()
            })
        }
    }

    private fun clearHighScores() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.remove(SCORES_KEY)
        editor.apply()

        topScores.clear()
    }

    // --- плавные анимации для UI игры ---

    private fun fadeOutViews(vararg views: View) {
        views.forEach { v ->
            if (v.visibility == View.VISIBLE) {
                v.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction {
                        v.visibility = View.GONE
                        v.alpha = 1f
                    }
                    .start()
            }
        }
    }

    private fun fadeInViews(vararg views: View) {
        views.forEach { v ->
            v.alpha = 0f
            v.visibility = View.VISIBLE
            v.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }

    // прячем всё лишнее во время активной игры
    private fun hideGameUI() {
        fadeOutViews(bottomButtons, resultCard)
    }

    // показываем кнопки и карточку после окончания игры
    private fun showGameUIAfterFinish() {
        fadeInViews(bottomButtons, resultCard)
    }

    // --- итоговые тексты + поддержка ---

    private fun updateResultTexts() {
        // Total / Rare
        totalEggsText.text = getString(R.string.total_eggs_this_game, tapCount)

        val stars = "★".repeat(bonusEggCount.coerceAtMost(5))
        val rareBase = getString(R.string.rare_eggs_this_game, bonusEggCount)
        rareEggsText.text = if (bonusEggCount > 0) {
            "$rareBase  $stars"
        } else {
            rareBase
        }

        // поддерживающие фразы в зависимости от результата
        val moodTitle: String
        val moodLine2: String
        val moodLine3: String

        when {
            tapCount <= 5 -> {
                moodTitle = getString(R.string.mood_calm_title)
                moodLine2 = getString(R.string.mood_calm_line2)
                moodLine3 = getString(R.string.mood_calm_line3)
            }
            tapCount <= 15 -> {
                moodTitle = getString(R.string.mood_balanced_title)
                moodLine2 = getString(R.string.mood_balanced_line2)
                moodLine3 = getString(R.string.mood_balanced_line3)
            }
            else -> {
                moodTitle = getString(R.string.mood_super_title)
                moodLine2 = getString(R.string.mood_super_line2)
                moodLine3 = getString(R.string.mood_super_line3)
            }
        }

        greetingTextView.text = moodTitle
        anotherTextView.text = moodLine2
        oneMoreTextView.text = moodLine3

        // цвета под настроение
        val colorMain: Int
        val colorSecondary: Int

        when {
            tapCount <= 5 -> {
                colorMain = ContextCompat.getColor(this, R.color.sandy_brown)
                colorSecondary = ContextCompat.getColor(this, R.color.blue)
            }
            tapCount <= 15 -> {
                colorMain = ContextCompat.getColor(this, R.color.orange)
                colorSecondary = ContextCompat.getColor(this, R.color.yellow)
            }
            else -> {
                colorMain = ContextCompat.getColor(this, R.color.green)
                colorSecondary = ContextCompat.getColor(this, R.color.blue)
            }
        }

        greetingTextView.setTextColor(colorMain)
        anotherTextView.setTextColor(colorSecondary)
        oneMoreTextView.setTextColor(colorSecondary)
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
